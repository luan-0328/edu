# 数据库结构

目标数据库为 MySQL 8 / InnoDB / utf8mb4，主键为 `BIGINT`，业务时间使用 UTC `DATETIME(3)`。Flyway 迁移脚本位于 `src/main/resources/db/migration`，新增结构只通过版本迁移维护。

## V1：用户

`sys_user` 存储唯一用户名、BCrypt 密码哈希、真实姓名、`ADMIN/TEACHER/STUDENT` 角色和 `ACTIVE/DISABLED` 状态。

## V2：教学基础模型

- `course`：课程描述、价格和 `DRAFT/PUBLISHED/OFFLINE` 状态。
- `edu_class`：关联课程及一名主讲教师，保存容量、`reserved_count`、`enrolled_count`、教学日期和班级状态。容量约束由业务事务校验：`capacity >= reserved_count + enrolled_count`。
- `classroom`：唯一名称、容量及启用状态。
- `class_schedule`：班级、教师、教室、开始和结束时间及 `SCHEDULED/CANCELLED` 状态。

课程和教学班分离；学生通过 V3 `class_student` 加入班级，课程、班级不存储 `student_id`。物理删除不作为课程和有历史记录的教学实体的管理操作。

## V3：报名、支付与消息

- `class_student`：班级与学生关系，唯一键 `(class_id, student_id)` 防止重复入班。
- `enrollment_order`：学生、班级、价格快照、到期时间及 `PENDING/PAID/CANCELLED/EXPIRED` 状态。
- `payment_record`：模拟支付流水，唯一 `order_id` 保证一次订单最多一条支付记录。
- `message_outbox`：订单相关消息在业务事务内写入；唯一 `dedupe_key` 防止重复调度。
- `message_consume_log`：唯一 `(consumer_name, event_id)` 对 RabbitMQ 重复投递幂等。

## V4：考勤、作业与通知

- `attendance`：一个课次对一个学生最多一条记录，唯一 `(schedule_id, student_id)`；状态限定 `PRESENT/LATE/ABSENT/LEAVE`。
- `assignment`：班级、负责教师、标题、内容、截止时间和 `DRAFT/PUBLISHED/CLOSED` 状态。
- `assignment_submission`：作业与学生唯一 `(assignment_id, student_id)`；保留提交状态、分数、反馈和提交/批改时间。
- `notification`：用户通知、来源类型和来源 ID；唯一 `(user_id, source_type, source_id)` 保证同一业务来源不重复提醒。

关键索引包含考勤学生时间、作业班级/状态/截止时间、教师创建时间、提交学生/状态和通知用户/创建时间/未读过滤。

## V5：题库、试卷与作答

- `question`：课程、创建教师、题型、题面、选项 JSON、答案 JSON、难度和启用状态；组合索引支持按课程、教师、题型、难度筛选题库。
- `exam`：班级、负责教师、考试窗口、答题时长和 `DRAFT/PUBLISHED/CLOSED` 状态。
- `exam_question`：考试内题目快照（题面、选项、答案、难度、分值及顺序）；`(exam_id, source_question_id)` 防重，`(exam_id, sort_order)` 保序。
- `exam_attempt`：学生考试尝试；唯一 `(exam_id, student_id)` 保证每场考试每个学生只有一次尝试。
- `exam_answer`：作答和阅卷信息；唯一 `(attempt_id, exam_question_id)` 保证每道快照题在一次尝试中最多一条答案。

索引支持班级考试列表、教师题库筛选、学生尝试、待批答案与成绩查询。题库答案不会通过学生考试 API 返回；试卷快照使考试发布后不受题库修改影响。

## 索引与一致性要点

- `course(status, created_at, id)`、`edu_class(course_id, status)`、`edu_class(teacher_id, status)`：课程列表及教学班查询。
- `class_schedule(teacher_id|classroom_id|class_id, status, start_time, end_time)`：各资源的课次冲突查询。
- `class_student(class_id, student_id)` 唯一，另有学生状态查询索引。
- `enrollment_order(student_id, status, created_at, id)` 和 `(status, expire_at, id)`：订单与过期扫描。
- `message_outbox(status, available_at, locked_until, id)`：发布器领取可发消息。
- V4/V5 的唯一约束和索引见上述各表说明。

名额计数只能由报名、支付、取消和超时释放事务维护。Redis 不是数据库事实源。排课时间冲突不是唯一键可表达的规则，必须依靠资源行锁、事务和锁定读完成检查。
