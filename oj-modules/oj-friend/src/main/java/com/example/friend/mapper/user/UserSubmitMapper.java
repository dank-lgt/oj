package com.example.friend.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.friend.domain.user.UserSubmit;

import java.util.List;

public interface UserSubmitMapper extends BaseMapper<UserSubmit> {

    UserSubmit selectCurrentUserSubmit(Long userId, Long examId, Long questionId, String currentTime);

    List<Long> selectHostQuestionList();
}
