# 服装网店退换货处理系统（贯穿式大作业 · 常规赛道）

选题二的可实现工程：**消费者、售后客服、仓库人员**在同一系统里共同处理一次退货或换货，
每个人只看到自己该处理的队列，每一次状态变化都留下岗位、时间和说明。

后端沿用课堂 3-5 Demo（`second_hand_demo`）的分层与运行方式：Spring Boot 3 + MyBatis + MySQL；
前端是 **Vue 3 + Vite + Vue Router + Axios** 工程：先登录，系统按账号识别岗位并进入对应工作台；
构建产物由后端 `static` 目录同源托管，也可以单独跑开发服务器。

---

## 1. 三分钟跑起来

### 1.1 准备数据库

```bash
# 在 MySQL 客户端或终端执行（脚本可反复执行，会重建表并写入演示数据）
mysql -u root -p < backend/database/init.sql
```

数据库名：`aftersales_demo`；端口默认 `8082`，与课堂 Demo（8081）互不冲突，可同时启动。

### 1.2 启动后端

```bash
cd backend
mvn spring-boot:run
```

MySQL 账号密码优先用环境变量注入，不要把真实密码写进文件：

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的密码'
```

### 1.3 跑前端（两种方式）

**方式 A：交付/演示用（构建进后端，只开一个端口）**

```bash
cd frontend
npm install          # 第一次需要，装 vue / vue-router / vite
npm run build        # 产物直接输出到 backend/src/main/resources/static
```

然后启动后端，浏览器访问 `http://localhost:8082/`，先看到登录页：用 `lin / 123456` 登录会直接进入消费者工作台。
`backend/src/main/resources/static/` 里的内容是 `npm run build` 生成的，不手工编辑，随时可以重新生成。

**方式 B：开发用（前端热更新）**

```bash
cd frontend
npm run dev          # 打开 http://localhost:5174
```

Vite 把 `/api` 代理到 `http://localhost:8082`，所以后端要同时在跑；改代码即时生效，不用每次 build。

> 注意启动顺序：**先跑 init.sql，再启动后端**。如果后端先启动，MySQL 连接池会停在失败状态，
> 之后即使建好库也仍然报错，必须重启后端。

### 1.4 前端结构：路由与组件

路由用 hash 模式（地址形如 `/#/detail?id=6`）。原因：产物由 Spring Boot 当静态文件托管，
刷新 `/detail` 这类真实路径后端没有对应文件会 404；hash 模式由前端自己接管，不需要后端写转发。

| 路由 | 视图组件 | 岗位 | 作用 |
| --- | --- | --- | --- |
| `/#/login` | `LoginView.vue` | 公开 | 账号密码登录，按岗位跳转；演示账号一键填入 |
| `/#/` | `HomeView.vue` | 需登录 | 链路说明、三个岗位入口、越权与异常自检 |
| `/#/consumer` | `ConsumerView.vue` | 消费者 | 可申请的商品行 + 我发起的售后单 |
| `/#/apply?itemId=N` | `ApplyView.vue` | 消费者 | 发起退货/换货申请，成功后跳详情 |
| `/#/cs` | `CsView.vue` | 售后客服 | 待处理队列（可按状态筛选）+ 全部单据 |
| `/#/warehouse` | `WarehouseView.vue` | 仓库人员 | 待收货 / 已收货待验货两组 |
| `/#/detail?id=N` | `DetailView.vue` | 三岗位 | 进度、单据信息、仓库记录、时间线、当前身份可做的动作 |

登录与权限的关系：

- 登录成功后后端发一个令牌，前端存在 localStorage，之后每个请求带 `Authorization: Bearer <token>`；
- 路由守卫只判断「有没有登录」，没登录一律回 `/#/login`（并记住原地址）；
- **这个岗位能不能看这一页由后端判断**：直接改地址访问别人的工作台不会被前端硬拦，后端返回 403，页面把原因显示出来；
- 导航栏只显示当前岗位能用的入口，右上角显示账号与岗位，可退出登录。

复用组件：`AppNav`（导航 + 身份切换 + 后端指示灯）、`AfterSaleCard`、`ProgressTrack`、`TimelineList`、
`WarehouseRecordPanel`、`ActionForm`（按动作动态渲染字段）、`StateBox`（加载/空/错误统一表现）、`NoticeBar`。

