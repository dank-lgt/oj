package com.example.judge.service;


import com.example.api.domain.dto.JudgeSubmitDTO;
import com.example.api.domain.vo.UserQuestionResultVO;

public interface IJudgeService {
    UserQuestionResultVO doJudgeJavaCode(JudgeSubmitDTO judgeSubmitDTO);
}
