import { expect } from '@playwright/test';
import { test, createBdd } from 'playwright-bdd';

export const { Given, When, Then } = createBdd(test);
export { expect };
