import * as fs from 'node:fs';
import * as path from 'node:path';
import { test as pwTest, type Page } from '@playwright/test';
import { expect } from './fixtures';

/** Screenshot attached to the test output (visible in the HTML report). */
export async function snap(page: Page, name: string): Promise<void> {
  await page.screenshot({ path: pwTest.info().outputPath(`${name}.png`), fullPage: true });
}

/** Absolute path of the repo `sample/` folder, used as datasource root. */
export const SAMPLES_DIR = path.resolve(__dirname, '..', '..', 'sample');

/** Scenario-scoped ids, filled by the `exists` steps below. */
export const world: { projectId?: string; tableId?: string } = {};

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
  const card = page.locator('.card-title', { hasText: name });
  if ((await card.count()) > 0) {
    await card.first().click();
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
  if ((await row.count()) > 0) return;

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
  await page.goto(`/projects/${projectId}/tables`);
  await expect(page.getByRole('heading', { name: /Tables of project/ })).toBeVisible();
  const card = page.locator('.card-title', { hasText: name });
  if ((await card.count()) > 0) {
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
