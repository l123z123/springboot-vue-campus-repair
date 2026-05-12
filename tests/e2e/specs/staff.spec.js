const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, staff } = require('../fixtures/test-data')

test.describe('Staff Full Flow', () => {
  test.use({ baseURL })

  test('staff workbench and order management', async ({ page }) => {
    // 1. 登录（维修工跳转 /staff/workbench）
    await login(page, staff.username, staff.password, staff.role)
    await expect(page).not.toHaveURL(/\/login/)

    // 2. 工作台统计卡片
    await page.goto('/staff/workbench')
    await expect(page.locator('.staff-workbench')).toBeVisible()
    await expect(page.locator('.stat-card').first()).toBeVisible()

    // 3. 工单管理
    await page.goto('/staff/tickets')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table, .staff-workbench')).toBeVisible()

    // 4. 接单确认弹窗
    const acceptBtn = page.locator('button:has-text("接单")')
    if (await acceptBtn.isVisible().catch(() => false)) {
      await acceptBtn.first().click()
      await page.waitForTimeout(500)
      // 关闭确认弹窗（测试中取消操作）
      const cancelBtn = page.locator('.el-message-box__close, button:has-text("取消")')
      if (await cancelBtn.isVisible().catch(() => false)) {
        await cancelBtn.click().catch(() => {})
      }
    }

    // 5. 消息
    await page.goto('/staff/message')
    await page.waitForTimeout(500)

    // 6. 公告
    await page.goto('/staff/notice')
    await expect(page.locator('.notice-page')).toBeVisible()

    // 7. 个人中心
    await page.goto('/profile')
    await expect(page.locator('.profile-page')).toBeVisible()

    console.log('Staff full flow: PASS')
  })
})
