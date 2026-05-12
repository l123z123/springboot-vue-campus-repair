const { expect } = require('@playwright/test')

async function navigateTo(page, menuItem, expectedPath) {
  await page.click(`.el-menu-item:has-text("${menuItem}"), a:has-text("${menuItem}")`)
  if (expectedPath) {
    await page.waitForURL(`**${expectedPath}`, { timeout: 10000 })
  }
}

async function verifyPageLoaded(page, selector) {
  await expect(page.locator(selector)).toBeVisible({ timeout: 10000 })
}

module.exports = { navigateTo, verifyPageLoaded }
