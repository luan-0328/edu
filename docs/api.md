# EduCore API 文档

所有接口以 `/api` 开头。除注册、登录外均需要 `Authorization: Bearer <JWT>`。统一响应结构：

```json
{"code":"SUCCESS","message":"操作成功","data":{},"requestId":"trace-id"}
```

错误响应的 `data` 为 `null`，不会暴露数据库堆栈。分页参数默认为 `page=1&size=20`，`size` 最大 100。除特别说明外，写请求体为 JSON。

## 第一阶段：账号和权限

| 方法 | 路径 | 访问范围 | 功能 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 公开 | 注册 STUDENT |
| POST | `/api/auth/login` | 公开 | 登录并签发 JWT |
| GET / PUT | `/api/users/me` | 登录用户 | 查询/修改本人资料 |
| POST | `/api/admin/teachers` | ADMIN | 创建教师 |
| GET | `/api/admin/users` | ADMIN | 用户分页 |
| PATCH | `/api/admin/users/{id}/status` | ADMIN | 启用/停用账号 |

注册示例：`{"username":"student01","password":"secret-pass","realName":"李同学"}`。

## 学生与管理员看板

| 方法 | 路径 | 访问范围 | 功能 |
| --- | --- | --- | --- |
| GET | `/api/students/me/dashboard` | STUDENT | 汇总本人班级进度、下一课次、近期作业/考试、成绩及未读通知 |
| GET | `/api/admin/dashboard` | ADMIN | 汇总学生、教学班、报名、已支付模拟订单、模拟金额、热门课程与班级教学情况 |
| GET | `/api/admin/audit-logs?page=1&size=20` | ADMIN | 分页查看业务写操作元数据，可按操作者和结果过滤 |
| GET | `/api/teacher/dashboard` | TEACHER | 汇总近期课表、待批作业、待批考试简答题和本人班级教学进度 |
| GET | `/api/admin/classes/{classId}/analytics` | ADMIN | 班级课时、出勤、作业完成、考试分数和及格率 |
| GET | `/api/teacher/classes/{classId}/analytics` | 负责该班的 TEACHER | 查看本人班级的相同教学统计 |

看板只返回当前登录学生自己的数据。管理员看板中的金额来自模拟支付流水，不代表真实营收。

## 第二阶段：课程、班级、教室、排课

| 方法 | 路径 | 访问范围 | 功能 |
| --- | --- | --- | --- |
| POST | `/api/admin/courses` | ADMIN | 创建草稿课程 |
| PUT | `/api/admin/courses/{id}` | ADMIN | 修改课程 |
| PATCH | `/api/admin/courses/{id}/status` | ADMIN | 上架/下架 |
| GET | `/api/courses?page=1&size=20` | 登录用户 | 已上架课程分页 |
| GET | `/api/courses/{id}` | 登录用户 | 已上架课程详情 |
| GET | `/api/admin/courses` | ADMIN | 管理端课程分页 |
| POST / PUT | `/api/admin/classes[/{id}]` | ADMIN | 创建/修改教学班并指定主讲教师 |
| PATCH | `/api/admin/classes/{id}/status` | ADMIN | 班级状态转换；结课时需完成课次、补齐考勤、关闭作业/考试并处理待批成绩 |
| GET | `/api/classes/{id}` | 登录用户 | 班级详情 |
| GET | `/api/classes?courseId={id}` | 登录用户 | 课程可报名班级 |
| GET | `/api/admin/classes` | ADMIN | 班级分页 |
| GET | `/api/teacher/classes` | TEACHER | 当前教师负责的班级 |
| GET | `/api/teacher/classes/{id}/students` | 负责该班的 TEACHER | 班级学生名单 |
| POST / PUT | `/api/admin/classrooms[/{id}]` | ADMIN | 创建/修改教室 |
| PATCH | `/api/admin/classrooms/{id}/status` | ADMIN | 启用/停用教室 |
| GET | `/api/admin/classrooms` | ADMIN | 教室列表 |
| POST / PUT | `/api/admin/schedules[/{id}]` | ADMIN | 创建/修改课次 |
| GET | `/api/classes/{id}/schedules` | 登录用户 | 班级课表 |
| GET | `/api/teacher/schedules` | TEACHER | 当前教师课表 |
| POST | `/api/teacher/schedules/{id}/complete` | 负责该课次的 TEACHER | 确认课次完成；结束时间已到且每位在读学生均已有考勤记录后才能完成 |

