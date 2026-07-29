import { expect, test } from '@playwright/test'
import { installMockApp, setAdminAuthenticated } from './fixtures/mockApp.js'

const documentId = 'doc-90071992547409931234'

async function expectColor(locator, property, value) {
  await expect(locator).toHaveCSS(property, value)
}

test.beforeEach(async ({ page }) => {
  await installMockApp(page)
})

test('design lab exposes the balanced classification and workflow roles', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/design-system.html')

  const autoMode = page.locator('[data-route-mode="auto"]')
  const shadowMode = page.locator('[data-route-mode="shadow"]')
  await expectColor(autoMode, 'color', 'rgb(43, 95, 160)')
  await expectColor(autoMode, 'background-color', 'rgb(234, 242, 251)')
  await expectColor(shadowMode, 'color', 'rgb(106, 84, 168)')
  await expectColor(shadowMode, 'background-color', 'rgb(242, 239, 250)')
  await expect(page.locator('[data-tone="success"]').filter({ hasText: '策略方案已确认' })).toHaveCount(1)
  await expect(page.locator('[data-tone="running"]').filter({ hasText: '构建索引可执行' })).toHaveCount(1)

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow).toBe(0)
  await page.screenshot({ path: testInfo.outputPath('design-system-balanced-mobile.png'), fullPage: true, animations: 'disabled' })
})

test('route trace keeps auto and shadow distinct without losing text labels', async ({ page }, testInfo) => {
  await page.route('**/manage/knowledge/route/trace/page/query', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({
        code: '0',
        message: 'ok',
        data: {
          pageNo: '1',
          pageSize: '20',
          totalSize: '2',
          totalPages: '1',
          records: [
            { id: 'auto-1', question: '自动路由展示验证', mode: 'auto', routeStatus: '1', confidence: '0.82', topScopesJson: '[]', topTopicsJson: '[]', topDocumentsJson: '[]' },
            { id: 'shadow-1', question: '影子路由展示验证', mode: 'shadow', routeStatus: '2', confidence: '0.61', topScopesJson: '[]', topTopicsJson: '[]', topDocumentsJson: '[]' }
          ]
        }
      })
    })
  })

  await page.goto('/chat')
  await setAdminAuthenticated(page, true)
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('/admin/knowledge-route/traces')

  const autoMode = page.locator('[data-route-mode="auto"]').first()
  const shadowMode = page.locator('[data-route-mode="shadow"]').first()
  await expect(autoMode).toContainText('自动知识路由')
  await expect(shadowMode).toContainText('影子路由对比')
  await expectColor(autoMode, 'background-color', 'rgb(234, 242, 251)')
  await expectColor(shadowMode, 'background-color', 'rgb(242, 239, 250)')
  await page.screenshot({ path: testInfo.outputPath('route-trace-balanced.png'), fullPage: true, animations: 'disabled' })
})

test('document execution distinguishes confirmed success from build running', async ({ page }, testInfo) => {
  await page.route('**/manage/document/detail/query', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({
        code: '0',
        message: 'ok',
        data: {
          documentId,
          documentName: '方案 B 配色验证文档',
          originalFileName: 'balanced-color-document.pdf',
          knowledgeBaseId: 'kb-90071992547409931234',
          knowledgeBaseName: '配色验证知识库',
          parseStatus: '3',
          strategyStatus: '3',
          indexStatus: '3',
          parseStatusName: '解析成功',
          strategyStatusName: '策略已确认',
          indexStatusName: '索引完成',
          currentPlanId: 'plan-balanced-color'
        }
      })
    })
  })
  await page.route('**/manage/document/strategy/plan/query', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({
        code: '0',
        message: 'ok',
        data: {
          planReady: true,
          parseStatus: '3',
          plan: {
            planId: 'plan-balanced-color',
            parentPipeline: { steps: [{ stepNo: '1', strategyType: '1' }] },
            childPipeline: { steps: [{ stepNo: '1', strategyType: '3' }] }
          }
        }
      })
    })
  })

  await page.goto('/chat')
  await setAdminAuthenticated(page, true)
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto(`/admin/documents/${documentId}`)
  // Navigate to the execution section via its workbench nav tab. (The overview no longer carries
  // three equal quick-entry buttons; it now has a single dynamic primary action.)
  await page.getByRole('button', { name: /确认并构建/ }).click()

  const confirmed = page.getByRole('button', { name: /策略方案已确认/ })
  const build = page.getByRole('button', { name: /构建索引执行/ })
  await expect(confirmed).toBeVisible()
  await expect(build).toBeVisible()
  await expectColor(confirmed, 'color', 'rgb(18, 125, 74)')
  await expectColor(build, 'color', 'rgb(10, 109, 122)')
  await page.screenshot({ path: testInfo.outputPath('document-execution-balanced.png'), fullPage: true, animations: 'disabled' })
})
