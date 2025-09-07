package com.example.system.service.exam.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.enums.ResultCode;
import com.example.common.security.exception.ServiceException;
import com.example.system.domain.exam.Exam;
import com.example.system.domain.exam.ExamQuestion;
import com.example.system.domain.exam.dto.ExamAddDTO;
import com.example.system.domain.exam.dto.ExamEditDTO;
import com.example.system.domain.exam.dto.ExamQueryDTO;
import com.example.system.domain.exam.dto.ExamQuestAddDTO;
import com.example.system.domain.exam.vo.ExamDetailVO;
import com.example.system.domain.exam.vo.ExamVO;
import com.example.system.domain.question.Question;
import com.example.system.domain.question.vo.QuestionVO;
import com.example.system.manager.ExamCacheManager;
import com.example.system.mapper.exam.ExamMapper;
import com.example.system.mapper.exam.ExamQuestionMapper;
import com.example.system.mapper.question.QuestionMapper;
import com.example.system.service.exam.IExamService;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements IExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private ExamCacheManager examCacheManager;


    /**
     * 查询竞赛列表
     *
     * @param examQueryDTO 竞赛查询条件DTO，包含分页信息和查询条件
     * @return 返回竞赛列表，包含分页数据
     */
    @Override
    public List<ExamVO> list(ExamQueryDTO examQueryDTO) {
        PageHelper.startPage(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
        List<ExamVO> examVOList = examMapper.selectExamList(examQueryDTO);
        return examVOList;
    }


    /**
     * 添加竞赛的方法
     *
     * @param examAddDTO 包含竞赛信息的DTO对象
     * @return 返回新创建的竞赛ID
     */
    @Override
    public String add(ExamAddDTO examAddDTO) {
        // 检查竞赛保存参数的有效性
        checkExamSaveParams(examAddDTO, null);
        // 创建新的竞赛对象
        Exam exam = new Exam();
        // 将DTO对象的属性复制到竞赛对象中
        BeanUtil.copyProperties(examAddDTO, exam);
        // 将竞赛信息插入到数据库中
        examMapper.insert(exam);
        // 返回新创建的竞赛ID
        return exam.getExamId().toString();
    }

    /**
     * 检查竞赛保存参数的合法性
     *
     * @param examAddDTO 竞赛添加数据传输对象，包含竞赛相关信息
     * @param examId     竞赛ID，用于更新操作时排除当前记录
     */
    private void checkExamSaveParams(ExamAddDTO examAddDTO, Long examId) {
        // 查询数据库中是否存在相同标题的竞赛记录（排除当前竞赛ID）
        List<Exam> examList = examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getTitle, examAddDTO.getTitle())  // 设置查询条件为竞赛标题等于传入的竞赛标题
                .ne(examId != null, Exam::getExamId, examId));  // 如果examId不为null，则排除当前竞赛ID
        // 如果查询结果不为空，说明已存在相同标题的竞赛
        if (CollectionUtil.isNotEmpty(examList)) {
            // 抛出业务异常，提示竞赛标题已存在
            throw new ServiceException(ResultCode.FAILED_ALREADY_EXISTS);
        }
    }


/**
 * 添加竞赛题目
 * @param examQuestAddDTO 竞赛题目添加DTO对象，包含竞赛ID和题目ID集合
 * @return 添加成功返回true，否则抛出异常
 */
    @Override
    public boolean questionAdd(ExamQuestAddDTO examQuestAddDTO) {
    // 根据竞赛ID获取竞赛对象
        Exam exam = getExam(examQuestAddDTO.getExamId());
    // 检查竞赛状态
        checkExam(exam);
    // 判断竞赛是否已发布
        if (Constants.TRUE.equals(exam.getStatus())) {
        // 如果竞赛已发布，则抛出异常
            throw new ServiceException(ResultCode.EXAM_IS_PUBLISH);
        }
    // 获取题目ID集合
        LinkedHashSet<Long> questionIdSet = examQuestAddDTO.getQuestionIdSet();
    // 如果题目ID集合为空，则直接返回true
        if (CollectionUtil.isEmpty(questionIdSet)) {
            return true;
        }
    // 根据题目ID集合查询题目列表
        List<Question> questionList = questionMapper.selectBatchIds(questionIdSet);
    // 检查题目是否存在，如果题目列表为空或题目数量不匹配则抛出异常
        if (CollectionUtil.isEmpty(questionList) || questionList.size() < questionIdSet.size()) {
            throw new ServiceException(ResultCode.EXAM_QUESTION_NOT_EXISTS);
        }
    // 保存竞赛题目关系并返回结果
        return saveExamQuestion(exam, questionIdSet);
    }