共享逻辑：`api/client.js` 是所有请求的唯一出口——导出一个配好 `baseURL: '/api'` 的 **Axios 实例**，
请求拦截器统一带上登录令牌 `Authorization: Bearer <token>`，响应拦截器把「成功 / 401 / 4xx-5xx / 连不上」收敛成同一个
`{ ok, status, data, message }`，页面因此不用认识 `AxiosError`，401 时自动清令牌回登录页；
`stores/auth.js` 管令牌与当前账号，`stores/identity.js` 只存枚举字典与演示账号列表，`stores/notice.js` 管全局提示；
`utils/actions.js` 定义「动作 → 接口 + 字段」的对应关系；`utils/labels.js` 负责中文标签与状态配色。

登录后按岗位进入工作台，演示账号（密码统一 `123456`，登录页有一键填入）：

| 演示账号 | 密码 | 姓名 | 岗位 | 登录后进入 |
| --- | --- | --- | --- | --- |
| `lin` | 123456 | 小林 | 消费者 | 消费者工作台 → 申请 → 详情 |
| `hong` | 123456 | 小红 | 消费者 | 消费者工作台 → 详情（补充凭证） |
| `kefu_wang` | 123456 | 王敏 | 售后客服 | 客服工作台 → 详情（审核、终定） |
| `wh_li` | 123456 | 李强 | 仓库人员 | 仓库工作台 → 详情（收货、验货） |

登录身份与页面岗位不一致时页面照常打开，请求被后端拒绝后把原因显示出来——
这正好用来现场演示越权拦截，导航栏下方会出现一条黄色提示。

---

## 2. 完整业务链路（首版必做项全部覆盖）

```text
消费者选择订单商品并申请售后           → PENDING_REVIEW
→ 客服检查申请条件                     → NEED_EVIDENCE（要补材料）/ RETURN_GUIDED（同意退回）/ REJECTED（拒绝）
→ 消费者补充材料或获得退回指引          → 回到 PENDING_REVIEW
→ 仓库登记收货与验货结果                → WAREHOUSE_RECEIVED → INSPECTION_PASSED / INSPECTION_FAILED
→ 客服确认换货、模拟退款或拒绝          → EXCHANGE_DONE / REFUND_DONE / REJECTED
→ 售后单关闭（写 closed_at）
```

| 首版必须完成 | 落在哪里 |
| --- | --- |
| 模拟订单和订单商品数据 | `mock_order`、`order_item` 表，init.sql 造好 3 单 5 行 |
| 退货、换货两种售后类型 | `AfterSaleType` 枚举 |
| 申请原因、数量、说明和图片凭证 | `after_sale.reason_code / quantity / description / evidence_url` |
| 客服审核、退回补充、同意和拒绝 | `POST /api/after-sales/{id}/review` |
| 仓库收货、验货和问题登记 | `POST /api/after-sales/{id}/receive`、`/inspect`，写入 `warehouse_record` |
| 换货时记录模拟补发单号 | `warehouse_record.reissue_no`，由客服在 `/finalize` 填写 |
| 模拟退款结果记录，不接真实资金 | `warehouse_record.refund_amount`、`refund_result='MOCK_SUCCESS'` |
| 售后时间线和各角色工作台 | `after_sale_event` 表 + 前端三个岗位工作台 |

---

## 3. 关键业务规则写在哪一段代码里

| 规则 | 实现位置 | 违反时的接口响应 |
| --- | --- | --- |
| 只能对本人已完成的模拟订单申请售后 | `AfterSaleServiceImpl.create()` 校验 `order.consumerId` 与 `status='FINISHED'` | 403 / 400 |
| 申请数量不能超过可售后数量 | `sumOccupiedQuantity()` 算剩余，`create()` 比较 | 400 |
| 超过售后期限不能申请 | `create()` 比较 `LocalDate.now()` 与 `order_item.after_sale_deadline` | 400 |
| 同一商品行不允许重复申请 | `findInProgressByItemId()` | 409 |
| 客服同意退回后，仓库才能登记收货和验货 | `receive()` / `inspect()` 校验前置状态 | 409 |
| 仓库验货不等于退款或换货完成 | `inspect()` 只推进到 `INSPECTION_PASSED/FAILED`，关闭由 `finalizeDecision()` | —— 设计上分离 |
| 拒绝、要求补充材料、验货异常必须填原因 | `requireReason()`、`abnormalReason` 非空校验 | 400 |
| 每次状态变化保留岗位、时间、说明 | 每个动作调用 `writeEvent()` 写 `after_sale_event` | —— |
| 消费者不能查看他人订单和售后资料 | `detail()` 校验归属；`OrderQueryService` 只按当前消费者查 | 403 |
| 岗位只能做自己环节的操作 | 所有动作先 `requireRole()`，再校验状态 | 403 |
| 未登录不能调用业务接口 | `AuthInterceptor` 校验 `Authorization: Bearer <token>`，无效直接拦下 | 401 |
| 登录身份来自令牌，不来自前端自报 | 拦截器解析令牌换出账号写入 `AuthContext`，Service 只认这个身份 | 401 |

