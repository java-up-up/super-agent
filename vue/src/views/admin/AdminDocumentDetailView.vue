<template>
  <section class="flex flex-col gap-4">

    <ChildPageDialog
      :open="showBuildBlockingOverlay"
      :title="buildOverlayTitle"
      :description="buildOverlayDescription"
      :show-close="false"
      close-label="构建进行中"
      @update:open="handleBuildDialogOpen"
    >
        <div class="grid gap-4">
          <div class="mb-4 flex items-center gap-4">
            <span class="build-overlay-spinner shrink-0" aria-hidden="true"></span>
            <div><h3 class="text-base font-semibold text-foreground">{{ buildOverlayTitle }}</h3><p class="mt-1 text-compact text-[var(--muted-foreground)]">{{ buildOverlayDescription }}</p></div>
          </div>
          <div class="mb-4 flex flex-wrap gap-3 text-xs text-muted-foreground"><span>任务 {{ buildTaskSnapshot?.taskId || activeBuildTaskId || '创建中' }}</span><span>当前阶段 {{ activeBuildStageLabel || '准备启动' }}</span></div>
          <div class="grid grid-cols-2 gap-2">
            <article v-for="stage in buildStageItems" :key="`overlay-stage-${stage.code}`" class="flex items-center gap-3 rounded-lg border p-3" :class="buildProgressVisual(stage.status).card">
              <span class="grid h-7 w-7 shrink-0 place-items-center rounded-full text-xs font-bold" :class="buildProgressVisual(stage.status).marker"><span v-if="stage.status === 'current'" class="stage-spinner" aria-hidden="true"></span><CheckCircleIcon v-else-if="['done','completed'].includes(stage.status)" class="h-4 w-4" /><span v-else>{{ stage.order }}</span></span>
              <div><strong class="block text-compact text-foreground">{{ stage.label }}</strong><span class="text-xs text-muted-foreground">{{ stage.statusLabel }}</span></div>
            </article>
          </div>
          <p class="mt-4 text-xs text-muted-foreground">执行期间页面已暂时锁定，避免重复发起构建或误改当前策略链路。</p>
        </div>
    </ChildPageDialog>

    <DocumentTaskHistoryDialog
      :open="logDrawerOpen"
      :document-detail="documentDetail"
      :logs="taskLogs"
      :loading="logLoading"
      @update:open="handleLogDialogOpen"
    />

    <ChildPageDialog
      :open="chunkDetailDrawerOpen"
      title="切块详情"
      :description="chunkDetail?.chunk ? `子块 C#${chunkDetail.chunk.chunkNo || '-'} · 父块 P#${chunkDetail.parentBlock?.parentBlockNo || '-'}` : '正在读取切块详情'"
      size="default"
      close-label="关闭切块详情"
      @update:open="handleChunkDialogOpen"
    >
        <div v-if="chunkDetailLoading" class="flex-1 py-8 text-center text-sm text-muted-foreground">正在加载 chunk 详情...</div>
        <div v-else-if="!chunkDetail?.chunk" class="flex-1 py-8 text-center text-sm text-muted-foreground">当前没有可展示的 chunk 详情。</div>
        <div v-else>
          <div class="mb-4 flex flex-wrap gap-3">
            <div class="flex items-center gap-2 text-xs text-muted-foreground"><span>当前子块</span><strong class="text-foreground">C#{{ chunkDetail.chunk.chunkNo || '-' }}</strong></div>
            <div class="flex items-center gap-2 text-xs text-muted-foreground"><span>所属父块</span><strong class="text-foreground">P#{{ chunkDetail.parentBlock?.parentBlockNo || '-' }}</strong></div>
            <div class="flex items-center gap-2 text-xs text-muted-foreground"><span>同父子块</span><strong class="text-foreground">{{ chunkDetail.parentBlock?.childCount || chunkDetail.siblingChunks?.length || 0 }}</strong></div>
          </div>
          <div class="pipeline-tone-child mb-4 rounded-lg border p-4" style="background: var(--pl-bg); border-color: var(--pl-border)">
            <div class="mb-2 flex items-center justify-between gap-2"><div class="flex items-center gap-2"><span class="rounded px-2 py-0.5 text-micro font-bold text-white" style="background: var(--pl-solid)">Child Evidence</span><h4 class="text-sm font-semibold text-foreground">当前子块 C#{{ chunkDetail.chunk.chunkNo || '-' }}</h4></div><span class="text-xs text-muted-foreground">{{ build子块RelationText(chunkDetail.chunk) }}</span></div>
            <div class="mb-2 flex flex-wrap gap-3 text-xs text-muted-foreground"><span>章节：{{ chunkDetail.chunk.sectionPath || '未识别章节' }}</span><span>字符：{{ formatCount(chunkDetail.chunk.charCount) }}</span><span>Token：{{ formatCount(chunkDetail.chunk.tokenCount) }}</span></div>
            <pre class="overflow-auto whitespace-pre-wrap break-words text-compact text-foreground">{{ chunkDetail.chunk.chunkText }}</pre>
          </div>
          <div v-if="chunkDetail.parentBlock" class="pipeline-tone-parent mb-4 rounded-lg border p-4" style="background: var(--pl-bg); border-color: var(--pl-border)">
            <div class="mb-2 flex items-center justify-between gap-2"><div class="flex items-center gap-2"><span class="rounded px-2 py-0.5 text-micro font-bold text-white" style="background: var(--pl-solid)">Parent Context</span><h4 class="text-sm font-semibold text-foreground">所属父块 P#{{ chunkDetail.parentBlock.parentBlockNo || '-' }}</h4></div><span class="text-xs text-muted-foreground">子块范围 C#{{ chunkDetail.parentBlock.start子块No || '-' }} - C#{{ chunkDetail.parentBlock.end子块No || '-' }}</span></div>
            <div class="mb-2 flex flex-wrap gap-3 text-xs text-muted-foreground"><span>章节：{{ chunkDetail.parentBlock.sectionPath || '未识别章节' }}</span><span>字符：{{ formatCount(chunkDetail.parentBlock.charCount) }}</span><span>Token：{{ formatCount(chunkDetail.parentBlock.tokenCount) }}</span></div>
            <pre class="overflow-auto whitespace-pre-wrap break-words text-compact text-foreground">{{ chunkDetail.parentBlock.parentText }}</pre>
          </div>
          <div v-if="Array.isArray(chunkDetail.siblingChunks) && chunkDetail.siblingChunks.length" class="pipeline-tone-child rounded-lg border p-4" style="background: var(--pl-bg); border-color: var(--pl-border)">
            <div class="mb-2 flex items-center justify-between gap-2"><h4 class="text-sm font-semibold text-foreground">同父子块关系</h4><span class="text-xs text-muted-foreground">点击可切换查看其他子块</span></div>
            <p class="mb-3 text-xs text-muted-foreground">当前父块 P#{{ chunkDetail.parentBlock?.parentBlockNo || '-' }} 内包含 {{ formatChunkCodeList(chunkDetail.siblingChunks) }} 这些子块，当前命中的是 C#{{ chunkDetail.chunk.chunkNo || '-' }}。</p>
            <div class="mb-3 flex flex-wrap items-center gap-2">
              <template v-for="(item, index) in chunkDetail.siblingChunks" :key="`track-${item.chunkId}`">
                <Button variant="ghost" size="sm" class="flex flex-col items-center rounded-lg border px-3 py-2 text-center !h-auto" :class="isCurrent子块(item) ? '' : 'border-border bg-card'" :style="isCurrent子块(item) ? 'background: var(--pl-bg); border-color: var(--pl-solid)' : ''" type="button" @click="open子块Detail(item.chunkId)"><strong class="text-compact text-foreground">C#{{ item.chunkNo || '-' }}</strong><span class="text-micro text-muted-foreground">{{ buildSiblingOrderLabel(index, chunkDetail.siblingChunks.length) }}</span></Button>
                <div v-if="index < chunkDetail.siblingChunks.length - 1" class="h-0.5 w-4 rounded bg-border"></div>
              </template>
            </div>
            <div class="grid gap-2" style="grid-template-columns:repeat(auto-fill,minmax(180px,1fr))">
              <Button v-for="item in chunkDetail.siblingChunks" :key="`sibling-${item.chunkId}`" variant="ghost" size="sm" class="grid gap-1 rounded-lg border p-3 text-left !h-auto !whitespace-normal" :class="normalizeCode(item.chunkId) === normalizeCode(chunkDetail.chunk.chunkId) ? '' : 'border-border bg-card'" :style="normalizeCode(item.chunkId) === normalizeCode(chunkDetail.chunk.chunkId) ? 'background: var(--pl-bg); border-color: var(--pl-border)' : ''" type="button" @click="open子块Detail(item.chunkId)">
                <div class="flex items-center justify-between gap-2"><strong class="text-compact text-foreground">子块 C#{{ item.chunkNo || '-' }}</strong><span class="text-xs text-muted-foreground">{{ build子块RelationText(item) }}</span></div>
                <p class="text-xs text-muted-foreground">{{ item.sectionPath || '未识别章节' }}</p>
                <span class="line-clamp-2 text-xs text-foreground">{{ item.chunkText }}</span>
              </Button>
            </div>
          </div>
        </div>
    </ChildPageDialog>

    <div class="flex items-center justify-between gap-4 max-sm:flex-col max-sm:items-stretch">
      <div class="min-w-0">
        <div class="mb-1 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
          <Button variant="outline" size="sm" class="gap-1.5" type="button" @click="goBack"><ArrowLeftIcon class="h-4 w-4" />返回文档列表</Button>
          <span>文档接入</span><span>/</span><strong class="text-foreground">文档工作台</strong>
        </div>
      </div>
      <Button variant="outline" class="rounded-md" size="lg" type="button" :disabled="loading" @click="loadAll">{{ loading ? '刷新中...' : '刷新详情' }}</Button>
    </div>

    <div v-if="pageNotice.message" class="rounded-md px-4 py-3 text-sm font-medium" :class="noticeClass(pageNotice.type)">{{ pageNotice.message }}</div>

    <article v-if="documentDetail" class="glass-card glass-edge rounded-glass border">
      <nav class="flex overflow-x-auto rounded-tl-lg rounded-tr-lg bg-secondary" aria-label="文档工作台章节导航">
        <Button v-for="item in workbenchSections" :key="`workbench-nav-${item.key}`"
          variant="ghost" size="sm"
          class="flex shrink-0 items-center gap-3 border-b-2 border-transparent px-5 py-4 !h-auto first:rounded-tl-lg last:rounded-tr-lg"
          :class="activeWorkbenchSection === item.key ? '!border-b-primary bg-card text-primary' : 'text-foreground hover:bg-secondary/80'"
          type="button" @click="scrollToWorkbenchSection(item.key)">
          <span class="grid h-7 w-7 shrink-0 place-items-center rounded-full text-xs font-bold"
            :class="activeWorkbenchSection === item.key ? 'bg-primary text-white' : 'bg-foreground/[0.08] text-muted-foreground'">{{ item.step }}</span>
          <span class="flex flex-col items-start">
            <strong class="text-compact font-semibold">{{ item.label }}</strong>
            <span class="text-xs text-muted-foreground">{{ item.caption }}</span>
          </span>
          <em class="hidden whitespace-nowrap text-micro not-italic text-muted-foreground md:block">{{ item.status }}</em>
        </Button>
      </nav>

      <div class="p-5">
        <section v-show="activeWorkbenchSection === 'overview'" ref="overviewSectionRef" data-workbench-section="overview">
          <!-- 文档标识 + 三状态徽章 -->
          <div class="mb-5 flex items-center justify-between gap-4 max-md:flex-col max-md:items-start">
            <div class="w-full min-w-0 max-w-full">
              <h2 class="max-w-full break-words text-lg font-semibold text-foreground [overflow-wrap:anywhere]">{{ documentDetail.documentName }}</h2>
              <p v-if="showOriginalFileName" class="mt-0.5 block w-full max-w-full truncate text-xs text-muted-foreground" :title="documentDetail.originalFileName">{{ documentDetail.originalFileName }}</p>
            </div>
            <div class="flex shrink-0 flex-wrap gap-1.5">
              <AdminStatusBadge :label="documentDetail.parseStatusName" :code="documentDetail.parseStatus" type="parse" />
              <AdminStatusBadge :label="documentDetail.strategyStatusName" :code="documentDetail.strategyStatus" type="strategy" />
              <AdminStatusBadge :label="documentDetail.indexStatusName" :code="documentDetail.indexStatus" type="index" />
            </div>
          </div>

          <!-- 关键指标带：真实字段大数字磁贴 -->
          <div class="mb-5 grid grid-cols-2 gap-px overflow-hidden rounded-lg border border-border bg-border sm:grid-cols-3 lg:grid-cols-5">
            <div v-for="metric in documentMetrics" :key="`metric-${metric.key}`" class="bg-card px-4 py-3">
              <div class="flex items-baseline gap-1.5">
                <span class="tabular-nums text-title font-semibold leading-none text-foreground">{{ metric.value }}</span>
                <span v-if="metric.tone === 'success'" class="h-1.5 w-1.5 shrink-0 translate-y-[-2px] rounded-full bg-[var(--status-success-fg)]" aria-hidden="true"></span>
              </div>
              <p class="mt-1.5 text-caption font-medium text-foreground">{{ metric.label }}</p>
              <p class="text-micro text-muted-foreground">{{ metric.hint }}</p>
            </div>
          </div>

          <!-- 处理状态轨道：完成=空心绿勾 / 当前=实心焦点 / 待办=中性，颜色非唯一信号 -->
          <div class="mb-5 flex items-start">
            <template v-for="(step, i) in workflowSteps" :key="step.key">
              <div class="flex flex-col items-center gap-1.5">
                <span class="grid h-8 w-8 place-items-center rounded-full text-caption font-bold transition-colors" :class="trackNodeVisual(step.state, step.key === 'build' && hasBuildInFlightStatus).node">
                  <CheckCircleIcon v-if="step.state === 'done' || (step.key === 'done' && step.state === 'current')" class="h-4 w-4" />
                  <ExclamationCircleIcon v-else-if="step.state === 'failed'" class="h-4 w-4" />
                  <span v-else>{{ i + 1 }}</span>
                </span>
                <span class="whitespace-nowrap text-micro" :class="trackNodeVisual(step.state, false).label">{{ step.label }}</span>
              </div>
              <span v-if="i < workflowSteps.length - 1" class="mx-1.5 mt-4 h-0.5 flex-1 rounded-full" :class="trackNodeVisual(step.state, false).line"></span>
            </template>
          </div>

          <!-- 当前状态 + 下一步 + 唯一主操作：中性单行动作带，状态用小圆点信号 -->
          <div class="rounded-lg border border-border p-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="flex min-w-0 flex-1 items-start gap-2.5">
                <span class="mt-1 h-2 w-2 shrink-0 rounded-full" :class="guidanceDotClass(workflowCurrentPhase.tone)" aria-hidden="true"></span>
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <strong class="text-sm text-foreground">{{ workflowCurrentPhase.title }}</strong>
                    <span class="inline-flex rounded-full bg-secondary px-2 py-0.5 text-micro font-semibold text-muted-foreground">{{ workflowCurrentPhase.shortLabel }}</span>
                  </div>
                  <p class="mt-1.5 text-xs leading-relaxed text-muted-foreground"><span class="font-semibold text-foreground">下一步：</span>{{ workflowNextAction.title }} — {{ workflowNextAction.description }}</p>
                </div>
              </div>
              <Button size="lg" class="shrink-0 gap-1.5 rounded-md" type="button" @click="runOverviewPrimaryAction">
                <span>{{ overviewPrimaryAction.label }}</span>
                <ArrowRightIcon class="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </section>

        <section v-show="activeWorkbenchSection === 'strategy'" ref="strategySectionRef" class="mt-5" data-workbench-section="strategy">

          <div v-if="documentDetail.parseErrorMsg" class="mt-4 rounded-md border border-destructive/10 bg-destructive/[0.06] px-3 py-2.5 text-sm text-destructive">{{ documentDetail.parseErrorMsg }}</div>

          <div v-if="strategySystemStages.length" class="mb-5 flex items-center">
            <template v-for="(item, i) in strategySystemStages" :key="`strategy-stage-${item.code}`">
              <div class="flex items-center gap-2">
                <span class="grid h-6 w-6 shrink-0 place-items-center rounded-full text-micro font-bold"
                  :class="trackNodeVisual(strategyStageState(item.status, i === strategySystemStages.length - 1), false).node">
                  <CheckCircleIcon v-if="item.status === 'completed'" class="h-3.5 w-3.5" />
                  <ExclamationCircleIcon v-else-if="item.status === 'failed'" class="h-3.5 w-3.5" />
                  <span v-else>{{ item.order }}</span>
                </span>
                <span class="whitespace-nowrap text-xs" :class="trackNodeVisual(strategyStageState(item.status, i === strategySystemStages.length - 1), false).label">{{ item.label }}</span>
              </div>
              <span v-if="i < strategySystemStages.length - 1" class="mx-2 h-0.5 flex-1 rounded-full" :class="trackNodeVisual(strategyStageState(item.status, false), false).line"></span>
            </template>
          </div>

          <div v-if="planLoading" class="mt-6 py-6 text-center text-sm text-muted-foreground">正在读取策略详情...</div>
          <div v-else-if="!strategyPlan?.planReady" class="mt-6 rounded-md border border-dashed border-border py-6 text-center text-sm text-muted-foreground">当前文档尚未生成策略方案，解析完成后可点击刷新查看。</div>

          <template v-else>
            <div class="mb-3">
              <h3 class="text-sm font-semibold text-foreground">双流水线</h3>
              <p class="mt-0.5 text-micro text-muted-foreground">系统推荐已内联到每条流水线，可增删策略、用 ↑↓ 调整顺序，或一键恢复推荐。</p>
            </div>

            <div class="grid grid-cols-2 gap-4 max-md:grid-cols-1">
              <div v-for="pipeline in strategyPipelineLibrary" :key="`lane-${pipeline.key}`"
                class="overflow-hidden rounded-lg border border-border"
                :class="pipeline.key === 'parent' ? 'pipeline-tone-parent' : 'pipeline-tone-child'">
                <!-- 分类身份：靠标签片 + 编号实心色承载，不用顶部粗色带 -->
                <div class="flex items-start justify-between gap-2 border-b border-border px-4 pb-3 pt-3">
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="rounded px-1.5 py-0.5 text-technical font-bold uppercase" style="background: var(--pl-bg); color: var(--pl-fg)">
                        {{ pipeline.key === 'parent' ? '父块' : '子块' }}
                      </span>
                      <span class="text-compact font-semibold text-foreground">{{ pipeline.label }}</span>
                    </div>
                    <p class="mt-0.5 text-micro text-muted-foreground">{{ pipeline.description }}</p>
                  </div>
                  <Button v-if="!pipelineMatchesRecommendation(pipeline.key)" variant="ghost" size="sm" class="shrink-0 gap-1 rounded-md text-muted-foreground hover:text-foreground" type="button" :disabled="hasBuildInFlightStatus" @click="restoreRecommendedPipeline(pipeline.key)">
                    <ArrowPathIcon class="h-3.5 w-3.5" aria-hidden="true" />
                    <span class="text-micro">恢复推荐</span>
                  </Button>
                </div>

                <!-- 连线步骤链 -->
                <div class="px-4 pb-4 pt-4">
                  <ol v-if="getPipelineStepRows(pipeline.key).length" class="flex flex-col">
                    <li v-for="(step, index) in getPipelineStepRows(pipeline.key)" :key="`step-${pipeline.key}-${step.type}`" class="relative">
                      <div class="flex items-start gap-2.5 rounded-md border p-2.5 pipeline-lane-surface" style="border-color: var(--pl-border)">
                        <span class="grid h-5 w-5 shrink-0 place-items-center rounded-full text-technical font-bold text-white" style="background: var(--pl-solid)">{{ step.order }}</span>
                        <div class="min-w-0 flex-1">
                          <div class="flex flex-wrap items-center gap-1.5">
                            <span class="text-compact font-medium text-foreground">{{ step.label }}</span>
                            <span v-if="step.recommended" class="rounded-full border px-1.5 text-technical font-medium" style="border-color: var(--pl-border); color: var(--pl-fg)">推荐</span>
                          </div>
                          <p class="mt-0.5 text-micro text-muted-foreground">{{ step.recommendReason || step.description }}</p>
                        </div>
                        <div class="flex shrink-0 items-center gap-0.5">
                          <Button variant="ghost" size="icon-sm" class="h-6 w-6 rounded" type="button" :disabled="index === 0 || hasBuildInFlightStatus" aria-label="上移" @click="moveStrategy(step.type, -1, pipeline.key)">
                            <ChevronUpIcon class="h-3.5 w-3.5" />
                          </Button>
                          <Button variant="ghost" size="icon-sm" class="h-6 w-6 rounded" type="button" :disabled="index === getPipelineStepRows(pipeline.key).length - 1 || hasBuildInFlightStatus" aria-label="下移" @click="moveStrategy(step.type, 1, pipeline.key)">
                            <ChevronDownIcon class="h-3.5 w-3.5" />
                          </Button>
                          <Button variant="ghost" size="icon-sm" class="h-6 w-6 rounded text-muted-foreground hover:text-destructive" type="button" :disabled="hasBuildInFlightStatus" aria-label="移除" @click="toggleStrategy(step.type, pipeline.key)">
                            <XMarkIcon class="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </div>
                      <div v-if="index < getPipelineStepRows(pipeline.key).length - 1" class="ml-[19px] flex h-3 items-center">
                        <span class="h-3 w-px pipeline-step-line"></span>
                        <ArrowDownIcon class="h-3 w-3 -ml-1.5 text-muted-foreground" aria-hidden="true" />
                      </div>
                    </li>
                  </ol>
                  <p v-else class="rounded-md border border-dashed border-border py-4 text-center text-xs text-muted-foreground">还未选择策略，从下方添加</p>

                  <!-- 调色板：只列未加入的策略 -->
                  <div v-if="getPaletteStrategies(pipeline.key).length" class="mt-3 border-t border-border pt-3">
                    <span class="mb-2 block text-micro font-semibold text-muted-foreground">添加策略</span>
                    <div class="flex flex-wrap gap-1.5">
                      <Button v-for="item in getPaletteStrategies(pipeline.key)" :key="`add-${pipeline.key}-${item.type}`"
                        variant="outline" size="sm"
                        class="!h-auto items-start gap-1.5 rounded-md border-dashed px-2.5 py-1.5 text-left"
                        type="button" :disabled="hasBuildInFlightStatus" @click="toggleStrategy(item.type, pipeline.key)">
                        <PlusIcon class="mt-0.5 h-3.5 w-3.5 shrink-0" style="color: var(--pl-fg)" aria-hidden="true" />
                        <span class="flex flex-col">
                          <span class="text-caption font-medium text-foreground">{{ item.label }}</span>
                          <span class="text-micro font-normal text-muted-foreground">{{ item.description }}</span>
                        </span>
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </section>

        <section v-show="activeWorkbenchSection === 'execution'" ref="executionSectionRef" class="mt-5" data-workbench-section="execution">
          <div class="flex items-center gap-2 max-sm:flex-col">
            <article class="flex-1 rounded-xl border p-3" :class="confirmStepVisual.stepCard">
              <div class="flex items-center gap-2 mb-2">
                <!-- 终态用图标替掉序号，和上方阶段轨道、下方构建轨迹同一套写法：颜色之外再给一个信号 -->
                <span class="inline-flex items-center justify-center rounded px-2 py-0.5 font-mono text-xs font-bold"
                  :class="confirmStepVisual.marker">
                  <CheckCircleIcon v-if="confirmStepTone === 'success'" class="h-3.5 w-3.5" aria-hidden="true" />
                  <ExclamationCircleIcon v-else-if="confirmStepTone === 'danger'" class="h-3.5 w-3.5" aria-hidden="true" />
                  <template v-else>01</template>
                </span>
                <span class="text-xs font-semibold text-muted-foreground">{{ confirmStepBadge }}</span>
              </div>
              <strong class="block text-sm text-foreground mb-1">先确认策略方案</strong>
              <p class="text-xs text-muted-foreground mb-2">{{ confirmStepDescription }}</p>
              <Button variant="outline" class="w-full gap-2 rounded-md" :class="confirmStepVisual.button" size="sm" type="button" :disabled="!canConfirmStrategyAction" @click="submitConfirmStrategy"><span>{{ confirmButtonLabel }}</span><CheckCircleIcon class="h-4 w-4" /></Button>
            </article>
            <div class="flex items-center justify-center px-1 max-sm:rotate-90">
              <ArrowRightIcon class="size-5" :class="confirmStepVisual.connector" aria-hidden="true" />
            </div>
            <article class="flex-1 rounded-xl border p-3" :class="buildStepVisual.stepCard">
              <div class="flex items-center gap-2 mb-2">
                <!-- 真正在跑时给转圈，动效比大色块更抓眼且不占静态视觉预算；ready 只是"可执行"，不给转圈 -->
                <span class="inline-flex items-center justify-center rounded px-2 py-0.5 font-mono text-xs font-bold"
                  :class="buildStepVisual.marker">
                  <span v-if="buildStepState === 'current'" class="stage-spinner" aria-hidden="true"></span>
                  <CheckCircleIcon v-else-if="buildStepTone === 'success'" class="h-3.5 w-3.5" aria-hidden="true" />
                  <ExclamationCircleIcon v-else-if="buildStepTone === 'danger'" class="h-3.5 w-3.5" aria-hidden="true" />
                  <template v-else>02</template>
                </span>
                <span class="text-xs font-semibold text-muted-foreground">{{ buildStepBadge }}</span>
              </div>
              <strong class="block text-sm text-foreground mb-1">再执行构建索引</strong>
              <p class="text-xs text-muted-foreground mb-2">{{ buildStepDescription }}</p>
              <Button variant="outline" class="w-full gap-2 rounded-md" :class="buildStepVisual.button" size="sm" type="button" :disabled="!canBuildIndexAction" @click="submitBuildIndex"><span>{{ buildButtonLabel }}</span><ArrowRightIcon class="h-4 w-4" /></Button>
            </article>
          </div>
          <div v-if="showBuildTracker" ref="buildTrackerRef" class="mt-4 rounded-xl border border-border bg-secondary p-4">
            <div class="mb-4 flex items-start justify-between gap-3">
              <div><strong class="block text-sm text-foreground">{{ buildTrackerTitle }}</strong><p class="mt-0.5 text-compact text-[var(--muted-foreground)]">{{ buildTrackerDescription }}</p></div>
              <span class="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold" :class="isBuildPolling?'bg-running/10 text-running':'bg-foreground/[0.06] text-muted-foreground'">{{ isBuildPolling?'实时轮询中':'轨迹已保留' }}</span>
            </div>
            <div class="flex flex-col gap-2">
              <template v-for="(row,rowIndex) in buildStageRows" :key="`build-row-${rowIndex}`">
                <div class="flex items-stretch gap-2">
                  <article v-if="row.leftItem" class="flex flex-1 items-center gap-3 rounded-lg border p-3" :class="buildProgressVisual(row.leftItem.status).card">
                    <div class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-xs font-bold" :class="buildProgressVisual(row.leftItem.status).marker"><span v-if="row.leftItem.status==='current'" class="stage-spinner" aria-hidden="true"></span><CheckCircleIcon v-else-if="['done','completed'].includes(row.leftItem.status)" class="h-4 w-4" /><span v-else>{{ row.leftItem.order }}</span></div>
                    <div><span class="mb-1 inline-flex items-center rounded-md border border-border bg-secondary px-1.5 py-0.5 text-micro font-medium text-muted-foreground">步骤 {{ Number(row.leftItem.order) }}</span><strong class="block text-compact text-foreground">{{ row.leftItem.label }}</strong><em class="mt-0.5 block text-xs not-italic text-muted-foreground">{{ row.leftItem.statusLabel }}</em></div>
                  </article>
                  <div v-else class="flex-1"></div>
                  <div class="flex w-6 items-center justify-center text-xs text-muted-foreground">{{ row.leftItem && row.rightItem ? (row.direction==='rtl'?'←':'→') : '' }}</div>
                  <article v-if="row.rightItem" class="flex flex-1 items-center gap-3 rounded-lg border p-3" :class="buildProgressVisual(row.rightItem.status).card">
                    <div class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-xs font-bold" :class="buildProgressVisual(row.rightItem.status).marker"><span v-if="row.rightItem.status==='current'" class="stage-spinner" aria-hidden="true"></span><CheckCircleIcon v-else-if="['done','completed'].includes(row.rightItem.status)" class="h-4 w-4" /><span v-else>{{ row.rightItem.order }}</span></div>
                    <div><span class="mb-1 inline-flex items-center rounded-md border border-border bg-secondary px-1.5 py-0.5 text-micro font-medium text-muted-foreground">步骤 {{ Number(row.rightItem.order) }}</span><strong class="block text-compact text-foreground">{{ row.rightItem.label }}</strong><em class="mt-0.5 block text-xs not-italic text-muted-foreground">{{ row.rightItem.statusLabel }}</em></div>
                  </article>
                  <div v-else class="flex-1"></div>
                </div>
                <div v-if="rowIndex < buildStageRows.length-1" class="flex" :class="row.downColumn==='right'?'justify-end pr-8':row.downColumn==='left'?'justify-start pl-8':'justify-center'"><span class="text-sm text-muted-foreground">↓</span></div>
              </template>
            </div>
            <div class="mt-4 flex flex-wrap gap-4 border-t border-border pt-3 text-xs text-muted-foreground">
              <span>任务 {{ buildTaskSnapshot?.taskId || activeBuildTaskId || '-' }}</span>
              <span>状态 {{ buildTaskSnapshot?.taskStatusName || (hasCode(documentDetail.indexStatus, 3) ? '成功' : '未知') }}</span>
              <span>耗时 {{ formatDuration(buildTaskSnapshot?.elapsedMillis || buildTaskSnapshot?.costMillis) }}</span>
            </div>
          </div>
        </section>

        <section v-show="activeWorkbenchSection === 'chunk'" ref="chunkSectionRef" class="mt-5" data-workbench-section="chunk">
          <div v-if="chunkLoading" class="py-6 text-center text-sm text-muted-foreground">正在加载 子块 列表...</div>
          <div v-else-if="!chunkRecords.length" class="rounded-md border border-dashed border-border py-6 text-center text-sm text-muted-foreground">当前文档还没有 子块 数据。请先完成索引构建，或等待构建任务继续执行。</div>
          <div v-else>
            <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
              <div class="flex flex-wrap gap-2">
                <div v-for="s in chunkStats" :key="s.label" class="grid gap-1 rounded-lg border border-border bg-secondary px-4 py-3">
                  <span class="text-xs text-muted-foreground">{{ s.label }}</span><strong class="text-sm text-foreground">{{ s.value }}</strong>
                </div>
              </div>
              <div class="flex gap-1 rounded-lg border border-border bg-secondary p-1">
                <Button v-for="m in [{k:'grouped',l:'按父块分组'},{k:'flat',l:'平铺列表'}]" :key="m.k" variant="ghost" size="sm" :class="chunkDisplayMode===m.k?'bg-card text-foreground shadow-sm':'text-muted-foreground hover:text-foreground'" type="button" @click="chunkDisplayMode=m.k">{{ m.l }}</Button>
                <Button v-if="chunkDisplayMode==='grouped'" variant="ghost" size="sm" type="button" @click="setAll子块GroupsCollapsed(false)">展开全部</Button>
                <Button v-if="chunkDisplayMode==='grouped'" variant="ghost" size="sm" type="button" @click="setAll子块GroupsCollapsed(true)">折叠全部</Button>
            </div>
            </div>
            <div v-if="chunkDisplayMode === 'grouped'" class="flex flex-col gap-4">
              <article v-for="group in chunkGroupedRecords" :key="`parent-group-${group.parentBlockId||group.parentBlockNo}`" class="grid gap-0">
                <div class="chunk-group-tone chunk-parent-head flex flex-wrap items-center justify-between gap-3 rounded-lg border px-4 py-3.5"
                  style="background: var(--pl-bg); border-color: var(--pl-border)">
                  <h4 class="m-0 flex min-w-0 flex-wrap items-center gap-2.5 text-sm font-semibold" style="color: var(--pl-fg)">
                    <span class="shrink-0 rounded-md px-2 py-0.5 text-micro font-bold text-white" style="background: var(--pl-solid)">P#{{ group.parentBlockNo || '-' }}</span>
                    <span class="min-w-0 break-words">{{ group.sectionPath || '未识别章节' }}</span>
                    <span class="shrink-0 font-normal opacity-70">{{ group.items.length }} 个子块</span>
                  </h4>
                  <div class="flex flex-wrap items-center gap-2">
                    <Button variant="outline" class="rounded-md border-border bg-card hover:bg-muted" size="lg" type="button" @click="openParentBlockDetail(group)">查看父块上下文</Button>
                    <Button variant="outline" class="rounded-md border-border bg-card hover:bg-muted" size="sm" type="button" @click="toggle子块Group(group.groupKey)">{{ is子块GroupCollapsed(group.groupKey)?'展开子块':'折叠子块' }}</Button>
                  </div>
                </div>
                <template v-if="!is子块GroupCollapsed(group.groupKey)">
                  <div class="chunk-rail">
                    <div class="chunk-rail-item overflow-hidden rounded-lg border border-border">
                      <div class="overflow-x-auto">
                        <table class="w-full min-w-[640px] border-collapse text-sm table-fixed">
                          <caption class="sr-only">按父块分组的文档子块列表</caption>
                          <colgroup>
                            <col style="width:130px">
                            <col style="width:200px">
                            <col style="width:120px">
                            <col style="width:68px">
                            <col style="width:68px">
                            <col>
                            <col style="width:100px">
                          </colgroup>
                          <thead><tr class="bg-secondary">
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">子块</th>
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">章节 / 标识</th>
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">来源 / 状态</th>
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">字符</th>
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">Token</th>
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">内容预览</th>
                            <th scope="col" class="px-4 py-3 text-left text-xs font-semibold text-muted-foreground">操作</th>
                          </tr></thead>
                          <tbody>
                            <tr v-for="item in group.items" :key="`group-row-${item.chunkId}`" class="border-t border-border transition-colors hover:bg-muted">
                              <td class="p-4"><strong class="block text-compact font-semibold text-foreground">#{{ item.chunkNo }}</strong></td>
                              <td class="p-4"><span class="block text-compact text-foreground">{{ item.sectionPath||'未识别章节' }}</span></td>
                              <td class="px-3 py-4"><div class="flex flex-col items-start gap-1.5"><span class="text-xs text-muted-foreground">{{ item.sourceTypeName||'-' }}</span><AdminStatusBadge :label="item.vectorStatusName||'-'" :code="normalizeCode(item.vectorStatus)||'0'" type="vector" /></div></td>
                              <td class="p-4 text-sm font-semibold text-foreground">{{ formatCount(item.charCount) }}</td>
                              <td class="p-4 text-sm font-semibold text-foreground">{{ formatCount(item.tokenCount) }}</td>
                              <td class="p-4"><p class="line-clamp-2 text-compact text-foreground">{{ item.chunkText }}</p></td>
                              <td class="p-4"><Button variant="outline" size="sm" type="button" @click="open子块Detail(item.chunkId)">查看详情</Button></td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                </template>
              </article>
            </div>
            <div v-else class="overflow-x-auto rounded-lg border border-border">
              <table class="w-full min-w-[640px] border-collapse text-sm table-fixed">
                <caption class="sr-only">文档子块列表</caption>
                <colgroup>
                  <col style="width:130px">
                  <col style="width:200px">
                  <col style="width:120px">
                  <col style="width:68px">
                  <col style="width:68px">
                  <col>
                  <col style="width:100px">
                </colgroup>
                <thead><tr class="bg-secondary">
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">子块</th>
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">章节 / 标识</th>
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">来源 / 状态</th>
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">字符</th>
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">Token</th>
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">内容预览</th>
                  <th scope="col" class="border-b border-border px-4 py-3 text-left text-xs font-semibold text-muted-foreground">操作</th>
                </tr></thead>
                <tbody>
                  <tr v-for="item in chunkRecords" :key="item.chunkId" class="border-b border-border transition-colors hover:bg-muted last:border-0">
                    <td class="p-4"><strong class="block text-compact font-semibold text-foreground">#{{ item.chunkNo }}</strong></td>
                    <td class="p-4"><span class="block text-compact text-foreground">{{ item.sectionPath||'未识别章节' }}</span></td>
                    <td class="px-3 py-4"><div class="flex flex-col items-start gap-1.5"><span class="text-xs text-muted-foreground">{{ item.sourceTypeName||'-' }}</span><AdminStatusBadge :label="item.vectorStatusName||'-'" :code="normalizeCode(item.vectorStatus)||'0'" type="vector" /></div></td>
                    <td class="p-4 text-sm font-semibold text-foreground">{{ formatCount(item.charCount) }}</td>
                    <td class="p-4 text-sm font-semibold text-foreground">{{ formatCount(item.tokenCount) }}</td>
                    <td class="p-4"><p class="line-clamp-3 text-compact text-foreground">{{ item.chunkText }}</p></td>
                    <td class="p-4"><Button variant="outline" size="sm" type="button" @click="open子块Detail(item.chunkId)">查看详情</Button></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="mt-4 flex items-center justify-between gap-3 max-sm:flex-col">
              <Button variant="outline" class="rounded-md" size="lg" type="button" :disabled="chunkCurrentPage<=1||chunkLoading" @click="change子块Page(chunkCurrentPage-1)">上一页</Button>
              <div class="flex flex-wrap items-center gap-3 text-sm">
                <label class="flex items-center gap-2 text-muted-foreground">每页显示<Select :model-value="String(chunkCurrentPageSize)" :disabled="chunkLoading" @update:model-value="change子块PageSize">
                  <SelectTrigger class="rounded-md border border-border bg-card px-2 py-1 text-foreground h-auto w-auto inline-flex gap-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      <SelectItem v-for="size in chunkPageSizeOptions" :key="size" :value="String(size)">{{ size }} 条</SelectItem>
                    </SelectGroup>
                  </SelectContent>
                </Select></label>
                <strong class="text-foreground">第 {{ chunkCurrentPage }} / {{ chunkTotalPages }} 页</strong>
                <span class="text-muted-foreground">共 {{ chunkTotalCount }} 条</span>
              </div>
              <Button variant="outline" class="rounded-md" size="lg" type="button" :disabled="chunkCurrentPage>=chunkTotalPages||chunkLoading" @click="change子块Page(chunkCurrentPage+1)">下一页</Button>
            </div>
          </div>
        </section>

        <section v-show="activeWorkbenchSection === 'tasks'" ref="taskSectionRef" class="mt-5" data-workbench-section="tasks">
          <div class="mb-4 flex items-center justify-between gap-3 max-md:flex-col max-md:items-start">
            <h2 class="text-base font-semibold text-foreground">查看任务记录</h2>
            <div class="flex flex-wrap items-center gap-2">
              <span class="inline-flex rounded-full bg-secondary px-3 py-1.5 text-xs font-semibold text-foreground">{{ taskSectionStatusText }}</span>
              <Button variant="outline" size="sm" class="rounded-md border border-border bg-card disabled:opacity-60" type="button" :disabled="!documentDetail.latestTaskId" @click="openLogDrawer">查看完整任务时间线</Button>
            </div>
          </div>
          <div v-if="logLoading" class="py-6 text-center text-sm text-muted-foreground">正在加载任务日志...</div>
          <div v-else-if="!taskLogs.length" class="rounded-md border border-dashed border-border py-6 text-center text-sm text-muted-foreground">当前文档还没有可查看的任务日志。</div>
          <div v-else class="flex flex-col gap-3">
            <article v-for="log in taskLogs.slice(0,3)" :key="log.id" class="rounded-lg border border-border bg-secondary p-3.5">
              <div class="mb-1 flex items-center justify-between gap-2 text-xs"><strong class="text-foreground">{{ log.stageTypeName }} · {{ log.eventTypeName }}</strong><span class="text-muted-foreground">{{ formatDateTime(log.createTime) }}</span></div>
              <p class="text-compact text-[var(--muted-foreground)]">{{ log.content }}</p>
            </article>
          </div>
        </section>
      </div>
    </article>

    <div v-else class="rounded-md border border-dashed border-border py-10 text-center text-sm text-muted-foreground">正在加载文档详情...</div>
  </section>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDownIcon, ArrowLeftIcon, ArrowPathIcon, ArrowRightIcon, CheckCircleIcon, ChevronDownIcon, ChevronUpIcon, ClockIcon, ExclamationCircleIcon, EyeIcon, PlusIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import { APIError, manageApi } from '../../api/api'
