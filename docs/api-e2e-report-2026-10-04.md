# EduCore 云端接口联调记录（2026-10-04）

## 环境与部署

- 目标：`http://114.55.232.26`，通过公网 Nginx 访问云端 Docker Compose 应用。
- 依据服务器实际 OpenAPI 3.1 文档枚举到 76 个操作：37 个 GET、39 个写操作。
- 本地以 `mvn verify -q` 构建并测试；JAR 上传后在服务器 `/opt/educore` 重建 `educore-backend` 镜像，再仅重建 backend 容器。没有在运行容器中直接覆盖 JAR。
- 更新前 JAR 备份位于 `/opt/educore/backups/api-validation-20261004/educore-before.jar`。
- 新 JAR 的本机及服务器 SHA-256：`b0650a297dc010320d081ee337dbf9b862516e30ef847953c798b1e07a150067`。
- 更新后通过真实登录及全部读取接口请求验证后端可用。

## 结果

| 验收项 | 实际结果 |
| --- | --- |
| OpenAPI GET 操作 | 37/37 返回 HTTP 200 |
| 39 个写操作的无效输入/不存在资源检查 | 30 个 HTTP 400、9 个 HTTP 404；没有意外 5xx |
| 跨角色越权检查 | 5/5 返回 HTTP 403 |
| 演示账号登录 | 管理员、教师、学生各成功登录 |
| 本机后端自动化测试 | `mvn verify -q` 退出码 0：38 个测试、0 失败、0 错误、0 跳过 |
| 本机前端测试 | `npm test`：3 个测试通过；`npm run build`：Vue 类型检查通过，Vite 构建 1715 个模块成功 |
| 浏览器页面联调 | 云端教师账号登录成功，工作台展示真实课表、待批作业和班级教学进度；“我的班级”页展示真实班级及容量、报名数据 |

## 此次修复

第一次扫描发现 Spring MVC 6.2 的 `HandlerMethodValidationException` 落入通用异常处理，部分参数校验错误被错误地响应为 HTTP 500。另一个扫描请求漏带 `/api/classes` 必填的 `courseId`；缺少必填查询参数同样应按客户端错误处理。

`GlobalExceptionHandler` 现在将方法参数校验异常和缺少必填查询参数映射为 HTTP 400 `VALIDATION_ERROR`。`TeachingAndExamIntegrationTest` 增加回归覆盖：班级 PUT、班级状态 PATCH 的参数校验，以及缺少 `courseId` 的 GET。修复后重新打包、部署并完成全操作扫描。

## 浏览器验收范围

在用户当前的内置浏览器页面中完成教师演示账号登录，打开工作台并进入“我的班级”。页面从服务端加载并显示两条负责班级数据。截图在本轮浏览器输出中，当前页保留在“我的班级”。

## 范围说明

- 本次 39 个写操作逐条调用了无效请求或不存在的资源 ID，用来检查鉴权、路由、参数校验和错误状态码；这**不代表** 39 个写操作都完成了成功业务写入。
- 没有向公网演示数据库创建订单、报名、课程、作业或考试。认证、报名、支付、排课、考勤、作业、考试、消息幂等等成功流程由本地 38 项集成/单元测试覆盖；这些测试使用 H2 MySQL 模式，不能替代本轮云端 MySQL 写入/并发验证。
- 失败的写请求按项目审计机制保留了失败审计记录；没有删除或重排服务器数据。
- 本轮重新运行了前端测试及生产构建，未修改前端源码，也未重新部署静态资源；浏览器验收的是当前已部署前端及刚更新的后端。

## 重跑接口扫描

PowerShell 中设置演示账号密码后运行：

```powershell
$env:EDUCORE_BASE_URL = 'http://114.55.232.26'
$env:EDUCORE_SMOKE_PASSWORD = '替换为演示密码'
python scripts/api-smoke.py
Remove-Item Env:EDUCORE_BASE_URL, Env:EDUCORE_SMOKE_PASSWORD
```

脚本按实时 OpenAPI 文档枚举路由，不输出令牌或密码；成功时报告每类 HTTP 状态和失败数。
