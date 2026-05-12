const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, staff } = require('../fixtures/test-data')

test.describe('Staff Full Flow', () => {
  test.use({ baseURL })

  test('staff workbench and order management', async ({ page }) => {
    await login(page, staff.username, staff.password)

    await page.goto('/staff/workbench')
    await expect(page.locator('.staff-workbench')).toBeVisible()
    await expect(page.locator('.stat-card').first()).toBeVisible()

    await page.goto('/staff/tickets')
    await expect(page.locator('.el-table')).toBeVisible()

    const acceptBtn = page.locator('button:has-text("接单")')
    if (await acceptBtn.isVisible()) {
      await acceptBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 3000 })
      await page.click('.el-message-box__close, .el-message-box button:has-text("取消")')
    }

    await page.goto('/staff/message')
    await expect(page.locator('body')).toBeVisible()

    await page.goto('/staff/notice')
    await expect(page.locator('.notice-page')).toBeVisible()

    await page.goto('/profile')
    await expect(page.locator('.profile-page')).toBeVisible()
  })
})