import AdminStatusBadge from '../../components/admin/AdminStatusBadge.vue'
import DocumentTaskHistoryDialog from '../../components/admin/DocumentTaskHistoryDialog.vue'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { formatCount, formatDateTime, hasCode, normalizeCode } from '../../utils/manageFormat'
import {
  STRATEGY_LIBRARY,
  STRATEGY_PIPELINE_LIBRARY,
  buildStrategyPreview,
  buildStrategySignature,
  extractPipelineStrategyTypes,
  normalizeStrategyTypeList,
  resolvePlanPipeline
} from '../../utils/documentStrategyPipeline'
import {
  buildIndexRequest,
  buildStrategyConfirmRequest,
  createSubmissionGuard,
  mergeIncrementalLogs,
  resolveWorkflowStepTone
} from '@/features/admin/documentWorkflow'

const route = useRoute()
const router = useRouter()
const OPERATOR_ID = '10001'
const DEFAULT_CHUNK_PAGE_SIZE = 20
const CHUNK_PAGE_SIZE_OPTIONS = [10, 20, 50, 100]
const WORKBENCH_SECTION_KEYS = ['overview', 'strategy', 'execution', 'chunk', 'tasks']

const strategyLibrary = STRATEGY_LIBRARY
const strategyPipelineLibrary = STRATEGY_PIPELINE_LIBRARY
const confirmSubmissionGuard = createSubmissionGuard()
const buildSubmissionGuard = createSubmissionGuard()

