# EduCore 教培管理平台

EduCore 是面向中小型培训机构的 Java 后端教培管理系统，采用 Spring Boot 模块化单体架构，并提供 Vue 3 管理界面。所有业务 API 使用 `/api` 前缀。

## 六阶段完成情况

1. **基础工程与用户权限**：注册、登录、JWT、角色权限、个人信息、教师账号和用户状态管理。
2. **课程、班级与排课**：课程上下架、教学班和主讲教师、教室、排课、冲突检查及事务资源锁。
3. **报名、订单与模拟支付**：并发名额预占、支付入班、取消和超时释放、RabbitMQ TTL/DLX、事务 outbox、消费幂等及 Redis 课程缓存。
4. **考勤、作业与通知**：教师考勤、学生考勤查询、作业发布/提交/批改、RabbitMQ 异步作业通知。
5. **题库、考试与自动阅卷**：题库、按规则组卷、试卷快照、考试作答、客观题自动阅卷、主观题批改和超时收卷。
6. **管理前端、文档与验收**：Vue 3 + TypeScript 单页应用，覆盖管理员、教师和学生主要操作；提供 API、架构、业务规则、数据库、并发、部署和测试文档。

课程和教学班分离；班级不保存 `student_id`，学生通过 `class_student` 关系入班。当前项目不使用 Docker。

## 技术栈

- Java 21、Spring Boot 3.5.5、Spring Security、JJWT
- MyBatis-Plus、MySQL 8、Flyway、HikariCP
- Redis、RabbitMQ
- Vue 3、TypeScript、Vite、Pinia、Element Plus
- JUnit 5、Mockito、Spring Boot Test、H2

## 环境要求

需要 JDK 21、Maven 3.9+、MySQL 8、Redis、RabbitMQ、Node.js 24+ 和 npm。先创建 MySQL 数据库：

```sql
CREATE DATABASE educore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Spring Boot 不自动读取 `.env`。从 `.env.example` 复制配置值到 shell/IDE 环境变量；真实 `.env` 已被 Git 忽略。`JWT_SECRET` 应使用至少 32 字节的随机值。`ADMIN_USERNAME` 与 `ADMIN_PASSWORD` 可用于首次启动时创建本地管理员；不设置管理员密码时初始化器会跳过，不会写入默认凭据。

PowerShell 启动后端示例：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/educore?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'educore'
$env:DB_PASSWORD = '本机数据库密码'
$env:JWT_SECRET = '请替换为至少32字节的随机秘密'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = '请设置本机管理员密码'
$env:REDIS_HOST = 'localhost'
$env:RABBITMQ_HOST = 'localhost'
mvn spring-boot:run
```

Flyway 会在应用启动时依次执行 `V1`–`V5`。Redis 和 RabbitMQ 的地址、凭据、虚拟主机及超时设置见 `.env.example`。无需容器即可本地启动所需服务。

前端开发：

```powershell
cd web
npm ci
npm run dev
```

Vite 开发服务器将 `/api` 请求转发到本地后端 `http://localhost:8080`。验证前端生产构建：

```powershell
npm run build
npm run preview
```

后端 OpenAPI UI：`http://localhost:8080/swagger-ui.html`。

## API 与设计文档

- [API 接口说明](docs/api.md)
- [总体架构](docs/architecture.md)
- [业务规则](docs/business-rules.md)
- [数据库结构与索引](docs/database.md)
- [并发与锁设计](docs/concurrency.md)
- [本地部署](docs/deployment.md)
- [测试报告](docs/test-report.md)
- [报名容量并发压测脚本](performance/README.md)

统一响应包含 `code`、`message`、`data`、`requestId`。受保护接口通过 `Authorization: Bearer <JWT>` 传递令牌；传入 `X-Request-Id` 可指定请求 ID。

## 验证命令

```powershell
mvn clean verify
cd web
npm ci
npm run build
```

Java 集成测试使用 H2；MySQL、Redis、RabbitMQ 的实际集成检查和结果见 [测试报告](docs/test-report.md)。

## 关键规则

- 只有上架课程才可开放班级报名；容量不得小于预占数与已报名数之和。
- `reserved_count` 和 `enrolled_count` 只由订单事务更新。订单用条件更新原子预占座位，支付事务将预占转为正式入班。
- 排课对教师、教室和班级按固定顺序加锁，锁内重新检查左闭右开区间 `[start,end)` 的冲突。
- 教师端对象查询从 JWT 身份执行归属校验；学生订单、通知、作业、考试和考勤查询限定当前用户。
- 考试对组卷题目保存快照，学生接口不返回答案；客观题自动阅卷，简答题由教师批改。
- Redis 仅缓存课程详情，不承载报名名额事实；RabbitMQ 事件写入事务 outbox 后发布，消费者以事件 ID 幂等处理。
