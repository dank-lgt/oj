package com.example.friend.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.example.api.RemoteJudgeService;
import com.example.api.domain.UserExeResult;
import com.example.api.domain.dto.JudgeSubmitDTO;
import com.example.api.domain.vo.UserQuestionResultVO;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.domain.R;
import com.example.commom.core.enums.ProgramType;
import com.example.commom.core.enums.QuestionResType;
import com.example.commom.core.enums.ResultCode;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.common.security.exception.ServiceException;
import com.example.friend.domain.question.Question;
import com.example.friend.domain.question.QuestionCase;
import com.example.friend.domain.user.UserSubmit;
import com.example.friend.domain.user.dto.UserSubmitDTO;
import com.example.friend.elasticsearch.QuestionRepository;
import com.example.friend.mapper.question.QuestionMapper;
import com.example.friend.mapper.user.UserSubmitMapper;
import com.example.friend.rabbit.JudgeProducer;
import com.example.friend.domain.question.es.QuestionES;
import com.example.friend.service.user.IUserQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserQuestionServiceImpl implements IUserQuestionService {


    public static void main(String[] args) {
        System.out.println(JSON.toJSONString(new UserExeResult()));
    }

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private RemoteJudgeService remoteJudgeService;

    @Autowired
    private JudgeProducer judgeProducer;

    @Override
    public R<UserQuestionResultVO> submit(UserSubmitDTO submitDTO) {
        Integer programType = submitDTO.getProgramType();
        if (ProgramType.JAVA.getValue().equals(programType)) {
            //按照java逻辑处理
            JudgeSubmitDTO judgeSubmitDTO = assembleJudgeSubmitDTO(submitDTO);
            return remoteJudgeService.doJudgeJavaCode(judgeSubmitDTO);
        }
        throw new ServiceException(ResultCode.FAILED_NOT_SUPPORT_PROGRAM);
    }

    @Override
    public boolean rabbitSubmit(UserSubmitDTO submitDTO) {
        Integer programType = submitDTO.getProgramType();
        if (ProgramType.JAVA.getValue().equals(programType)) {
            //按照java逻辑处理
            JudgeSubmitDTO judgeSubmitDTO = assembleJudgeSubmitDTO(submitDTO);
            judgeProducer.produceMsg(judgeSubmitDTO);
            return true;
        }
        throw new ServiceException(ResultCode.FAILED_NOT_SUPPORT_PROGRAM);
    }

    @Override
    public UserQuestionResultVO exeResult(Long examId, Long questionId, String currentTime) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        UserSubmit userSubmit = userSubmitMapper.selectCurrentUserSubmit(userId, examId, questionId, currentTime);
        UserQuestionResultVO resultVO = new UserQuestionResultVO();
        if (userSubmit == null) {
            resultVO.setPass(QuestionResType.IN_JUDGE.getValue());
        } else {
            resultVO.setPass(userSubmit.getPass());
            resultVO.setExeMessage(userSubmit.getExeMessage());
            if (StrUtil.isNotEmpty(userSubmit.getCaseJudgeRes())) {
                resultVO.setUserExeResultList(JSON.parseArray(userSubmit.getCaseJudgeRes(), UserExeResult.class));
            }
        }
        return resultVO;
    }

/**
 * 将用户提交的数据转换为判题所需的数据传输对象(DTO)
 * 该方法处理题目信息，从数据库或缓存中获取题目详情，并组装判题所需的数据
 *
 * @param submitDTO 用户提交的数据传输对象，包含题目ID、考试ID、代码等信息
 * @return JudgeSubmitDTO 包含判题所需完整信息的DTO对象
 */
    private JudgeSubmitDTO assembleJudgeSubmitDTO(UserSubmitDTO submitDTO) {
    // 获取题目ID
        Long questionId = submitDTO.getQuestionId();
    // 尝试从ES(搜索引擎)中获取题目信息
        QuestionES questionES = questionRepository.findById(questionId).orElse(null);
    // 创建判题DTO对象
        JudgeSubmitDTO judgeSubmitDTO = new JudgeSubmitDTO();
    // 如果ES中存在题目信息
        if (questionES != null) {
        // 将ES中的题目信息复制到判题DTO中
            BeanUtil.copyProperties(questionES, judgeSubmitDTO);
        } else {
        // 如果ES中不存在题目信息，则从数据库中获取
            Question question = questionMapper.selectById(questionId);
        // 将数据库中的题目信息复制到判题DTO中
            BeanUtil.copyProperties(question, judgeSubmitDTO);
        // 创建ES题目对象并保存
            questionES = new QuestionES();
            BeanUtil.copyProperties(question, questionES);
            questionRepository.save(questionES);
        }
    // 设置用户ID(从线程本地变量中获取)
        judgeSubmitDTO.setUserId(ThreadLocalUtil.get(Constants.USER_ID, Long.class));
    // 设置考试ID
        judgeSubmitDTO.setExamId(submitDTO.getExamId());
    // 设置程序类型
        judgeSubmitDTO.setProgramType(submitDTO.getProgramType());
    // 合并用户代码和题目主函数
        judgeSubmitDTO.setUserCode(codeConnect(submitDTO.getUserCode(), questionES.getMainFuc()));
    // 将题目的测试用例转换为列表
        List<QuestionCase> questionCaseList = JSONUtil.toList(questionES.getQuestionCase(), QuestionCase.class);
    // 提取所有测试用例的输入
        List<String> inputList = questionCaseList.stream().map(QuestionCase::getInput).toList();
        judgeSubmitDTO.setInputList(inputList);
    // 提取所有测试用例的输出
        List<String> outputList = questionCaseList.stream().map(QuestionCase::getOutput).toList();
        judgeSubmitDTO.setOutputList(outputList);
    // 返回组装好的判题DTO
        return judgeSubmitDTO;
    }

    private String codeConnect(String userCode, String mainFunc) {
        String targetCharacter = "}";
        int targetLastIndex = userCode.lastIndexOf(targetCharacter);
        if (targetLastIndex != -1) {
            return userCode.substring(0, targetLastIndex) + "\n" + mainFunc + "\n" + userCode.substring(targetLastIndex);
        }
        throw new ServiceException(ResultCode.FAILED);
    }
}
