package com.educore.examination.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.educore.examination.enums.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("exam_question")
public class ExamQuestionEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long examId; private Long sourceQuestionId; private QuestionType type;
    private String contentSnapshot; private String optionsSnapshot; private String answerSnapshot; private QuestionDifficulty difficulty;
    private BigDecimal score; private Integer sortOrder; private LocalDateTime createdAt;
}
