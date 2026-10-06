SET NAMES utf8mb4;
SET time_zone = '+00:00';
START TRANSACTION;

-- Local demo shortcuts only. These credentials are intentionally
-- simple and must never be used for a production installation.
-- Password for all three accounts: LocalDemo2026!
-- Use INSERT IGNORE so a public registration race cannot promote a student
-- account to administrator if one of these usernames was claimed already.
INSERT IGNORE INTO sys_user (username, password_hash, real_name, role, status) VALUES
('admin',   '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '展示管理员', 'ADMIN',   'ACTIVE'),
('teacher', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '展示教师',   'TEACHER', 'ACTIVE'),
('student', '$2a$10$Ol2J.Ct09VlOi7LdWw3iYeEeNdhxBkhcoH50tmwnAn7zLvjXZV.tK', '展示学生',   'STUDENT', 'ACTIVE');

SET @course_id = (SELECT id FROM course WHERE name='Java 后端工程实践' ORDER BY id LIMIT 1);
SET @teacher_id = (SELECT id FROM sys_user WHERE username='teacher' AND role='TEACHER' ORDER BY id LIMIT 1);
SET @student_id = (SELECT id FROM sys_user WHERE username='student' AND role='STUDENT' ORDER BY id LIMIT 1);

INSERT IGNORE INTO classroom (name, capacity, status)
VALUES ('快捷演示教室', 20, 'ACTIVE');
SET @room_id = (SELECT id FROM classroom WHERE name='快捷演示教室' ORDER BY id LIMIT 1);

INSERT INTO edu_class (course_id, teacher_id, name, capacity, reserved_count, enrolled_count, start_date, end_date, status)
SELECT @course_id, @teacher_id, '快捷账号体验班', 10, 0, 0, UTC_DATE(), DATE_ADD(UTC_DATE(), INTERVAL 60 DAY), 'IN_PROGRESS'
WHERE @course_id IS NOT NULL AND @teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_class WHERE course_id=@course_id AND name='快捷账号体验班');
SET @class_id = (SELECT id FROM edu_class WHERE course_id=@course_id AND name='快捷账号体验班' ORDER BY id LIMIT 1);

INSERT IGNORE INTO class_student (class_id, student_id, status, enrolled_at)
SELECT @class_id, @student_id, 'ENROLLED', UTC_TIMESTAMP(3)
WHERE @class_id IS NOT NULL AND @student_id IS NOT NULL;

INSERT IGNORE INTO enrollment_order (order_no, student_id, class_id, amount, status, expire_at, paid_at)
SELECT 'SHOWCASE-STUDENT-001', @student_id, @class_id, c.price, 'PAID', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM course c
WHERE c.id=@course_id AND @class_id IS NOT NULL AND @student_id IS NOT NULL;
SET @order_id = (SELECT id FROM enrollment_order WHERE order_no='SHOWCASE-STUDENT-001');
INSERT IGNORE INTO payment_record (payment_no, order_id, amount, status, paid_at)
SELECT 'SHOWCASE-PAYMENT-001', id, amount, 'SUCCESS', paid_at
FROM enrollment_order WHERE id=@order_id AND @order_id IS NOT NULL;

UPDATE edu_class ec
SET enrolled_count=(SELECT COUNT(*) FROM class_student cs WHERE cs.class_id=ec.id AND cs.status='ENROLLED'),
    reserved_count=(SELECT COUNT(*) FROM enrollment_order eo WHERE eo.class_id=ec.id AND eo.status='PENDING')
WHERE ec.id=@class_id AND @class_id IS NOT NULL;

INSERT INTO class_schedule (class_id, teacher_id, classroom_id, start_time, end_time, status)
SELECT @class_id, @teacher_id, @room_id,
       TIMESTAMP(DATE_ADD(UTC_DATE(), INTERVAL 1 DAY), '10:00:00'),
       TIMESTAMP(DATE_ADD(UTC_DATE(), INTERVAL 1 DAY), '11:30:00'), 'SCHEDULED'
WHERE @class_id IS NOT NULL AND @teacher_id IS NOT NULL AND @room_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM class_schedule WHERE class_id=@class_id AND status='SCHEDULED' AND end_time>UTC_TIMESTAMP(3));

INSERT INTO assignment (class_id, teacher_id, title, content, deadline, status)
SELECT @class_id, @teacher_id, '入门课后练习', '请用自己的话说明：Java 中事务为什么需要提交或回滚？', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY), 'PUBLISHED'
WHERE @class_id IS NOT NULL AND @teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM assignment WHERE class_id=@class_id AND title='入门课后练习');
SET @assignment_id = (SELECT id FROM assignment WHERE class_id=@class_id AND title='入门课后练习' ORDER BY id LIMIT 1);

INSERT IGNORE INTO assignment_submission (assignment_id, student_id, content, status, submitted_at)
SELECT @assignment_id, @student_id, '演示作答：事务保证一组数据库操作作为整体提交；发生异常时回滚，避免只完成一半。', 'SUBMITTED', UTC_TIMESTAMP(3)
WHERE @assignment_id IS NOT NULL AND @student_id IS NOT NULL;

COMMIT;
