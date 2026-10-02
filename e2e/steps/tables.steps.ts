import * as path from 'node:path';
import { Given, When, Then, expect } from './fixtures';
import { SAMPLES_DIR, countCsvWhere, csvColumnIndex, ensureCsvTable, snap, world } from './support';

const gridRows = '.ag-center-cols-container .ag-row';

Given(
  'the CSV table {string} exists from {string} in datasource {string}',
  async ({ page }, name: string, csvFile: string, datasourceName: string) => {
    if (!world.projectId) throw new Error('No project in world: add a "the project ... exists" step first.');
    world.tableId = await ensureCsvTable(page, world.projectId, name, csvFile, datasourceName);
  },
);

When(
  'I create a CSV table named {string} from {string} in datasource {string}',
  async ({ page }, name: string, csvFile: string, datasourceName: string) => {
    if (!world.projectId) throw new Error('No project in world: add a "the project ... exists" step first.');
    world.tableId = await ensureCsvTable(page, world.projectId, name, csvFile, datasourceName);
  },
);

Then('the table shows data with the {string} column', async ({ page }, column: string) => {
  await expect(page.locator('.ag-header-cell-text', { hasText: column }).first()).toBeVisible();
  await expect(page.locator('.ag-cell', { hasText: '@example.fr' }).first()).toBeVisible();
  expect(await page.locator(gridRows).count()).toBeGreaterThan(0);
  await snap(page, 'table-content');
});

When('I filter where {string} equals {string}', async ({ page }, column: string, value: string) => {
  if (!world.projectId || !world.tableId) {
    throw new Error('No table in world: create the table first.');
  }
  await page.goto(`/projects/${world.projectId}/tables/${world.tableId}/content`);
  await expect(page.locator(gridRows).first()).toBeVisible();

  await page.locator('summary').click();
  await page.getByRole('button', { name: '+ Condition' }).click();
  // First select is the AND/OR group operator, second is the field.
  await page.locator('.query-group select').nth(1).selectOption(column);
  await page.locator('.query-group input').fill(value);
  await page.getByRole('button', { name: 'Apply filters' }).click();

  const expected = countCsvWhere(path.join(SAMPLES_DIR, 'clients.csv'), column, value);
  await expect(page.locator(gridRows)).toHaveCount(expected);
});

Then('every visible row has city {string}', async ({ page }, city: string) => {
  const cityIndex = csvColumnIndex(path.join(SAMPLES_DIR, 'clients.csv'), 'city');
  const rows = page.locator(gridRows);
  const count = await rows.count();
  expect(count).toBeGreaterThan(0);
  for (let i = 0; i < count; i++) {
    await expect(rows.nth(i).locator('.ag-cell').nth(cityIndex)).toHaveText(city);
  }
  await snap(page, 'table-filtered');
});
