import { AxeBuilder } from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { installMockApp, setAdminAuthenticated } from './fixtures/mockApp.js'

const evidenceRoot = resolve(process.cwd(), process.env.NEXUS_PLAYWRIGHT_EVIDENCE_DIR || 'test-results/f09')

const routes = [
  { key: 'chat', path: '/chat', admin: false },
  { key: 'admin-login', path: '/admin/login', login: true },
  { key: 'admin-dashboard', path: '/admin/dashboard', admin: true },
  { key: 'admin-documents', path: '/admin/documents', admin: true },
  { key: 'admin-document-detail', path: '/admin/documents/doc-90071992547409931234', admin: true },
  { key: 'admin-knowledge-bases', path: '/admin/knowledge-bases', admin: true },
  { key: 'admin-knowledge-route', path: '/admin/knowledge-route', admin: true },
  { key: 'admin-route-traces', path: '/admin/knowledge-route/traces', admin: true },
  { key: 'admin-observability', path: '/admin/observability', admin: true },
  { key: 'admin-system-config', path: '/admin/settings/configuration', admin: true },
  { key: 'admin-session', path: '/admin/observability/conversation_01HZYX0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ', admin: true },
  { key: 'admin-exchange', path: '/admin/observability/conversation_01HZYX0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ/exchanges/90071992547409931234', admin: true },
  // 追加到末尾：CPU 节流测试按位置索引取 routes[3]/[6]/[8]，插到中间会打乱它们。
  { key: 'admin-quality-overview', path: '/admin/quality-overview', admin: true }
]

const viewports = [
  { key: '375x812', width: 375, height: 812 },
  { key: '390x844', width: 390, height: 844 },
  { key: '768x1024', width: 768, height: 1024 },
  { key: '1024x768', width: 1024, height: 768 },
  { key: '1440x1000', width: 1440, height: 1000 },
  { key: '1920x1080', width: 1920, height: 1080 }
]

async function openRoute(page, route) {
  if (page.url() === 'about:blank') {
    await page.goto('/chat')
  }
  await setAdminAuthenticated(page, !route.login)
  await page.goto(route.path, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('main').first()).toBeVisible()
  await page.waitForTimeout(120)
  await expect(page).toHaveURL(new RegExp(`${route.path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?:\\?.*)?$`))
}

async function pageMetrics(page, durationMs, apiRequests) {
  return page.evaluate(({ duration, requests }) => {
    const documentElement = document.documentElement
    const resources = performance.getEntriesByType('resource')
    const longTasks = performance.getEntriesByType('longtask')
    const overflowOffenders = [...document.querySelectorAll('body *')]
      .filter((element) => {
        const rect = element.getBoundingClientRect()
        if (rect.right <= documentElement.clientWidth + 1 || rect.left >= documentElement.clientWidth) return false
        let parent = element.parentElement
        while (parent && parent !== document.body) {
          const overflowX = getComputedStyle(parent).overflowX
          if (['auto', 'scroll', 'hidden', 'clip'].includes(overflowX)) return false
          parent = parent.parentElement
        }
        return true
      })
      .slice(0, 12)
      .map((element) => ({
        tag: element.tagName.toLowerCase(),
        className: String(element.className || '').slice(0, 180),
        text: String(element.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 120),
        right: Math.round(element.getBoundingClientRect().right)
      }))
    return {
      durationMs: duration,
      domNodes: document.querySelectorAll('*').length,
      focusableNodes: document.querySelectorAll('a[href],button:not([disabled]),input:not([disabled]),textarea:not([disabled]),select:not([disabled]),[tabindex]:not([tabindex="-1"])').length,
      apiRequestCount: requests.length,
      apiRequests: requests,
      resourceCount: resources.length,
      resourceTransferBytes: resources.reduce((sum, entry) => sum + Number(entry.transferSize || 0), 0),
      longTaskCount: longTasks.length,
      clientWidth: documentElement.clientWidth,
      scrollWidth: documentElement.scrollWidth,
      horizontalOverflow: Math.max(0, documentElement.scrollWidth - documentElement.clientWidth),
      overflowOffenders
    }
  }, { duration: durationMs, requests: apiRequests })
}

test.beforeEach(async ({ page }) => {
  await installMockApp(page)
})