页面上的操作按钮来自后端返回的 `allowedActions`：**前端不自己判断能不能点**，
所以「页面上有这个按钮」等价于「后端确认这个岗位此刻允许这个动作」。

---

## 4. 数据表与字段对应关系

| 表 | 作用 | 对应 Java 对象 | 主要接口 |
| --- | --- | --- | --- |
| `sys_user` | 演示账号与岗位 | `UserEntity` | `GET /api/demo/users` |
| `mock_order` | 已完成的模拟订单 | `MockOrderEntity` | `GET /api/orders/my-items` |
| `order_item` | 订单商品行、售后期限 | `OrderItemEntity` | `GET /api/orders/my-items` |
| `after_sale` | 售后单与状态机 | `AfterSaleEntity` | `POST /api/after-sales` |
| `warehouse_record` | 收货、验货、补发单号、模拟退款 | `WarehouseRecordEntity` | `/receive`、`/inspect`、`/finalize` |
| `after_sale_event` | 时间线（岗位/动作/前后状态/说明/时间） | `AfterSaleEventEntity` | `GET /api/after-sales/{id}` |

`warehouse_record` 单独建表是这门课最想说明的一点：
**「收到退货」「同意退款」「售后结束」是三个不同的业务事实**，发生在不同时间、由不同岗位负责，
塞进一张表就会退化成「状态字段改一下」，看不出协作。

---

## 5. 接口清单

| 方法 | 地址 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 账号密码登录，返回 `{token, user}` | 否 |
| GET | `/api/auth/me` | 用令牌换回当前账号（刷新页面用） | 是 |
| POST | `/api/auth/logout` | 作废令牌 | 是 |
| GET | `/api/health` | 探活 | 否 |
| GET | `/api/demo/users` | 演示账号列表（不含密码） | 否 |
| GET | `/api/demo/meta` | 岗位/类型/原因/状态/动作的中文标签字典 | 否 |
| GET | `/api/orders/my-items` | 我的订单商品行 + 剩余可售后数量 + 能否申请 | 消费者 |
| POST | `/api/after-sales` | 发起退货或换货申请 | 消费者 |
| GET | `/api/after-sales/mine` | 我的售后单 | 消费者 |
| POST | `/api/after-sales/{id}/evidence` | 补充凭证后重新进入审核 | 消费者（本人单据） |
| GET | `/api/after-sales/{id}` | 详情 + 时间线 + 仓库记录 | 三岗位；消费者仅限本人 |
| GET | `/api/after-sales/cs-queue` | 客服待处理队列 | 售后客服 |
| GET | `/api/after-sales/all` | 全部售后单 | 售后客服 |
| POST | `/api/after-sales/{id}/review` | 要求补充凭证 / 同意退回 / 拒绝 | 售后客服 |
| POST | `/api/after-sales/{id}/finalize` | 确认换货 / 确认退款 / 异常后拒绝 | 售后客服 |
| GET | `/api/after-sales/warehouse-queue` | 仓库待处理队列 | 仓库人员 |
| POST | `/api/after-sales/{id}/receive` | 登记收到退回包裹 | 仓库人员 |
| POST | `/api/after-sales/{id}/inspect` | 登记验货结果 | 仓库人员 |

除登录与公开接口外，请求都要带 `Authorization: Bearer <token>`；岗位不符返回 403，令牌缺失/失效返回 401。

请求体字段与 DTO 一一对应：`AfterSaleCreateDTO`、`ReviewDecisionDTO`、`WarehouseReceiveDTO`、
`InspectionDTO`、`FinalDecisionDTO`、`EvidenceSupplementDTO`。
失败统一返回 `{"message": "中文原因", "status": "400|401|403|404|409"}`。

---

## 6. 六种核心验收场景怎么当场演示

