# 本地部署与运行

本文使用本机已安装的服务，不需要 Docker。

## 依赖

- JDK 21 和 Maven 3.9+
- MySQL 8，创建 `educore` 数据库
- Redis 7+
- RabbitMQ 3.13+（项目已用 RabbitMQ 4.3 验证）
- Node.js 24+ 和 npm

## 配置后端

复制 `.env.example` 的变量到 IDE 运行配置或当前 shell。Spring Boot 不会自动加载 `.env` 文件。设置数据库连接、随机 `JWT_SECRET`、Redis 与 RabbitMQ 连接。首次开发时可以设置 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 创建管理员；勿在共享/生产环境使用演示密码。

PowerShell 示例：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/educore?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'educore'
$env:DB_PASSWORD = '本机数据库密码'
$env:JWT_SECRET = '至少32字节的随机值'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = '本机管理员密码'
$env:REDIS_HOST = 'localhost'
$env:RABBITMQ_HOST = 'localhost'
mvn spring-boot:run
```

应用启动时 Flyway 自动执行 `V1`–`V5`。确认 RabbitMQ 用户能访问配置的 virtual host；Redis 用于课程详情缓存。Swagger UI 为 `http://localhost:8080/swagger-ui.html`。

常用配置：

| 环境变量 | 用途 | 默认值 |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL | 本机 `educore` |
| `JWT_SECRET`, `JWT_TTL_SECONDS` | JWT 签名和有效期 | secret 应自行设置；3600 秒 |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | 启动时引导管理员 | 密码空时跳过 |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `CACHE_ENABLED` | 课程缓存 | localhost:6379、启用 |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`, `MESSAGING_ENABLED` | 异步消息 | localhost:5672、`/`、启用 |
| `ORDER_TTL_MINUTES`, `ORDER_EXPIRATION_ENABLED`, `ORDER_EXPIRATION_SCAN_MS` | 待支付订单释放 | 30 分钟、启用、60 秒扫描 |
| `EXAM_EXPIRATION_ENABLED`, `EXAM_EXPIRATION_SCAN_MS` | 考试超时收卷 | 启用、60 秒扫描 |

应用关闭 RabbitMQ/Redis 集成时仅适合本地隔离开发。关闭 RabbitMQ 会停用异步消息投递，不代表队列行为已验证。

## 启动前端

```powershell
cd web
npm ci
npm run dev
```

开发服务器会将 `/api` 转发到 `http://localhost:8080`。生产构建命令为 `npm run build`；可使用 `npm run preview` 本地预览构建产物。

## 验证

```powershell
# 仓库根目录
mvn clean verify

# web 目录
npm ci
npm run build
```

另可按 [测试报告](test-report.md) 启动真实依赖做端到端验证，按 [压测说明](../performance/README.md) 运行报名容量压力场景。测试结束时删除临时数据库和凭证文件。

