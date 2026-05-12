const { expect } = require('@playwright/test')

// 登录后根据角色跳转不同页面：学生→/home，维修工→/staff/workbench，管理员→/admin/dashboard
const roleHome = { 0: '/home', 1: '/staff/workbench', 2: '/admin/dashboard' }

async function login(page, username, password, role = 0) {
  await page.goto('/login')
  await page.waitForSelector('input[placeholder*="用户名"]', { timeout: 10000 })
  await page.fill('input[placeholder*="用户名"]', username)
  await page.fill('input[placeholder*="密码"]', password)
  await page.click('button:has-text("登录")')
  await page.waitForLoadState('networkidle')
  // 等待成功跳转（不再限定 /home）
  await expect(page).not.toHaveURL(/\/login/, { timeout: 10000 })
}

async function logout(page) {
  // 尝试不同的退出入口
  const menuBtn = page.locator('[aria-label="用户菜单"], .el-dropdown, .user-avatar')
  if (await menuBtn.isVisible().catch(() => false)) {
    await menuBtn.first().click()
    const logoutBtn = page.locator('text=退出登录')
    if (await logoutBtn.isVisible().catch(() => false)) {
      await logoutBtn.click()
      await page.waitForURL('**/login', { timeout: 5000 })
    }
  }
}

module.exports = { login, logout, roleHome }
