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

/**
 * 用户状态检查切面类
 * 用于在需要检查用户状态的方法执行前进行拦截验证
 */
@Aspect
@Component
public class UserStatusCheckAspect {

    @Autowired
    private UserCacheManager userCacheManager;  // 用户缓存管理器，用于获取用户信息

    /**
     * 前置通知方法
     * 在带有@CheckUserStatus注解的方法执行前进行用户状态检查
     * @param point 连接点，可以获取目标方法的相关信息
     */
    @Before(value = "@annotation(com.example.friend.aspect.CheckUserStatus)")
    public void before(JoinPoint point){
        // 从ThreadLocal中获取当前用户ID
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        // 根据用户ID从缓存中获取用户信息
        UserVO user = userCacheManager.getUserById(userId);
        // 检查用户是否存在，不存在则抛出用户不存在异常
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        // 检查用户状态是否为禁用状态，是则抛出用户被禁用异常
        if (Objects.equals(user.getStatus(), Constants.FALSE)) {
            throw new ServiceException(ResultCode.FAILED_USER_BANNED);
        }
    }
}
