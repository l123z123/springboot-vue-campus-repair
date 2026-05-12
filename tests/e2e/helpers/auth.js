const { expect } = require('@playwright/test')

async function login(page, username, password) {
  await page.goto('/login')
  await page.waitForSelector('input[placeholder*="用户名"]', { timeout: 10000 })
  await page.fill('input[placeholder*="用户名"]', username)
  await page.fill('input[placeholder*="密码"]', password)
  await page.click('button:has-text("登录")')
  await page.waitForURL('**/home', { timeout: 10000 })
  await expect(page.locator('.hero-greeting, .welcome-title, .welcome-text')).toBeVisible({ timeout: 5000 })
}

async function logout(page) {
  await page.click('[aria-label="用户菜单"], .user-dropdown')
  await page.click('text=退出登录')
  await page.waitForURL('**/login', { timeout: 5000 })
}

module.exports = { login, logout }
