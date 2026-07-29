<template>
  <section class="flex flex-col gap-4">
    <header class="glass-card glass-edge flex items-start justify-between gap-5 rounded-glass border px-[26px] py-6 max-[900px]:flex-col">
      <div class="w-full min-w-0">
        <h2 class="m-0 mt-0.5 text-base font-semibold text-foreground">知识路由配置</h2>
        <p class="mt-1.5 text-sm text-muted-foreground">按 范围 → 主题 → 画像 → 关联 的顺序逐步配置，构建自动知识问答的候选预选体系。</p>
      </div>
      <div class="flex flex-wrap justify-end gap-2.5 max-[900px]:w-full">
        <Button variant="outline" size="lg" class="rounded-md gap-1.5" type="button" :disabled="loading || actionLoading" @click="loadAll">刷新数据</Button>
        <Button size="lg" class="rounded-md gap-1.5" type="button" :disabled="!documents.length || batchLoading" @click="regenerateAllProfiles">{{ batchLoading ? '批量重建中...' : '批量重建画像' }}</Button>
      </div>
    </header>

    <div v-if="notice.message" class="rounded-md px-4 py-3 text-sm font-medium" :class="noticeClass(notice.type)">{{ notice.message }}</div>

    <div class="grid gap-3" style="grid-template-columns:repeat(auto-fit,minmax(170px,1fr))">
      <article v-for="item in summaryCards" :key="item.label" class="glass-card glass-edge grid gap-2 rounded-glass border p-4">
        <span class="text-xs text-muted-foreground">{{ item.label }}</span>
        <strong class="text-title text-foreground">{{ item.value }}</strong>
        <small class="text-xs text-muted-foreground">{{ item.description }}</small>
      </article>
    </div>

    <div class="glass-card glass-edge rounded-glass border p-[18px]">
      <div
        class="flex cursor-pointer select-none items-start justify-between gap-4 rounded-md focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/40"
        role="button"
        tabindex="0"
        :aria-expanded="!coveragePanelCollapsed"
        aria-controls="knowledge-route-coverage-panel"
        @click="coveragePanelCollapsed = !coveragePanelCollapsed"
        @keydown.enter.prevent="coveragePanelCollapsed = !coveragePanelCollapsed"
        @keydown.space.prevent="coveragePanelCollapsed = !coveragePanelCollapsed"
      >
        <div>
          <h3 class="m-0 mt-1.5 text-sm font-semibold text-foreground">范围覆盖率统计</h3>
        </div>
        <div class="flex items-center gap-2.5">
          <span class="inline-flex items-center rounded-full bg-secondary px-3 py-1.5 text-xs text-foreground">整体覆盖率 {{ overallCoverageRateText }}</span>
          <span class="text-xs text-muted-foreground transition-transform" :class="coveragePanelCollapsed ? '-rotate-90' : ''">&#9660;</span>
        </div>
      </div>
      <div id="knowledge-route-coverage-panel" v-show="!coveragePanelCollapsed" class="mt-4 grid gap-3" style="grid-template-columns:repeat(auto-fit,minmax(220px,1fr))">
        <article v-for="item in scopeCoverageRows" :key="item.scopeCode"
          class="grid gap-2.5 rounded-xl border border-border p-3.5"
          :class="item.pendingTopicCount > 0 ? 'bg-gradient-to-br from-amber-500/[0.07] to-white/90' : 'bg-card'">
          <div class="flex items-start justify-between gap-2">
            <div>
              <strong class="block text-sm text-foreground">{{ item.scopeName }}</strong>
            </div>
            <span class="text-sm font-bold text-foreground">{{ item.coverageRateText }}</span>
          </div>
          <div class="mt-1 h-2 overflow-hidden rounded-full bg-foreground/[0.08]">
            <span class="block h-full rounded-full bg-gradient-to-r from-primary to-teal-700" :style="{ width: item.coverageRateText }"></span>
          </div>
          <div class="flex flex-wrap gap-2.5 text-xs text-muted-foreground">
            <span>主题 {{ item.topicCount }}</span>
            <span>已覆盖 {{ item.coveredTopicCount }}</span>
            <span>未关联 {{ item.pendingTopicCount }}</span>
            <span>文档 {{ item.documentCount }}</span>
          </div>
        </article>
      </div>
    </div>

    <nav class="flex gap-1 glass-card glass-edge rounded-glass border p-1.5 max-[1080px]:flex-wrap" role="tablist" aria-label="知识路由配置步骤">
      <Button v-for="tab in TAB_LIST" :key="tab.key"
        variant="ghost" size="sm"
        class="flex flex-1 items-center gap-2.5 rounded-lg px-4 py-3 !h-auto"
        :class="activeTab === tab.key ? 'bg-primary/[0.08]' : 'hover:bg-primary/[0.04]'"
        type="button"
        role="tab"
        :id="`knowledge-route-tab-${tab.key}`"
        :aria-selected="activeTab === tab.key"
        :aria-controls="`knowledge-route-panel-${tab.key}`"
        :tabindex="activeTab === tab.key ? 0 : -1"
        @click="activeTab = tab.key"
        @keydown="handleTabKeydown($event, tab.key)">
        <span class="grid h-[26px] w-[26px] shrink-0 place-items-center rounded-full text-xs font-bold"
          :class="activeTab === tab.key ? 'bg-primary text-white' : 'bg-foreground/[0.08] text-muted-foreground'">{{ tab.step }}</span>
        <span class="whitespace-nowrap font-semibold text-foreground">{{ tab.label }}</span>
        <span class="whitespace-nowrap text-xs text-muted-foreground max-[1080px]:hidden">{{ tab.hint }}</span>
      </Button>
    </nav>

    <section id="knowledge-route-panel-scope" v-show="activeTab === 'scope'" class="tab-content" role="tabpanel" aria-labelledby="knowledge-route-tab-scope" tabindex="0">
      <div class="glass-card glass-edge rounded-glass border p-[22px]">
        <div class="flex items-start justify-between gap-4 max-[900px]:flex-col">
          <div><h3 class="m-0 text-sm font-semibold text-foreground">知识范围</h3><p class="mt-1 text-sm text-muted-foreground">先把大范围定清楚，自动知识问答才能稳定地在正确文档池里预选。</p></div>
          <Button size="lg" class="rounded-md" type="button" @click="openCreateDrawer('scope')">新建范围</Button>
        </div>
        <div class="mt-3.5"><Input v-model="scopeKeyword" aria-label="筛选知识范围" class="h-9 text-sm" placeholder="按范围名称、别名或描述筛选" /></div>
        <div class="mt-4 grid max-h-[520px] gap-3 overflow-y-auto" style="grid-template-columns:repeat(auto-fill,minmax(280px,1fr))">
          <div v-for="item in filteredScopes" :key="item.scopeCode"
            class="grid cursor-pointer gap-2 rounded-xl border p-3.5 transition-all"
            :class="sameId(item.scopeCode, activeScopeId) ? 'border-primary/30 bg-primary/[0.04]' : 'border-border bg-secondary hover:border-primary/20 hover:shadow-sm'"
            role="button"
            tabindex="0"
            @click="openDrawer('scope', item, 'view')"
            @keydown.enter.prevent="openDrawer('scope', item, 'view')"
            @keydown.space.prevent="openDrawer('scope', item, 'view')">
            <div class="flex items-center justify-between gap-2"><strong class="text-sm text-foreground">{{ item.scopeName }}</strong></div>
            <small class="line-clamp-2 text-xs text-muted-foreground">{{ item.description || '暂无描述' }}</small>
            <div class="flex gap-3 text-xs text-muted-foreground">
              <span>主题 {{ topics.filter(t => sameId(t.scopeCode, item.scopeCode)).length }}</span>
              <span>文档 {{ linkedDocumentCountByScope(item.scopeCode) }}</span>
            </div>
          </div>
          <div v-if="!filteredScopes.length" class="text-sm text-muted-foreground">没有匹配的知识范围。</div>
        </div>
      </div>
    </section>

    <section id="knowledge-route-panel-topic" v-show="activeTab === 'topic'" class="tab-content" role="tabpanel" aria-labelledby="knowledge-route-tab-topic" tabindex="0">
      <div class="glass-card glass-edge rounded-glass border p-[22px]">
        <div class="flex items-start justify-between gap-4 max-[900px]:flex-col">
          <div><h3 class="m-0 text-sm font-semibold text-foreground">知识主题</h3><p class="mt-1 text-sm text-muted-foreground">主题是范围里的可回答单元，后续会通过主题文档关联把文档候选进一步收窄。</p></div>
          <Button size="lg" class="rounded-md" type="button" @click="openCreateDrawer('topic')">新建主题</Button>
        </div>
        <div class="mt-3.5 grid items-center gap-2.5 max-[1080px]:grid-cols-1" style="grid-template-columns:180px minmax(0,1fr)">
          <Select v-model="activeScopeFilterValue">
            <SelectTrigger aria-label="按知识范围筛选主题" class="rounded-md border border-border bg-card px-3 py-2.5 text-sm text-foreground h-auto">
              <SelectValue placeholder="全部范围" />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectItem value="__all__">全部范围</SelectItem>
                <SelectItem v-for="item in scopes" :key="item.scopeCode" :value="String(item.scopeCode)">{{ item.scopeName }}</SelectItem>
              </SelectGroup>
            </SelectContent>
          </Select>
          <Input v-model="topicKeyword" aria-label="筛选知识主题" class="h-9 text-sm" placeholder="按主题名称、别名或描述筛选" />
        </div>
        <div class="mt-4 grid max-h-[520px] gap-3 overflow-y-auto" style="grid-template-columns:repeat(auto-fill,minmax(280px,1fr))">
          <div v-for="item in filteredTopics" :key="item.topicCode"
            class="grid cursor-pointer gap-2 rounded-xl border p-3.5 transition-all"
            :class="sameId(item.topicCode, activeTopicId) ? 'border-primary/30 bg-primary/[0.04]' : 'border-border bg-secondary hover:border-primary/20 hover:shadow-sm'"
            role="button"
            tabindex="0"
            @click="openDrawer('topic', item, 'view')"
            @keydown.enter.prevent="openDrawer('topic', item, 'view')"
            @keydown.space.prevent="openDrawer('topic', item, 'view')">
            <strong class="text-sm text-foreground">{{ item.topicName }}</strong>
            <div class="flex flex-wrap gap-2">
              <span class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-foreground">{{ formatAnswerShapeLabel(item.answerShape) }}</span>
              <span class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-foreground">{{ formatExecutionPreferenceLabel(item.executionPreference) }}</span>
            </div>
            <small class="line-clamp-2 text-xs text-muted-foreground">{{ item.description || '暂无描述' }}</small>
          </div>
          <div v-if="!filteredTopics.length" class="text-sm text-muted-foreground">当前范围下还没有主题。</div>
        </div>
      </div>
    </section>

    <section id="knowledge-route-panel-profile" v-show="activeTab === 'profile'" class="tab-content" role="tabpanel" aria-labelledby="knowledge-route-tab-profile" tabindex="0">
      <div class="glass-card glass-edge rounded-glass border p-[22px]">
        <div class="flex items-start justify-between gap-4 max-[900px]:flex-col">
          <div><h3 class="m-0 text-sm font-semibold text-foreground">文档画像</h3><p class="mt-1 text-sm text-muted-foreground">查看文档的类型、摘要、核心主题和图能力开关，判断自动路由是否有足够信息。</p></div>
        </div>
        <div class="mt-3.5"><Input v-model="documentKeyword" aria-label="筛选文档画像" class="h-9 text-sm" placeholder="按文档名、原始文件名或知识域筛选文档" /></div>

        <div v-if="profileAnomalyRows.length" class="mt-4 rounded-lg border border-border bg-card p-[18px]">
          <div class="flex items-start justify-between gap-4">
            <div
              class="flex min-w-0 flex-1 cursor-pointer select-none items-center justify-between gap-3 rounded-md focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/40"
              role="button"
              tabindex="0"
              :aria-expanded="!anomalyCollapsed"
              aria-controls="knowledge-route-anomaly-panel"
              @click="anomalyCollapsed = !anomalyCollapsed"
              @keydown.enter.prevent="anomalyCollapsed = !anomalyCollapsed"
              @keydown.space.prevent="anomalyCollapsed = !anomalyCollapsed"
            >
              <h4 class="m-0 mt-1.5 text-sm font-semibold text-foreground">画像异常清单 ({{ profileAnomalyRows.length }})</h4>
              <span class="text-xs text-muted-foreground transition-transform" :class="anomalyCollapsed ? '-rotate-90' : ''">&#9660;</span>
            </div>
            <Button variant="outline" class="shrink-0 rounded-md" size="sm" type="button" :disabled="!selectedProfileRepairIds.length || batchLoading" @click="batchRepairProfiles">{{ batchLoading ? '修复中...' : `批量重建 ${selectedProfileRepairIds.length} 份` }}</Button>
          </div>
          <div id="knowledge-route-anomaly-panel" v-show="!anomalyCollapsed">
            <div class="mt-3 flex items-center gap-2.5">
              <label class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-secondary px-3 py-2 text-compact text-foreground">
                <Checkbox :model-value="Boolean(allVisibleAnomaliesSelected)" @update:model-value="toggleAllVisibleAnomalies" />
                <span>全选异常</span>
              </label>
            </div>
            <div class="mt-3 grid gap-3" style="grid-template-columns:repeat(auto-fit,minmax(220px,1fr))">
              <article v-for="item in profileAnomalyRows" :key="`anomaly-${item.documentId}`"
                class="grid grid-cols-[auto_1fr_auto] items-start gap-3 rounded-xl border p-3.5"
                :class="item.tone === 'danger' ? 'bg-gradient-to-br from-red-500/[0.06] to-white/92' : 'bg-gradient-to-br from-amber-500/[0.07] to-white/90'">
                <label class="inline-flex cursor-pointer items-center">
                  <Checkbox :aria-label="`选择异常文档 ${item.documentName}`" :model-value="selectedProfileRepairIds.includes(item.documentId)" @update:model-value="() => toggleProfileRepair(item.documentId)" />
                </label>
                <div class="grid gap-2">
                  <strong class="text-sm text-foreground">{{ item.documentName }}</strong>
                  <span class="text-xs text-muted-foreground">{{ item.scopeText }}</span>
                  <div class="flex flex-wrap gap-1.5">
                    <span v-for="problem in item.problems" :key="`${item.documentId}-${problem}`" class="inline-flex rounded-full bg-amber-500/[0.14] px-2.5 py-1 text-xs text-amber-700">{{ problem }}</span>
                  </div>
                </div>
                <Button variant="outline" class="rounded-md" size="sm" type="button" @click.stop="selectAnomalyDocument(item); openDrawer('profile', selectedProfileDocument, 'view')">查看</Button>
              </article>
            </div>
          </div>
        </div>

        <div class="mt-4 grid max-h-[520px] gap-3 overflow-y-auto" style="grid-template-columns:repeat(auto-fill,minmax(280px,1fr))">
          <div v-for="item in filteredDocuments" :key="item.documentId"
            class="grid cursor-pointer gap-2 rounded-xl border p-3.5 transition-all"
            :class="item.documentId === profileDocumentId ? 'border-primary/30 bg-primary/[0.04]' : 'border-border bg-secondary hover:border-primary/20 hover:shadow-sm'"
            role="button"
            tabindex="0"
            @click="selectDocument(item); openDrawer('profile', item, 'view')"
            @keydown.enter.prevent="selectDocument(item); openDrawer('profile', item, 'view')"
            @keydown.space.prevent="selectDocument(item); openDrawer('profile', item, 'view')">
            <strong class="text-sm text-foreground">{{ item.documentName }}</strong>
            <small class="text-xs text-muted-foreground">{{ documentMetaLine(item) }}</small>
          </div>
          <div v-if="!filteredDocuments.length" class="text-sm text-muted-foreground">没有匹配的文档。</div>
        </div>
      </div>
    </section>

    <section id="knowledge-route-panel-relation" v-show="activeTab === 'relation'" class="tab-content" role="tabpanel" aria-labelledby="knowledge-route-tab-relation" tabindex="0">
      <div class="glass-card glass-edge rounded-glass border p-[22px]">
        <div class="flex items-start justify-between gap-4 max-[900px]:flex-col">
          <div><h3 class="m-0 text-sm font-semibold text-foreground">主题文档关联</h3><p class="mt-1 text-sm text-muted-foreground">把"哪个主题该优先看哪份文档"显式维护下来，低置信自动路由时会直接受益。</p></div>
          <Button size="lg" class="rounded-md" type="button" @click="openCreateDrawer('relation')">新建关联</Button>
        </div>
        <div class="mt-3.5 grid grid-cols-[180px_minmax(0,1fr)_auto] items-center gap-2.5 max-[1080px]:grid-cols-1">
          <Select v-model="relationScopeFilterValue">
            <SelectTrigger
              aria-label="按知识范围筛选关联"
              class="h-auto w-full min-w-0 rounded-md border border-border bg-card px-3 py-2.5 text-sm text-foreground *:data-[slot=select-value]:min-w-0"
              :title="relationScopeFilterId ? scopeNameText(relationScopeFilterId) : '全部范围'"
            >
              <SelectValue placeholder="全部范围" />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectItem value="__all__">全部范围</SelectItem>
                <SelectItem v-for="item in scopes" :key="item.scopeCode" :value="String(item.scopeCode)">{{ item.scopeName }}</SelectItem>
              </SelectGroup>
            </SelectContent>
          </Select>
          <Input v-model="relationKeyword" aria-label="筛选主题文档关联" class="h-9 text-sm" placeholder="按主题、文档、原因筛选关联结果" />
          <Button variant="outline" class="rounded-md" size="lg" type="button" :disabled="actionLoading" @click="loadRelations">刷新</Button>
        </div>
        <div class="mt-3.5 flex items-center gap-2.5">
          <span class="inline-flex items-center rounded-full bg-secondary px-3 py-1.5 text-xs text-foreground">{{ relations.length }} 条可见关联</span>
        </div>
        <div class="mt-3.5 grid max-h-[520px] gap-2 overflow-y-auto">
          <article v-for="item in relations" :key="`${item.topicCode}-${item.documentId}`"
            class="flex items-center justify-between gap-3 rounded-xl border border-border bg-secondary p-3 transition-colors hover:border-primary/20 max-[900px]:flex-col max-[900px]:items-start">
            <div class="grid gap-1 min-w-0">
              <strong class="text-sm text-foreground">{{ item.documentName }}</strong>
              <span class="text-xs text-muted-foreground">{{ item.topicName || topicNameText(item.topicCode) }} · 分数 {{ item.relationScore }} · {{ item.knowledgeScopeName || item.knowledgeScopeCode || topicScopeText(item.topicCode) }}</span>
              <small class="text-xs text-muted-foreground">{{ item.reason || documentMetaLine(item) }}</small>
            </div>
            <div class="flex shrink-0 gap-2">
              <Button variant="outline" class="rounded-md" size="sm" type="button" @click="openDrawer('relation', item, 'view')">查看详情</Button>
              <Button variant="outline" class="rounded-md border-destructive/[0.14] bg-destructive/[0.06] text-destructive hover:bg-destructive/10" size="sm" type="button" :disabled="actionLoading" @click="removeRelation(item)">移除</Button>
            </div>
          </article>
          <div v-if="!relations.length" class="text-sm text-muted-foreground">当前筛选下还没有保存的文档关联。</div>
        </div>
      </div>
    </section>

    <ChildPageDialog
      :open="drawerVisible"
      :title="drawerTitle"
      :description="drawerSubtitle"
      close-label="关闭知识路由子页面"
      @update:open="handleDrawerOpen"
    >

          <template v-if="drawerType === 'scope'">
            <template v-if="drawerMode === 'view' && drawerTarget">
              <div class="grid gap-3.5">
                <div v-for="row in scopeViewRows" :key="row.label" class="grid gap-1"><span class="text-xs text-muted-foreground">{{ row.label }}</span><strong class="text-sm text-foreground">{{ row.value }}</strong></div>
                <div v-if="drawerTarget.aliases" class="grid gap-2"><p class="m-0 text-xs text-muted-foreground">别名</p><div class="flex flex-wrap gap-2"><span v-for="a in parseTextList(drawerTarget.aliases)" :key="a" class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-foreground">{{ a }}</span></div></div>
                <div v-if="drawerTarget.examples" class="grid gap-2"><p class="m-0 text-xs text-muted-foreground">典型问题</p><div class="flex flex-wrap gap-2"><span v-for="e in parseJsonArray(drawerTarget.examples)" :key="e" class="inline-flex rounded-full bg-primary/[0.08] px-2.5 py-1 text-xs text-[var(--accent-foreground)]">{{ e }}</span></div></div>
              </div>
            </template>
            <template v-if="drawerMode === 'edit'">
              <div class="mt-4 grid gap-2.5">
                <Input v-model="scopeForm.scopeCode" aria-label="范围编码" class="h-9 text-sm" placeholder="范围编码，例如 operation_rule" />
                <p class="-mt-1 m-0 text-xs text-muted-foreground">范围编码用于与文档知识域和后端路由合同对齐。</p>
                <Input v-model="scopeForm.scopeName" aria-label="范围名称" class="h-9 text-sm" placeholder="范围名称，例如 运营规则" />
                <Select v-model="scopeForm.parentScopeCode">
                  <SelectTrigger aria-label="父级范围" class="drawer-input h-auto">
                    <SelectValue placeholder="不设置父级范围" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem value="__none__">不设置父级范围</SelectItem>
                      <SelectItem v-for="item in scopeParentOptions" :key="item.scopeCode" :value="String(item.scopeCode)">{{ item.scopeName }}</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
                <Input v-model="scopeForm.aliases" aria-label="范围别名" class="h-9 text-sm" placeholder="别名，英文逗号分隔" />
                <Input v-model="scopeForm.sortOrder" aria-label="范围排序值" class="h-9 text-sm" placeholder="排序值" />
                <Textarea v-model="scopeForm.description" aria-label="范围描述" class="min-h-[74px]" placeholder="范围描述" />
                <Textarea v-model="scopeForm.examples" aria-label="范围典型问题 JSON" class="min-h-[74px]" placeholder='典型问题 JSON，例如 ["上线观察多久"]' />
              </div>
            </template>
          </template>

          <template v-if="drawerType === 'topic'">
            <template v-if="drawerMode === 'view' && drawerTarget">
              <div class="grid gap-3.5">
                <div v-for="row in topicViewRows" :key="row.label" class="grid gap-1"><span class="text-xs text-muted-foreground">{{ row.label }}</span><strong class="text-sm text-foreground">{{ row.value }}</strong></div>
                <div v-if="drawerTarget.aliases" class="grid gap-2"><p class="m-0 text-xs text-muted-foreground">别名</p><div class="flex flex-wrap gap-2"><span v-for="a in parseTextList(drawerTarget.aliases)" :key="a" class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-foreground">{{ a }}</span></div></div>
                <div v-if="drawerTarget.examples" class="grid gap-2"><p class="m-0 text-xs text-muted-foreground">典型问题</p><div class="flex flex-wrap gap-2"><span v-for="e in parseJsonArray(drawerTarget.examples)" :key="e" class="inline-flex rounded-full bg-primary/[0.08] px-2.5 py-1 text-xs text-[var(--accent-foreground)]">{{ e }}</span></div></div>
              </div>
            </template>
            <template v-if="drawerMode === 'edit'">
              <div class="mt-4 grid gap-2.5">
                <Input v-model="topicForm.topicCode" aria-label="主题编码" class="h-9 text-sm" placeholder="主题编码，例如 deploy_check" />
                <Input v-model="topicForm.topicName" aria-label="主题名称" class="h-9 text-sm" placeholder="主题名称" />
                <Select v-model="topicForm.scopeCode">
                  <SelectTrigger aria-label="主题所属范围" class="drawer-input h-auto">
                    <SelectValue placeholder="选择所属范围" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem v-for="item in scopes" :key="item.scopeCode" :value="String(item.scopeCode)">{{ item.scopeName }}</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
                <Input v-model="topicForm.aliases" aria-label="主题别名" class="h-9 text-sm" placeholder="别名，英文逗号分隔" />
                <Select v-model="topicForm.answerShape">
                  <SelectTrigger aria-label="主题回答形态" class="drawer-input h-auto">
                    <SelectValue placeholder="选择回答形态" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem v-for="item in ANSWER_SHAPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
                <Select v-model="topicForm.executionPreference">
                  <SelectTrigger aria-label="主题执行偏好" class="drawer-input h-auto">
                    <SelectValue placeholder="选择执行偏好" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem v-for="item in EXECUTION_PREFERENCE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
                <Input v-model="topicForm.sortOrder" aria-label="主题排序值" class="h-9 text-sm" placeholder="排序值" />
                <Textarea v-model="topicForm.description" aria-label="主题描述" class="min-h-[74px]" placeholder="主题描述" />
                <Textarea v-model="topicForm.examples" aria-label="主题典型问题 JSON" class="min-h-[74px]" placeholder="典型问题 JSON" />
              </div>
            </template>
          </template>

          <template v-if="drawerType === 'profile'">
            <div class="grid gap-3.5">
              <div class="grid gap-1"><span class="text-xs text-muted-foreground">文档名称</span><strong class="text-sm text-foreground">{{ selectedProfileDocument?.documentName || '-' }}</strong></div>
              <div class="grid gap-1"><span class="text-xs text-muted-foreground">元数据</span><small class="text-xs text-muted-foreground">{{ selectedProfileDocumentMeta }}</small></div>
            </div>
            <div class="mt-3 flex flex-wrap gap-2.5">
              <Button class="rounded-md" type="button" :disabled="!profileDocumentId || actionLoading" @click="loadProfile">查看画像</Button>
              <Button variant="outline" class="rounded-md" type="button" :disabled="!profileDocumentId || actionLoading" @click="regenerateProfile">重新生成</Button>
            </div>
            <div v-if="profile" class="mt-4 grid gap-4 rounded-xl border border-border bg-secondary p-4">
              <div class="flex items-start justify-between gap-3 max-[900px]:flex-col">
                <strong class="text-sm text-foreground">{{ selectedProfileDocument?.documentName || `文档 ${profileDocumentId}` }}</strong>
                <span class="inline-flex whitespace-nowrap rounded-full px-2.5 py-1.5 text-xs font-bold" :class="profileStatusClass(profile.profileStatus)">{{ profileStatusText(profile.profileStatus) }}</span>
              </div>
              <p class="m-0 leading-[1.75] text-sm text-muted-foreground">{{ profile.documentSummary || '当前画像还没有生成摘要。' }}</p>
              <div class="grid grid-cols-2 gap-2.5">
                <article v-for="card in profileMiniCards" :key="card.label" class="grid gap-1.5 rounded-lg border border-border bg-card p-3">
                  <span class="text-xs text-muted-foreground">{{ card.label }}</span><strong class="text-sm text-foreground">{{ card.value }}</strong>
                </article>
              </div>
              <div class="grid gap-2"><p class="m-0 text-xs text-muted-foreground">核心主题</p><div class="flex flex-wrap gap-1.5"><span v-for="item in parseJsonArray(profile.coreTopics)" :key="`dt-${item}`" class="inline-flex rounded-full bg-primary/[0.08] px-2.5 py-1 text-xs text-[var(--accent-foreground)]">{{ item }}</span><span v-if="!parseJsonArray(profile.coreTopics).length" class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-muted-foreground">暂无</span></div></div>
              <div class="grid gap-2"><p class="m-0 text-xs text-muted-foreground">示例问题</p><div class="flex flex-wrap gap-1.5"><span v-for="item in parseJsonArray(profile.exampleQuestions)" :key="`dq-${item}`" class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-foreground">{{ item }}</span><span v-if="!parseJsonArray(profile.exampleQuestions).length" class="inline-flex rounded-full bg-foreground/[0.06] px-2.5 py-1 text-xs text-muted-foreground">暂无</span></div></div>
            </div>
          </template>

          <template v-if="drawerType === 'relation'">
            <template v-if="drawerMode === 'view' && drawerTarget">
              <div class="grid gap-3.5">
                <div class="grid gap-1"><span class="text-xs text-muted-foreground">主题</span><strong class="text-sm text-foreground">{{ drawerTarget.topicName || topicNameText(drawerTarget.topicCode) }}</strong></div>
                <div class="grid gap-1"><span class="text-xs text-muted-foreground">文档名称</span><strong class="text-sm text-foreground">{{ drawerTarget.documentName }}</strong></div>
                <div class="grid gap-1"><span class="text-xs text-muted-foreground">关联分数</span><strong class="text-sm text-foreground">{{ drawerTarget.relationScore }}</strong></div>
                <div class="grid gap-1"><span class="text-xs text-muted-foreground">关联来源</span><strong class="text-sm text-foreground">{{ drawerTarget.relationSource || '-' }}</strong></div>
                <div class="grid gap-1"><span class="text-xs text-muted-foreground">原因</span><p class="m-0 text-sm leading-relaxed text-foreground">{{ drawerTarget.reason || '未填写' }}</p></div>
              </div>
            </template>
            <template v-if="drawerMode === 'edit'">
              <div class="mt-4 grid gap-2.5">
                <Select v-model="relationForm.topicCode">
                  <SelectTrigger aria-label="关联主题" class="drawer-input h-auto">
                    <SelectValue placeholder="选择主题" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem v-for="item in relationTopicOptions" :key="item.topicCode" :value="String(item.topicCode)">{{ item.topicName }}</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
                <Select v-model="relationForm.documentId">
                  <SelectTrigger aria-label="关联文档" class="drawer-input h-auto">
                    <SelectValue placeholder="选择文档" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem v-for="item in documents" :key="item.documentId" :value="item.documentId">{{ item.documentName }}</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select>
                <Input v-model="relationForm.relationScore" aria-label="关联分数" class="h-9 text-sm" placeholder="关联分数，例如 0.9200" />
                <Input v-model="relationForm.reason" aria-label="关联原因" class="h-9 text-sm" placeholder="关联原因" />
              </div>
            </template>
          </template>
        <template #footer>
          <div class="flex w-full flex-wrap gap-2.5">
          <template v-if="drawerType === 'scope'">
            <template v-if="drawerMode === 'view'"><Button class="rounded-md" size="sm" type="button" @click="switchDrawerToEdit">编辑</Button><Button variant="outline" class="rounded-md border-destructive/[0.14] bg-destructive/[0.06] text-destructive hover:bg-destructive/10" size="sm" type="button" :disabled="actionLoading" @click="deleteScope">删除</Button></template>
            <template v-else><Button class="rounded-md" size="sm" type="button" :disabled="actionLoading" @click="saveScope">保存</Button><Button variant="outline" class="rounded-md" size="sm" type="button" @click="closeDrawer">取消</Button></template>
          </template>
          <template v-if="drawerType === 'topic'">
            <template v-if="drawerMode === 'view'"><Button class="rounded-md" size="sm" type="button" @click="switchDrawerToEdit">编辑</Button><Button variant="outline" class="rounded-md border-destructive/[0.14] bg-destructive/[0.06] text-destructive hover:bg-destructive/10" size="sm" type="button" :disabled="actionLoading" @click="deleteTopic">删除</Button></template>
            <template v-else><Button class="rounded-md" size="sm" type="button" :disabled="actionLoading" @click="saveTopic">保存</Button><Button variant="outline" class="rounded-md" size="sm" type="button" @click="closeDrawer">取消</Button></template>
          </template>
          <template v-if="drawerType === 'profile'"><Button variant="outline" class="rounded-md" size="sm" type="button" @click="closeDrawer">关闭</Button></template>
          <template v-if="drawerType === 'relation'">
            <template v-if="drawerMode === 'view'"><Button class="rounded-md" size="sm" type="button" @click="switchDrawerToEdit">编辑</Button><Button variant="outline" class="rounded-md border-destructive/[0.14] bg-destructive/[0.06] text-destructive hover:bg-destructive/10" size="sm" type="button" :disabled="actionLoading" @click="removeRelation(drawerTarget); closeDrawer()">移除</Button></template>
            <template v-else><Button class="rounded-md" size="sm" type="button" :disabled="actionLoading" @click="saveRelation">保存</Button><Button variant="outline" class="rounded-md" size="sm" type="button" @click="closeDrawer">取消</Button></template>
          </template>
          </div>
        </template>
    </ChildPageDialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { manageApi } from '../../api/api'