test('redirects, auth guard, and observation return context stay intact', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('/chat')

  await setAdminAuthenticated(page, false)
  await page.goto('/')
  await expect(page).toHaveURL(/\/chat$/)

  await setAdminAuthenticated(page, true)
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/admin\/dashboard$/)

  const protectedTarget = '/admin/observability/conversation_01HZYX0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ/exchanges/90071992547409931234?listKeyword=policy&listMode=DOCUMENT&listStatus=COMPLETED&listPage=3&listPageSize=24&turnPage=2'
  await setAdminAuthenticated(page, false)
  await page.goto(protectedTarget)
  await expect(page).toHaveURL(/\/admin\/login\?redirect=/)
  expect(new URL(page.url()).searchParams.get('redirect')).toBe(protectedTarget)

  await setAdminAuthenticated(page, true)
  await page.reload()
  await expect(page).toHaveURL(new RegExp(`${protectedTarget.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`))
  await expect(page.getByText(/当前查看的是 exchange/)).toHaveCount(0)
  await expect(page.getByText(/执行路径是/)).toHaveCount(0)

  await page.getByRole('link', { name: '返回会话轮次列表' }).click()
  await expect(page).toHaveURL(/\/admin\/observability\/conversation_01HZYX0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ\?/)
  let query = new URL(page.url()).searchParams
  expect(Object.fromEntries(query)).toEqual({
    listKeyword: 'policy',
    listMode: 'DOCUMENT',
    listStatus: 'COMPLETED',
    listPage: '3',
    listPageSize: '24',
    turnPage: '2'
  })

  await page.getByRole('link', { name: '返回会话列表' }).click()
  await expect(page).toHaveURL(/\/admin\/observability\?/)
  query = new URL(page.url()).searchParams
  expect(Object.fromEntries(query)).toEqual({
    listKeyword: 'policy',
    listMode: 'DOCUMENT',
    listStatus: 'COMPLETED',
    listPage: '3',
    listPageSize: '24'
  })
})

test('chat keeps the latest assistant answer in view after session hydration', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  const question = '滚动回归问题'
  const finalParagraph = '第 120 段长回答内容，用于确认会话回填完成后仍停留在智能助手回答末尾。'
  const answer = [
    ...Array.from({ length: 119 }, (_, index) => `第 ${index + 1} 段长回答内容，用于撑开真实消息滚动区域。`),
    finalParagraph
  ].join('\n\n')
  const exchange = {
    exchangeId: 'exchange-scroll-regression',
    question,
    answer,
    status: 'COMPLETED',
    references: [],
    recommendations: [],
    thinkingSteps: [],
    usedTools: [],
    createTime: '2026-07-28T14:18:00Z',
    editTime: '2026-07-28T14:18:01Z'
  }
  const session = {
    conversationId: 'conversation-scroll-regression',
    latestUserMessage: question,
    latestAssistantMessage: finalParagraph,
    chatMode: 'OPEN_CHAT',
    selectedKnowledgeBaseIds: [],
    knowledgeBaseSelectionMode: 'NONE',
    messageCount: 2,
    exchanges: [exchange],
    updatedAt: '2026-07-28T14:18:01Z'
  }
  const fulfill = (route, data) => route.fulfill({
    status: 200,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify({ code: '0', message: 'ok', data })
  })
  await page.route('**/api/chat/session/list**', (route) => fulfill(route, {
    pageNo: '1', pageSize: '20', totalSize: '1', totalPages: '1', sessions: [session]
  }))
  await page.route('**/api/chat/session/detail**', (route) => fulfill(route, session))

  await page.goto('/chat')
  const messagesPanel = page.getByLabel('对话消息')
  await expect(messagesPanel.getByText(finalParagraph, { exact: true })).toBeVisible()
  await expect.poll(() => messagesPanel.evaluate((element) => (
    Math.abs(element.scrollHeight - element.scrollTop - element.clientHeight)
  ))).toBeLessThanOrEqual(1)

  await page.getByRole('button', { name: `打开会话：${question}` }).click()
  await expect(messagesPanel.getByText(finalParagraph, { exact: true })).toBeVisible()
  await expect.poll(() => messagesPanel.evaluate((element) => (
    Math.abs(element.scrollHeight - element.scrollTop - element.clientHeight)
  ))).toBeLessThanOrEqual(1)
})

