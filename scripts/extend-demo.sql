SET NAMES utf8mb4;
SET time_zone = '+00:00';
START TRANSACTION;

-- Additional isolated demo identities. They all use the documented demo password.
INSERT INTO sys_user (username, password_hash, real_name, role, status) VALUES
('demo-teacher-02', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '王老师', 'TEACHER', 'ACTIVE'),
('demo-teacher-03', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '周老师', 'TEACHER', 'ACTIVE'),
('demo-student-02', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '赵明', 'STUDENT', 'ACTIVE'),
('demo-student-03', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '周雨', 'STUDENT', 'ACTIVE'),
('demo-student-04', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '王一诺', 'STUDENT', 'ACTIVE'),
('demo-student-05', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '刘晨', 'STUDENT', 'ACTIVE'),
('demo-student-06', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '杨帆', 'STUDENT', 'ACTIVE'),
('demo-student-07', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '徐佳', 'STUDENT', 'ACTIVE'),
('demo-student-08', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '孙浩', 'STUDENT', 'ACTIVE'),
('demo-student-09', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '郑可', 'STUDENT', 'ACTIVE'),
('demo-student-10', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '吴桐', 'STUDENT', 'ACTIVE'),
('demo-student-11', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '许安', 'STUDENT', 'ACTIVE'),
('demo-student-12', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '蔡文', 'STUDENT', 'ACTIVE'),
('demo-student-13', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '唐可', 'STUDENT', 'ACTIVE'),
('demo-student-14', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '顾言', 'STUDENT', 'ACTIVE'),
('demo-student-15', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '沈悦', 'STUDENT', 'ACTIVE'),
('demo-student-16', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '蒋宁', 'STUDENT', 'ACTIVE'),
('demo-student-17', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '许清', 'STUDENT', 'ACTIVE'),
('demo-student-18', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '贺然', 'STUDENT', 'ACTIVE'),
('demo-student-19', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '唐希', 'STUDENT', 'ACTIVE'),
('demo-student-20', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '邵云', 'STUDENT', 'ACTIVE'),
('demo-student-21', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '陆川', 'STUDENT', 'ACTIVE'),
('demo-student-22', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '叶舟', 'STUDENT', 'ACTIVE'),
('demo-student-23', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '方宁', 'STUDENT', 'ACTIVE'),
('demo-student-24', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '陈默', 'STUDENT', 'ACTIVE'),
('demo-student-25', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '苏禾', 'STUDENT', 'ACTIVE'),
('demo-student-26', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '韩立', 'STUDENT', 'ACTIVE'),
('demo-student-27', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '杜若', 'STUDENT', 'ACTIVE'),
('demo-student-28', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '罗安', 'STUDENT', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), real_name=VALUES(real_name), role=VALUES(role), status='ACTIVE';

SET @teacher_java = (SELECT id FROM sys_user WHERE username='demo-teacher');
SET @teacher_python = (SELECT id FROM sys_user WHERE username='demo-teacher-02');
SET @teacher_english = (SELECT id FROM sys_user WHERE username='demo-teacher-03');
SET @student_pending = (SELECT id FROM sys_user WHERE username='demo-student-07');
SET @student_cancelled = (SELECT id FROM sys_user WHERE username='demo-student-13');

INSERT INTO course (name, description, price, status)
SELECT 'Python 数据分析入门', '使用 Python、Pandas 和可视化工具完成数据清洗与分析练习。', 699.00, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='Python 数据分析入门');
INSERT INTO course (name, description, price, status)
SELECT '英语口语沟通进阶', '围绕日常表达、情景对话和发音练习提升英语沟通能力。', 599.00, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='英语口语沟通进阶');

SET @course_java = (SELECT id FROM course WHERE name='Java 后端工程实践' ORDER BY id LIMIT 1);
SET @course_python = (SELECT id FROM course WHERE name='Python 数据分析入门' ORDER BY id LIMIT 1);
SET @course_english = (SELECT id FROM course WHERE name='英语口语沟通进阶' ORDER BY id LIMIT 1);
SET @price_java = (SELECT price FROM course WHERE id=@course_java);
SET @price_python = (SELECT price FROM course WHERE id=@course_python);
SET @price_english = (SELECT price FROM course WHERE id=@course_english);

