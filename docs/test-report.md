# 测试与验收报告

本报告区分 V1.0 的历史真实依赖联调/压测、V1.1 本地自动化结果和 V1.1 云端 Docker 复验。结果描述实际运行环境，不构成生产性能承诺。

## 云端全接口联调（2026-10-04，最新）

服务器 OpenAPI 列出的 76 个操作已逐条请求：37 个 GET 均为 200，39 个写操作使用无效参数/不存在资源进行拒绝路径检查（30 个 400、9 个 404），没有意外 5xx；5 项跨角色检查均为 403。内置浏览器完成教师登录、工作台和“我的班级”数据页验收。扫描中修复了参数校验异常被映射为 500 的问题。本机 `mvn verify -q` 为 38 个测试全部通过；前端 `npm test` 3 项通过、`npm run build` 类型检查和生产构建通过。写操作的云端成功业务流程和 MySQL 并发行为不在本轮覆盖范围。详见 [云端接口联调记录](api-e2e-report-2026-10-04.md)。

## requestId 日志关联补齐（2026-10-04，最新）

- `mvn clean verify -q`：退出码 0，37 个测试、7 个套件，0 失败、0 错误、0 跳过，JAR 打包成功。此处是在原 34 个测试基础上新增 3 个 RequestLoggingTest 后的真实计数，与早期旧报告重复计数无关。
- 新测试覆盖请求日志的 MDC 编号、响应头/属性一致、请求元数据日志不包含查询参数/Authorization/请求体、非法传入编号替换、异常后的 MDC 清理、未知异常堆栈记录及通用错误响应。现有教学集成测试同时验证响应编号与审计表编号一致。
- 日志配置打印 `[requestId=...]`；每次请求记录方法、路径、状态和耗时。预期业务拒绝记录业务码，未知异常记录堆栈，前端响应不暴露堆栈。
- 后端重新打包上传并重建 Docker backend；JAR 本地与服务器 SHA-256 一致（`bb6c56868c7234f0f0ea7411d8d4c2d4e7e9e7cc1ee9675e0218f3cac0260ca6`）。备份在 `/opt/educore/backups/request-logging-20261004/backend.jar`。容器 running、restart=0、OOMKilled=false。
- 云端使用管理员发送无效课程表单，得到 400 `VALIDATION_ERROR`；响应体/响应头编号一致：`request-log-b3d078c48c374022b876809677031b8b`。MySQL 审计表查询对应 `demo-admin`、`/api/admin/courses`、400、FAILED；Docker 日志同编号记录 `HTTP POST /api/admin/courses status=400 durationMs=421`。未创建课程，产生一条预期失败审计记录。
- 没有在云端人为制造 500；异常堆栈分支由本地测试验证。本次未修改前端，未重新运行前端测试或完整并发/故障测试。排查步骤见 [部署文档](deployment.md#使用-requestid-排查请求)。

## 展示问题修复与实际验收（2026-10-04，最新）

| 检查 | 实际结果 |
| --- | --- |
| `mvn clean verify -q` | 退出码 0；34 个测试、6 个套件，0 失败、0 错误、0 跳过；JAR 打包成功 |
| `web/npm test` | 3 个测试全部通过：UTC/时区与日期语义、四种题型序列化和回显、友好表单校验 |
| `web/npm run build` | 类型检查及生产构建通过，1715 个模块 |
| 及格率集成回归 | 50/100 分试卷按总分 60% 判断；覆盖临界及格、低于临界、满分和未批答卷；班级及考试统计一致 |
| 云端 MySQL 8.4 SQL 回归 | 在 MySQL Docker 容器内建立会话临时表，执行源码中的考试聚合 SQL（仅替换为临时表名称）；50 分试卷 30 分及格、29 分不及格，100 分试卷 60 分及格、59 分不及格；待批满分答卷不计入时两个试卷各 50.00%，转最终评分后各 66.67% |
| 云端部署 | 本地 JAR、dist、Nginx 配置上传并重建 backend/frontend；初次上传包 SHA-256 本地/服务器一致；之后补齐时间格式遗漏和窄屏布局，仅更新 frontend |
| 云端运行状态 | backend/frontend 均 running，restart=0、OOMKilled=false；后端日志确认启动成功，Nginx `-t` 成功；MySQL/Redis/RabbitMQ 保持健康 |
| 公网接口文档 | `/swagger-ui.html`、`/swagger-ui/index.html` 返回 Swagger HTML；`/v3/api-docs`、`/v3/api-docs/swagger-config` 返回 JSON；OpenAPI 3.1.0、69 条路径 |
| 三角色接口与页面 | 登录与看板接口返回 SUCCESS；浏览器实际登录管理员、教师、学生并读取工作台，保存真实截图 |
| 业务时间 | 教师与学生的下一课次 04:07 UTC 显示 12:07 Asia/Shanghai；学生成绩提交时间也转换为北京时间；纯日期保持原样 |
| 题库表单 | 浏览器打开已有单选题：题型/难度中文、选项正常回显、正确答案 A 正常选中；保存修改成功、弹窗关闭并刷新列表 |

备份目录：`/opt/educore/backups/showcase-20261004-1930`，含原 JAR、前端静态文件、Nginx 配置、数据库 SQL gzip。本次没有数据库结构变更，因此不新增 Flyway 迁移。MySQL 回归只使用连接级临时表，未污染演示数据。浏览器保存已有题目时没有改变其题干、选项或答案，产生了正常更新及审计记录。

浏览器使用期间首次登录遇到部署前旧脚本引用已移除的工作台文件，刷新页面后新版本正常加载；用户已有窗口应刷新后再体验。浏览器最初的简便绑定接口持续超时，改用浏览器文档提供的 tabs/Playwright 接口后成功完成上述有限验收。

未执行：本版本 100 并发报名、支付/超时竞争、Redis/MQ 故障注入、完整浏览器端到端测试套件。上述 MySQL 聚合回归不能代替并发测试。

## 包结构调整后复验（2026-10-04）

| 检查 | 结果 |
| --- | --- |
| `mvn clean test -q`（展示审查时重新执行） | 通过：33 个测试，0 失败、0 错误、0 跳过，6 个测试套件 |
| `mvn -q -DskipTests package` | 通过：后端和测试源码编译，Spring Boot JAR 打包成功 |
| 包与目录核对 | 通过：Java package 声明与源码路径一致；未发现旧全局 `controller/service/mapper` 包引用 |

更正：此前按残留 XML 报告统计为 37 个、7 个套件，其中旧包名 `com.educore.service.user.UserServiceTest` 的 4 个测试报告被重复计入。2026-10-04 展示审查执行 `mvn clean test -q` 后，实际为 33 个测试、6 个套件；没有删除任何源码测试。展示审查同时重新执行前端 `npm run build`，类型检查及生产构建通过。浏览器绑定超时，未完成浏览器端到端验收；本次包结构调整尚未重新部署到云端。此前云端 Docker 复验记录对应其部署时的源码版本。详见 [展示审查](showcase-review-2026-10-04.md)。

## 操作弹窗优化与云端前端更新（2026-10-04）

`web` 下 `npm run build` 通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建（1710 个模块转换）。学生报名与订单支付使用结构化模拟支付弹窗；考试成绩使用成绩卡片弹窗；订单/班级取消确认和编辑提示统一了弹窗样式。前端静态文件上传后仅重建 `frontend` 容器，Docker Compose 显示容器已重建并启动。公网首页、入口脚本、工作台脚本及 CSS 均返回 HTTP 200，并确认新支付提示和弹窗样式已发布；后端与数据库未重启或修改。本次没有浏览器端到端自动化测试。

## V1.1 最终本地复验（2026-10-04）

| 检查 | 结果 |
| --- | --- |
| `mvn -q test` | 通过：33 个单元/集成测试，0 失败、0 错误、0 跳过 |
| `mvn -q package` | 通过：执行完整 Maven package 生命周期并生成可部署 JAR |
| `cd web; npm run build` | 通过：`vue-tsc --noEmit` 和 Vite 生产构建，1709 个模块转换成功 |
| Element Plus / 路由拆包 | 构建通过；登录脚本 2.23 KB，主入口 26.15 KB，工作台脚本 408.62 KB（未压缩）；原单块约 1.13 MB |
| 云端 MySQL Flyway V1–V7 | 通过：MySQL 8.4 云端数据库成功迁移至 V7，V7 执行约 0.14 秒 |
| 云端 Docker Compose | 通过：从本地 JAR 和 Vite `dist` 重建后端、前端；五个 EduCore 容器保持运行 |
| 云端三角色 API | 通过：管理员运营看板、教师工作台、学生学习中心和管理员审计列表均经 Nginx 返回 HTTP 200 |
| 云端 UTF-8 页面 | 通过：首页/登录页返回 `text/html; charset=utf-8`，登录脚本返回 UTF-8 且包含正常中文 |

33 个后端测试覆盖：报名订单过期替换和名额回收、重复支付幂等、同学生并发重复报名、排课并发冲突和教师归属、UTC 截止时间、考勤与课次完成互斥、班级结课条件、作业通知恢复和消费幂等、考试快照/草稿保存与恢复/自动评分/超时收卷/并发主观题阅卷、看板查询和角色数据隔离、课程缓存失效重试、用户停用约束、JWT。

前端生产构建在 V1.1 资源拆分改动后通过。主工作台仍包含多个业务面板，页面交互通过真实 API；本次没有新增浏览器端自动化测试，也没有生成管理员、教师、学生工作台截图。当前任务中的浏览器自动化绑定多次超时，因此未能完成截图验收。

## V1.1 最新云端 Docker 复验（2026-10-04）

部署前将 MySQL `educore` 数据库备份至 `/opt/educore/backups/educore-before-v11-20261004-145019.sql.gz`，并保存上一版后端 JAR 和前端静态文件。部署包与本地构建产物 SHA-256 一致。云端现有 Dockerfile、Compose、Nginx 配置与本地部署配置哈希一致；执行 `docker compose up -d --build --no-deps backend frontend`，仅重建后端和前端，未重启 MySQL、Redis、RabbitMQ，也未覆盖服务器 `.env` 或命名卷。

Spring Boot 完成 Flyway V7，新增 `operation_audit_log`、`course_cache_invalidation` 两张表。后端日志报告 `Started EduCoreApplication`，数据库迁移成功；后端和前端容器 restart count 为 0，后端 `OOMKilled=false`。MySQL、Redis、RabbitMQ 容器原有健康状态保持正常。主机可用内存约 472 MiB，因此没有在云机运行 100 并发 JMeter 场景。

通过 Nginx `/api` 代理真实 HTTP 登录管理员、教师、学生账号，分别读取 `/api/admin/dashboard`、`/api/teacher/dashboard`、`/api/students/me/dashboard`，均返回 HTTP 200 和 `SUCCESS`；`/api/admin/audit-logs?page=1&size=5` 返回 200。管理员看板实数为学生 28、在教班级 2、当前报名 26、已支付订单 26、模拟金额 22174.0。对管理员课程创建接口发送无效表单，返回 400 `VALIDATION_ERROR`；随后审计列表包含相同路径的 `FAILED`、400 记录，未创建课程。`/v3/api-docs` 返回 200，下载 62,140 字节。

服务数据仍有管理员 1 名、教师 3 名、学生 28 名；四个班级容量分别为 30、25、20、18，正式入班人数分别为 20、1、3、2，预占人数分别为 0、1、0、0。除 Flyway 迁移和预期的失败操作审计记录外，冒烟测试没有修改演示用户、订单、入班或容量数据；缓存失效队列无残留。本次是云端容器、Flyway 和 API 冒烟验收，不等同于真实 MySQL 高并发、RabbitMQ 故障重投或浏览器端到端测试。

## V1.1 云端容器部署复验（2026-10-04）

在 Ubuntu 22.04 云主机上使用本地 `mvn verify` 产物 JAR 和本地 Vite `dist` 构建运行镜像；没有在服务器重复下载 Maven/npm 依赖。Docker Compose CLI 插件升级到 v5.6.0，Docker Engine 保持原版本。五个 EduCore 容器均启动，MySQL 与 RabbitMQ health check 通过；Spring Boot 完成 Flyway V1–V6 迁移并成功启动。

导入演示 SQL 后，数据库有 3 个演示账号、1 个班级、1 个订单和 1 条支付记录。通过公网 HTTP 验证登录页返回 200，管理员/教师/学生三种角色均可登录；管理员和学生 dashboard API 返回 200。容器统计内存合计约 660 MiB，主机当时可用内存约 434 MiB。MySQL 镜像为 8.4，Flyway 对该版本输出“高于已测试支持版本”的兼容性提示，但本次 6 个迁移均实际执行成功。

部署地址为 `http://114.55.232.26`，只通过 80 端口提供 HTTP 演示，未配置 TLS。旧 `geo-*` 项目容器已停止；其目录和 MySQL named volume `myapp_mysql-data` 保留，未删除。

## V1.1 云端演示数据扩容与 API 复验（2026-10-04）

扩充演示 SQL 后先备份数据库，再通过 MySQL 容器事务导入。当前演示数据包含管理员 1 名、教师 3 名、学生 13 名、课程 3 门、班级 4 个和正式入班记录 11 条；订单有已支付 11 笔、待支付 1 笔、已取消 1 笔，并关联 11 条模拟支付记录。另有已完成及未来课次、混合出勤记录、已发布作业、已批与待批提交、已完成及即将开始的考试。

使用新增教师 `demo-teacher-02` 请求 `/api/teacher/classes` 返回其 Python 班；使用新增学生 `demo-student-08` 请求 `/api/students/me/dashboard` 返回 Python 课程进度；管理员 dashboard 返回学生数 13、已教班级数 2、当前入班人数 11 和已支付订单数 11。以上是线上接口人工冒烟验证，不代表浏览器端到端自动化或完整回归测试。

## V1.1 管理列表排序与学生演示数据补充（2026-10-04）

管理员账号、课程、教学班和教室列表，以及教师所授班级列表改为按数据库 ID 或开课日期升序查询。发布前运行 `mvn -q test`：28 个测试全部通过；`mvn -q -DskipTests package` 成功；`cd web; npm run build` 的 TypeScript 检查和 Vite 构建均通过。

更新后的 `scripts/extend-demo.sql` 先在由线上备份恢复的隔离 MySQL schema 中连续执行两次，再检查数据未重复，之后才导入线上库。线上当前为管理员 1 名、教师 3 名、学生 28 名；Java 实战班容量 30、正式入班 20、剩余 10 个名额。已支付模拟订单和对应支付记录各 26 条，另有待支付、已取消订单各 1 条。新学生 `demo-student-14` 至 `demo-student-28` 使用 README 中的统一演示密码，并正式加入 Java 实战班。

部署后通过 HTTP 验证前端和 OpenAPI 均返回 200；管理员、`demo-student-28` 均可登录，学生学习中心返回 1 个在读班级，管理员看板返回 28 名学生、26 个当前入班人数及 26 笔已支付订单。管理员用户、课程、班级、教室 API 均返回升序 ID（用户列表首批 ID 为 1、2、3、33…；课程及教室为 1、6、7；班级为 1、8、9、10）。ID 间隔来自此前失败并回滚的演示数据插入所消耗的 MySQL 自增值，不代表有对应账号、课程或教室被遗漏；主键未重排，以免破坏关联。

本次没有运行浏览器端自动化测试；页面行为通过前端生产构建及其调用的线上 API 进行验证。

## V1.1 MySQL 联调（2026-10-04）

在本机 MySQL 8.0.46 中创建隔离验证 schema `educore_v11_verify_20261004`，未改动现有业务库。以当前代码启动 Spring Boot 后，Flyway 成功验证并依次执行 V1–V6。将 `scripts/init-demo.sql` 连续执行两次后，3 个演示账号、课程、班级、成员、支付订单、支付流水、作业和两场考试均保持唯一；演示种子还包含已完成及未来课次、考勤、作业提交和已出分考试。

分别使用管理员、教师和学生演示账号执行真实 HTTP 登录。`/api/admin/dashboard` 返回学生数 1、在教班级 1、已支付订单 1、模拟金额 899；`/api/students/me/dashboard` 返回一个班级及课程进度、近期作业、考试、成绩、通知；`/api/teacher/classes/{id}/analytics` 返回 2 个课次（1 个已完成）和 2 场考试分析。演示数据和应用仅存在于临时验证环境，已在本次验收后清理。

## V1.0 真实依赖联调记录（2026-10-03）

以下记录来自先前本机联调，覆盖当时的 V1.0 代码和 Flyway V1–V5，不包含 V1.1 新增的 V6：

- 使用 MySQL 8.0.46 临时数据库执行并验证 Flyway V1–V5。
- 使用 Redis 7.0.15 和 RabbitMQ 4.3.2，在独立 vhost 中启动应用。
- 以 HTTP 完成管理员、教师、学生登录，创建课程/班级/课次和报名订单；模拟支付后正式入班。
- 验证作业发布的 Outbox、消费幂等记录与站内通知，并验证课程缓存命中及课程修改后的失效。
- 联调后清理了临时数据库和 RabbitMQ vhost，并停止当时启动的服务进程。

## 报名容量并发（V1.0 记录）

使用 JMeter 5.6.3、MySQL 8.0.46 和独立应用实例：容量 30 的班级、100 个不同学生令牌、1 秒 ramp-up 并发请求。

- 请求数 100；预期结果 HTTP 200 成功 30 个、HTTP 409 班满 70 个，JMeter 错误数 0。
- 平均响应 14 ms、最短 4 ms、最长 82 ms，约 92.2 请求/秒，场景用时约 1 秒。
- 最终数据库状态 `capacity=30`、`reserved_count=30`、`enrolled_count=0`、`PENDING` 订单 30 个，容量未超限。

数据来自一台本地机器的一次场景。测试数据库、JWT CSV、JTL 结果文件和临时 vhost 已清理；仓库仅保留不含令牌的 JMeter 测试计划。

## 未覆盖的验证范围

- 本机没有 Docker CLI；云端 Compose 的 MySQL/Redis/RabbitMQ/API/Web 多容器编排已通过启动和 HTTP/API 验证。
- V1.1 后端自动化使用 H2/MySQL 模式，不能代替 MySQL 8 的真实隔离级别、锁等待和故障恢复验收。
- 本次没有在多节点实例或生产配置下压测；也没有运行角色登录后的浏览器端到端自动化。
- PowerShell 版 `scripts/load-demo-data.ps1` 未在服务器运行；云端演示 SQL 已通过 MySQL 容器命令导入并完成三角色登录验证。