for (const viewport of viewports) {
  test(`12 route matrix ${viewport.key}`, async ({ page }, testInfo) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    const projectKey = testInfo.project.name
    const targetDir = resolve(evidenceRoot, 'screenshots', projectKey, viewport.key)
    await mkdir(targetDir, { recursive: true })
    const routeMetrics = []

    for (const route of routes) {
      const apiRequests = []
      const consoleErrors = []
      const pageErrors = []
      const requestListener = (request) => {
        const pathname = new URL(request.url()).pathname
        if (pathname.startsWith('/api/') || pathname.startsWith('/manage/') || pathname.startsWith('/admin/auth/')) {
          apiRequests.push(pathname)
        }
      }
      const consoleListener = (message) => {
        if (message.type() !== 'error') return
        consoleErrors.push({ text: message.text(), location: message.location() })
      }
      const pageErrorListener = (error) => {
        pageErrors.push({ message: error.message, stack: error.stack || '' })
      }
      page.on('request', requestListener)
      page.on('console', consoleListener)
      page.on('pageerror', pageErrorListener)
      const startedAt = Date.now()
      await openRoute(page, route)
      if (route.key === 'admin-system-config') {
        const categoryTabsBottom = await page.locator('[data-config-group-tab]').evaluateAll((elements) => (
          Math.max(...elements.map((element) => element.getBoundingClientRect().bottom))
        ))
        const groupContentTop = await page.locator('.config-group-intro').evaluate((element) => element.getBoundingClientRect().top)
        expect(groupContentTop, `${route.path} category content overlaps the group tabs at ${viewport.key}`).toBeGreaterThanOrEqual(categoryTabsBottom)

        const hierarchyBounds = await page.locator('[data-config-category]').first().evaluate((category) => {
          const trigger = category.querySelector('[data-config-category-trigger]')
          const item = category.querySelector('[data-config-item]')
          const triggerRect = trigger.getBoundingClientRect()
          const itemRect = item.getBoundingClientRect()
          return {
            leftInset: itemRect.left - triggerRect.left,
            rightInset: triggerRect.right - itemRect.right
          }
        })
        expect(hierarchyBounds.leftInset, `${route.path} config items need a visible parent-child inset at ${viewport.key}`).toBeGreaterThanOrEqual(24)
        expect(hierarchyBounds.rightInset, `${route.path} config items need a visible nested width at ${viewport.key}`).toBeGreaterThanOrEqual(0)
      }
      const metrics = await pageMetrics(page, Date.now() - startedAt, apiRequests)
      page.off('request', requestListener)
      page.off('console', consoleListener)
      page.off('pageerror', pageErrorListener)

      expect(metrics.horizontalOverflow, `${route.path} at ${viewport.key}: ${JSON.stringify(metrics.overflowOffenders)}`).toBe(0)
      expect(consoleErrors, `${route.path} console.error at ${viewport.key}`).toEqual([])
      expect(pageErrors, `${route.path} pageerror at ${viewport.key}`).toEqual([])
      routeMetrics.push({ route: route.path, ...metrics, consoleErrors, pageErrors })
      await page.screenshot({ path: resolve(targetDir, `${route.key}.png`), fullPage: true, animations: 'disabled' })
    }

    await writeFile(resolve(targetDir, 'metrics.json'), `${JSON.stringify(routeMetrics, null, 2)}\n`, 'utf8')
  })
}

test('system configuration edits one typed value and exposes its before and after history', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 })
  const systemConfigRoute = routes.find((route) => route.key === 'admin-system-config')
  await openRoute(page, systemConfigRoute)

  const groupIconBackgrounds = await page.locator('[data-config-group-tab] .config-group-tab-icon').evaluateAll((elements) => (
    elements.map((element) => getComputedStyle(element).backgroundColor)
  ))
  expect(groupIconBackgrounds).toHaveLength(4)
  expect(groupIconBackgrounds).not.toContain('rgb(255, 255, 255)')
  // 方案 A：四个分类不再各自着色。静止 tab 图标统一中性，仅选中的 tab 图标带品红，
  // 因此恰好出现 2 种颜色（1 个选中品红 + 3 个相同中性），而不是过去的 4 种糖果色。
  expect(new Set(groupIconBackgrounds).size).toBe(2)

  await page.getByRole('tab', { name: /检索与排序/ }).click()
  const relevanceSection = page.getByRole('button', { name: /相关性阈值/ })
  await expect(relevanceSection).toHaveAttribute('aria-expanded', 'true')
  await expect(relevanceSection).not.toHaveCSS('background-color', 'rgb(255, 255, 255)')
  await expect(relevanceSection).not.toHaveCSS('background-color', 'rgba(0, 0, 0, 0)')
  await relevanceSection.click()
  await expect(relevanceSection).toHaveAttribute('aria-expanded', 'false')
  await relevanceSection.click()
  const configRow = page.locator('article').filter({ hasText: '向量最低相似度' }).first()
  await expect(configRow).not.toHaveCSS('background-color', 'rgb(255, 255, 255)')
  await expect(configRow).not.toHaveCSS('background-color', await relevanceSection.evaluate((element) => getComputedStyle(element).backgroundColor))
  await expect(configRow).toContainText('45%')

  const viewTrigger = configRow.getByRole('button', { name: '查看', exact: true })
  await viewTrigger.click()
  const viewDialog = page.getByRole('dialog', { name: '查看 向量最低相似度' })
  await expect(viewDialog).toBeVisible()
  await expect(viewDialog).toContainText('ragRuntime.minVectorSimilarity')
  await expect(viewDialog).toContainText('45%')
  await expect(viewDialog).toContainText('新会话生效')
  await expect(viewDialog.locator('input, textarea, select')).toHaveCount(0)
  await viewDialog.getByRole('button', { name: '关闭', exact: true }).click()
  await expect(viewTrigger).toBeFocused()

  await configRow.getByRole('button', { name: '编辑' }).click()

  const editDialog = page.getByRole('dialog', { name: '编辑 向量最低相似度' })
  await expect(editDialog).toBeVisible()
  await editDialog.getByRole('spinbutton', { name: '新值' }).fill('52')
  await editDialog.getByRole('textbox', { name: '修改说明' }).fill('提高低相关候选过滤强度')
  await editDialog.getByRole('button', { name: '保存修改' }).click()
  await expect(page.getByRole('status')).toContainText('当前版本为 v7')

  await page.getByRole('tab', { name: '修改历史' }).click()
  await expect(page.getByText('45%', { exact: true })).toBeVisible()
  await expect(page.getByText('52%', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '查看详情' }).click()

  const detailDialog = page.getByRole('dialog', { name: '配置修改详情' })
  await expect(detailDialog).toBeVisible()
  await expect(detailDialog).toContainText('v6 → v7')
  await expect(detailDialog).toContainText('45%')
  await expect(detailDialog).toContainText('52%')
})