INSERT INTO classroom (name, capacity, status) VALUES
('演示教室 B', 30, 'ACTIVE'), ('演示教室 C', 24, 'ACTIVE')
ON DUPLICATE KEY UPDATE capacity=VALUES(capacity), status='ACTIVE';
SET @room_a = (SELECT id FROM classroom WHERE name='演示教室 A');
SET @room_b = (SELECT id FROM classroom WHERE name='演示教室 B');
SET @room_c = (SELECT id FROM classroom WHERE name='演示教室 C');

INSERT INTO edu_class (course_id, teacher_id, name, capacity, reserved_count, enrolled_count, start_date, end_date, status)
SELECT @course_java, @teacher_java, 'Java 应用开发晚班', 25, 0, 0,
       DATE_ADD(UTC_DATE(), INTERVAL 14 DAY), DATE_ADD(UTC_DATE(), INTERVAL 104 DAY), 'ENROLLING'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE course_id=@course_java AND name='Java 应用开发晚班');
INSERT INTO edu_class (course_id, teacher_id, name, capacity, reserved_count, enrolled_count, start_date, end_date, status)
SELECT @course_python, @teacher_python, 'Python 数据分析周末班', 20, 0, 0,
       DATE_SUB(UTC_DATE(), INTERVAL 30 DAY), DATE_ADD(UTC_DATE(), INTERVAL 60 DAY), 'IN_PROGRESS'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE course_id=@course_python AND name='Python 数据分析周末班');
INSERT INTO edu_class (course_id, teacher_id, name, capacity, reserved_count, enrolled_count, start_date, end_date, status)
SELECT @course_english, @teacher_english, '英语口语晨班', 18, 0, 0,
       DATE_ADD(UTC_DATE(), INTERVAL 7 DAY), DATE_ADD(UTC_DATE(), INTERVAL 97 DAY), 'ENROLLING'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE course_id=@course_english AND name='英语口语晨班');

SET @class_java = (SELECT id FROM edu_class WHERE course_id=@course_java AND name='Java 后端实战班' ORDER BY id LIMIT 1);
SET @class_java_open = (SELECT id FROM edu_class WHERE course_id=@course_java AND name='Java 应用开发晚班' ORDER BY id LIMIT 1);
SET @class_python = (SELECT id FROM edu_class WHERE course_id=@course_python AND name='Python 数据分析周末班' ORDER BY id LIMIT 1);
SET @class_english = (SELECT id FROM edu_class WHERE course_id=@course_english AND name='英语口语晨班' ORDER BY id LIMIT 1);

-- Paid rosters cover an active Java cohort, a Python cohort and the new English cohort.
INSERT INTO class_student (class_id, student_id, status, enrolled_at)
SELECT @class_java, id, 'ENROLLED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 8 DAY)
FROM sys_user WHERE username IN ('demo-student-02','demo-student-03','demo-student-04','demo-student-05')
ON DUPLICATE KEY UPDATE status='ENROLLED';
INSERT INTO class_student (class_id, student_id, status, enrolled_at)
SELECT @class_java, id, 'ENROLLED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 6 DAY)
FROM sys_user WHERE username BETWEEN 'demo-student-14' AND 'demo-student-28'
ON DUPLICATE KEY UPDATE status='ENROLLED';
INSERT INTO class_student (class_id, student_id, status, enrolled_at)
SELECT @class_java_open, id, 'ENROLLED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY)
FROM sys_user WHERE username='demo-student-06'
ON DUPLICATE KEY UPDATE status='ENROLLED';
INSERT INTO class_student (class_id, student_id, status, enrolled_at)
SELECT @class_python, id, 'ENROLLED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 18 DAY)
FROM sys_user WHERE username IN ('demo-student-08','demo-student-09','demo-student-10')
ON DUPLICATE KEY UPDATE status='ENROLLED';
INSERT INTO class_student (class_id, student_id, status, enrolled_at)
SELECT @class_english, id, 'ENROLLED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY)
FROM sys_user WHERE username IN ('demo-student-11','demo-student-12')
ON DUPLICATE KEY UPDATE status='ENROLLED';