课程请求：`{"name":"Java 后端开发","description":"课程介绍","price":199.00}`；状态：`{"status":"PUBLISHED"}`。班级请求：`{"courseId":1,"teacherId":2,"name":"Java 一班","capacity":30,"startDate":"2026-11-01","endDate":"2027-01-31"}`。班级状态：`DRAFT`、`ENROLLING`、`IN_PROGRESS`、`FINISHED`、`CANCELLED`。教室请求：`{"name":"第一教室","capacity":32}`。课次请求：`{"classId":1,"classroomId":1,"startTime":"2026-11-02T09:00:00","endTime":"2026-11-02T11:00:00"}`；主讲教师从班级取得，客户端不能指定。

班级容量计数是只读字段，只能由订单业务更新；教师数据由认证身份确定并检查班级归属。课次区间为左闭右开 `[startTime,endTime)`。

## 第三阶段：报名、订单和模拟支付

| 方法 | 路径 | 访问范围 | 功能 |
| --- | --- | --- | --- |
| POST | `/api/enrollments/orders` | STUDENT | 创建或复用待支付订单并预占名额 |
| GET | `/api/orders?page=1&size=20` | STUDENT | 本人订单分页 |
| GET | `/api/orders/{id}` | 订单本人 | 订单详情 |
| POST | `/api/orders/{id}/pay` | 订单本人 | 模拟支付并正式入班 |
| POST | `/api/orders/{id}/cancel` | 订单本人 | 取消待支付订单并释放名额 |
| GET | `/api/students/me/classes` | STUDENT | 本人已加入班级 |
| GET | `/api/students/me/course-enrollments` | STUDENT | 本人课程的在读班级及有效待支付订单，返回 courseId、classId、className、status、orderId |
| GET | `/api/admin/orders` | ADMIN | 按学生、班级、状态筛选订单 |

请求：`{"classId":1}`。同一学生同一班级重复创建有效订单会复用订单；同一课程已有报名中或教学中的班级时，跨班下单和旧订单支付均返回 `409 ALREADY_ENROLLED`。同课程另一个班级已有有效待支付订单时，返回 `409 INVALID_ORDER_STATE`，需先处理原订单。只有支付成功才写入 `class_student`。订单状态为 `PENDING/PAID/CANCELLED/EXPIRED`。

班级状态转换到 `FINISHED` 前，系统检查结课日期、至少一个计划课次、无未完成课次和已完成课次考勤完整性。教学数据汇总和成绩统计由 MySQL 聚合查询完成。

## 第四阶段：考勤、作业、通知

| 方法 | 路径 | 访问范围 | 功能 |
| --- | --- | --- | --- |
| POST | `/api/teacher/schedules/{id}/attendance` | 该课次负责教师 | 批量记录/更新考勤 |
| GET | `/api/teacher/schedules/{id}/attendance` | 该课次负责教师 | 查看学生花名册与考勤 |
| GET | `/api/students/me/attendance` | STUDENT | 本人考勤记录 |
| POST | `/api/teacher/assignments` | TEACHER | 创建草稿作业 |
| PUT | `/api/teacher/assignments/{id}` | 作业负责教师 | 修改草稿 |
| POST | `/api/teacher/assignments/{id}/publish` | 作业负责教师 | 发布作业并发出通知事件 |
| POST | `/api/teacher/assignments/{id}/close` | 作业负责教师 | 关闭作业 |
| GET | `/api/teacher/classes/{classId}/assignments` | 该班负责教师 | 班级作业列表 |
| GET | `/api/students/me/assignments` | STUDENT | 本人班级已发布作业 |
| PUT | `/api/assignments/{id}/submission` | 班级学生 | 提交或更新作业 |
| GET | `/api/teacher/assignments/{id}/submissions` | 作业负责教师 | 查看提交进度 |
| POST | `/api/teacher/submissions/{id}/grade` | 作业负责教师 | 批改作业，分数 0–100 |
| GET | `/api/notifications` | 登录用户 | 本人通知列表 |
| GET | `/api/notifications/unread-count` | 登录用户 | 未读数 |
| PATCH | `/api/notifications/{id}/read` | 通知本人 | 标记已读 |

