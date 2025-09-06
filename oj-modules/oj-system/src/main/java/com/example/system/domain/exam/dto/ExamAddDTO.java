package com.example.system.domain.exam.dto;

import com.example.system.TimeCheck.inte.NotLaterThan;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NotLaterThan(
        startTimeField = "startTime",
        endTimeField = "endTime",
        message = "结束时间必须比开始时间晚至少2小时"
)
public class ExamAddDTO {

    private String title;

    @FutureOrPresent(message = "开始时间不能早于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull
    private LocalDateTime endTime;
}
