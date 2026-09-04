# 熵基科技轻量招聘系统

按《熵基科技轻量招聘系统需求分析》V1.3 实现的招聘官网 + HR 管理后台，覆盖需求文档 §20 的全部 P0 范围。

- 后端：Java 21 + Spring Boot 3.4 + MyBatis-Plus + MySQL 8
- 前端：Vue 3 + Vite + Element Plus + Pinia
- 认证：服务端 Session + HttpOnly Cookie，写操作强制 CSRF Token
- 首页：前端配置文件 + 静态资源维护，不走后台内容管理

## 目录结构

```text
project/
├── 熵基科技轻量招聘系统需求分析.md    需求基线 V1.3
├── 熵基公司素材/                      品牌素材（复制到 frontend/public/site/）
├── backend/                          Spring Boot 后端
│   ├── src/main/java/com/zkteco/recruit/
│   │   ├── common/                   统一响应、错误码表、全局异常、分页
│   │   ├── config/                   安全、CSRF、MyBatis、数据初始化
│   │   ├── security/                 会话、三层权限校验、限流
│   │   ├── domain/                   实体、枚举、查询 VO
│   │   ├── mapper/                   数据访问（含乐观锁条件更新）
│   │   ├── service/                  业务逻辑
│   │   └── web/                      Public / Candidate / HR / Admin 四组接口
│   └── src/main/resources/
│       ├── db/schema.sql             建表脚本（11 张表）
│       └── application.yml
├── frontend/                         Vue 前端
│   ├── public/site/                  首页配图静态资源
│   └── src/
│       ├── api/                      请求层（CSRF 自动携带与重试）
│       ├── config/homeContent.js     首页文案与企业数据
│       ├── layouts/                  官网布局、HR 后台布局
│       ├── views/public/             首页、职位列表、职位详情
│       ├── views/candidate/          我的简历、我的投递
│       ├── views/hr/                 工作台、职位管理、投递管理与详情
│       └── views/admin/              字典、HR 账号
├── scripts/smoke-test.sh             验收冒烟测试
└── output/playwright/                页面验证截图
```

## 快速启动

### 1. 准备数据库

```bash
mysql -u root -p -e "
CREATE DATABASE IF NOT EXISTS zk_recruit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'zk_recruit'@'localhost' IDENTIFIED BY 'Zk_recruit#2026';
GRANT ALL PRIVILEGES ON zk_recruit.* TO 'zk_recruit'@'localhost';
FLUSH PRIVILEGES;"

mysql -u root -p < backend/src/main/resources/db/schema.sql
```

如需改用其他账号，通过环境变量覆盖：`DB_USER`、`DB_PASSWORD`。建表脚本会顺带删除历史上的 `site_content`、`site_asset` 表。

