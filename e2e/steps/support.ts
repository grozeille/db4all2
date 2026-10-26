import * as fs from 'node:fs';
import * as path from 'node:path';
import { test as pwTest, type Page } from '@playwright/test';
import { expect } from './fixtures';

/** Screenshot saved to the test output and attached to the HTML report. */
export async function snap(page: Page, name: string): Promise<void> {
  const fileName = `${name}.png`;
  const filePath = pwTest.info().outputPath(fileName);
  await page.screenshot({ path: filePath, fullPage: true });
  await pwTest.info().attach(fileName, { path: filePath, contentType: 'image/png' });
}

/** Absolute path of the repo `sample/` folder, used as datasource root. */
export const SAMPLES_DIR = path.resolve(__dirname, '..', '..', 'sample');

/** Scenario-scoped ids, filled by the `exists` steps below. */
export const world: { projectId?: string; tableId?: string; viewId?: string } = {};

export function projectIdFromUrl(page: Page): string {
  const match = page.url().match(/\/projects\/([^/]+)/);
  if (!match) throw new Error(`No project id in URL: ${page.url()}`);
  return match[1];
}

export function tableIdFromUrl(page: Page): string {
  const match = page.url().match(/\/tables\/([^/]+)/);
  if (!match) throw new Error(`No table id in URL: ${page.url()}`);
  return match[1];
}

export function viewIdFromUrl(page: Page): string {
  const match = page.url().match(/\/views\/([^/]+)/);
  if (!match) throw new Error(`No view id in URL: ${page.url()}`);
  return match[1];
}

/** Card of the unified tables/views list, qualified by its kind badge. */
export function listCard(page: Page, name: string, badge: 'Table' | 'View') {
  return page
    .locator('.card', { has: page.locator('.card-title', { hasText: name }) })
    .filter({ has: page.locator('.badge', { hasText: badge }) });
}

/** Opens the unified tables/views list and waits until results render. */
export async function gotoTablesList(page: Page, projectId: string): Promise<void> {
  await page.goto(`/projects/${projectId}/tables`);
  await expect(page.getByRole('heading', { name: /Tables and views of project/ })).toBeVisible();
  // The spinner unmounts in the same render pass as the results: hidden means settled.
  await page.locator('.spinner-border').waitFor({ state: 'hidden', timeout: 30_000 });
}

export async function loginAs(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill(password);
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();
}

/** Returns the project id, creating the project through the UI if missing. */
export async function ensureProject(page: Page, name: string): Promise<string> {
  await page.goto('/projects');
  await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();
  // No loading marker on this page: click with a timeout instead of an
  // instantaneous count(), which would race the list fetch.
  const opened = await page
    .locator('.card-title', { hasText: name })
    .first()
    .click({ timeout: 10_000 })
    .then(
      () => true,
      () => false,
    );
  if (opened) {
    await page.waitForURL(/\/projects\/[^/]+\/tables/);
    return projectIdFromUrl(page);
  }
  await page.getByRole('button', { name: 'Create' }).click();
  await page.locator('#projectName').fill(name);
  await page.getByRole('button', { name: 'Create' }).click();
  await page.waitForURL(/\/projects\/[^/]+\/settings/);
  await expect(page.getByRole('heading', { name: `Project settings: ${name}` })).toBeVisible();
  return projectIdFromUrl(page);
}

/** Creates the datasource through the settings UI, unless already listed. */
export async function ensureDatasource(
  page: Page,
  projectId: string,
  name: string,
  rootPath: string,
): Promise<void> {
  await page.goto(`/projects/${projectId}/settings`);
  await expect(page.getByRole('heading', { name: /Project settings/ })).toBeVisible();
  const row = page.locator('tbody tr').filter({ hasText: name }).filter({ hasText: rootPath });
  // Rows load separately from the heading: wait instead of counting instantly.
  const found = await expect(row)
    .toBeVisible({ timeout: 10_000 })
    .then(
      () => true,
      () => false,
    );
  if (found) return;

  await page.getByRole('button', { name: 'Add datasource' }).click();
  const modal = page.locator('.modal-content');
  // Name and root-path inputs have no associated <label for>, locate by order.
  await modal.locator('.modal-body input.form-control').nth(0).fill(name);
  await modal.locator('.modal-body input.form-control').nth(1).fill(rootPath);
  await modal.getByRole('button', { name: 'Save' }).click();
  await expect(page.locator('.alert-success', { hasText: 'Datasource created.' })).toBeVisible();
  await expect(
    page.locator('tbody tr').filter({ hasText: name }).filter({ hasText: rootPath }),
  ).toBeVisible();
}

/** Returns the table id, creating the CSV table through the UI if missing. */
export async function ensureCsvTable(
  page: Page,
  projectId: string,
  name: string,
  csvFile: string,
  datasourceName: string,
): Promise<string> {
  await gotoTablesList(page, projectId);
  // Narrow by name first: the card may sit on another page.
  await page.getByPlaceholder('Search a table or a view...').fill(name);
  const card = listCard(page, name, 'Table');
  const found = await card
    .first()
    .waitFor({ state: 'visible', timeout: 10_000 })
    .then(
      () => true,
      () => false,
    );
  if (found) {
    await card.first().click();
    await page.waitForURL(/\/tables\/[^/]+\/content/);
    return tableIdFromUrl(page);
  }

  await page.goto(`/projects/${projectId}/tables/new`);
  // Form labels have no associated <label for>, locate fields by order/type.
  await page.locator('form input').nth(0).fill(name);
  await page.locator('form select').nth(1).selectOption({ label: datasourceName });
  await page.getByPlaceholder('data/customers.csv').fill(csvFile);
  await page.locator('form input').nth(2).fill(';');
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForURL(/\/tables\/[^/]+\/content/);
  return tableIdFromUrl(page);
}

