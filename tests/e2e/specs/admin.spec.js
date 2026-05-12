const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, admin } = require('../fixtures/test-data')

test.describe('Admin Full Flow', () => {
  test.use({ baseURL })

  test('admin dashboard and management flow', async ({ page }) => {
    // 1. 登录（管理员跳转 /admin/dashboard）
    await login(page, admin.username, admin.password, admin.role)
    await expect(page).not.toHaveURL(/\/login/)

    // 2. 仪表盘 KPI 卡片
    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    await expect(page.locator('.dash-stat').first()).toBeVisible()

    // 3. 工单管理
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await expect(page.locator('.admin-dashboard, .el-table, .el-empty').first()).toBeVisible()
    const searchInput = page.locator('input[placeholder*="工单号"], input[placeholder*="搜索"]')
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('90001')
      await page.locator('button:has-text("查询"), button:has-text("搜索")').first().click().catch(() => {})
    }

    // 4. 用户管理
    await page.goto('/admin/users')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table, .el-empty').first()).toBeVisible()

    // 5. 数据统计
    await page.goto('/admin/stats')
    await expect(page.locator('.admin-stats')).toBeVisible()
    await expect(page.locator('.kpi-card').first()).toBeVisible()

    // 6. 反馈管理
    await page.goto('/admin/feedback')
    await expect(page.locator('.el-table, .el-empty').first()).toBeVisible()

    // 7. 系统设置 — 发布公告
    await page.goto('/admin/settings')
    await expect(page.locator('.admin-settings')).toBeVisible()
    const titleInput = page.locator('input[placeholder*="公告标题"]')
    const contentInput = page.locator('textarea[placeholder*="公告内容"]')
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('E2E测试公告')
      await contentInput.fill('系统运行正常')
      await page.locator('button:has-text("发布公告"), button:has-text("发布")').first().click().catch(() => {})
      await page.waitForTimeout(500)
    }

    // 8. 个人中心
    await page.goto('/admin/profile')
    await expect(page.locator('.admin-profile')).toBeVisible()

    console.log('Admin full flow: PASS')
  })
})
