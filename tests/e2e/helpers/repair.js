const { expect } = require('@playwright/test')

async function submitRepair(page, data) {
  await page.goto('/publish')
  await page.waitForSelector('.publish-card', { timeout: 10000 })

  await page.click('.el-select:first-of-type')
  await page.click(`.el-select-dropdown__item:has-text("${data.campus}")`)

  await page.waitForTimeout(500)
  await page.click('.el-select:nth-of-type(2)')
  await page.click(`.el-select-dropdown__item:has-text("${data.area}")`)

  if (data.locationDetail) {
    await page.fill('input[placeholder*="详细位置"], input[placeholder*="楼栋"]', data.locationDetail)
  }

  if (data.description) {
    await page.fill('textarea[placeholder*="故障"], textarea', data.description)
  }

  await page.fill('input[placeholder*="手机"]', data.phone || '13800138000')

  await page.click('button:has-text("立即提交")')
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 10000 })
}

async function selectOrderFromList(page, index = 0) {
  await page.goto('/orders')
  await page.waitForSelector('.el-table__row, .recent-item', { timeout: 10000 })
  const rows = page.locator('.el-table__row, .recent-item')
  await rows.nth(index).click()
  await page.waitForURL('**/repair/detail/**', { timeout: 10000 })
}

module.exports = { submitRepair, selectOrderFromList }
