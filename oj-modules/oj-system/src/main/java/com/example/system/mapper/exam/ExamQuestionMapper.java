package com.example.system.mapper.exam;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.system.domain.exam.ExamQuestion;
import com.example.system.domain.question.vo.QuestionVO;

import java.util.List;

public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    List<QuestionVO> selectExamQuestionList(Long examId);
}
