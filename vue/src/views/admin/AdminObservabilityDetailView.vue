<template>
  <section class="flex flex-col gap-6">
    <div class="flex items-center justify-between gap-3">
      <RouterLink :to="sessionReturnTarget"
        class="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-sm font-semibold text-foreground hover:bg-secondary">
        <ArrowLeftIcon class="h-4 w-4" />返回会话轮次列表
      </RouterLink>
    </div>

    <div v-if="pageError" class="rounded-md border border-destructive/10 bg-destructive/[0.06] px-3.5 py-3 text-sm text-destructive">{{ pageError }}</div>
    <div v-if="loadingPage && !activeExchangeDetail" class="rounded-md border border-dashed border-border px-6 py-10 text-center text-sm text-muted-foreground">正在加载轮次详情...</div>
    <div v-else-if="!activeExchange" class="rounded-md border border-dashed border-border px-6 py-10 text-center text-sm text-muted-foreground">没有找到这条轮次，请返回会话页重新选择。</div>

    <template v-else>
      <header class="border-b border-border pb-5">
        <h2 class="my-2 text-xl font-semibold leading-snug text-foreground">{{ activeExchange.question || '未记录问题' }}</h2>
        <dl class="mt-4 flex flex-wrap gap-x-6 gap-y-2">
          <div v-for="pair in headerMetaPairs" :key="pair.dt" class="flex gap-2 text-compact">
            <dt class="text-muted-foreground">{{ pair.dt }}</dt>
            <dd class="m-0 text-foreground">{{ pair.dd }}</dd>
          </div>
        </dl>
      </header>

      <Tabs v-model="activeDetailTab" class="w-full">
        <TabsList class="flex w-full flex-wrap justify-start gap-1 bg-secondary">
          <TabsTrigger v-for="tab in detailTabs" :key="tab.value" :value="tab.value" class="flex-1 min-w-[120px]">{{ tab.label }}</TabsTrigger>
        </TabsList>
      </Tabs>

      <div v-if="loadingRetrievalData" class="rounded-md border border-border bg-secondary/40 px-3 py-2 text-xs text-muted-foreground" role="status">
        正在读取检索与通道观测；已加载的轮次阶段不会被清空。
      </div>
      <div v-if="retrievalError" class="rounded-md border border-destructive/20 bg-destructive/[0.05] px-3 py-2 text-xs text-destructive" role="status">
        {{ retrievalError }}；已保留另一份成功读取的检索观测。
      </div>
      <div v-if="benchmarkError" class="rounded-md border border-border bg-secondary/40 px-3 py-2 text-xs text-muted-foreground" role="status">
        {{ benchmarkError }}；当前仅显示本轮真实耗时。
      </div>

      <div v-show="(activeDetailTab === 'evidence' && !evidenceTabHasData) || (activeDetailTab === 'prompt' && !promptTabHasData)"
        class="rounded-md border border-dashed border-border px-6 py-10 text-center text-sm text-muted-foreground">
        该分类暂无可展示的数据。
      </div>

      <section v-show="activeDetailTab === 'retrieval'" v-if="loadingRetrievalData && !retrievalTabHasData" data-retrieval-flow-skeleton
        class="border-y border-border bg-secondary/25 px-4 py-5 sm:px-5" aria-label="正在加载检索汇流工作台">
        <div class="h-4 w-36 animate-pulse rounded bg-foreground/[0.08]"></div>
        <div class="mt-4 grid gap-3 sm:grid-cols-5">
          <span v-for="index in 5" :key="`retrieval-skeleton-stage-${index}`" class="h-12 animate-pulse rounded-md bg-foreground/[0.06]"></span>
        </div>
        <div class="mt-5 grid gap-2">
          <span v-for="index in 3" :key="`retrieval-skeleton-lane-${index}`" class="h-14 animate-pulse rounded-md bg-foreground/[0.06]"></span>
        </div>
      </section>

      <section v-show="activeDetailTab === 'retrieval'" v-else-if="!retrievalTabHasData"
        class="border-y border-border bg-secondary/25 px-6 py-10 text-center">
        <h3 class="m-0 text-base font-semibold text-foreground">暂无检索与融合观测</h3>
        <p class="mx-auto mb-0 mt-2 max-w-prose text-compact leading-relaxed text-muted-foreground">这轮没有返回通道执行或候选结果，因此无法绘制真实汇流关系。</p>
      </section>

      <section v-show="activeDetailTab === 'overview'">
        <h3 class="mb-1 mt-1 text-base font-semibold text-foreground">这轮回答的关键结果</h3>
        <div data-answer-signal-map role="group" aria-label="本轮诊断汇流图" class="mt-4 border-y border-border bg-secondary/25 px-3 py-5 sm:px-5 lg:px-7 lg:py-7">
          <div class="signal-map-body relative">
            <svg class="pointer-events-none absolute inset-0 z-0 hidden size-full lg:block" viewBox="0 0 1000 460" preserveAspectRatio="none" aria-hidden="true">
              <defs>
                <marker id="signal-flow-arrow" markerHeight="10" markerWidth="10" orient="auto" refX="9" refY="5">
                  <path d="M0,0 L10,5 L0,10 Z" fill="var(--border-strong)" />
                </marker>
                <marker id="signal-flow-arrow-active" markerHeight="13" markerWidth="13" orient="auto" refX="12" refY="6.5">
                  <path d="M0,0 L13,6.5 L0,13 Z" fill="var(--foreground)" />
                </marker>
              </defs>
              <path v-for="link in orderedSignalFlowLinks" :key="link.key" :data-signal-flow-link="link.key"
                pathLength="1" class="signal-flow-path" :class="{
                  'signal-flow-path--answer': link.key === 'answer',
                  'signal-flow-path--active': signalFlowLinkActive(link)
                }" :marker-end="signalMarkerEnd(...link.signals)" :d="link.path" />
            </svg>

            <div class="relative z-[1] grid gap-5 lg:min-h-[28rem] lg:grid-cols-[minmax(0,31%)_minmax(0,20%)_minmax(0,37%)] lg:items-center lg:gap-x-[6%] lg:gap-y-0">
              <div class="grid gap-4 lg:min-h-[28rem] lg:content-between">
                <article v-for="signal in answerSignalMap.inputs" :key="signal.key" data-signal-input data-signal-node-panel
                  class="signal-node signal-node-panel group relative grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-start gap-3 glass-card glass-edge rounded-glass border p-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center"
                  @mouseenter="highlightedSignal = signal.key" @mouseleave="highlightedSignal = ''"
                  @focusin="highlightedSignal = signal.key" @focusout="highlightedSignal = ''">
                  <span class="signal-port relative z-[1] flex size-10 shrink-0 items-center justify-center rounded-full border border-border-strong bg-background text-muted-foreground transition-colors group-hover:border-foreground group-hover:text-foreground group-focus-within:border-foreground group-focus-within:text-foreground lg:col-start-2" aria-hidden="true">
                    <component :is="signalIcons[signal.key]" class="size-5" />
                  </span>
                  <div class="min-w-0 lg:col-start-1 lg:row-start-1 lg:text-right">
                    <h4 class="m-0 text-caption font-semibold text-muted-foreground">{{ signal.label }}</h4>
                    <p class="m-0 mt-1 break-words text-body-sm font-semibold leading-relaxed text-foreground [overflow-wrap:anywhere]">{{ signal.value }}</p>
                    <p class="m-0 mt-0.5 text-caption leading-relaxed text-muted-foreground">{{ signal.summary }}</p>
                    <p v-if="signal.details?.length" class="m-0 mt-1 break-words text-caption leading-relaxed text-foreground">{{ signal.details.join(' · ') }}</p>
                    <Button v-if="canOpenSignalStage(signal.stageKey)" variant="outline" size="sm" class="mt-2 rounded-md max-[640px]:h-11" type="button"
                      :aria-label="`查看${signal.label}过程`" @click="openSignalStage(signal.stageKey)">
                      查看过程
                      <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                    </Button>
                  </div>
                </article>
              </div>

              <article v-if="answerSignalMap.evidence" data-signal-evidence data-signal-node-panel
                class="signal-node signal-node-panel group relative grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-start gap-3 glass-card glass-edge rounded-glass border p-4 lg:flex lg:flex-col lg:items-center lg:text-center"
                @mouseenter="highlightedSignal = 'evidence'" @mouseleave="highlightedSignal = ''"
                @focusin="highlightedSignal = 'evidence'" @focusout="highlightedSignal = ''">
                <span class="signal-evidence-node relative z-[1] flex size-12 shrink-0 flex-col items-center justify-center rounded-full border-2 transition-colors lg:size-16"
                  :class="signalEvidenceClass(answerSignalMap.evidence.state)" aria-hidden="true">
                  <strong class="text-base tabular-nums lg:text-lg">{{ answerSignalMap.evidence.count }}</strong>
                </span>
                <div class="min-w-0">
                  <h4 class="m-0 text-caption font-semibold text-muted-foreground">{{ answerSignalMap.evidence.label }}</h4>
                  <p class="m-0 mt-1 text-body-sm font-semibold text-foreground">{{ answerSignalMap.evidence.value }}</p>
                  <p class="m-0 mt-0.5 text-caption leading-relaxed text-muted-foreground">{{ answerSignalMap.evidence.summary }}</p>
                  <Button v-if="canOpenSignalStage(answerSignalMap.evidence.stageKey)" variant="outline" size="sm" class="mt-2 rounded-md max-[640px]:h-11" type="button"
                    :aria-label="`查看${answerSignalMap.evidence.label}过程`" @click="openSignalStage(answerSignalMap.evidence.stageKey)">
                    查看过程
                    <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                  </Button>
                </div>
              </article>

              <div class="grid min-w-0 content-center">
                <article v-if="answerSignalMap.answer" data-signal-answer data-signal-node-panel
                  class="signal-node signal-node-panel group relative grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-start gap-3 glass-card glass-edge rounded-glass border p-4"
                  @mouseenter="highlightedSignal = 'answer'" @mouseleave="highlightedSignal = ''"
                  @focusin="highlightedSignal = 'answer'" @focusout="highlightedSignal = ''">
                  <span class="signal-port relative z-[1] flex size-10 shrink-0 items-center justify-center rounded-full border-2 transition-colors"
                    :class="signalAnswerClass(answerSignalMap.answer.statusCode)" aria-hidden="true">
                    <ChatBubbleBottomCenterTextIcon class="size-5" />
                  </span>
                  <div class="min-w-0">
                    <h4 class="m-0 text-caption font-semibold text-muted-foreground">{{ answerSignalMap.answer.label }}</h4>
                    <div class="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
                      <strong class="text-title-sm font-semibold" :class="signalStatusTextClass(answerSignalMap.answer.statusCode)">{{ answerSignalMap.answer.status }}</strong>
                      <span class="text-caption tabular-nums text-muted-foreground">首包 {{ answerSignalMap.answer.latency }}</span>
                    </div>
                    <blockquote class="m-0 mt-3 max-w-prose border-l border-border-strong pl-3 text-compact leading-relaxed text-foreground">
                      {{ answerSignalMap.answer.preview }}
                    </blockquote>
                    <Button v-if="canOpenSignalStage(answerSignalMap.answer.stageKey)" variant="outline" size="sm" class="mt-3 rounded-md max-[640px]:h-11" type="button"
                      :aria-label="`查看${answerSignalMap.answer.label}过程`" @click="openSignalStage(answerSignalMap.answer.stageKey)">
                      查看过程
                      <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                    </Button>
                  </div>
                </article>

                <div data-signal-resource-link class="signal-resource-link hidden h-9 items-center justify-center lg:flex"
                  :class="{ 'signal-resource-link--active': signalPathActive('answer', 'resources') }" aria-hidden="true">
                  <span class="signal-resource-link-line"></span>
                  <ArrowDownIcon class="signal-resource-link-arrow" :class="signalPathActive('answer', 'resources') ? 'size-6' : 'size-5'" />
                </div>

                <article data-signal-resource-rail data-signal-node-panel
                  class="signal-node signal-node-panel group relative grid min-w-0 gap-4 glass-card glass-edge rounded-glass border p-4"
                  @mouseenter="highlightedSignal = 'resources'" @mouseleave="highlightedSignal = ''"
                  @focusin="highlightedSignal = 'resources'" @focusout="highlightedSignal = ''">
                  <div class="flex items-center justify-between gap-3">
                    <div class="flex items-center gap-2">
                      <span class="flex size-9 shrink-0 items-center justify-center rounded-full border border-border-strong bg-background text-muted-foreground transition-colors group-hover:border-foreground group-hover:text-foreground group-focus-within:border-foreground group-focus-within:text-foreground" aria-hidden="true">
                        <CpuChipIcon class="size-5" />
                      </span>
                      <span class="text-caption font-semibold text-foreground">模型资源</span>
                    </div>
                    <Button v-if="canOpenSignalStage('usage')" variant="outline" size="sm" class="rounded-md max-[640px]:h-11" type="button"
                      aria-label="查看模型资源过程" @click="openSignalStage('usage')">
                      查看过程
                      <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                    </Button>
                  </div>
                  <dl class="grid grid-cols-2 gap-x-4 gap-y-3">
                    <div v-for="resource in answerSignalMap.resources" :key="resource.label" class="min-w-0 border-l border-border pl-3">
                      <dt class="text-caption text-muted-foreground">{{ resource.label }}</dt>
                      <dd class="m-0 mt-1 break-words text-body-sm font-semibold tabular-nums" :class="signalResourceClass(resource.tone)">{{ resource.value }}</dd>
                    </div>
                  </dl>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-show="activeDetailTab === 'overview'">
        <h3 class="mb-1 mt-1 text-base font-semibold text-foreground">执行阶段时间线</h3>
        <p class="m-0 text-compact leading-relaxed text-[var(--muted-foreground)]">按照执行顺序浏览各阶段，需要时通过操作按钮查看详细过程。</p>
        <div v-if="!stageTraces.length" class="mt-3 rounded-md border border-dashed border-border px-6 py-6 text-center text-sm text-muted-foreground">当前轮次还没有可展示的阶段轨迹。</div>
        <div v-else class="mt-4 flex flex-col gap-2">
          <article v-for="(trace, index) in stageTraces" :key="trace.stageId" data-stage-trace-row class="flex gap-3 sm:gap-4">
            <div class="flex w-5 shrink-0 flex-col items-center pt-4" aria-hidden="true">
              <span class="z-[1] h-2.5 w-2.5 shrink-0 rounded-full" :class="dotClass(trace.stageState)"></span>
              <span v-if="index < stageTraces.length - 1" class="w-0.5 flex-1 bg-border"></span>
            </div>
            <div data-stage-trace-panel class="min-w-0 flex-1 rounded-md border border-border bg-secondary/40 px-3 py-3 transition-colors hover:bg-foreground/[0.08] sm:px-4">
              <div class="flex items-center justify-between gap-3 max-[760px]:flex-col max-[760px]:items-start">
                <div class="flex flex-wrap items-center gap-1.5">
                  <strong class="text-compact text-foreground">{{ trace.stageName }}</strong>
                  <span v-if="trace.stageCode" class="inline-flex rounded bg-foreground/[0.06] px-2 py-0.5 font-mono text-technical font-semibold text-[var(--muted-foreground)]">{{ trace.stageCode }}</span>
                  <span class="inline-flex rounded px-2 py-0.5 text-micro font-semibold" :class="statusBadgeClass(trace.stageState)">{{ formatStatusLabel(trace.stageState) }}</span>
                </div>
                <time class="whitespace-nowrap text-xs tabular-nums text-muted-foreground">{{ formatDateTime(trace.startTime) }}</time>
              </div>
              <p class="mb-0 mt-2 text-compact leading-relaxed text-[var(--muted-foreground)]">{{ trace.summaryText || '当前阶段已记录。' }}</p>
              <div class="mt-3 grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
                <div class="min-w-0">
                  <div class="h-1.5 w-full overflow-hidden rounded-full bg-foreground/[0.08]">
                    <div class="h-full rounded-full bg-foreground/[0.55] transition-all" :style="{ width: traceBarWidth(trace) }"></div>
                  </div>
                  <div class="mt-1.5 text-xs tabular-nums text-muted-foreground">耗时 {{ trace.durationMs ? `${trace.durationMs} ms` : '无' }}</div>
                </div>
                <Button variant="outline" size="sm" class="rounded-md max-[640px]:h-11 max-[640px]:w-full" type="button"
                  :aria-label="`查看${trace.stageName || trace.stageCode || '当前'}阶段详情`" @click="openTraceDetail(trace)">
                  查看阶段详情
                  <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                </Button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section v-show="activeDetailTab === 'evidence'" v-if="tableEvidenceItems.length > 0">
        <div class="detail-section-head flex items-start justify-between gap-3 rounded-lg px-3 py-3.5 max-[768px]:flex-col sm:px-4">
          <div class="flex min-w-0 items-start gap-3">
            <span class="detail-section-icon mt-0.5 grid size-6 shrink-0 place-items-center rounded-md" aria-hidden="true">
              <TableCellsIcon class="size-4" />
            </span>
            <div class="min-w-0">
              <h3 class="m-0 text-body font-semibold text-foreground">最终表格证据定位</h3>
              <p class="m-0 mt-0.5 text-compact leading-relaxed text-muted-foreground">表格问答最终引用到的表、行、列、单元格和 bbox 都在这里，方便核对结构化查询有没有真正落到原表位置。</p>
            </div>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <span class="inline-flex rounded bg-primary/[0.08] px-2.5 py-1 text-xs font-semibold text-primary">{{ tableEvidenceItems.length }} 条表格证据</span>
            <span v-if="tableEvidenceBboxCount" class="inline-flex rounded bg-secondary px-2.5 py-1 text-xs font-semibold text-muted-foreground">{{ tableEvidenceBboxCount }} 个 cell bbox</span>
          </div>
        </div>
        <div class="detail-rail">
          <div class="detail-rail-item grid gap-3" style="grid-template-columns:repeat(auto-fit,minmax(280px,1fr))">
            <article v-for="item in tableEvidenceItems" :key="item.key" class="glass-card glass-edge rounded-glass border p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-1.5">
                    <span class="inline-flex rounded bg-foreground/[0.06] px-2 py-0.5 text-micro font-semibold text-[var(--muted-foreground)]">引用 {{ item.referenceId }}</span>
                    <span class="inline-flex rounded bg-primary/[0.08] px-2 py-0.5 text-micro font-semibold text-primary">{{ item.channel }}</span>
                  </div>
                  <h4 class="m-0 mt-2 text-sm font-semibold text-foreground">{{ item.tableTitle }}</h4>
                  <p class="m-0 mt-0.5 truncate text-xs text-muted-foreground">{{ item.documentName }}<span v-if="item.sectionPath"> / {{ item.sectionPath }}</span></p>
                </div>
              </div>
              <dl class="mt-3 grid grid-cols-2 gap-3 border-t border-border pt-3 text-compact max-[540px]:grid-cols-1">
                <div v-for="pair in tableEvidencePairs(item)" :key="`${item.key}-${pair.label}`" class="grid gap-1">
                  <dt class="text-xs text-muted-foreground">{{ pair.label }}</dt>
                  <dd class="m-0 break-words text-foreground">{{ pair.value }}</dd>
                </div>
              </dl>
              <p v-if="item.snippet" class="mt-3 rounded-md bg-secondary px-3 py-2 text-xs leading-relaxed text-[var(--muted-foreground)]">{{ truncate(item.snippet, 160) }}</p>
              <details v-if="item.tableBboxJson || item.cellBboxJsons.length" class="mt-3 border-t border-border pt-2">
                <summary class="cursor-pointer text-compact font-semibold text-foreground">查看 bbox 原文</summary>
                <pre v-if="item.tableBboxJson" class="mt-2 overflow-auto whitespace-pre-wrap rounded-md bg-code p-3 text-xs text-code-foreground">{{ item.tableBboxJson }}</pre>
                <pre v-if="item.cellBboxJsons.length" class="mt-2 max-h-44 overflow-auto whitespace-pre-wrap rounded-md bg-code p-3 text-xs text-code-foreground">{{ item.cellBboxJsons.join('\n') }}</pre>
              </details>
              <RouterLink v-if="item.documentId" :to="tableEvidenceDocumentRoute(item)" target="_blank" rel="noopener"
                class="mt-3 inline-flex rounded-md border border-primary/20 bg-primary/[0.06] px-3 py-2 text-compact font-semibold text-primary transition-colors hover:bg-primary/[0.10]">
                在文档表格中高亮
              </RouterLink>
            </article>
          </div>
        </div>
      </section>

      <section v-show="activeDetailTab === 'retrieval'" v-if="fusionTraceView.hasData" data-retrieval-fusion-workbench>
        <div class="flex items-start justify-between gap-4 max-[768px]:flex-col">
          <div class="min-w-0">
            <h3 class="mb-1 mt-1 text-base font-semibold text-foreground">检索汇流工作台</h3>
            <p class="m-0 max-w-prose text-compact leading-relaxed text-muted-foreground">沿真实通道、融合分、重排分和最终结果追踪候选去向。</p>
          </div>
          <p class="m-0 text-right text-caption tabular-nums text-muted-foreground max-[768px]:text-left">
            {{ fusionTraceView.summary.subQuestionCount }} 个子问题 · {{ fusionTraceView.summary.candidateCount }} 个候选 · {{ fusionTraceView.summary.filteredCount }} 个被过滤
          </p>
        </div>

        <div data-retrieval-flow-summary class="mt-4 rounded-lg border border-border bg-secondary/40 px-3 py-4 sm:px-4">
          <h4 class="m-0 mb-3 text-caption font-semibold uppercase tracking-wide text-muted-foreground">整轮汇流总览</h4>
          <ol class="m-0 grid list-none grid-cols-2 gap-3 p-0 sm:grid-cols-5" aria-label="整轮检索汇流总览">
            <li v-for="(stage, index) in retrievalFlowSummary.stages" :key="stage.key" class="relative flex min-w-0 items-center gap-3">
              <div class="min-w-0">
                <span class="block text-caption text-muted-foreground">{{ stage.label }}</span>
                <strong class="mt-0.5 block text-title-sm font-semibold tabular-nums" :class="stage.tone === 'success' ? 'text-primary' : 'text-foreground'">{{ stage.value }}</strong>
              </div>
              <ArrowRightIcon v-if="index < retrievalFlowSummary.stages.length - 1" class="ml-auto hidden size-5 shrink-0 text-border-strong sm:block" aria-hidden="true" />
            </li>
          </ol>
        </div>

        <Accordion type="multiple" v-model="openFusionGroupKeys" class="mt-5 grid gap-4">
          <AccordionItem v-for="group in fusionTraceView.groups" :key="`fusion-group-${group.index}`"
            :value="String(group.index)"
            class="fusion-subquestion border-0 bg-transparent">
            <AccordionTrigger class="fusion-subquestion-trigger rounded-lg px-3 py-3.5 hover:no-underline sm:px-4">
              <span class="flex min-w-0 flex-1 flex-wrap items-start gap-x-4 gap-y-2.5 pr-2 max-[768px]:flex-col">
                <span class="flex min-w-0 flex-1 items-start gap-3">
                  <span class="fusion-subquestion-index mt-0.5 grid size-6 shrink-0 place-items-center rounded-md text-caption font-semibold tabular-nums" aria-hidden="true">{{ group.index }}</span>
                  <span class="min-w-0">
                    <span class="block text-caption font-semibold text-muted-foreground">子问题 {{ group.index }}</span>
                    <span class="mt-0.5 block break-words text-body font-semibold leading-relaxed text-foreground">{{ group.question }}</span>
                  </span>
                </span>
                <dl class="m-0 flex flex-wrap items-center justify-end gap-x-5 gap-y-1.5 text-caption tabular-nums max-[768px]:justify-start">
                  <div><dt class="inline text-muted-foreground">融合候选 </dt><dd class="inline font-semibold text-foreground">{{ group.fusedCandidateCount ?? group.resultRows?.length ?? '-' }}</dd></div>
                  <div><dt class="inline text-muted-foreground">父块提升 </dt><dd class="inline font-semibold text-foreground">{{ group.parentCandidateCount ?? '-' }}</dd></div>
                  <div><dt class="inline text-muted-foreground">重排候选 </dt><dd class="inline font-semibold text-foreground">{{ group.rerankedCandidateCount ?? group.rerankedCount ?? '-' }}</dd></div>
                  <div><dt class="inline text-muted-foreground">最终证据 </dt><dd class="inline font-semibold text-primary">{{ group.referenceCount ?? group.selectedCount }}</dd></div>
                </dl>
              </span>
            </AccordionTrigger>
            <AccordionContent class="p-0 pb-0">
              <div class="detail-rail">
                <section class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4"
                  :aria-labelledby="`channel-lanes-title-${group.index}`">
                  <div class="flex items-end justify-between gap-3">
                    <div>
                      <h4 :id="`channel-lanes-title-${group.index}`" class="m-0 text-body-sm font-semibold text-foreground">通道执行轨道</h4>
                      <p class="m-0 mt-1 text-caption text-muted-foreground">每一列共享同一召回上限，长度直接对应真实数量。</p>
                    </div>
                    <span class="text-caption tabular-nums text-muted-foreground">{{ fusionFlowsMap[group.index]?.channels.length ?? 0 }} 次通道执行</span>
                  </div>
                  <div v-if="fusionFlowsMap[group.index]?.channels.length" class="mt-3 border-t border-border">
                    <article v-for="channel in fusionFlowsMap[group.index].channels" :key="`${group.index}-${channel.key}`" data-retrieval-channel-lane
                      :data-channel-type="channel.channelType"
                      class="fusion-channel-lane grid min-w-0 gap-4 border-b border-border px-3 py-3 last:border-b-0 xl:grid-cols-[minmax(150px,0.8fr)_minmax(360px,2fr)_minmax(220px,1fr)] xl:items-center">
                      <div class="min-w-0">
                        <strong class="block text-body-sm font-semibold text-foreground">{{ channel.channelLabel }}</strong>
                        <span class="mt-1 inline-flex items-center gap-1.5 text-caption" :class="channelStateTextClass(channel)">
                          <span class="size-1.5 rounded-full" :class="channelStateDotClass(channel)" aria-hidden="true"></span>
                          {{ channel.errorMessage ? '执行异常' : formatExecutionState(channel.executionState) }}
                        </span>
                        <p v-if="channel.retrievalIntent" class="m-0 mt-1 break-words text-micro text-muted-foreground">{{ channel.retrievalIntent }}</p>
                      </div>
                      <div class="grid min-w-0 grid-cols-3 gap-3">
                        <div v-for="item in channelThroughputItems(channel)" :key="`${channel.key}-${item.key}`" class="min-w-0">
                          <div class="flex items-baseline justify-between gap-2">
                            <span class="text-micro text-muted-foreground">{{ item.label }}</span>
                            <strong class="text-caption font-semibold tabular-nums" :class="item.key === 'selected' ? 'text-primary' : 'text-foreground'">{{ item.value }}</strong>
                          </div>
                          <span class="mt-1.5 block h-1.5 overflow-hidden rounded-full bg-foreground/[0.07]">
                            <span class="fusion-channel-fill block h-full origin-left rounded-full" :class="channelThroughputFillClass(item.key)" :style="{ '--lane-scale': item.ratio }"></span>
                          </span>
                        </div>
                      </div>
                      <dl class="m-0 grid grid-cols-3 gap-3 text-caption tabular-nums">
                        <div><dt class="text-muted-foreground">权重</dt><dd class="m-0 mt-0.5 font-semibold text-foreground">{{ channel.channelWeightText }}</dd></div>
                        <div><dt class="text-muted-foreground">耗时</dt><dd class="m-0 mt-0.5 font-semibold text-foreground">{{ channel.durationMs ? `${channel.durationMs} ms` : '-' }}</dd></div>
                        <div><dt class="text-muted-foreground">平均分</dt><dd class="m-0 mt-0.5 font-semibold text-foreground">{{ formatScore(channel.avgScore) }}</dd></div>
                      </dl>
                      <p v-if="channel.errorMessage" class="m-0 rounded-md bg-destructive/[0.06] px-2.5 py-2 text-caption text-destructive xl:col-span-3">{{ channel.errorMessage }}</p>
                    </article>
                  </div>
                  <div v-else class="mt-3 border-t border-border bg-secondary/25 px-4 py-6 text-center text-compact text-muted-foreground">当前子问题没有通道执行记录。</div>
                </section>

                <section class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4"
                  :aria-labelledby="`candidate-flow-title-${group.index}`">
                  <div class="flex items-end justify-between gap-3 max-[640px]:items-start">
                    <h4 :id="`candidate-flow-title-${group.index}`" class="m-0 text-body-sm font-semibold text-foreground">候选决策轨道</h4>
                    <span class="shrink-0 text-caption tabular-nums text-muted-foreground">
                      展示 {{ fusionFlowsMap[group.index]?.candidates.length ?? 0 }} / {{ fusionFlowsMap[group.index]?.totalCount ?? 0 }}
                      <template v-if="fusionFlowsMap[group.index]?.hiddenCount">，另有 {{ fusionFlowsMap[group.index].hiddenCount }} 条</template>
                    </span>
                  </div>

                  <div v-if="fusionFlowsMap[group.index]?.candidates.length" data-fusion-candidate-map class="mt-3 grid gap-5">
                    <section v-for="outGroup in fusionFlowsMap[group.index].groups" :key="outGroup.key" data-fusion-outcome-group
                      :aria-label="`${outGroup.label}，${outGroup.candidates.length} 条`">
                      <div class="grid gap-2">
                        <article v-for="candidate in outGroup.candidates" :key="candidate.key" data-fusion-candidate-row
                          class="fusion-candidate-track-row" :data-candidate-key="candidate.key"
                          :data-channel-type="candidate.row.channelType" :data-content-kind="candidate.contentKind"
                          :data-tone="candidate.tone" :aria-label="candidate.ariaLabel">
                          <div class="fusion-candidate-context">
                            <span class="fusion-candidate-layer-card fusion-candidate-layer-card--kind">{{ candidate.contentKind }}</span>
                            <span class="fusion-candidate-layer-card fusion-candidate-layer-card--content">{{ candidate.contentLabel }}</span>
                            <span class="fusion-candidate-layer-card fusion-candidate-layer-card--source">{{ candidate.contentMeta }}</span>
                          </div>

                          <ol data-fusion-candidate-track class="fusion-candidate-track" :aria-label="`${candidate.contentKind}的候选决策阶段`">
                            <li v-for="(stage, stageIdx) in candidate.stages" :key="stage.key" data-fusion-stage
                              :data-stage-key="stage.key" class="fusion-candidate-stage">
                              <span class="fusion-candidate-stage-node">
                                <span class="fusion-candidate-stage-label">{{ stage.label }}</span>
                                <strong class="fusion-candidate-stage-value">{{ stage.value }}</strong>
                              </span>
                              <span v-if="stageIdx < candidate.stages.length - 1" data-fusion-stage-arrow
                                class="fusion-candidate-stage-arrow" aria-hidden="true">
                                <ArrowRightIcon />
                              </span>
                            </li>
                          </ol>

                          <Button variant="outline" size="sm" class="fusion-candidate-detail-button rounded-md max-[640px]:h-11"
                            type="button" :aria-label="`查看候选详情：${candidate.contentText}`" @click="openCandidateDetail(candidate.row)">
                            查看详情
                            <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                          </Button>
                        </article>
                      </div>
                    </section>
                  </div>

                  <div v-else class="mt-3 border-t border-border bg-secondary/25 px-4 py-8 text-center">
                    <strong class="text-body-sm text-foreground">没有候选结果</strong>
                    <p class="m-0 mt-1 text-caption text-muted-foreground">通道执行数据仍保留在上方，当前子问题没有候选结果。</p>
                  </div>
                </section>
              </div>
            </AccordionContent>
          </AccordionItem>
        </Accordion>

      </section>

      <section v-show="activeDetailTab === 'evidence'" v-if="citationBindingView.hasData">
        <div class="detail-section-head flex items-start justify-between gap-3 rounded-lg px-3 py-3.5 max-[768px]:flex-col sm:px-4">
          <div class="flex min-w-0 items-start gap-3">
            <span class="detail-section-icon mt-0.5 grid size-6 shrink-0 place-items-center rounded-md" aria-hidden="true">
              <LinkIcon class="size-4" />
            </span>
            <div class="min-w-0">
              <h3 class="m-0 text-body font-semibold text-foreground">显式引用绑定</h3>
              <p class="m-0 mt-0.5 text-compact leading-relaxed text-muted-foreground">只展示答案中的合法 [n] token、同轮 Prompt manifest 绑定和最终 source snapshot，不做相似度补齐。</p>
            </div>
          </div>
          <div class="flex flex-wrap justify-end gap-1.5">
            <span class="inline-flex rounded-md bg-secondary px-2.5 py-1 text-xs font-semibold text-muted-foreground">Prompt source {{ citationBindingView.summary.renderedSourceCount }}</span>
            <span class="inline-flex rounded-md bg-secondary px-2.5 py-1 text-xs font-semibold text-muted-foreground">合法绑定 {{ citationBindingView.summary.bindingCount }}</span>
            <span class="inline-flex rounded-md bg-primary/[0.08] px-2.5 py-1 text-xs font-semibold text-primary">最终 {{ citationBindingView.summary.finalReferenceCount }}</span>
            <span v-if="citationBindingView.summary.rejectedTokenCount" class="inline-flex rounded-md bg-[var(--status-waiting-bg)] px-2.5 py-1 text-xs font-semibold text-[var(--status-waiting-fg)]">拒绝 {{ citationBindingView.summary.rejectedTokenCount }}</span>
          </div>
        </div>
        <div class="detail-rail">
          <section class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4">
            <h4 class="m-0 text-body-sm font-semibold text-foreground">绑定概览</h4>
            <div class="mt-3 grid gap-3 border-t border-border pt-3" style="grid-template-columns:repeat(auto-fit,minmax(180px,1fr))">
              <div v-for="item in citationSummaryItems" :key="item.label" class="min-w-0">
                <span class="text-xs text-muted-foreground">{{ item.label }}</span>
                <strong class="mt-1 block break-all text-sm leading-5 text-foreground">{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4">
            <div class="flex items-baseline justify-between gap-3">
              <h4 class="m-0 text-body-sm font-semibold text-foreground">最终引用列表</h4>
              <span v-if="citationBindingView.finalReferences.length" class="shrink-0 text-caption tabular-nums text-muted-foreground">{{ citationBindingView.finalReferences.length }} 条</span>
            </div>
            <div v-if="citationBindingView.finalReferences.length" class="mt-3 divide-y divide-border border-t border-border">
              <article v-for="item in citationBindingView.finalReferences" :key="item.key" class="py-3.5 first:pt-3">
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <strong class="text-compact text-foreground">引用 [{{ item.referenceId }}]</strong>
                      <span class="rounded-md bg-secondary px-2 py-0.5 text-micro font-semibold text-muted-foreground">{{ item.channelLabel }}</span>
                    </div>
                    <p class="mt-1 break-all font-mono text-micro text-muted-foreground">{{ item.identity || '未记录稳定 identity' }}</p>
                    <p class="mt-2 text-compact font-semibold text-foreground">{{ item.documentName }}</p>
                    <p v-if="item.sectionPath" class="mt-0.5 text-xs text-muted-foreground">{{ item.sectionPath }}</p>
                  </div>
                  <RouterLink v-if="item.documentId" :to="citationDocumentRoute(item)" target="_blank" rel="noopener" class="inline-flex rounded-md border border-primary/20 bg-primary/[0.06] px-3 py-1.5 text-caption font-semibold text-primary hover:bg-primary/[0.10]">查看文档证据</RouterLink>
                </div>
                <p v-if="item.quoteText" class="mt-3 rounded-md bg-secondary px-3 py-2 text-xs leading-relaxed text-foreground">{{ truncate(item.quoteText, 220) }}</p>
              </article>
            </div>
            <p v-else class="m-0 mt-3 border-t border-border pt-3 text-compact text-muted-foreground">答案中没有合法显式引用 token，最终引用列表为空。</p>
          </section>

          <section v-if="citationBindingView.rejectedTokens.length"
            class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4">
            <div class="flex items-baseline justify-between gap-3">
              <h4 class="m-0 text-body-sm font-semibold text-foreground">被拒绝的引用 token</h4>
              <span class="shrink-0 text-caption tabular-nums text-muted-foreground">{{ citationBindingView.rejectedTokens.length }} 条</span>
            </div>
            <div class="mt-3 overflow-x-auto border-t border-border">
              <table class="w-full min-w-[560px] border-collapse text-sm">
                <caption class="sr-only">显式引用绑定结果</caption>
                <thead><tr><th v-for="h in ['Token','Reference','出现顺序','拒绝原因']" :key="h" scope="col" class="border-b border-border px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground">{{ h }}</th></tr></thead>
                <tbody>
                  <tr v-for="(item, index) in citationBindingView.rejectedTokens" :key="`rejected-${index}-${item.token}`" class="border-b border-border last:border-0">
                    <td class="px-3 py-2.5 font-mono text-xs text-foreground">{{ item.token || '-' }}</td>
                    <td class="px-3 py-2.5 font-mono text-xs text-foreground">{{ item.referenceId || '-' }}</td>
                    <td class="px-3 py-2.5 text-compact text-muted-foreground">{{ item.occurrence || '-' }}</td>
                    <td class="px-3 py-2.5 text-compact text-muted-foreground">{{ item.reason || '未记录' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </section>

      <section v-show="activeDetailTab === 'evidence'" v-if="evidenceBudgetSnapshot">
        <div class="detail-section-head flex items-start gap-3 rounded-lg px-3 py-3.5 sm:px-4">
          <span class="detail-section-icon mt-0.5 grid size-6 shrink-0 place-items-center rounded-md" aria-hidden="true">
            <ScaleIcon class="size-4" />
          </span>
          <div class="min-w-0">
            <h3 class="m-0 text-body font-semibold text-foreground">证据预算分析</h3>
            <p class="m-0 mt-0.5 text-compact leading-relaxed text-muted-foreground">查看证据选择过程和预算使用情况。</p>
          </div>
        </div>
        <div class="detail-rail">
          <section class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4">
            <h4 class="m-0 text-body-sm font-semibold text-foreground">预算用量</h4>
            <dl class="m-0 mt-3 flex flex-wrap gap-x-6 gap-y-3 border-t border-border pt-3">
              <div v-for="b in budgetItems" :key="b.label" class="grid gap-1">
                <dt class="text-xs text-muted-foreground">{{ b.label }}</dt>
                <dd class="m-0 text-sm font-semibold tabular-nums" :class="b.highlight ? 'text-primary' : 'text-foreground'">{{ b.value }}</dd>
              </div>
            </dl>
          </section>

          <section v-if="evidenceBudgetSnapshot.renderedReferenceDetails?.length"
            class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4">
            <div class="flex items-baseline justify-between gap-3">
              <h4 class="m-0 text-body-sm font-semibold text-foreground">已纳入 Prompt 的证据</h4>
              <span class="shrink-0 text-caption tabular-nums text-muted-foreground">{{ evidenceBudgetSnapshot.renderedReferenceDetails.length }} 条</span>
            </div>
            <ul class="m-0 mt-3 grid list-none gap-2 border-t border-border p-0 pt-3">
              <li v-for="(detail, idx) in evidenceBudgetSnapshot.renderedReferenceDetails" :key="`rendered-${idx}`"
                class="flex gap-2.5 text-compact leading-relaxed text-muted-foreground">
                <span class="mt-1.5 size-1.5 shrink-0 rounded-full bg-primary" aria-hidden="true"></span>
                <span class="min-w-0 break-words">{{ detail }}</span>
              </li>
            </ul>
          </section>

          <section v-if="evidenceBudgetSnapshot.omittedReferenceDetails?.length"
            class="detail-rail-item glass-card rounded-glass border px-3 py-3.5 sm:px-4">
            <div class="flex items-baseline justify-between gap-3">
              <h4 class="m-0 text-body-sm font-semibold text-foreground">因预算限制省略的证据</h4>
              <span class="shrink-0 text-caption tabular-nums text-muted-foreground">{{ evidenceBudgetSnapshot.omittedReferenceDetails.length }} 条</span>
            </div>
            <ul class="m-0 mt-3 grid list-none gap-2 border-t border-border p-0 pt-3">
              <li v-for="(detail, idx) in evidenceBudgetSnapshot.omittedReferenceDetails" :key="`omitted-${idx}`"
                class="flex gap-2.5 text-compact leading-relaxed text-muted-foreground">
                <span class="mt-1.5 size-1.5 shrink-0 rounded-full bg-[var(--status-waiting-fg)]" aria-hidden="true"></span>
                <span class="min-w-0 break-words">{{ detail }}</span>
              </li>
            </ul>
          </section>
        </div>
      </section>

      <section v-show="activeDetailTab === 'prompt'" v-if="hasPromptData">
        <h3 class="mb-1 mt-1 text-base font-semibold text-foreground">Prompt 预览</h3>
        <p class="m-0 text-compact text-[var(--muted-foreground)]">查看最终喂给模型的完整 Prompt。</p>
        <div class="mt-4 flex flex-wrap items-center gap-2">
          <Button v-for="t in ['system','user']" :key="t" type="button"
            variant="ghost" size="sm"
            :class="activePromptTab === t ? 'bg-primary/[0.08] text-primary' : 'bg-secondary text-foreground'"
            @click="activePromptTab = t">{{ t === 'system' ? 'System Prompt' : 'User Prompt' }}</Button>
          <Button variant="outline" size="sm" class="rounded-md" type="button" @click="promptWrapped = !promptWrapped">{{ promptWrapped ? '不换行' : '换行' }}</Button>
          <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="!activePromptText" @click="copyPrompt">{{ promptCopyState || '复制当前 Prompt' }}</Button>
        </div>
        <pre class="mt-3 max-h-[520px] overflow-auto rounded-lg bg-code p-4 text-xs text-code-foreground" :class="promptWrapped ? 'whitespace-pre-wrap break-words' : 'whitespace-pre'">{{ activePromptText || '无' }}</pre>
      </section>

      <section v-show="activeDetailTab === 'prompt'" v-if="stageTraces.length > 0 && stageBenchmarks.length > 0">
        <h3 class="mb-1 mt-1 text-base font-semibold text-foreground">阶段性能基准对比</h3>
        <p class="m-0 text-compact text-[var(--muted-foreground)]">对比当前执行与历史基准（P50/P90/P99），识别异常慢的阶段。</p>
        <div class="mt-4 grid gap-3" style="grid-template-columns:repeat(auto-fit,minmax(220px,1fr))">
          <article v-for="trace in stageTraces.filter(t => t.durationMs)" :key="trace.stageId" class="glass-card glass-edge rounded-glass border p-4">
            <div class="mb-3 flex items-center justify-between gap-2">
              <strong class="text-sm text-foreground">{{ trace.stageName }}</strong>
              <span v-if="findBenchmark(trace.stageCode, trace.executionMode)"
                class="inline-flex rounded-md px-2.5 py-1 text-micro font-bold"
                :class="benchmarkLevelClass(formatBenchmarkComparison(trace.durationMs, findBenchmark(trace.stageCode, trace.executionMode))?.level)">
                {{ formatBenchmarkComparison(trace.durationMs, findBenchmark(trace.stageCode, trace.executionMode))?.text || '-' }}
              </span>
            </div>
            <div class="grid grid-cols-2 gap-2 text-compact">
              <div class="grid gap-0.5"><span class="text-xs text-muted-foreground">本次</span><span class="font-semibold text-primary">{{ trace.durationMs }} ms</span></div>
              <template v-if="findBenchmark(trace.stageCode, trace.executionMode)">
                <div v-for="bm in benchmarkCols(trace)" :key="bm.label" class="grid gap-0.5">
                  <span class="text-xs text-muted-foreground">{{ bm.label }}</span>
                  <span class="text-foreground">{{ bm.value }}</span>
                </div>
              </template>
              <div v-else class="grid gap-0.5"><span class="text-xs text-muted-foreground">基准</span><span class="text-muted-foreground">暂无数据</span></div>
            </div>
          </article>
        </div>
      </section>

      <ChildPageDialog
        :open="traceDetailOpen && Boolean(overlayInspector)"
        :title="overlayInspector?.title || '阶段详情'"
        :description="overlayInspector?.summary || '这个阶段已经执行完成，下面是它记录下来的结构化细节。'"
        close-label="关闭阶段详情"
        @update:open="handleTraceDialogUpdate"
      >
          <div v-if="overlayInspector" class="grid gap-4">
            <div class="flex flex-wrap items-center gap-2">
              <span v-if="overlayInspector.stageCode" class="inline-flex rounded-md bg-secondary px-2 py-0.5 font-mono text-technical font-semibold text-muted-foreground">{{ overlayInspector.stageCode }}</span>
            </div>
            <div class="mb-4 flex flex-wrap gap-3 text-compact text-muted-foreground">
              <span>状态：{{ formatStatusLabel(overlayInspector.status) }}</span>
              <span v-if="overlayInspector.stageCode">阶段代码：{{ overlayInspector.stageCode }}</span>
              <span>开始：{{ formatDateTime(overlayInspector.startTime) }}</span>
              <span>结束：{{ formatDateTime(overlayInspector.endTime) }}</span>
              <span>耗时：{{ overlayInspector.durationMs ? `${overlayInspector.durationMs} ms` : '无' }}</span>
            </div>
            <div v-if="overlayInspector.summaryItems?.length" class="grid grid-cols-2 gap-3 max-[540px]:grid-cols-1">
              <div v-for="item in overlayInspector.summaryItems" :key="`trace-item-${item.label}`" class="grid gap-1">
                <span class="text-xs text-muted-foreground">{{ item.label }}</span>
                <pre v-if="item.code" class="overflow-auto rounded-md bg-code p-3 text-xs text-code-foreground whitespace-pre-wrap">{{ item.value }}</pre>
                <strong v-else class="text-sm text-foreground">{{ item.value }}</strong>
              </div>
            </div>
            <div v-if="overlayInspector.listSections?.length" class="mt-4 flex flex-col gap-4">
              <section v-for="item in overlayInspector.listSections" :key="`trace-list-${item.label}`">
                <span class="text-xs text-muted-foreground">{{ item.label }}</span>
                <ol v-if="item.ordered" class="mt-2 list-decimal pl-5 flex flex-col gap-1 text-compact text-foreground">
                  <li v-for="(entry, idx) in item.items" :key="`${item.label}-${idx}`">{{ entry }}</li>
                </ol>
                <ul v-else class="mt-2 list-disc pl-5 flex flex-col gap-1 text-compact text-foreground">
                  <li v-for="(entry, idx) in item.items" :key="`${item.label}-${idx}`">{{ entry }}</li>
                </ul>
              </section>
            </div>
            <div v-if="overlayInspector.tableSections?.length" class="mt-4 flex flex-col gap-4">
              <section v-for="table in overlayInspector.tableSections" :key="`trace-table-${table.label}`">
                <span class="text-xs text-muted-foreground">{{ table.label }}</span>
                <div class="mt-2 overflow-x-auto rounded-md border border-border">
                  <table class="w-full border-collapse text-sm">
                    <caption class="sr-only">{{ table.title || '检索证据表格' }}</caption>
                    <thead><tr class="bg-secondary"><th v-for="col in table.columns" :key="col" scope="col" class="border-b border-border px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground">{{ col }}</th></tr></thead>
                    <tbody>
                      <tr v-for="(row, ri) in table.rows" :key="`row-${table.label}-${ri}`" class="border-b border-border last:border-0">
                        <td v-for="(cell, ci) in row.cells" :key="`cell-${ri}-${ci}`" class="px-3 py-2.5 text-compact text-foreground">{{ cell }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>
            </div>
            <details v-if="overlayInspector.advancedItems?.length" class="mt-4 border-t border-border pt-3">
              <summary class="cursor-pointer text-compact font-semibold text-primary">查看这个阶段的原始快照</summary>
              <div class="mt-3 grid grid-cols-2 gap-3 max-[540px]:grid-cols-1">
                <div v-for="item in overlayInspector.advancedItems" :key="`trace-advanced-${item.label}`" class="grid gap-1" :class="item.code ? 'col-span-2 max-[540px]:col-span-1' : ''">
                  <span class="text-xs text-muted-foreground">{{ item.label }}</span>
                  <pre v-if="item.code" class="max-h-[420px] overflow-auto rounded-md bg-code p-3 text-xs leading-relaxed text-code-foreground whitespace-pre-wrap">{{ item.value }}</pre>
                  <strong v-else class="text-sm text-foreground">{{ item.value }}</strong>
                </div>
              </div>
            </details>
          </div>
      </ChildPageDialog>

      <ChildPageDialog
        :open="candidateDetailOpen && Boolean(candidateDetail)"
        :title="candidateDetail?.documentName || '候选详情'"
        :description="candidateDetail?.sectionPath || '查看候选证据的排序、分数和权威身份。'"
        close-label="关闭候选详情"
        @update:open="handleCandidateDialogUpdate"
      >
          <div v-if="candidateDetail" class="grid gap-4">
            <div class="flex flex-wrap items-center gap-2">
              <span class="inline-flex rounded-md px-2 py-0.5 text-xs font-semibold" :class="fusionStatusClass(candidateDetail.status.tone)">{{ candidateDetail.status.label }}</span>
              <span class="inline-flex rounded-md bg-secondary px-2 py-0.5 text-micro font-semibold text-muted-foreground">{{ candidateDetail.channelLabel }}</span>
            </div>
            <!-- CANDIDATE_BODY -->
            <div class="flex flex-wrap gap-x-6 gap-y-2 text-compact">
              <div class="flex gap-2"><span class="text-muted-foreground">通道排名</span><span class="text-foreground">#{{ candidateDetail.channelRank || '-' }}</span></div>
              <div v-if="candidateDetail.rrfRank" class="flex gap-2"><span class="text-muted-foreground">融合排名</span><span class="text-foreground">#{{ candidateDetail.rrfRank }}</span></div>
              <div class="flex gap-2"><span class="text-muted-foreground">最终排名</span><span class="font-semibold" :class="candidateDetail.isSelected ? 'text-primary' : 'text-foreground'">{{ candidateDetail.finalRank ? `#${candidateDetail.finalRank}` : '未进入' }}</span></div>
              <div class="flex gap-2"><span class="text-muted-foreground">文档块</span><span class="text-foreground">chunk {{ candidateDetail.chunkNo || candidateDetail.chunkId || '-' }} / 父块 {{ candidateDetail.parentBlockNo || candidateDetail.parentBlockId || '-' }}</span></div>
            </div>

            <h4 class="mb-2 mt-5 text-sm font-semibold text-foreground">分数拆解</h4>
            <div class="flex flex-col gap-2.5">
              <div v-for="score in candidateDetail.scoreItems" :key="score.key" class="grid grid-cols-[92px_56px_1fr] items-center gap-3">
                <span class="text-compact text-muted-foreground">{{ score.label }}</span>
                <span class="text-right font-mono text-compact text-foreground">{{ score.text }}</span>
                <span class="h-1.5 overflow-hidden rounded-full bg-foreground/[0.08]"><span class="block h-full rounded-full bg-primary/60" :style="{ width: score.width }"></span></span>
              </div>
            </div>

            <h4 class="mb-2 mt-5 text-sm font-semibold text-foreground">证据身份</h4>
            <div class="flex flex-wrap items-center gap-2">
              <span v-if="candidateDetail.contextOnly" class="inline-flex rounded bg-amber-500/[0.14] px-2.5 py-1 text-xs font-semibold text-amber-700">仅作上下文·不可引用</span>
              <span v-else-if="candidateDetail.citationIdentity" class="inline-flex rounded bg-[var(--status-success-bg)] px-2.5 py-1 text-xs font-semibold text-[var(--status-success-fg)]">可引用证据</span>
              <span v-else class="inline-flex rounded bg-foreground/[0.06] px-2.5 py-1 text-xs font-semibold text-muted-foreground">普通候选</span>
              <span v-if="candidateDetail.citationEvidenceType" class="text-xs text-muted-foreground">类型：{{ candidateDetail.citationEvidenceType }}</span>
            </div>

            <h4 class="mb-2 mt-5 text-sm font-semibold text-foreground">命中 / 筛除原因</h4>
            <p class="m-0 rounded-md bg-secondary px-3 py-2.5 text-compact leading-relaxed text-foreground">{{ candidateDetail.selectionReasonText }}</p>

            <template v-if="candidateDetail.preview">
              <h4 class="mb-2 mt-5 text-sm font-semibold text-foreground">片段预览</h4>
              <p class="m-0 rounded-md border border-border bg-secondary/50 px-3 py-2.5 text-compact leading-relaxed text-[var(--muted-foreground)]">{{ candidateDetail.preview }}</p>
            </template>
          </div>
      </ChildPageDialog>
    </template>
  </section>
</template>

<script setup>
import { computed, ref, watch, watchEffect } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  ArrowDownIcon, ArrowLeftIcon, ArrowRightIcon, ChatBubbleBottomCenterTextIcon,
  CpuChipIcon, DocumentTextIcon, LightBulbIcon, LinkIcon, MagnifyingGlassIcon,
  ScaleIcon, TableCellsIcon
} from '@heroicons/vue/24/outline'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { Button } from '@/components/ui/button'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import { chatApi } from '../../api/api'
import {
  buildExchangeSignalMap, buildExchangeStages, buildTraceStageInspector, buildUsageStageInspector,
  buildExplicitCitationView, buildFusionCandidateFlow, buildFusionTraceGroups, buildRetrievalFlowSummary, buildTableEvidenceItems,
  formatDateTime, formatStatusLabel, formatChannelType,
  formatExecutionState, formatScore, normalizeError, statusTone, truncate
  } from './observabilityHelpers'

marked.setOptions({ breaks: true, gfm: true })

function renderMarkdown(text) {
  if (!text) return ''
  const rendered = marked.parse(text)
  return DOMPurify.sanitize(rendered, { ADD_ATTR: ['target', 'rel', 'class'] })
}

const route = useRoute()
const loadingPage = ref(false)
const activeSession = ref(null)
const activeExchangeDetail = ref(null)
const pageError = ref('')
const traceDetailOpen = ref(false)
const overlayInspector = ref(null)
const candidateDetailOpen = ref(false)
const candidateDetail = ref(null)
const retrievalResults = ref([])
const channelExecutions = ref([])
const loadingRetrievalData = ref(false)
const retrievalError = ref('')
const stageBenchmarks = ref([])
const loadingBenchmarks = ref(false)
const benchmarkError = ref('')
const activePromptTab = ref('system')
const promptWrapped = ref(true)
const promptCopyState = ref('')
const activeDetailTab = ref('overview')
const highlightedSignal = ref('')
const openFusionGroupKeys = ref([])

const signalIcons = {
  scope: DocumentTextIcon,
  understanding: LightBulbIcon,
  retrieval: MagnifyingGlassIcon
}
const signalFlowLinks = [
  { key: 'scope', signals: ['scope', 'evidence'], path: 'M 310 72 C 338 72 340 230 362 230' },
  { key: 'understanding', signals: ['understanding', 'evidence'], path: 'M 310 230 H 362' },
  { key: 'retrieval', signals: ['retrieval', 'evidence'], path: 'M 310 388 C 338 388 340 230 362 230' },
  { key: 'answer', signals: ['evidence', 'answer'], path: 'M 570 230 C 598 230 598 132 625 132' }
]

const conversationId = computed(() => String(route.params.conversationId || ''))
const exchangeId = computed(() => String(route.params.exchangeId || ''))
const sessionReturnTarget = computed(() => ({
  name: 'AdminObservabilitySession',
  params: { conversationId: conversationId.value },
  query: observationReturnQuery(route.query)
}))
const activeExchange = computed(() => activeExchangeDetail.value?.exchange || null)
const stageTraces = computed(() => activeExchangeDetail.value?.stageTraces || [])
const exchangeStages = computed(() => buildExchangeStages(activeSession.value, activeExchange.value))
const answerSignalMap = computed(() => buildExchangeSignalMap(activeSession.value, activeExchange.value))
const orderedSignalFlowLinks = computed(() => [...signalFlowLinks].sort((left, right) => Number(signalFlowLinkActive(left)) - Number(signalFlowLinkActive(right))))
const modelUsageTraces = computed(() => Array.isArray(activeExchange.value?.debugTrace?.modelUsageTraces) ? activeExchange.value.debugTrace.modelUsageTraces : [])
const totalTokenText = computed(() => modelUsageTraces.value.length
  ? String(modelUsageTraces.value.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0))
  : '-')
const totalCostText = computed(() => {
  if (!modelUsageTraces.value.length) return '-'
  const total = modelUsageTraces.value.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)
  return `¥ ${total.toFixed(4)}`
})
const maxTraceDuration = computed(() => stageTraces.value.reduce((max, item) => Math.max(max, Number(item?.durationMs || 0)), 0))
const fusionTraceView = computed(() => buildFusionTraceGroups(retrievalResults.value, channelExecutions.value, stageTraces.value, activeExchange.value?.references || []))
const retrievalFlowSummary = computed(() => buildRetrievalFlowSummary(fusionTraceView.value))
const fusionFlowsMap = computed(() => {
  const map = {}
  for (const group of fusionTraceView.value.groups) map[group.index] = buildFusionCandidateFlow(group)
  return map
})
const evidenceBudgetSnapshot = computed(() => stageTraces.value.find((item) => item.stageCode === 'EVIDENCE_BUDGET')?.snapshot || null)
const tableEvidenceItems = computed(() => buildTableEvidenceItems(activeExchange.value?.references || []))
const tableEvidenceBboxCount = computed(() => tableEvidenceItems.value.reduce((sum, item) => sum + item.cellBboxJsons.length, 0))
const citationBindingView = computed(() => buildExplicitCitationView(stageTraces.value, activeExchange.value?.references || []))
const citationSummaryItems = computed(() => [
  { label: '权威', value: citationBindingView.value.authority || '-' },
  { label: '解析 token', value: citationBindingView.value.summary.parsedTokenCount },
  { label: '合法绑定', value: citationBindingView.value.summary.bindingCount },
  { label: '拒绝 token', value: citationBindingView.value.summary.rejectedTokenCount },
  { label: '最终引用', value: citationBindingView.value.summary.finalReferenceCount },
  { label: '守恒状态', value: citationBindingView.value.conservationStatus || '-' }
])
const ragSystemPrompt = computed(() => activeExchange.value?.debugTrace?.ragSystemPrompt || '')
const ragUserPrompt = computed(() => activeExchange.value?.debugTrace?.ragUserPrompt || '')
const hasPromptData = computed(() => Boolean(ragSystemPrompt.value || ragUserPrompt.value))
const activePromptText = computed(() => activePromptTab.value === 'system' ? ragSystemPrompt.value : ragUserPrompt.value)

// 轮次详情分组 Tab（概览 / 检索与融合 / 证据与引用 / Prompt 与性能）
const detailTabs = computed(() => [
  { value: 'overview', label: '概览' },
  { value: 'retrieval', label: '检索与融合' },
  { value: 'evidence', label: '证据与引用' },
  { value: 'prompt', label: 'Prompt 与性能' }
])
const retrievalTabHasData = computed(() => fusionTraceView.value.hasData)
const evidenceTabHasData = computed(() => tableEvidenceItems.value.length > 0 || citationBindingView.value.hasData || Boolean(evidenceBudgetSnapshot.value))
const promptTabHasData = computed(() => hasPromptData.value || (stageTraces.value.length > 0 && stageBenchmarks.value.length > 0))
const headerMetaPairs = computed(() => activeExchange.value ? [
  { dt: '文档范围', dd: activeSession.value?.selectedDocumentName || '未绑定文档' },
  { dt: '配置快照', dd: activeExchange.value.retrievalConfigSnapshotJson ? '已记录' : '未记录' },
  { dt: '执行时间', dd: formatDateTime(activeExchange.value.editTime || activeExchange.value.createTime) },
  { dt: '总耗时', dd: activeExchange.value.totalResponseTimeMs ? `${activeExchange.value.totalResponseTimeMs} ms` : '无' },
  { dt: '引用 / 推荐', dd: `${activeExchange.value.references?.length || 0} / ${activeExchange.value.recommendations?.length || 0}` },
  { dt: '总 Token / 成本', dd: `${totalTokenText.value} / ${totalCostText.value}` }
] : [])
const budgetItems = computed(() => evidenceBudgetSnapshot.value ? [
  { label: '总预算', value: observedMetric(evidenceBudgetSnapshot.value.totalBudget, '字符') },
  { label: '单子问题预算', value: observedMetric(evidenceBudgetSnapshot.value.perSubQuestionBudget, '字符') },
  { label: '已纳入', value: observedMetric(evidenceBudgetSnapshot.value.renderedReferenceCount, '条'), highlight: true },
  { label: '已省略', value: observedMetric(evidenceBudgetSnapshot.value.omittedReferenceCount, '条') }
] : [])

function observedMetric(value, unit) {
  return value == null || value === '' ? '-' : `${value} ${unit}`
}

function observationReturnQuery(query = {}) {
  return ['listKeyword', 'listMode', 'listStatus', 'listPage', 'listPageSize', 'turnPage'].reduce((result, key) => {
    if (query[key] != null && query[key] !== '') result[key] = String(query[key])
    return result
  }, {})
}

function statusBadgeClass(status) {
  const tone = statusTone(status)
  if (tone === 'completed') return 'bg-[var(--status-success-bg)] text-[var(--status-success-fg)]'
  if (tone === 'failed') return 'bg-[var(--status-danger-bg)] text-destructive'
  if (tone === 'stopped') return 'bg-[var(--status-waiting-bg)] text-[var(--status-waiting-fg)]'
  if (tone === 'running') return 'bg-[var(--status-running-bg)] text-running'
  return 'bg-foreground/[0.06] text-[var(--muted-foreground)]'
}
function dotClass(status) {
  const tone = statusTone(status)
  if (tone === 'completed') return 'bg-border-strong'
  if (tone === 'failed') return 'bg-[var(--status-danger-fg)]'
  if (tone === 'stopped') return 'bg-[var(--status-waiting-fg)]'
  if (tone === 'running') return 'bg-running'
  return 'bg-border-strong'
}
function benchmarkLevelClass(level) {
  if (level === 'excellent') return 'bg-green-500/[0.12] text-green-700'
  if (level === 'good') return 'bg-[var(--status-success-bg)] text-[var(--status-success-fg)]'
  if (level === 'warning') return 'bg-amber-500/[0.14] text-amber-700'
  return 'bg-red-500/[0.12] text-red-700'
}
function fusionStatusClass(tone) {
  if (tone === 'success') return 'bg-[var(--citation-bg)] text-[var(--citation-fg)]'
  if (tone === 'warning') return 'bg-[var(--route-mode-shadow-bg)] text-[var(--route-mode-shadow-fg)]'
  return 'bg-[var(--status-waiting-bg)] text-[var(--status-waiting-fg)]'
}
function channelThroughputItems(channel) {
  return [
    { key: 'recalled', label: '召回', value: channel.recalledCount, ratio: channel.recalledRatio },
    { key: 'accepted', label: '闸门通过', value: channel.acceptedCount, ratio: channel.acceptedRatio },
    { key: 'selected', label: '最终证据', value: channel.finalSelectedCount, ratio: channel.selectedRatio }
  ]
}
function channelThroughputFillClass(key) {
  if (key === 'selected') return 'bg-primary'
  if (key === 'accepted') return 'bg-foreground/[0.32]'
  return 'bg-foreground/[0.55]'
}
function channelStateTextClass(channel) {
  if (channel.errorMessage || [2, 3].includes(Number(channel.executionState))) return 'text-destructive'
  if (Number(channel.executionState) === 1) return 'text-[var(--status-success-fg)]'
  if (Number(channel.executionState) === 4) return 'text-[var(--status-waiting-fg)]'
  return 'text-muted-foreground'
}
function channelStateDotClass(channel) {
  if (channel.errorMessage || [2, 3].includes(Number(channel.executionState))) return 'bg-destructive'
  if (Number(channel.executionState) === 1) return 'bg-[var(--status-success-fg)]'
  if (Number(channel.executionState) === 4) return 'bg-[var(--status-waiting-fg)]'
  return 'bg-border-strong'
}
function tableEvidencePairs(item) {
  return [
    { label: '表格 ID / 编号', value: [item.tableId ? `ID ${item.tableId}` : '', item.tableNo ? `T#${item.tableNo}` : ''].filter(Boolean).join(' / ') || '无' },
    { label: '查询操作', value: item.operationText },
    { label: '命中行数', value: item.matchedRowCount === '' ? '无' : `${item.matchedRowCount} 行` },
    { label: '证据行号', value: item.rowsText },
    { label: '证据列', value: item.columnsText },
    { label: '单元格坐标', value: item.cellsText },
    { label: '页码/位置', value: item.locationText },
    { label: 'bbox 状态', value: item.bboxText }
  ]
}
function tableEvidenceDocumentRoute(item) {
  return {
    name: 'AdminDocumentDetail',
    params: { documentId: item.documentId },
    query: {
      section: 'rag',
      highlightTableId: item.tableId || undefined,
      highlightTableNo: item.tableNo || undefined,
      highlightRows: item.rowNos.length ? item.rowNos.join(',') : undefined,
      highlightColumns: item.columnNames.length ? item.columnNames.join(',') : undefined,
      highlightCells: item.cellCoordinates.length ? item.cellCoordinates.join(',') : undefined
    }
  }
}
function citationDocumentRoute(item) {
  return {
    name: 'AdminDocumentDetail',
    params: { documentId: item.documentId },
    query: {
      section: 'rag',
      highlightChunkId: item.chunkId || undefined,
      highlightParentBlockId: item.parentBlockId || undefined,
      highlightTableId: item.tableId || undefined,
      highlightPageNo: item.pageNo || undefined
    }
  }
}
function benchmarkCols(trace) {
  const bm = findBenchmark(trace.stageCode, trace.executionMode)
  if (!bm) return []
  return [
    { label: 'P50', value: `${bm.p50DurationMs || '-'} ms` },
    { label: 'P90', value: `${bm.p90DurationMs || '-'} ms` },
    { label: 'P99', value: `${bm.p99DurationMs || '-'} ms` },
    { label: '样本数', value: bm.sampleCount }
  ]
}
async function loadStageBenchmarks() {
  loadingBenchmarks.value = true
  benchmarkError.value = ''
  try {
    stageBenchmarks.value = await chatApi.getStageBenchmarks() || []
  } catch (error) {
    stageBenchmarks.value = []
    benchmarkError.value = normalizeError(error, '阶段基准读取失败')
  } finally { loadingBenchmarks.value = false }
}
function findBenchmark(stageCode, executionMode) {
  return stageBenchmarks.value.find((b) => b.stageCode === stageCode && b.executionMode === executionMode) || null
}
function formatBenchmarkComparison(actualMs, benchmark) {
  if (!benchmark || !actualMs) return null
  const { p50DurationMs: p50 = 0, p90DurationMs: p90 = 0, p99DurationMs: p99 = 0 } = benchmark
  if (actualMs <= p50) return { level: 'excellent', text: '优秀（≤ P50）' }
  if (actualMs <= p90) return { level: 'good', text: '良好（P50-P90）' }
  if (actualMs <= p99) return { level: 'warning', text: '偏慢（P90-P99）' }
  return { level: 'slow', text: '异常慢（> P99）' }
}
async function loadRetrievalObserveData() {
  if (!conversationId.value || !exchangeId.value) return
  loadingRetrievalData.value = true
  retrievalError.value = ''
  const [results, executions] = await Promise.allSettled([
    chatApi.getRetrievalResults(conversationId.value, exchangeId.value),
    chatApi.getChannelExecutions(conversationId.value, exchangeId.value)
  ])
  if (results.status === 'fulfilled') {
    retrievalResults.value = Array.isArray(results.value) ? results.value : []
  } else {
    retrievalError.value = normalizeError(results.reason, '检索结果读取失败')
  }
  if (executions.status === 'fulfilled') {
    channelExecutions.value = Array.isArray(executions.value) ? executions.value : []
  } else {
    retrievalError.value = [retrievalError.value, normalizeError(executions.reason, '通道执行读取失败')].filter(Boolean).join('；')
  }
  loadingRetrievalData.value = false
}
async function loadPage() {
  if (!conversationId.value || !exchangeId.value) return
  loadingPage.value = true; pageError.value = ''
  try {
    const [session, exchangeDetail] = await Promise.all([chatApi.getSession(conversationId.value), chatApi.getExchangeDetail(conversationId.value, exchangeId.value)])
    activeSession.value = session; activeExchangeDetail.value = exchangeDetail
    loadRetrievalObserveData(); loadStageBenchmarks()
  } catch (error) { activeSession.value = null; activeExchangeDetail.value = null; pageError.value = normalizeError(error, '加载轮次详情失败') }
  finally { loadingPage.value = false }
}
function openTraceDetail(trace) { overlayInspector.value = buildTraceStageInspector(trace, activeExchange.value); traceDetailOpen.value = true }
function closeTraceDetail() { traceDetailOpen.value = false; overlayInspector.value = null }
function openCandidateDetail(row) { candidateDetail.value = row; candidateDetailOpen.value = true }
function closeCandidateDetail() { candidateDetailOpen.value = false; candidateDetail.value = null }
function handleTraceDialogUpdate(value) { if (!value) closeTraceDetail() }
function handleCandidateDialogUpdate(value) { if (!value) closeCandidateDetail() }
async function copyPrompt() {
  if (!activePromptText.value || typeof navigator === 'undefined' || !navigator.clipboard?.writeText) return
  try {
    await navigator.clipboard.writeText(activePromptText.value)
    promptCopyState.value = '已复制'
    window.setTimeout(() => { promptCopyState.value = '' }, 1600)
  } catch {
    promptCopyState.value = '复制失败'
  }
}
function traceBarWidth(trace) { const d = Number(trace?.durationMs || 0); const m = maxTraceDuration.value; if (!d || !m) return '6%'; return `${Math.max((d / m) * 100, 6)}%` }
function findStageTrace(stageTitle) {
  if (!stageTitle) return null
  if (stageTitle.includes('检索执行')) return stageTraces.value.find((item) => item.stageCode === 'RAG_RETRIEVE' || item.stageCode === 'REACT_AGENT') || null
  if (stageTitle.includes('前置编排')) return stageTraces.value.find((item) => item.stageCode === 'INTENT') || null
  if (stageTitle.includes('请求入口')) return stageTraces.value.find((item) => item.stageCode === 'ROUTE') || null
  if (stageTitle.includes('生成回答') || stageTitle.includes('模型使用')) return stageTraces.value.find((item) => item.stageCode === 'ANSWER_GENERATE') || null
  if (stageTitle.includes('结果与诊断')) return stageTraces.value.find((item) => item.stageCode === 'FINALIZE') || null
  return null
}
function canOpenStage(stage) { return stage?.key === 'usage' || Boolean(findStageTrace(stage?.title)) }
function openSummaryStage(stage) {
  if (!stage) return
  if (stage.key === 'usage') { overlayInspector.value = buildUsageStageInspector(activeExchange.value); traceDetailOpen.value = true; return }
  const trace = findStageTrace(stage.title)
  if (!trace) return
  overlayInspector.value = buildTraceStageInspector(trace, activeExchange.value); traceDetailOpen.value = true
}
function signalStage(stageKey) { return exchangeStages.value.find((stage) => stage.key === stageKey) || null }
function canOpenSignalStage(stageKey) { return canOpenStage(signalStage(stageKey)) }
function openSignalStage(stageKey) { openSummaryStage(signalStage(stageKey)) }
function signalPathActive(...keys) { return keys.includes(highlightedSignal.value) }
function signalFlowLinkActive(link) { return signalPathActive(...link.signals) }
function signalMarkerEnd(...keys) { return signalPathActive(...keys) ? 'url(#signal-flow-arrow-active)' : 'url(#signal-flow-arrow)' }
function signalEvidenceClass(state) {
  if (state === 'available') return 'border-[var(--status-success-border)] bg-[var(--status-success-bg)] text-[var(--status-success-fg)]'
  return 'border-[var(--status-waiting-border)] bg-[var(--status-waiting-bg)] text-[var(--status-waiting-fg)]'
}
function signalAnswerClass(status) {
  const tone = statusTone(status)
  if (tone === 'completed') return 'border-[var(--status-success-border)] bg-[var(--status-success-bg)] text-[var(--status-success-fg)]'
  if (tone === 'failed') return 'border-[var(--status-danger-border)] bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)]'
  if (tone === 'stopped') return 'border-[var(--status-waiting-border)] bg-[var(--status-waiting-bg)] text-[var(--status-waiting-fg)]'
  if (tone === 'running') return 'border-[var(--status-running-border)] bg-[var(--status-running-bg)] text-[var(--status-running-fg)]'
  return 'border-border-strong bg-background text-muted-foreground'
}
function signalStatusTextClass(status) {
  const tone = statusTone(status)
  if (tone === 'completed') return 'text-[var(--status-success-fg)]'
  if (tone === 'failed') return 'text-[var(--status-danger-fg)]'
  if (tone === 'stopped') return 'text-[var(--status-waiting-fg)]'
  if (tone === 'running') return 'text-[var(--status-running-fg)]'
  return 'text-foreground'
}
function signalResourceClass(tone) {
  return tone === 'warning' ? 'text-[var(--status-waiting-fg)]' : 'text-foreground'
}
watch(() => fusionTraceView.value.groups.map((group) => String(group.index)).join('|'), () => {
  const keys = fusionTraceView.value.groups.map((group) => String(group.index))
  // keep any already-open groups; add any new keys (default open)
  const existing = new Set(openFusionGroupKeys.value)
  const merged = keys.filter((k) => !existing.has(k))
  if (merged.length) openFusionGroupKeys.value = [...openFusionGroupKeys.value, ...merged]
}, { immediate: true })
watch([conversationId, exchangeId], () => { activeSession.value = null; activeExchangeDetail.value = null; traceDetailOpen.value = false; overlayInspector.value = null; candidateDetailOpen.value = false; candidateDetail.value = null; loadPage() }, { immediate: true })

watchEffect(() => {
  if (typeof window === 'undefined') return
  window.__obsDetailState = { loadingPage: loadingPage.value, hasSession: Boolean(activeSession.value), hasExchangeDetail: Boolean(activeExchangeDetail.value), conversationId: conversationId.value, exchangeId: exchangeId.value, traceDetailOpen: traceDetailOpen.value, overlayTitle: overlayInspector.value?.title || '' }
})
</script>

<style scoped>
.answer-segment :deep(strong) {
  font-weight: 600;
}
.answer-segment :deep(a) {
  color: var(--accent-foreground);
  text-decoration: underline;
  text-underline-offset: 3px;
}
.answer-segment :deep(code) {
  padding: 1px 5px;
  border-radius: var(--radius-token-sm);
  background: var(--secondary);
  font-size: 0.85em;
}
.answer-segment :deep(pre) {
  overflow-x: auto;
  margin: 8px 0;
  padding: 10px;
  border-radius: var(--radius-token-card);
  background: var(--code);
  color: var(--code-foreground);
}
.answer-segment :deep(pre code) {
  background: none;
  padding: 0;
}

.fusion-subquestion {
  display: grid;
  gap: 0;
  border: 0;
  background: transparent;
}

.fusion-subquestion :deep(.fusion-subquestion-trigger) {
  border: 1px solid var(--border);
  background: var(--secondary);
  color: var(--foreground);
}

.fusion-subquestion :deep(.fusion-subquestion-trigger:hover) {
  border-color: var(--border-strong);
  background: var(--muted);
}

.fusion-subquestion :deep(.fusion-subquestion-trigger[data-state='open']) {
  border-end-start-radius: 0;
  border-end-end-radius: 0;
}

.fusion-subquestion-index {
  background: var(--border-strong);
  color: var(--card);
}

.detail-section-head {
  border: 1px solid var(--border);
  background: var(--secondary);
}

.detail-section-icon {
  background: var(--border-strong);
  color: var(--card);
}

.detail-rail {
  position: relative;
  display: grid;
  gap: 0.75rem;
  margin-inline-start: 1.25rem;
  padding-block: 0.875rem 0.25rem;
  padding-inline-start: 1.25rem;
  border-inline-start: 1px solid var(--border-strong);
}

/*
 * 这些区段不能加 .glass-edge：它和下面的 ::before 抢同一个伪元素。glass-edge 声明
 * inset-inline: 0（含 left: 0），这里声明 right: 100% + width；三者同时存在时 CSS 规定
 * 忽略 right，连接短横会被拉进卡片左边缘、压在标题上。玻璃面用 .glass-card 就够了。
 */
.detail-rail-item {
  position: relative;
}

.detail-rail-item::before {
  position: absolute;
  top: 1.625rem;
  right: 100%;
  width: 1.25rem;
  height: 1px;
  background: var(--border-strong);
  content: '';
}

@media (min-width: 40rem) {
  .detail-rail {
    margin-inline-start: 1.75rem;
    padding-inline-start: 1.5rem;
  }

  .detail-rail-item::before {
    width: 1.5rem;
  }
}

.fusion-channel-lane {
  background: transparent;
  transition: background-color var(--motion-standard) var(--ease-standard);
}

.fusion-channel-lane:hover,
.fusion-channel-lane:focus-within {
  background: var(--muted);
}

.fusion-channel-fill {
  transform: scaleX(var(--lane-scale, 0));
  animation: fusion-lane-reveal var(--motion-slow) var(--ease-standard) both;
}

.fusion-candidate-track-row {
  --fusion-outcome-bg: var(--card);
  --fusion-outcome-border: var(--border-strong);
  --fusion-outcome-fg: var(--muted-foreground);
}

.fusion-candidate-track-row[data-tone='success'] {
  --fusion-outcome-bg: var(--selection-bg);
  --fusion-outcome-border: var(--selection-fg);
  --fusion-outcome-fg: var(--selection-fg);
}

.fusion-candidate-track-row[data-tone='warning'] {
  --fusion-outcome-bg: var(--status-waiting-bg);
  --fusion-outcome-border: var(--status-waiting-border);
  --fusion-outcome-fg: var(--status-waiting-fg);
}

.fusion-candidate-track-row {
  --fusion-content-chip-bg: var(--route-mode-auto-bg);
  --fusion-content-chip-border: var(--route-mode-auto-border);
  --fusion-content-chip-fg: var(--route-mode-auto-fg);
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(13rem, 1fr) minmax(29rem, 1.8fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  border: 1px solid transparent;
  border-radius: var(--radius-token-card);
  background: var(--secondary);
  box-shadow: var(--shadow-control);
  cursor: default;
  transition:
    background-color var(--motion-standard) var(--ease-standard),
    border-color var(--motion-standard) var(--ease-standard),
    box-shadow var(--motion-standard) var(--ease-standard),
    transform var(--motion-standard) var(--ease-standard);
}

.fusion-candidate-track-row:hover {
  border-color: var(--border-strong);
  background: var(--secondary-hover);
  box-shadow: var(--shadow-control);
  transform: translateY(-1px);
}

.fusion-candidate-track-row[data-content-kind='正文预览'] {
  --fusion-content-chip-bg: var(--status-success-bg);
  --fusion-content-chip-border: var(--status-success-border);
  --fusion-content-chip-fg: var(--status-success-fg);
}

.fusion-candidate-track-row[data-content-kind='内容状态'] {
  --fusion-content-chip-bg: var(--status-danger-bg);
  --fusion-content-chip-border: var(--status-danger-border);
  --fusion-content-chip-fg: var(--status-danger-fg);
}

.fusion-candidate-context {
  display: grid;
  min-width: 0;
  gap: 0.25rem;
  justify-items: start;
}

.fusion-candidate-layer-card {
  box-sizing: border-box;
  display: block;
  width: fit-content;
  max-width: 100%;
  padding: 0.125rem 0.375rem;
  border: 1px solid;
  border-radius: var(--radius-token-sm);
  font-family: var(--font-sans-token);
  line-height: 1.25;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.fusion-candidate-layer-card--kind {
  border-color: var(--fusion-content-chip-border);
  background: var(--fusion-content-chip-bg);
  color: var(--fusion-content-chip-fg);
  font-size: var(--text-micro);
  font-weight: 600;
}

.fusion-candidate-layer-card--content {
  border-color: var(--fusion-content-chip-border);
  background: var(--fusion-content-chip-bg);
  color: var(--fusion-content-chip-fg);
  font-size: var(--text-caption);
  font-weight: 500;
}

.fusion-candidate-layer-card--source {
  border-color: var(--border);
  background: var(--background);
  color: var(--muted-foreground);
  font-size: var(--text-micro);
}

.fusion-candidate-track {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1.25rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.fusion-candidate-stage {
  position: relative;
  min-width: 0;
}

.fusion-candidate-stage-node {
  display: grid;
  min-width: 0;
  min-height: 3.5rem;
  align-content: center;
  gap: 0.25rem;
  padding: 0.375rem 0.25rem;
  border-radius: var(--radius-token-md);
}

.fusion-candidate-stage-node::before {
  width: 0.375rem;
  height: 0.375rem;
  border-radius: var(--radius-token-round);
  background: var(--border-strong);
  content: '';
}

.fusion-candidate-stage[data-stage-key='outcome'] .fusion-candidate-stage-node {
  border: 1px solid var(--fusion-outcome-border);
  background: var(--fusion-outcome-bg);
  color: var(--fusion-outcome-fg);
}

.fusion-candidate-stage[data-stage-key='outcome'] .fusion-candidate-stage-node::before {
  background: var(--fusion-outcome-fg);
}

.fusion-candidate-track-row[data-tone='neutral'] .fusion-candidate-stage[data-stage-key='outcome'] .fusion-candidate-stage-node::before {
  border-radius: var(--radius-token-sm);
  transform: rotate(45deg);
}

.fusion-candidate-track-row[data-tone='warning'] .fusion-candidate-stage[data-stage-key='outcome'] .fusion-candidate-stage-node::before {
  border-radius: var(--radius-token-sm);
}

.fusion-candidate-stage-label {
  display: block;
  color: var(--muted-foreground);
  font-size: var(--text-micro);
  line-height: 1.25;
}

.fusion-candidate-stage-value {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--foreground);
  font-size: var(--text-caption);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;
}

.fusion-candidate-stage[data-stage-key='outcome'] .fusion-candidate-stage-label,
.fusion-candidate-stage[data-stage-key='outcome'] .fusion-candidate-stage-value {
  color: currentColor;
}

.fusion-candidate-stage-arrow {
  position: absolute;
  top: 50%;
  right: -1.125rem;
  display: block;
  width: 1rem;
  height: 1rem;
  color: var(--border-strong);
  transform: translateY(-50%);
}

.fusion-candidate-stage-arrow > svg {
  width: 100%;
  height: 100%;
}

.fusion-candidate-detail-button {
  justify-self: end;
  white-space: nowrap;
}

@media (max-width: 1279px) {
  .fusion-candidate-track-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .fusion-candidate-track {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .fusion-candidate-detail-button {
    grid-column: 2;
    grid-row: 1;
  }
}

@media (max-width: 639px) {
  .fusion-candidate-track-row {
    grid-template-columns: minmax(0, 1fr);
    gap: 0.75rem;
  }

  .fusion-candidate-track {
    grid-column: auto;
    grid-row: auto;
    gap: 0.75rem;
  }

  .fusion-candidate-stage-node {
    min-height: 3.25rem;
    padding-inline: 0;
  }

  .fusion-candidate-stage-arrow {
    right: -0.875rem;
    width: 0.875rem;
    height: 0.875rem;
  }

  .fusion-candidate-detail-button {
    width: 100%;
    grid-column: auto;
    grid-row: auto;
    justify-self: stretch;
  }
}

@keyframes fusion-lane-reveal {
  from {
    transform: scaleX(0);
  }
}

.signal-flow-path {
  fill: none;
  stroke: var(--border-strong);
  stroke-width: 1.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  vector-effect: non-scaling-stroke;
  animation: signal-path-reveal var(--motion-slow) var(--ease-standard) forwards;
  transition: stroke var(--motion-standard) var(--ease-standard), stroke-width var(--motion-standard) var(--ease-standard);
}

.signal-flow-path--answer {
  animation-delay: var(--motion-fast);
}

.signal-flow-path--active {
  stroke: var(--foreground);
  stroke-width: 2.25;
}

.signal-node-panel {
  transform: scale(1);
  transform-origin: center;
  transition:
    background-color var(--motion-standard) var(--ease-standard),
    border-color var(--motion-standard) var(--ease-standard),
    box-shadow var(--motion-standard) var(--ease-standard),
    transform var(--motion-standard) var(--ease-standard);
}

/* 静息态是 L2 玻璃面（0.48），hover 抬到 raised（0.62）而不是跳到不透明的 --muted：
   后者会在 hover 瞬间把玻璃变成实色，读成闪一下而不是抬起。 */
.signal-node-panel:hover,
.signal-node-panel:focus-within {
  border-color: var(--border-strong);
  background: var(--glass-raised);
  box-shadow: var(--shadow-glass-lg);
  transform: scale(1.015);
}

.signal-resource-link {
  position: relative;
  color: var(--border-strong);
  transition: color var(--motion-standard) var(--ease-standard);
}

.signal-resource-link--active {
  color: var(--foreground);
}

.signal-resource-link-line {
  position: absolute;
  top: 0;
  bottom: 0.35rem;
  left: 50%;
  width: 1px;
  background: currentColor;
  transform: translateX(-50%);
}

.signal-resource-link-arrow {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  transition: width var(--motion-standard) var(--ease-standard), height var(--motion-standard) var(--ease-standard);
}

@keyframes signal-path-reveal {
  to {
    stroke-dashoffset: 0;
  }
}

@media (max-width: 1023px) {
  .signal-map-body::before {
    position: absolute;
    top: 1.75rem;
    bottom: 1.75rem;
    left: 1.75rem;
    width: 1px;
    background: var(--border-strong);
    content: '';
  }
}

@media (prefers-reduced-motion: reduce) {
  .fusion-channel-fill {
    animation: none;
  }

  .fusion-candidate-track-row {
    transition: none;
  }

  .fusion-candidate-track-row:hover {
    transform: none;
  }

  .signal-flow-path {
    stroke-dashoffset: 0;
    animation: none;
  }

  .signal-node-panel:hover,
  .signal-node-panel:focus-within {
    transform: none;
  }

  .signal-resource-link-arrow {
    transition: none;
  }
}
</style>
