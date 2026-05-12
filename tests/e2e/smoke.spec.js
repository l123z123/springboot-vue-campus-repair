const { test, expect } = require('@playwright/test')
const { login } = require('./helpers/auth')
const { baseURL } = require('./fixtures/test-data')

test.describe('Smoke Tests', () => {
  test.use({ baseURL })

  test('student login and home page loads', async ({ page }) => {
    await login(page, 'test_student', 'Test123456')
    await expect(page).toHaveURL(/\/home/)
    await expect(page.locator('.hero-greeting, .welcome-title')).toBeVisible()
  })

  test('staff login and workbench loads', async ({ page }) => {
    await login(page, 'test_worker', 'Test123456')
    await page.goto('/staff/workbench')
    await expect(page.locator('.staff-workbench')).toBeVisible()
  })

  test('admin login and dashboard loads', async ({ page }) => {
    await login(page, 'admin', 'admin123')
    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    await expect(page.locator('.dash-stat').first()).toBeVisible()
  })
})
