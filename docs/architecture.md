# 系统架构

## 运行形态

EduCore 使用单个 Spring Boot 部署单元，按业务领域组织为模块化单体。REST 端统一 `/api` 前缀，Controller 解析输入和认证主体，Service 执行业务校验与事务，MyBatis-Plus Mapper 访问 MySQL。数据库模式由 Flyway 版本迁移管理。

```text
Vue 3 SPA ──HTTP/JWT──> Spring Boot REST
                             ├── MySQL 8 + Flyway
                             ├── Redis（课程详情缓存）
                             └── RabbitMQ（超时事件、作业通知）
```

前端按管理员、教师、学生提供路由和操作页；Axios 统一携带 JWT、解包通用响应并处理认证失效。Vite 开发代理将 `/api` 转发到后端。

## 业务模块

- `user/security/common`：登录、JWT、角色、统一响应和错误处理。
- `course/teachingclass/classroom/schedule`：课程、班级、教室和排课。
- `enrollment`：订单、名额、模拟支付、outbox 与消息消费。
- `attendance/assignment/notification`：考勤、作业、批改和站内通知。
- `examination`：题库、试卷、考试尝试、答题、自动/人工阅卷与超时收卷。

Controller、Service、Mapper 统一按技术层归档，同时各层以业务域子包隔离：

```text
com.educore
├── controller/{user,course,teachingclass,enrollment,...}
│   └── advice              # REST 全局异常处理
├── service/{user,course,teachingclass,enrollment,...}
├── mapper/{user,course,teachingclass,enrollment,...}
├── config                  # Spring、MyBatis、缓存和 RabbitMQ 配置
├── common                  # 响应、错误、分页和 requestId
├── security                # JWT 身份认证组件
└── {user,course,enrollment,...}/{dto,entity,enums,vo,messaging}
```

DTO、Entity、枚举、VO 和领域消息仍归属各业务域；这样入口、业务逻辑和数据访问能按层快速定位，同时避免同名类跨模块混淆。

模块间以服务和领域 DTO 交互。学生归属从认证主体取得，教师管理教学数据前执行班级/课次归属校验。

## 数据与异步任务

MySQL 是订单、报名、排课、作业和考试的事实来源。业务通知/订单到期消息先写入事务 outbox，再由后台发布器发送 RabbitMQ；消费者依靠消费日志及业务唯一键幂等。Redis 仅用于课程详情缓存，不决定用户权限、订单状态或名额。订单和考试的过期处理均有数据库定时补偿路径。

当前没有 Docker 编排，应用通过环境变量连接本机或独立部署的 MySQL、Redis、RabbitMQ。
