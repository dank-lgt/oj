package com.example.common.message.service;

import cn.hutool.core.util.RandomUtil;
import com.example.commom.core.enums.ResultCode;
import com.example.common.message.config.EmailConfig;
import com.example.common.redis.service.RedisService;
import com.example.common.security.exception.EmailException;
import org.dromara.email.jakarta.comm.entity.MailMessage;
import org.dromara.email.jakarta.core.factory.MailFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private RedisService redisService;

    /**
     * 发送简单文本验证码邮件
     */
    public boolean sendSimpleVerifyCode(String toEmail) {
        try {
            String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

            MailMessage message = MailMessage.Builder()
                    .mailAddress(toEmail)
                    .title("您的验证码")
                    .body(String.format("您的验证码是: %s，该验证码%d分钟内有效。",
                            verifyCode, emailConfig.getExpireMinutes()))
                    .build();

            MailFactory.createMailClient("default").send(message);

            // 保存验证码到缓存（实际实现）
            saveVerifyCode(toEmail, verifyCode);

            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送HTML验证码邮件（使用模板文件）
     */
    public boolean sendHtmlVerifyCode(String toEmail) {
        try {
            String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

            // 准备模板参数
            Map<String, String> params = new HashMap<>();
            params.put("verifyCode", verifyCode);
            params.put("expireMinutes", String.valueOf(emailConfig.getExpireMinutes()));

            MailMessage message = MailMessage.Builder()
                    .mailAddress(toEmail)
                    .title("您的验证码")
                    .html("verify-code.html") // 模板文件位于resources/template目录
                    .htmlValues(params)
                    .build();

            //
            MailFactory.createMailClient("default").send(message);

            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
           throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送带附件的邮件
     */
    public boolean sendEmailWithAttachment(String toEmail, Map<String, String> attachments) {
        try {
            String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

            MailMessage message = MailMessage.Builder()
                    .mailAddress(toEmail)
                    .title("您的验证码和附件")
                    .body(String.format("您的验证码是: %s", verifyCode))
                    .files(attachments)
                    .build();

            MailFactory.createMailClient("default").send(message);

            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送压缩附件邮件
     */
    public boolean sendCompressedEmail(String toEmail, Map<String, String> attachments, String zipName) {
        try {
            String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

            MailMessage message = MailMessage.Builder()
                    .mailAddress(toEmail)
                    .title("您的验证码和压缩附件")
                    .body(String.format("您的验证码是: %s", verifyCode))
                    .files(attachments)
                    .zipName(zipName)
                    .build();

            MailFactory.createMailClient("default").send(message);

            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送给多个收件人（包括抄送和密送）
     */
    public boolean sendToMultipleRecipients(
            java.util.List<String> toEmails,
            java.util.List<String> ccEmails,
            java.util.List<String> bccEmails) {

        try {
            String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

            MailMessage message = MailMessage.Builder()
                    .mailAddress(toEmails)
                    .title("您的验证码")
                    .body(String.format("您的验证码是: %s", verifyCode))
                    .cc(ccEmails)
                    .bcc(bccEmails)
                    .build();

            MailFactory.createMailClient("default").send(message);

            // 为每个收件人保存验证码
            for (String email : toEmails) {
                saveVerifyCode(email, verifyCode);
            }

            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 保存验证码到缓存
     */
    private void saveVerifyCode(String email, String code) {
        // 实际实现应该使用Redis等缓存工具
        redisService.setCacheObject(email,code);
        redisService.expire("verify_code:" + code,
                emailConfig.getExpireMinutes(), TimeUnit.MINUTES);
        System.out.println("保存验证码: " + email + " -> " + code);
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String inputCode) {
        // 从缓存获取验证码（实际实现）
        // String savedCode = redisTemplate.opsForValue().get("verify_code:" + email);
//        String savedCode = "123456"; // 示例代码
        String savedCode = redisService.getCacheObject(email, String.class);
        return savedCode != null && savedCode.equals(inputCode);
    }
}