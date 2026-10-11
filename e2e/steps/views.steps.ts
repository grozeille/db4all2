import type { Page } from '@playwright/test';
import { Given, When, Then, expect } from './fixtures';
import { ensureExtraTables, ensureView, gotoTablesList, listCard, snap, viewIdFromUrl, world } from './support';

const gridRows = '.ag-center-cols-container .ag-row';

async function renameColumn(page: Page, column: string, newName: string): Promise<void> {
  await page.locator(`.ag-header-cell[col-id="${column}"]`).click({ button: 'right' });
  await page.getByRole('button', { name: 'Rename column', exact: true }).click();
  const input = page.locator('.modal-body input');
  await expect(input).toBeFocused();
  await input.fill(newName);
  await input.press('Enter');
}

function requireWorld(): { projectId: string; tableId: string } {
  if (!world.projectId || !world.tableId) {
    throw new Error('No table in world: add the project/table "exists" steps first.');
  }
  return { projectId: world.projectId, tableId: world.tableId };
}

Given('the view {string} exists', async ({ page }, name: string) => {
  const { projectId, tableId } = requireWorld();
  world.viewId = await ensureView(page, projectId, tableId, name);
});

When('I open the project tables and views list', async ({ page }) => {
  if (!world.projectId) throw new Error('No project in world: add a "the project ... exists" step first.');
  await gotoTablesList(page, world.projectId);
});

Then(
  'I see the table {string} and the view {string} with their badges',
  async ({ page }, tableName: string, viewName: string) => {
    await expect(listCard(page, tableName, 'Table')).toBeVisible();
    await expect(listCard(page, viewName, 'View')).toBeVisible();
    await snap(page, 'unified-list');
  },
);

When('I show only views', async ({ page }) => {
  await page.getByRole('button', { name: 'Views', exact: true }).click();
});

When('I show only tables', async ({ page }) => {
  await page.getByRole('button', { name: 'Tables', exact: true }).click();
});

When('I show tables and views', async ({ page }) => {
  await page.getByRole('button', { name: 'All', exact: true }).click();
});

Then('the list shows {string} but not {string}', async ({ page }, visible: string, hidden: string) => {
  await expect(page.locator('.card-title', { hasText: visible })).toBeVisible();
  await expect(page.locator('.card-title', { hasText: hidden })).toBeHidden();
  await snap(page, 'filtered-list');
});

When('I search the list for {string}', async ({ page }, text: string) => {
  await page.getByPlaceholder('Search a table or a view...').fill(text);
});

When('I clear the list search', async ({ page }) => {
  await page.getByPlaceholder('Search a table or a view...').fill('');
});

When('I open the view {string}', async ({ page }, name: string) => {
  if (!world.projectId) throw new Error('No project in world: add a "the project ... exists" step first.');
  await page.goto(`/projects/${world.projectId}/tables`);
  await listCard(page, name, 'View').click();
  await page.waitForURL(/\/views\/[^/]+\/content/);
  world.viewId = viewIdFromUrl(page);
});

Then('the breadcrumb shows {string}', async ({ page }, trail: string) => {
  const expected = trail.split(',').map((crumb) => crumb.trim());
  await expect(page.locator('.breadcrumb .breadcrumb-item')).toHaveText(expected);
  await snap(page, 'view-breadcrumb');
});

When('I open the table content', async ({ page }) => {
  const { projectId, tableId } = requireWorld();
  await page.goto(`/projects/${projectId}/tables/${tableId}/content`);
  await expect(page.locator(gridRows).first()).toBeVisible();
});

When(
  'I rename the {string} column to {string} from the table content',
  async ({ page }, column: string, newName: string) => {
    const { projectId, tableId } = requireWorld();
    await page.goto(`/projects/${projectId}/tables/${tableId}/content`);
    await expect(page.locator(gridRows).first()).toBeVisible();
    await renameColumn(page, column, newName);
  },
);

When('I rename the {string} column to {string}', async ({ page }, column: string, newName: string) => {
  await renameColumn(page, column, newName);
});

When('I save the table as view {string}', async ({ page }, name: string) => {
  await page.getByRole('button', { name: 'Save as view' }).click();
  const modal = page.locator('.modal-content');
  await modal.locator('.modal-body input.form-control').first().fill(name);
  await modal.getByRole('button', { name: 'Save view' }).click();
  await page.waitForURL(/\/views\/[^/]+\/content/);
});

Then(
  'the grid shows the {string} column and a {string} step',
  async ({ page }, column: string, step: string) => {
    await expect(page.locator('.ag-header-cell-text', { hasText: column }).first()).toBeVisible();
    await expect(page.locator('.list-group-item', { hasText: step }).first()).toBeVisible();
    await snap(page, 'column-renamed');
  },
);

When('I collapse the transformation steps panel', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Transformation steps' })).toBeVisible();
  await page.locator('button[title="Hide transformation steps"]').click();
});

Then('the grid stays visible without the steps panel', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Transformation steps' })).toBeHidden();
  await expect(page.locator(gridRows).first()).toBeVisible();
  await snap(page, 'steps-collapsed');
});

When('I expand the transformation steps panel', async ({ page }) => {
  await page.locator('button[title="Show transformation steps"]').click();
});

Then('the transformation steps panel is visible again', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Transformation steps' })).toBeVisible();
  await snap(page, 'steps-expanded');
});

Given('the project has {int} extra tables', async ({ page }, count: number) => {
  if (!world.projectId) throw new Error('No project in world: add a "the project ... exists" step first.');
  await ensureExtraTables(page, world.projectId, count);
});

When('I go to the second page', async ({ page }) => {
  await expect(page.getByText('Page 1 of 2', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Next', exact: true }).click();
  await expect(page.getByText('Page 2 of 2', { exact: true })).toBeVisible();
});

Then('the pagination shows {string}', async ({ page }, text: string) => {
  await expect(page.getByText(text, { exact: true })).toBeVisible();
  await snap(page, 'pagination-reset');
});

When('I click on the {string} step', async ({ page }, label: string) => {
  await page.locator('ol.list-group').getByText(label, { exact: true }).first().click();
});

When('I start a new filter', async ({ page }) => {
  await page.getByRole('button', { name: 'Cancel', exact: true }).click();
});

Then('the query builder is ready for a new filter', async ({ page }) => {
  await expect(page.getByRole('button', { name: 'Apply', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Update filter', exact: true })).toBeHidden();
  await expect(page.locator('.query-group input')).toHaveCount(0);
  await snap(page, 'filter-not-editable');
});

Then(
  'the query builder shows a filter on {string} with value {string}',
  async ({ page }, column: string, value: string) => {
    await expect(page.getByRole('button', { name: 'Update filter', exact: true })).toBeVisible();
    await expect(page.locator('.query-group select').nth(1)).toHaveValue(column);
    await expect(page.locator('.query-group input')).toHaveValue(value);
    await snap(page, 'filter-editing');
  },
);

When('I change the filter value to {string} and update', async ({ page }, value: string) => {
  await page.locator('.query-group input').fill(value);
  await page.getByRole('button', { name: 'Update filter', exact: true }).click();
});

Then('there is a single {string} step', async ({ page }, label: string) => {
  await expect(page.locator('ol.list-group').getByText(label, { exact: true })).toHaveCount(1);
  await snap(page, 'filter-updated');
});
