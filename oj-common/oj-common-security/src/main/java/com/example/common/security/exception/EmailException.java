package com.example.common.security.exception;

import com.example.commom.core.enums.ResultCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailException extends RuntimeException {
    private ResultCode resultCode;

    public EmailException(ResultCode resultCode) {
        this.resultCode = resultCode;
    }
}