import { defineConfig, devices } from '@playwright/test';

const PORT = Number(process.env.PORT ?? 8089);
const BASE_URL = process.env.BASE_URL ?? `http://localhost:${PORT}`;
// An already installed Chromium, for environments that cannot download the browser of this Playwright version
const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE;

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  // No green by repetition: locally no retry, so flaky tests show up at once
  retries: process.env.CI ? 1 : 0,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI
    ? [['html', { open: 'never' }], ['list'], ['junit', { outputFile: 'test-results/junit.xml' }]]
    : [['html', { open: 'never' }], ['list']],

  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    locale: 'de-DE',
    timezoneId: 'Europe/Berlin',
    launchOptions: executablePath ? { executablePath } : {},
  },

  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    // The main flows once more on a phone (Chromium, touch)
    { name: 'mobile', use: { ...devices['Pixel 7'] }, testMatch: /spiel\.spec\.ts/ },
  ],

  // The backend serves the frontend; the test API lets tests start games from any position.
  // Build it first with ./gradlew :backend:installDist. Never point BASE_URL at a server for real players.
  webServer: process.env.BASE_URL
    ? undefined
    : {
      command: '../backend/build/install/backend/bin/backend',
      url: `${BASE_URL}/`,
      env: { PORT: String(PORT), MULE_TEST_API: 'true' },
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
});
