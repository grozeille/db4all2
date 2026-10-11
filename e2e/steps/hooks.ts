import { AfterStep } from './fixtures';
import { snap } from './support';

let stepCounter = 0;

/** Screenshot after every step, attached to the scenario output (see HTML report). */
AfterStep(async ({ page, $step }) => {
  stepCounter += 1;
  const slug = $step.title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60);
  await snap(page, `step-${String(stepCounter).padStart(2, '0')}-${slug}`);
});
