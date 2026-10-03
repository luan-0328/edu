package com.educore.enrollment.vo;

import com.educore.course.enums.CourseStatus;
import com.educore.teachingclass.enums.ClassStatus;
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
