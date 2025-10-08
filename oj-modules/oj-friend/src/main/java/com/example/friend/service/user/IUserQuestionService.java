package com.example.friend.service.user;


import com.example.api.domain.vo.UserQuestionResultVO;
import com.example.commom.core.domain.R;
import com.example.friend.domain.user.dto.UserSubmitDTO;

public interface IUserQuestionService {
    R<UserQuestionResultVO> submit(UserSubmitDTO submitDTO);

    boolean rabbitSubmit(UserSubmitDTO submitDTO);
//
    UserQuestionResultVO exeResult(Long examId, Long questionId, String currentTime);
}
