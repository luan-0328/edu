-- Public showcase for the literal admin / teacher / student accounts.
-- Repeat runs insert missing samples without overwriting visitor submissions.
SET NAMES utf8mb4;
SET time_zone = '+00:00';
START TRANSACTION;
SET @admin = (SELECT id FROM sys_user WHERE username='admin' AND role='ADMIN' AND status='ACTIVE');
SET @teacher = (SELECT id FROM sys_user WHERE username='teacher' AND role='TEACHER' AND status='ACTIVE');
SET @student = (SELECT id FROM sys_user WHERE username='student' AND role='STUDENT' AND status='ACTIVE');
CREATE TEMPORARY TABLE showcase_accounts (id BIGINT NOT NULL);
INSERT INTO showcase_accounts VALUES (@admin),(@teacher),(@student);
UPDATE sys_user SET real_name=CASE username WHEN 'admin' THEN '展示管理员' WHEN 'teacher' THEN '林老师' WHEN 'student' THEN '陈同学' END
WHERE id IN (@admin,@teacher,@student);

INSERT INTO course (name,description,price,status)
SELECT 'Java 后端工程实践','Java 基础、面向对象、集合与事务。已有教学班，可查看完整学习记录。',899,'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='Java 后端工程实践');
INSERT INTO course (name,description,price,status)
SELECT 'Spring Boot 项目实战','从接口设计到项目交付；开放报名，可体验下单和模拟支付。',1299,'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='Spring Boot 项目实战');
INSERT INTO course (name,description,price,status)
SELECT '数据库设计入门','学习表结构、关系设计和 SQL 查询；管理员可体验上架与开班。',499,'DRAFT'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='数据库设计入门');
SET @java=(SELECT id FROM course WHERE name='Java 后端工程实践' ORDER BY id LIMIT 1);
SET @spring=(SELECT id FROM course WHERE name='Spring Boot 项目实战' ORDER BY id LIMIT 1);
SET @sql=(SELECT id FROM course WHERE name='数据库设计入门' ORDER BY id LIMIT 1);
INSERT INTO classroom (name,capacity,status) VALUES ('实战教室 A',30,'ACTIVE'),('研讨教室 B',20,'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name);
SET @room=(SELECT id FROM classroom WHERE name='实战教室 A');

INSERT INTO edu_class (course_id,teacher_id,name,capacity,start_date,end_date,status)
SELECT @java,@teacher,'Java 实战一班',20,UTC_DATE()-INTERVAL 14 DAY,UTC_DATE()+INTERVAL 90 DAY,'IN_PROGRESS'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE name='Java 实战一班' AND course_id=@java);
INSERT INTO edu_class (course_id,teacher_id,name,capacity,start_date,end_date,status)
SELECT @java,@teacher,'Java 周末体验班',20,UTC_DATE()+INTERVAL 7 DAY,UTC_DATE()+INTERVAL 90 DAY,'ENROLLING'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE name='Java 周末体验班' AND course_id=@java);
INSERT INTO edu_class (course_id,teacher_id,name,capacity,start_date,end_date,status)
SELECT @spring,@teacher,'Spring Boot 新班',20,UTC_DATE()+INTERVAL 14 DAY,UTC_DATE()+INTERVAL 90 DAY,'ENROLLING'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE name='Spring Boot 新班' AND course_id=@spring);
INSERT INTO edu_class (course_id,teacher_id,name,capacity,start_date,end_date,status)
SELECT @sql,@teacher,'数据库入门筹备班',20,UTC_DATE()+INTERVAL 14 DAY,UTC_DATE()+INTERVAL 90 DAY,'DRAFT'
WHERE NOT EXISTS (SELECT 1 FROM edu_class WHERE name='数据库入门筹备班' AND course_id=@sql);
SET @class=(SELECT id FROM edu_class WHERE name='Java 实战一班' AND course_id=@java ORDER BY id LIMIT 1);
INSERT IGNORE INTO class_student (class_id,student_id,status,enrolled_at)
SELECT @class,id,'ENROLLED',UTC_TIMESTAMP(3)-INTERVAL 12 DAY FROM sys_user
WHERE id=@student OR (username IN ('demo-student-01','demo-student-02','demo-student-03','demo-student-04','demo-student-05') AND role='STUDENT' AND status='ACTIVE');
INSERT IGNORE INTO enrollment_order (order_no,student_id,class_id,amount,status,expire_at,paid_at,created_at)
SELECT CONCAT('HR-PAID-',cs.student_id),cs.student_id,@class,899,'PAID',cs.enrolled_at+INTERVAL 15 MINUTE,cs.enrolled_at,cs.enrolled_at
FROM class_student cs WHERE cs.class_id=@class;
INSERT IGNORE INTO payment_record (payment_no,order_id,amount,status,paid_at)
SELECT CONCAT('HR-PAY-',id),id,amount,'SUCCESS',paid_at FROM enrollment_order WHERE class_id=@class AND order_no LIKE 'HR-PAID-%';
UPDATE edu_class ec SET enrolled_count=(SELECT COUNT(*) FROM class_student WHERE class_id=ec.id AND status='ENROLLED'),
reserved_count=(SELECT COUNT(*) FROM enrollment_order WHERE class_id=ec.id AND status='PENDING') WHERE ec.id=@class;

