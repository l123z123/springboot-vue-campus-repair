const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, student, admin } = require('../fixtures/test-data')

test.describe('Search & Filter Tests', () => {
  test.use({ baseURL })

  // ========== Admin 工单搜索 ==========

  test('admin search by ticketNo with T prefix', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', 'T90001')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    const text = await page.locator('.el-table__row').first().textContent()
    expect(text).toContain('90001')
  })

  test('admin search by pure number orderId', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '90001')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    const text = await page.locator('.el-table__row').first().textContent()
    expect(text).toContain('90001')
  })

  test('admin search by description keyword', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '空调')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
  })

  test('admin search by location', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '河东')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
  })

  // ========== Admin 状态筛选 ==========

  test('admin status filter tabs', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)

    const statusSelect = page.locator('.filter-item .el-select').first()
    const tabs = ['全部', '待办', '在办', '完工', '结束']

    for (const label of tabs) {
      await statusSelect.click()
      await page.waitForTimeout(300)
      const option = page.locator('.el-select-dropdown__item').filter({ hasText: label })
      if (await option.isVisible().catch(() => false)) {
        await option.click()
        await page.waitForTimeout(300)
        await page.click('button:has-text("查询")')
        await page.waitForTimeout(500)
        const hasRows = await page.locator('.el-table__row').first().isVisible().catch(() => false)
        const hasEmpty = await page.locator('.el-empty').isVisible().catch(() => false)
        expect(hasRows || hasEmpty).toBeTruthy()
      }
    }
  })

  // ========== Student 工单搜索 ==========

  test('student search by location', async ({ page }) => {
    await login(page, student.username, student.password, student.role)
    await page.goto('/orders')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '河东')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    const visible = await page.locator('.el-table__row, .el-empty').first().isVisible()
    expect(visible).toBeTruthy()
  })

  test('student status filter tabs', async ({ page }) => {
    await login(page, student.username, student.password, student.role)
    await page.goto('/orders')
    await page.waitForTimeout(800)

    const filterTabs = ['全部', '待处理', '进行中', '已完成', '已关闭']
    for (const label of filterTabs) {
      const select = page.locator('.filter-bar .el-select').first()
      await select.click()
      await page.waitForTimeout(300)
      const option = page.locator('.el-select-dropdown__item').filter({ hasText: label })
      if (await option.isVisible().catch(() => false)) {
        await option.click()
        await page.waitForTimeout(500)
        const visible = await page.locator('.el-table__row, .el-empty').first().isVisible()
        expect(visible).toBeTruthy()
      }
    }
  })

  // ========== Dashboard 待办 ==========

  test('admin dashboard pending list loads', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/dashboard')
    await page.waitForTimeout(1000)
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    await expect(page.locator('.dash-stat').first()).toBeVisible()
  })

  // ========== 用户管理搜索 ==========

  test('admin user search by username', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/users')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="用户名"]', 'admin')
    await page.locator('input[placeholder*="用户名"]').press('Enter')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
  })

  test('admin user search by realName', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/users')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="用户名"]', '子涵')
    await page.locator('input[placeholder*="用户名"]').press('Enter')
    await page.waitForTimeout(800)
    const text = await page.locator('.el-table__row').first().textContent()
    expect(text).toContain('子涵')
  })

  // ========== 反馈管理搜索 ==========

  test('admin feedback search page loads', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/feedback')
    await page.waitForTimeout(800)
    const searchInput = page.locator('input[placeholder*="反馈内容"]')
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('反馈')
      await searchInput.press('Enter')
      await page.waitForTimeout(800)
    }
    const visible = await page.locator('.el-table, .el-empty').first().isVisible()
    expect(visible).toBeTruthy()
  })
})
