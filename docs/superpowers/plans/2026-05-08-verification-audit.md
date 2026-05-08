# Campus Repair System - 3-Round Verification Audit

> **Goal:** 从答辩老师角度全面验收项目，检查核心业务流程完整性、功能模块实现度、前端UI正确性

**Tech Stack:** SpringBoot 2.7 + Vue 3 + Element Plus + MyBatis-Plus + MySQL

---

## Round 1: Code Audit (Dead Code + Security + Quick Fixes)

### Task 1.1: Remove dead code files
- [ ] Delete `frontend/src/views/admin/DashboardMobile.vue` (not in router, 10 lines)
- [ ] Delete `frontend/src/views/home/SmartHome.vue` (not in router)
- [ ] Delete `frontend/src/views/ProfileEdit.vue` (merged into Profile.vue)
- [ ] Delete `frontend/src/api/booking.js` (stub with Promise.resolve, no real backend)

### Task 1.2: Fix gray "text-muted" → colored tags
- [ ] `StaffWorkbench.vue:35` - `text-muted` 普通 → `el-tag type="info"` 普通
- [ ] `OrderManage.vue:46` - `text-muted` 普通 → `el-tag type="info"` 普通
- [ ] `StaffTickets.vue:35` - `text-muted` 普通 → `el-tag type="info"` 普通

### Task 1.3: Security verification
- [ ] Verify no hardcoded passwords (AuthController checked - removed in R9)
- [ ] Verify SecurityConfig public endpoints match actual controllers
- [ ] Verify JWT filter properly sets roles

### Task 1.4: Remove dead CSS
- [ ] Remove `.text-muted` styles from OrderManage, StaffTickets, StaffWorkbench

---

## Round 2: Backend Business Logic Verification

### Task 2.1: State machine completeness
- [ ] 11 states all defined in RepairOrderStatus enum
- [ ] All transitions validated by canTransitionTo()
- [ ] Audit flow: SUBMITTED→WAITING_AUDIT→WAITING_DISPATCH→DISPATCHED→PROCESSING→COMPLETED→CONFIRMED→CLOSED
- [ ] Rejection: WAITING_AUDIT→REJECTED
- [ ] Cancellation: SUBMITTED/WAITING_AUDIT/WAITING_DISPATCH/DISPATCHED→CANCELLED
- [ ] SLA auto-cancel for stale orders

### Task 2.2: API endpoint completeness
- [ ] All controllers return proper Result<T> responses
- [ ] All admin endpoints under /admin/** protected by hasRole("ADMIN")
- [ ] User endpoints properly filter by role

### Task 2.3: Data integrity
- [ ] Evaluation: only status=7 (CONFIRMED) can submit, then →8 (CLOSED)
- [ ] Feedback: auto-reply when admin marks processed
- [ ] Chat: validates participants belong to order

---

## Round 3: Frontend UI/UX Verification

### Task 3.1: Page-by-page layout check
- [ ] Login/Register/ForgotPassword - auth pages
- [ ] AppLayout + AdminLayout - navigation, sidebar, breadcrumbs
- [ ] Student pages: Home, Publish, OrderManage, OrderDetail, OrderChat, Message, Notice, Help, Profile, Settings
- [ ] Staff pages: StaffWorkbench, StaffTickets
- [ ] Admin pages: Dashboard, Tickets, Users, Stats, FeedbackManage, Settings, Profile

### Task 3.2: Color audit
- [ ] No gray text that looks like "not implemented"
- [ ] All status tags use proper Element Plus tag types (success/danger/warning/info/primary)

### Task 3.3: Build verification
- [ ] Backend: `mvn compile` zero errors
- [ ] Frontend: `npx vite build` zero errors

---

## Defense Teacher Checklist
- [ ] System has all 3 roles with distinct UIs
- [ ] Core repair flow is complete (submit→audit→dispatch→repair→confirm→evaluate)
- [ ] Real-time chat works (WebSocket)
- [ ] Admin dashboard shows real data (no mock)
- [ ] Statistics use real data from DB
- [ ] Feedback system has reply loop
- [ ] Notice system is backend-driven
- [ ] No hardcoded test data in production code
- [ ] Security: password encrypted, JWT auth, role-based access
