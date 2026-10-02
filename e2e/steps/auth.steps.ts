import { Given, When, Then, expect } from './fixtures';
import { loginAs } from './support';

Given('I am on the login page', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();
});

When('I log in as {string} with password {string}', async ({ page }, email: string, password: string) => {
  await loginAs(page, email, password);
});

Given('I am logged in as {string} with password {string}', async ({ page }, email: string, password: string) => {
  await loginAs(page, email, password);
});

Then('I see the project list', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();
});