考勤请求：`{"records":[{"studentId":10,"status":"PRESENT"}]}`，状态为 `PRESENT/LATE/ABSENT/LEAVE`。作业请求：`{"classId":1,"title":"第一周练习","content":"...","deadline":"2026-11-10T23:59:00"}`；提交：`{"content":"作业内容"}`；批改：`{"score":90,"feedback":"完成良好"}`。作业通知由 outbox 发布至 RabbitMQ，消费者按来源幂等写入用户通知。

## 第五阶段：题库、考试、成绩

| 方法 | 路径 | 访问范围 | 功能 |
| --- | --- | --- | --- |
| POST / PUT | `/api/teacher/questions[/{id}]` | TEACHER | 创建/修改本人题库题目 |
| GET | `/api/teacher/questions?courseId=&type=&difficulty=` | TEACHER | 查询本人题目 |
| PATCH | `/api/teacher/questions/{id}/status` | 题目创建教师 | 启用/停用 |
| POST / GET | `/api/teacher/exams` | TEACHER | 创建/查询本人负责班级的考试 |
| POST | `/api/teacher/exams/{id}/generate` | 考试负责教师 | 按题型、难度和数量抽题组卷 |
| POST | `/api/teacher/exams/{id}/publish` | 考试负责教师 | 发布试卷 |
| POST | `/api/teacher/exams/{id}/close` | 考试负责教师 | 关闭考试 |
| GET | `/api/students/me/exams` | STUDENT | 本人班级考试 |
| POST | `/api/exams/{id}/start` | 班级学生 | 开始/恢复本人唯一考试尝试 |
| PUT | `/api/exams/{id}/answers` | 考试本人 | 暂存进行中尝试的答案；考试截止后或提交后不可保存 |
| POST | `/api/exams/{id}/submit` | 考试本人 | 提交答案 |
| GET | `/api/exams/{id}/result` | 考试本人 | 查看结果；未全部评分前不可见 |
| POST | `/api/teacher/exam-answers/{id}/grade` | 考试负责教师 | 批改主观题 |
| GET | `/api/teacher/exams/{id}/answers` | 考试负责教师 | 待批主观答案 |
| GET | `/api/teacher/exams/{id}/results` | 考试负责教师 | 班级成绩 |
| GET | `/api/admin/exams/{id}/results` | ADMIN | 管理端考试成绩 |

题型：`SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE`、`SHORT_ANSWER`；难度：`EASY`、`MEDIUM`、`HARD`。题目请求含 `courseId/type/content/optionsJson/answerJson/difficulty`。考试请求含 `classId/title/startTime/endTime/durationMinutes`。组卷请求示例：`{"rules":[{"type":"SINGLE_CHOICE","difficulty":"EASY","count":5,"score":10}]}`。暂存和提交格式均为 `{"answers":{"<examQuestionId>":"A"}}`。开始/继续考试响应中的 `savedAnswers` 仅包含本人未提交尝试的已保存答案，不含正确答案；前端会自动保存并提供手动保存按钮。

组卷将题面与答案快照写入考试，不随题库后续修改而变化。学生考试页只返回题面和选项，不返回答案。选择题、判断题自动评分，简答题由教师评分；单个学生每场考试只能有一次尝试。

## 错误与权限

常见错误码包括 `VALIDATION_ERROR`、`UNAUTHENTICATED`、`FORBIDDEN`、`INVALID_STATE`、`CAPACITY_EXCEEDED`、`SCHEDULE_CONFLICT`、`CLASS_FULL`、`ALREADY_ENROLLED`、`ASSIGNMENT_NOT_FOUND`、`EXAM_NOT_FOUND`、`QUESTION_POOL_INSUFFICIENT`。教师接口会根据 JWT 中教师 ID 校验课次、班级、作业、考试和题目的归属；学生资源按本人身份校验。不要以客户端传入的用户 ID 作为授权依据。
