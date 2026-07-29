import { nextTick, ref, defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { resolveStatusTone } from '@/components/system/status'
import { resolveRouteModeBadgeClass, resolveStatusBadgeClass } from '@/components/system/toneClasses'
import StatusBadge from '@/components/system/StatusBadge.vue'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'

describe('F03 status contract', () => {
  it('keeps business status mapping in one shared tone resolver', () => {
    expect(resolveStatusTone('parse', '3')).toBe('success')
    expect(resolveStatusTone('parse', '2')).toBe('running')
    expect(resolveStatusTone('parse', '4')).toBe('danger')
    expect(resolveStatusTone('vector', '3')).toBe('success')
    expect(resolveStatusTone('vector', '4')).toBe('danger')
    expect(resolveStatusTone('vector', '1')).toBe('waiting')
    expect(resolveStatusTone('strategy', '0')).toBe('waiting')
    expect(resolveStatusTone('task', '0')).toBe('default')
  })

  it('renders status text and a non-color structural marker', () => {
    const wrapper = mount(StatusBadge, { props: { tone: 'success', label: '已完成' } })
    expect(wrapper.attributes('data-tone')).toBe('success')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.find('.status-badge__dot').exists()).toBe(true)
    expect(wrapper.classes()).toContain('text-[var(--status-success-fg)]')
    expect(resolveStatusBadgeClass('running')).toContain('bg-[var(--status-running-bg)]')
  })
})

describe('F03 primitive contracts', () => {
  it('uses the locked operation size and exposes loading state', () => {
    const wrapper = mount(Button, {
      props: { size: 'lg', loading: true, loadingText: '保存中' },
      slots: { default: '保存' }
    })

    expect(wrapper.classes()).toContain('rounded-md')
    expect(wrapper.classes()).toContain('h-9')
    expect(wrapper.attributes('disabled')).toBeDefined()
    expect(wrapper.attributes('aria-busy')).toBe('true')
    expect(wrapper.text()).toContain('保存中')
  })

  it('keeps icon-only buttons explicitly labelled', () => {
    const wrapper = mount(Button, {
      props: { size: 'icon', 'aria-label': '关闭' },
      slots: { default: '<span aria-hidden="true">x</span>' }
    })
    expect(wrapper.attributes('aria-label')).toBe('关闭')
    expect(wrapper.classes()).toContain('size-8')
  })

  it('preserves boolean and string checkbox value contracts', async () => {
    const booleanWrapper = mount(Checkbox, { props: { modelValue: false } })
    await booleanWrapper.find('[data-slot="checkbox"]').trigger('click')
    expect(booleanWrapper.emitted('update:modelValue')?.at(-1)?.[0]).toBe(true)

    const stringWrapper = mount(Checkbox, {
      props: { modelValue: '1', trueValue: '1', falseValue: '0' }
    })
    await stringWrapper.find('[data-slot="checkbox"]').trigger('click')
    expect(stringWrapper.emitted('update:modelValue')?.at(-1)?.[0]).toBe('0')
  })

  it('keeps SelectItem inside SelectGroup and TabsTrigger inside TabsList', () => {
    const selectGroupSource = readFileSync(resolve(process.cwd(), 'src/components/ui/select/SelectGroup.vue'), 'utf8')
    const tabsListSource = readFileSync(resolve(process.cwd(), 'src/components/ui/tabs/TabsList.vue'), 'utf8')
    const tabsTriggerSource = readFileSync(resolve(process.cwd(), 'src/components/ui/tabs/TabsTrigger.vue'), 'utf8')
    expect(selectGroupSource).toContain('data-slot="select-group"')
    expect(tabsListSource).toContain('data-slot="tabs-list"')
    expect(tabsTriggerSource).toContain('data-slot="tabs-trigger"')
  })
})