INSERT INTO class_schedule (class_id,teacher_id,classroom_id,start_time,end_time,status)
SELECT @class,@teacher,@room,TIMESTAMP(UTC_DATE()-INTERVAL days_ago DAY,'01:00:00'),TIMESTAMP(UTC_DATE()-INTERVAL days_ago DAY,'02:30:00'),'COMPLETED'
FROM (SELECT 10 days_ago UNION ALL SELECT 3) d
WHERE NOT EXISTS (SELECT 1 FROM class_schedule s WHERE s.class_id=@class AND s.start_time=TIMESTAMP(UTC_DATE()-INTERVAL days_ago DAY,'01:00:00'));
INSERT INTO class_schedule (class_id,teacher_id,classroom_id,start_time,end_time,status)
SELECT @class,@teacher,@room,TIMESTAMP(UTC_DATE()+INTERVAL days_ahead DAY,'01:00:00'),TIMESTAMP(UTC_DATE()+INTERVAL days_ahead DAY,'02:30:00'),'SCHEDULED'
FROM (SELECT 0 days_ahead UNION ALL SELECT 1 UNION ALL SELECT 3 UNION ALL SELECT 7) d
WHERE NOT EXISTS (SELECT 1 FROM class_schedule s WHERE s.class_id=@class AND s.start_time=TIMESTAMP(UTC_DATE()+INTERVAL days_ahead DAY,'01:00:00'));
INSERT IGNORE INTO attendance (schedule_id,student_id,status,checked_at)
SELECT s.id,cs.student_id,CASE WHEN cs.student_id=@student THEN 'PRESENT' WHEN MOD(cs.student_id,4)=0 THEN 'LATE' ELSE 'PRESENT' END,s.end_time
FROM class_schedule s JOIN class_student cs ON cs.class_id=s.class_id WHERE s.class_id=@class AND s.status='COMPLETED';

