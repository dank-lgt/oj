package com.example.system.manager;


import com.example.commom.core.constans.CacheConstants;
import com.example.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuestionCacheManager {

    @Autowired
    private RedisService redisService;

/**
 * 将问题ID添加到缓存列表中
 * 使用Redis列表结构存储问题ID，新添加的问题ID会被添加到列表的左侧（头部）
 * @param questionId 需要缓存的问题ID
 */
    public void addCache(Long questionId) {
    // 调用Redis服务，将问题ID添加到指定名称的列表左侧
        redisService.leftPushForList(CacheConstants.QUESTION_LIST, questionId);
    }

/**
 * 根据问题ID删除缓存
 * @param questionId 问题ID，用于标识需要删除缓存的问题
 */
    public void deleteCache(Long questionId) {
    // 调用redisService的方法从问题列表缓存中移除指定questionId的缓存
        redisService.removeForList(CacheConstants.QUESTION_LIST, questionId);
    }
}
