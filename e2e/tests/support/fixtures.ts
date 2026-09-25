// Every test gets its own browser context, so its own local storage and its own game on the server.
// Games are never shared between tests; the server forgets them after six hours without use.
import { test as base, expect } from '@playwright/test';
import { MulePage } from './mule';

export const test = base.extend<{ mule: MulePage }>({
  mule: async ({ page, request }, use) => {
    await use(new MulePage(page, request));
  },
});

export { expect };