| 验收场景 | 演示步骤（页面级） | 期望结果 |
| --- | --- | --- |
| 1 消费者对自己的订单商品发起退货或换货 | `/#/consumer` 点 T 恤行「申请售后」→ `/#/apply?itemId=N` 选换货、1 件 → 提交 | 跳 `/#/detail?id=N`，状态「待客服审核」，时间线 1 条 |
| 2 超出可申请数量或重复申请被拒 | 申请数量填 5（剩余只有 2）→ 提交 | 400「申请数量超过可售后数量，最多还能申请 2 件」；改 1 件对已有进行中单的商品行再申请 → 409 |
| 3 客服要求补充材料，消费者补充后重新审核 | `kefu_wang` 在 `/#/cs` 打开待审核单 → 「要求补充凭证」→ 切 `hong`/`lin` 在详情页补凭证 | 状态回到「待客服审核」，时间线增加两条 |
| 4 未经客服同意，仓库不能提前登记退回商品 | 用 `wh_li` 打开一张「待客服审核」的单（首页自检第三个按钮同效） | 409「客服还未同意退回，仓库不能提前登记收货」 |
| 5 仓库验货异常后，客服继续处理并留下结论 | `/#/warehouse` → 收货 → 验货结果选「验货异常」→ 填原因 → 切 `kefu_wang` 在详情页确认退款 | 验货异常无原因先被 400 拦下；客服结论为空也被 400 拦下；填齐后变「退货完成」 |
| 6 普通消费者不能查看他人订单和售后资料 | 用 `lin` 打开 `/#/detail?id=2`（小红的单），或直接访问 `/#/cs` | 403「消费者只能查看自己的订单和售后资料」；客服页显示「这一步应由『售后客服』处理」 |

三种非正常状态也都能在页面上看到：
**空数据**（仓库处理完后 `/#/warehouse` 两个分组都是空状态框）、
**无权限**（上表场景 6）、**后端不可用**（导航栏右上角小圆点变红，列表显示「连不上后端」）。

### 6.1 三条交互约定的实测证据

| 约定 | 实现方式（Vue 版） | 实测结果 |
| --- | --- | --- |
| 失败时保留输入并说明原因 | `ActionForm.vue` / `ApplyView.vue` 失败时只写本地 `errorText`，不重建表单，`v-model` 的内容留在原处；文案带 HTTP 状态码与后端中文原因 | 数量填 5 提交 → 400，数量与说明原样保留；验货异常不填原因 → 400，原因框仍可继续编辑 |
| 重复处理不额外产生结果 | 前端：`busy` 状态期间提交按钮 `:disabled`，并在入口再判一次；后端：状态机前置校验 + `warehouse_record.after_sale_id` 唯一索引（`DuplicateKeyException` 翻译成 409） | 对同一张单连点三次「同意退回」，Network 里只有 1 个 POST，时间线只多 1 条，状态只推进一次 |
| 刷新后状态与历史一致 | 单据 id 放在路由参数（`/#/detail?id=6`），身份存 localStorage，进度与时间线每次进入页面都重新 GET | F5 后仍是同一张单、同一身份、同一状态与同样的 2 条时间线 |

> 这三条最初是在原生 HTML 版本上逐条实测通过的（见第 11 节记录），改成 Vue 后机制一一对应，
> 请求仍然只走 `api/client.js`，判断仍然只在后端 Service。


---

## 7. 状态自检：一次状态变化的三处证据

以「仓库登记收货」为例，答辩时按这条链讲：

1. 页面：卡片状态从 `客服已同意，待仓库收货` 变成 `仓库已收货，待验货`；
2. 接口：F12 Network 里 `POST /api/after-sales/3/receive` 返回 200，响应体 `status='WAREHOUSE_RECEIVED'`，
   `allowedActions` 同步只剩 `INSPECT`；
3. 数据库：
   ```sql
   SELECT id, status, updated_at FROM after_sale WHERE id = 3;
   SELECT * FROM warehouse_record WHERE after_sale_id = 3;
   SELECT operator_role, action, from_status, to_status, note, created_at
     FROM after_sale_event WHERE after_sale_id = 3 ORDER BY id;
   ```

---

## 8. 已知限制（不能直接投入生产的原因）

| 限制 | 说明与后续方向 |
| --- | --- |
| 没有真实认证体系（演示级登录） | 密码明文存库、令牌放内存（重启即失效）、没有刷新令牌与权限注解；生产要换 BCrypt + JWT/Redis 会话 + Spring Security |
| 图片凭证是假路径 | `evidence_url` 只存字符串，没有文件上传与存储 |
| 退款、补发单号是模拟 | 不接支付、快递与仓储系统，补发单号不做真实性校验 |
| 售后单号用时间戳生成 | 高并发下可能重复，生产要用发号器或数据库序列 |
| 表之间没有外键约束 | 靠 Service 保证一致性，便于反复导入演示数据 |
| 未做多商品行合并、部分退款、运费与优惠分摊 | 属于「首版不做」清单 |
| 并发处理未测 | 两个客服同时处理同一张单目前会都通过，需要乐观锁（`version` 字段）才能保证只成功一次 |
| 无自动化测试与 Git 过程记录 | 第六章之后的章节交付内容，本 README 先留出接口与场景清单作为测试用例来源 |

