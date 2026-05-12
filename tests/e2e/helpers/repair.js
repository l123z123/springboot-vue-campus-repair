const { expect } = require('@playwright/test')

/**
 * 填写报修表单并提交
 * Element Plus 下拉框：点击触发→等下拉出现→选选项
 */
async function submitRepair(page, data) {
  await page.goto('/publish')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(500)

  // 1. 选择校区
  const campusSelect = page.locator('.publish-form .el-select').first()
  await campusSelect.click()
  await page.waitForTimeout(400)
  await page.locator('.el-select-dropdown__item').filter({ hasText: data.campusLabel }).first().click()
  await page.waitForTimeout(300)

  // 2. 选择区域
  const areaSelect = page.locator('.publish-form .el-select').nth(1)
  await areaSelect.click()
  await page.waitForTimeout(400)
  await page.locator('.el-select-dropdown__item').filter({ hasText: data.areaLabel }).first().click()
  await page.waitForTimeout(300)

  // 3. 填写详细位置
  if (data.locationDetail) {
    await page.locator('input[placeholder*="如："]').fill(data.locationDetail)
  }

  // 4. 填写故障描述
  if (data.description) {
    await page.locator('textarea').first().fill(data.description)
  }

  // 5. 联系电话
  await page.locator('input[placeholder*="手机号码"]').fill(data.phone || '13800138000')

  // 6. 提交
  await page.locator('button:has-text("提交")').first().click()
  await expect(page.locator('.el-message--success, .el-message--error')).toBeVisible({ timeout: 10000 })
}

async function selectOrderFromList(page, index = 0) {
  await page.goto('/orders')
  await page.waitForTimeout(1000)
  const rows = page.locator('.el-table__row, .recent-item')
  const count = await rows.count()
  if (count > index) {
    await rows.nth(index).click()
    await page.waitForLoadState('networkidle')
  }
}

module.exports = { submitRepair, selectOrderFromList }
