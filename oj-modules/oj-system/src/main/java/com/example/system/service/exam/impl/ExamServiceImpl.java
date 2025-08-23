package com.example.system.service.exam.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.domain.exam.ExamQuestion;
import com.example.system.domain.exam.dto.ExamQueryDTO;
import com.example.system.domain.exam.vo.ExamVO;
import com.example.system.mapper.exam.ExamMapper;
import com.example.system.mapper.exam.ExamQuestionMapper;
import com.example.system.mapper.question.QuestionMapper;
import com.example.system.service.exam.IExamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//@Service
//@Slf4j
//public class ExamServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements IExamService {
//
//    @Autowired
//    private ExamMapper examMapper;
//
//    @Autowired
//    private QuestionMapper questionMapper;
//
//    @Autowired
//    private ExamQuestionMapper examQuestionMapper;
//
//    @Autowired
//    private ExamCacheManager examCacheManager;
//
//
//}