describe('F03 centered child-page dialog contract', () => {
  it('locks body scroll, keeps one scrollable body, and restores focus on close', async () => {
    const open = ref(false)
    const host = mount(defineComponent({
      setup() {
        return () => h(ChildPageDialog, {
            open: open.value,
            title: '证据详情',
            description: '当前证据的完整内容',
            'onUpdate:open': (value) => { open.value = value }
          }, {
            trigger: () => h(Button, { type: 'button', 'data-testid': 'dialog-trigger' }, { default: () => '打开详情' }),
            default: () => h('p', { 'data-testid': 'dialog-content' }, '长内容'),
            footer: () => h(Button, { type: 'button' }, { default: () => '完成' })
          })
      }
    }))

    const trigger = host.find('[data-testid="dialog-trigger"]')
    trigger.element.focus()
    await trigger.trigger('click')
    await nextTick()

    const dialog = document.body.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog.getAttribute('aria-modal')).toBe('true')
    expect(dialog.parentElement.className).toContain('items-center')
    expect(dialog.querySelector('[data-slot="dialog-body"]').className).toContain('min-h-0')
    expect(dialog.querySelector('[data-slot="dialog-body"]').className).toContain('overflow-y-auto')
    expect(document.body.style.overflow).toBe('hidden')

    dialog.querySelector('[data-dialog-close]').click()
    await nextTick()
    await nextTick()
    await new Promise((resolve) => setTimeout(resolve, 280))
    expect(open.value).toBe(false)
    expect(document.body.style.overflow).toBe('')
    expect([trigger.element, document.body]).toContain(document.activeElement)
    expect(trigger.attributes('aria-haspopup')).toBe('dialog')
  })
})

describe('F03 CSS and motion contracts', () => {
  it('keeps the clean cool-neutral substrate and classification colors token-driven', () => {
    const tokenCss = readFileSync(resolve(process.cwd(), 'src/assets/tailwind.css'), 'utf8')

    expect(tokenCss).toContain('--neutral-25: #fafbfd;')
    expect(tokenCss).toContain('--neutral-50: #f3f6f9;')
    expect(tokenCss).toContain('--neutral-100: #e9edf3;')
    expect(tokenCss).toContain('--neutral-200: #dce3ec;')
    expect(tokenCss).toContain('--neutral-300: #c1cbd8;')
    expect(tokenCss).toContain('--neutral-600: #4f5b6b;')
    expect(tokenCss).toContain('--neutral-900: #161d28;')
    expect(tokenCss).toContain('--running-50: #e4f6f8;')
    expect(tokenCss).toContain('--running-200: #a2d8df;')
    expect(tokenCss).toContain('--running-700: #0a6d7a;')
    expect(tokenCss).toContain('--route-mode-auto-bg: #eaf2fb;')
    expect(tokenCss).toContain('--route-mode-auto-fg: #2b5fa0;')
    expect(tokenCss).toContain('--route-mode-shadow-bg: #f2effa;')
    expect(tokenCss).toContain('--route-mode-shadow-fg: #6a54a8;')
    expect(tokenCss.match(/--graph-node-\d:/g)).toHaveLength(8)
    expect(tokenCss).toContain('--graph-canvas: var(--neutral-25);')
    expect(resolveRouteModeBadgeClass('auto')).toContain('text-[var(--route-mode-auto-fg)]')
    expect(resolveRouteModeBadgeClass('shadow')).toContain('text-[var(--route-mode-shadow-fg)]')
  })

  it('documents the GraphRAG classification palette in the design lab', () => {
    const labSource = readFileSync(resolve(process.cwd(), 'src/design-system/DesignSystemLab.vue'), 'utf8')

    expect(labSource).toContain('GraphRAG 实体分类色板')
    expect(labSource).toContain('var(--graph-node-${index + 1})')
  })

  it('keeps main.css as a consumer and preserves a reduced-motion terminal state', () => {
    const mainCss = readFileSync(resolve(process.cwd(), 'src/assets/main.css'), 'utf8')
    expect(mainCss).not.toMatch(/--(?:color|background|foreground|primary|radius|shadow)-[\w-]+\s*:/)
    expect(mainCss).not.toMatch(/#[0-9a-f]{3,8}|rgba?\(|oklch\(/i)
    expect(mainCss).toContain('overflow-wrap: anywhere')
    expect(mainCss).toContain('white-space: normal')
    expect(mainCss).toContain('prefers-reduced-motion: reduce')
  })
})
