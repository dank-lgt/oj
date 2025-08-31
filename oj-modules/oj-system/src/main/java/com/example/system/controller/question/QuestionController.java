package com.example.system.controller.question;


import com.example.commom.core.controller.BaseController;
import com.example.commom.core.domain.R;
import com.example.commom.core.domain.TableDataInfo;
import com.example.system.domain.question.dto.QuestionAddDTO;
import com.example.system.domain.question.dto.QuestionEditDTO;
import com.example.system.domain.question.dto.QuestionQueryDTO;
import com.example.system.domain.question.vo.QuestionDetailVO;
import com.example.system.service.question.IQuestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question")
@Tag(name = "题目管理接口")
public class QuestionController extends BaseController {

    @Autowired
    private IQuestionService questionService;

    @GetMapping("/list")
    public TableDataInfo list(QuestionQueryDTO questionQueryDTO) {
        return getTableDataInfo(questionService.list(questionQueryDTO));
    }

    //  /question/add
    @PostMapping("/add")
    public R<Void> add(@RequestBody QuestionAddDTO questionAddDTO) {
        return toR(questionService.add(questionAddDTO));
    }

    //  /question/detail
    @GetMapping("/detail")
    public R<QuestionDetailVO> detail(Long questionId) {
        return R.ok(questionService.detail(questionId));
    }

    //  /question/edit
    @PutMapping("/edit")
    public R<Void> edit(@RequestBody QuestionEditDTO questionEditDTO) {
        return toR(questionService.edit(questionEditDTO));
    }

    //  /question/delete
    @DeleteMapping("/delete")
    public R<Void> delete(Long questionId) {
        return toR(questionService.delete(questionId));
    }
}
