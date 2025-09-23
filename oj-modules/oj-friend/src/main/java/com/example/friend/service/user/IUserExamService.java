package com.example.friend.service.user;

import com.example.commom.core.domain.TableDataInfo;
import com.example.friend.domain.exam.dto.ExamQueryDTO;

public interface IUserExamService {

    int enter(String token, Long examId);

    TableDataInfo list(ExamQueryDTO examQueryDTO);
}
