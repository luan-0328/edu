SET NAMES utf8mb4;

-- Repair the demo records affected by importing UTF-8 SQL through a legacy
-- Windows code page. Scope every update to the documented demo accounts.
SET @teacher_id = (SELECT id FROM sys_user WHERE username = 'demo-teacher');
SET @student_id = (SELECT id FROM sys_user WHERE username = 'demo-student');
SET @class_id = (
    SELECT c.id
    FROM edu_class c
    JOIN class_student cs ON cs.class_id = c.id
    WHERE c.teacher_id = @teacher_id AND cs.student_id = @student_id
    ORDER BY c.id
    LIMIT 1
);
SET @course_id = (SELECT course_id FROM edu_class WHERE id = @class_id);

UPDATE sys_user
SET real_name = CASE username
    WHEN 'demo-admin' THEN '演示管理员'
    WHEN 'demo-teacher' THEN '林老师'
    WHEN 'demo-student' THEN '陈同学'
END
WHERE username IN ('demo-admin', 'demo-teacher', 'demo-student');

UPDATE course
SET name = 'Java 后端工程实践',
    description = '从 Java 基础、Spring Boot 到关系数据库与并发事务的完整训练。'
WHERE id = @course_id;

UPDATE edu_class
SET name = 'Java 后端实战班'
WHERE id = @class_id;

SET @classroom_id = (
    SELECT classroom_id FROM class_schedule
    WHERE class_id = @class_id
    ORDER BY id
    LIMIT 1
);
UPDATE classroom room
LEFT JOIN classroom other_room
  ON other_room.name = '演示教室 A' AND other_room.id <> @classroom_id
SET room.name = '演示教室 A'
WHERE room.id = @classroom_id AND other_room.id IS NULL;

UPDATE assignment
SET title = '事务与并发练习',
    content = '请结合课堂内容说明事务隔离级别与幂等处理，并给出一个业务示例。'
WHERE id = (
    SELECT id FROM (
        SELECT id FROM assignment
        WHERE class_id = @class_id AND teacher_id = @teacher_id
        ORDER BY id LIMIT 1
    ) demo_assignment
);
SET @assignment_id = (
    SELECT id FROM assignment
    WHERE class_id = @class_id AND teacher_id = @teacher_id
    ORDER BY id LIMIT 1
);
UPDATE assignment_submission
SET content = '事务保证多条写入一起提交；幂等键可以阻止重复请求重复创建业务结果。'
WHERE assignment_id = @assignment_id AND student_id = @student_id;
UPDATE notification
SET title = '欢迎来到 EduCore',
    content = '演示数据已准备好。你可以查看课程进度、作业、考试和通知。'
WHERE user_id = @student_id AND source_type = 'DEMO';

UPDATE question
SET content = '以下哪项最适合防止同一支付请求被重复记账？',
    options_json = '[{"id":"A","text":"消费幂等键"},{"id":"B","text":"延长页面加载时间"}]'
WHERE id = (
    SELECT id FROM (
        SELECT id FROM question
        WHERE course_id = @course_id AND teacher_id = @teacher_id
        ORDER BY id LIMIT 1
    ) demo_question
);
SET @question_id = (
    SELECT id FROM question
    WHERE course_id = @course_id AND teacher_id = @teacher_id
    ORDER BY id LIMIT 1
);

UPDATE exam
SET title = CASE status
    WHEN 'CLOSED' THEN '阶段测验 · 已完成'
    ELSE '阶段测验 · 即将开始'
END
WHERE class_id = @class_id AND teacher_id = @teacher_id
ORDER BY id
LIMIT 2;

UPDATE exam_question eq
JOIN exam e ON e.id = eq.exam_id
JOIN question q ON q.id = eq.source_question_id
SET eq.content_snapshot = q.content,
    eq.options_snapshot = q.options_json
WHERE e.class_id = @class_id
  AND e.teacher_id = @teacher_id
  AND q.id = @question_id;
