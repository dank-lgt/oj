package com.example.friend.controller.question;


import com.example.commom.core.controller.BaseController;
import com.example.commom.core.domain.R;
import com.example.commom.core.domain.TableDataInfo;
import com.example.friend.domain.question.dto.QuestionQueryDTO;
import com.example.friend.domain.question.vo.QuestionDetailVO;
import com.example.friend.domain.question.vo.QuestionVO;
import com.example.friend.service.question.IQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/question")
public class QuestionController extends BaseController {

    @Autowired
    private IQuestionService questionService;

    @GetMapping("/semiLogin/list")
    public TableDataInfo list(QuestionQueryDTO questionQueryDTO) {
        return questionService.list(questionQueryDTO);
    }

    @GetMapping("/semiLogin/dbList")
    public TableDataInfo dbList(QuestionQueryDTO questionQueryDTO) {
        //数据库版题目列表接口
        return null;
    }

    @GetMapping("/semiLogin/hotList")
    public R<List<QuestionVO>> hotList() {
        return R.ok(questionService.hotList());
    }
//
    @GetMapping("/detail")
    public R<QuestionDetailVO> detail(Long questionId) {
        return R.ok(questionService.detail(questionId));
    }

    //题目的顺序列表 : 先从redis  redis中没有数据查询数据库
    // 当前题目是哪个（questionId）
    //redis  list  数据类型  key: q:l value : questionId
    @GetMapping("/preQuestion")
    public R<String> preQuestion(Long questionId) {
        return R.ok(questionService.preQuestion(questionId));
    }

    @GetMapping("/nextQuestion")
    public R<String> nextQuestion(Long questionId) {
        return R.ok(questionService.nextQuestion(questionId));
    }
}