---

## 9. 目录结构

```text
after_sale_demo/
├── Dockerfile                               三阶段构建：Vue → Spring Boot jar → JRE 运行镜像
├── docker-compose.yml                       MySQL 8 + 应用，一条命令起整套
├── .dockerignore                            构建上下文瘦身（不含 node_modules / target / 生成物）
├── scripts/                                 pull-and-run.ps1 / pull-and-run.sh：拉镜像 + 起容器 + 等健康
├── backend/
│   ├── database/init.sql                    建库建表 + 演示数据（可反复执行）
│   └── src/main/
│       ├── java/com/example/aftersales/
│       │   ├── AfterSaleApplication.java    启动类
│       │   ├── auth/                        演示身份拦截器与上下文
│       │   ├── config/WebConfig.java        拦截器注册 + CORS
│       │   ├── controller/                  接口层（3 个 Controller）
│       │   ├── dto/                         请求入参（6 个 DTO）
│       │   ├── entity/                      与数据表一一对应（6 个 Entity）
│       │   ├── enums/                       岗位、类型、状态、动作、原因、验货结果
│       │   ├── exception/                   统一业务异常与全局处理
│       │   ├── mapper/                      MyBatis 接口
│       │   ├── service/ + service/impl/     业务规则与状态机所在
│       │   └── vo/                          给页面用的视图对象
│       └── resources/
│           ├── application.yml
│           ├── mapper/*.xml                 SQL 写在 XML 里
│           └── static/                      同一份前端，后端直接托管
├── frontend/                                Vue 3 + Vite 工程
│   ├── index.html                           Vite 入口（只挂 #app）
│   ├── package.json  vite.config.js         依赖与构建配置（dev 代理 / api、build 输出到后端 static）
│   └── src/
│       ├── main.js  App.vue                 应用入口与外壳（导航、通知、页脚）
│       ├── router/index.js                  六条路由，hash 模式，meta.role 标注岗位
│       ├── views/                           LoginView / HomeView / ConsumerView / ApplyView / CsView / WarehouseView / DetailView
│       ├── components/                      AppNav AfterSaleCard ProgressTrack TimelineList
│       │                                    WarehouseRecordPanel ActionForm StateBox NoticeBar
│       ├── api/client.js                     Axios 实例 + 拦截器：带 Bearer 令牌、统一整理状态码与中文原因
│       ├── stores/                          auth.js（令牌与当前账号）、identity.js（字典）、notice.js（全局提示）
│       ├── utils/                           actions.js（动作→接口+字段）、labels.js、format.js、useLoader.js
│       └── styles/main.css                  六个视图共用样式
└── README.md
```

`backend/src/main/resources/static/` 是 `npm run build` 的输出目录（生成物，不进手写代码）。

---

## 10. 与大作业各章交付的对应关系

| 章节 | 交付物 | 本工程里的位置 |
| --- | --- | --- |
| 第一章 | 项目方向卡 | 选题二：服装网店退换货；本 README 第 2、3 节可摘录 |
| 第二章 | 项目需求说明 | README 第 2、3、6 节（链路、规则、验收场景、首版范围与不做项） |
| 第三章 | 可实现设计 | 第 4 节（表与对象映射）、第 5 节（接口清单）、第 1.4 节（页面结构）、`enums/AfterSaleStatus`（状态机） |
| 第四章 | 核心功能编码 | 本工程的 controller/service/mapper/entity/dto + init.sql + 演示账号 |
| 第五章 | AI 协作与版本记录 | 待补：AI 任务说明、差异检查、Git 提交与一次越界修改复盘 |
| 第六至八章 | 联调、Review、测试 | 第 6 节场景表 + 第 11 节实测记录（含已修复缺陷与未覆盖项） |
| 第九至十章 | 部署、演示、交付 | 第 1、7、8 节（初始化可复现、证据链、已知限制） |

---

## 11. 实测记录（2026-10-19，浏览器逐步操作完成）

这一节是第六章 Review 与测试的素材：跑通了什么、发现了什么、怎么修的。

### 11.1 已跑通的链路

