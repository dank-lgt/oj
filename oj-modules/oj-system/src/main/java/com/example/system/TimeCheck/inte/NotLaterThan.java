package com.example.system.TimeCheck.inte;

import com.example.system.TimeCheck.Validator.NotLaterThanValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotLaterThanValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotLaterThan {
    String message() default "结束时间不能早于开始时间";

    String startTimeField();
    String endTimeField();

    // 新增：最小小时差，默认2小时
    int minHours() default 2;

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}