import { useConfirm } from '@/composables/useConfirm'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import { buildRelationRequest, buildScopeRequest, buildTopicRequest } from '@/features/admin/knowledgeRouteWorkflow'
const { confirm } = useConfirm()

const OPERATOR_ID = '10001'
const ANSWER_SHAPE_OPTIONS = Object.freeze([
  { value: 'list', label: '列表型回答' },
  { value: 'explain', label: '解释说明型回答' },
  { value: 'steps', label: '步骤型回答' }
])
const EXECUTION_PREFERENCE_OPTIONS = Object.freeze([
  { value: 'retrieval', label: '普通检索优先' },
  { value: 'graph_assist', label: '图辅助优先' }
])
const DOCUMENT_TYPE_OPTIONS = Object.freeze([
  { value: 'intro', label: '介绍型文档' }, { value: 'manual', label: '操作手册' },
  { value: 'rule', label: '规则文档' }, { value: 'faq', label: '常见问题' },
  { value: 'troubleshooting', label: '故障排查' }, { value: 'spec', label: '规格说明' }
])
const PROFILE_SOURCE_OPTIONS = Object.freeze([
  { value: 'auto', label: '自动生成' }, { value: 'manual', label: '手动维护' }, { value: 'mixed', label: '自动 + 手动' }
])
const ANSWER_SHAPE_LABEL_MAP = Object.freeze(ANSWER_SHAPE_OPTIONS.reduce((r, i) => { r[i.value] = i.label; return r }, {}))
const EXECUTION_PREFERENCE_LABEL_MAP = Object.freeze(EXECUTION_PREFERENCE_OPTIONS.reduce((r, i) => { r[i.value] = i.label; return r }, {}))
const DOCUMENT_TYPE_LABEL_MAP = Object.freeze(DOCUMENT_TYPE_OPTIONS.reduce((r, i) => { r[i.value] = i.label; return r }, {}))
const PROFILE_SOURCE_LABEL_MAP = Object.freeze(PROFILE_SOURCE_OPTIONS.reduce((r, i) => { r[i.value] = i.label; return r }, {}))