/** Returns the table id, creating the JSON table through the UI if missing. */
export async function ensureJsonTable(
  page: Page,
  projectId: string,
  name: string,
  jsonFile: string,
  jsonPath: string,
  datasourceName: string,
): Promise<string> {
  await gotoTablesList(page, projectId);
  // Narrow by name first: the card may sit on another page.
  await page.getByPlaceholder('Search a table or a view...').fill(name);
  const card = listCard(page, name, 'Table');
  const found = await card
    .first()
    .waitFor({ state: 'visible', timeout: 10_000 })
    .then(
      () => true,
      () => false,
    );
  if (found) {
    await card.first().click();
    await page.waitForURL(/\/tables\/[^/]+\/content/);
    return tableIdFromUrl(page);
  }

  await page.goto(`/projects/${projectId}/tables/new`);
  // Form labels have no associated <label for>, locate fields by order/type.
  await page.locator('form select').nth(0).selectOption({ label: 'JSON file' });
  await page.locator('form input').nth(0).fill(name);
  await page.locator('form select').nth(1).selectOption({ label: datasourceName });
  await page.getByPlaceholder('data/orders.json').fill(jsonFile);
  if (jsonPath) {
    await page.getByPlaceholder('$.orders').fill(jsonPath);
  }
  // Capture the filled JSON configuration form before saving.
  await snap(page, 'table-new-json-form');
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForURL(/\/tables\/[^/]+\/content/);
  return tableIdFromUrl(page);
}

/** Returns the view id, saving the table content as a view through the UI if missing. */
export async function ensureView(page: Page, projectId: string, tableId: string, name: string): Promise<string> {
  await gotoTablesList(page, projectId);
  // Narrow by name first: the card may sit on another page.
  await page.getByPlaceholder('Search a table or a view...').fill(name);
  const card = listCard(page, name, 'View');
  const found = await card
    .first()
    .waitFor({ state: 'visible', timeout: 10_000 })
    .then(
      () => true,
      () => false,
    );
  if (found) {
    await card.first().click();
    await page.waitForURL(/\/views\/[^/]+\/content/);
    return viewIdFromUrl(page);
  }

  await page.goto(`/projects/${projectId}/tables/${tableId}/content`);
  await expect(page.locator('.ag-center-cols-container .ag-row').first()).toBeVisible();
  await page.getByRole('button', { name: 'Save as view' }).click();
  const modal = page.locator('.modal-content');
  await modal.locator('.modal-body input.form-control').first().fill(name);
  await modal.getByRole('button', { name: 'Save view' }).click();
  // Saving redirects straight to the view editor.
  await page.waitForURL(/\/views\/[^/]+\/content/);
  return viewIdFromUrl(page);
}

/**
 * Ensures `count` pagination fixture tables (`extra-01`, ...) exist, creating
 * the missing ones through the API: fast seeding for scenarios that need a
 * second list page. Requires a logged-in page (token read from localStorage).
 */
export async function ensureExtraTables(page: Page, projectId: string, count: number): Promise<void> {
  const token = await page.evaluate(() => window.localStorage.getItem('token'));
  if (!token) throw new Error('No auth token: log in first.');
  const origin = new URL(page.url()).origin;
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

  const tablesRes = await page.request.get(`${origin}/api/v2/projects/${projectId}/tables?size=100`, { headers });
  if (!tablesRes.ok()) throw new Error(`Unable to list tables: ${tablesRes.status()}`);
  const existing = new Set<string>(
    ((await tablesRes.json()) as { content: { name: string }[] }).content.map((table) => table.name),
  );

  const wanted: string[] = [];
  for (let i = 1; i <= count; i++) {
    const name = `extra-${String(i).padStart(2, '0')}`;
    if (!existing.has(name)) wanted.push(name);
  }
  if (wanted.length === 0) return;

  const datasourcesRes = await page.request.get(`${origin}/api/v2/projects/${projectId}/datasources`, { headers });
  if (!datasourcesRes.ok()) throw new Error(`Unable to list datasources: ${datasourcesRes.status()}`);
  const datasource = ((await datasourcesRes.json()) as { id: string; name: string }[]).find(
    (candidate) => candidate.name === 'sample-data',
  );
  if (!datasource) throw new Error('Datasource "sample-data" not found.');

  for (const name of wanted) {
    const created = await page.request.post(`${origin}/api/v2/projects/${projectId}/tables`, {
      headers,
      data: {
        name,
        description: 'e2e pagination fixture',
        datasourceId: datasource.id,
        sourceKind: 'CSV',
        configuration: { path: 'clients.csv' },
      },
    });
    if (!created.ok()) throw new Error(`Unable to create table ${name}: ${created.status()}`);
  }
}

/** Counts `;`-separated CSV rows where `column` equals `value` (header excluded). */
export function countCsvWhere(csvPath: string, column: string, value: string): number {
  const lines = fs.readFileSync(csvPath, 'utf-8').trim().split('\n');
  const headers = (lines[0] ?? '').split(';');
  const index = headers.indexOf(column);
  if (index < 0) throw new Error(`Column ${column} not found in ${csvPath}`);
  return lines.slice(1).filter((line) => line.split(';')[index] === value).length;
}

/** Zero-based column index in the `;`-separated CSV file. */
export function csvColumnIndex(csvPath: string, column: string): number {
  const headers = fs.readFileSync(csvPath, 'utf-8').split('\n')[0].split(';');
  const index = headers.indexOf(column);
  if (index < 0) throw new Error(`Column ${column} not found in ${csvPath}`);
  return index;
}
