package com.example.system.mapper.exam;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.system.domain.exam.Exam;
import com.example.system.domain.exam.dto.ExamQueryDTO;
import com.example.system.domain.exam.vo.ExamVO;

import java.util.List;

public interface ExamMapper extends BaseMapper<Exam> {

    List<ExamVO> selectExamList(ExamQueryDTO examQueryDTO);

}