-- Matching simulated paid orders and payment rows keep enrollment totals coherent.
INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
SELECT CONCAT('DEMO-PAID-JAVA-', SUBSTRING_INDEX(username,'-',-1)), id, @class_java, @price_java, 'PAID',
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 11 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 DAY)
FROM sys_user WHERE username IN ('demo-student-02','demo-student-03','demo-student-04','demo-student-05')
ON DUPLICATE KEY UPDATE amount=VALUES(amount), status='PAID', paid_at=VALUES(paid_at);
INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
SELECT CONCAT('DEMO-PAID-JAVA-', SUBSTRING_INDEX(username,'-',-1)), id, @class_java, @price_java, 'PAID',
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 6 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 6 DAY)
FROM sys_user WHERE username BETWEEN 'demo-student-14' AND 'demo-student-28'
ON DUPLICATE KEY UPDATE amount=VALUES(amount), status='PAID', paid_at=VALUES(paid_at);
INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
SELECT CONCAT('DEMO-PAID-OPEN-', SUBSTRING_INDEX(username,'-',-1)), id, @class_java_open, @price_java, 'PAID',
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY)
FROM sys_user WHERE username='demo-student-06'
ON DUPLICATE KEY UPDATE amount=VALUES(amount), status='PAID', paid_at=VALUES(paid_at);
INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
SELECT CONCAT('DEMO-PAID-PY-', SUBSTRING_INDEX(username,'-',-1)), id, @class_python, @price_python, 'PAID',
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 17 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 18 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 18 DAY)
FROM sys_user WHERE username IN ('demo-student-08','demo-student-09','demo-student-10')
ON DUPLICATE KEY UPDATE amount=VALUES(amount), status='PAID', paid_at=VALUES(paid_at);
INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
SELECT CONCAT('DEMO-PAID-ENG-', SUBSTRING_INDEX(username,'-',-1)), id, @class_english, @price_english, 'PAID',
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY)
FROM sys_user WHERE username IN ('demo-student-11','demo-student-12')
ON DUPLICATE KEY UPDATE amount=VALUES(amount), status='PAID', paid_at=VALUES(paid_at);

INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
VALUES ('DEMO-PENDING-0007', @student_pending, @class_java_open, @price_java, 'PENDING',
        DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 1 DAY), NULL, UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(enrollment_order.id);
INSERT INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at, created_at)
VALUES ('DEMO-CANCELLED-0013', @student_cancelled, @class_java_open, @price_java, 'CANCELLED',
        DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY), NULL, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(enrollment_order.id);

INSERT INTO payment_record (payment_no, order_id, amount, status, paid_at)
SELECT CONCAT('PAY-', eo.order_no), eo.id, eo.amount, 'SUCCESS', eo.paid_at
FROM enrollment_order eo
WHERE eo.order_no LIKE 'DEMO-PAID-%'
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(payment_record.id);

UPDATE edu_class c
SET enrolled_count=(SELECT COUNT(*) FROM class_student cs WHERE cs.class_id=c.id AND cs.status='ENROLLED'),
    reserved_count=(SELECT COUNT(*) FROM enrollment_order eo WHERE eo.class_id=c.id AND eo.status='PENDING')
WHERE c.id IN (@class_java, @class_java_open, @class_python, @class_english);

-- Distinct room/time allocations include completed lessons for attendance and future lessons for calendars.
INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_python, @teacher_python, @room_b, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 3 DAY) + INTERVAL 90 MINUTE, 'COMPLETED'
WHERE NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_python AND status='COMPLETED');
INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_python, @teacher_python, @room_b, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY) + INTERVAL 90 MINUTE, 'SCHEDULED'
WHERE NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_python AND status='SCHEDULED' AND start_time > UTC_TIMESTAMP(3));
INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_english, @teacher_english, @room_c, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 8 DAY), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 8 DAY) + INTERVAL 90 MINUTE, 'SCHEDULED'
WHERE NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_english AND status='SCHEDULED' AND start_time > UTC_TIMESTAMP(3));
INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_java_open, @teacher_java, @room_a, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 15 DAY), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 15 DAY) + INTERVAL 90 MINUTE, 'SCHEDULED'
WHERE NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_java_open AND status='SCHEDULED' AND start_time > UTC_TIMESTAMP(3));

SET @java_completed_schedule = (SELECT id FROM class_schedule WHERE class_id=@class_java AND status='COMPLETED' ORDER BY id LIMIT 1);
SET @python_completed_schedule = (SELECT id FROM class_schedule WHERE class_id=@class_python AND status='COMPLETED' ORDER BY id LIMIT 1);
INSERT INTO attendance (schedule_id, student_id, status, checked_at)
SELECT s.id, cs.student_id,
       CASE MOD(cs.student_id, 5) WHEN 0 THEN 'ABSENT' WHEN 1 THEN 'LATE' ELSE 'PRESENT' END,
       s.start_time
