import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

const routeTrace = 'src/views/admin/AdminKnowledgeRouteTraceView.vue'
const sessionList = 'src/views/admin/AdminObservabilityListView.vue'
const sessionDetail = 'src/views/admin/AdminObservabilitySessionView.vue'
const exchangeDetail = 'src/views/admin/AdminObservabilityDetailView.vue'

describe('F07 observation and RAG high-density contracts', () => {
  it('keeps route traces server-paged and mobile as a single list/detail panel', () => {
    const content = source(routeTrace)
    expect(content).toContain('queryKnowledgeRouteTracePage')
    expect(content).toContain('workspacePane')
    expect(content).toContain('server-paged')
    expect(content).not.toMatch(/<Button[^>]*rounded-full/)
    expect(content).not.toContain('lowConfidenceWidened')
    expect(content).not.toContain('confidenceBand')
    expect(content).not.toContain('低置信时会放宽')
    expect(content).not.toContain('基于文档画像与元数据综合召回')
    expect(content).not.toContain('bg-gradient-to')
  })

  it('uses centered child-page dialogs for stage and candidate evidence details', () => {
    const content = source(exchangeDetail)
    expect(content).toContain("import ChildPageDialog from '@/components/system/ChildPageDialog.vue'")
    expect(content).toContain('<ChildPageDialog')
    expect(content).not.toMatch(/fixed inset-0[^\n]*z-50/)
    expect(content).not.toContain('useBodyScrollLock')
    expect(content).not.toMatch(/<Sheet|SheetContent|slide-in-from-right|translate-x-full/)
  })

  it('keeps execution stages visually grouped while only explicit buttons open details', () => {
    const content = source(exchangeDetail)
    expect(content).toContain('data-stage-trace-row')
    expect(content).toContain('data-stage-trace-panel')
    expect(content).toContain('bg-secondary/40')
    expect(content).toContain('hover:bg-foreground/[0.08]')
    expect(content).toContain('查看阶段详情')
    expect(content).toContain('@click="openTraceDetail(trace)"')
    expect(content).not.toContain('@click="openTraceDetail(trace.stageId)"')
    expect(content).not.toContain('selectedTraceStageId')
  })

  it('renders key results as one diagnostic signal map instead of a card grid', () => {
    const content = source(exchangeDetail)
    expect(content).toContain('data-answer-signal-map')
    expect(content).toContain('data-signal-input')
    expect(content).toContain('data-signal-evidence')
    expect(content).toContain('data-signal-answer')
    expect(content).toContain('data-signal-resource-rail')
    expect(content).toContain('data-signal-node-panel')
    expect(content).toContain('data-signal-resource-link')
    expect(content).toContain('signal-flow-path')
    expect(content).toContain('data-signal-flow-link')
    expect(content).toContain('v-for="link in orderedSignalFlowLinks"')
    expect(content).toContain('Number(signalFlowLinkActive(left)) - Number(signalFlowLinkActive(right))')
    expect(content).toContain('marker-end')
    expect(content).toContain('signal-flow-arrow-active')
    expect(content).toContain('id="signal-flow-arrow" markerHeight="10" markerWidth="10"')
    expect(content).toContain('id="signal-flow-arrow-active" markerHeight="13" markerWidth="13"')
    expect(content).toContain("signalPathActive('answer', 'resources') ? 'size-6' : 'size-5'")
    expect(content).toContain('signal-node-panel')
    expect(content).toContain('scale(1.015)')
    expect(content).toContain('查看过程')
    expect(content).not.toContain('data-key-results-grid')
    expect(content).not.toContain('data-key-result-card')
    expect(content).not.toContain('class="signal-flow-path signal-flow-arrow')
    expect(content).not.toMatch(/<article[^>]*@click=/)
    expect(content.indexOf('data-answer-signal-map')).toBeLessThan(content.indexOf('data-stage-trace-row'))
  })

  it('renders retrieval fusion as a bounded flow workbench with button-only details', () => {
    const content = source(exchangeDetail)
    expect(content).toContain('data-retrieval-fusion-workbench')
    expect(content).toContain('data-retrieval-flow-summary')
    expect(content).toContain('data-retrieval-channel-lane')
    expect(content).toContain('data-fusion-candidate-map')
    expect(content).toContain('data-fusion-outcome-group')
    expect(content).toContain('data-fusion-candidate-row')
    expect(content).toContain('data-fusion-candidate-track')
    expect(content).toContain('data-fusion-stage')
    expect(content).toContain('data-fusion-stage-arrow')
    expect(content).toContain('buildFusionCandidateFlow')
    expect(content).toContain('查看详情')
    expect(content).toContain('@click="openCandidateDetail(candidate.row)"')
    expect(content).toContain('fusion-candidate-track-row')
    expect(content).toContain('fusion-candidate-layer-card--kind')
    expect(content).toContain('fusion-candidate-layer-card--content')
    expect(content).toContain('fusion-candidate-layer-card--source')
    expect(content).toContain('width: fit-content')
    expect(content).toContain('--fusion-content-chip-bg: var(--route-mode-auto-bg)')
    expect(content).toContain('--fusion-outcome-bg: var(--selection-bg)')
    expect(content).toContain('--fusion-outcome-border: var(--selection-fg)')
    expect(content).toContain('--fusion-outcome-fg: var(--selection-fg)')
    expect(content).toContain("data-content-kind='正文预览'")
    expect(content).toContain("data-content-kind='内容状态'")
    expect(content).toContain('background: var(--secondary)')
    expect(content).not.toContain('通道执行对比')
    expect(content).not.toContain('融合候选结果')
    expect(content).not.toContain('候选排名汇流图')
    expect(content).not.toContain('data-fusion-candidate-inspector')
    expect(content).not.toContain('fusion-candidate-line')
    expect(content).not.toContain('fusion-candidate-foreign')
    expect(content).not.toContain('inspectedFusionCandidate')
    expect(content).not.toContain('hoverFusionCandidate')
    expect(content).not.toContain('fusion-candidate-hit')
    expect(content).not.toContain('fusion-outcome-header')
    expect(content).not.toContain('fusion-outcome-mark')
    expect(content).not.toContain('grid-template-columns:repeat(auto-fit,minmax(260px,1fr))')
    expect(content).not.toMatch(/<article[^>]*data-fusion-candidate-row[^>]*@click=/s)
  })

  it('segments long sessions and carries list/page context through exchange navigation', () => {
    const list = source(sessionList)
    const session = source(sessionDetail)
    const exchange = source(exchangeDetail)
    expect(session).toContain('pagedAssistantExchanges')
    expect(session).toContain('EXCHANGE_PAGE_SIZE')
    expect(list).toContain('listNavigationQuery')
    expect(session).toContain('observationListQuery')
    expect(exchange).toContain('sessionReturnTarget')
  })

})
