# EduCore API（阶段一至三）

所有接口前缀为 `/api`。受保护请求添加 `Authorization: Bearer <JWT>`。统一响应：

```json
{"code":"SUCCESS","message":"操作成功","data":{},"requestId":"trace-id"}
```

错误响应中的 `data` 为 `null`，不会返回数据库异常堆栈。分页请求默认 `page=1&size=20`，最大 `size=100`。

## 用户与权限

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/auth/register` | 公开；创建 STUDENT |
| POST | `/api/auth/login` | 公开 |
| GET / PUT | `/api/users/me` | 已登录 |
| POST | `/api/admin/teachers` | ADMIN |
| GET | `/api/admin/users` | ADMIN |
| PATCH | `/api/admin/users/{id}/status` | ADMIN |

## 课程

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/admin/courses` | ADMIN | 创建草稿课程 |
| PUT | `/api/admin/courses/{id}` | ADMIN | 修改课程信息 |
| PATCH | `/api/admin/courses/{id}/status` | ADMIN | DRAFT→PUBLISHED→OFFLINE；OFFLINE 可重新上架 |
| GET | `/api/courses?page=1&size=20` | 已登录 | 分页列出已上架课程 |
| GET | `/api/courses/{id}` | 已登录 | 查询已上架课程详情 |

课程创建/修改请求：`{"name":"Java 后端开发","description":"课程说明","price":199.00}`。状态请求：`{"status":"PUBLISHED"}`。价格必须非负，最多两位小数。

## 教学班

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/admin/classes` | ADMIN | 创建草稿班级并分配一名教师 |
| PUT | `/api/admin/classes/{id}` | ADMIN | 修改草稿/报名中班级 |
| PATCH | `/api/admin/classes/{id}/status` | ADMIN | 修改班级状态 |
| GET | `/api/classes/{id}` | 已登录 | 班级详情 |
| GET | `/api/teacher/classes` | TEACHER | 当前教师所授班级 |

班级请求：`{"courseId":1,"teacherId":2,"name":"Java 一班","capacity":30,"startDate":"2026-11-01","endDate":"2027-01-31"}`。状态值：`DRAFT`、`ENROLLING`、`IN_PROGRESS`、`FINISHED`、`CANCELLED`。转换合法性和日期条件由服务端校验。`reservedCount`、`enrolledCount` 是只读响应字段，不接受请求修改。

## 教室

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/admin/classrooms` | ADMIN | 创建启用教室 |
| PUT | `/api/admin/classrooms/{id}` | ADMIN | 修改名称和容量 |
| PATCH | `/api/admin/classrooms/{id}/status` | ADMIN | ACTIVE / DISABLED |
| GET | `/api/admin/classrooms` | ADMIN | 查询教室列表 |

教室请求：`{"name":"第一教室","capacity":32}`；状态请求：`{"status":"DISABLED"}`。

## 排课

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/admin/schedules` | ADMIN | 创建课次 |
| PUT | `/api/admin/schedules/{id}` | ADMIN | 修改课次 |
| GET | `/api/classes/{id}/schedules` | 已登录 | 查询班级课表 |
| GET | `/api/teacher/schedules` | TEACHER | 查询当前教师课表 |

课次请求：`{"classId":1,"classroomId":1,"startTime":"2026-11-01T09:00:00","endTime":"2026-11-01T11:00:00"}`。`teacherId` 从班级当前主讲教师取得，不接受客户端指定。时间采用 UTC 的本地日期时间格式，区间为 `[startTime,endTime)`；结束时刻相接的课次不冲突。

## 报名、订单与模拟支付

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/enrollments/orders` | STUDENT | 为开放报名的班级创建/复用待支付订单并预占一个名额 |
| GET | `/api/orders?page=1&size=20` | STUDENT | 查询当前学生的订单 |
| GET | `/api/orders/{id}` | STUDENT | 查询本人订单；其他学生不能访问 |
| POST | `/api/orders/{id}/pay` | STUDENT | 模拟支付；成功后正式入班，重复支付返回同一支付记录 |
| POST | `/api/orders/{id}/cancel` | STUDENT | 取消本人待支付订单并释放一次名额 |
| GET | `/api/students/me/classes` | STUDENT | 查询当前学生已正式加入的班级 |
| GET | `/api/admin/orders?page=1&size=20&studentId=&classId=&status=` | ADMIN | 按学生、班级、状态筛选订单 |

创建订单请求：`{"classId":1}`。订单有效时间由 `ORDER_TTL_MINUTES` 配置，默认 30 分钟。有效待支付订单重复创建时返回同一订单；已报名学生不能重复报名。只有模拟支付成功会新增 `class_student`，订单创建只增加 `reserved_count`。取消、超时释放预占，支付把 `reserved_count` 减一并将 `enrolled_count` 加一。已支付订单不能取消。

RabbitMQ TTL/DLX 负责订单超时通知，周期补偿扫描处理 RabbitMQ 不可用或重复/延迟投递；时间判断以 MySQL `UTC_TIMESTAMP(3)` 为准。Redis 只缓存上架课程详情，名额判定完全使用数据库。

## 主要业务错误码

`VALIDATION_ERROR`、`UNAUTHENTICATED`、`FORBIDDEN`、`COURSE_NOT_FOUND`、`CLASS_NOT_FOUND`、`CLASSROOM_NOT_FOUND`、`SCHEDULE_NOT_FOUND`、`INVALID_STATE`、`CAPACITY_EXCEEDED`、`RESOURCE_UNAVAILABLE`、`SCHEDULE_CONFLICT`、`ORDER_NOT_FOUND`、`ORDER_OWNER_MISMATCH`、`ORDER_EXPIRED`、`ALREADY_ENROLLED`、`CLASS_FULL`、`INVALID_ORDER_STATE`。