const loading = ref(false)
const actionLoading = ref(false)
const batchLoading = ref(false)
const scopes = ref([])
const topics = ref([])
const documents = ref([])
const allRelations = ref([])
const profileDocumentId = ref('')
const profile = ref(null)
const activeScopeId = ref('')
const activeScopeFilterValue = computed({
  get: () => activeScopeId.value || '__all__',
  set: (val) => { activeScopeId.value = val === '__all__' ? '' : val }
})
const relationScopeFilterId = ref('')
const relationScopeFilterValue = computed({
  get: () => relationScopeFilterId.value || '__all__',
  set: (val) => { relationScopeFilterId.value = val === '__all__' ? '' : val }
})
const activeTopicId = ref('')
const scopeKeyword = ref('')
const topicKeyword = ref('')
const documentKeyword = ref('')
const relationKeyword = ref('')
const selectedProfileRepairIds = ref([])
const scopeSectionRef = ref(null)
const profileSectionRef = ref(null)
const relationSectionRef = ref(null)
const notice = reactive({ type: 'info', message: '' })
const activeTab = ref('scope')
const TAB_LIST = Object.freeze([
  { key: 'scope', label: '知识范围', step: 1, hint: '定义知识领域边界' },
  { key: 'topic', label: '知识主题', step: 2, hint: '范围下的可回答单元' },
  { key: 'profile', label: '文档画像', step: 3, hint: '文档类型与能力分析' },
  { key: 'relation', label: '主题文档关联', step: 4, hint: '主题与文档的绑定关系' }
])

