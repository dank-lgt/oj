package com.example.system.domain.exam.dto;

import com.example.system.TimeCheck.inte.NotLaterThan;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamEditDTO extends ExamAddDTO {
    private Long examId;
}
