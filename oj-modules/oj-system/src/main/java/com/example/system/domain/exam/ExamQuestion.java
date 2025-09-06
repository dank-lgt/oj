package com.example.system.domain.exam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.commom.core.domain.BaseEntity;
import io.netty.channel.ChannelHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_exam_question")
public class ExamQuestion extends BaseEntity {

    @TableId(value = "EXAM_QUESTION_ID", type = IdType.ASSIGN_ID)
    private Long examQuestionId;

    private Long examId;

    private Long questionId;

    private Integer questionOrder;
}