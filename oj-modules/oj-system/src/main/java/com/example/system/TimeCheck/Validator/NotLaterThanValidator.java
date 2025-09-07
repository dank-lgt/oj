package com.example.system.TimeCheck.Validator;

import com.example.system.TimeCheck.inte.NotLaterThan;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class NotLaterThanValidator implements ConstraintValidator<NotLaterThan, Object> {

    private String startTimeField;
    private String endTimeField;
    private long minHoursDifference; // 最小小时差

    @Override
    public void initialize(NotLaterThan constraintAnnotation) {
        this.startTimeField = constraintAnnotation.startTimeField();
        this.endTimeField = constraintAnnotation.endTimeField();
        this.minHoursDifference = constraintAnnotation.minHours(); // 新增参数
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field startField = value.getClass().getDeclaredField(startTimeField);
            Field endField = value.getClass().getDeclaredField(endTimeField);

            startField.setAccessible(true);
            endField.setAccessible(true);

            Object startValue = startField.get(value);
            Object endValue = endField.get(value);

            if (startValue == null || endValue == null) {
                return true; // 允许空值，由@NotNull等其他注解处理
            }

            // 处理Date类型
            if (startValue instanceof Date && endValue instanceof Date) {
                Date startDate = (Date) startValue;
                Date endDate = (Date) endValue;

                if (endDate.before(startDate)) {
                    return false; // 结束时间早于开始时间
                }

                // 计算小时差
                long diffInMillis = endDate.getTime() - startDate.getTime();
                long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

                return diffInHours >= minHoursDifference;
            }

            // 处理LocalDateTime类型
            if (startValue instanceof LocalDateTime && endValue instanceof LocalDateTime) {
                LocalDateTime startDateTime = (LocalDateTime) startValue;
                LocalDateTime endDateTime = (LocalDateTime) endValue;

                if (endDateTime.isBefore(startDateTime)) {
                    return false; // 结束时间早于开始时间
                }

                // 计算小时差
                long diffInMinutes = ChronoUnit.MINUTES.between(startDateTime, endDateTime);
                return diffInMinutes >= minHoursDifference * 60;
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
        return false;
    }
}