# alanapi-backend — API 开放平台主后端

API 开放平台的核心服务：用户体系、密钥管理、接口管理、调用次数（计费）管理、调用日志与统计分析。前端（alanapi-frontend）与本服务交互，实际接口执行由 alanapi-interface 提供，客户端通过 alanapi-client-sdk 调用。

> 基于 SpringBoot 初始模板（by alan）演化而来，保留了通用响应 / 全局异常 / 错误码等基建。

## 技术栈

- Spring Boot 2.7 + MyBatis-Plus + Redis（Session 共享 / 验证码 / 防重放）
- MySQL（`alan` 库，与 alanapi-interface 共用同一数据库）
- Knife4j 接口文档（`/api/doc.html`）、火山引擎 TOS（SDK 文件存储）

## 核心功能

### 用户体系
- 三种登录方式：账号密码 / 手机号短信验证码 / 邮箱验证码，后两者用户不存在时自动注册
- 密钥管理：`/user/generateKey` 生成（重新生成）accessKey / secretKey
- 账号状态：`userStatus`（0-正常，1-冻结），冻结用户无法登录、存量会话立即失效
- 用户注销：`/user/cancel` 自行注销（逻辑删除），管理员账号不允许注销

### 接口管理（管理员）
- 接口信息 CRUD、发布（`/online`）/ 下线（`/offline`）
- 发布前自动试调用验证：用管理员 ak/sk 签名真实调用一次接口服务，通过（统一响应 code=200 且 data 非空）才允许发布

### 调用次数管理
- 每用户 × 每接口一条调用关系（`user_interface_info` 表，`(userId, interfaceInfoId)` 唯一索引防重复）
- 管理员：分配初始次数（`/add`）、按增量充值（`/charge`，原子累加）、调整次数与禁用（`/update`）、分页查询（含用户名 / 接口名）
- 用户自助：`/userInterfaceInfo/my` 查询自己的调用次数（个人中心展示）
- 原子扣次 SQL：`totalNum + 1, leftNum - 1` 且条件 `leftNum > 0`，并发下不会扣成负数

### 调用日志与统计
- `invoke_log` 记录每次调用：用户、接口、请求方式 / 路径 / 参数、响应数据、成败、耗时
- 被平台拒绝的调用（接口下线 / 缺凭证 / 无法连接接口服务）同样留痕，拒绝原因写入响应字段
- 接口分析（管理员）：调用次数排行 TOP10、近 30 天调用趋势、调用总览（总次数 / 成功率 / 平均耗时 / 调用用户数）

### 其他
- 文档管理、SDK 管理（jar 包上传下载）、短信 / 邮箱验证码发送

## 快速启动

```bash
# 1. 初始化数据库（建库、建表、唯一索引）
mysql -uroot -p < sql/ddl.sql

# 2. 修改 application.yml：数据库 / Redis / TOS / 短信邮箱配置
# 3. 启动（默认端口 7529，context-path /api）
mvn spring-boot:run
```

前置依赖：JDK 8+、MySQL 8、Redis。接口管理里的示例接口依赖 [alanapi-interface](https://github.com/AlanYsc6/alanapi-interface)（localhost:8123）在线。

## 主要接口

| 分组 | 路径 | 说明 |
|---|---|---|
| 用户 | `/api/user/login`、`/register`、`/get/login`、`/logout`、`/cancel` | 登录 / 注册 / 当前用户 / 退出 / 注销 |
| 用户 | `/api/user/list/page`、`/add`、`/update`、`/delete`（管理员） | 用户管理，update 支持 `userStatus` 冻结 |
| 密钥 | `/api/user/generateKey` | 生成 ak/sk |
| 接口 | `/api/interfaceInfo/list/page`、`/online`、`/offline`、`/invoke` | 接口列表 / 发布下线 / 在线调用（自动签名转发） |
| 次数 | `/api/userInterfaceInfo/add`、`/charge`、`/update`、`/list/page`、`/my` | 次数分配 / 充值 / 禁用 / 查询 / 自助查询 |
| 日志 | `/api/invokeLog/list/page`、`/delete`（管理员） | 调用日志查询 |
| 分析 | `/api/analysis/top/interface/invoke`、`/invoke/trend`、`/invoke/overview` | 排行 / 趋势 / 总览（管理员） |

完整接口调试：启动后访问 `http://localhost:7529/api/doc.html`（Knife4j）。

## 相关项目

- [alanapi-interface](https://github.com/AlanYsc6/alanapi-interface)：接口服务（验签、计数、日志的实际执行方）
- [alanapi-client-sdk](https://github.com/AlanYsc6/alanapi-client-sdk)：客户端 SDK
- [alanapi-frontend](https://github.com/AlanYsc6/alanapi-frontend)：Web 前端
