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
- `dashboard`：管理员、教师和学生工作台及 MySQL 教学分析。
- `audit/course-cache`：业务操作审计和 Redis 课程缓存失效补偿。

Controller、Service、Mapper 按业务模块归档。模块模型统一放在 `entity` 包下，持久化实体直接位于该包，DTO、VO 和枚举分别位于其子包：

```text
com.educore
├── user/{controller,service,mapper}
│   └── entity/{dto,vo,enums}
├── course/{controller,service,mapper}
│   └── entity/{dto,vo,enums}
├── enrollment/{controller,service,mapper,messaging}
│   └── entity/{dto,vo,enums}
├── assignment/{controller,service,mapper}
│   └── entity/{dto,vo,enums}
├── examination/{controller,service,mapper,messaging}
│   └── entity/{dto,vo,enums}
├── dashboard/{controller,service,mapper}
│   └── entity/vo
├── audit/{controller,service,mapper}
│   └── entity
├── common/web              # REST 全局异常处理及通用类型
├── security                # JWT 身份认证组件
├── config                  # Spring、MyBatis、缓存和 RabbitMQ 配置
└── {attendance,classroom,schedule,teachingclass,notification}/...
```

例如 `com.educore.user.entity.UserEntity`、`com.educore.user.entity.dto.RegisterRequest` 和 `com.educore.user.entity.vo.UserView` 都属于用户模块模型。MyBatis-Plus `BaseMapper`、条件构造器和分页用于常规 CRUD；多表聚合、行锁、条件原子更新、Outbox 租约及幂等插入保留明确的定制 SQL，以维持业务事务语义。

模块间以服务和领域 DTO 交互。学生归属从认证主体取得，教师管理教学数据前执行班级/课次归属校验。

## 数据与异步任务

MySQL 是订单、报名、排课、作业和考试的事实来源。业务通知/订单到期消息先写入事务 outbox，再由后台发布器发送 RabbitMQ；消费者依靠消费日志及业务唯一键幂等。Redis 仅用于课程详情缓存，不决定用户权限、订单状态或名额；课程更新事务同步写缓存失效记录，失效异常由数据库任务重试。订单和考试的过期处理均有数据库定时补偿路径。已认证的 API 写操作写入精简审计表，不持久化请求体或敏感凭据。

前端可通过 `compose.yaml` 和 Nginx 容器提供静态资源并反向代理 `/api`；Compose 同时定义 MySQL、Redis、RabbitMQ 和 Spring Boot 容器。云端已使用 Docker Compose 5.6.0 构建运行镜像并完成五个容器的启动和 API 验证，详见 [测试报告](test-report.md)。

管理员、教师和学生看板及班级分析均在 Spring 服务内聚合 Mapper 的 MySQL 查询，不引入额外分析系统。考试草稿复用 V5 `exam_answer` 唯一键。V6 增加课次 `COMPLETED` 状态，V7 增加操作审计及课程缓存失效补偿表。
