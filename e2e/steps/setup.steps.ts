import { Given, When, Then, expect } from './fixtures';

Given('I am on the setup page', async ({ page }) => {
  await page.goto('/setup');
  await expect(page.getByRole('heading', { name: 'Application Setup' })).toBeVisible();
});

When('I create the administrator account {string} with password {string}', async ({ page }, email: string, password: string) => {
  await page.locator('#email').fill(email);
  await page.locator('#password').fill(password);
  await page.locator('#confirmPassword').fill(password);
  await page.getByRole('button', { name: 'Create Administrator' }).click();
});

Then('I am redirected to the login page', async ({ page }) => {
  await page.waitForURL('**/login');
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
});
