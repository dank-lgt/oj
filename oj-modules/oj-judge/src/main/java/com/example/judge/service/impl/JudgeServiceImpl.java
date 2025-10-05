package com.example.judge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.domain.UserExeResult;
import com.example.api.domain.dto.JudgeSubmitDTO;
import com.example.api.domain.vo.UserQuestionResultVO;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.constans.JudgeConstants;
import com.example.commom.core.enums.CodeRunStatus;
import com.example.judge.domain.SandBoxExecuteResult;
import com.example.judge.domain.UserSubmit;
import com.example.judge.mapper.UserSubmitMapper;
import com.example.judge.service.IJudgeService;
import com.example.judge.service.ISandboxPoolService;
import com.example.judge.service.ISandboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class JudgeServiceImpl implements IJudgeService {

    @Autowired
    private ISandboxService sandboxService;

    @Autowired
    private ISandboxPoolService sandboxPoolService;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Override
    public UserQuestionResultVO doJudgeJavaCode(JudgeSubmitDTO judgeSubmitDTO) {
        // 添加参数校验
        if (judgeSubmitDTO == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        log.info("---- 判题逻辑开始 -------");
        UserQuestionResultVO userQuestionResultVO = new UserQuestionResultVO();
        SandBoxExecuteResult sandBoxExecuteResult = null;
        try {
            sandBoxExecuteResult =
                    sandboxPoolService.exeJavaCode(judgeSubmitDTO.getUserId(), judgeSubmitDTO.getUserCode(), judgeSubmitDTO.getInputList());
            
            if (sandBoxExecuteResult != null && CodeRunStatus.SUCCEED.equals(sandBoxExecuteResult.getRunStatus())) {
                //比对直接结果  时间限制  空间限制的比对
                userQuestionResultVO = doJudge(judgeSubmitDTO, sandBoxExecuteResult, userQuestionResultVO);
            } else {
                userQuestionResultVO.setPass(Constants.FALSE);
                if (sandBoxExecuteResult != null) {
                    userQuestionResultVO.setExeMessage(sandBoxExecuteResult.getExeMessage());
                } else {
                    userQuestionResultVO.setExeMessage(CodeRunStatus.UNKNOWN_FAILED.getMsg());
                }
                userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            }
        } catch (Exception e) {
            log.error("判题过程中发生异常: ", e);
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.UNKNOWN_FAILED.getMsg());
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
        } finally {
            // 确保用户提交记录被保存
            try {
                saveUserSubmit(judgeSubmitDTO, userQuestionResultVO);
            } catch (Exception e) {
                log.error("保存用户提交记录时发生异常: ", e);
            }
        }
        log.info("判题逻辑结束，判题结果为： {} ", userQuestionResultVO);
        return userQuestionResultVO;
    }

    private UserQuestionResultVO doJudge(JudgeSubmitDTO judgeSubmitDTO,
                                         SandBoxExecuteResult sandBoxExecuteResult,
                                         UserQuestionResultVO userQuestionResultVO) {
        // 添加空值检查
        if (judgeSubmitDTO == null || sandBoxExecuteResult == null || userQuestionResultVO == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        List<String> exeOutputList = sandBoxExecuteResult.getOutputList();
        List<String> outputList = judgeSubmitDTO.getOutputList();
        
        // 添加空值检查
        if (outputList == null || exeOutputList == null) {
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.NOT_ALL_PASSED.getMsg());
            return userQuestionResultVO;
        }
        
        if (outputList.size() != exeOutputList.size()) {
            userQuestionResultVO.setScore(JudgeConstants.ERROR_SCORE);
            userQuestionResultVO.setPass(Constants.FALSE);
            userQuestionResultVO.setExeMessage(CodeRunStatus.NOT_ALL_PASSED.getMsg());
            return userQuestionResultVO;
        }
        List<UserExeResult> userExeResultList = new ArrayList<>();
        boolean passed = resultCompare(judgeSubmitDTO, exeOutputList, outputList, userExeResultList);
        return assembleUserQuestionResultVO(judgeSubmitDTO, sandBoxExecuteResult, userQuestionResultVO, userExeResultList, passed);
    }

    private UserQuestionResultVO assembleUserQuestionResultVO(JudgeSubmitDTO judgeSubmitDTO,
                                                              SandBoxExecuteResult sandBoxExecuteResult,
                                                              UserQuestionResultVO userQuestionResultVO,
                                                              List<UserExeResult> userExeResultList, boolean passed) {
        userQuestionResultVO.setUserExeResultList(userExeResultList);
        if (!passed) {
            return setResultVOFailure(userQuestionResultVO, CodeRunStatus.NOT_ALL_PASSED.getMsg());
        }
        
        // 将资源检查前置，使逻辑更清晰
        if (sandBoxExecuteResult.getUseMemory() > judgeSubmitDTO.getSpaceLimit()) {
            return setResultVOFailure(userQuestionResultVO, CodeRunStatus.OUT_OF_MEMORY.getMsg());
        }
        
        if (sandBoxExecuteResult.getUseTime() > judgeSubmitDTO.getTimeLimit()) {
            return setResultVOFailure(userQuestionResultVO, CodeRunStatus.OUT_OF_TIME.getMsg());
        }
        
        userQuestionResultVO.setPass(Constants.TRUE);
        int score = judgeSubmitDTO.getDifficulty() * JudgeConstants.DEFAULT_SCORE;
        userQuestionResultVO.setScore(score);
        return userQuestionResultVO;
    }
    
    /**
     * 设置结果VO为失败状态的通用方法
     */
    private UserQuestionResultVO setResultVOFailure(UserQuestionResultVO vo, String message) {
        vo.setPass(Constants.FALSE);
        vo.setScore(JudgeConstants.ERROR_SCORE);
        vo.setExeMessage(message);
        return vo;
    }

    private boolean resultCompare(JudgeSubmitDTO judgeSubmitDTO, List<String> exeOutputList,
                                  List<String> outputList, List<UserExeResult> userExeResultList) {
        boolean passed = true;
        for (int index = 0; index < outputList.size(); index++) {
            String output = outputList.get(index);
            String exeOutPut = exeOutputList.get(index);
            String input = judgeSubmitDTO.getInputList().get(index);
            UserExeResult userExeResult = new UserExeResult();
            userExeResult.setInput(input);
            userExeResult.setOutput(output);
            userExeResult.setExeOutput(exeOutPut);
            userExeResultList.add(userExeResult);
            if (!output.equals(exeOutPut)) {
                passed = false;
                log.info("输入：{}， 期望输出：{}， 实际输出：{} ", input, output, exeOutputList);
            }
        }
        return passed;
    }

    private void saveUserSubmit(JudgeSubmitDTO judgeSubmitDTO, UserQuestionResultVO userQuestionResultVO) {
        UserSubmit userSubmit = new UserSubmit();
        BeanUtil.copyProperties(userQuestionResultVO, userSubmit);
        userSubmit.setUserId(judgeSubmitDTO.getUserId());
        userSubmit.setQuestionId(judgeSubmitDTO.getQuestionId());
        userSubmit.setExamId(judgeSubmitDTO.getExamId());
        userSubmit.setProgramType(judgeSubmitDTO.getProgramType());
        userSubmit.setUserCode(judgeSubmitDTO.getUserCode());
        userSubmit.setCaseJudgeRes(JSON.toJSONString(userQuestionResultVO.getUserExeResultList()));
        userSubmit.setCreateBy(judgeSubmitDTO.getUserId());
        userSubmitMapper.delete(new LambdaQueryWrapper<UserSubmit>()
                .eq(UserSubmit::getUserId, judgeSubmitDTO.getUserId())
                .eq(UserSubmit::getQuestionId, judgeSubmitDTO.getQuestionId())
                .isNull(judgeSubmitDTO.getExamId() == null, UserSubmit::getExamId)
                .eq(judgeSubmitDTO.getExamId() != null, UserSubmit::getExamId, judgeSubmitDTO.getExamId()));
        userSubmitMapper.insert(userSubmit);
    }
}