/**
 * 保存考试题目关联信息
 * @param exam 考试对象，包含考试ID等信息
 * @param questionIdSet 题目ID集合，需要关联到该考试的题目
 * @return boolean 是否批量保存成功
 */
private boolean saveExamQuestion(Exam exam, Set<Long> questionIdSet) {
    Long examId = exam.getExamId();

    // 1. 查询该考试已存在的题目关联记录
    List<ExamQuestion> existingQuestions = this.list(new QueryWrapper<ExamQuestion>()
            .eq("exam_id", examId)
            .orderByAsc("question_order"));

    // 2. 获取已存在的题目ID集合和当前最大题号
    Set<Long> existingQuestionIds = existingQuestions.stream()
            .map(ExamQuestion::getQuestionId)
            .collect(Collectors.toSet());

    int maxOrder = existingQuestions.stream()
            .mapToInt(ExamQuestion::getQuestionOrder)
            .max()
            .orElse(0); // 如果没有现有记录，从0开始

    // 3. 筛选出新题目ID（排除已存在的）
    Set<Long> newQuestionIds = questionIdSet.stream()
            .filter(id -> !existingQuestionIds.contains(id))
            .collect(Collectors.toSet());

    // 如果没有新题目需要添加，直接返回true
    if (CollectionUtil.isEmpty(newQuestionIds)) {
        log.info("考试ID: {} 没有需要添加的新题目，所有题目已存在", examId);
        return true;
    }

    // 4. 创建新题目的关联列表
    List<ExamQuestion> examQuestionList = new ArrayList<>();
    int currentOrder = maxOrder + 1; // 从当前最大序号+1开始

    for (Long questionId : newQuestionIds) {
        ExamQuestion examQuestion = new ExamQuestion()
                .setExamId(examId)
                .setQuestionId(questionId)
                .setQuestionOrder(currentOrder++);
        examQuestionList.add(examQuestion);
    }

    // 5. 批量保存新题目的关联信息
    boolean saveResult = saveBatch(examQuestionList);

    if (saveResult) {
        log.info("考试ID: {} 成功添加 {} 个新题目，题号从 {} 开始",
                examId, newQuestionIds.size(), maxOrder + 1);
    }

    return saveResult;
}


    /**
     * 删除竞赛中的指定题目
     * @param examId 竞赛ID
     * @param questionId 题目ID
     * @return 删除的记录数
     * @throws ServiceException 当竞赛已发布时抛出异常
     */
    @Override
    public int questionDelete(Long examId, Long questionId) {
        // 根据竞赛ID获取竞赛信息
        Exam exam = getExam(examId);
        // 检查竞赛是否存在
//        checkExam(exam);
        // 检查竞赛是否已发布，如果已发布则抛出异常
        if (Constants.TRUE.equals(exam.getStatus())) {
            throw new ServiceException(ResultCode.EXAM_IS_PUBLISH);
        }
        // 删除竞赛与题目的关联关系，并返回删除的记录数
        return examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .eq(ExamQuestion::getQuestionId, questionId));
    }

/**
 * 获取竞赛详情信息
 * @param examId 竞赛ID
 * @return ExamDetailVO 竞赛详情视图对象，包含竞赛基本信息及题目列表
 */
    @Override
    public ExamDetailVO detail(Long examId) {
    // 创建竞赛详情视图对象
        ExamDetailVO examDetailVO = new ExamDetailVO();
    // 根据竞赛ID获取竞赛实体对象
        Exam exam = getExam(examId);
    // 将竞赛实体对象的属性复制到视图对象中
        BeanUtil.copyProperties(exam, examDetailVO);
    // 查询竞赛相关的题目列表
        List<QuestionVO> questionVOList = examQuestionMapper.selectExamQuestionList(examId);
    // 如果题目列表为空，直接返回竞赛详情对象
        if (CollectionUtil.isEmpty(questionVOList)) {
            return examDetailVO;
        }
    // 设置竞赛详情对象的题目列表
        examDetailVO.setExamQuestionList(questionVOList);
    // 返回完整的竞赛详情信息
        return examDetailVO;
    }