| 链路 | 结果 |
| --- | --- |
| 换货：`apply.html` 提交 → 客服同意退回 → 仓库收货 → 验货通过 → 客服确认换货 | 终态 `EXCHANGE_DONE`，补发单号落 `warehouse_record.reissue_no`，时间线 5 条 |
| 退货：客服已同意 → 仓库收货 → **验货异常** → 客服确认退款 | 终态 `REFUND_DONE`，`refund_amount=179`、`refund_result='MOCK_SUCCESS'`，时间线 5 条 |
| 补充凭证：客服要求补充 → 消费者补凭证 | 状态回到 `PENDING_REVIEW`，时间线 3 条 |
| 队列与权限：客服 3 条待处理 / 全部 6 张；仓库处理完后两组均空状态 | 岗位队列正确，按状态筛选可用 |

### 11.2 实测发现并已修复的缺陷

| 缺陷 | 现象 | 根因 | 修复 |
| --- | --- | --- | --- |
| 仓库登记收货 500 | `POST /receive` 返回「服务器处理失败：MyBatisSystemException」 | `WarehouseRecordMapper.xml` 的 `insertReceived` 用了 `useGeneratedKeys keyProperty="id"`，但入参是多个 `@Param`，MyBatis 无法决定把自增主键回填给谁 | 去掉 `useGeneratedKeys`（业务用不到这个 id），并在 XML 注释里写明原因 |
| 页面端口写死 | 换端口部署时前端仍请求 `localhost:8082` | 用 `window.location.port === '8082'` 判断是否同源 | 改为「只要是 http(s) 提供的页面就用同源 `/api`」 |
| 静态资源缺失被报成 500 | 浏览器缓存了已删除的 `app.js`，请求返回 500 | 兜底 `@ExceptionHandler(Exception.class)` 把 `NoResourceFoundException` 也当成服务端错误 | 单独处理该异常返回 404 |
| 后端在建库前启动就永久失败 | 建好库后接口仍报 `MyBatisSystemException` | 连接池停在启动时的失败状态 | 在 README 1.3 写明「先建库再启动」的顺序要求 |

### 11.3 前端后续变更：原生 HTML 多页面 → Vue 3 + Vite + Router

六个页面重写为六个视图组件（`src/views/`），共用组件与请求层收敛到 `src/components/`、`src/api/client.js`、
`src/stores/`。接口契约、状态机、权限判断没有改动，Vue 版只是换了展示层实现方式。

**Vue 版已重跑全部场景（2026-10-23，`http://localhost:8082/#/...`）**：

| 场景 | 结果 |
| --- | --- |
| 消费者申请（含数量超范围被 400、输入保留） | `/#/apply?itemId=6` 填 5 件 → 400「最多还能申请 2 件」，数量与说明留在原处 |
| 客服审核（必填指引、连点三次只生效一次） | 空指引被前端必填拦截；连点三次提交 → Network 只有 1 个 POST、时间线只多 1 条 |
| 仓库收货与验货（异常无原因被 400） | 异常不填原因 → 400 且原因文本保留；填「验货通过」后进入待客服确认 |
| 客服终定：模拟退款 | 金额留空 → 前端必填拦截；填 999 → 400「不能超过该行商品成交金额 179.00 元」；填 179 + 结论 → `REFUND_DONE` |
| 客服终定：换货补发 | 补发单号留空 → 必填拦截；填写后 → `EXCHANGE_DONE`，单号与结论落 `warehouse_record` |
| 拒绝分支（审核阶段 / 验货异常后） | 审核阶段走 `/review`，进度条把中间三个环节标为「未走此环节」；验货异常后走 `/finalize`，结论必填 |
| 消费者补充凭证 | 空提交 → 400；补充后状态回到「待客服审核」，时间线 3 条 |
| 越权拦截 | 消费者打开他人单据 → 403；消费者打开客服页 → 403 且岗位提示条说明原因 |
| 空数据 | 仓库任务处理完后两个分组都显示空状态框 |
| 刷新一致性 | 详情页硬刷新后仍是同一单据、同一身份、同一状态与同样的 3 条时间线 |

随后请求层由原生 `fetch` 换成 **Axios**（`src/api/client.js`）：`axios.create({ baseURL: '/api', timeout: 10000 })`，
请求拦截器统一写 `Authorization: Bearer <token>`，响应拦截器把成功、401、4xx/5xx、连不上收敛成同一个 `{ ok, status, data, message }`，
页面与组件没有改一行。改完后复测：GET 列表、POST 审核动作、403 越权提示都正常；打包体积从 133 KB 增至 188 KB。

