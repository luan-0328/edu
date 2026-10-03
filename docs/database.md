# 数据库结构

MySQL 8 / InnoDB / utf8mb4，BIGINT 主键，UTC `DATETIME(3)`。Flyway 迁移位于 `src/main/resources/db/migration`。

## V1：sys_user

用户名唯一；角色 `ADMIN`、`TEACHER`、`STUDENT`；状态 `ACTIVE`、`DISABLED`。密码以 BCrypt 哈希存储。

## V2：教学基础模型

- `course`：名称、描述、`DECIMAL(10,2)` 价格、`DRAFT/PUBLISHED/OFFLINE` 状态。
- `edu_class`：关联课程和一位主讲教师，容量、预占数、报名数、教学日期和班级状态。`capacity >= reserved_count + enrolled_count`。
- `classroom`：教室名称唯一、容量和启用状态。
- `class_schedule`：班级、教师、教室、起止时间和 `SCHEDULED/CANCELLED` 状态；教师 ID 在创建课次时从班级复制。

V2 建立外键，禁止引用不存在课程、用户、班级或教室。课程及班级均不存储学生 ID，报名关系留待后续阶段。阶段二不提供课程、班级或历史课次的物理删除 API。

## 关键索引

- `course(status, created_at, id)`：已上架课程分页。
- `edu_class(course_id, status)`、`edu_class(teacher_id, status)`：课程和教师班级查找。
- `class_schedule(teacher_id, status, start_time, end_time)`。
- `class_schedule(classroom_id, status, start_time, end_time)`。
- `class_schedule(class_id, status, start_time, end_time)`。
- `classroom(name)` 唯一索引。

时间冲突是区间条件而非唯一键，依赖事务行锁和锁定读执行，不能只依赖索引或先查后插。
