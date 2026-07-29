import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminLoginView from './AdminLoginView.vue'

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  replace: vi.fn(),
  push: vi.fn(),
  saveAdminAuth: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace, push: mocks.push }),
  useRoute: () => ({ query: { redirect: '/admin/documents?keyword=policy' } })
}))

vi.mock('../api/api', () => ({
  APIError: class APIError extends Error {},
  adminAuthApi: { login: mocks.login }
}))

vi.mock('../utils/adminAuth', () => ({ saveAdminAuth: mocks.saveAdminAuth }))

describe('F05 admin login behavior', () => {
  beforeEach(() => {
    Object.values(mocks).forEach((mock) => mock.mockReset())
    mocks.login.mockResolvedValue({ username: 'operator', token: 'signed-token' })
  })

  it('starts empty, toggles password visibility, and restores the safe admin redirect', async () => {
    const wrapper = mount(AdminLoginView)
    const username = wrapper.get('#login-username')
    const password = wrapper.get('#login-password')

    expect(username.element.value).toBe('')
    expect(password.element.value).toBe('')
    expect(password.attributes('type')).toBe('password')

    await wrapper.get('button[aria-label="显示密码"]').trigger('click')
    expect(password.attributes('type')).toBe('text')

    await username.setValue(' operator ')
    await password.setValue('secret')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.login).toHaveBeenCalledWith({ username: 'operator', password: 'secret' })
    expect(mocks.saveAdminAuth).toHaveBeenCalledWith({ username: 'operator', token: 'signed-token' })
    expect(mocks.replace).toHaveBeenCalledWith('/admin/documents?keyword=policy')
  })

  it('reports required fields without issuing a request', async () => {
    const wrapper = mount(AdminLoginView)
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('请输入账号和密码。')
    expect(mocks.login).not.toHaveBeenCalled()
  })
})