function handleTabKeydown(event, currentKey) {
  const currentIndex = TAB_LIST.findIndex((item) => item.key === currentKey)
  if (currentIndex < 0) return

  let nextIndex = currentIndex
  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') nextIndex = (currentIndex + 1) % TAB_LIST.length
  else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') nextIndex = (currentIndex - 1 + TAB_LIST.length) % TAB_LIST.length
  else if (event.key === 'Home') nextIndex = 0
  else if (event.key === 'End') nextIndex = TAB_LIST.length - 1
  else return

  event.preventDefault()
  const nextTab = TAB_LIST[nextIndex]
  activeTab.value = nextTab.key
  document.getElementById(`knowledge-route-tab-${nextTab.key}`)?.focus()
}

const drawerVisible = ref(false)
const drawerMode = ref('view')
const drawerTarget = ref(null)
const drawerType = ref('')
const drawerTitle = computed(() => {
  if (drawerType.value === 'scope') return drawerMode.value === 'edit' && !drawerTarget.value ? '新建知识范围' : '知识范围详情'
  if (drawerType.value === 'topic') return drawerMode.value === 'edit' && !drawerTarget.value ? '新建知识主题' : '知识主题详情'
  if (drawerType.value === 'profile') return '文档画像详情'
  if (drawerType.value === 'relation') return drawerMode.value === 'edit' && !drawerTarget.value ? '新建主题文档关联' : '关联详情'
  return '知识路由详情'
})
const drawerSubtitle = computed(() => {
  if (drawerType.value === 'profile') return '查看当前文档画像、生成状态与核心主题。'
  if (drawerType.value === 'relation') return '维护主题与文档的显式关联，不在前端重算路由。'
  return drawerMode.value === 'edit' ? '保存前会校验字段，关闭会保留已提交的数据。' : '当前对象的服务端字段与维护动作。'
})
const coveragePanelCollapsed = ref(false)
const anomalyCollapsed = ref(true)
const scopeForm = reactive({ id: '', scopeCode: '', scopeName: '', parentScopeCode: '', description: '', aliases: '', examples: '', sortOrder: '0', operatorId: OPERATOR_ID })
const topicForm = reactive({ id: '', topicCode: '', topicName: '', scopeCode: '', description: '', aliases: '', examples: '', answerShape: '', executionPreference: '', sortOrder: '0', operatorId: OPERATOR_ID })
const relationForm = reactive({ topicCode: '', documentId: '', relationScore: '0.9000', relationSource: 'manual', reason: '', operatorId: OPERATOR_ID })

