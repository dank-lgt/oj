package com.example.system.manager;


import com.example.commom.core.constans.CacheConstants;
import com.example.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuestionCacheManager {

    @Autowired
    private RedisService redisService;

    public void addCache(Long questionId) {
        redisService.leftPushForList(CacheConstants.QUESTION_LIST, questionId);
    }

    public void deleteCache(Long questionId) {
        redisService.removeForList(CacheConstants.QUESTION_LIST, questionId);
    }
}
