package com.educore.enrollment.entity.vo;

import com.educore.course.entity.enums.CourseStatus;
import com.educore.teachingclass.entity.enums.ClassStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor
public class EnrollmentOffering {
    private Long classId;
    private Long courseId;
    private BigDecimal price;
    private CourseStatus courseStatus;
    private ClassStatus classStatus;
    private LocalDate startDate;
    private LocalDate endDate;
}