const BUILD_STAGE_LIBRARY = [
  { code: '5', order: '01', label: '切块执行', description: '按照当前策略链路生成原始 chunk' },
  { code: '6', order: '02', label: '切块后处理', description: '清洗空块并整理最终可入库片段' },
  { code: '7', order: '03', label: '向量化', description: '生成 embedding 并写入 PGVector' },
  { code: '8', order: '04', label: '入库完成', description: '回写状态并将本次索引标记为可用' }
]

const BUILD_STAGE_CODE_SET = new Set(BUILD_STAGE_LIBRARY.map((item) => item.code))

const documentDetail = ref(null)
const strategyPlan = ref(null)
const selectedParentStrategyTypes = ref([])
const selectedChildStrategyTypes = ref([])
const adjustNote = ref('')
const taskLogs = ref([])
const taskLogSnapshot = ref(null)
const buildTaskSnapshot = ref(null)
const chunkQuery = ref(null)
const chunkDetail = ref(null)
const chunkDisplayMode = ref('grouped')
const chunkGroupCollapsedMap = ref({})
const chunkPageNo = ref(1)
const chunkPageSize = ref(DEFAULT_CHUNK_PAGE_SIZE)
const loading = ref(false)
const planLoading = ref(false)
const confirmLoading = ref(false)
const buildLoading = ref(false)
const logLoading = ref(false)
const chunkLoading = ref(false)
const chunkDetailLoading = ref(false)
const logDrawerOpen = ref(false)
const chunkDetailDrawerOpen = ref(false)
const planPollTimer = ref(null)
const buildPollTimer = ref(null)
const buildProgressLatestLogId = ref(null)
const buildProgressNoticeStage = ref(null)
const buildTrackerRef = ref(null)
const parentBlockSectionRef = ref(null)
const overviewSectionRef = ref(null)
const strategySectionRef = ref(null)
const executionSectionRef = ref(null)
const chunkSectionRef = ref(null)
const taskSectionRef = ref(null)
const chunkDetailFocusMode = ref('chunk')
const activeWorkbenchSection = ref('overview')
const pageNotice = reactive({
  type: 'info',
  message: ''
})