FROM class_schedule s
JOIN class_student cs ON cs.class_id=s.class_id AND cs.status='ENROLLED'
WHERE s.id IN (@java_completed_schedule, @python_completed_schedule)
ON DUPLICATE KEY UPDATE status=VALUES(status), checked_at=VALUES(checked_at);

-- Published and unsubmitted work is left for the learner demo account to act on.
INSERT INTO assignment (class_id, teacher_id, title, content, deadline, status)
SELECT @class_python, @teacher_python, 'Pandas 数据清洗练习', '整理给定数据集，处理缺失值并提交关键分析步骤。', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 5 DAY), 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM assignment WHERE class_id=@class_python AND title='Pandas 数据清洗练习');
SET @assignment_python = (SELECT id FROM assignment WHERE class_id=@class_python AND title='Pandas 数据清洗练习' ORDER BY id LIMIT 1);
INSERT INTO assignment_submission (assignment_id, student_id, content, status, score, feedback, submitted_at, graded_at)
SELECT @assignment_python, u.id,
       CONCAT('已完成数据清洗练习，提交人：', u.real_name),
       CASE u.username WHEN 'demo-student-10' THEN 'SUBMITTED' ELSE 'GRADED' END,
       CASE u.username WHEN 'demo-student-08' THEN 92.00 WHEN 'demo-student-09' THEN 78.00 ELSE NULL END,
       CASE u.username WHEN 'demo-student-08' THEN '分析过程清晰，继续补充边界数据说明。' WHEN 'demo-student-09' THEN '结果正确，可以进一步解释处理依据。' ELSE NULL END,
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY),
       CASE WHEN u.username='demo-student-10' THEN NULL ELSE DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 12 HOUR) END
FROM sys_user u WHERE u.username IN ('demo-student-08','demo-student-09','demo-student-10')
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(assignment_submission.id);

INSERT INTO assignment (class_id, teacher_id, title, content, deadline, status)
SELECT @class_english, @teacher_english, '情景对话录入练习', '选择一个日常场景，整理不少于八句英语对话。', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 10 DAY), 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM assignment WHERE class_id=@class_english AND title='情景对话录入练习');
SET @assignment_english = (SELECT id FROM assignment WHERE class_id=@class_english AND title='情景对话录入练习' ORDER BY id LIMIT 1);
INSERT INTO assignment_submission (assignment_id, student_id, content, status, score, feedback, submitted_at, graded_at)
SELECT @assignment_english, u.id, '已整理机场问路场景对话。', 'GRADED', 88.00,
       '表达清楚，注意时态和礼貌用语。', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 6 HOUR), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 HOUR)
FROM sys_user u WHERE u.username='demo-student-11'
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(assignment_submission.id);

-- Python exam records exercise question snapshots, objective grading and score history.
INSERT INTO question (course_id, teacher_id, type, content, options_json, answer_json, difficulty, status)
SELECT @course_python, @teacher_python, 'SINGLE_CHOICE', 'Python 中 list[1:3] 会取出几个元素？',
       '[{"id":"A","text":"1 个"},{"id":"B","text":"2 个"},{"id":"C","text":"3 个"}]', '"B"', 'EASY', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE course_id=@course_python AND teacher_id=@teacher_python AND content='Python 中 list[1:3] 会取出几个元素？');
INSERT INTO question (course_id, teacher_id, type, content, options_json, answer_json, difficulty, status)
SELECT @course_python, @teacher_python, 'SHORT_ANSWER', '说明 Pandas 处理缺失值的一种方式。', NULL, '"由教师根据答案合理性评分"', 'MEDIUM', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE course_id=@course_python AND teacher_id=@teacher_python AND content='说明 Pandas 处理缺失值的一种方式。');
SET @question_python_choice = (SELECT id FROM question WHERE course_id=@course_python AND teacher_id=@teacher_python AND content='Python 中 list[1:3] 会取出几个元素？' ORDER BY id LIMIT 1);
SET @question_python_text = (SELECT id FROM question WHERE course_id=@course_python AND teacher_id=@teacher_python AND content='说明 Pandas 处理缺失值的一种方式。' ORDER BY id LIMIT 1);

