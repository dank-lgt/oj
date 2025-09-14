package com.example.common.message.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.verify-code")
@Getter
@Setter
public class VerifyCodeConfig {
    private int length = 6;
    private int expireMinutes = 5;
}