const activeScope = computed(() => scopes.value.find((item) => sameId(item.scopeCode, activeScopeId.value)) || null)
const activeTopic = computed(() => topics.value.find((item) => sameId(item.topicCode, activeTopicId.value)) || null)
const scopeParentOptions = computed(() => scopes.value.filter((item) => !scopeForm.id || !sameId(item.scopeCode, scopeForm.scopeCode)))
const relationTopicOptions = computed(() => activeScopeId.value
  ? topics.value.filter((item) => sameId(item.scopeCode, activeScopeId.value))
  : topics.value)
const filteredScopes = computed(() => {
  const keyword = scopeKeyword.value.trim().toLowerCase()
  if (!keyword) return scopes.value
  return scopes.value.filter((item) => [item.scopeCode, item.scopeName, item.description, item.aliases].filter(Boolean).join(' ').toLowerCase().includes(keyword))
})
const filteredTopics = computed(() => {
  const keyword = topicKeyword.value.trim().toLowerCase()
  return topics.value.filter((item) => {
    if (activeScopeId.value && !sameId(item.scopeCode, activeScopeId.value)) return false
    if (!keyword) return true
    return [item.topicCode, item.topicName, item.description, item.aliases].filter(Boolean).join(' ').toLowerCase().includes(keyword)
  })
})
const filteredDocuments = computed(() => {
  const keyword = documentKeyword.value.trim().toLowerCase()
  return documents.value.filter((item) => {
    if (!keyword) return true
    return [item.documentName, item.originalFileName, item.knowledgeScopeCode, item.knowledgeScopeName, item.businessCategory, item.documentTags].filter(Boolean).join(' ').toLowerCase().includes(keyword)
  })
})
const selectedProfileDocument = computed(() => documents.value.find((item) => item.documentId === profileDocumentId.value) || null)
const selectedProfileDocumentMeta = computed(() => selectedProfileDocument.value ? documentMetaLine(selectedProfileDocument.value) : '未选择文档')
const selectedScopeAliases = computed(() => parseTextList(activeScope.value?.aliases))
const selectedScopeExamples = computed(() => parseJsonArray(activeScope.value?.examples))
const selectedScopeStats = computed(() => {
  if (!activeScope.value) return null
  const scopeTopics = topics.value.filter((item) => sameId(item.scopeCode, activeScope.value.scopeCode))
  const topicCodes = new Set(scopeTopics.map((item) => item.topicCode))
  const scopeRelations = allRelations.value.filter((item) => topicCodes.has(String(item.topicCode)))
  return { topicCount: scopeTopics.length, relationCount: scopeRelations.length, documentCount: new Set(scopeRelations.map((item) => String(item.documentId))).size }
})
const selectedTopicRelations = computed(() => activeTopic.value ? allRelations.value.filter((item) => sameId(item.topicCode, activeTopic.value.topicCode)) : [])
const selectedTopicStats = computed(() => {
  if (!activeTopic.value) return null
  const scores = selectedTopicRelations.value.map((item) => Number(item.relationScore)).filter((item) => Number.isFinite(item))
  return { relationCount: selectedTopicRelations.value.length, linkedDocumentCount: new Set(selectedTopicRelations.value.map((item) => item.documentId)).size, averageScoreText: scores.length ? (scores.reduce((s, i) => s + i, 0) / scores.length).toFixed(4) : '-' }
})
const relations = computed(() => {
  const keyword = relationKeyword.value.trim().toLowerCase()
  return allRelations.value.filter((item) => {
    const topic = topics.value.find((t) => sameId(t.topicCode, item.topicCode))
    if (relationScopeFilterId.value && !sameId(topic?.scopeCode, relationScopeFilterId.value)) return false
    if (!keyword) return true
    return [item.topicName, topic?.topicName, item.knowledgeScopeName, item.knowledgeScopeCode, scopeNameText(topic?.scopeCode), item.documentName, item.reason].filter(Boolean).join(' ').toLowerCase().includes(keyword)
  })
})
const selectedProfileRelatedTopics = computed(() => {
  if (!selectedProfileDocument.value) return []
  const topicCodes = new Set(allRelations.value.filter((item) => sameId(item.documentId, selectedProfileDocument.value.documentId)).map((item) => String(item.topicCode)))
  return topics.value.filter((item) => topicCodes.has(String(item.topicCode)))
})
const scopeCoverageRows = computed(() => scopes.value.map((scope) => {
  const scopeTopics = topics.value.filter((t) => sameId(t.scopeCode, scope.scopeCode))
  const topicCodes = new Set(scopeTopics.map((t) => t.topicCode))
  const scopeRelations = allRelations.value.filter((r) => topicCodes.has(String(r.topicCode)))
  const coveredTopicCodes = new Set(scopeRelations.map((r) => String(r.topicCode)))
  const linkedDocumentIds = new Set(scopeRelations.map((r) => String(r.documentId)))
  const coverageRate = scopeTopics.length ? (coveredTopicCodes.size / scopeTopics.length) * 100 : 0
  return { scopeCode: scope.scopeCode, scopeName: scope.scopeName, topicCount: scopeTopics.length, coveredTopicCount: coveredTopicCodes.size, pendingTopicCount: Math.max(0, scopeTopics.length - coveredTopicCodes.size), documentCount: linkedDocumentIds.size, relationCount: scopeRelations.length, coverageRate, coverageRateText: `${coverageRate.toFixed(0)}%` }
}))
const overallCoverageRateText = computed(() => {
  if (!topics.value.length) return '0%'
  const coveredTopicCodes = new Set(allRelations.value.map((r) => String(r.topicCode)))
  return `${((coveredTopicCodes.size / topics.value.length) * 100).toFixed(0)}%`
})
const profileAnomalyRows = computed(() => {
  const linkedDocumentIds = new Set(allRelations.value.map((r) => String(r.documentId)))
  return documents.value.map((document) => {
    const problems = []
    if (!linkedDocumentIds.has(String(document.documentId))) problems.push('未绑定主题')
    return { documentId: String(document.documentId), documentName: document.documentName, scopeText: documentMetaLine(document), problems, tone: problems.length >= 2 ? 'danger' : 'warning', suggestion: buildAnomalySuggestion(problems) }
  }).filter((item) => item.problems.length > 0)
})
const allVisibleAnomaliesSelected = computed(() => profileAnomalyRows.value.length && profileAnomalyRows.value.every((item) => selectedProfileRepairIds.value.includes(item.documentId)))
const summaryCards = computed(() => {
  const documentWithMetaCount = documents.value.filter((item) => {
    return Boolean(item.knowledgeScopeCode || item.knowledgeScopeName || item.businessCategory || item.documentTags)
  }).length
  const pendingTopicCount = topics.value.filter((item) => !allRelations.value.some((r) => sameId(r.topicCode, item.topicCode))).length
  return [
    { label: '知识范围', value: String(scopes.value.length), description: '知识范围是自动路由的第一层收敛边界' },
    { label: '知识主题', value: String(topics.value.length), description: '主题是范围里的可回答单元' },
    { label: '文档数', value: String(documents.value.length), description: '当前可维护画像和路由关系的文档数量' },
    { label: '已补元数据文档', value: String(documentWithMetaCount), description: '至少填了范围、业务类目或标签的文档数' },
    { label: '已保存关联', value: String(allRelations.value.length), description: '当前所有主题已保存的文档关联数' },
    { label: '未关联主题', value: String(pendingTopicCount), description: '还没有绑定任何文档关系的主题数' }
  ]
})
const scopeViewRows = computed(() => !drawerTarget.value ? [] : [
  { label: '知识域编码', value: drawerTarget.value.scopeCode || '-' },
  { label: '范围名称', value: drawerTarget.value.scopeName },
  { label: '父级范围', value: scopeNameText(drawerTarget.value.parentScopeCode) || '-' },
  { label: '排序值', value: drawerTarget.value.sortOrder },
  { label: '描述', value: drawerTarget.value.description || '暂无描述' }
])
const topicViewRows = computed(() => !drawerTarget.value ? [] : [
  { label: '主题名称', value: drawerTarget.value.topicName },
  { label: '所属范围', value: scopeNameText(drawerTarget.value.scopeCode) || '-' },
  { label: '回答形态', value: formatAnswerShapeLabel(drawerTarget.value.answerShape) },
  { label: '执行偏好', value: formatExecutionPreferenceLabel(drawerTarget.value.executionPreference) },
  { label: '排序值', value: drawerTarget.value.sortOrder },
  { label: '描述', value: drawerTarget.value.description || '暂无描述' }
])
const profileMiniCards = computed(() => !profile.value ? [] : [
  { label: '文档类型', value: formatDocumentTypeLabel(profile.value.documentType) },
  { label: '画像来源', value: formatProfileSourceLabel(profile.value.profileSource) },
  { label: '图能力', value: graphCapabilityText(profile.value) },
  { label: '核心主题数', value: parseJsonArray(profile.value.coreTopics).length }
])

