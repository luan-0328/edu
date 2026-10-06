# 部署与本地运行

项目支持直接运行 Spring Boot 和 Vite，也提供 Docker Compose 配置。所有示例仅面向本地开发演示；对外部署前应更换所有密码和 JWT 秘钥，并使用 TLS、访问控制及外部密钥管理。

## Docker Compose 本地演示

需要 Docker Engine/Desktop、Docker Compose v2 和 PowerShell 7（用于演示数据脚本）。

```powershell
Copy-Item .env.example .env
# 编辑 .env：替换数据库、RabbitMQ 和 JWT 秘密；避免把真实 .env 提交到 Git。
mvn -q verify
Push-Location web; npm ci; npm run build; Pop-Location
docker compose up --build -d
docker compose ps
./scripts/load-demo-data.ps1
```

默认 Dockerfile 将本地生成的 `target/educore-*.jar` 和 `web/dist` 复制到运行镜像，部署机不需要 Maven 或 Node.js。若要从源码构建镜像，使用 `Dockerfile.source` 与 `web/Dockerfile.source`。

前端：`http://localhost`。Swagger UI：`http://localhost/swagger-ui.html`，OpenAPI JSON：`http://localhost/v3/api-docs`；也可通过回环地址 `http://localhost:8080/swagger-ui.html` 访问后端文档。RabbitMQ 管理页：`http://localhost:15673`。Web 容器将 `/api` 业务路径及 `/swagger-ui.html`、`/swagger-ui/`、`/v3/api-docs` 文档路径反向代理给 Spring Boot；MySQL、Redis 和 RabbitMQ 数据保存在 named volumes。

Compose 的数据库健康检查通过后启动后端；Spring Boot 在启动期间运行 Flyway V1–V7。数据初始化脚本先轮询 OpenAPI 地址，确认 Flyway 已完成，再以 UTF-8 依次运行演示数据修复、[`scripts/init-demo.sql`](../scripts/init-demo.sql) 和 [`scripts/extend-demo.sql`](../scripts/extend-demo.sql)。可重复执行演示脚本，它按演示账号及演示业务标识插入或更新数据。

演示账号：管理员 `demo-admin`；教师 `demo-teacher`、`demo-teacher-02`、`demo-teacher-03`；学生 `demo-student`、`demo-student-02` 至 `demo-student-28`。共享密码为 `LocalDemo2026!`，仅适用于隔离的演示实例。Java 实战班容量 30 人，种子数据有 20 名学生入班并保留 10 个可演示名额。模拟订单金额不是真实收款。

常用操作：

```powershell
docker compose logs -f backend
docker compose logs -f mysql
docker compose ps
docker compose down
```

`docker compose down` 保留 named volumes；删除演示库数据需要显式执行 `docker compose down -v`。MySQL、Redis、RabbitMQ 与后端的映射端口默认只绑定 `127.0.0.1`，公网仅通过前端 Nginx 访问。

测试报告分别记录本机 MySQL 8.0.46 与云端 Docker 的实际迁移版本、服务状态和 API 验收结果。服务器升级前应备份 MySQL；只更新后端/前端镜像时，不要删除 named volumes。

当前临时公网演示地址为 `http://114.55.232.26`。该实例仅用于展示，尚未配置域名和 HTTPS，不要放入真实学生或支付数据。

## 不使用 Docker 的本地运行

需要 JDK 21、Maven 3.9+、Node.js 24+、MySQL 8、Redis 7 和 RabbitMQ 4。创建 MySQL 数据库：

```sql
CREATE DATABASE educore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

在 PowerShell 中提供应用配置。Spring Boot 不会自动加载 `.env`：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/educore?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'educore'
$env:DB_PASSWORD = '本机数据库密码'
$env:JWT_SECRET = '请设置至少32字节的随机值'
$env:ADMIN_USERNAME = 'demo-admin'
$env:ADMIN_PASSWORD = '仅用于本地演示的密码'
$env:REDIS_HOST = 'localhost'
$env:RABBITMQ_HOST = 'localhost'
mvn spring-boot:run
```

Flyway 会在后端启动时执行 V1–V7。另开 PowerShell：

```powershell
cd web
npm ci
npm run dev
```

Vite 在 `http://localhost:5173` 提供前端，并把 `/api` 代理到 `http://localhost:8080`。加载种子演示数据前，先启动后端并将 `scripts/init-demo.sql` 执行到 `educore` 数据库。Redis 仅承载课程详情缓存；`CACHE_ENABLED=false` 可供隔离调试。关闭 RabbitMQ 时设置 `MESSAGING_ENABLED=false`，但不等价于验证消息投递。

## 环境变量

容器使用 `.env` 注入这些值；普通 `mvn spring-boot:run` 需要由 shell 或 IDE 设置应用变量。

| 变量 | 用途 | 说明 |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL 地址及账号 | Compose 内部 URL 由服务名 `mysql` 提供 |
| `MYSQL_ROOT_PASSWORD` | 初始化 MySQL root | 只用于本机容器启动 |
| `JWT_SECRET`, `JWT_TTL_SECONDS` | JWT 签名和有效期 | secret 至少 32 字节 |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | 启动时引导管理员 | 密码为空时初始化器跳过 |
| `REDIS_HOST`, `REDIS_PORT`, `CACHE_ENABLED` | 课程缓存 | Compose 服务名为 `redis` |
| `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`, `MESSAGING_ENABLED` | 消息服务 | Compose 创建专用演示用户，不使用默认 guest 远程连接 |
| `ORDER_TTL_MINUTES`, `ORDER_EXPIRATION_ENABLED`, `ORDER_EXPIRATION_SCAN_MS` | 待支付订单释放 | 默认 30 分钟并启用扫描补偿 |
| `EXAM_EXPIRATION_ENABLED`, `EXAM_EXPIRATION_SCAN_MS` | 考试超时收卷 | 默认启用并每分钟扫描 |

## 构建和测试

```powershell
# 根目录
mvn -q verify

# web 目录
npm ci
npm run build
```

自动化结果和未覆盖范围见 [测试报告](test-report.md)。V1.1 Compose 已在云端完成镜像构建、多容器启动和 API 联调；本机仍未安装 Docker CLI。

## 使用 requestId 排查请求

1. 从浏览器 Network 中取失败响应的 `requestId`，或响应头 `X-Request-Id`。
2. 查询审计表确认操作者、接口、状态和发生时间。审计只记录已认证的业务写请求，不包含登录及普通查询。

```sql
SELECT a.request_id, a.actor_id, u.username, a.http_method,
       a.request_path, a.response_status, a.outcome, a.created_at
FROM operation_audit_log a
LEFT JOIN sys_user u ON u.id = a.actor_id
WHERE a.request_id = '替换为实际请求编号'
ORDER BY a.id;
```

3. 在服务器的 Compose 目录查同一编号对应的日志：

```bash
cd /opt/educore
docker compose logs --since 30m --no-color backend | grep -F '替换为实际请求编号'
```

请求日志包含方法、路径、HTTP 状态、耗时；业务拒绝日志包含业务错误码；未知异常记录堆栈，前端仍只返回通用错误提示。日志格式包含 `[requestId=...]`。请求元数据日志不记录查询参数、Authorization、请求体或响应体。没有 HTTP 请求上下文的定时任务日志显示 `requestId=none`。

requestId 用于关联记录，不能代替用户权限校验或业务主键。同一个用户的多次请求通常有不同编号；客户端也可通过合法的 `X-Request-Id` 提供编号，因此审计表查询可能返回多条记录。
