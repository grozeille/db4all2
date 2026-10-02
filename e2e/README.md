# db4all e2e tests

End-to-end tests: Gherkin features executed by the Playwright runner via
[playwright-bdd](https://github.com/vitalets/playwright-bdd).

## Prerequisites

- Java 21, Node 22
- Packaged app: `mvn -DskipTests package -P release` from the repo root
  (builds `ui/dist`, then the executable `api/target/db4all-api-*.jar`
  which serves both API and UI; the `release` profile is what embeds
  the UI statics into the jar)
- Deps + browser (from `e2e/`):
  `npm install && npm run install-browsers`

## Commands (from `e2e/`)

- `npm test` — generate specs from features, boot the jar, run all scenarios
- `npm run test:headed` — same with a visible browser
- `npx playwright test --grep "Filter"` — run selected scenarios
  (each scenario sets up its own prerequisites, except scenario 1
  which needs a fresh database)

## How it works

- `playwright.config.ts` starts the packaged jar on port **18080** with a
  **fresh timestamped H2 file** in `e2e/.data-e2e/` per run: no clash with
  dev servers (8080/5173), no pollution of the dev database.
  The server is stopped automatically at the end of the run.
- `features/*.feature` — Gherkin scenarios, one file per journey.
- `steps/*.steps.ts` — step definitions; `steps/support.ts` — shared
  UI helpers (`ensureProject`, `ensureDatasource`, ...).
- Datasources point at the repo `sample/` folder
  (`clients.csv`, `products.csv`).

## Conventions for new tests

- Prefer `Given ... exists` steps: they create the fixture through the UI
  only if missing, so scenarios stay independently runnable.
- Keep selectors close to the user: role/name first, CSS fallback with a
  comment when labels are not associated.

## Troubleshooting

- `API jar not found` → run `mvn -DskipTests package -P release` from the repo root.
- `Executable doesn't exist` (browser) → `npm run install-browsers`.
- Port 18080 busy → Playwright reuses an already-running server locally;
  stop it or let the run reuse it (CI always starts a fresh one).
- `Playwright does not support chromium on <distro>` (old Ubuntu) →
  install Google Chrome and run with `E2E_CHANNEL=chrome`.

## Editor support

The `CucumberOpen.cucumber-official` VS Code extension (see
`.devcontainer/devcontainer.json`) highlights `.feature` files. Step
navigation may be limited because step definitions use `playwright-bdd`
wrappers instead of plain `@cucumber/cucumber` imports.
