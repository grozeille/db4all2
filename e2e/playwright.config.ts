import { defineConfig } from '@playwright/test';
import { defineBddConfig } from 'playwright-bdd';
import * as fs from 'node:fs';
import * as path from 'node:path';

const E2E_PORT = 18080;
// Fresh H2 database per run, so the setup scenario always starts uninitialized
// and runs never pollute the dev database.
const h2File = path.resolve(__dirname, '.data-e2e', `e2e-${Date.now()}`);

function findApiJar(): string {
  const targetDir = path.resolve(__dirname, '..', 'api', 'target');
  let entries: string[] = [];
  try {
    entries = fs.readdirSync(targetDir);
  } catch {
    throw new Error(
      `API target dir not found: ${targetDir}. Build it first: mvn -DskipTests package -P release (from repo root).`,
    );
  }
  const jar = entries
    .filter(
      (f) =>
        f.startsWith('db4all-api-') &&
        f.endsWith('.jar') &&
        !f.endsWith('-sources.jar') &&
        !f.endsWith('-javadoc.jar'),
    )
    .sort()
    .pop();
  if (!jar) {
    throw new Error(
      `API jar not found in ${targetDir}. Build it first: mvn -DskipTests package -P release (from repo root).`,
    );
  }
  return path.join(targetDir, jar);
}

const testDir = defineBddConfig({
  paths: ['features/*.feature'],
  require: ['steps/*.ts'],
});

export default defineConfig({
  testDir,
  // Scenarios share one server + one database and run sequentially.
  workers: 1,
  fullyParallel: false,
  timeout: 120_000,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://127.0.0.1:${E2E_PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // E2E_CHANNEL=chrome uses the system Google Chrome instead of the
    // Playwright-bundled Chromium (for distros it no longer supports).
    channel: process.env.E2E_CHANNEL as 'chrome' | undefined,
  },
  webServer: {
    // The jar serves both the API and the UI statics: one process to boot,
    // exactly like the packaged desktop app.
    command: `java -jar "${findApiJar()}"`,
    cwd: __dirname,
    env: {
      SERVER_PORT: String(E2E_PORT),
      SPRING_DATASOURCE_URL: `jdbc:h2:file:${h2File};DB_CLOSE_DELAY=-1`,
    },
    port: E2E_PORT,
    timeout: 180_000,
    reuseExistingServer: !process.env.CI,
  },
});