for (const viewport of [viewports[1], viewports[4]]) {
  test(`axe route matrix ${viewport.key}`, async ({ page }, testInfo) => {
    test.skip(testInfo.project.name === 'chrome-reduce', 'axe semantics are motion-independent')
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    const results = []

    for (const route of routes) {
      await openRoute(page, route)
      const accessibilityTree = await page.locator('main').first().ariaSnapshot()
      const analysis = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'])
        .analyze()
      results.push({
        route: route.path,
        accessibilityTree,
        violations: analysis.violations.map(({ id, impact, help, nodes }) => ({
          id,
          impact,
          help,
          targets: nodes.map((node) => node.target)
        }))
      })
    }

    const targetDir = resolve(evidenceRoot, 'axe')
    await mkdir(targetDir, { recursive: true })
    await writeFile(resolve(targetDir, `${viewport.key}.json`), `${JSON.stringify(results, null, 2)}\n`, 'utf8')
    expect(results.flatMap((result) => result.violations)).toEqual([])
  })
}

test('keyboard skip, drawers and centered workflow dialog restore focus', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })

  await openRoute(page, routes[0])
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: '跳到主要内容' })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(page.locator('#chat-main-content')).toBeFocused()

  const composer = page.getByRole('textbox', { name: '输入问题' })
  await composer.focus()
  await composer.fill('键盘可达性验证')
  await expect(composer).toBeFocused()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: '发送消息' })).toBeFocused()
  await expect(page.getByRole('button', { name: '发送消息' })).toBeEnabled()

  await openRoute(page, routes[2])
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: '跳到主要内容' })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(page.locator('#admin-main-content')).toBeFocused()

  const mobileNavTrigger = page.getByRole('button', { name: '打开后台导航' })
  await mobileNavTrigger.click()
  await expect(page.getByTestId('admin-mobile-drawer')).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(mobileNavTrigger).toBeFocused()

  await openRoute(page, routes[3])
  const uploadTrigger = page.getByRole('button', { name: '上传文档' })
  await uploadTrigger.click()
  const dialog = page.getByRole('dialog', { name: '上传资料并进入推荐流程' })
  await expect(dialog).toBeVisible()
  await expect(dialog.locator('[data-slot="dialog-body"]')).toHaveCount(1)
  await page.keyboard.press('Escape')
  await expect(uploadTrigger).toBeFocused()

  await openRoute(page, routes.find((route) => route.key === 'admin-session'))
  const exchangeDetailLink = page.getByRole('link', { name: '查看第 1 轮详情' })
  await expect(exchangeDetailLink).toBeVisible()
  await expect(exchangeDetailLink).toContainText('查看轮次详情')
  await exchangeDetailLink.focus()
  await expect(exchangeDetailLink).toBeFocused()

  const summaryTrigger = page.getByRole('button', { name: '查看长期摘要' })
  await summaryTrigger.focus()
  await page.keyboard.press('Enter')
  const summaryDialog = page.getByRole('dialog', { name: '长期摘要快照' })
  await expect(summaryDialog).toBeVisible()
  await expect(summaryDialog).toContainText('覆盖 1 轮 · 第 1 版 · 已压缩 1 次')
  await expect(summaryDialog.getByRole('heading', { name: '会话目标', level: 3 })).toBeVisible()
  await expect(summaryDialog.getByRole('listitem')).toHaveCount(2)
  await expect(summaryDialog.locator('[data-slot="dialog-body"]')).toHaveCount(1)
  await page.keyboard.press('Escape')
  await expect(summaryTrigger).toBeFocused()

  await openRoute(page, routes.find((route) => route.key === 'admin-exchange'))
  await expect(page.getByRole('button', { name: /刷新这一轮详情/ })).toHaveCount(0)
  const signalMap = page.locator('[data-answer-signal-map]')
  await expect(signalMap).toHaveCount(1)
  await expect(signalMap.locator('[data-signal-input]')).toHaveCount(3)
  await expect(signalMap.locator('[data-signal-evidence]')).toHaveCount(1)
  await expect(signalMap.locator('[data-signal-answer]')).toHaveCount(1)
  await expect(signalMap.locator('[data-signal-resource-rail]')).toContainText('958')
  await expect(signalMap.locator('[data-signal-node-panel]')).toHaveCount(6)
  await expect(signalMap.locator('[data-signal-resource-link]')).toHaveCount(1)
  await expect(signalMap.locator('[data-signal-flow-link]')).toHaveCount(4)
  await expect(signalMap.locator('marker')).toHaveCount(2)
  await expect(signalMap.locator('#signal-flow-arrow')).toHaveAttribute('markerWidth', '10')
  await expect(signalMap.locator('#signal-flow-arrow')).toHaveAttribute('markerHeight', '10')
  await expect(signalMap.locator('#signal-flow-arrow-active')).toHaveAttribute('markerWidth', '13')
  await expect(signalMap.locator('#signal-flow-arrow-active')).toHaveAttribute('markerHeight', '13')
  await expect(page.locator('[data-key-result-card]')).toHaveCount(0)
  const firstSignalPath = signalMap.locator('[data-signal-flow-link="scope"]')
  const restingSignalStroke = await firstSignalPath.evaluate((element) => getComputedStyle(element).stroke)
  const restingSignalMarker = await firstSignalPath.getAttribute('marker-end')
  const firstSignalPanel = signalMap.locator('[data-signal-input]').first()
  const restingSignalBackground = await firstSignalPanel.evaluate((element) => getComputedStyle(element).backgroundColor)
  const restingSignalTransform = await firstSignalPanel.evaluate((element) => getComputedStyle(element).transform)
  const reducesMotion = await page.evaluate(() => matchMedia('(prefers-reduced-motion: reduce)').matches)
  await firstSignalPanel.hover()
  await expect.poll(() => firstSignalPath.evaluate((element) => getComputedStyle(element).stroke)).not.toBe(restingSignalStroke)
  await expect.poll(() => firstSignalPath.getAttribute('marker-end')).not.toBe(restingSignalMarker)
  await expect(signalMap.locator('[data-signal-flow-link]').last()).toHaveAttribute('data-signal-flow-link', 'scope')
  await expect.poll(() => firstSignalPanel.evaluate((element) => getComputedStyle(element).backgroundColor)).not.toBe(restingSignalBackground)
  if (reducesMotion) {
    await expect.poll(() => firstSignalPanel.evaluate((element) => getComputedStyle(element).transform)).toBe(restingSignalTransform)
  } else {
    await expect.poll(() => firstSignalPanel.evaluate((element) => getComputedStyle(element).transform)).not.toBe(restingSignalTransform)
  }
  await page.getByRole('heading', { name: '这轮回答的关键结果' }).hover()
  await expect.poll(() => firstSignalPath.evaluate((element) => getComputedStyle(element).stroke)).toBe(restingSignalStroke)
  await expect.poll(() => firstSignalPath.getAttribute('marker-end')).toBe(restingSignalMarker)
  await expect.poll(() => firstSignalPanel.evaluate((element) => getComputedStyle(element).backgroundColor)).toBe(restingSignalBackground)
  await expect.poll(() => firstSignalPanel.evaluate((element) => getComputedStyle(element).transform)).toBe(restingSignalTransform)
  for (const [index, key] of ['understanding', 'retrieval'].entries()) {
    const path = signalMap.locator(`[data-signal-flow-link="${key}"]`)
    await signalMap.locator('[data-signal-input]').nth(index + 1).hover()
    await expect(path).toHaveAttribute('marker-end', 'url(#signal-flow-arrow-active)')
    await expect(signalMap.locator('[data-signal-flow-link]').last()).toHaveAttribute('data-signal-flow-link', key)
  }
  await page.getByRole('heading', { name: '这轮回答的关键结果' }).hover()
  const resourceLink = signalMap.locator('[data-signal-resource-link]')
  const resourceArrow = resourceLink.locator('.signal-resource-link-arrow')
  const restingResourceLinkColor = await resourceLink.evaluate((element) => getComputedStyle(element).color)
  const restingResourceArrowWidth = await resourceArrow.evaluate((element) => Number.parseFloat(getComputedStyle(element).width))
  expect(restingResourceArrowWidth).toBeGreaterThanOrEqual(20)
  await signalMap.locator('[data-signal-resource-rail]').hover()
  await expect.poll(() => resourceLink.evaluate((element) => getComputedStyle(element).color)).not.toBe(restingResourceLinkColor)
  await expect.poll(() => resourceArrow.evaluate((element) => Number.parseFloat(getComputedStyle(element).width))).toBeGreaterThan(restingResourceArrowWidth)
  await signalMap.getByText('文档范围', { exact: true }).click()
  await expect(page.getByRole('dialog', { name: '请求入口' })).toHaveCount(0)
  const signalScopeTrigger = signalMap.getByRole('button', { name: '查看文档范围过程' })
  await signalScopeTrigger.click()
  await expect(page.getByRole('dialog', { name: '请求入口' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(signalScopeTrigger).toBeFocused()
  const stageRows = page.locator('[data-stage-trace-row]')
  await expect(stageRows).toHaveCount(2)
  const firstStagePanel = stageRows.first().locator('[data-stage-trace-panel]')
  const restingStageBackground = await firstStagePanel.evaluate((element) => getComputedStyle(element).backgroundColor)
  await firstStagePanel.hover()
  await expect.poll(() => firstStagePanel.evaluate((element) => getComputedStyle(element).backgroundColor)).not.toBe(restingStageBackground)
  await page.mouse.move(0, 0)
  await expect.poll(() => firstStagePanel.evaluate((element) => getComputedStyle(element).backgroundColor)).toBe(restingStageBackground)
  await stageRows.first().getByText('请求入口', { exact: true }).click()
  await expect(page.getByRole('dialog', { name: '请求入口' })).toHaveCount(0)

  const stageDetailTrigger = page.getByRole('button', { name: '查看请求入口阶段详情' })
  await stageDetailTrigger.click()
  await expect(page.getByRole('dialog', { name: '请求入口' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(stageDetailTrigger).toBeFocused()
  await page.getByRole('heading', { name: '执行阶段时间线' }).hover()
  await expect(stageRows.first()).not.toHaveClass(/bg-primary/)

  await openRoute(page, routes[6])
  const scopeTab = page.getByRole('tab', { name: /范围/ })
  const topicTab = page.locator('#knowledge-route-tab-topic')
  await scopeTab.focus()
  await page.keyboard.press('ArrowRight')
  await expect(topicTab).toBeFocused()
  await expect(topicTab).toHaveAttribute('aria-selected', 'true')
  await page.keyboard.press('Home')
  await expect(scopeTab).toBeFocused()
  await expect(scopeTab).toHaveAttribute('aria-selected', 'true')

  const scopeCard = page.getByRole('button', { name: /制度范围/ })
  await scopeCard.focus()
  await page.keyboard.press('Enter')
  const scopeDialog = page.getByRole('dialog', { name: '知识范围详情' })
  await expect(scopeDialog).toBeVisible()
  const scopeDialogBox = await scopeDialog.boundingBox()
  expect(scopeDialogBox).not.toBeNull()
  expect(Math.abs(scopeDialogBox.x + scopeDialogBox.width / 2 - 195)).toBeLessThanOrEqual(2)
  await page.keyboard.press('Escape')
  await expect(scopeCard).toBeFocused()

  await openRoute(page, routes[7])
  const insightToggle = page.getByRole('button', { name: '路由洞察' })
  await insightToggle.focus()
  await expect(insightToggle).toHaveAttribute('aria-expanded', 'false')
  await page.keyboard.press('Enter')
  await expect(insightToggle).toHaveAttribute('aria-expanded', 'true')
  await page.keyboard.press('Space')
  await expect(insightToggle).toHaveAttribute('aria-expanded', 'false')
})

test('retrieval fusion workbench groups outcomes and keeps candidate details button-only', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 1000 })
  await openRoute(page, routes.find((route) => route.key === 'admin-exchange'))
  await page.getByRole('tab', { name: '检索与融合', exact: true }).click()
  const screenshotDir = resolve(evidenceRoot, 'retrieval-fusion', testInfo.project.name)
  await mkdir(screenshotDir, { recursive: true })

  const workbench = page.locator('[data-retrieval-fusion-workbench]')
  await expect(workbench).toBeVisible()
  await expect(workbench.locator('[data-retrieval-flow-summary]')).toContainText('通道执行')
  await expect(workbench.locator('[data-retrieval-flow-summary]')).toContainText('最终证据')
  // 子问题分组使用可展开的 Accordion，不是 Tabs：断言其真实的按钮语义与展开状态。
  await expect(workbench.getByRole('button', { name: /子问题 1/ })).toHaveCount(1)
  await expect(workbench.getByRole('button', { name: /子问题 2/ })).toHaveCount(1)
  await expect(workbench.getByRole('button', { name: /子问题 1/ })).toHaveAttribute('aria-expanded', 'true')
  // Accordion 是 type="multiple"，两个子问题默认全开，candidateMap 选择器覆盖两个 group：
  // 子问题1(4候选/3结果组/3通道) + 子问题2(2候选/2结果组/2通道)
  await expect(workbench.locator('[data-retrieval-channel-lane]')).toHaveCount(5)
  await expect(workbench.locator('table')).toHaveCount(0)

  const candidateMap = workbench.locator('[data-fusion-candidate-map]')
  await expect(candidateMap.first()).toBeVisible()
  await expect(candidateMap.locator('[data-fusion-outcome-group]')).toHaveCount(5)
  await expect(candidateMap.locator('[data-fusion-candidate-row]')).toHaveCount(6)
  await expect(candidateMap.locator('[data-fusion-candidate-track]')).toHaveCount(6)
  await expect(candidateMap.locator('[data-fusion-stage]')).toHaveCount(24)
  await expect(candidateMap.locator('[data-fusion-stage-arrow]')).toHaveCount(18)
  await expect(candidateMap.locator('svg[role="img"]')).toHaveCount(0)
  await expect(candidateMap.locator('.fusion-candidate-line')).toHaveCount(0)
  await expect(candidateMap.locator('.fusion-outcome-header')).toHaveCount(0)
  await expect(candidateMap.getByRole('button', { name: /查看候选详情/ })).toHaveCount(6)

  // 文本内容在整个工作台范围内断言，避免 candidateMap 多元素 strict 模式冲突
  await expect(workbench).toContainText('章节路径')
  await expect(workbench).toContainText('正文预览')
  await expect(workbench).toContainText('来源：关键词检索')
  await expect(workbench).toContainText('第六章 检索观测 / 6.4 候选融合与最终证据确认流程')
  await expect(workbench).toContainText('该候选通过质量闸门，但没有进入本轮最终证据窗口。')
  await expect(workbench).toContainText('召回候选')
  await expect(workbench).toContainText('融合分')
  await expect(workbench).toContainText('重排分')
  await expect(workbench).toContainText('最终结果')
  await expect(workbench).not.toContainText('通道内第')
  await expect(workbench).not.toContainText('星联智服全渠道客服平台上线与运营管理手册_响应式与可访问性验证长文件名.md')
  await expect(workbench).not.toContainText('P#')
  await expect(workbench).not.toContainText('C#')
  await expect(workbench.locator('[data-fusion-candidate-inspector]')).toHaveCount(0)
  await expect(workbench.getByRole('button', { name: '查看上一候选' })).toHaveCount(0)
  await expect(workbench.getByRole('button', { name: '查看下一候选' })).toHaveCount(0)

  const firstCandidateRow = candidateMap.locator('[data-fusion-candidate-row]').first()
  const firstContext = firstCandidateRow.locator('.fusion-candidate-context')
  const firstKindCard = firstCandidateRow.locator('.fusion-candidate-layer-card--kind')
  const [contextBox, kindBox] = await Promise.all([firstContext.boundingBox(), firstKindCard.boundingBox()])
  expect(contextBox).not.toBeNull()
  expect(kindBox).not.toBeNull()
  expect(kindBox.width).toBeLessThan(contextBox.width)

  const palette = await page.evaluate(() => {
    const resolveColor = (value) => {
      const probe = document.createElement('span')
      probe.style.color = value
      document.body.appendChild(probe)
      const color = getComputedStyle(probe).color
      probe.remove()
      return color
    }
    return {
      section: resolveColor('var(--route-mode-auto-fg)'),
      preview: resolveColor('var(--status-success-fg)'),
      secondary: resolveColor('var(--secondary)'),
      background: resolveColor('var(--background)'),
      selectionBackground: resolveColor('var(--selection-bg)'),
      routeAutoBackground: resolveColor('var(--route-mode-auto-bg)'),
      citationBackground: resolveColor('var(--citation-bg)')
    }
  })
  const sectionKindCard = candidateMap.locator('[data-content-kind="章节路径"] .fusion-candidate-layer-card--kind').first()
  const previewKindCard = candidateMap.locator('[data-content-kind="正文预览"] .fusion-candidate-layer-card--kind').first()
  expect(await sectionKindCard.evaluate((element) => getComputedStyle(element).color)).toBe(palette.section)
  expect(await previewKindCard.evaluate((element) => getComputedStyle(element).color)).toBe(palette.preview)

  const finalOutcomeNode = candidateMap.locator(
    '[data-tone="success"] [data-stage-key="outcome"] .fusion-candidate-stage-node'
  ).first()
  const finalOutcomeBackground = await finalOutcomeNode.evaluate((element) => getComputedStyle(element).backgroundColor)
  expect(finalOutcomeBackground).toBe(palette.selectionBackground)
  expect(finalOutcomeBackground).not.toBe(palette.routeAutoBackground)
  expect(finalOutcomeBackground).not.toBe(palette.citationBackground)

  const restingRowBackground = await firstCandidateRow.evaluate((element) => getComputedStyle(element).backgroundColor)
  expect(restingRowBackground).toBe(palette.secondary)
  expect(restingRowBackground).not.toBe(palette.background)
  await firstCandidateRow.hover()
  await expect.poll(() => firstCandidateRow.evaluate((element) => getComputedStyle(element).backgroundColor)).not.toBe(restingRowBackground)
  await page.getByRole('heading', { name: '候选决策轨道' }).first().hover()
  await expect.poll(() => firstCandidateRow.evaluate((element) => getComputedStyle(element).backgroundColor)).toBe(restingRowBackground)

  const keywordCandidate = candidateMap.locator('[data-fusion-candidate-row]').filter({
    hasText: '该候选通过质量闸门，但没有进入本轮最终证据窗口。'
  })
  await keywordCandidate.locator('.fusion-candidate-layer-card--content').click()
  await expect(page.getByRole('dialog')).toHaveCount(0)

  const detailTrigger = keywordCandidate.getByRole('button', { name: /查看候选详情/ })
  await detailTrigger.click()
  const candidateDialog = page.getByRole('dialog', { name: '客服平台故障排查与指标定义补充说明.md' })
  await expect(candidateDialog).toBeVisible()
  await expect(candidateDialog).toContainText('未选入')
  await page.keyboard.press('Escape')
  await expect(detailTrigger).toBeFocused()
  await page.getByRole('heading', { name: '候选决策轨道' }).first().hover()

  await page.screenshot({ path: resolve(screenshotDir, 'desktop-1440.png'), fullPage: true, animations: 'disabled' })

  // 折叠子问题 1，只留子问题 2 展开：验证第二个子问题的真实数据独立成立。
  const firstGroupTrigger = workbench.getByRole('button', { name: /子问题 1/ })
  await firstGroupTrigger.click()
  await expect(firstGroupTrigger).toHaveAttribute('aria-expanded', 'false')
  await expect(workbench.locator('[data-retrieval-channel-lane]')).toHaveCount(2)
  await expect(candidateMap.locator('[data-fusion-outcome-group]')).toHaveCount(2)
  await expect(candidateMap.locator('[data-fusion-candidate-row]')).toHaveCount(2)
  await expect(candidateMap.locator('[data-fusion-stage]')).toHaveCount(8)
  await expect(candidateMap.locator('[data-fusion-stage-arrow]')).toHaveCount(6)
  await firstGroupTrigger.click()
  await expect(firstGroupTrigger).toHaveAttribute('aria-expanded', 'true')

  for (const viewport of [
    { key: 'mobile-390', width: 390, height: 844 },
    { key: 'tablet-768', width: 768, height: 1024 },
    { key: 'desktop-1024', width: 1024, height: 768 },
    { key: 'wide-1920', width: 1920, height: 1080 }
  ]) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    // 两个子问题分组均展开：6 条候选轨道、24 个阶段节点，所有断点消费同一组数据结构。
    await expect(candidateMap.locator('[data-fusion-candidate-track]')).toHaveCount(6)
    await expect(candidateMap.locator('[data-fusion-stage]')).toHaveCount(24)
    await expect(candidateMap.locator('svg[role="img"]')).toHaveCount(0)
    const viewportMetrics = await page.evaluate(() => ({
      clientWidth: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth
    }))
    expect(viewportMetrics.scrollWidth, viewport.key).toBe(viewportMetrics.clientWidth)
    await page.screenshot({ path: resolve(screenshotDir, `${viewport.key}.png`), fullPage: true, animations: 'disabled' })
  }
})