/**
 * 编辑竞赛信息的方法
 * @param examEditDTO 包含竞赛编辑信息的DTO对象
 * @return 返回更新的记录数
 * @throws ServiceException 如果竞赛已发布，则抛出异常
 */
    @Override
    public int edit(ExamEditDTO examEditDTO) {
    // 根据竞赛ID获取竞赛实体对象
        Exam exam = getExam(examEditDTO.getExamId());
    // 检查竞赛状态是否为已发布
        if (Constants.TRUE.equals(exam.getStatus())) {
        // 如果竞赛已发布，抛出异常
            throw new ServiceException(ResultCode.EXAM_IS_PUBLISH);
        }
    // 检查竞赛信息是否有效
        checkExam(exam);
    // 检查竞赛保存参数是否合法
        checkExamSaveParams(examEditDTO, examEditDTO.getExamId());
    // 更新竞赛标题
        exam.setTitle(examEditDTO.getTitle());
    // 更新竞赛开始时间
        exam.setStartTime(examEditDTO.getStartTime());
    // 更新竞赛结束时间
        exam.setEndTime(examEditDTO.getEndTime());
    // 执行更新操作并返回更新的记录数
        return examMapper.updateById(exam);
    }

    @Override
    /**
     * 删除竞赛信息
     * @param examId 竞赛ID
     * @return 删除的记录数
     * @throws ServiceException 如果竞赛已发布，则抛出异常
     */
    public int delete(Long examId) {
        // 根据竞赛ID获取竞赛信息
        Exam exam = getExam(examId);
        // 检查竞赛状态是否已发布
        if (Constants.TRUE.equals(exam.getStatus())) {
            // 如果竞赛已发布，抛出异常
            throw new ServiceException(ResultCode.EXAM_IS_PUBLISH);
        }
        // 检查竞赛的其他条件
        checkExam(exam);
        // 删除竞赛与题目的关联关系
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId));
        // 删除竞赛记录
        return examMapper.deleteById(exam);
    }

    @Override
    /**
     * 发布竞赛方法
     * @param examId 竞赛ID
     * @return 返回更新的记录数
     */
    public int publish(Long examId) {
        // 根据竞赛ID查询竞赛信息
        Exam exam = getExam(examId);
        // 检查竞赛是否已结束（通过比较竞赛结束时间和当前时间）
        //select count(0) from tb_exam_question where exam_id = #{examId}
        if (exam.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.EXAM_IS_FINISH);
        }
        // 查询该竞赛关联的题目数量
        Long count = examQuestionMapper
                .selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        // 如果竞赛没有题目，则抛出异常
        if (count == null || count <= 0) {
            throw new ServiceException(ResultCode.EXAM_NOT_HAS_QUESTION);
        }
        // 设置竞赛状态为已发布
        exam.setStatus(Constants.TRUE);

        //要将新发布的竞赛数据存储到redis   e:t:l  e:d:examId
        examCacheManager.addCache(exam);
        return examMapper.updateById(exam);
    }

/**
 * 取消竞赛发布的方法
 * @param examId 竞赛ID
 * @return 更新后的记录数，通常为1表示成功
 */
    @Override
    public int cancelPublish(Long examId) {
    // 根据竞赛ID获取竞赛信息
        Exam exam = getExam(examId);
    // 检查竞赛是否存在
        checkExam(exam);
    // 判断竞赛是否已经结束，如果已结束则抛出异常
        if (exam.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.EXAM_IS_FINISH);
        }
    // 将竞赛状态设置为未发布
        exam.setStatus(Constants.FALSE);
    // 删除竞赛缓存
        examCacheManager.deleteCache(examId);
    // 更新数据库中的竞赛信息并返回更新记录数
        return examMapper.updateById(exam);
    }

    private void checkExam(Exam exam) {
        if (exam.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.EXAM_STARTED);
        }
    }

    private Exam getExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }
        return exam;
    }
}