INSERT INTO assignment (class_id,teacher_id,title,content,deadline,status)
SELECT @class,@teacher,d.title,d.content,UTC_TIMESTAMP(3)+INTERVAL d.days_left DAY,d.status
FROM (
 SELECT '集合与异常练习 · 待提交' title,'请说明 List 与 Set 的区别，并举一个处理异常的例子。' content,5 days_left,'PUBLISHED' status
 UNION ALL SELECT '事务场景分析 · 待批改','结合报名和支付场景，说明事务提交与回滚的作用。',7,'PUBLISHED'
 UNION ALL SELECT '面向对象练习 · 已批改','请解释封装、继承和多态，并给出应用示例。',10,'PUBLISHED'
 UNION ALL SELECT '接口设计练习 · 草稿','设计一个课程报名接口，写出输入、输出和错误处理。',14,'DRAFT'
) d WHERE NOT EXISTS (SELECT 1 FROM assignment a WHERE a.class_id=@class AND a.title=d.title);
INSERT IGNORE INTO assignment_submission (assignment_id,student_id,content,status,score,feedback,submitted_at,graded_at)
SELECT a.id,cs.student_id,
CASE WHEN a.title LIKE '面向对象%' THEN '封装隐藏内部状态，继承复用公共能力，多态使接口支持不同实现。例如不同支付方式实现统一支付接口。' ELSE '报名与支付需要整体提交，任何一步失败都回滚，避免扣款成功却没有入班。' END,
CASE WHEN a.title LIKE '面向对象%' THEN 'GRADED' ELSE 'SUBMITTED' END,
CASE WHEN a.title LIKE '面向对象%' THEN 92 ELSE NULL END,
CASE WHEN a.title LIKE '面向对象%' THEN '概念清晰，示例贴合业务。建议补充接口与抽象类的区别。' ELSE NULL END,
UTC_TIMESTAMP(3)-INTERVAL 1 DAY,CASE WHEN a.title LIKE '面向对象%' THEN UTC_TIMESTAMP(3)-INTERVAL 12 HOUR ELSE NULL END
FROM assignment a JOIN class_student cs ON cs.class_id=a.class_id
WHERE a.class_id=@class AND (a.title LIKE '事务场景%' OR a.title LIKE '面向对象%');

INSERT INTO notification (user_id,source_type,source_id,title,content)
SELECT @student,'ASSIGNMENT',a.id,CONCAT('新作业：',a.title),CONCAT('林老师发布了作业，请到“我的作业”查看：',a.title)
FROM assignment a WHERE a.class_id=@class AND a.status='PUBLISHED'
AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id=@student AND n.source_type='ASSIGNMENT' AND n.source_id=a.id);

INSERT INTO question (course_id,teacher_id,type,content,options_json,answer_json,difficulty,status)
SELECT @java,@teacher,d.type,d.content,d.options,d.answer,'EASY','ACTIVE' FROM (
 SELECT 'SINGLE_CHOICE' type,'哪种集合不允许重复元素？' content,'[{"id":"A","text":"List"},{"id":"B","text":"Set"},{"id":"C","text":"数组"},{"id":"D","text":"String"}]' options,'"B"' answer
 UNION ALL SELECT 'SINGLE_CHOICE','Java 中哪个关键字用于继承类？','[{"id":"A","text":"extends"},{"id":"B","text":"import"},{"id":"C","text":"new"},{"id":"D","text":"return"}]','"A"'
 UNION ALL SELECT 'SINGLE_CHOICE','捕获异常使用哪个关键字？','[{"id":"A","text":"throw"},{"id":"B","text":"catch"},{"id":"C","text":"class"},{"id":"D","text":"package"}]','"B"'
 UNION ALL SELECT 'SINGLE_CHOICE','事务失败后通常应该执行什么操作？','[{"id":"A","text":"提交"},{"id":"B","text":"忽略异常"},{"id":"C","text":"回滚"},{"id":"D","text":"重复扣款"}]','"C"'
 UNION ALL SELECT 'SINGLE_CHOICE','HTTP GET 通常用于什么操作？','[{"id":"A","text":"查询资源"},{"id":"B","text":"删除用户"},{"id":"C","text":"修改密码"},{"id":"D","text":"确认支付"}]','"A"'
 UNION ALL SELECT 'MULTIPLE_CHOICE','哪些属于 Java 面向对象的基本特征？','[{"id":"A","text":"封装"},{"id":"B","text":"继承"},{"id":"C","text":"多态"},{"id":"D","text":"排序"}]','["A","B","C"]'
 UNION ALL SELECT 'TRUE_FALSE','一个 Java 类可以直接继承多个类。',NULL,'false'
 UNION ALL SELECT 'SHORT_ANSWER','请解释为什么课程报名和支付需要事务。',NULL,'"事务使支付和入班作为整体提交，失败时回滚，保持业务数据一致。"'
) d WHERE NOT EXISTS (SELECT 1 FROM question q WHERE q.teacher_id=@teacher AND q.course_id=@java AND q.content=d.content);