test('desktop reflow proxy at 200 percent keeps all routes reachable', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name === 'chrome-reduce', 'reflow is motion-independent')
  await page.setViewportSize({ width: 720, height: 500 })
  const results = []
  for (const route of routes) {
    await openRoute(page, route)
    const metric = await page.evaluate(() => ({
      path: location.pathname,
      clientWidth: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth
    }))
    expect(metric.scrollWidth - metric.clientWidth, `${route.path} at 200% reflow proxy`).toBeLessThanOrEqual(0)
    results.push(metric)
  }
  const targetDir = resolve(evidenceRoot, 'zoom-200')
  await mkdir(targetDir, { recursive: true })
  await writeFile(resolve(targetDir, 'metrics.json'), `${JSON.stringify(results, null, 2)}\n`, 'utf8')
})

test('key routes stay responsive with four-times CPU throttling', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name === 'chrome-reduce', 'CPU evidence is independent of motion preference')
  await page.setViewportSize({ width: 1440, height: 1000 })
  const client = await page.context().newCDPSession(page)
  const results = []
  await client.send('Emulation.setCPUThrottlingRate', { rate: 4 })

  try {
    for (const route of [routes[0], routes[3], routes[6], routes[8]]) {
      const loadStartedAt = Date.now()
      await openRoute(page, route)
      const loadMs = Date.now() - loadStartedAt
      const interactionStartedAt = Date.now()

      if (route.key === 'chat') {
        await page.getByRole('textbox', { name: '输入问题' }).fill('四倍 CPU 降速交互验证')
        await expect(page.getByRole('button', { name: '发送消息' })).toBeEnabled()
      } else if (route.key === 'admin-documents') {
        await page.getByRole('button', { name: '上传文档' }).click()
        await expect(page.getByRole('dialog', { name: '上传资料并进入推荐流程' })).toBeVisible()
        await page.keyboard.press('Escape')
      } else if (route.key === 'admin-knowledge-route') {
        const topicTab = page.locator('#knowledge-route-tab-topic')
        await topicTab.click()
        await expect(topicTab).toHaveAttribute('aria-selected', 'true')
      } else {
        const refreshButton = page.getByRole('button', { name: '刷新会话列表' })
        await refreshButton.click()
        await expect(refreshButton).toBeEnabled()
      }

      const interactionMs = Date.now() - interactionStartedAt
      const domNodes = await page.locator('*').count()
      expect(loadMs, `${route.path} load under 4x CPU throttle`).toBeLessThan(5000)
      expect(interactionMs, `${route.path} interaction under 4x CPU throttle`).toBeLessThan(3000)
      results.push({ route: route.path, loadMs, interactionMs, domNodes })
    }
  } finally {
    await client.send('Emulation.setCPUThrottlingRate', { rate: 1 }).catch(() => {})
    await client.detach().catch(() => {})
  }

  const targetDir = resolve(evidenceRoot, 'cpu-throttle')
  await mkdir(targetDir, { recursive: true })
  await writeFile(resolve(targetDir, '4x.json'), `${JSON.stringify(results, null, 2)}\n`, 'utf8')
})
