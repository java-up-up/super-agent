import { readdir, readFile, writeFile } from 'node:fs/promises'
import { join, relative } from 'node:path'

const root = process.cwd()
const srcRoot = join(root, 'src')
const tokenPath = join(srcRoot, 'assets/tailwind.css')
const mainPath = join(srcRoot, 'assets/main.css')

async function filesIn(dir) {
  const entries = await readdir(dir, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const path = join(dir, entry.name)
    if (entry.isDirectory()) files.push(...await filesIn(path))
    else if (/\.(vue|js|css)$/.test(entry.name) && !entry.name.endsWith('.spec.js')) files.push(path)
  }
  return files
}

function parseVariables(css) {
  const values = new Map()
  for (const match of css.matchAll(/--([\w-]+)\s*:\s*([^;]+);/g)) values.set(match[1], match[2].trim())
  return values
}

function resolveVariable(name, values, stack = []) {
  if (stack.includes(name)) throw new Error(`Token cycle: ${[...stack, name].join(' -> ')}`)
  const raw = values.get(name)
  if (!raw) return null
  return raw.replace(/var\(--([\w-]+)\)/g, (_, dependency) => resolveVariable(dependency, values, [...stack, name]) || '')
}

function parseHex(value) {
  const match = value.match(/^#([0-9a-f]{3,8})$/i)
  if (!match) return null
  let hex = match[1]
  if (hex.length === 3) hex = hex.split('').map((part) => part + part).join('')
  if (hex.length === 8) hex = hex.slice(0, 6)
  return [0, 2, 4].map((offset) => Number.parseInt(hex.slice(offset, offset + 2), 16) / 255)
}

function linearize(channel) {
  return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4
}

function contrast(foreground, background) {
  const fg = parseHex(foreground)
  const bg = parseHex(background)
  if (!fg || !bg) return null
  const luminance = ([r, g, b]) => 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
  const light = Math.max(luminance(fg), luminance(bg))
  const dark = Math.min(luminance(fg), luminance(bg))
  return (light + 0.05) / (dark + 0.05)
}

const tokenCss = await readFile(tokenPath, 'utf8')
const mainCss = await readFile(mainPath, 'utf8')
const packageJson = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'))
const rootBlock = tokenCss.match(/:root\s*{([\s\S]*?)\n}/)?.[1] || tokenCss
const values = parseVariables(rootBlock)
const pairs = [
  ['body', 'foreground', 'background', 4.5],
  ['muted', 'muted-foreground', 'background', 4.5],
  ['primary button', 'primary-foreground', 'primary', 4.5],
  ['default status', 'status-default-fg', 'status-default-bg', 4.5],
  ['waiting status', 'status-waiting-fg', 'status-waiting-bg', 4.5],
  ['running status', 'status-running-fg', 'status-running-bg', 4.5],
  ['success status', 'status-success-fg', 'status-success-bg', 4.5],
  ['danger status', 'status-danger-fg', 'status-danger-bg', 4.5],
  ['selection', 'selection-fg', 'selection-bg', 4.5],
  ['citation', 'citation-fg', 'citation-bg', 4.5],
  ['auto route mode', 'route-mode-auto-fg', 'route-mode-auto-bg', 4.5],
  ['shadow route mode', 'route-mode-shadow-fg', 'route-mode-shadow-bg', 4.5],
  ['code', 'code-foreground', 'code', 4.5]
]

const contrastRows = pairs.map(([label, foreground, background, threshold]) => {
  const fg = resolveVariable(foreground, values)
  const bg = resolveVariable(background, values)
  const ratio = contrast(fg, bg)
  return { label, foreground: fg, background: bg, ratio, pass: ratio !== null && ratio >= threshold }
})

const allFiles = await filesIn(srcRoot)
const sharedRoots = [join(srcRoot, 'assets'), join(srcRoot, 'components/ui'), join(srcRoot, 'components/system'), join(srcRoot, 'design-system')]
const sharedFiles = allFiles.filter((path) => sharedRoots.some((prefix) => path.startsWith(prefix)))
const routeFiles = allFiles.filter((path) => path.includes('/views/'))
const directColorPattern = /(?<!&)#[0-9a-f]{3,8}\b|rgba?\(|oklch\(/i
const arbitraryTypePattern = /text-\[(?:\d|calc\(|clamp\()[^\]]+\]|font-size\s*:\s*[\d.]+(?:px|rem)/g
const radiusPattern = /rounded-\[[^\]]+\]|border-radius\s*:\s*[\d.]+(?:px|rem)/g
const lucidePattern = /@lucide\/vue|lucide-vue-next/g
const retiredAliases = [
  'color-bg', 'color-surface', 'color-surface-soft', 'color-text', 'color-text-strong',
  'color-muted', 'color-muted-strong', 'color-primary', 'color-primary-strong',
  'color-primary-soft', 'color-accent', 'color-success', 'color-warning', 'color-danger',
  'color-admin-bg', 'color-admin-border', 'color-admin-sidebar', 'status-processing-fg'
]

async function countMatches(files, pattern, exclude = []) {
  const rows = []
  for (const path of files) {
    if (exclude.includes(path)) continue
    const content = await readFile(path, 'utf8')
    const matches = content.match(pattern)
    if (matches?.length) rows.push([relative(root, path), matches.length])
  }
  return rows
}

const sharedDirectColors = await countMatches(sharedFiles, directColorPattern, [tokenPath])
const routeDirectColors = await countMatches(routeFiles, directColorPattern)
const sharedArbitraryTypes = await countMatches(sharedFiles, arbitraryTypePattern, [tokenPath])
const routeArbitraryTypes = await countMatches(routeFiles, arbitraryTypePattern)
const sharedRadii = await countMatches(sharedFiles, radiusPattern, [tokenPath])
const routeRadii = await countMatches(routeFiles, radiusPattern)
const lucideConsumers = await countMatches(allFiles, lucidePattern)
const allSource = (await Promise.all(allFiles.map((path) => readFile(path, 'utf8')))).join('\n')
const retiredAliasProducers = retiredAliases.filter((alias) => new RegExp(`--${alias}\\s*:`).test(rootBlock))
const retiredAliasConsumers = retiredAliases.filter((alias) => allSource.includes(`var(--${alias})`))
const chipCompatibilityConsumers = allSource.match(/--(?:color-)?chip\s*:|\bbg-chip\b|var\(--chip\)/g) || []
const packageDependencies = { ...packageJson.dependencies, ...packageJson.devDependencies }
const retiredPackages = ['@' + 'lucide/vue', 'lucide' + '-vue-next', '@tailwindcss/' + 'postcss']
  .filter((packageName) => packageDependencies[packageName])
const rawButtonConsumers = []

for (const path of allFiles.filter((candidate) => candidate.endsWith('.vue') && !candidate.endsWith('/components/Chat.vue'))) {
  if (/<button\b/.test(await readFile(path, 'utf8'))) rawButtonConsumers.push(relative(root, path))
}

const failures = []
if (/--(?:color|background|foreground|primary|radius|shadow)-[\w-]+\s*:/.test(mainCss)) failures.push('main.css defines a backing token')
if (directColorPattern.test(mainCss)) failures.push('main.css contains a direct color')
if (contrastRows.some((row) => !row.pass)) failures.push('contrast threshold failed')
if (sharedDirectColors.length) failures.push('shared foundation contains direct colors outside tailwind.css')
if (routeDirectColors.length) failures.push('route source contains direct colors')
if (sharedArbitraryTypes.length || routeArbitraryTypes.length) failures.push('arbitrary production font size remains')
if (sharedRadii.length || routeRadii.length) failures.push('arbitrary production radius remains')
if (lucideConsumers.length) failures.push('Lucide consumer remains in source')
if (retiredAliasProducers.length || retiredAliasConsumers.length) failures.push('retired F03 alias remains')
if (chipCompatibilityConsumers.length) failures.push('temporary chip compatibility token remains')
if (retiredPackages.length) failures.push('retired frontend dependency remains')
if (rawButtonConsumers.length) failures.push('raw Vue button remains outside the markdown citation boundary')

console.log('F03 contrast report')
for (const row of contrastRows) console.log(`${row.pass ? 'PASS' : 'FAIL'} ${row.label}: ${row.ratio?.toFixed(2) ?? 'n/a'}:1 (${row.foreground} on ${row.background})`)
console.log(`\nShared foundation direct colors: ${sharedDirectColors.length}`)
console.log(`Shared foundation arbitrary type/radius declarations: ${sharedArbitraryTypes.length}/${sharedRadii.length}`)
console.log(`Route residual direct colors (F04-F09 owners): ${routeDirectColors.length}`)
console.log(`Route residual arbitrary type/radius declarations (F04-F09 owners): ${routeArbitraryTypes.length}/${routeRadii.length}`)
console.log(`Lucide consumers: ${lucideConsumers.length}`)
console.log(`Retired alias producers/consumers: ${retiredAliasProducers.length}/${retiredAliasConsumers.length}`)
console.log(`Temporary chip compatibility consumers: ${chipCompatibilityConsumers.length}`)
console.log(`Retired dependencies: ${retiredPackages.length}`)
console.log(`Raw Vue buttons outside markdown citations: ${rawButtonConsumers.length}`)

const report = {
  generatedAt: new Date().toISOString(),
  contrast: contrastRows,
  shared: {
    directColors: sharedDirectColors,
    arbitraryType: sharedArbitraryTypes,
    arbitraryRadius: sharedRadii
  },
  routes: {
    directColors: routeDirectColors,
    arbitraryType: routeArbitraryTypes,
    arbitraryRadius: routeRadii
  },
  compatibility: {
    lucideConsumers,
    retiredAliasProducers,
    retiredAliasConsumers,
    chipCompatibilityConsumerCount: chipCompatibilityConsumers.length,
    retiredPackages,
    rawButtonConsumers
  },
  failures
}
const jsonArgument = process.argv.find((argument) => argument.startsWith('--json='))
if (jsonArgument) {
  await writeFile(join(root, jsonArgument.slice('--json='.length)), `${JSON.stringify(report, null, 2)}\n`, 'utf8')
}
if (failures.length) {
  console.error(`\nFAIL ${failures.join('; ')}`)
  process.exitCode = 1
} else {
  console.log('\nPASS F03 shared foundation gate')
}
