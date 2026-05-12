const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { submitRepair } = require('../helpers/repair')
const { baseURL, student, repairOrder } = require('../fixtures/test-data')

test.describe('Student Full Flow', () => {
  test.use({ baseURL })

  test('complete student repair lifecycle', async ({ page }) => {
    // 1. 登录
    await login(page, student.username, student.password, student.role)
    await expect(page).not.toHaveURL(/\/login/)

    // 2. 提交报修
    await submitRepair(page, repairOrder)

    // 3. 查看我的工单列表
    await page.goto('/orders')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row, .recent-item, .el-empty').first()).toBeVisible()

    // 4. 个人中心
    await page.goto('/profile')
    await expect(page.locator('.profile-page')).toBeVisible()
    const editBtn = page.locator('button:has-text("编辑资料")')
    if (await editBtn.isVisible()) {
      await editBtn.click()
      await page.waitForTimeout(500)
      await page.locator('button:has-text("取消")').first().click().catch(() => {})
    }

    // 5. 帮助反馈
    await page.goto('/help')
    await expect(page.locator('.help-page')).toBeVisible()

    console.log('Student full flow: PASS')
  })
})
