import { describe, expect, it } from 'vitest'
import {
  buildPaginationItems,
  createLatestRequestGuard,
  resolveDocumentPrimaryStatus,
  resolveSessionStatus
} from './adminBehavior'

describe('F05 admin data contracts', () => {
  it('maps one primary document status and one shared session status tone', () => {
    expect(resolveDocumentPrimaryStatus({ parseStatus: '4' })).toEqual({ label: '解析失败', tone: 'danger' })
    expect(resolveDocumentPrimaryStatus({ parseStatus: '3', strategyStatus: '2' })).toEqual({ label: '策略处理中', tone: 'running' })
    expect(resolveDocumentPrimaryStatus({ parseStatus: '3', strategyStatus: '3', indexStatus: '3' })).toEqual({ label: '索引可用', tone: 'success' })
    expect(resolveSessionStatus({ running: true })).toEqual({ label: '实时执行中', tone: 'running' })
    expect(resolveSessionStatus({ latestTurnStatus: 'STOPPED' })).toEqual({ label: '已停止', tone: 'waiting' })
  })

  it('creates compact stable pagination', () => {
    expect(buildPaginationItems(10, 6)).toEqual(['1', '...', '5', '6', '7', '...', '10'])
  })

  it('rejects stale request completions', () => {
    const guard = createLatestRequestGuard()
    const first = guard.begin()
    const second = guard.begin()
    expect(guard.isCurrent(first)).toBe(false)
    expect(guard.isCurrent(second)).toBe(true)
  })
})
