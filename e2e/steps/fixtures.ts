import { expect } from '@playwright/test';
import { test, createBdd } from 'playwright-bdd';

export const { Given, When, Then, AfterStep } = createBdd(test);
export { expect };
