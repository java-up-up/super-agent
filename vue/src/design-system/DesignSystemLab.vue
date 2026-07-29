<script setup>
import { ref } from 'vue'
import { AdjustmentsHorizontalIcon, ArrowPathIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'
import AsyncState from '@/components/system/AsyncState.vue'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import CodeSurface from '@/components/system/CodeSurface.vue'
import DataTableShell from '@/components/system/DataTableShell.vue'
import FilterToolbar from '@/components/system/FilterToolbar.vue'
import PageHeader from '@/components/system/PageHeader.vue'
import StatusBadge from '@/components/system/StatusBadge.vue'
import { resolveRouteModeBadgeClass } from '@/components/system/toneClasses'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

const dialogOpen = ref(false)
const includeArchived = ref(false)
const scope = ref('all')
const activeTab = ref('overview')

const statuses = [
  ['default', '未设置'],
  ['waiting', '等待处理'],
  ['running', '解析中'],
  ['success', '已完成'],
  ['danger', '处理失败']
]

const swatches = [
  ['品牌', 'var(--brand-600)', '主操作、当前导航'],
  ['成功', 'var(--success-600)', '完成、可用'],
  ['运行中', 'var(--running-600)', '实时执行、生成'],
  ['选中', 'var(--selection-700)', '当前行、当前节点'],
  ['引用', 'var(--citation-700)', '引用与证据连接'],
  ['警告', 'var(--warning-700)', '需关注但可继续'],
  ['危险', 'var(--danger-700)', '失败与破坏性操作']
]

const routeModes = [
  ['auto', '自动知识路由'],
  ['shadow', '影子路由对比']
]

const graphSwatches = Array.from({ length: 8 }, (_, index) => [
  `类型 ${index + 1}`,
  `var(--graph-node-${index + 1})`
])
</script>

<template>
  <main class="min-h-screen bg-background text-foreground">
    <div class="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 lg:px-8">
      <PageHeader
        title="Nexus Agent Pro 设计系统实验台"
        description="当前生产 Token、基础组件和居中子页面弹窗合同。"
      >
        <template #actions>
          <Button variant="outline" size="sm" type="button">
            <AdjustmentsHorizontalIcon data-icon="inline-start" />
            紧凑操作
          </Button>
          <Button size="lg" type="button">后台主操作</Button>
        </template>
      </PageHeader>

      <section class="lab-section" aria-labelledby="lab-color-title">
        <div class="lab-section__heading">
          <h2 id="lab-color-title">语义颜色</h2>
          <p>品牌、成功、运行中、选中和引用各自承担单一角色。</p>
        </div>
        <div class="grid gap-px overflow-hidden rounded-lg border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
          <div v-for="item in swatches" :key="item[0]" class="flex min-h-24 items-start gap-3 bg-card p-4">
            <span class="mt-0.5 size-7 shrink-0 rounded-md border border-border" :style="{ background: item[1] }"></span>
            <div class="min-w-0">
              <strong class="block text-sm">{{ item[0] }}</strong>
              <span class="mt-1 block text-xs leading-5 text-muted-foreground">{{ item[2] }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="lab-section" aria-labelledby="lab-classification-title">
        <div class="lab-section__heading">
          <h2 id="lab-classification-title">分类与工作流语义</h2>
          <p>分类色保持低饱和；确认完成与构建执行使用不同状态角色。</p>
        </div>
        <div class="grid gap-px overflow-hidden rounded-lg border border-border bg-border md:grid-cols-2">
          <div class="flex min-h-24 flex-col justify-center gap-3 bg-card p-4">
            <strong class="text-sm">知识路由模式</strong>
            <div class="flex flex-wrap gap-2">
              <Badge v-for="item in routeModes" :key="item[0]" variant="outline" :class="resolveRouteModeBadgeClass(item[0])" :data-route-mode="item[0]">{{ item[1] }}</Badge>
            </div>
          </div>
          <div class="flex min-h-24 flex-col justify-center gap-3 bg-card p-4">
            <strong class="text-sm">确认并构建</strong>
            <div class="flex flex-wrap gap-2">
              <StatusBadge tone="success" label="策略方案已确认" />
              <StatusBadge tone="running" label="构建索引可执行" />
            </div>
          </div>
        </div>
        <div class="mt-4 border-y border-border bg-card px-4 py-3">
          <strong class="text-xs">GraphRAG 实体分类色</strong>
          <div class="mt-2 flex flex-wrap gap-3" aria-label="GraphRAG 实体分类色板">
            <span v-for="item in graphSwatches" :key="item[0]" class="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
              <span class="size-3 rounded-full" :style="{ background: item[1] }"></span>
              {{ item[0] }}
            </span>
          </div>
        </div>
      </section>

      <section class="lab-section" aria-labelledby="lab-controls-title">
        <div class="lab-section__heading">
          <h2 id="lab-controls-title">控件与组合</h2>
          <p>后台操作使用 36px 主按钮；紧凑操作保持 32px 和 rounded-md。</p>
        </div>

        <FilterToolbar>
          <div class="grid min-w-0 gap-1.5">
            <Label for="lab-search">搜索</Label>
            <div class="relative min-w-[15rem] max-w-full">
              <MagnifyingGlassIcon class="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input id="lab-search" class="pl-8" placeholder="文档名、任务 ID 或知识范围" />
            </div>
          </div>
          <div class="grid gap-1.5">
            <Label for="lab-scope">知识范围</Label>
            <Select v-model="scope">
              <SelectTrigger id="lab-scope" class="w-44">
                <SelectValue placeholder="选择范围" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="all">全部范围</SelectItem>
                  <SelectItem value="course">课程知识</SelectItem>
                  <SelectItem value="internal">内部手册</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <label class="flex h-8 items-center gap-2 text-sm">
            <Checkbox v-model="includeArchived" />
            包含已归档记录
          </label>
          <template #actions>
            <Button variant="outline" size="sm" type="button">重置</Button>
            <Button size="lg" type="button" loading loading-text="查询中" />
          </template>
        </FilterToolbar>

        <div class="mt-5 flex flex-wrap gap-2" aria-label="状态徽章">
          <StatusBadge v-for="item in statuses" :key="item[0]" :tone="item[0]" :label="item[1]" />
        </div>
      </section>

      <section class="lab-section" aria-labelledby="lab-density-title">
        <div class="lab-section__heading">
          <h2 id="lab-density-title">三档密度</h2>
          <p>信息密度变化不改变状态、字号语义或点击目标。</p>
        </div>
        <div class="grid gap-px overflow-hidden rounded-lg border border-border bg-border lg:grid-cols-3">
          <article class="bg-card p-5">
            <span class="text-xs font-semibold text-muted-foreground">comfortable</span>
            <h3 class="mt-2 text-lg font-semibold">阅读与帮助内容</h3>
            <p class="mt-3 max-w-[65ch] text-base leading-7 text-muted-foreground">用于学员答案、帮助说明和需要持续阅读的中文长文本，段落节奏更松。</p>
          </article>
          <article class="bg-card p-4">
            <span class="text-xs font-semibold text-muted-foreground">default</span>
            <h3 class="mt-2 text-base font-semibold">普通表单与详情</h3>
            <p class="mt-2 text-sm leading-6 text-muted-foreground">用于后台常规操作、设置和业务详情，保持稳定的 8px 控件圆角上限。</p>
          </article>
          <article class="bg-card p-3">
            <span class="text-xs font-semibold text-muted-foreground">compact</span>
            <h3 class="mt-1.5 text-sm font-semibold">追踪与证据列表</h3>
            <p class="mt-1.5 text-xs leading-5 text-muted-foreground">用于高密度数据比较，数字使用 tabular numeral，关键信息不缩成 9px。</p>
          </article>
        </div>
      </section>

      <section class="lab-section" aria-labelledby="lab-table-title">
        <div class="lab-section__heading">
          <h2 id="lab-table-title">表格与长值</h2>
          <p>长中文和稳定 identity 允许换行或截断，不撑破操作区。</p>
        </div>
        <DataTableShell caption="文档处理状态示例">
          <thead>
            <tr class="border-b border-border text-left text-xs text-muted-foreground">
              <th class="px-3 py-2 font-medium">文档</th>
              <th class="px-3 py-2 font-medium">任务 identity</th>
              <th class="px-3 py-2 font-medium">状态</th>
              <th class="px-3 py-2 text-right font-medium">耗时</th>
            </tr>
          </thead>
          <tbody>
            <tr class="border-b border-border last:border-0">
              <td class="px-3 py-3 text-sm font-medium">验收说明文档</td>
              <td class="max-w-72 break-all px-3 py-3 font-mono text-xs text-muted-foreground">PROMPT_RENDERED_SOURCE:8f1c0a9e-7b42-40c1-b63a-evidence-000128</td>
              <td class="px-3 py-3"><StatusBadge tone="running" label="解析中" /></td>
              <td class="px-3 py-3 text-right text-sm tabular-nums">1,284 ms</td>
            </tr>
          </tbody>
        </DataTableShell>
      </section>

      <section class="lab-section" aria-labelledby="lab-states-title">
        <div class="lab-section__heading">
          <h2 id="lab-states-title">异步状态</h2>
          <p>全空、筛选无匹配、失败和局部失败不能混成同一种空白。</p>
        </div>
        <div class="grid gap-px overflow-hidden rounded-lg border border-border bg-border sm:grid-cols-2 xl:grid-cols-5">
          <div class="bg-card"><AsyncState state="loading" /></div>
          <div class="bg-card"><AsyncState state="empty" /></div>
          <div class="bg-card"><AsyncState state="filtered" /></div>
          <div class="bg-card"><AsyncState state="error" /></div>
          <div class="bg-card"><AsyncState state="partial" /></div>
        </div>
      </section>

      <section class="lab-section" aria-labelledby="lab-tabs-title">
        <div class="lab-section__heading">
          <h2 id="lab-tabs-title">技术表面与 Tabs</h2>
          <p>深色表面只用于 Prompt、JSON、manifest、日志和 source snapshot。</p>
        </div>
        <Tabs v-model="activeTab" class="min-w-0">
          <TabsList>
            <TabsTrigger value="overview">概览</TabsTrigger>
            <TabsTrigger value="prompt">Prompt</TabsTrigger>
          </TabsList>
          <TabsContent value="overview" class="mt-4 text-sm leading-6 text-muted-foreground">
            当前链路保留真实阶段、证据和 citation identity；详情通过居中子页面弹窗展开。
          </TabsContent>
          <TabsContent value="prompt" class="mt-4">
            <CodeSurface label="PROMPT_RENDERED_SOURCE">
{{ `{\n  "identity": "source-000128",\n  "citationEligible": true,\n  "stage": "PROMPT_RENDERED_SOURCE"\n}` }}
            </CodeSurface>
          </TabsContent>
        </Tabs>
      </section>

      <section class="lab-section border-b-0" aria-labelledby="lab-dialog-title">
        <div class="lab-section__heading">
          <h2 id="lab-dialog-title">居中子页面弹窗</h2>
          <p>业务详情、开发子层和证据详情不使用右侧滑入 Sheet。</p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <ChildPageDialog
            v-model:open="dialogOpen"
            title="证据详情"
            description="当前引用对应的完整证据与 manifest identity。"
            size="default"
          >
            <template #trigger>
              <Button size="lg" type="button">打开证据详情</Button>
            </template>
            <div class="space-y-4">
              <div>
                <span class="text-xs font-semibold text-muted-foreground">证据身份</span>
                <p class="mt-1 break-all font-mono text-sm">PROMPT_RENDERED_SOURCE:8f1c0a9e-7b42-40c1-b63a-evidence-000128</p>
              </div>
              <CodeSurface label="source snapshot">
{{ `{\n  "sourceSnapshotIdentities": ["source-000128"],\n  "explicitCitationEligible": true\n}` }}
              </CodeSurface>
              <p class="text-sm leading-6 text-muted-foreground">正文是唯一滚动容器；头部与提交区保持固定，移动端仍维持居中子页面语义。</p>
            </div>
            <template #footer>
              <Button variant="outline" size="sm" type="button" @click="dialogOpen = false">取消</Button>
              <Button size="lg" type="button" @click="dialogOpen = false">完成</Button>
            </template>
          </ChildPageDialog>
          <span class="inline-flex items-center gap-2 text-xs text-muted-foreground">
            <ArrowPathIcon class="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />
            normal / reduced-motion 终态
          </span>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.lab-section {
  padding-block: 2rem;
  border-bottom: 1px solid var(--border);
}

.lab-section__heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.lab-section__heading h2,
.lab-section__heading p {
  margin: 0;
}

.lab-section__heading h2 {
  font-size: var(--text-title-sm);
  font-weight: 650;
}

.lab-section__heading p {
  max-width: 60ch;
  color: var(--muted-foreground);
  font-size: var(--text-caption);
  line-height: 1.6;
  text-align: right;
}

@media (max-width: 48rem) {
  .lab-section__heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .lab-section__heading p {
    text-align: left;
  }
}
</style>
