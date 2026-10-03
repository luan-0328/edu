# 报名并发测试

`enrollment-capacity.jmx` 为 100 个不同学生并发请求同一个开放班级的报名订单。脚本只把 HTTP 200 和预期的 `409 CLASS_FULL` 视为通过；最终座位数必须以数据库核对。

## 准备 CSV

先在测试环境创建一个 `capacity=30` 的开放班级，并准备至少 100 个有效学生 JWT。将 token 和班级 ID 写入本机私有 CSV，首行为列名，后续每个学生一行：

```csv
token,classId
<student-jwt-1>,123
<student-jwt-2>,123
```

不要提交 JWT 或测试账号密码。每个 token 必须属于不同的、尚未报名该班级的学生；目标班级应没有已预占或已报名名额。

## 运行

```powershell
jmeter -n -t .\performance\enrollment-capacity.jmx `
  "-JusersFile=C:\private\educore-enrollment-users.csv" `
  -Jhost=localhost -Jport=8080 -Jusers=100 -Jramp=1 `
  -l .\target\enrollment-capacity.jtl
```

确认 JMeter 汇总结果中 100 个样本全部成功（业务成功或预期名额冲突），并检查 MySQL 最终满足：

```sql
SELECT capacity, reserved_count, enrolled_count,
       reserved_count + enrolled_count AS occupied
FROM edu_class WHERE id = <class-id>;

SELECT status, COUNT(*)
FROM enrollment_order WHERE class_id = <class-id>
GROUP BY status;
```

应满足 `occupied <= capacity`，且本场景新建的待支付订单数不超过容量。JMeter 数据只代表运行它的实际机器和数据库配置；不要把本地结果外推为生产吞吐承诺。
