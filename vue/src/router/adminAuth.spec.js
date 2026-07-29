import { beforeEach, describe, expect, it } from 'vitest'
import router from './index'

const TOKEN_KEY = 'super-agent-admin-token'

function validToken() {
  const header = window.btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
  const payload = window.btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))
  return `${header}.${payload}.signature`
}

describe('F05 admin route guard', () => {
  beforeEach(async () => {
    window.localStorage.clear()
    await router.replace('/chat')
  })

  it('redirects unauthenticated admin navigation and preserves the full target', async () => {
    await router.push('/admin/documents?keyword=policy')
    expect(router.currentRoute.value.name).toBe('AdminLogin')
    expect(router.currentRoute.value.query.redirect).toBe('/admin/documents?keyword=policy')
  })

  it('sends an authenticated login visit back to its safe admin target', async () => {
    window.localStorage.setItem(TOKEN_KEY, validToken())
    await router.push('/admin/login?redirect=/admin/observability')
    expect(router.currentRoute.value.fullPath).toBe('/admin/observability')
  })
})