### 2. 启动后端

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # macOS + Homebrew 示例
cd backend
mvn spring-boot:run
```

首次启动会自动完成初始化（幂等，重复启动不会重复写入）：

- 创建管理员 HR 账号
- 写入 6 个职位类别、8 个工作地点
- 生成 7 条演示职位与 1 个演示求职者（可通过 `app.init.demo-data=false` 关闭）

首页文案与配图不走后端初始化，改 `frontend/src/config/homeContent.js` 与 `frontend/public/site/` 后重新构建前端即可。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 。Vite 已把 `/api` 代理到 8080，浏览器与后端同源，Session 与 CSRF Cookie 正常工作。

## 演示账号

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 管理员 HR | `hr.admin@zkteco.com` | `Admin@2026` | 可管理账号、字典、职位归属 |
| 求职者 | `demo.candidate@example.com` | `Demo@2026` | 资料与在线简历已完善，可直接投递 |

普通 HR 账号请用管理员在「系统配置 → HR 账号管理」中创建，创建后系统返回一次性临时密码。

## 验收测试

```bash
bash scripts/smoke-test.sh
```

覆盖需求文档 §19 的关键验收要点，并确认首页内容管理接口已下线：

| 分组 | 覆盖内容 |
|---|---|
| CSRF | 缺失/错误 token 的写请求返回 1006；GET 不受影响 |
| 注册登录 | 无验证码全流程、重复邮箱、密码不一致、未同意隐私、失败 5 次锁定、退出后 401 |
| 投递前置校验 | 缺手机号/简历时不可投递并返回缺失项 |
| 快照 | 改在线简历、换附件后历史投递内容不变 |
| 撤回与重投 | 三种状态可撤回、出结论后 3006、重投 attempt_no 递增、超 3 次 3007 |
| 多 HR 归属 | 普通 HR 读全局可、写他人负责的返回 3008、管理员接口 1007 |
| 状态机 | 非法转换 3005、版本冲突 3004、结论可撤销、HR 不能代撤回 |
| 撤回只读 | 撤回记录 readOnly、写操作 3005、默认列表不含撤回 |
| 职位与字典 | 按类型必填校验、状态转换限制、字典重名 5002、不可删除 5001 |
| 首页内容已移除 | `/api/public/site-content`、`/api/admin/site-content`、`/api/admin/site-assets`、`/api/public/assets/{id}` 均返回 1004 |
| 权限边界 | 求职者访问 HR 接口 1003、未登录 1002、越权查看他人投递被拦截 |

浏览器端已验证的流程（截图见 `output/playwright/`）：首页七区块渲染、未登录点投递→登录→自动继续投递→确认弹窗、我的投递与撤回入口、HR 工作台、投递详情（快照 + 状态时间线 + 内部备注）。

## 关键实现说明

### 投递快照（需求 D6）

投递时在同一事务内冻结四份内容，写入后永不修改：

- `resume_snapshot`：在线简历 JSON 副本
- `resume_file_id`：指向不可变的 `resume_file` 记录（附件多版本，只切换 `is_current`）
- `job_snapshot`：职位内容副本 + 当时的 `job.version`
- `candidate_snapshot`：投递时的姓名、手机号、邮箱

HR 端一律展示快照；职位被改动后详情页显示「职位已被修改」。

### 撤回与重投（需求 D8）

唯一约束为 `(candidate_id, job_id, attempt_no)`，`attempt_no` 由服务端算成「已有记录数 + 1」。并发重复提交会撞同一个 `attempt_no`，由唯一约束拦截并转成 3002，不会返回 500。同一职位累计最多 3 条记录。

### 权限三层（需求 D10）

1. 角色：拦截器按路径前缀 fail-closed（`/api/admin/**`、`/api/hr/**`、`/api/candidate/**`）
2. 管理员位：`/api/admin/**` 额外校验 `hr_admin`，越权 1007
3. 数据归属：写操作在 Service 内按 `job.owner_hr_id` 校验，越权 3008；管理员豁免

普通 HR 可以读全部数据，无权限的按钮置灰并提示原因，而不是隐藏。

### 文件存储边界（需求 D7、D13）

系统内只有简历附件一类文件：

| 类别 | 目录 | 访问方式 |
|---|---|---|
| 简历附件 | `storage/resume/{candidateId}/` | 只能通过鉴权接口下载；HR 必须经 `applicationId` 关联，不能按 fileId 直下 |

数据库只保存 `storage_key`，任何响应体都不返回该字段。简历做「大小 + 扩展名 + 文件头」三重校验。首页配图属于前端静态资源，不经过后端存储。

## 与需求文档的已知偏差

| 项 | 文档要求 | 实现情况 | 原因 |
|---|---|---|---|
| 富文本 | 岗位职责/任职要求为富文本 | 用 jsoup 白名单过滤后按纯文本 + 换行渲染 | Demo 阶段未接入富文本编辑器，安全优先 |
| 姓名字段 | `candidate_profile.name` | 同时写入 `sys_user.name` 并在一个事务内同步 | HR 账号也需要显示名，避免两处数据分叉 |
| 限流 | 最小限流 | 内存实现 | 单实例部署，与 Q5 的部署形态待确认一致 |
| 首页视觉 | P1 含视差、粒子、地图动效 | 仅实现基础渐入与 SVG 节点脉冲 | 属于 P1 范围，未在本次 P0 内交付 |

## 尚未实现（需求 §4.2 明确排除）

邮箱验证、短信验证码、图形验证码、密码自助找回、任何站外通知（邮件/短信/IM）、首页内容管理（后台维护页与站点图片库）、账号自助注销与数据导出、多轮面试、独立面试官角色。

这些排除项带来的后果在界面上均有对应提示：求职者「我的投递」页固定提示不发通知，HR 安排面试后弹窗提醒电话联系并展示候选人手机号，登录弹窗与页脚说明忘记密码需联系管理员。首页文案调整需要改前端配置并重新发布。

## 上线前必须补齐

对应需求文档 §24：简历数据留存期限与清理任务、账号注销与数据导出、隐私政策正式文本、邮件通知、HTTPS 与 `Secure` Cookie、多实例部署时替换为对象存储并外置 Session（Redis）。
