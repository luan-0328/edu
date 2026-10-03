# 并发排课

## 事务与锁

创建和修改课次均在一个 Spring 事务中执行。全局资源锁顺序固定为：

1. 教师用户行，按用户 ID 升序。
2. 教室行，按教室 ID 升序。
3. 教学班行，按班级 ID 升序。

修改课次时先普通读取课次以确定旧资源，再锁定旧、新教师、教室和班级集合；之后对课次行做 `FOR UPDATE`，核对旧资源没有被并发修改。创建不需要锁课次行。教师停用、教室停用和班级状态修改也会锁对应资源行，以和排课校验串行。

获得资源锁后，服务重新检查教师与教室启用状态、班级可排课状态、教室容量、班级日期范围和时间先后。

## 冲突检测

重叠判断为：

```sql
existing.start_time < :new_end_time
AND existing.end_time > :new_start_time
```

只检查 `SCHEDULED` 记录；修改时排除当前课次。冲突查询使用 `SELECT id ... FOR UPDATE`，保证 MySQL 默认 `REPEATABLE READ` 下读取已提交的最新课次，避免事务早期的一致性读快照漏掉刚提交的并发记录。教师、教室、班级任意一项冲突即回滚。

资源行锁负责同一资源的并发串行；冲突查询锁定读负责在串行后检查最新数据。共享教师、教室、班级的所有排课路径必须遵循相同资源锁顺序。

## 报名订单与名额

创建订单事务使用 `READ COMMITTED`，并按顺序锁定当前学生 `sys_user` 行，再通过班级条件更新竞争名额。学生行锁串行同一学生的创建请求；锁内读取待支付订单和正式入班关系，复用有效订单或阻止重复入班。待支付订单和成员关系使用普通读，避免对不存在的索引键做范围锁后与不同学生的插入形成间隙锁死锁。READ COMMITTED 让每条语句读取最新已提交状态，因此若支付刚好完成，后续成员查询能看到同一事务写入的正式入班记录。名额通过条件更新原子预占：

```sql
UPDATE edu_class
SET reserved_count = reserved_count + 1
WHERE id = :class_id AND status = 'ENROLLING'
  AND reserved_count + enrolled_count < capacity;
```

支付、取消和超时先 `SELECT ... FOR UPDATE` 锁订单，再更新班级名额；这些路径不锁学生行，避免与同学生的创建请求形成反向锁等待。支付事务同一提交写入支付流水、把预占计数转换为已报名计数、插入 `class_student` 并将订单改为 `PAID`。订单终态检查使重复支付/取消/超时不能重复改计数，唯一键提供幂等保护。

超时 outbox 与订单在同一数据库事务提交。发布器在短事务中以 `FOR UPDATE SKIP LOCKED` 领取记录并设置租约，提交后才向 RabbitMQ 发送并等待 publisher confirm；确认后标记 SENT，失败则延时重试。崩溃导致的重复投递由消费日志唯一键和订单终态检查消重。RabbitMQ TTL/DLX 触发常规超时处理，MySQL UTC 到期扫描补偿消息丢失或服务暂停。

## 测试边界

`ScheduleIntegrationTest` 使用 H2/MySQL 模式覆盖并发同教师排课、左闭右开边界、教师课表归属、班级状态和容量校验。测试不替代 MySQL 8 的真实并发验收；部署验收应在 MySQL 8 下重跑并查询最终 `class_schedule` 状态。
