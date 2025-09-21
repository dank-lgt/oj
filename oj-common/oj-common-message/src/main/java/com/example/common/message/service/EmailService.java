package com.example.common.message.service;

import cn.hutool.core.util.RandomUtil;
import com.example.commom.core.enums.ResultCode;
import com.example.common.message.config.EmailConfig;
import com.example.common.redis.service.RedisService;
import com.example.common.security.exception.EmailException;
import org.dromara.email.jakarta.api.MailClient;
import org.dromara.email.jakarta.comm.entity.MailMessage;
import org.dromara.email.jakarta.core.factory.MailFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class EmailService {
    private  MailClient mailClient;

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private RedisService redisService;

    // 邮件模板配置
//    @Value("${email.template.verify-code:verify-code.html}")
//    private String verifyCodeTemplate;

    /**
     * 发送简单文本验证码邮件
     */
    public boolean sendSimpleVerifyCode(String toEmail) {
        String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());
        MailMessage message = MailMessage.Builder()
                .mailAddress(toEmail)
                .title("您的验证码")
                .body(String.format("您的验证码是: %s，该验证码%d分钟内有效。",
                        verifyCode, emailConfig.getExpireMinutes()))
                .build();
        try {
            mailClient = MailFactory.createMailClient("default");
            mailClient.send(message);
//            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送HTML验证码邮件（使用模板文件）
     */
    public boolean sendHtmlVerifyCode(String toEmail,String verifyCode) {
//        String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());
        // 准备模板参数
        Map<String, String> params = new HashMap<>();
        params.put("verifyCode", verifyCode);
        params.put("expireMinutes", String.valueOf(emailConfig.getExpireMinutes()));

        MailMessage message = MailMessage.Builder()
                .mailAddress(toEmail)
                .title("您的验证码")
                .html("verify-code.html")
                .htmlValues(params)
                .build();

        try {
            mailClient = MailFactory.createMailClient("default");
            mailClient.send(message);
//            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送带附件的邮件
     */
    public boolean sendEmailWithAttachment(String toEmail, Map<String, String> attachments) {
        String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

        MailMessage message = MailMessage.Builder()
                .mailAddress(toEmail)
                .title("您的验证码和附件")
                .body(String.format("您的验证码是: %s", verifyCode))
                .files(attachments)
                .build();

        try {
            mailClient = MailFactory.createMailClient("default");
            mailClient.send(message);
//            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送压缩附件邮件
     */
    public boolean sendCompressedEmail(String toEmail, Map<String, String> attachments, String zipName) {
        String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

        MailMessage message = MailMessage.Builder()
                .mailAddress(toEmail)
                .title("您的验证码和压缩附件")
                .body(String.format("您的验证码是: %s", verifyCode))
                .files(attachments)
                .zipName(zipName)
                .build();

        try {
            mailClient = MailFactory.createMailClient("default");
            mailClient.send(message);
//            saveVerifyCode(toEmail, verifyCode);
            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 发送给多个收件人（包括抄送和密送）
     */
    public boolean sendToMultipleRecipients(List<String> toEmails, List<String> ccEmails, List<String> bccEmails) {
        String verifyCode = RandomUtil.randomNumbers(emailConfig.getCodeLength());

        MailMessage message = MailMessage.Builder()
                .mailAddress(toEmails)
                .title("您的验证码")
                .body(String.format("您的验证码是: %s", verifyCode))
                .cc(ccEmails)
                .bcc(bccEmails)
                .build();

        try {
            mailClient = MailFactory.createMailClient("default");
            mailClient.send(message);
            // 为每个收件人保存验证码
//            for (String email : toEmails) {
//                saveVerifyCode(email, verifyCode);

            return true;
        } catch (Exception e) {
            throw new EmailException(ResultCode.FAILED_SEND_CODE);
        }
    }

    /**
     * 保存验证码到缓存
     */
    private void saveVerifyCode(String email, String code) {
        String cacheKey = "verify_code:" + email;
        redisService.setCacheObject(cacheKey, code);
        redisService.expire(cacheKey, emailConfig.getExpireMinutes(), TimeUnit.MINUTES);
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String inputCode) {
        String cacheKey = "verify_code:" + email;
        String savedCode = redisService.getCacheObject(cacheKey, String.class);
        return savedCode != null && savedCode.equals(inputCode);
    }

    public long getExpireMinutes() {
        return emailConfig.getExpireMinutes();
    }

    public int getCodeLength() {
        return emailConfig.getCodeLength();
    }

    public Boolean getIsSend() {
        return emailConfig.getIsSend();
    }

    public Integer getSendLimit() {
        return emailConfig.getSendLimit();
    }
}