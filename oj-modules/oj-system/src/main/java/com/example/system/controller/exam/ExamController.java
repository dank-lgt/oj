package com.example.system.controller.exam;

import com.example.commom.core.controller.BaseController;
import com.example.commom.core.domain.R;
import com.example.commom.core.domain.TableDataInfo;
import com.example.system.domain.exam.dto.ExamAddDTO;
import com.example.system.domain.exam.dto.ExamEditDTO;
import com.example.system.domain.exam.dto.ExamQueryDTO;
import com.example.system.domain.exam.dto.ExamQuestAddDTO;
import com.example.system.domain.exam.vo.ExamDetailVO;
import com.example.system.service.exam.IExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exam")
public class ExamController extends BaseController {

//    @Autowired
//    private IExamService examService;
//
//    //exam/list
//    @GetMapping("/list")
//    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
//        return getTableDataInfo(examService.list(examQueryDTO));
//    }
//
//    @PostMapping("/add")
//    public R<String> add(@RequestBody ExamAddDTO examAddDTO) {
//        return R.ok(examService.add(examAddDTO));
//    }
//
//    @PostMapping("/question/add")
//    public R<Void> questionAdd(@RequestBody ExamQuestAddDTO examQuestAddDTO) {
//        return toR(examService.questionAdd(examQuestAddDTO));
//    }
//
//    @DeleteMapping("/question/delete")
//    public R<Void> questionDelete(Long examId, Long questionId) {
//        return toR(examService.questionDelete(examId, questionId));
//    }
//
//    @GetMapping("/detail")
//    public R<ExamDetailVO> detail(Long examId) {
//        return R.ok(examService.detail(examId));
//    }
//
//    @PutMapping("/edit")
//    public R<Void> edit(@RequestBody ExamEditDTO examEditDTO) {
//        return toR(examService.edit(examEditDTO));
//    }
//
//    @DeleteMapping("/delete")
//    public R<Void> delete(Long examId) {
//        return toR(examService.delete(examId));
//    }
//
//    @PutMapping("/publish")
//    public R<Void> publish(Long examId) {
//        return toR(examService.publish(examId));
//    }
//
//    @PutMapping("/cancelPublish")
//    public R<Void> cancelPublish(Long examId) {
//        return toR(examService.cancelPublish(examId));
//    }
}
