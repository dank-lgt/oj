package com.example.system.service.question.impl;


import com.example.system.domain.question.dto.QuestionAddDTO;
import com.example.system.domain.question.dto.QuestionEditDTO;
import com.example.system.domain.question.dto.QuestionQueryDTO;
import com.example.system.domain.question.vo.QuestionDetailVO;
import com.example.system.domain.question.vo.QuestionVO;
import com.example.system.mapper.question.QuestionMapper;
import com.example.system.service.question.IQuestionService;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public List<QuestionVO> list(QuestionQueryDTO questionQueryDTO) {
        PageHelper.startPage(questionQueryDTO.getPageNum(), questionQueryDTO.getPageSize());
        List<QuestionVO> questionVOList = questionMapper.selectQuestionList(questionQueryDTO);
        return questionVOList;
    }

    @Override
    public boolean add(QuestionAddDTO questionAddDTO) {
        return false;
    }

    @Override
    public QuestionDetailVO detail(Long questionId) {
        return null;
    }

    @Override
    public int edit(QuestionEditDTO questionEditDTO) {
        return 0;
    }

    @Override
    public int delete(Long questionId) {
        return 0;
    }
}