**登录改造（2026-10-23）**：账号表加 `password`（演示统一 123456），新增 `POST /api/auth/login`、`GET /api/auth/me`、
`POST /api/auth/logout`，拦截器由「读 `X-Demo-User` 头」改为「解 `Bearer` 令牌」，前端新增登录页与路由守卫。
已验证：
- 接口层：错误密码 → 401「账号或密码不正确」；正确登录返回 `token + user`（不含密码）；
  无令牌访问业务接口 → 401；旧的 `X-Demo-User` 头已失效（同样 401）；消费者令牌访问客服接口 → 403；
- 页面层：访问根路径被守卫送到 `/#/login`（记住原地址）；错误密码时输入保留；点演示账号卡片填入后登录，
  客服账号直接进入 `/#/cs`，导航只显示「项目首页 + 客服工作台」；客服改地址访问 `/#/consumer` → 403 + 岗位提示条；
  带令牌提交「要求补充凭证」成功；退出登录后回到登录页、令牌被清空、导航入口隐藏。

**登录后的全链路回归（2026-10-26）**：严格按三个账号轮流登录走完整条换货链路——

1. `lin` 登录 → 消费者工作台 → 对 T 恤行申请换货 1 件 → 新单 `待客服审核`；
2. 退出 → `kefu_wang` 登录 → 客服工作台 → 同意退回并填指引 → `客服已同意，待仓库收货`；
3. 退出 → `wh_li` 登录 → 仓库工作台 → 登记收货 → 验货通过 → `验货通过，待客服确认结果`（仓库此后无按钮）；
4. 退出 → `kefu_wang` 登录 → 确认换货：补发单号留空被必填拦截，填写后 → `换货完成`；
5. 详情页硬刷新 → 仍是同一状态、5 条时间线、5 个进度时间点；
6. 客服改地址访问 `/#/consumer` → 403 + 岗位提示条；
7. 数据库核对：`status=EXCHANGE_DONE`、`closed_at` 非空、`reissue_no=SF20261026001`、结论写入、5 条事件；
8. 换回 `lin`：该商品行「购买 2 件 / 已占用 1 件」，仍可再次申请。

过程中发现并修复一个真实缺陷：**退出后立即换账号登录时，旧令牌的在途请求晚一步返回 401，
会把刚拿到的新令牌一起清掉**（表现为刚登录就被踢回登录页）。修复方式是 401 处理时比对「发出该请求时用的令牌」
与「当前存储的令牌」，只有一致才清空并跳登录页。

**剩余分支补测（2026-10-29）**：

| 分支 | 结果 |
| --- | --- |
| 审核阶段拒绝 | 新建单 `待客服审核` → 客服填理由拒绝 → `申请已拒绝`，进度条把中间三环节标为「未走此环节」，接口走 `/review` |
| 消费者补充凭证 | `待消费者补充凭证` → 补充凭证地址 + 说明 → 回到 `待客服审核`，时间线 3 条 |
| 重复提交 | 仓库「登记收货」连点 3 次 → 状态只推进一次、时间线只多 1 条、`warehouse_record` 只有 1 行 |
| 验货异常 | 选「验货异常」不填原因 → 400「验货异常必须填写原因」；填写后 → `验货异常，待客服处理`，仓库无后续按钮 |
| 验货异常后拒绝 | 客服在该状态走 `/finalize` 拒绝，结论必填 → `申请已拒绝`，验货结果 `ABNORMAL`、原因与结论都落库 |
| 空状态 | 仓库队列处理完后两组都显示「空」图标 + 说明文案 |
| 越权 | 客服访问消费者页、仓库页 → 403，页面显示错误图标与后端原因 |

**界面优化（同一轮）**：品牌化导航（标识 + 系统名 + 副标题）、登录页改成「左侧链路说明 + 右侧登录表单」两栏并给出四步流程、
售后单卡片加状态色条与「下一步」标签、详情页顶部加状态横幅（终态提示关闭时间）、时间线改带序号、
动作按钮把主要动作高亮为主按钮、空/错状态加图标；同时移除了界面里的 emoji，全部换成文字标签。

### 11.4 还没覆盖的部分

- **并发**：两个岗位同时点同一个动作，目前靠状态校验 + 唯一索引兜底，未压测；需要乐观锁才算真正解决。
- **自动化测试**：全部为手工点击验证，第六章要求补 JUnit / 接口自动化用例。
- **性能与无障碍**：未做列表分页与键盘操作检查，单据量到几百条时队列接口需要分页。

---

## 12. Docker 运行与镜像分发

同一份代码支持两种交付方式，答辩时按现场条件选：

