package com.example.friend.service.question.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.domain.TableDataInfo;
import com.example.friend.domain.question.Question;
import com.example.friend.domain.question.dto.QuestionQueryDTO;
import com.example.friend.domain.question.vo.QuestionDetailVO;
import com.example.friend.domain.question.vo.QuestionVO;
import com.example.friend.elasticsearch.QuestionRepository;
import com.example.friend.manager.QuestionCacheManager;
import com.example.friend.mapper.question.QuestionMapper;
import com.example.friend.mapper.user.UserSubmitMapper;
import com.example.friend.service.question.IQuestionService;
import com.github.pagehelper.PageHelper;
import com.example.friend.domain.question.es.QuestionES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class QuestionServiceImpl implements IQuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private QuestionCacheManager questionCacheManager;

    @Override
    public TableDataInfo list(QuestionQueryDTO questionQueryDTO) {
        long count = questionRepository.count();
        long totalCount = questionMapper.selectCount(new LambdaQueryWrapper<>());
        if (count < totalCount / 2) {
            refreshQuestion();
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(questionQueryDTO.getPageNum() - 1, questionQueryDTO.getPageSize(), sort);
        Integer difficulty = questionQueryDTO.getDifficulty();
        String keyword = questionQueryDTO.getKeyword();
        Page<QuestionES> questionESPage;
        if (difficulty == null && StrUtil.isEmpty(keyword)) {
            questionESPage = questionRepository.findAll(pageable);
        } else if (StrUtil.isEmpty(keyword)) {
            questionESPage = questionRepository.findQuestionByDifficulty(difficulty, pageable);
        } else if (difficulty == null) {
            questionESPage = questionRepository.findByTitleOrContent(keyword, keyword, pageable);
        } else {
            questionESPage = questionRepository.findByTitleOrContentAndDifficulty(keyword, keyword, difficulty, pageable);
        }
        long total = questionESPage.getTotalElements();
        if (total <= 0) {
            return TableDataInfo.empty();
        }
        List<QuestionES> questionESList = questionESPage.getContent();
        List<QuestionVO> questionVOList = BeanUtil.copyToList(questionESList, QuestionVO.class);
        return TableDataInfo.success(questionVOList, total);
    }

    @Override
    public List<QuestionVO> hotList() {
        Long total = questionCacheManager.getHostListSize();
        List<Long> hotQuestionIdList;
        if (total == null || total <= 0) {
            PageHelper.startPage(Constants.HOST_QUESTION_LIST_START, Constants.HOST_QUESTION_LIST_END);
            hotQuestionIdList = userSubmitMapper.selectHostQuestionList();
            questionCacheManager.refreshHotQuestionList(hotQuestionIdList);
        } else {
            hotQuestionIdList = questionCacheManager.getHostList();
        }
        return assembleQuestionVOList(hotQuestionIdList);
    }

    @Override
    public QuestionDetailVO detail(Long questionId) {
        QuestionES questionES = questionRepository.findById(questionId).orElse(null);
        QuestionDetailVO questionDetailVO = new QuestionDetailVO();
        if (questionES != null) {
            BeanUtil.copyProperties(questionES, questionDetailVO);
            return questionDetailVO;
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return null;
        }
        refreshQuestion();
        BeanUtil.copyProperties(question, questionDetailVO);
        return questionDetailVO;
    }

    @Override
    public String preQuestion(Long questionId) {
        Long listSize = questionCacheManager.getListSize();
        if (listSize == null || listSize <= 0) {
            questionCacheManager.refreshCache();
        }
        return questionCacheManager.preQuestion(questionId).toString();
    }

    @Override
    /**
     * 获取下一个问题的方法
     * @param questionId 当前问题的ID，用于获取下一个问题
     * @return 返回下一个问题的字符串表示
     */
    public String nextQuestion(Long questionId) {
        // 从缓存管理器获取问题列表的大小
        Long listSize = questionCacheManager.getListSize();
        // 检查列表是否为空或无效
        if (listSize == null || listSize <= 0) {
            // 如果列表为空或无效，刷新缓存
            questionCacheManager.refreshCache();
        }
        // 获取下一个问题并将其转换为字符串返回
        return questionCacheManager.nextQuestion(questionId).toString();
    }

    /**
     * 刷新问题数据到Elasticsearch的方法
     * 从数据库中查询所有问题，然后同步到Elasticsearch中
     */
    private void refreshQuestion() {
        // 从数据库中查询所有问题
        List<Question> questionList = questionMapper.selectList(new LambdaQueryWrapper<Question>());
        // 如果查询结果为空，则直接返回
        if (CollectionUtil.isEmpty(questionList)) {
            return;
        }
        // 将数据库实体列表转换为Elasticsearch实体列表
        List<QuestionES> questionESList = BeanUtil.copyToList(questionList, QuestionES.class);
        // 将所有问题数据保存到Elasticsearch中
        questionRepository.saveAll(questionESList);
    }

    /**
     * 将热门问题ID列表转换为问题VO对象列表
     *
     * @param hotQuestionIdList 热门问题ID列表
     * @return 转换后的问题VO对象列表，如果输入列表为空则返回空列表
     */
    private List<QuestionVO> assembleQuestionVOList(List<Long> hotQuestionIdList) {
        // 检查输入列表是否为空，如果为空直接返回空列表
        if (CollectionUtil.isEmpty(hotQuestionIdList)) {
            return new ArrayList<>();
        }
        // 创建结果列表
        List<QuestionVO> resultList = new ArrayList<>();
        // 遍历问题ID列表
        for (Long questionId : hotQuestionIdList) {
            // 创建问题VO对象
            QuestionVO questionVO = new QuestionVO();
            // 获取问题详细信息
            QuestionDetailVO detail = detail(questionId);
            // 设置问题标题
            questionVO.setTitle(detail.getTitle());
            // 将问题VO添加到结果列表
            resultList.add(questionVO);
        }
        // 返回结果列表
        return resultList;
    }
}