INSERT INTO exam (class_id, teacher_id, title, start_time, end_time, duration_minutes, status)
SELECT @class_python, @teacher_python, 'Python 阶段测验（已结束）', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 DAY) + INTERVAL 2 HOUR, 60, 'CLOSED'
WHERE NOT EXISTS (SELECT 1 FROM exam WHERE class_id=@class_python AND title='Python 阶段测验（已结束）');
INSERT INTO exam (class_id, teacher_id, title, start_time, end_time, duration_minutes, status)
SELECT @class_python, @teacher_python, 'Python 单元测验（即将开始）', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY) + INTERVAL 2 HOUR, 60, 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM exam WHERE class_id=@class_python AND title='Python 单元测验（即将开始）');
SET @exam_python_past = (SELECT id FROM exam WHERE class_id=@class_python AND title='Python 阶段测验（已结束）' ORDER BY id LIMIT 1);
SET @exam_python_upcoming = (SELECT id FROM exam WHERE class_id=@class_python AND title='Python 单元测验（即将开始）' ORDER BY id LIMIT 1);

INSERT INTO exam_question (exam_id, source_question_id, type, content_snapshot, options_snapshot, answer_snapshot, difficulty, score, sort_order)
SELECT @exam_python_past, q.id, q.type, q.content, q.options_json, q.answer_json, q.difficulty, IF(q.id=@question_python_choice,60.00,40.00), IF(q.id=@question_python_choice,1,2)
FROM question q
WHERE q.id IN (@question_python_choice,@question_python_text)
  AND NOT EXISTS (SELECT 1 FROM exam_question eq WHERE eq.exam_id=@exam_python_past AND eq.source_question_id=q.id);
INSERT INTO exam_question (exam_id, source_question_id, type, content_snapshot, options_snapshot, answer_snapshot, difficulty, score, sort_order)
SELECT @exam_python_upcoming, q.id, q.type, q.content, q.options_json, q.answer_json, q.difficulty, IF(q.id=@question_python_choice,60.00,40.00), IF(q.id=@question_python_choice,1,2)
FROM question q
WHERE q.id IN (@question_python_choice,@question_python_text)
  AND NOT EXISTS (SELECT 1 FROM exam_question eq WHERE eq.exam_id=@exam_python_upcoming AND eq.source_question_id=q.id);

INSERT INTO exam_attempt (exam_id, student_id, status, started_at, submitted_at, score)
SELECT @exam_python_past, u.id, 'GRADED', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 DAY), DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 DAY) + INTERVAL 50 MINUTE,
       CASE u.username WHEN 'demo-student-08' THEN 100.00 WHEN 'demo-student-09' THEN 60.00 ELSE 40.00 END
FROM sys_user u WHERE u.username IN ('demo-student-08','demo-student-09','demo-student-10')
ON DUPLICATE KEY UPDATE status='GRADED', score=VALUES(score), submitted_at=VALUES(submitted_at);

INSERT INTO exam_answer (attempt_id, exam_question_id, answer_json, score, feedback, graded_at)
SELECT ea.id, eq.id,
       CASE WHEN eq.source_question_id=@question_python_choice
            THEN CASE WHEN u.username='demo-student-10' THEN '"A"' ELSE '"B"' END
            ELSE CASE WHEN u.username='demo-student-09' THEN NULL ELSE '"缺失值可以使用 fillna 填充，或通过 dropna 删除。"' END END,
       CASE WHEN eq.source_question_id=@question_python_choice
            THEN CASE WHEN u.username='demo-student-10' THEN 0.00 ELSE 60.00 END
            ELSE CASE WHEN u.username='demo-student-09' THEN 0.00 ELSE 40.00 END END,
       CASE WHEN eq.source_question_id=@question_python_choice THEN '客观题自动评分' ELSE '教师已完成简答题评分' END,
       DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 4 DAY)
FROM exam_attempt ea
JOIN sys_user u ON u.id=ea.student_id
JOIN exam_question eq ON eq.exam_id=ea.exam_id
WHERE ea.exam_id=@exam_python_past
  AND u.username IN ('demo-student-08','demo-student-09','demo-student-10')
ON DUPLICATE KEY UPDATE answer_json=VALUES(answer_json), score=VALUES(score), feedback=VALUES(feedback), graded_at=VALUES(graded_at);

COMMIT;
