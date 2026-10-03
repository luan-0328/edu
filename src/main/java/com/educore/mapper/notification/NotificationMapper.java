package com.educore.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educore.notification.entity.NotificationEntity;
import com.educore.notification.vo.NotificationView;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {
    @Insert("INSERT IGNORE INTO notification(user_id,source_type,source_id,title,content) VALUES(#{userId},'ASSIGNMENT',#{assignmentId},#{title},#{content})")
    int insertForStudent(@Param("userId") Long userId,@Param("assignmentId") Long assignmentId,@Param("title") String title,@Param("content") String content);
    @Select("SELECT id,source_type,source_id,title,content,read_at,created_at FROM notification WHERE user_id=#{userId} ORDER BY created_at DESC,id DESC LIMIT 200")
    @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="source_type",javaType=String.class),@Arg(column="source_id",javaType=Long.class),@Arg(column="title",javaType=String.class),@Arg(column="content",javaType=String.class),@Arg(column="read_at",javaType=java.time.LocalDateTime.class),@Arg(column="created_at",javaType=java.time.LocalDateTime.class)})
    List<NotificationView> listMine(@Param("userId") Long userId);
    @Select("SELECT COUNT(*) FROM notification WHERE user_id=#{userId} AND read_at IS NULL") long unreadCount(@Param("userId") Long userId);
    @Update("UPDATE notification SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP(3)) WHERE id=#{id} AND user_id=#{userId}") int markRead(@Param("id") Long id,@Param("userId") Long userId);
    @Select("SELECT COUNT(*) FROM notification WHERE id=#{id} AND user_id=#{userId}") int owns(@Param("id") Long id,@Param("userId") Long userId);
    @Select("SELECT student_id FROM class_student WHERE class_id=#{classId} AND status='ENROLLED' ORDER BY student_id")
    List<Long> enrolledStudents(@Param("classId") Long classId);
}
