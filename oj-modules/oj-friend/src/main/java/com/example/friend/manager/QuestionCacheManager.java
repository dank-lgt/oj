package com.example.friend.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.commom.core.constans.CacheConstants;
import com.example.commom.core.enums.ResultCode;
import com.example.common.redis.service.RedisService;
import com.example.common.security.exception.ServiceException;
import com.example.friend.domain.question.Question;
import com.example.friend.mapper.question.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 问题缓存管理器
 * 负责处理问题相关的缓存操作，包括普通问题列表和热门问题列表的缓存管理
 */
@Component
public class QuestionCacheManager {

    @Autowired
    private RedisService redisService;  // Redis服务，用于操作Redis缓存

    @Autowired
    private QuestionMapper questionMapper;  // 问题数据访问层，用于从数据库获取问题数据

    /**
     * 获取普通问题列表的大小
     * @return 普通问题列表的大小
     */
    public Long getListSize() {
        return redisService.getListSize(CacheConstants.QUESTION_LIST);
    }

    /**
     * 获取热门问题列表的大小
     * @return 热门问题列表的大小
     */
    public Long getHostListSize() {
        return redisService.getListSize(CacheConstants.QUESTION_HOST_LIST);
    }

    /**
     * 刷新问题缓存
     * 从数据库获取最新问题列表，并更新到Redis缓存中
     */
    public void refreshCache() {
        // 从数据库查询问题ID列表，按创建时间降序排列
        List<Question> questionList = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .select(Question::getQuestionId).orderByDesc(Question::getCreateTime));
        // 如果列表为空，直接返回
        if (CollectionUtil.isEmpty(questionList)) {
            return;
        }
        // 提取问题ID列表
        List<Long> questionIdList = questionList.stream().map(Question::getQuestionId).toList();
        // 将问题ID列表添加到Redis列表的右侧
        redisService.rightPushAll(CacheConstants.QUESTION_LIST, questionIdList);
    }

    /**
     * 获取当前问题的上一个问题ID
     * @param questionId 当前问题ID
     * @return 上一个问题的ID
     * @throws ServiceException 如果当前问题是第一个问题
     */
    public Long preQuestion(Long questionId) {
        // 注释掉的代码：获取整个问题列表
//        List<Long> list = redisService.getCacheListByRange(CacheConstants.QUESTION_LIST, 0, -1, Long.class);
        // 获取当前问题在列表中的索引
        Long index = redisService.indexOfForList(CacheConstants.QUESTION_LIST, questionId);
        // 如果当前问题是第一个问题，抛出异常
        if (index == 0) {
            throw new ServiceException(ResultCode.FAILED_FIRST_QUESTION);
        }
        // 返回上一个问题的ID
        return redisService.indexForList(CacheConstants.QUESTION_LIST, index - 1, Long.class);
    }

    /**
     * 获取当前问题的下一个问题ID
     * @param questionId 当前问题ID
     * @return 下一个问题的ID
     * @throws ServiceException 如果当前问题是最后一个问题
     */
    public Object nextQuestion(Long questionId) {
        // 获取当前问题在列表中的索引
        Long index = redisService.indexOfForList(CacheConstants.QUESTION_LIST, questionId);
        // 获取列表的最后一个索引
        long lastIndex = getListSize() - 1;
        // 如果当前问题是最后一个问题，抛出异常
        if (index == lastIndex) {
            throw new ServiceException(ResultCode.FAILED_LAST_QUESTION);
        }
        // 返回下一个问题的ID
        return redisService.indexForList(CacheConstants.QUESTION_LIST, index + 1, Long.class);
    }

    /**
     * 获取热门问题列表
     * @return 热门问题ID列表
     */
    public List<Long> getHostList() {
        // 从Redis获取指定范围的热门问题列表
        return redisService.getCacheListByRange(CacheConstants.QUESTION_HOST_LIST,
                CacheConstants.DEFAULT_START, CacheConstants.DEFAULT_END, Long.class);
    }

    /**
     * 刷新热门问题列表
     * @param hotQuestionIdList 热门问题ID列表
     */
    public void refreshHotQuestionList(List<Long> hotQuestionIdList) {
        // 如果列表为空，直接返回
        if (CollectionUtil.isEmpty(hotQuestionIdList)) {
            return;
        }
        // 将热门问题ID列表添加到Redis列表的右侧
        redisService.rightPushAll(CacheConstants.QUESTION_HOST_LIST, hotQuestionIdList);
    }
}
