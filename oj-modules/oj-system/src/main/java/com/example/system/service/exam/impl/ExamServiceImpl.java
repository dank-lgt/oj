package com.example.system.service.exam.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.enums.ResultCode;
import com.example.common.security.exception.ServiceException;
import com.example.system.domain.exam.Exam;
import com.example.system.domain.exam.ExamQuestion;
import com.example.system.domain.exam.dto.ExamAddDTO;
import com.example.system.domain.exam.dto.ExamQueryDTO;
import com.example.system.domain.exam.dto.ExamQuestAddDTO;
import com.example.system.domain.exam.vo.ExamDetailVO;
import com.example.system.domain.exam.vo.ExamVO;
import com.example.system.domain.question.Question;
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

@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements IExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

//    @Autowired
//    private ExamCacheManager examCacheManager;


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
    // 检查竞赛状态（已注释掉的代码）
//        checkExam(exam);
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

    private boolean saveExamQuestion(Exam exam, Set<Long> questionIdSet) {
        int num = 1;
        List<ExamQuestion> examQuestionList = new ArrayList<>();
        for (Long questionId : questionIdSet) {
            ExamQuestion examQuestion = new ExamQuestion()
                    .setExamId(exam.getExamId()).setQuestionId(questionId).setQuestionOrder(num++);
            examQuestionList.add(examQuestion);
        }
        return saveBatch(examQuestionList);
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
        checkExam(exam);
        // 检查竞赛是否已发布，如果已发布则抛出异常
        if (Constants.TRUE.equals(exam.getStatus())) {
            throw new ServiceException(ResultCode.EXAM_IS_PUBLISH);
        }
        // 删除竞赛与题目的关联关系，并返回删除的记录数
        return examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .eq(ExamQuestion::getQuestionId, questionId));
    }

    @Override
    public ExamDetailVO detail(Long examId) {
        return null;
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
    public int publish(Long examId) {
        return 0;
    }

    @Override
    public int cancelPublish(Long examId) {
        return 0;
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
