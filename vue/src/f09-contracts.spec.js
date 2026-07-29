import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { extname, join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const root = process.cwd()

function source(path) {
  return readFileSync(resolve(root, path), 'utf8')
}

function sourceFiles(dir = resolve(root, 'src')) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name)
    if (entry.isDirectory()) return sourceFiles(path)
    return ['.vue', '.js', '.css'].includes(extname(path)) ? [path] : []
  })
}

const retiredAliases = [
  'color-bg',
  'color-surface',
  'color-surface-soft',
  'color-text',
  'color-text-strong',
  'color-muted',
  'color-muted-strong',
  'color-primary',
  'color-primary-strong',
  'color-primary-soft',
  'color-accent',
  'color-success',
  'color-warning',
  'color-danger',
  'color-admin-bg',
  'color-admin-border',
  'color-admin-sidebar',
  'status-processing-fg'
]

describe('F09 compatibility cleanup contracts', () => {
  it('removes the short-lived F03 root aliases and all direct consumers', () => {
    const tokenCss = source('src/assets/tailwind.css')
    const rootBlock = tokenCss.match(/:root\s*{([\s\S]*?)\n}/)?.[1] || ''
    const applicationSource = sourceFiles().map((path) => readFileSync(path, 'utf8')).join('\n')

    retiredAliases.forEach((alias) => {
      expect(rootBlock, alias).not.toMatch(new RegExp(`--${alias}\\s*:`))
      expect(applicationSource, alias).not.toContain(`var(--${alias})`)
    })
  })

  it('removes temporary tokens and upgrade-only knowledge-base wrappers', () => {
    const applicationSource = sourceFiles().map((path) => readFileSync(path, 'utf8')).join('\n')
    expect(applicationSource).not.toMatch(/--(?:color-)?chip\s*:|\bbg-chip\b|var\(--chip\)/)
    expect(existsSync(resolve(root, 'src/components/admin/KnowledgeBaseConfigDialog.vue'))).toBe(false)
    expect(existsSync(resolve(root, 'src/views/admin/AdminKnowledgeBaseView.vue'))).toBe(false)
    expect(existsSync(resolve(root, 'src/views/admin/AdminQualityOverviewView.vue'))).toBe(false)
    expect(existsSync(resolve(root, 'src/views/admin/AdminSystemConfigView.vue'))).toBe(false)
  })

  it('keeps raw buttons limited to generated markdown citation tokens', () => {
    const files = sourceFiles().filter((path) => path.endsWith('.vue') && !path.endsWith('/components/Chat.vue'))
    const offenders = files.filter((path) => /<button\b/.test(readFileSync(path, 'utf8')))
    expect(offenders).toEqual([])
  })
})

describe('F09 dependency and toolchain contracts', () => {
  it('uses Heroicons in source without dormant Lucide packages', () => {
    const packageJson = JSON.parse(source('package.json'))
    const dependencies = { ...packageJson.dependencies, ...packageJson.devDependencies }
    const applicationSource = sourceFiles().map((path) => readFileSync(path, 'utf8')).join('\n')
    const retiredPackages = ['@' + 'lucide/vue', 'lucide' + '-vue-next']

    retiredPackages.forEach((packageName) => {
      expect(dependencies).not.toHaveProperty(packageName)
      expect(applicationSource).not.toContain(packageName)
    })

    // shadcn-vue generates Lucide imports; generated primitives are converted to
    // Heroicons before merge and the source-level assertion above enforces that.
    expect(JSON.parse(source('components.json')).iconLibrary).toBe('lucide')
  })

  it('uses the Tailwind Vite plugin on the locked development port', () => {
    const packageJson = JSON.parse(source('package.json'))
    const viteConfig = source('vite.config.js')
    const postcssConfig = source('postcss.config.js')

    expect(packageJson.dependencies).not.toHaveProperty('@tailwindcss/postcss')
    expect(viteConfig).toContain("from '@tailwindcss/vite'")
    expect(viteConfig).toMatch(/plugins:\s*\[tailwindcss\(\), vue\(\)\]/)
    expect(viteConfig).toMatch(/port:\s*5173/)
    expect(viteConfig).toMatch(/strictPort:\s*true/)
    expect(postcssConfig).toMatch(/plugins:\s*{\s*}/)
  })
})

describe('F09 centered detail contracts', () => {
  it('keeps business, development, and evidence details out of right-side sheets', () => {
    const detailSource = [
      'src/views/admin/AdminDocumentDetailView.vue',
      'src/views/admin/AdminKnowledgeRouteView.vue',
      'src/views/admin/AdminKnowledgeRouteTraceView.vue',
      'src/views/admin/AdminObservabilityDetailView.vue',
      'src/views/admin/AdminObservabilitySessionView.vue'
    ].map(source).join('\n')

    expect(detailSource).not.toMatch(/<Sheet|SheetContent|slide-in-from-right|translate-x-full/)
    expect(detailSource).toContain('ChildPageDialog')
  })
})
