import { defineConfig } from '@playwright/test'
import { resolve } from 'node:path'

const evidenceRoot = resolve(process.cwd(), process.env.NEXUS_PLAYWRIGHT_EVIDENCE_DIR || 'test-results/f09')

export default defineConfig({
  testDir: './e2e',
  outputDir: resolve(evidenceRoot, 'playwright-results'),
  fullyParallel: false,
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['line'],
    ['html', { outputFolder: resolve(evidenceRoot, 'playwright-report'), open: 'never' }],
    ['json', { outputFile: resolve(evidenceRoot, 'playwright-report.json') }]
  ],
  use: {
    baseURL: 'http://localhost:5173',
    browserName: 'chromium',
    channel: 'chrome',
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    colorScheme: 'light',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  webServer: {
    command: 'npm run dev -- --host localhost',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 30_000
  },
  projects: [
    {
      name: 'chrome-normal',
      use: { reducedMotion: 'no-preference' }
    },
    {
      name: 'chrome-reduce',
      use: { reducedMotion: 'reduce' }
    }
  ]
})
