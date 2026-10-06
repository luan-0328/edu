package com.educore.dashboard.mapper;

import com.educore.dashboard.entity.vo.ExamAnalyticsView;
import com.educore.dashboard.entity.vo.StudentExamHistoryView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import java.util.List;

@Mapper
public interface ClassAnalyticsMapper {
    ClassMetrics classMetrics(@Param("classId") Long classId);

    List<ExamAnalyticsView> exams(@Param("classId") Long classId);

    List<StudentExamHistoryView> recentStudentResults(@Param("classId") Long classId);

    @Select("SELECT id,name,teacher_id FROM edu_class WHERE id=#{classId}")
    @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="name",javaType=String.class),@Arg(column="teacher_id",javaType=Long.class)})
    ClassOwner classOwner(@Param("classId") Long classId);

    record ClassOwner(Long id,String name,Long teacherId) { }
    record ClassMetrics(Long classId,String className,String status,Long plannedLessons,Long completedLessons,
                        java.math.BigDecimal attendanceRate,java.math.BigDecimal assignmentCompletionRate,
                        java.math.BigDecimal averageScore,java.math.BigDecimal highestScore,
                        java.math.BigDecimal lowestScore,java.math.BigDecimal examPassRate) { }
}
