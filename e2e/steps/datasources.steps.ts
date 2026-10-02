import { Given } from './fixtures';
import { SAMPLES_DIR, ensureDatasource, world } from './support';

Given('the datasource {string} points to the samples folder', async ({ page }, name: string) => {
  if (!world.projectId) throw new Error('No project in world: add a "the project ... exists" step first.');
  await ensureDatasource(page, world.projectId, name, SAMPLES_DIR);
});