| 方式 | 需要什么 | 命令 |
| --- | --- | --- |
| **git clone 源码** | JDK 17、Maven、Node 20、本机 MySQL | 按第 1 节三步走 |
| **拉取镜像** | 只要 Docker | `docker pull <仓库>/aftersales-demo:1.0.0` 后 `docker run`（见下） |

### 12.1 一条命令起整套（推荐）

```bash
docker compose up -d --build
```

- `mysql`：MySQL 8.0，首次启动自动执行 `backend/database/init.sql`，数据存在命名卷 `mysql-data`；
- `app`：前端已打进镜像（无需 Node/npm），连 `mysql` 服务，等 MySQL 健康后才启动；
- 访问 `http://localhost:8082/`；宿主机数据库端口是 **3307**（避开本机已装的 MySQL）。

日常操作：

```bash
docker compose ps                 # 看两个容器的健康状态
docker compose logs -f app        # 应用日志
docker compose down               # 停止，保留数据
docker compose down -v            # 停止并清空数据库，下次启动重新执行 init.sql
```

想改 root 密码：`MYSQL_ROOT_PASSWORD=你的密码 docker compose up -d`（应用会读同一个变量）。

### 12.2 只运行应用镜像（数据库用已有的）

仓库自带一键脚本，省掉记参数：

```powershell
# Windows：拉镜像 → 起容器 → 等健康检查 → 打印访问地址
.\scripts\pull-and-run.ps1                                   # 默认 aftersales-demo:1.0.0
.\scripts\pull-and-run.ps1 -Image 'ghcr.io/you/aftersales-demo:1.0.0'
.\scripts\pull-and-run.ps1 -DbHost 192.168.1.10 -DbPassword '你的密码'
.\scripts\pull-and-run.ps1 -Build                            # 不拉镜像，改用本地源码构建
```

```bash
# macOS / Linux / Git Bash
./scripts/pull-and-run.sh
IMAGE=ghcr.io/you/aftersales-demo:1.0.0 DB_PASSWORD=你的密码 ./scripts/pull-and-run.sh
BUILD=1 ./scripts/pull-and-run.sh
```

等价的原始命令（脚本做的就是这些）：

```bash
# 构建（或改成 docker pull <registry>/<namespace>/aftersales-demo:1.0.0）
docker build -t aftersales-demo:1.0.0 .

# 运行（Windows / macOS 用 host.docker.internal 指向宿主机 MySQL）
docker run -d --name aftersales-app -p 8082:8082 \
  -e DB_HOST=host.docker.internal -e DB_PORT=3306 \
  -e DB_NAME=aftersales_demo -e DB_USERNAME=root -e DB_PASSWORD=你的密码 \
  aftersales-demo:1.0.0
```

前提：目标库已经执行过 `init.sql`。`DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD / SERVER_PORT`
全部走环境变量，与第 1.2 节本地运行用的是同一套配置。

### 12.3 打标签、推送与拉取

```bash
# 打成带版本和 latest 的标签，推到自己的仓库（Docker Hub / 阿里云 ACR / GitHub GHCR 均可）
docker tag  aftersales-demo:1.0.0 <registry>/<namespace>/aftersales-demo:1.0.0
docker tag  aftersales-demo:1.0.0 <registry>/<namespace>/aftersales-demo:latest
docker push <registry>/<namespace>/aftersales-demo:1.0.0
docker push <registry>/<namespace>/aftersales-demo:latest

# 换台机器：拉镜像 + 起数据库就完事
docker pull <registry>/<namespace>/aftersales-demo:1.0.0
```

镜像内包含：Spring Boot 可执行 jar、Vue 构建产物、健康检查（`/api/health`）、非 root 用户运行；
不包含：数据库、日志卷、任何密码（全部由环境变量注入）。

### 12.4 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| `failed to resolve reference "docker.io/library/mysql:8.0"` | 拉不到 Docker Hub。给 Docker Desktop 配代理（Settings → Resources → Proxies），或在 `daemon.json` 里加 `registry-mirrors`（如 `https://docker.m.daocloud.io`）后重启 Docker |
| app 起来但报 `MyBatisSystemException` / `Communications link failure` | 数据库还没就绪或地址不对。compose 已用健康检查等待；单独 `docker run` 时确认 `DB_HOST` 可达、`init.sql` 已执行 |
| 想重新演示初始数据 | `docker compose down -v` 后再 `up`，或进容器执行 `mysql < /docker-entrypoint-initdb.d/01-init.sql` |
| 端口被占用 | 本机已有 MySQL 时把 compose 里 `3307:3306` 换成其他端口；8082 冲突就改 `app.ports` 左侧 |
