# EduCore 教培管理平台

EduCore 是面向中小型培训机构的 Java 后端管理系统，采用 Spring Boot 模块化单体架构，所有业务 API 使用 `/api` 前缀。

## 已实现阶段

- **第一阶段：基础工程与用户权限**：注册、登录、JWT 认证、角色权限、个人信息、教师账号和用户状态管理。
- **第二阶段：课程、班级与排课**：课程/班级/教室管理，教师分配，课次创建修改及并发安全的排课冲突检查。

报名订单、考勤、作业、考试、前端和部署仍属于后续阶段，尚未实现。当前不使用 Docker。

## 技术栈

- Java 21、Spring Boot 3.5.5、Spring Security、JWT（JJWT）
- MyBatis-Plus、MySQL 8、Flyway、HikariCP
- Spring Validation、SpringDoc OpenAPI
- JUnit 5、Mockito、Spring Boot Test、H2（测试）

## 环境与启动

需要 JDK 21、Maven 3.9+ 和 MySQL 8。预先创建数据库：

```sql
CREATE DATABASE educore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、至少 32 字节随机值的 `JWT_SECRET`。可选设置 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 初始化演示管理员；未提供密码时初始化器跳过，不会使用默认密码。Spring Boot 不自动读取 `.env`，请在 shell 或 IDE 中设置变量。`.env` 已加入 `.gitignore`，不要提交真实凭据。

PowerShell 示例：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/educore?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'educore'
$env:DB_PASSWORD = '本机数据库密码'
$env:JWT_SECRET = '请替换成至少32字节的随机秘密'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = '请设置本机管理员密码'
mvn spring-boot:run
```

Flyway 启动时按顺序执行 `V1`（用户表）和 `V2`（课程、班级、教室、排课表）。成功启动后，接口文档 UI 为 `http://localhost:8080/swagger-ui.html`。

## API 文档

详见 [docs/api.md](docs/api.md)。所有响应包含 `code`、`message`、`data`、`requestId`；传入 `X-Request-Id` 可指定请求 ID。JWT 使用 `Authorization: Bearer <token>`。

## 测试

```shell
mvn clean verify
```

单元测试与 H2 集成测试无需 MySQL。集成场景覆盖并发排课及教师数据权限；H2 与 MySQL 锁行为并不完全相同，真实 MySQL 的 Flyway、启动和并发行为仍需在 MySQL 8 环境验收。

## 数据与业务规则

- 数据库时间使用 UTC，业务时间字段为 `DATETIME(3)`。
- 课程与教学班分离；班级预留 `reserved_count`、`enrolled_count`，本阶段不开放报名写入。
- 教室容量不得低于已有排课班级容量；教师端查询从 JWT 身份取得当前教师 ID。
- 课程只能在上架后开放班级报名；容量不得小于已预占与已报名人数。
- 排课采用左闭右开区间。事务按教师 ID、教室 ID、班级 ID 的固定顺序加锁，再以锁定读重查冲突。
- 课程及历史排课不提供物理删除接口。

数据库结构见 [docs/database.md](docs/database.md)，排课加锁设计见 [docs/concurrency.md](docs/concurrency.md)。
