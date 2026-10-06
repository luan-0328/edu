SET time_zone = '+00:00';

-- Local demo accounts all use the documented demo password. Do not use these credentials outside a demo instance.
INSERT INTO sys_user (username, password_hash, real_name, role, status) VALUES
('demo-admin', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '演示管理员', 'ADMIN', 'ACTIVE'),
('demo-teacher', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '林老师', 'TEACHER', 'ACTIVE'),
('demo-student', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '陈同学', 'STUDENT', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), real_name=VALUES(real_name), role=VALUES(role), status=VALUES(status);

SET @teacher_id = (SELECT id FROM sys_user WHERE username='demo-teacher');
SET @student_id = (SELECT id FROM sys_user WHERE username='demo-student');

INSERT INTO course (name, description, price, status)
SELECT 'Java 后端工程实践', '从 Java 基础、Spring Boot 到关系数据库与并发事务的完整训练。', 899.00, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='Java 后端工程实践');
SET @course_id = (SELECT id FROM course WHERE name='Java 后端工程实践' ORDER BY id LIMIT 1);

INSERT INTO classroom (name, capacity, status)
VALUES ('演示教室 A', 32, 'ACTIVE')
ON DUPLICATE KEY UPDATE capacity=VALUES(capacity), status='ACTIVE';
SET @classroom_id = (SELECT id FROM classroom WHERE name='演示教室 A');

INSERT INTO edu_class (course_id, teacher_id, name, capacity, reserved_count, enrolled_count, start_date, end_date, status)
SELECT @course_id, @teacher_id, 'Java 后端实战班', 30, 0, 0, DATE_SUB(UTC_DATE(), INTERVAL 14 DAY), DATE_ADD(UTC_DATE(), INTERVAL 45 DAY), 'IN_PROGRESS'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE name='Java 后端实战班' AND course_id=@course_id);
SET @class_id = (SELECT id FROM edu_class WHERE name='Java 后端实战班' AND course_id=@course_id ORDER BY id LIMIT 1);

INSERT INTO class_student (class_id, student_id, status, enrolled_at)
VALUES (@class_id, @student_id, 'ENROLLED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 DAY))
ON DUPLICATE KEY UPDATE status='ENROLLED';
UPDATE edu_class SET enrolled_count=(SELECT COUNT(*) FROM class_student WHERE class_id=@class_id AND status='ENROLLED'), reserved_count=(SELECT COUNT(*) FROM enrollment_order WHERE class_id=@class_id AND status='PENDING') WHERE id=@class_id;

INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
VALUES ('DEMO-PAID-0001', @student_id, @class_id, 899.00, 'PAID', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 11 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 DAY))
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @order_id = (SELECT id FROM enrollment_order WHERE order_no='DEMO-PAID-0001');
INSERT INTO payment_record (payment_no, order_id, amount, status, paid_at)
SELECT 'DEMO-PAYMENT-0001', @order_id, 899.00, 'SUCCESS', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 DAY)
WHERE NOT EXISTS (SELECT 1 FROM payment_record WHERE order_id=@order_id);

INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_id, @teacher_id, @classroom_id, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY) + INTERVAL 90 MINUTE, 'COMPLETED'
WHERE NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_id AND status='COMPLETED');
INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_id, @teacher_id, @classroom_id, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 1 DAY), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 1 DAY) + INTERVAL 90 MINUTE, 'SCHEDULED'
WHERE NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_id AND start_time > UTC_TIMESTAMP(3) AND status='SCHEDULED');
SET @completed_schedule_id = (SELECT id FROM class_schedule WHERE class_id=@class_id AND status='COMPLETED' ORDER BY id LIMIT 1);
INSERT INTO attendance (schedule_id, student_id, status, checked_at)
VALUES (@completed_schedule_id, @student_id, 'PRESENT', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE status='PRESENT';

INSERT INTO assignment (class_id, teacher_id, title, content, deadline, status)
SELECT @class_id, @teacher_id, '事务与并发练习', '请结合课堂内容说明事务隔离级别与幂等处理，并给出一个业务示例。', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 3 DAY), 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM assignment WHERE class_id=@class_id AND title='事务与并发练习');
SET @assignment_id = (SELECT id FROM assignment WHERE class_id=@class_id AND title='事务与并发练习' ORDER BY id LIMIT 1);
INSERT INTO assignment_submission (assignment_id, student_id, content, status, submitted_at)
VALUES (@assignment_id, @student_id, '事务保证多条写入一起提交；幂等键可以阻止重复请求重复创建业务结果。', 'SUBMITTED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
INSERT INTO notification (user_id, source_type, source_id, title, content)
VALUES (@student_id, 'DEMO', @assignment_id, '欢迎来到 EduCore', '演示数据已准备好。你可以查看课程进度、作业、考试和通知。')
ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content);

