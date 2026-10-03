# EduCore 教培管理平台

当前仓库实现开发执行方案的**第一阶段：基础工程与用户权限**。系统采用 Spring Boot 模块化单体结构，API 前缀为 `/api`。后续课程、报名、教学和考试业务尚未实现。

## 技术栈

- Java 21、Spring Boot 3.5.5、Spring Security
- MyBatis-Plus、MySQL 8、Flyway
- JWT（JJWT）、BCrypt、Spring Validation、SpringDoc OpenAPI
- JUnit 5、Mockito、Spring Boot Test

## 环境配置

需要 JDK 21、Maven 3.9+ 和 MySQL 8。数据库需预先创建，例如 `CREATE DATABASE educore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`。Flyway 会自动创建 `sys_user` 表。

复制 `.env.example` 为 `.env`，设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、至少 32 字节的随机 `JWT_SECRET`。Spring Boot 不会自动读取 `.env` 文件；请在启动 shell/IDE 中导出这些环境变量。可选设置 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 创建演示管理员。若未设置 `ADMIN_PASSWORD`，初始化器跳过建管，且不会写入任何默认密码。不要将 `.env` 提交到版本库。

Windows PowerShell 示例：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/educore?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'educore'
$env:DB_PASSWORD = '本机数据库密码'
$env:JWT_SECRET = '请替换成至少32字节的随机秘密'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = '请使用本机演示密码'
mvn spring-boot:run
```

应用启动成功后，OpenAPI UI 地址为 `http://localhost:8080/swagger-ui.html`。

## 第一阶段接口

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/api/auth/register` | 公开，注册为 STUDENT |
| POST | `/api/auth/login` | 公开 |
| GET | `/api/users/me` | 已登录 |
| PUT | `/api/users/me` | 已登录 |
| POST | `/api/admin/teachers` | ADMIN |
| GET | `/api/admin/users?page=1&size=20` | ADMIN |
| PATCH | `/api/admin/users/{id}/status` | ADMIN |

请求体示例：注册 `{"username":"student01","password":"Password123","realName":"张三"}`；登录 `{"username":"student01","password":"Password123"}`；状态更新 `{"status":"DISABLED"}`。登录响应提供 Bearer JWT。统一响应结构为 `code`、`message`、`data`、`requestId`；`X-Request-Id` 可由调用方传入或由服务生成。

## 构建和测试

```shell
mvn clean verify
```

单元测试不依赖数据库。应用启动和 Flyway 集成验证需要可访问的 MySQL 8。

## 数据库和安全说明

`src/main/resources/db/migration/V1__create_sys_user.sql` 创建用户表，使用自增 BIGINT、唯一用户名、角色/状态检查约束和 `DATETIME(3)` UTC 时间字段。管理员初始化从 `ADMIN_PASSWORD` 环境变量读取并通过 BCrypt 哈希后入库，SQL 中不含账户密码。

每次携带 JWT 的受保护请求都会按用户 ID 重新读取用户，使用数据库中的当前角色授权；禁用账号的旧 Token 会立即失效。JWT 内含用户 ID、签发时间和过期时间。密码字段不会通过用户 VO 返回。

## 当前阶段限制

- 当前未提供 Docker Compose、Redis、RabbitMQ 或前端，这些属于后续阶段。
- 此阶段没有 MySQL 集成测试配置；需在本机 MySQL 可用后验证真实迁移和启动流程。
- 接口权限由 Spring Security 路径规则执行：`/api/admin/**` 仅 ADMIN，其余受保护 API 需认证。
