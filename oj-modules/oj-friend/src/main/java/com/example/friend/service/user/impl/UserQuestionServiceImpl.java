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
import com.example.commom.core.enums.ResultCode;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.common.security.exception.ServiceException;
import com.example.friend.domain.question.Question;
import com.example.friend.domain.question.QuestionCase;
import com.example.friend.domain.user.dto.UserSubmitDTO;
import com.example.friend.elasticsearch.QuestionRepository;
import com.example.friend.mapper.question.QuestionMapper;
import com.example.friend.mapper.user.UserSubmitMapper;
import com.exapmle.friend.domain.question.es.QuestionES;
import com.example.friend.service.user.IUserQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserQuestionServiceImpl implements IUserQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private RemoteJudgeService remoteJudgeService;

//    @Autowired
//    private JudgeProducer judgeProducer;

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

//    @Override
//    public boolean rabbitSubmit(UserSubmitDTO submitDTO) {
//        Integer programType = submitDTO.getProgramType();
//        if (ProgramType.JAVA.getValue().equals(programType)) {
//            //按照java逻辑处理
//            JudgeSubmitDTO judgeSubmitDTO = assembleJudgeSubmitDTO(submitDTO);
//            judgeProducer.produceMsg(judgeSubmitDTO);
//            return true;
//        }
//        throw new ServiceException(ResultCode.FAILED_NOT_SUPPORT_PROGRAM);
//    }
//
//    @Override
//    public UserQuestionResultVO exeResult(Long examId, Long questionId, String currentTime) {
//        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
//        UserSubmit userSubmit = userSubmitMapper.selectCurrentUserSubmit(userId, examId, questionId, currentTime);
//        UserQuestionResultVO resultVO = new UserQuestionResultVO();
//        if (userSubmit == null) {
//            resultVO.setPass(QuestionResType.IN_JUDGE.getValue());
//        } else {
//            resultVO.setPass(userSubmit.getPass());
//            resultVO.setExeMessage(userSubmit.getExeMessage());
//            if (StrUtil.isNotEmpty(userSubmit.getCaseJudgeRes())) {
//                resultVO.setUserExeResultList(JSON.parseArray(userSubmit.getCaseJudgeRes(), UserExeResult.class));
//            }
//        }
//        return resultVO;
//    }

    private JudgeSubmitDTO assembleJudgeSubmitDTO(UserSubmitDTO submitDTO) {
        Long questionId = submitDTO.getQuestionId();
        QuestionES questionES = questionRepository.findById(questionId).orElse(null);
        JudgeSubmitDTO judgeSubmitDTO = new JudgeSubmitDTO();
        if (questionES != null) {
            BeanUtil.copyProperties(questionES, judgeSubmitDTO);
        } else {
            Question question = questionMapper.selectById(questionId);
            BeanUtil.copyProperties(question, judgeSubmitDTO);
            questionES = new QuestionES();
            BeanUtil.copyProperties(question, questionES);
            questionRepository.save(questionES);
        }
        judgeSubmitDTO.setUserId(ThreadLocalUtil.get(Constants.USER_ID, Long.class));
        judgeSubmitDTO.setExamId(submitDTO.getExamId());
        judgeSubmitDTO.setProgramType(submitDTO.getProgramType());
        judgeSubmitDTO.setUserCode(codeConnect(submitDTO.getUserCode(), questionES.getMainFuc()));
        List<QuestionCase> questionCaseList = JSONUtil.toList(questionES.getQuestionCase(), QuestionCase.class);
        List<String> inputList = questionCaseList.stream().map(QuestionCase::getInput).toList();
        judgeSubmitDTO.setInputList(inputList);
        List<String> outputList = questionCaseList.stream().map(QuestionCase::getOutput).toList();
        judgeSubmitDTO.setOutputList(outputList);
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
