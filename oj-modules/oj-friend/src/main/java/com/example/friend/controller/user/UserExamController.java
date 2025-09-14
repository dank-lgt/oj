//package com.example.friend.controller.user;
//
//
//import com.example.commom.core.constans.HttpConstants;
//import com.example.commom.core.controller.BaseController;
//import com.example.commom.core.domain.R;
//import com.example.commom.core.domain.TableDataInfo;
//import com.example.friend.domain.exam.dto.ExamDTO;
//import com.example.friend.domain.exam.dto.ExamQueryDTO;
//import com.example.friend.service.user.IUserExamService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/user/exam")
//public class UserExamController extends BaseController {
//
//    @Autowired
//    private IUserExamService userExamService;
//
////    @CheckUserStatus
//    @PostMapping("/enter")
//    public R<Void> enter(@RequestHeader(HttpConstants.AUTHENTICATION) String token, @RequestBody ExamDTO examDTO) {
//        return toR(userExamService.enter(token, examDTO.getExamId()));
//    }
//
//    @GetMapping("/list")
//    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
//        return userExamService.list(examQueryDTO);
//    }
//}