const documentId = computed(() => String(route.params.documentId || ''))
const showOriginalFileName = computed(() => {
  const documentName = String(documentDetail.value?.documentName || '').trim()
  const originalFileName = String(documentDetail.value?.originalFileName || '').trim()
  return Boolean(originalFileName) && originalFileName !== documentName
})
const isBuildPolling = computed(() => buildPollTimer.value != null)
const selectedParentStrategyPreview = computed(() => buildStrategyPreview(selectedParentStrategyTypes.value, strategyLibrary))
const selectedChildStrategyPreview = computed(() => buildStrategyPreview(selectedChildStrategyTypes.value, strategyLibrary))
const selectedParentStrategyRows = computed(() => buildSequenceRows(selectedParentStrategyPreview.value))
const selectedChildStrategyRows = computed(() => buildSequenceRows(selectedChildStrategyPreview.value))
const confirmedParentStrategyTypes = computed(() => extractPipelineStrategyTypes(strategyPlan.value?.plan, 'parent', strategyLibrary))
const confirmedChildStrategyTypes = computed(() => extractPipelineStrategyTypes(strategyPlan.value?.plan, 'child', strategyLibrary))
const chunkRecords = computed(() => Array.isArray(chunkQuery.value?.records) ? chunkQuery.value.records : [])
const chunkTotalCount = computed(() => Number(chunkQuery.value?.total || chunkRecords.value.length || 0))
const chunkCurrentPage = computed(() => Number(chunkQuery.value?.pageNo || chunkPageNo.value || 1))
const chunkCurrentPageSize = computed(() => Number(chunkQuery.value?.pageSize || chunkPageSize.value || DEFAULT_CHUNK_PAGE_SIZE))
const chunkPageSizeOptions = computed(() => {
  return Array.from(new Set([...CHUNK_PAGE_SIZE_OPTIONS, chunkCurrentPageSize.value]))
    .sort((left, right) => left - right)
})
const chunkTotalPages = computed(() => {
  return Math.max(1, Math.ceil(chunkTotalCount.value / Math.max(1, chunkCurrentPageSize.value)))
})
const chunkParentCount = computed(() => {
  return new Set(
    chunkRecords.value
      .map((item) => normalizeCode(item.parentBlockId))
      .filter(Boolean)
  ).size
})
const chunkVectorReadyCount = computed(() => {
  return chunkRecords.value.filter((item) => normalizeCode(item.vectorStatus) === '3').length
})
const chunkVectorPendingCount = computed(() => {
  return chunkRecords.value.filter((item) => normalizeCode(item.vectorStatus) !== '3').length
})
const chunkAverageTokens = computed(() => {
  if (!chunkRecords.value.length) {
    return 0
  }

  const totalTokens = chunkRecords.value.reduce((sum, item) => sum + Number(item.tokenCount || 0), 0)
  return Math.round(totalTokens / chunkRecords.value.length)
})
const chunkGroupedRecords = computed(() => {
  const groupMap = new Map()
  chunkRecords.value.forEach((item) => {
    const parentKey = normalizeCode(item.parentBlockId) || `unbound-${normalizeCode(item.chunkId)}`
    if (!groupMap.has(parentKey)) {
      groupMap.set(parentKey, {
        parentBlockId: item.parentBlockId,
        parentBlockNo: item.parentBlockNo,
        parentChildCount: item.parentChildCount,
        parentStart子块No: item.parentStart子块No,
        parentEnd子块No: item.parentEnd子块No,
        sectionPath: item.sectionPath,
        items: []
      })
    }
    groupMap.get(parentKey).items.push(item)
  })
  return Array.from(groupMap.values())
    .map((group) => ({
      ...group,
      groupKey: normalizeCode(group.parentBlockId) || `unbound-${normalizeCode(group.items[0]?.chunkId)}`,
      items: [...group.items].sort((left, right) => Number(left.chunkNo || 0) - Number(right.chunkNo || 0))
    }))
    .sort((left, right) => Number(left.parentBlockNo || 0) - Number(right.parentBlockNo || 0))
})
const hasBuildTaskSnapshot = computed(() => hasCode(buildTaskSnapshot.value?.taskType, 2))
const activeBuildTaskId = computed(() => {
  if (hasBuildTaskSnapshot.value && buildTaskSnapshot.value?.taskId) {
    return buildTaskSnapshot.value.taskId
  }
  if (hasCode(documentDetail.value?.latestTaskType, 2)) {
    return documentDetail.value?.latestTaskId || ''
  }
  return documentDetail.value?.lastIndexTaskId || ''
})
const hasSelectedStrategy = computed(() => selectedParentStrategyPreview.value.length > 0 && selectedChildStrategyPreview.value.length > 0)
const hasConfirmedStrategy = computed(() => Boolean(documentDetail.value?.currentPlanId) && hasCode(documentDetail.value?.strategyStatus, 3))
const hasUnconfirmedStrategyChanges = computed(() => {
  return buildStrategySignature(selectedParentStrategyTypes.value, strategyLibrary) !== buildStrategySignature(confirmedParentStrategyTypes.value, strategyLibrary)
    || buildStrategySignature(selectedChildStrategyTypes.value, strategyLibrary) !== buildStrategySignature(confirmedChildStrategyTypes.value, strategyLibrary)
    || Boolean(adjustNote.value.trim())
})
const hasBuildInFlightStatus = computed(() => {
  const taskStatus = normalizeCode(buildTaskSnapshot.value?.taskStatus)
  return buildLoading.value
    || taskStatus === '1'
    || taskStatus === '2'
    || hasCode(documentDetail.value?.indexStatus, 2)
    || (hasCode(documentDetail.value?.latestTaskType, 2) && ['1', '2'].includes(normalizeCode(documentDetail.value?.latestTaskStatus)))
})
const showBuildBlockingOverlay = computed(() => hasBuildInFlightStatus.value)

const showBuildTracker = computed(() => {
  return Boolean(activeBuildTaskId.value) || hasBuildTaskSnapshot.value
})

const activeBuildStageLabel = computed(() => {
  const currentStageItem = buildStageItems.value.find((item) => item.status === 'current')
  if (currentStageItem) {
    return currentStageItem.label
  }
  if (hasBuildInFlightStatus.value) {
    return buildTaskSnapshot.value?.currentStageName || BUILD_STAGE_LIBRARY[0].label
  }
  if ((hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 3)) || hasCode(documentDetail.value?.indexStatus, 3)) {
    return BUILD_STAGE_LIBRARY[BUILD_STAGE_LIBRARY.length - 1]?.label || '入库完成'
  }
  return ''
})

const canConfirmStrategyAction = computed(() => {
  return hasSelectedStrategy.value
    && !confirmLoading.value
    && !hasBuildInFlightStatus.value
    && (!hasConfirmedStrategy.value || hasUnconfirmedStrategyChanges.value)
})

const canBuildIndexAction = computed(() => {
  return hasSelectedStrategy.value
    && hasConfirmedStrategy.value
    && !hasUnconfirmedStrategyChanges.value
    && !hasBuildInFlightStatus.value
})

const confirmStepState = computed(() => {
  if (confirmLoading.value) {
    return 'current'
  }
  if (!hasSelectedStrategy.value) {
    return 'locked'
  }
  if (hasConfirmedStrategy.value && !hasUnconfirmedStrategyChanges.value) {
    return 'completed'
  }
  return 'ready'
})

const buildStepState = computed(() => {
  if (buildLoading.value || hasBuildInFlightStatus.value) {
    return 'current'
  }
  if (!hasSelectedStrategy.value || !hasConfirmedStrategy.value || hasUnconfirmedStrategyChanges.value) {
    return 'locked'
  }
  return 'ready'
})

const confirmStepVisual = computed(() => workflowStepVisual('confirm', confirmStepState.value))
const buildStepVisual = computed(() => workflowStepVisual('build', buildStepState.value))
// 模板要按语义分支选图标，而 *StepVisual 只带 class 串，所以单独把 tone 暴露出来。
const confirmStepTone = computed(() => resolveWorkflowStepTone('confirm', confirmStepState.value))
const buildStepTone = computed(() => resolveWorkflowStepTone('build', buildStepState.value))

