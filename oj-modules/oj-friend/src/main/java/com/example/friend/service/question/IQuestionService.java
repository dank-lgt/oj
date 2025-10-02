package com.example.friend.service.question;



import com.example.commom.core.domain.TableDataInfo;
import com.example.friend.domain.question.dto.QuestionQueryDTO;
import com.example.friend.domain.question.vo.QuestionDetailVO;
import com.example.friend.domain.question.vo.QuestionVO;

import java.util.List;

public interface IQuestionService {

    TableDataInfo list(QuestionQueryDTO questionQueryDTO);

    List<QuestionVO> hotList();

    QuestionDetailVO detail(Long questionId);

    String preQuestion(Long questionId);

    String nextQuestion(Long questionId);
}
