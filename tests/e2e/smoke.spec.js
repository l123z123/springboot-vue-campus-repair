const { test, expect } = require('@playwright/test')
const { login } = require('./helpers/auth')
const { baseURL, student, staff, admin } = require('./fixtures/test-data')

test.describe('Smoke Tests', () => {
  test.use({ baseURL })

  test('student login and redirect to home', async ({ page }) => {
    await login(page, student.username, student.password, student.role)
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.home-page')).toBeVisible()
  })

  test('staff login and redirect to workbench', async ({ page }) => {
    await login(page, staff.username, staff.password, staff.role)
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.staff-workbench')).toBeVisible()
  })

  test('admin login and redirect to dashboard', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.admin-dashboard')).toBeVisible()
  })
})
