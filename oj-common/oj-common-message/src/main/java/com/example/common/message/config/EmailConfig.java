package com.example.common.message.config;

import org.dromara.email.jakarta.comm.config.MailSmtpConfig;
import org.dromara.email.jakarta.core.factory.MailFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private String port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String auth;

    @Value("${sms.email.from}")
    private String from;

    // 直接使用spring.mail.properties.mail.smtp.ssl.enable的值，或者默认为true
    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private String sslEnable;

    @Value("${app.verify-code.length}")
    private int codeLength;

    @Value("${app.verify-code.expire-minutes}")
    private int expireMinutes;

    @Value("${app.send-limit:3}")
    private Integer sendLimit;

    @Value("${app.is-send}")
    private boolean isSend;  //开关打开：true  开关关闭false

    @Bean
    public MailSmtpConfig mailSmtpConfig() {
        return MailSmtpConfig.builder()
                .smtpServer(host)
                .port(port)
                .fromAddress(from)
                .nickName("Web-oj")
                .username(username)
                .password(password)
                .isSSL(sslEnable)
                .isAuth("true")
                .build();
    }

    @Bean
    public void initMailFactory() {
        MailFactory.put("default", mailSmtpConfig());
    }

    public int getCodeLength() {
        return codeLength;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public boolean getIsSend() {
        return isSend;
    }

    public Integer getSendLimit() {
        return sendLimit;
    }
}