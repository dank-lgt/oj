package com.example.commom.core.enums;

import lombok.Getter;

@Getter
public enum QuestionResult {

    ERROR(0),

    PASS(1);

    private Integer value;

    QuestionResult(Integer value) {
        this.value = value;
    }
}