INSERT INTO exam (class_id,teacher_id,title,start_time,end_time,duration_minutes,status)
SELECT @class,@teacher,d.title,UTC_TIMESTAMP(3)+INTERVAL d.start_days DAY,UTC_TIMESTAMP(3)+INTERVAL d.end_days DAY,60,d.status
FROM (
 SELECT '基础知识测验 · 历史成绩' title,-7 start_days,-6 end_days,'CLOSED' status
 UNION ALL SELECT '事务简答测验 · 待批改',-3,-2,'CLOSED'
 UNION ALL SELECT '在线体验测验 · 可立即参加',-1,60,'PUBLISHED'
 UNION ALL SELECT '综合阶段测验 · 即将开始',2,60,'PUBLISHED'
 UNION ALL SELECT '题库组卷体验 · 草稿',3,60,'DRAFT'
) d WHERE NOT EXISTS (SELECT 1 FROM exam e WHERE e.class_id=@class AND e.title=d.title);
-- Published and historical papers have fixed snapshots; draft is left for HR to generate.
INSERT INTO exam_question (exam_id,source_question_id,type,content_snapshot,options_snapshot,answer_snapshot,difficulty,score,sort_order)
SELECT e.id,q.id,q.type,q.content,q.options_json,q.answer_json,q.difficulty,
CASE WHEN e.title LIKE '事务简答%' THEN 100 ELSE 20 END,
ROW_NUMBER() OVER (PARTITION BY e.id ORDER BY q.id)
FROM exam e JOIN question q ON q.course_id=@java AND q.teacher_id=@teacher
WHERE e.class_id=@class AND e.status<>'DRAFT'
AND ((e.title LIKE '事务简答%' AND q.type='SHORT_ANSWER') OR (e.title NOT LIKE '事务简答%' AND q.type='SINGLE_CHOICE'))
AND NOT EXISTS (SELECT 1 FROM exam_question eq WHERE eq.exam_id=e.id);
SET @past=(SELECT id FROM exam WHERE class_id=@class AND title='基础知识测验 · 历史成绩');
SET @pending=(SELECT id FROM exam WHERE class_id=@class AND title='事务简答测验 · 待批改');
INSERT IGNORE INTO exam_attempt (exam_id,student_id,status,started_at,submitted_at,score)
SELECT @past,student_id,'GRADED',UTC_TIMESTAMP(3)-INTERVAL 7 DAY,UTC_TIMESTAMP(3)-INTERVAL 7 DAY+INTERVAL 30 MINUTE,
CASE WHEN student_id=@student THEN 100 ELSE 60+MOD(student_id,3)*20 END FROM class_student WHERE class_id=@class;
INSERT IGNORE INTO exam_answer (attempt_id,exam_question_id,answer_json,score,graded_at)
SELECT a.id,q.id,CASE WHEN q.sort_order<=a.score/20 THEN q.answer_snapshot ELSE '"D"' END,
CASE WHEN q.sort_order<=a.score/20 THEN 20 ELSE 0 END,a.submitted_at
FROM exam_attempt a JOIN exam_question q ON q.exam_id=a.exam_id WHERE a.exam_id=@past;
INSERT IGNORE INTO exam_attempt (exam_id,student_id,status,started_at,submitted_at,score)
VALUES (@pending,@student,'SUBMITTED',UTC_TIMESTAMP(3)-INTERVAL 3 DAY,UTC_TIMESTAMP(3)-INTERVAL 3 DAY+INTERVAL 30 MINUTE,NULL);
INSERT IGNORE INTO exam_answer (attempt_id,exam_question_id,answer_json)
SELECT a.id,q.id,'"事务可以保证扣款与入班同时成功，失败时全部回滚，防止重复扣款和状态不一致。"'
FROM exam_attempt a JOIN exam_question q ON q.exam_id=a.exam_id WHERE a.exam_id=@pending AND a.student_id=@student;
COMMIT;
DROP TEMPORARY TABLE showcase_accounts;
