package com.example.friend.aspect;

import com.example.commom.core.constans.Constants;
import com.example.commom.core.enums.ResultCode;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.common.security.exception.ServiceException;
import com.example.friend.domain.user.vo.UserVO;
import com.example.friend.manager.UserCacheManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
public class UserStatusCheckAspect {

    @Autowired
    private UserCacheManager userCacheManager;

    @Before(value = "@annotation(com.example.friend.aspect.CheckUserStatus)")
    public void before(JoinPoint point){
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        UserVO user = userCacheManager.getUserById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if (Objects.equals(user.getStatus(), Constants.FALSE)) {
            throw new ServiceException(ResultCode.FAILED_USER_BANNED);
        }
    }
}
