const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { submitRepair } = require('../helpers/repair')
const { baseURL, student, repairOrder } = require('../fixtures/test-data')

test.describe('Student Full Flow', () => {
  test.use({ baseURL })

  test('complete student repair lifecycle', async ({ page }) => {
    await login(page, student.username, student.password)

    await submitRepair(page, repairOrder)

    await page.goto('/orders')
    await expect(page.locator('.el-table__row, .recent-item').first()).toBeVisible()

    await page.goto('/profile')
    await expect(page.locator('.profile-page')).toBeVisible()

    await page.click('button:has-text("编辑资料")')
    await expect(page.locator('.edit-form')).toBeVisible()
    await page.click('button:has-text("取消")')

    await page.goto('/help')
    await expect(page.locator('.el-collapse, form')).toBeVisible()

    const feedbackTextarea = page.locator('textarea[placeholder*="反馈"], textarea[placeholder*="意见"]')
    if (await feedbackTextarea.isVisible()) {
      await feedbackTextarea.fill('E2E测试反馈：系统运行正常')
      await page.click('button:has-text("提交反馈"), button:has-text("提交")')
    }

    await page.goto('/settings')
    await expect(page.locator('.el-card')).toBeVisible()
  })
})