INSERT INTO question (course_id, teacher_id, type, content, options_json, answer_json, difficulty, status)
SELECT @course_id, @teacher_id, 'SINGLE_CHOICE', '以下哪项最适合防止同一支付请求被重复记账？', '[{"id":"A","text":"消费幂等键"},{"id":"B","text":"延长页面加载时间"}]', '"A"', 'EASY', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE teacher_id=@teacher_id AND content='以下哪项最适合防止同一支付请求被重复记账？');
SET @question_id = (SELECT id FROM question WHERE teacher_id=@teacher_id AND content='以下哪项最适合防止同一支付请求被重复记账？' ORDER BY id LIMIT 1);

INSERT INTO exam (class_id, teacher_id, title, start_time, end_time, duration_minutes, status)
SELECT @class_id, @teacher_id, '阶段测验 · 已完成', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 8 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 8 DAY) + INTERVAL 2 HOUR, 60, 'CLOSED'
WHERE NOT EXISTS (SELECT 1 FROM exam WHERE class_id=@class_id AND title='阶段测验 · 已完成');
SET @past_exam_id = (SELECT id FROM exam WHERE class_id=@class_id AND title='阶段测验 · 已完成' ORDER BY id LIMIT 1);
INSERT INTO exam_question (exam_id, source_question_id, type, content_snapshot, options_snapshot, answer_snapshot, difficulty, score, sort_order)
SELECT @past_exam_id, @question_id, 'SINGLE_CHOICE', '以下哪项最适合防止同一支付请求被重复记账？', '[{"id":"A","text":"消费幂等键"},{"id":"B","text":"延长页面加载时间"}]', '"A"', 'EASY', 100.00, 1
WHERE NOT EXISTS (SELECT 1 FROM exam_question WHERE exam_id=@past_exam_id AND source_question_id=@question_id);
INSERT INTO exam_attempt (exam_id, student_id, status, started_at, submitted_at, score)
VALUES (@past_exam_id, @student_id, 'GRADED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 8 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 8 DAY) + INTERVAL 40 MINUTE, 100.00)
ON DUPLICATE KEY UPDATE score=VALUES(score), status='GRADED';
SET @attempt_id = (SELECT id FROM exam_attempt WHERE exam_id=@past_exam_id AND student_id=@student_id);
SET @exam_question_id = (SELECT id FROM exam_question WHERE exam_id=@past_exam_id AND source_question_id=@question_id);
INSERT INTO exam_answer (attempt_id, exam_question_id, answer_json, score, graded_at)
VALUES (@attempt_id, @exam_question_id, '"A"', 100.00, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 8 DAY) + INTERVAL 41 MINUTE)
ON DUPLICATE KEY UPDATE answer_json=VALUES(answer_json), score=VALUES(score);

INSERT INTO exam (class_id, teacher_id, title, start_time, end_time, duration_minutes, status)
SELECT @class_id, @teacher_id, '阶段测验 · 即将开始', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY) + INTERVAL 2 HOUR, 60, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM exam WHERE class_id=@class_id AND title='阶段测验 · 即将开始');
SET @upcoming_exam_id = (SELECT id FROM exam WHERE class_id=@class_id AND title='阶段测验 · 即将开始' ORDER BY id LIMIT 1);
INSERT INTO exam_question (exam_id, source_question_id, type, content_snapshot, options_snapshot, answer_snapshot, difficulty, score, sort_order)
SELECT @upcoming_exam_id, @question_id, 'SINGLE_CHOICE', '以下哪项最适合防止同一支付请求被重复记账？', '[{"id":"A","text":"消费幂等键"},{"id":"B","text":"延长页面加载时间"}]', '"A"', 'EASY', 100.00, 1
WHERE NOT EXISTS (SELECT 1 FROM exam_question WHERE exam_id=@upcoming_exam_id AND source_question_id=@question_id);
