//package com.example.friend.controller.user;
//
//
//import com.example.commom.core.controller.BaseController;
//import com.example.commom.core.domain.PageQueryDTO;
//import com.example.commom.core.domain.TableDataInfo;
//import com.example.friend.service.user.IUserMessageService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/user/message")
//public class UserMessageController extends BaseController {
//
//    @Autowired
//    private IUserMessageService userMessageService;
//
//    @GetMapping("/list")
//    public TableDataInfo list(PageQueryDTO dto) {
//        return userMessageService.list(dto);
//    }
//}