const strategySystemStages = computed(() => {
  const parseStatus = normalizeCode(documentDetail.value?.parseStatus)
  const parseFailed = parseStatus === '4'
  const parseReady = parseStatus === '3'
  const parentReady = Boolean(resolvePlanPipeline(strategyPlan.value?.plan, 'parent')?.steps?.length)
  const childReady = Boolean(resolvePlanPipeline(strategyPlan.value?.plan, 'child')?.steps?.length)
  const confirmed = hasConfirmedStrategy.value

  return [
    {
      code: 'parse',
      order: '01',
      label: '解析完成',
      description: parseFailed ? '解析失败，无法继续推荐。' : parseReady ? '文本已解析，可进入推荐阶段。' : '正在等待文档解析结果。',
      status: parseFailed ? 'failed' : parseReady ? 'completed' : 'pending'
    },
    {
      code: 'parent',
      order: '02',
      label: '父流水线生成',
      description: parentReady ? '系统已生成回答阶段父块边界。' : parseReady ? '正在生成父块推荐。' : '等待解析完成后生成。',
      status: parseFailed ? 'failed' : parentReady ? 'completed' : parseReady ? 'current' : 'pending'
    },
    {
      code: 'child',
      order: '03',
      label: '子流水线生成',
      description: childReady ? '系统已生成检索阶段子块边界。' : parentReady ? '正在生成子块推荐。' : '等待父流水线准备完成。',
      status: parseFailed ? 'failed' : childReady ? 'completed' : parentReady ? 'current' : 'pending'
    },
    {
      code: 'confirm',
      order: '04',
      label: confirmed ? '方案已确认' : '等待人工确认',
      description: confirmed ? '当前双流水线已成为生效方案。' : childReady ? '系统推荐已完成，请人工确认。' : '待系统完成推荐后再确认。',
      status: parseFailed ? 'failed' : confirmed ? 'completed' : childReady ? 'current' : 'pending'
    }
  ]
})

const confirmStepBadge = computed(() => {
  if (confirmLoading.value) {
    return '确认中'
  }
  if (!hasSelectedStrategy.value) {
    return '请先选择'
  }
  if (hasConfirmedStrategy.value && !hasUnconfirmedStrategyChanges.value) {
    return '已确认'
  }
  if (hasConfirmedStrategy.value && hasUnconfirmedStrategyChanges.value) {
    return '待重新确认'
  }
  return '待确认'
})

const buildStepBadge = computed(() => {
  if (buildLoading.value) {
    return '启动中'
  }
  if (hasBuildInFlightStatus.value) {
    return activeBuildStageLabel.value || '执行中'
  }
  if (!hasSelectedStrategy.value || !hasConfirmedStrategy.value) {
    return '已锁定'
  }
  if (hasUnconfirmedStrategyChanges.value) {
    return '待重新确认'
  }
  return hasCode(documentDetail.value?.indexStatus, 3) ? '可再次执行' : '已解锁'
})

const confirmStepDescription = computed(() => {
  if (!hasSelectedStrategy.value) {
    return '请先分别完成父块流水线和子块流水线配置，再提交这次最终执行方案。'
  }
  if (hasConfirmedStrategy.value && !hasUnconfirmedStrategyChanges.value) {
    return '当前双流水线已经确认完成，这一版方案可以直接用于后续索引构建。'
  }
  if (hasConfirmedStrategy.value && hasUnconfirmedStrategyChanges.value) {
    return '你刚刚调整了父块/子块流水线顺序或补充说明，需要重新确认后才会真正生效。'
  }
  return '推荐双流水线已经生成，请先确认当前方案，再继续执行索引构建。'
})

const buildStepDescription = computed(() => {
  if (buildLoading.value) {
    return '系统正在创建索引构建任务，并同步最新阶段轨迹，请稍候。'
  }
  if (hasBuildInFlightStatus.value) {
    return `当前执行到「${activeBuildStageLabel.value || '索引构建中'}」，页面已暂时锁定并会实时刷新步骤进度。`
  }
  if (!hasSelectedStrategy.value) {
    return '当前还没有完整的父块 / 子块流水线，请先从上方补齐两条流水线。'
  }
  if (!hasConfirmedStrategy.value) {
    return '这里会保持锁定，直到你先完成上一步“确认策略方案”。'
  }
  if (hasUnconfirmedStrategyChanges.value) {
    return '当前有未确认的双流水线调整，请先重新确认方案，再执行索引构建。'
  }
  if (hasCode(documentDetail.value?.indexStatus, 3)) {
    return '最近一次构建已经完成；如果方案没变，这里也支持你再次发起构建。'
  }
  return '确认完成后可直接点击，构建进度会显示在下方，无需再往上查找。'
})

const confirmButtonLabel = computed(() => {
  if (confirmLoading.value) {
    return '确认中...'
  }
  if (hasConfirmedStrategy.value && !hasUnconfirmedStrategyChanges.value) {
    return '策略方案已确认'
  }
  if (hasConfirmedStrategy.value && hasUnconfirmedStrategyChanges.value) {
    return '重新确认策略方案'
  }
  return '确认策略方案'
})

const buildButtonLabel = computed(() => {
  if (buildLoading.value) {
    return '构建启动中...'
  }
  if (hasBuildInFlightStatus.value) {
    return '索引构建执行中'
  }
  if (!hasConfirmedStrategy.value) {
    return '先确认策略方案'
  }
  if (hasUnconfirmedStrategyChanges.value) {
    return '请先重新确认'
  }
  return '构建索引执行'
})

const workflowCurrentPhase = computed(() => {
  if (documentDetail.value?.parseErrorMsg || hasCode(documentDetail.value?.parseStatus, 4)) {
    return {
      tone: 'danger',
      shortLabel: '需处理',
      title: '文档解析失败',
      description: documentDetail.value?.parseErrorMsg || '请先排查解析异常，再继续后续推荐与构建流程。'
    }
  }
  if (!hasCode(documentDetail.value?.parseStatus, 3)) {
    return {
      tone: 'neutral',
      shortLabel: '待解析',
      title: '等待文档解析',
      description: '文档刚进入处理流程，当前先等待解析完成并生成可用文本。'
    }
  }
  if (!strategyPlan.value?.planReady) {
    return {
      tone: 'primary',
      shortLabel: '待推荐',
      title: '等待策略推荐',
      description: '解析已完成，系统正在准备父块与子块的推荐策略。'
    }
  }
  if (hasBuildInFlightStatus.value) {
    return {
      tone: 'running',
      shortLabel: '执行中',
      title: `正在${activeBuildStageLabel.value || '构建索引'}`,
      description: '索引构建正在执行，页面会持续刷新阶段轨迹与任务状态。'
    }
  }
  if (!hasConfirmedStrategy.value) {
    return {
      tone: 'warning',
      shortLabel: '待确认',
      title: '等待确认策略',
      description: '推荐方案已经生成，请先确认父块与子块的最终执行链路。'
    }
  }
  if (hasUnconfirmedStrategyChanges.value) {
    return {
      tone: 'warning',
      shortLabel: '待重确认',
      title: '存在未确认调整',
      description: '你已经修改过双流水线，需要重新确认后才能继续构建。'
    }
  }
  if (hasCode(documentDetail.value?.indexStatus, 3)) {
    return {
      tone: 'success',
      shortLabel: '已完成',
      title: '索引已可用',
      description: '最近一次索引构建已经完成，可以开始验证分块和回看任务记录。'
    }
  }
  return {
    tone: 'neutral',
    shortLabel: '待构建',
    title: '准备执行构建',
    description: '策略方案已确认完成，下一步可以直接发起索引构建。'
  }
})

const workflowNextAction = computed(() => {
  if (documentDetail.value?.parseErrorMsg || hasCode(documentDetail.value?.parseStatus, 4)) {
    return {
      title: '先查看错误并修正文档',
      description: '建议先检查解析错误和最近任务日志，解决异常后再继续后续流程。'
    }
  }
  if (!hasCode(documentDetail.value?.parseStatus, 3)) {
    return {
      title: '等待解析完成',
      description: '当前还不需要人工操作，解析完成后刷新页面查看策略推荐结果。'
    }
  }
  if (!strategyPlan.value?.planReady) {
    return {
      title: '刷新并查看系统推荐',
      description: '解析完成后系统会生成父块与子块推荐策略，先阅读推荐再做人工调整。'
    }
  }
  if (!hasSelectedStrategy.value) {
    return {
      title: '补齐双流水线配置',
      description: '请分别为父块回答链路和子块召回链路至少选择一个策略。'
    }
  }
  if (!hasConfirmedStrategy.value || hasUnconfirmedStrategyChanges.value) {
    return {
      title: '前往确认策略方案',
      description: '先完成当前双流水线方案确认，再启动索引构建。'
    }
  }
  if (hasBuildInFlightStatus.value) {
    return {
      title: '观察构建执行轨迹',
      description: '构建已经开始，重点关注下方阶段轨迹与任务状态变化。'
    }
  }
  if (!hasCode(documentDetail.value?.indexStatus, 3)) {
    return {
      title: '执行构建索引',
      description: '当前方案已确认，下一步就是进入执行区启动索引构建。'
    }
  }
  return {
    title: '验证分块与任务记录',
    description: '索引已经可用，建议先检查分块效果，再复盘任务时间线。'
  }
})

// Single primary action for the overview action band. Always resolves to a safe navigation
// target (open a dialog or switch to the owning section); the actual confirm/build guards still
// live in their own sections. label follows the same state ladder as workflowCurrentPhase.
const overviewPrimaryAction = computed(() => {
  if (documentDetail.value?.parseErrorMsg || hasCode(documentDetail.value?.parseStatus, 4)) {
    return { label: '查看任务记录', target: 'tasks' }
  }
  if (!hasCode(documentDetail.value?.parseStatus, 3)) {
    return { label: '刷新详情', action: 'refresh' }
  }
  if (!strategyPlan.value?.planReady || !hasSelectedStrategy.value) {
    return { label: '配置策略', target: 'strategy' }
  }
  if (!hasConfirmedStrategy.value || hasUnconfirmedStrategyChanges.value) {
    return { label: '确认并构建', target: 'execution' }
  }
  if (hasBuildInFlightStatus.value) {
    return { label: '查看构建进度', target: 'execution' }
  }
  if (!hasCode(documentDetail.value?.indexStatus, 3)) {
    return { label: '执行构建索引', target: 'execution' }
  }
  return { label: '验证分块结果', target: 'chunk' }
})

function runOverviewPrimaryAction() {
  const action = overviewPrimaryAction.value
  if (action.action === 'refresh') {
    loadAll()
    return
  }
  if (action.target) {
    scrollToWorkbenchSection(action.target)
  }
}

const workflowSteps = computed(() => {
  const d = documentDetail.value || {}
  const parseFailed = Boolean(d.parseErrorMsg) || hasCode(d.parseStatus, 4)
  const parseDone = hasCode(d.parseStatus, 3)
  const planReady = Boolean(strategyPlan.value?.planReady)
  const confirmed = hasConfirmedStrategy.value && !hasUnconfirmedStrategyChanges.value
  const building = hasBuildInFlightStatus.value
  const indexDone = hasCode(d.indexStatus, 3)
  const state = (done, current, failed = false) => failed ? 'failed' : done ? 'done' : current ? 'current' : 'pending'
  return [
    { key: 'parse', label: '解析', state: state(parseDone, !parseDone && !parseFailed, parseFailed) },
    { key: 'recommend', label: '策略推荐', state: state(planReady, parseDone && !planReady) },
    { key: 'confirm', label: '确认方案', state: state(confirmed, planReady && !confirmed) },
    { key: 'build', label: '构建索引', state: state(indexDone, building || (confirmed && !indexDone)) },
    // Terminal node becomes the single brand focus once reached, so a fully-done track still has
    // one clear anchor instead of an all-neutral (lifeless) row.
    { key: 'done', label: '可用', state: indexDone ? 'current' : 'pending' }
  ]
})
function stepDotClass(state) {
  if (state === 'done') return 'bg-[var(--status-success-fg)] text-white'
  if (state === 'current') return 'bg-primary text-white ring-4 ring-primary/15'
  if (state === 'failed') return 'bg-destructive text-white'
  return 'bg-foreground/[0.08] text-muted-foreground'
}
function stepLineClass(state) {
  return state === 'done' ? 'bg-[var(--status-success-fg)]/50' : 'bg-border'
}

// Status-track node visual. Done steps read as calm hollow success ticks; the single current
// step is the solid focus (primary fuchsia, or running teal when a build is executing); failed
// is solid danger; pending stays neutral. Colour is never the only signal — a tick / number /
// dot shape and the label below each carry the same state.
function trackNodeVisual(state, running = false) {
  if (state === 'done') {
    // Completed: solid brand fuchsia + white check. Line also turns fuchsia so the whole
    // traversed chain reads as one continuous progress fill.
    return {
      node: 'bg-primary border-2 border-primary text-primary-foreground',
      label: 'text-muted-foreground',
      line: 'bg-primary/50'
    }
  }
  if (state === 'current') {
    if (running) {
      return {
        node: 'border-2 border-[var(--status-running-fg)] bg-[var(--status-running-fg)] text-white ring-4 ring-[var(--status-running-fg)]/15',
        label: 'font-semibold text-[var(--status-running-fg)]',
        line: 'bg-border'
      }
    }
    return {
      node: 'border-2 border-primary bg-primary text-white ring-4 ring-primary/15',
      label: 'font-semibold text-primary',
      line: 'bg-border'
    }
  }
  if (state === 'failed') {
    return {
      node: 'border-2 border-destructive bg-destructive text-white',
      label: 'font-semibold text-destructive',
      line: 'bg-border'
    }
  }
  return {
    node: 'border-2 border-border bg-card text-muted-foreground',
    label: 'text-muted-foreground',
    line: 'bg-border'
  }
}

