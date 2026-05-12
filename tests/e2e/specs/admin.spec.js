const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, admin } = require('../fixtures/test-data')

test.describe('Admin Full Flow', () => {
  test.use({ baseURL })

  test('admin dashboard and management flow', async ({ page }) => {
    await login(page, admin.username, admin.password)

    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    await expect(page.locator('.dash-stat').first()).toBeVisible()

    await page.goto('/admin/tickets')
    await expect(page.locator('.el-table')).toBeVisible()
    await page.fill('input[placeholder*="工单号"]', 'test')
    await page.click('button:has-text("查询")')

    await page.goto('/admin/users')
    await expect(page.locator('.el-table')).toBeVisible()

    await page.goto('/admin/stats')
    await expect(page.locator('.admin-stats')).toBeVisible()
    await expect(page.locator('.kpi-card').first()).toBeVisible()

    await page.goto('/admin/feedback')
    await expect(page.locator('.el-table, .el-empty')).toBeVisible()

    await page.goto('/admin/settings')
    await expect(page.locator('.admin-settings')).toBeVisible()
    await page.fill('input[placeholder*="公告标题"]', 'E2E测试公告')
    await page.fill('textarea[placeholder*="公告内容"]', '系统运行正常')
    await page.click('button:has-text("发布公告")')
    await expect(page.locator('.el-message--success, .el-message--error')).toBeVisible({ timeout: 10000 })

    await page.goto('/admin/profile')
    await expect(page.locator('.profile-page')).toBeVisible()
  })
})
