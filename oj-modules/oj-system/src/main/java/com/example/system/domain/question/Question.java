package com.example.system.domain.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.commom.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@TableName("tb_question")
@Getter
@Setter
@Schema(description = "题目")
public class Question extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long questionId;

    @Schema(description = "题目名称")
    @NotBlank(message = "题目名称不能为空")
    private String title;

    @Schema(description = "难度等级")
    private Integer difficulty;

    @Schema(description = "时间限制")
    private Long timeLimit;

    @Schema(description = "空间限制")
    private Long spaceLimit;

    @Schema(description = "题目内容")
    private String content;

    @Schema(description = "题目样例")
    private String questionCase;

    @Schema(description = "题目答案")
    private String defaultCode;

    @Schema(description = "主函数")
    private String mainFuc;
}
