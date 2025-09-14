package com.example.friend.test;


import com.example.commom.core.controller.BaseController;
import com.example.commom.core.domain.R;
import com.example.common.message.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController extends BaseController {

//    @Autowired
//    private AliSmsService aliSmsService;
    @Autowired
    private EmailService emailService;

    @GetMapping("/sendCode")
    public R<Void> sendCode(String email) {
        log.info("验证码发送测试");
        return toR(emailService.sendSimpleVerifyCode(email));
    }
}
