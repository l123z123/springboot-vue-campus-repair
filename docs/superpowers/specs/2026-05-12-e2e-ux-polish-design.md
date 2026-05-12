# E2E 测试 + UX 打磨设计文档

> 状态：设计完成，待评审 | 日期：2026-05-12

## 目标

针对校园报修管理系统毕设，进行 UX/UI 打磨 + 全流程 E2E 自动化测试，确保答辩演示零故障，前端体验精美完善。

## 推进策略

分层推进（方案 A）：按问题类型分批修复，同类问题统一处理保证一致性，最后写全流程 E2E 测试。

---

## Layer 0 — 基础设施（先修，上层依赖）

### Axios 响应拦截器 (`frontend/src/utils/request.js`)

在响应拦截器中增加统一错误处理：对所有非 401 错误弹出 `ElMessage.error`。各组件中的 catch 块保留不动——它们负责降级 UI 状态（设为空数组等），不再负责错误提示。

### 全局 CSS 变量 (`frontend/src/styles/variables.css` — 新建)

统一定义：
- `--radius-sm: 6px` / `--radius-md: 10px` / `--radius-lg: 16px`
- `--shadow-card: 0 2px 12px rgba(0,0,0,0.06)` / `--shadow-hover: 0 8px 24px rgba(0,0,0,0.10)`
- `--gap-sm: 8px` / `--gap-md: 16px` / `--gap-lg: 24px`

不改变现有组件布局结构，为后续视觉升级提供变量引用。

### Element Plus 主题覆盖 (`frontend/src/styles/element-override.css` — 新建)

通过 CSS 变量覆盖 primary 色值为略深蓝色（`#2563EB`），微调 success/warning/danger 色值。不引入 SCSS 编译。

---

## Layer 1 — 数据反馈标准化

### 补齐 v-loading / 骨架屏

- `StaffWorkbench.vue`：添加 v-loading 指令
- `Notice.vue`：添加 v-loading 指令
- `Home.vue`：已有 v-loading，确认正常运行

### 补齐错误提示

以下组件的 catch 块中补齐 `ElMessage.error`：
- `Home.vue` loadData()
- `StaffWorkbench.vue` loadData()
- `Notice.vue` fetchNotices()
- `Profile.vue` loadData()

### 空状态统一

- 检查所有 `el-empty` 是否都有 `description` 和引导操作按钮
- 无数据的列表页统一使用 `el-empty`（不留空白区域）

---

## Layer 2 — 操作安全

### 状态变更确认弹窗

以下操作补齐 `ElMessageBox.confirm`：
- `OrderDetail.vue`：开始维修、完成维修按钮
- `StaffWorkbench.vue`：接单、完成按钮
- `AdminTickets.vue`：审核驳回按钮

### 按钮 loading 态

- 逐个检查所有 `@click` 异步操作是否绑定 `:loading` 属性
- 防止用户重复点击提交

### 表单验证补齐

- `AdminSettings.vue` 公告表单添加 `el-form` rules（标题必填 + 长度限制，内容必填 + 长度限制）

---

## Layer 3 — 状态一致性

### 工单详情状态清理

- `watch(orderId)` 回调中先重置 `recommendations = []`
- 审核表单状态在 `loadDetail()` 完成后重置（`auditForm.approved`、`auditForm.dispatchMode`）

### 图表资源清理

- `AdminStats.vue` 在 `onUnmounted` 中确认解绑 ECharts resize 监听
- 添加导航守卫：离开统计页时清理未完成的渲染

---

## Layer 4 — 可达性与边界

### 无障碍

- 纯图标按钮添加 `aria-label`（聊天上传图片、通知图标、操作按钮等）
- 通知弹窗内图标按钮添加描述

### 边界情况

- 纯图片无文字报修单提示用户添加描述（`Publish.vue`）
- 移动端侧栏菜单溢出加滚动

---

## Layer 5 — 关键页面视觉升级

### 首页 (Home.vue)

- **Hero Banner**：顶部欢迎区域改为渐变背景（`#2563EB` → `#1D4ED8`），含用户头像 + 动态问候语（按时段：早上好/下午好/晚上好）+ 当日统计数字
- **快捷操作卡片**：3 张卡片（报修、我的工单、联系客服）改为彩色图标 + 标题 + 描述 + 渐变底色，hover 浮升 + 阴影加深
- **最近工单**：卡片式列表替换纯表格，每条带状态标签颜色区分，hover 整行可点击
- 不改变现有 API 接口调用

### 工单详情 (OrderDetail.vue)

- **进度步骤**：El-Steps 替换为自定义步骤条，每个节点带图标+状态色，当前步骤有 pulsing 动画指示
- **信息区**：卡片网格排布（基础信息、报修内容、图片附件），替代逐行展示
- **操作区**：根据角色和状态动态显示浮层操作栏，非当前角色操作区折叠隐藏
- **沟通面板**：聊天气泡左右区分+头像，输入框优化

### 管理员仪表盘 (AdminDashboard.vue)

- **KPI 卡片**：4 张卡片加图标 + 渐变背景，数字滚动入场动画，超时工单红色脉冲提示
- **待处理表格**：SLA 超时标记（>24h 黄色、>48h 红色），行内快捷操作按钮
- 不新增图表组件

### 通用视觉增强

- 全局 loading 的 spinner 改为项目主题色
- 消息提示添加淡入动画
- 按钮 hover/active 态统一

---

## E2E 测试

### 技术选型

`webapp-testing` 技能（Playwright），真实 Chromium 浏览器模拟用户操作，支持截图、录屏、浏览器日志。

### 测试架构

```
tests/e2e/
├── fixtures/
│   └── test-data.js          # 测试账号、测试数据
├── helpers/
│   ├── auth.js               # 登录/登出封装
│   ├── repair.js             # 工单操作封装
│   └── navigation.js         # 页面跳转辅助
├── specs/
│   ├── student.spec.js       # 学生完整流程
│   ├── staff.spec.js         # 维修工完整流程
│   └── admin.spec.js         # 管理员完整流程
└── smoke.spec.js             # 冒烟测试
```

### 测试用例

**冒烟测试** (smoke.spec.js)：
- 学生/维修工/管理员登录 → 首页 200 → 关键元素可见

**学生流程** (student.spec.js)：
1. 注册新账号 → 登录 → 首页加载
2. 提交报修工单（完整表单填写 + 图片上传）
3. 工单列表查看 → 进入详情页
4. 与维修工聊天（发送消息 + 接收显示）
5. 确认维修完成 → 提交星级评价
6. 个人主页查看 → 编辑资料
7. 帮助页提交反馈

**维修工流程** (staff.spec.js)：
1. 登录 → 工作台页面加载
2. 待接单列表查看 → 接受工单
3. 标记维修完成
4. 聊天功能（与学生沟通）
5. 工单历史查看
6. 公告查看

**管理员流程** (admin.spec.js)：
1. 登录 → 仪表盘页面加载
2. 审核工单（通过 + 驳回两个分支）
3. 派单给指定维修工
4. 用户管理（创建用户 + 编辑 + 禁用/启用）
5. 统计页面查看
6. 反馈管理（查看 + 标记已处理）
7. 发布系统公告

### 运行方式

- 测试前启动后端 + 前端服务
- 使用独立测试账号，测试数据在 `test-data.js` 中配置
- 测试用例按顺序执行（学生 → 维修工 → 管理员），模拟完整协作流程
- 关键步骤截图保存到 `tests/e2e/screenshots/`

---

## 不改动的范围

- 后端 API 接口保持不变
- 数据库结构不变
- 路由结构不变
- 业务逻辑不变
