import { Given, When, Then, expect } from './fixtures';
import { ensureProject, world } from './support';

When('I create a project named {string}', async ({ page }, name: string) => {
  await page.goto('/projects/new');
  await page.locator('#projectName').fill(name);
  await page.getByRole('button', { name: 'Create' }).click();
  await page.waitForURL(/\/projects\/[^/]+\/settings/);
});

Then('the project settings for {string} are displayed', async ({ page }, name: string) => {
  await expect(page.getByRole('heading', { name: `Project settings: ${name}` })).toBeVisible();
});

Given('the project {string} exists', async ({ page }, name: string) => {
  world.projectId = await ensureProject(page, name);
});
