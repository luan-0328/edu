# 测试与验收报告

测试日期：2026-10-03。结果针对本机开发环境，不代表生产吞吐承诺。

## 自动化测试

| 检查 | 结果 |
| --- | --- |
| `mvn -q clean verify` | 通过：19 个单元/集成测试，0 失败、0 错误 |
| `cd web; npm run build` | 通过：TypeScript 类型检查和 Vite 生产构建完成 |
| JMeter 测试计划解析 | 通过：JMeter 5.6.3 成功加载 `.jmx` 并运行场景 |

Java 集成测试涵盖排课冲突及教师归属、考勤和班级成员校验、作业发布通知幂等、作业提交/批改、考试试卷快照与答案隔离、一次考试尝试、阅卷和超时收卷。测试运行在 H2/MySQL 模式；实际 MySQL 服务检查见下节。

前端构建通过，但 Element Plus 全量安装产生约 1.12 MB 的压缩前 JavaScript 包，Vite 给出大 chunk 提示；后续可按路由拆分组件以缩小首屏资源。

## 实际依赖联调

- 使用本机 MySQL 8.0.46 单独创建临时数据库，Flyway 成功验证并执行 `V1` 至 `V5`。
- 启用本机 Redis 7.0.15 和 RabbitMQ 4.3.2，在独立 RabbitMQ vhost 中启动应用。
- 通过真实 HTTP 流程完成管理员、教师、学生登录，课程/班级/课次和报名订单；模拟支付后学生正式入班。
- 发布作业后验证 RabbitMQ outbox 发布、消费者幂等日志与站内通知各一条；验证课程缓存命中及课程修改后的缓存失效。
- 联调结束后已删除临时数据库和 RabbitMQ vhost，并停止本轮启动的 Redis、RabbitMQ 与应用进程。

## 报名容量并发

使用 JMeter 5.6.3、真实 MySQL 8.0.46 和独立应用实例；创建容量为 30 的报名中班级，准备 100 个不同学生令牌，以 1 秒 ramp-up 并发请求报名：

- 请求数：100；JMeter 错误：0（成功创建订单的 HTTP 200 与预期班满 HTTP 409 均视为预期结果）。
- 响应码：HTTP 200 为 30 个，HTTP 409 为 70 个。
- JMeter 汇总：平均 14 ms，最短 4 ms，最长 82 ms，约 92.2 请求/秒，场景用时约 1 秒。
- MySQL 最终值：`capacity=30`、`reserved_count=30`、`enrolled_count=0`，`PENDING` 订单 30 个；`reserved_count + enrolled_count` 没有超过容量。

这些数字仅描述这台本地机器和该次运行。测试专用数据库、JWT CSV、JTL 结果文件及临时 RabbitMQ vhost 已清理；仓库只保留不含凭证的 JMeter 测试计划。

## 未覆盖与边界

- 没有在生产部署环境或多节点服务集群上做压测。
- H2 集成测试不能替代针对 MySQL 事务隔离、锁等待和故障恢复的生产容量验收。
- 按项目当前约定不提供 Docker Compose/Nginx 容器部署；开发/运行依赖由本机或独立服务提供。