// Map the strategy system-stage status vocabulary onto the shared track-node states.
// The terminal stage, once completed, becomes the single brand focus (matches the overview
// track's terminal node) instead of fading into the neutral done treatment.
function strategyStageState(status, isTerminal = false) {
  if (status === 'completed') return isTerminal ? 'current' : 'done'
  if (status === 'current') return 'current'
  if (status === 'failed') return 'failed'
  return 'pending'
}

// Key-indicator band. Every value is a real, already-loaded field — no invented metrics.
const documentMetrics = computed(() => {
  const activeParent = hasConfirmedStrategy.value ? confirmedParentStrategyTypes.value : selectedParentStrategyTypes.value
  const activeChild = hasConfirmedStrategy.value ? confirmedChildStrategyTypes.value : selectedChildStrategyTypes.value
  const pipelineReady = strategyPlan.value?.planReady
  const indexReady = hasCode(documentDetail.value?.indexStatus, 3)
  return [
    { key: 'chunks', label: '子块总数', value: formatCount(chunkTotalCount.value), hint: '检索召回片段', tone: indexReady ? 'success' : 'neutral' },
    { key: 'parents', label: '父块数', value: formatCount(chunkParentCount.value), hint: '回答阶段边界', tone: 'neutral' },
    { key: 'samples', label: '任务阶段', value: formatCount(taskLogs.value.length), hint: '普通版任务日志', tone: 'neutral' },
    {
      key: 'pipeline',
      label: '生效策略',
      value: pipelineReady ? `父 ${activeParent.length} · 子 ${activeChild.length}` : '待推荐',
      hint: '父 / 子流水线',
      tone: 'neutral'
    },
    { key: 'tasks', label: '任务日志', value: formatCount(taskLogs.value.length), hint: '构建与操作记录', tone: 'neutral' }
  ]
})

const strategySectionStatusText = computed(() => {
  if (planLoading.value) {
    return '读取中'
  }
  if (documentDetail.value?.parseErrorMsg || hasCode(documentDetail.value?.parseStatus, 4)) {
    return '不可用'
  }
  if (!strategyPlan.value?.planReady) {
    return '待推荐'
  }
  if (!hasSelectedStrategy.value) {
    return '待选择'
  }
  if (hasConfirmedStrategy.value && !hasUnconfirmedStrategyChanges.value) {
    return '已确认'
  }
  if (hasUnconfirmedStrategyChanges.value) {
    return '待重新确认'
  }
  return '可调整'
})

const executionSectionStatusText = computed(() => {
  if (buildLoading.value) {
    return '启动中'
  }
  if (hasBuildInFlightStatus.value) {
    return activeBuildStageLabel.value || '执行中'
  }
  if (!strategyPlan.value?.planReady) {
    return '待策略就绪'
  }
  if (!hasConfirmedStrategy.value) {
    return '待确认'
  }
  if (hasUnconfirmedStrategyChanges.value) {
    return '待重新确认'
  }
  if (hasCode(documentDetail.value?.indexStatus, 3)) {
    return '已完成'
  }
  return '可构建'
})

const chunkSectionStatusText = computed(() => {
  if (chunkLoading.value) {
    return '加载中'
  }
  if (chunkTotalCount.value > 0) {
    return `${chunkTotalCount.value} 条`
  }
  return '暂无数据'
})

const taskSectionStatusText = computed(() => {
  if (logLoading.value) {
    return '读取中'
  }
  if (taskLogs.value.length) {
    return `${taskLogs.value.length} 条日志`
  }
  if (documentDetail.value?.latestTaskId) {
    return '有记录'
  }
  return '暂无任务'
})

const workbenchSections = computed(() => {
  return [
    {
      key: 'overview',
      step: '00',
      label: '文档概览',
      caption: '先看阶段与关键指标',
      status: workflowCurrentPhase.value.shortLabel
    },
    {
      key: 'strategy',
      step: '01',
      label: '配置策略',
      caption: '推荐 + 双流水线调整',
      status: strategySectionStatusText.value
    },
    {
      key: 'execution',
      step: '02',
      label: '确认并构建',
      caption: '确认方案并执行索引',
      status: executionSectionStatusText.value
    },
    {
      key: 'chunk',
      step: '03',
      label: '验证分块结果',
      caption: '检查分块结果与分页',
      status: chunkSectionStatusText.value
    },
    {
      key: 'tasks',
      step: '04',
      label: '查看任务记录',
      caption: '复盘日志与时间线',
      status: taskSectionStatusText.value
    }
  ]
})

const buildTrackerTitle = computed(() => {
  if (!showBuildTracker.value) {
    return ''
  }
  if (hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 4)) {
    return `最近一次构建在「${buildTaskSnapshot.value?.currentStageName || '未知阶段'}」失败`
  }
  if ((hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 3)) || hasCode(documentDetail.value?.indexStatus, 3)) {
    return '最近一次索引构建已完成'
  }
  return `当前阶段：${hasBuildTaskSnapshot.value ? (buildTaskSnapshot.value?.currentStageName || '索引构建中') : '索引构建中'}`
})

const buildTrackerDescription = computed(() => {
  if (!showBuildTracker.value) {
    return ''
  }
  if (hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 4)) {
    return buildTaskSnapshot.value?.errorMsg || '请展开右侧时间线查看失败阶段和具体报错。'
  }
  if ((hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 3)) || hasCode(documentDetail.value?.indexStatus, 3)) {
    return '即使任务执行很快，这里也会保留完整阶段轨迹，方便复盘和教学演示。'
  }
  return '系统正在自动轮询任务状态，阶段完成后会保留已完成轨迹，不会一闪而过。'
})

const buildStageItems = computed(() => {
  const taskStatus = normalizeCode(buildTaskSnapshot.value?.taskStatus)
  const currentStage = normalizeCode(buildTaskSnapshot.value?.currentStage)
  const activeStage = currentStage || (hasBuildInFlightStatus.value ? BUILD_STAGE_LIBRARY[0]?.code || '' : '')
  const logs = Array.isArray(buildTaskSnapshot.value?.logs) ? buildTaskSnapshot.value.logs : []
  const completedStages = new Set()
  const failedStages = new Set()
  const touchedStages = new Set()

  logs.forEach((log) => {
    const stageCode = normalizeCode(log.stageType)
    if (!BUILD_STAGE_CODE_SET.has(stageCode)) {
      return
    }
    touchedStages.add(stageCode)
    if (hasCode(log.eventType, 2)) {
      completedStages.add(stageCode)
    }
    if (hasCode(log.eventType, 3)) {
      failedStages.add(stageCode)
    }
  })

  const currentIndex = BUILD_STAGE_LIBRARY.findIndex((item) => item.code === activeStage)
  return BUILD_STAGE_LIBRARY.map((stage, index) => {
    let status = 'pending'
    let statusLabel = '等待执行'
    if (failedStages.has(stage.code) || (taskStatus === '4' && activeStage === stage.code)) {
      status = 'failed'
      statusLabel = '执行失败'
    }
    else if (taskStatus === '3') {
      status = 'completed'
      statusLabel = '已完成'
    }
    else if ((taskStatus === '1' || taskStatus === '2' || (hasBuildInFlightStatus.value && !currentStage)) && activeStage === stage.code) {
      status = 'current'
      statusLabel = '当前阶段'
    }
    else if (completedStages.has(stage.code) || ((taskStatus === '1' || taskStatus === '2') && currentIndex > index)) {
      status = 'completed'
      statusLabel = '已完成'
    }
    else if (touchedStages.has(stage.code)) {
      status = 'completed'
      statusLabel = '已完成'
    }
    return { ...stage, status, statusLabel }
  })
})

const buildStageRows = computed(() => buildSequenceRows(buildStageItems.value))
const buildOverlayTitle = computed(() => {
  if (buildLoading.value && !activeBuildTaskId.value && !hasBuildTaskSnapshot.value) {
    return '正在发起索引构建任务'
  }
  return activeBuildStageLabel.value ? `当前执行到「${activeBuildStageLabel.value}」` : '索引构建执行中'
})

const buildOverlayDescription = computed(() => {
  if (buildLoading.value && !activeBuildTaskId.value && !hasBuildTaskSnapshot.value) {
    return '系统正在锁定当前确认方案并创建异步任务，稍后会自动进入四个执行阶段。'
  }
  return '构建中的四个阶段会实时刷新，当前步骤会显示转圈提示，完成后自动解除页面锁定。'
})

function showNotice(message, type = 'info') {
  pageNotice.type = type
  pageNotice.message = message
}

function clearNotice() {
  pageNotice.message = ''
}

function asArray(value) {
  return Array.isArray(value) ? value : []
}

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : value
}

function build子块RelationText(chunk) {
  if (!chunk) {
    return '父子关系未知'
  }
  const parentNo = chunk.parentBlockNo || '-'
  const total = Number(chunk.parentChildCount || 0)
  const current子块No = Number(chunk.chunkNo || 0)
  const start子块No = Number(chunk.parentStart子块No || 0)
  if (total > 0 && current子块No > 0 && start子块No > 0) {
    const siblingIndex = current子块No - start子块No + 1
    return `父块 P#${parentNo} · 同父第 ${siblingIndex}/${total} 子块`
  }
  return `父块 P#${parentNo} · 共 ${total || 0} 子块`
}

function isCurrent子块(chunk) {
  return normalizeCode(chunk?.chunkId) === normalizeCode(chunkDetail.value?.chunk?.chunkId)
}

function buildSiblingOrderLabel(index, total) {
  const current = Number(index || 0) + 1
  return `第${current}/${total || 0}子块`
}

function formatChunkCodeList(chunks) {
  const chunkList = Array.isArray(chunks) ? chunks : []
  return chunkList
    .map((item) => `C#${item?.chunkNo || '-'}`)
    .join('、')
}

function format子块CodeList(子块) {
  return formatChunkCodeList(子块)
}

function is子块GroupCollapsed(groupKey) {
  return Boolean(chunkGroupCollapsedMap.value[groupKey])
}

function toggle子块Group(groupKey) {
  chunkGroupCollapsedMap.value = {
    ...chunkGroupCollapsedMap.value,
    [groupKey]: !chunkGroupCollapsedMap.value[groupKey]
  }
}

function setAll子块GroupsCollapsed(collapsed) {
  const nextMap = {}
  chunkGroupedRecords.value.forEach((group) => {
    nextMap[group.groupKey] = collapsed
  })
  chunkGroupCollapsedMap.value = nextMap
}

function scrollToWorkbenchSection(key) {
  activeWorkbenchSection.value = key
}

function goBack() {
  router.push({ name: 'AdminDocuments' })
}

function buildSequenceRows(items) {
  const sourceList = Array.isArray(items) ? items : []
  const rows = []
  for (let index = 0; index < sourceList.length; index += 2) {
    const pair = sourceList.slice(index, index + 2)
    const rowIndex = rows.length
    const direction = rowIndex % 2 === 0 ? 'ltr' : 'rtl'
    const leftItem = direction === 'ltr' ? pair[0] || null : pair[1] || null
    const rightItem = direction === 'ltr' ? pair[1] || null : pair[0] || null
    rows.push({
      direction,
      leftItem,
      rightItem,
      downColumn: direction === 'ltr' ? 'right' : 'left'
    })
  }
  return rows
}

function getSelectedStrategyTypes(pipelineKey) {
  return pipelineKey === 'parent' ? selectedParentStrategyTypes.value : selectedChildStrategyTypes.value
}

function setSelectedStrategyTypes(pipelineKey, nextList) {
  const normalizedList = normalizeStrategyTypeList(nextList, strategyLibrary)
  if (pipelineKey === 'parent') {
    selectedParentStrategyTypes.value = normalizedList
    return
  }
  selectedChildStrategyTypes.value = normalizedList
}

function getSelectedStrategyPreview(pipelineKey) {
  return pipelineKey === 'parent' ? selectedParentStrategyPreview.value : selectedChildStrategyPreview.value
}

function getSelectedStrategyRows(pipelineKey) {
  return pipelineKey === 'parent' ? selectedParentStrategyRows.value : selectedChildStrategyRows.value
}

function toggleStrategy(type, pipelineKey) {
  if (hasBuildInFlightStatus.value) {
    return
  }
  const normalizedType = normalizeCode(type)
  if (!normalizedType) {
    return
  }
  const currentTypes = getSelectedStrategyTypes(pipelineKey)
  if (currentTypes.includes(normalizedType)) {
    setSelectedStrategyTypes(pipelineKey, currentTypes.filter((item) => item !== normalizedType))
    return
  }
  setSelectedStrategyTypes(pipelineKey, [...currentTypes, normalizedType])
}

function moveStrategy(type, direction, pipelineKey) {
  if (hasBuildInFlightStatus.value) {
    return
  }
  const sourceType = normalizeCode(type)
  const orderedTypes = normalizeStrategyTypeList(getSelectedStrategyTypes(pipelineKey), strategyLibrary)
  const sourceIndex = orderedTypes.indexOf(sourceType)
  if (sourceIndex < 0) {
    return
  }
  const targetIndex = sourceIndex + direction
  if (targetIndex < 0 || targetIndex >= orderedTypes.length) {
    return
  }
  const nextList = [...orderedTypes]
  ;[nextList[sourceIndex], nextList[targetIndex]] = [nextList[targetIndex], nextList[sourceIndex]]
  setSelectedStrategyTypes(pipelineKey, nextList)
}

