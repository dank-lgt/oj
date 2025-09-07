package com.example.system.service.question.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.enums.ResultCode;
import com.example.common.security.exception.ServiceException;
import com.example.system.domain.question.Question;
import com.example.system.domain.question.dto.QuestionAddDTO;
import com.example.system.domain.question.dto.QuestionEditDTO;
import com.example.system.domain.question.dto.QuestionQueryDTO;
import com.example.system.domain.question.vo.QuestionDetailVO;
import com.example.system.domain.question.vo.QuestionVO;
import com.example.system.mapper.question.QuestionMapper;
import com.example.system.service.question.IQuestionService;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionServiceImpl implements IQuestionService {


    @Autowired
    private QuestionMapper questionMapper;

//    @Autowired
//    private QuestionRepository questionRepository;
//
//    @Autowired
//    private QuestionCacheManager questionCacheManager;

    /**
     * 查询问题列表
     *
     * @param questionQueryDTO 问题查询条件DTO，包含分页信息和查询条件
     * @return 返回问题列表，包含分页数据
     */
    @Override
    public List<QuestionVO> list(QuestionQueryDTO questionQueryDTO) {
        String excludeIdStr = questionQueryDTO.getExcludeIdStr();
        if (StrUtil.isNotEmpty(excludeIdStr)) {
            String[] excludeIdArr = excludeIdStr.split(Constants.SPLIT_SEM);
            Set<Long> excludeIdSet = Arrays.stream(excludeIdArr)
                    .map(String::trim) // 去除空格
                    .filter(StrUtil::isNotBlank) // 过滤空字符串
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
            questionQueryDTO.setExcludeIdSet(excludeIdSet);
        }
        PageHelper.startPage(questionQueryDTO.getPageNum(), questionQueryDTO.getPageSize());
        List<QuestionVO> questionVOList = questionMapper.selectQuestionList(questionQueryDTO);
        return questionVOList;
        //questionVOList == null || questionVOList.isEmpty()
//        if (CollectionUtil.isEmpty(questionVOList)) {
//            return TableDataInfo.empty();
//        }
//        new PageInfo<>(questionVOList).getTotal(); //获取符合查询条件的数据的总数
//        return TableDataInfo.success(questionVOList, questionVOList.size());
    }

    /**
     * 添加问题方法
     *
     * @param questionAddDTO 添加问题的数据传输对象
     * @return 添加成功返回true，否则返回false
     */
    @Override
    public boolean add(QuestionAddDTO questionAddDTO) {
        // 查询数据库中是否已存在相同标题的问题
        List<Question> questionList = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getTitle, questionAddDTO.getTitle()));
        if (CollectionUtil.isNotEmpty(questionList)) {
            throw new ServiceException(ResultCode.FAILED_ALREADY_EXISTS);
        }
        Question question = new Question();
        BeanUtil.copyProperties(questionAddDTO, question);
        int insert = questionMapper.insert(question);
        if (insert <= 0) {
            return false;
        }
//        QuestionES questionES = new QuestionES();
//        BeanUtil.copyProperties(question, questionES);
//        questionRepository.save(questionES);
//        questionCacheManager.addCache(question.getQuestionId());
        return true;
    }


    /**
     * 获取问题详情的方法
     *
     * @param questionId 问题ID
     * @return QuestionDetailVO 问题详情的视图对象
     */
    @Override
    public QuestionDetailVO detail(Long questionId) {
        // 根据问题ID查询问题信息
        Question question = questionMapper.selectById(questionId);
        // 判断问题是否存在，不存在则抛出异常
        if (question == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 创建问题详情视图对象
        QuestionDetailVO questionDetailVO = new QuestionDetailVO();
        // 将问题对象的属性复制到视图对象中
        BeanUtils.copyProperties(question, questionDetailVO);
        // 返回问题详情视图对象
        return questionDetailVO;
    }

    /**
     * 编辑问题信息的方法
     *
     * @param questionEditDTO 包含问题编辑信息的DTO对象
     * @return 返回更新的记录数
     * @throws ServiceException 当要编辑的问题不存在时抛出异常
     */
    @Override
    public int edit(QuestionEditDTO questionEditDTO) {
        // 根据问题ID查询旧的问题信息
        Question oldQuestion = questionMapper.selectById(questionEditDTO.getQuestionId());
        // 检查问题是否存在，不存在则抛出异常
        if (oldQuestion == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // 更新问题的标题
        oldQuestion.setTitle(questionEditDTO.getTitle());
        // 更新问题的难度
        oldQuestion.setDifficulty(questionEditDTO.getDifficulty());
        // 更新问题的时间限制
        oldQuestion.setTimeLimit(questionEditDTO.getTimeLimit());
        // 更新问题的空间限制
        oldQuestion.setSpaceLimit(questionEditDTO.getSpaceLimit());
        // 更新问题的内容
        oldQuestion.setContent(questionEditDTO.getContent());
        // 更新问题的测试用例
        oldQuestion.setQuestionCase(questionEditDTO.getQuestionCase());
        // 更新问题的默认代码
        oldQuestion.setDefaultCode(questionEditDTO.getDefaultCode());
        // 更新问题的主函数
        oldQuestion.setMainFuc(questionEditDTO.getMainFuc());
        // 执行更新操作并返回更新的记录数
        return questionMapper.updateById(oldQuestion);
    }


    @Override
    /**
     * 根据问题ID删除问题
     * @param questionId 问题ID
     * @return 返回受影响的行数，如果删除成功则返回1
     * @throws ServiceException 当问题不存在时抛出异常
     */
    public int delete(Long questionId) {
        // First, check if the question exists
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            // Throw exception if question doesn't exist
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        // If exists, delete the question and return affected rows
        return questionMapper.deleteById(questionId);
    }
}