watch(() => relationForm.topicCode, (value) => {
  activeTopicId.value = value || ''
  const currentTopic = topics.value.find((item) => sameId(item.topicCode, value))
  if (currentTopic?.scopeCode) activeScopeId.value = currentTopic.scopeCode
})

function noticeClass(type) {
  if (type === 'success') return 'bg-[var(--status-success-fg)]/10 text-[var(--status-success-fg)]'
  if (type === 'danger') return 'bg-[var(--status-danger-fg)]/10 text-[var(--status-danger-fg)]'
  return 'bg-primary/[0.08] text-primary'
}
function openDrawer(type, item, mode = 'view') {
  if (item && type === 'scope') activeScopeId.value = item.scopeCode || ''
  if (item && type === 'topic') { activeTopicId.value = item.topicCode || ''; activeScopeId.value = item.scopeCode || activeScopeId.value }
  drawerType.value = type
  drawerTarget.value = item ? { ...item } : null
  drawerMode.value = mode
  drawerVisible.value = true
}
function closeDrawer() { drawerVisible.value = false; drawerTarget.value = null; drawerMode.value = 'view'; drawerType.value = '' }
function handleDrawerOpen(value) {
  if (value) drawerVisible.value = true
  else closeDrawer()
}
function switchDrawerToEdit() {
  drawerMode.value = 'edit'
  if (drawerType.value === 'scope' && drawerTarget.value) { editScope(drawerTarget.value) }
  else if (drawerType.value === 'topic' && drawerTarget.value) { editTopic(drawerTarget.value) }
  else if (drawerType.value === 'relation' && drawerTarget.value) { Object.assign(relationForm, { topicCode: drawerTarget.value.topicCode || '', documentId: drawerTarget.value.documentId || '', relationScore: drawerTarget.value.relationScore || '0.9000', relationSource: 'manual', reason: drawerTarget.value.reason || '', operatorId: OPERATOR_ID }) }
}
function openCreateDrawer(type) {
  if (type === 'scope') resetScopeForm()
  else if (type === 'topic') resetTopicForm()
  else if (type === 'relation') Object.assign(relationForm, { topicCode: activeTopicId.value || '', documentId: '', relationScore: '0.9000', relationSource: 'manual', reason: '', operatorId: OPERATOR_ID })
  openDrawer(type, null, 'edit')
}
function scrollToSection(section) {
  const element = { scope: scopeSectionRef.value, profile: profileSectionRef.value, relation: relationSectionRef.value }[section]
  element?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function focusCoverageScope(item) { activeScopeId.value = item.scopeCode; const scope = scopes.value.find((s) => sameId(s.scopeCode, item.scopeCode)); if (scope) editScope(scope); scrollToSection('scope') }
function showNotice(message, type = 'info') { notice.message = message; notice.type = type }
function sameId(left, right) { return String(left || '') === String(right || '') }
function scopeNameText(scopeCode) {
  if (!scopeCode) return ''
  return scopes.value.find((item) => sameId(item.scopeCode, scopeCode))?.scopeName || ''
}
function topicNameText(topicCode) {
  if (!topicCode) return ''
  return topics.value.find((item) => sameId(item.topicCode, topicCode))?.topicName || ''
}
function normalizeScopeItem(item = {}) {
  const scopeCode = String(item.scopeCode || item.id || '')
  return { ...item, id: String(item.id || ''), scopeCode, parentScopeCode: item.parentScopeCode || '' }
}
function normalizeTopicItem(item = {}) {
  const topicCode = String(item.topicCode || item.id || '')
  const scopeCode = String(item.scopeCode || '')
  return { ...item, id: String(item.id || ''), topicCode, scopeCode }
}
function normalizeRelationItem(item = {}) {
  return {
    ...item,
    topicCode: String(item.topicCode || ''),
    knowledgeScopeCode: String(item.knowledgeScopeCode || item.scopeCode || ''),
    knowledgeScopeName: String(item.knowledgeScopeName || item.scopeName || '')
  }
}
function topicScopeText(topicCode) {
  const topic = topics.value.find((item) => sameId(item.topicCode, topicCode))
  if (!topic?.scopeCode) return '未分范围'
  return scopeNameText(topic.scopeCode) || '未分范围'
}
function linkedDocumentCountByScope(scopeCode) {
  const topicCodes = new Set(topics.value.filter((item) => sameId(item.scopeCode, scopeCode)).map((item) => String(item.topicCode)))
  return new Set(allRelations.value.filter((item) => topicCodes.has(String(item.topicCode))).map((item) => String(item.documentId))).size
}
function resetScopeForm() { Object.assign(scopeForm, { id: '', scopeCode: '', scopeName: '', parentScopeCode: '__none__', description: '', aliases: '', examples: '', sortOrder: '0', operatorId: OPERATOR_ID }); activeScopeId.value = '' }
function resetTopicForm() { Object.assign(topicForm, { id: '', topicCode: '', topicName: '', scopeCode: activeScopeId.value || '', description: '', aliases: '', examples: '', answerShape: '', executionPreference: '', sortOrder: '0', operatorId: OPERATOR_ID }); activeTopicId.value = '' }
function editScope(item) { activeScopeId.value = item.scopeCode; if (activeTopic.value && !sameId(activeTopic.value.scopeCode, item.scopeCode)) { activeTopicId.value = ''; relationForm.topicCode = '' }; Object.assign(scopeForm, { id: item.id || '', scopeCode: item.scopeCode || '', scopeName: item.scopeName || '', parentScopeCode: item.parentScopeCode || '', description: item.description || '', aliases: item.aliases || '', examples: item.examples || '', sortOrder: String(item.sortOrder || '0'), operatorId: OPERATOR_ID }); topicForm.scopeCode = String(item.scopeCode || '') }
function editTopic(item) { activeScopeId.value = item.scopeCode; activeTopicId.value = item.topicCode; relationForm.topicCode = item.topicCode; Object.assign(topicForm, { id: item.id || '', topicCode: item.topicCode || '', topicName: item.topicName || '', scopeCode: item.scopeCode || '', description: item.description || '', aliases: item.aliases || '', examples: item.examples || '', answerShape: item.answerShape || '', executionPreference: item.executionPreference || '', sortOrder: String(item.sortOrder || '0'), operatorId: OPERATOR_ID }) }
function selectDocument(item) { profileDocumentId.value = item.documentId; profile.value = null }
async function withAction(task, successMessage = '') {
  if (actionLoading.value) return null
  actionLoading.value = true
  try { const result = await task(); if (successMessage) showNotice(successMessage, 'success'); return result }
  catch (error) { showNotice(error.message || '执行失败', 'danger'); return null }
  finally { actionLoading.value = false }
}
async function loadAll() {
  loading.value = true
  try {
    const [scopeList, topicList, docPage] = await Promise.all([
      manageApi.listKnowledgeScopes(),
      manageApi.listKnowledgeTopics(),
      manageApi.queryDocumentPage({ pageNo: '1', pageSize: '200', keyword: '' })
    ])
    scopes.value = Array.isArray(scopeList) ? scopeList.map(normalizeScopeItem) : []
    topics.value = Array.isArray(topicList) ? topicList.map(normalizeTopicItem) : []
    documents.value = Array.isArray(docPage?.records) ? docPage.records : []
    if (activeScopeId.value && !scopes.value.some((item) => sameId(item.scopeCode, activeScopeId.value))) activeScopeId.value = ''
    if (relationScopeFilterId.value && !scopes.value.some((item) => sameId(item.scopeCode, relationScopeFilterId.value))) relationScopeFilterId.value = ''
    if (activeTopicId.value && !topics.value.some((item) => sameId(item.topicCode, activeTopicId.value))) { activeTopicId.value = ''; relationForm.topicCode = '' }
    await loadRelations()
  } catch (error) { showNotice(error.message || '加载知识路由数据失败', 'danger') }
  finally { loading.value = false }
}
async function saveScope() {
  await withAction(async () => {
    const payload = buildScopeRequest(scopeForm)
    const data = await manageApi.saveKnowledgeScope(payload)
    activeScopeId.value = data?.scopeCode || scopeForm.scopeCode
    await loadAll()
    closeDrawer()
  }, '知识范围已保存')
}
async function deleteScope() {
  if (!activeScope.value || !await confirm(`确认删除范围「${activeScope.value.scopeName}」吗？`, '确认删除')) return
  await withAction(async () => { await manageApi.deleteKnowledgeScope({ scopeCode: activeScope.value.scopeCode, operatorId: OPERATOR_ID }); resetScopeForm(); closeDrawer(); await loadAll() }, '知识范围已删除')
}
async function saveTopic() {
  await withAction(async () => {
    const payload = buildTopicRequest(topicForm)
    const data = await manageApi.saveKnowledgeTopic(payload)
    activeTopicId.value = data?.topicCode || payload.topicCode
    relationForm.topicCode = activeTopicId.value
    await loadAll()
    closeDrawer()
  }, '知识主题已保存')
}
async function deleteTopic() {
  if (!activeTopic.value || !await confirm(`确认删除主题「${activeTopic.value.topicName}」吗？`, '确认删除')) return
  await withAction(async () => { await manageApi.deleteKnowledgeTopic({ topicCode: activeTopic.value.topicCode, operatorId: OPERATOR_ID }); resetTopicForm(); relationForm.topicCode = ''; closeDrawer(); await loadAll() }, '知识主题已删除')
}
async function loadProfile() { if (!profileDocumentId.value) return; await withAction(async () => { profile.value = await manageApi.queryDocumentProfile({ documentId: profileDocumentId.value }) }) }
async function regenerateProfile() { if (!profileDocumentId.value) return; await withAction(async () => { profile.value = await manageApi.regenerateDocumentProfile({ documentId: profileDocumentId.value, operatorId: OPERATOR_ID }) }, '文档画像已重新生成') }
async function regenerateAllProfiles() {
  if (!documents.value.length || !await confirm(`确认批量重建 ${documents.value.length} 份文档画像吗？`, '批量重建画像')) return
  batchLoading.value = true
  try { await manageApi.batchRegenerateDocumentProfiles({ documentIds: documents.value.map((item) => item.documentId), operatorId: OPERATOR_ID }); showNotice(`已触发 ${documents.value.length} 份文档的画像重建`, 'success'); if (profileDocumentId.value) await loadProfile() }
  catch (error) { showNotice(error.message || '批量重建文档画像失败', 'danger') }
  finally { batchLoading.value = false }
}
async function batchRepairProfiles() {
  const documentIds = [...selectedProfileRepairIds.value]
  if (!documentIds.length) { showNotice('请先选择要批量修复的文档。', 'danger'); return }
  batchLoading.value = true
  try { await manageApi.batchRegenerateDocumentProfiles({ documentIds, operatorId: OPERATOR_ID }); selectedProfileRepairIds.value = []; showNotice(`已批量重建 ${documentIds.length} 份文档画像。`, 'success'); if (profileDocumentId.value) await loadProfile(); await loadAll() }
  catch (error) { showNotice(error.message || '批量重建文档画像失败', 'danger') }
  finally { batchLoading.value = false }
}
async function loadRelations() {
  try {
    const data = await manageApi.listTopicDocuments({ topicCode: '' })
    allRelations.value = Array.isArray(data) ? data.map(normalizeRelationItem) : []
  }
  catch (error) { showNotice(error.message || '加载主题文档关联失败', 'danger') }
}
async function saveRelation() {
  await withAction(async () => {
    const payload = buildRelationRequest(relationForm)
    await manageApi.saveTopicDocumentRelation(payload)
    await loadRelations()
    closeDrawer()
  }, '主题文档关联已保存')
}
async function removeRelation(item) { await withAction(async () => { await manageApi.removeTopicDocumentRelation({ topicCode: item.topicCode, documentId: item.documentId, operatorId: OPERATOR_ID }); await loadRelations() }, '主题文档关联已移除') }
function documentMetaLine(item = {}) { return [item.knowledgeScopeName || item.knowledgeScopeCode, item.businessCategory, item.documentTags].filter(Boolean).join(' · ') || '还没有知识域 / 类目 / 标签元数据' }
function parseTextList(value) { const n = String(value || '').trim(); if (!n) return []; return n.split(',').map((item) => item.trim()).filter(Boolean) }
function formatAnswerShapeLabel(value) { return formatMappedLabel(value, ANSWER_SHAPE_LABEL_MAP) }
function formatExecutionPreferenceLabel(value) { return formatMappedLabel(value, EXECUTION_PREFERENCE_LABEL_MAP) }
function formatDocumentTypeLabel(value) { return formatMappedLabel(value, DOCUMENT_TYPE_LABEL_MAP) }
function formatProfileSourceLabel(value) { return formatMappedLabel(value, PROFILE_SOURCE_LABEL_MAP) }
function formatMappedLabel(value, labelMap) { const n = String(value || '').trim(); if (!n) return '未设置'; return labelMap[n] || n }
function buildAnomalySuggestion(problems) {
  if (problems.includes('未绑定主题')) return '建议在主题文档关联区为该文档至少绑定 1 个核心主题。'
  return '建议重建画像后查看核心主题、示例问题和图能力是否恢复正常。'
}
function toggleProfileRepair(documentId) { const n = String(documentId); if (selectedProfileRepairIds.value.includes(n)) { selectedProfileRepairIds.value = selectedProfileRepairIds.value.filter((item) => item !== n); return }; selectedProfileRepairIds.value = [...selectedProfileRepairIds.value, n] }
function toggleAllVisibleAnomalies() {
  if (allVisibleAnomaliesSelected.value) { const visibleIds = new Set(profileAnomalyRows.value.map((item) => item.documentId)); selectedProfileRepairIds.value = selectedProfileRepairIds.value.filter((item) => !visibleIds.has(item)); return }
  const merged = new Set(selectedProfileRepairIds.value); profileAnomalyRows.value.forEach((item) => merged.add(item.documentId)); selectedProfileRepairIds.value = [...merged]
}
function selectAnomalyDocument(item) { profileDocumentId.value = item.documentId; profile.value = null; loadProfile() }
function parseJsonArray(value) { if (!value) return []; try { const parsed = JSON.parse(value); return Array.isArray(parsed) ? parsed.filter(Boolean) : [] } catch { return [] } }
function graphCapabilityText(profileValue = {}) { const enabled = []; if (String(profileValue.supportsGraphOutline) === '1') enabled.push('大纲导航'); if (String(profileValue.supportsItemLookup) === '1') enabled.push('条目定位'); if (String(profileValue.supportsGraphAssist) === '1') enabled.push('图辅助检索'); return enabled.length ? enabled.join(' / ') : '未开启' }
function profileStatusText(status) { if (String(status) === '2') return '已生成'; if (String(status) === '3') return '生成失败'; return '待生成' }
function profileStatusClass(status) { if (String(status) === '2') return 'bg-green-500/[0.12] text-green-700'; if (String(status) === '3') return 'bg-red-500/[0.12] text-red-700'; return 'bg-amber-500/[0.14] text-amber-700' }

onMounted(loadAll)
</script>

<style scoped>
/* 保留：Vue <transition> 钩子类名无法用 Tailwind 替换 */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
/* tab 切换淡入动画 */
.tab-content { animation: tabFadeIn 0.2s ease; }
@keyframes tabFadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
/* drawer 表单输入公共样式（多处复用，统一定义避免重复） */
.drawer-input { width: 100%; border: 1px solid var(--admin-border); border-radius: var(--radius-sm); padding: 10px 12px; background: var(--card); color: var(--foreground); }
.drawer-input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--accent); }
@media (prefers-reduced-motion: reduce) {
  .tab-content { animation: none; }
  .modal-enter-active, .modal-leave-active { transition: none; }
}
</style>