// Selected steps with the system's recommend reason folded in, so the "推荐" readout and the
// editable chain are one list instead of two copies of the same data.
function getPipelineStepRows(pipelineKey) {
  const recommendedSteps = resolvePlanPipeline(strategyPlan.value?.plan, pipelineKey)?.steps || []
  const reasonByType = new Map(recommendedSteps.map((step) => [normalizeCode(step.strategyType), step.recommendReason]))
  const recommendedTypeSet = new Set(recommendedSteps.map((step) => normalizeCode(step.strategyType)))
  return getSelectedStrategyPreview(pipelineKey).map((item, index) => ({
    ...item,
    order: index + 1,
    recommendReason: reasonByType.get(normalizeCode(item.type)) || '',
    recommended: recommendedTypeSet.has(normalizeCode(item.type))
  }))
}

// Palette lists only strategies not already in the chain, removing the duplicate "已选/点击添加"
// grid — a selected strategy lives in the chain above, never twice.
function getPaletteStrategies(pipelineKey) {
  const selected = getSelectedStrategyTypes(pipelineKey)
  return strategyLibrary.filter((item) => !selected.includes(normalizeCode(item.type)))
}

// Whether the current chain already equals the system recommendation (hide restore when identical).
function pipelineMatchesRecommendation(pipelineKey) {
  const recommendedTypes = extractPipelineStrategyTypes(strategyPlan.value?.plan, pipelineKey, strategyLibrary)
  return buildStrategySignature(getSelectedStrategyTypes(pipelineKey), strategyLibrary) === buildStrategySignature(recommendedTypes, strategyLibrary)
}

function restoreRecommendedPipeline(pipelineKey) {
  if (hasBuildInFlightStatus.value) {
    return
  }
  setSelectedStrategyTypes(pipelineKey, extractPipelineStrategyTypes(strategyPlan.value?.plan, pipelineKey, strategyLibrary))
}

function focusBuildTracker() {
  nextTick(() => {
    buildTrackerRef.value?.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
  })
}

async function loadDocumentDetail() {
  documentDetail.value = await manageApi.queryDocumentDetail(documentId.value)
}

async function loadStrategyPlan() {
  planLoading.value = true
  try {
    strategyPlan.value = await manageApi.queryStrategyPlan(documentId.value)
    selectedParentStrategyTypes.value = extractPipelineStrategyTypes(strategyPlan.value?.plan, 'parent', strategyLibrary)
    selectedChildStrategyTypes.value = extractPipelineStrategyTypes(strategyPlan.value?.plan, 'child', strategyLibrary)
    adjustNote.value = ''
  } finally {
    planLoading.value = false
  }
}

async function loadTaskLogs() {
  const latestTaskId = documentDetail.value?.latestTaskId
  if (!latestTaskId) {
    taskLogs.value = []
    taskLogSnapshot.value = null
    return
  }
  logLoading.value = true
  try {
    const data = await manageApi.queryTaskLogs({
      taskId: latestTaskId,
      pageNo: '1',
      pageSize: '30'
    })
    taskLogSnapshot.value = data || null
    taskLogs.value = Array.isArray(data?.logs) ? data.logs : []
  } catch (error) {
    console.error('读取任务日志失败', error)
    taskLogSnapshot.value = null
    taskLogs.value = []
  } finally {
    logLoading.value = false
  }
}

async function loadBuildTaskLogs() {
  const buildTaskId = activeBuildTaskId.value
  if (!buildTaskId) {
    buildTaskSnapshot.value = null
    return
  }
  try {
    const data = await manageApi.queryTaskLogs({
      taskId: buildTaskId,
      pageNo: '1',
      pageSize: '30'
    })
    buildTaskSnapshot.value = data || null
  } catch (error) {
    console.error('读取构建任务日志失败', error)
    buildTaskSnapshot.value = null
  }
}

async function loadBuildProgress(options = {}) {
  return null
}

function applyBuildProgress(data, resetLogs = false) {
  if (!data) {
    return
  }
  const previousTaskStatus = normalizeCode(buildTaskSnapshot.value?.taskStatus)
  const previous = resetLogs ? [] : asArray(buildTaskSnapshot.value?.logs)
  const mergedLogs = mergeTaskLogs(previous, asArray(data.logs))
  buildTaskSnapshot.value = {
    ...buildTaskSnapshot.value,
    ...data,
    logs: mergedLogs
  }
  buildProgressLatestLogId.value = data.latestLogId || latestLogId(mergedLogs) || buildProgressLatestLogId.value || null
  if (hasCode(data.taskStatus, 4)) {
    buildProgressNoticeStage.value = null
    showNotice(data.errorMsg || '索引构建失败，请查看当前任务轨迹里的失败阶段。', 'danger')
  } else if (hasCode(data.taskStatus, 3) && previousTaskStatus !== '3') {
    buildProgressNoticeStage.value = null
  } else if (data.building === true && data.currentStageName && normalizeCode(data.currentStage) !== buildProgressNoticeStage.value) {
    buildProgressNoticeStage.value = normalizeCode(data.currentStage)
    showNotice(`索引构建进行中：${data.currentStageName}`, 'info')
  }
  if (documentDetail.value) {
    documentDetail.value = {
      ...documentDetail.value,
      indexStatus: data.indexStatus ?? documentDetail.value.indexStatus,
      indexStatusName: data.indexStatusName || documentDetail.value.indexStatusName,
      latestTaskId: data.taskId || documentDetail.value.latestTaskId,
      latestTaskType: data.taskType || documentDetail.value.latestTaskType,
      latestTaskTypeName: data.taskTypeName || documentDetail.value.latestTaskTypeName,
      latestTaskStatus: data.taskStatus || documentDetail.value.latestTaskStatus,
      latestTaskStatusName: data.taskStatusName || documentDetail.value.latestTaskStatusName
    }
  }
}

function mergeTaskLogs(previousLogs, incomingLogs) {
  return mergeIncrementalLogs(previousLogs, incomingLogs)
}

function latestLogId(logs) {
  return asArray(logs)
    .map((item) => Number(item?.id || 0))
    .filter((id) => Number.isFinite(id) && id > 0)
    .reduce((max, id) => Math.max(max, id), 0) || null
}

async function loadDocumentChunks(page = chunkCurrentPage.value, options = {}) {
  const {
    resetCollapse = true,
    reset子块Detail = false
  } = options

  chunkLoading.value = true
  try {
    if (reset子块Detail) {
      chunkDetail.value = null
      chunkDetailDrawerOpen.value = false
      chunkDetailFocusMode.value = 'chunk'
    }
    chunkQuery.value = await manageApi.queryDocumentChunks({
      documentId: documentId.value,
      pageNo: page,
      pageSize: chunkPageSize.value
    })
    chunkPageNo.value = Number(chunkQuery.value?.pageNo || page || 1)
    chunkPageSize.value = Number(chunkQuery.value?.pageSize || chunkPageSize.value || DEFAULT_CHUNK_PAGE_SIZE)
    if (resetCollapse) {
      chunkGroupCollapsedMap.value = {}
    }
  } catch (error) {
    console.error('读取 chunk 列表失败', error)
    chunkQuery.value = null
  } finally {
    chunkLoading.value = false
  }
}

function applyRouteWorkbenchFocus() {
  const section = firstQueryValue(route.query?.section)
  if (section && WORKBENCH_SECTION_KEYS.includes(section)) {
    activeWorkbenchSection.value = section
  }
}

function change子块Page(page) {
  if (page < 1 || page > chunkTotalPages.value || page === chunkCurrentPage.value || chunkLoading.value) {
    return
  }
  loadDocumentChunks(page, {
    resetCollapse: true,
    reset子块Detail: true
  })
}

function change子块PageSize(pageSize) {
  const nextPageSize = Number(pageSize || DEFAULT_CHUNK_PAGE_SIZE)
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === chunkCurrentPageSize.value || chunkLoading.value) {
    return
  }
  chunkPageSize.value = nextPageSize
  chunkPageNo.value = 1
  loadDocumentChunks(1, {
    resetCollapse: true,
    reset子块Detail: true
  })
}

async function loadAll() {
  loading.value = true
  clearNotice()
  try {
    await loadDocumentDetail()
    await Promise.all([
      loadStrategyPlan(),
      loadTaskLogs(),
      loadBuildTaskLogs(),
      loadDocumentChunks()
    ])
  } catch (error) {
    console.error('读取文档详情失败', error)
    showNotice(normalizeError(error, '读取文档详情失败'), 'danger')
  } finally {
    loading.value = false
  }
}

async function submitConfirmStrategy() {
  if (!strategyPlan.value?.plan?.planId) {
    showNotice('当前还没有可确认的策略方案。', 'danger')
    return
  }
  if (!hasSelectedStrategy.value) {
    showNotice('请先分别配置父块流水线和子块流水线，再确认当前方案。', 'danger')
    return
  }
  if (hasBuildInFlightStatus.value) {
    showNotice('索引构建执行中，暂时不能修改或确认策略方案。', 'danger')
    return
  }
  if (confirmSubmissionGuard.pending()) {
    showNotice('策略方案正在提交，请等待当前结果。', 'info')
    return
  }

  await confirmSubmissionGuard.run(async () => {
    confirmLoading.value = true
    clearNotice()
    try {
      await manageApi.confirmStrategy(buildStrategyConfirmRequest({
        documentId: documentId.value,
        basePlanId: strategyPlan.value.plan.planId,
        adjustNote: adjustNote.value,
        operatorId: OPERATOR_ID,
        parentTypes: selectedParentStrategyTypes.value,
        childTypes: selectedChildStrategyTypes.value
      }, strategyLibrary))
      showNotice('策略方案已确认，接下来可以直接构建索引。', 'success')
      await loadAll()
    } catch (error) {
      console.error('确认策略失败', error)
      showNotice(normalizeError(error, '确认策略失败'), 'danger')
    } finally {
      confirmLoading.value = false
    }
  })
}

async function submitBuildIndex() {
  if (!hasSelectedStrategy.value) {
    showNotice('请先选择并确认父块 / 子块双流水线，再执行索引构建。', 'danger')
    return
  }
  if (!hasConfirmedStrategy.value || !documentDetail.value?.currentPlanId) {
    showNotice('请先点击“确认策略方案”，确认后才能执行索引构建。', 'danger')
    return
  }
  if (hasUnconfirmedStrategyChanges.value) {
    showNotice('当前双流水线有未确认的改动，请先重新确认方案。', 'danger')
    return
  }
  if (hasBuildInFlightStatus.value) {
    showNotice('索引构建正在执行中，请等待当前任务完成。', 'info')
    return
  }
  if (buildSubmissionGuard.pending()) {
    showNotice('索引构建请求正在提交，请等待当前结果。', 'info')
    return
  }

  await buildSubmissionGuard.run(async () => {
    buildLoading.value = true
    clearNotice()
    try {
      const result = await manageApi.buildIndex(buildIndexRequest({
        documentId: documentId.value,
        currentPlanId: documentDetail.value.currentPlanId,
        confirmed: hasConfirmedStrategy.value,
        dirty: hasUnconfirmedStrategyChanges.value,
        operatorId: OPERATOR_ID
      }))
      showNotice(`索引任务 ${result.taskId} 已创建，系统正在异步构建中。`, 'success')
      await Promise.all([
        loadDocumentDetail(),
        loadTaskLogs(),
        loadDocumentChunks(chunkCurrentPage.value, { resetCollapse: false })
      ])
      focusBuildTracker()
    } catch (error) {
      console.error('构建索引失败', error)
      showNotice(normalizeError(error, '构建索引失败'), 'danger')
    } finally {
      buildLoading.value = false
    }
  })
}

async function open子块Detail(chunkId, focusMode = 'chunk') {
  if (!chunkId) {
    return
  }
  chunkDetailDrawerOpen.value = true
  chunkDetailLoading.value = true
  chunkDetailFocusMode.value = focusMode
  try {
    chunkDetail.value = await manageApi.queryDocumentChunkDetail({
      documentId: documentId.value,
      taskId: chunkQuery.value?.taskId || null,
      chunkId
    })
    if (focusMode === 'parent') {
      await nextTick()
      parentBlockSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  } catch (error) {
    console.error('读取 chunk 详情失败', error)
    showNotice(normalizeError(error, '读取 chunk 详情失败'), 'danger')
    chunkDetail.value = null
  } finally {
    chunkDetailLoading.value = false
  }
}

function openParentBlockDetail(group) {
  if (!group?.items?.length) {
    return
  }
  open子块Detail(group.items[0].chunkId, 'parent')
}

function openLogDrawer() {
  logDrawerOpen.value = true
  loadTaskLogs()
}

function handleLogDialogOpen(open) {
  if (open) {
    logDrawerOpen.value = true
    return
  }
  closeLogDrawer()
}

function handleBuildDialogOpen(open) {
  if (!open && showBuildBlockingOverlay.value) {
    showNotice('构建正在执行，完成前不能关闭进度。', 'info')
  }
}

function closeLogDrawer() {
  logDrawerOpen.value = false
}

function close子块DetailDrawer() {
  chunkDetailDrawerOpen.value = false
  chunkDetailFocusMode.value = 'chunk'
}

function handleChunkDialogOpen(open) {
  if (open) {
    chunkDetailDrawerOpen.value = true
    return
  }
  close子块DetailDrawer()
}

function clearBuildPolling() {
  if (buildPollTimer.value) {
    window.clearInterval(buildPollTimer.value)
    buildPollTimer.value = null
  }
}

function startBuildPolling() {
  clearBuildPolling()
  let pollCount = 0
  buildPollTimer.value = window.setInterval(async () => {
    pollCount += 1
    try {
      await loadAll()
      const building = hasCode(documentDetail.value?.indexStatus, 2)
        || (hasCode(documentDetail.value?.latestTaskType, 2) && ['1', '2'].includes(normalizeCode(documentDetail.value?.latestTaskStatus)))
      if (!building || pollCount >= 30) {
        clearBuildPolling()
      }
    } catch (error) {
      console.error('轮询索引构建状态失败', error)
      clearBuildPolling()
    }
  }, 3000)
}

async function refreshBuildCompletionArtifacts() {
  try {
    await loadDocumentDetail()
    await Promise.all([
      loadTaskLogs(),
      loadDocumentChunks(chunkCurrentPage.value, { resetCollapse: false }),
    ])
  } catch (error) {
    console.error('刷新构建完成后的产物失败', error)
  }
}

function startPlanPolling() {
  if (planPollTimer.value) {
    window.clearInterval(planPollTimer.value)
  }
  let pollCount = 0
  planPollTimer.value = window.setInterval(async () => {
    pollCount += 1
    try {
      await loadDocumentDetail()
      await loadStrategyPlan()
      if (strategyPlan.value?.planReady || normalizeCode(strategyPlan.value?.parseStatus) === '4' || pollCount >= 8) {
        window.clearInterval(planPollTimer.value)
        planPollTimer.value = null
      }
    } catch (error) {
      console.error('轮询策略结果失败', error)
      window.clearInterval(planPollTimer.value)
      planPollTimer.value = null
    }
  }, 2500)
}

function formatDuration(value) {
  const millis = Number(value || 0)
  if (!Number.isFinite(millis) || millis <= 0) {
    return '-'
  }
  if (millis < 1000) {
    return `${millis} ms`
  }
  if (millis < 60_000) {
    return `${(millis / 1000).toFixed(1)} s`
  }
  return `${(millis / 60_000).toFixed(1)} min`
}

function normalizeError(error, fallbackMessage) {
  if (error instanceof APIError && error.message) {
    return error.message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

watch(() => route.params.documentId, async (value, oldValue) => {
  if (!value || value === oldValue) {
    return
  }
  activeWorkbenchSection.value = 'overview'
  chunkPageNo.value = 1
  chunkPageSize.value = DEFAULT_CHUNK_PAGE_SIZE
  chunkGroupCollapsedMap.value = {}
  chunkDetail.value = null
  chunkDetailDrawerOpen.value = false
  chunkDetailFocusMode.value = 'chunk'
  await loadAll()
  applyRouteWorkbenchFocus()
  await nextTick()
})

watch(() => route.query, async () => {
  applyRouteWorkbenchFocus()
}, { deep: true })

watch(documentDetail, (value) => {
  if (!value) {
    clearBuildPolling()
    return
  }
  const building = hasCode(value.indexStatus, 2)
    || (hasCode(value.latestTaskType, 2) && ['1', '2'].includes(normalizeCode(value.latestTaskStatus)))
  if (building && !buildPollTimer.value) {
    startBuildPolling()
    return
  }
  if (!building && buildPollTimer.value) {
    clearBuildPolling()
  }
})

onMounted(async () => {
  await loadAll()
  applyRouteWorkbenchFocus()
  await nextTick()
  if (!strategyPlan.value?.planReady && normalizeCode(strategyPlan.value?.parseStatus) !== '4') {
    startPlanPolling()
  }
})

onBeforeUnmount(() => {
  if (planPollTimer.value) {
    window.clearInterval(planPollTimer.value)
    planPollTimer.value = null
  }
  clearBuildPolling()
})

const chunkTableHeads = ['子块', '章节 / 标识', '来源 / 状态', '字符', 'Token', '内容预览']
const chunkStats = computed(() => [
  { label: '父块数', value: formatCount(chunkParentCount.value) },
  { label: '总片段', value: formatCount(chunkTotalCount.value) },
  { label: '向量可用', value: formatCount(chunkVectorReadyCount.value) },
  { label: '待处理', value: formatCount(chunkVectorPendingCount.value) },
  { label: '平均 Token', value: formatCount(chunkAverageTokens.value) }
])
function noticeClass(type) {
  if (type === 'success') return 'bg-[var(--status-success-fg)]/10 text-[var(--status-success-fg)]'
  if (type === 'warning') return 'bg-[var(--status-waiting-fg)]/10 text-[var(--status-waiting-fg)]'
  if (type === 'danger') return 'bg-[var(--status-danger-fg)]/10 text-[var(--status-danger-fg)]'
  return 'bg-primary/[0.08] text-primary'
}
// Guidance band stays a neutral surface (no tinted fill, no thick left strip). State is carried
// by a small leading dot + the existing text pill — colour as a small signal, not a fill.
function guidanceDotClass(tone) {
  if (tone === 'success') return 'bg-[var(--status-success-fg)]'
  if (tone === 'running') return 'bg-[var(--status-running-fg)]'
  if (tone === 'warning') return 'bg-[var(--status-waiting-fg)]'
  if (tone === 'danger') return 'bg-destructive'
  return 'bg-primary'
}

/*
 * card 和 stepCard 是两种尺度，不能共用一套底色。
 *
 * card 用在构建轨迹的 4 个阶段卡上：一屏里只有 1 个处于 current，面填充是高信号的
 * "就是这一步"，保留。
 *
 * stepCard 用在"先确认策略方案 / 再执行构建索引"这两张大卡上：单张 533x134 约 71000px²，
 * 满填 --status-*-bg 就是一块大色块——面积大、底色浅，噪音高信号低。这里改成中性底 +
 * 带色描边 + 一档阴影：颜色只留在 1px 描边、编号片（满彩度实心）和按钮上，"轮到你了"
 * 由抬升（阴影）而不是由更多颜色表达。已完成/未开始不抬升，不跟当前步抢注意力。
 */
const WORKFLOW_TONE_CLASSES = Object.freeze({
  default: {
    card: 'border-border bg-secondary',
    stepCard: 'border-border bg-secondary',
    marker: 'bg-foreground/[0.08] text-muted-foreground',
    button: 'border-border bg-background text-muted-foreground hover:bg-muted',
    connector: 'text-muted-foreground'
  },
  primary: {
    card: 'border-primary/20 bg-primary/[0.04]',
    stepCard: 'border-primary/30 bg-card shadow-[var(--shadow-glass)]',
    marker: 'bg-primary text-primary-foreground',
    button: 'border-primary/25 bg-primary/[0.08] text-primary hover:bg-primary/[0.12]',
    connector: 'text-primary'
  },
  waiting: {
    card: 'border-border bg-secondary',
    stepCard: 'border-border bg-secondary',
    marker: 'bg-[var(--status-waiting-fg)] text-white',
    button: 'border-[var(--status-waiting-border)] bg-[var(--status-waiting-bg)] text-[var(--status-waiting-fg)] hover:bg-[var(--status-waiting-bg)]',
    connector: 'text-[var(--status-waiting-fg)]'
  },
  running: {
    card: 'border-[var(--status-running-border)] bg-[var(--status-running-bg)]',
    stepCard: 'border-[var(--status-running-border)] bg-card shadow-[var(--shadow-glass)]',
    marker: 'bg-[var(--status-running-fg)] text-white',
    button: 'border-[var(--status-running-border)] bg-[var(--status-running-bg)] text-[var(--status-running-fg)] hover:border-[var(--status-running-fg)]/40 hover:bg-[var(--status-running-bg)]',
    connector: 'text-[var(--status-running-fg)]'
  },
  success: {
    card: 'border-border bg-card',
    stepCard: 'border-border bg-card',
    marker: 'bg-[var(--status-success-fg)] text-white',
    button: 'border-[var(--status-success-border)] bg-[var(--status-success-bg)] text-[var(--status-success-fg)] hover:border-[var(--status-success-fg)]/40 hover:bg-[var(--status-success-bg)]',
    connector: 'text-[var(--status-success-fg)]'
  },
  danger: {
    card: 'border-[var(--status-danger-border)] bg-[var(--status-danger-bg)]',
    stepCard: 'border-[var(--status-danger-border)] bg-card shadow-[var(--shadow-glass)]',
    marker: 'bg-[var(--status-danger-fg)] text-white',
    button: 'border-[var(--status-danger-border)] bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)] hover:bg-[var(--status-danger-bg)]',
    connector: 'text-[var(--status-danger-fg)]'
  }
})

function workflowStepVisual(kind, state) {
  return WORKFLOW_TONE_CLASSES[resolveWorkflowStepTone(kind, state)] || WORKFLOW_TONE_CLASSES.default
}

function buildProgressVisual(status) {
  return workflowStepVisual('build', status)
}
function strategyStatusStepClass(status) {
  if (status === 'done' || status === 'completed') return 'border-[var(--status-success-fg)]/20 bg-[var(--status-success-fg)]/[0.04]'
  if (status === 'current') return 'border-primary/20 bg-primary/[0.04]'
  if (status === 'failed') return 'border-destructive/20 bg-destructive/[0.04]'
  return 'border-border bg-secondary'
}
function chunkChipClass(code) {
  if (code === '2') return 'bg-[var(--status-success-fg)]/10 text-[var(--status-success-fg)]'
  if (code === '3') return 'bg-destructive/10 text-destructive'
  return 'bg-foreground/[0.06] text-muted-foreground'
}
function chunkStatusIcon(code) {
  if (code === '3') return ExclamationCircleIcon
  return ClockIcon
}
</script>

<style scoped>
/* Pipeline classification tone: one scope per lane sets local vars from the single
   backing palette in tailwind.css. Children consume var(--pl-*), never raw blue/amber. */
.chunk-group-tone {
  --pl-bg: var(--chunk-group-bg);
  --pl-fg: var(--chunk-group-fg);
  --pl-border: var(--chunk-group-border);
  --pl-solid: var(--chunk-group-solid);
}
.pipeline-tone-parent {
  --pl-bg: var(--pipeline-parent-bg);
  --pl-fg: var(--pipeline-parent-fg);
  --pl-border: var(--pipeline-parent-border);
  --pl-solid: var(--pipeline-parent-solid);
}
.pipeline-tone-child {
  --pl-bg: var(--pipeline-child-bg);
  --pl-fg: var(--pipeline-child-fg);
  --pl-border: var(--pipeline-child-border);
  --pl-solid: var(--pipeline-child-solid);
}
/* Parent block -> child chunks: config-page rail hierarchy. The parent head is a
   standalone bar; the child table hangs off an indented rail with a connector tick. */
.chunk-rail {
  position: relative;
  display: grid;
  gap: 0.75rem;
  margin-inline-start: 1.25rem;
  padding-block: 0.875rem 0.25rem;
  padding-inline-start: 1.25rem;
  border-inline-start: 1px solid var(--border-strong);
}

.chunk-rail-item {
  position: relative;
}

.chunk-rail-item::before {
  position: absolute;
  top: 1.625rem;
  right: 100%;
  width: 1.25rem;
  height: 1px;
  background: var(--border-strong);
  content: '';
}

@media (min-width: 40rem) {
  .chunk-rail {
    margin-inline-start: 1.75rem;
    padding-inline-start: 1.5rem;
  }

  .chunk-rail-item::before {
    width: 1.5rem;
  }
}

/*
 * 步骤行故意不铺 --pl-bg 面填充。填充面积随策略条数线性增长（两步就占泳道 35%，
 * 四步过半），而底色为了压住正文必须够浅，于是面积大、每像素信号弱，读成"整块是彩色的"。
 * 分类身份改由三个小而满彩度的元素承担：顶部 父块/子块 标签片、每行的实心编号圆
 * （--pl-solid）、推荐 pill 的描边。编号圆沿列纵向重复，两三个就足够让"左品红右琥珀"
 * 成立——这也是父块彩度敢拉到子块 1.7 倍的前提：小面积才吃得住高彩度。
 * 不要"恢复"成 color-mix 面填充。
 */
.pipeline-lane-surface { background: var(--card); }
.pipeline-step-line { background: color-mix(in oklab, var(--pl-border) 70%, var(--pl-bg)); }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.build-mask-fade-enter-active, .build-mask-fade-leave-active { transition: opacity 0.2s ease; }
.build-mask-fade-enter-from, .build-mask-fade-leave-to { opacity: 0; }
@keyframes spin { to { transform: rotate(360deg); } }
.stage-spinner, .build-overlay-spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid currentColor; border-top-color: transparent; border-radius: 50%; animation: spin 0.7s linear infinite; }
.build-overlay-spinner { width: 28px; height: 28px; border-width: 3px; color: var(--status-running-fg); }
@media (prefers-reduced-motion: reduce) {
  .stage-spinner, .build-overlay-spinner { animation: none; }
  .modal-enter-active, .modal-leave-active,
  .build-mask-fade-enter-active, .build-mask-fade-leave-active { transition: none; }
}
</style>
