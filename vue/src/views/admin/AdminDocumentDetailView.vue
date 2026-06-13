<template>
  <section class="document-detail-page">
    <transition name="drawer-fade">
      <div v-if="logDrawerOpen" class="drawer-overlay" @click="closeLogDrawer"></div>
    </transition>
    <transition name="drawer-fade">
      <div v-if="chunkDetailDrawerOpen" class="drawer-overlay" @click="closeChunkDetailDrawer"></div>
    </transition>

    <transition name="build-mask-fade">
      <div v-if="showBuildBlockingOverlay" class="build-overlay">
        <div class="build-overlay-card">
          <div class="build-overlay-head">
            <span class="build-overlay-spinner" aria-hidden="true"></span>
            <div>
              <h3>{{ buildOverlayTitle }}</h3>
              <p class="build-overlay-text">{{ buildOverlayDescription }}</p>
            </div>
          </div>

          <div class="build-overlay-task-meta">
            <span>任务 {{ buildTaskSnapshot?.taskId || activeBuildTaskId || '创建中' }}</span>
            <span>当前阶段 {{ activeBuildStageLabel || '准备启动' }}</span>
          </div>

          <div class="build-overlay-stage-list">
            <article
              v-for="stage in buildStageItems"
              :key="`overlay-stage-${stage.code}`"
              class="build-overlay-stage"
              :class="`build-overlay-stage-${stage.status}`"
            >
              <span class="build-overlay-stage-icon">
                <span v-if="stage.status === 'current'" class="stage-spinner" aria-hidden="true"></span>
                <span v-else>{{ stage.order }}</span>
              </span>
              <div class="build-overlay-stage-copy">
                <strong>{{ stage.label }}</strong>
                <span>{{ stage.statusLabel }}</span>
              </div>
            </article>
          </div>

          <p class="build-overlay-tip">执行期间页面已暂时锁定，避免重复发起构建或误改当前策略链路。</p>
        </div>
      </div>
    </transition>

    <transition name="drawer-slide">
      <aside v-if="logDrawerOpen" class="log-drawer">
        <div class="drawer-header">
          <div>
            <h3>任务执行详情</h3>
            <p class="drawer-subtitle">
              任务 {{ documentDetail?.latestTaskId || '-' }} · {{ documentDetail?.latestTaskTypeName || '暂无任务类型' }}
            </p>
          </div>
          <button class="icon-button" type="button" aria-label="关闭任务执行详情" @click="closeLogDrawer">
            <XMarkIcon class="drawer-icon" />
          </button>
        </div>

        <div class="drawer-summary">
          <div class="summary-chip">
            <span>当前状态</span>
            <AdminStatusBadge
              :label="documentDetail?.latestTaskStatusName || '暂无状态'"
              :code="documentDetail?.latestTaskStatus"
              type="task"
            />
          </div>
          <div class="summary-chip">
            <span>索引状态</span>
            <AdminStatusBadge
              :label="documentDetail?.indexStatusName || '暂无状态'"
              :code="documentDetail?.indexStatus"
              type="index"
            />
          </div>
        </div>

        <div v-if="logLoading" class="drawer-empty">正在加载任务日志...</div>
        <div v-else-if="!taskLogs.length" class="drawer-empty">当前任务还没有日志记录。</div>

        <div v-else class="drawer-timeline">
          <article v-for="log in taskLogs" :key="log.id" class="drawer-log-item">
            <div class="drawer-log-node"></div>
            <div class="drawer-log-body">
              <div class="drawer-log-head">
                <strong>{{ log.stageTypeName }} · {{ log.eventTypeName }}</strong>
                <span>{{ formatDateTime(log.createTime) }}</span>
              </div>
              <p>{{ log.content }}</p>
              <pre v-if="log.detailJson" class="drawer-log-detail">{{ log.detailJson }}</pre>
            </div>
          </article>
        </div>
      </aside>
    </transition>
    <transition name="drawer-slide">
      <aside v-if="chunkDetailDrawerOpen" class="log-drawer chunk-detail-drawer">
        <div class="drawer-header">
          <div>
            <h3>Chunk 详情</h3>
            <p class="drawer-subtitle">
              <template v-if="chunkDetail?.chunk">
                子块 C#{{ chunkDetail.chunk.chunkNo || '-' }} · 父块 P#{{ chunkDetail.parentBlock?.parentBlockNo || '-' }}
              </template>
              <template v-else>
                正在读取切块详情
              </template>
            </p>
          </div>
          <button class="icon-button" type="button" aria-label="关闭 Chunk 详情" @click="closeChunkDetailDrawer">
            <XMarkIcon class="drawer-icon" />
          </button>
        </div>

        <div class="chunk-detail-body">
          <div v-if="chunkDetailLoading" class="drawer-empty">正在加载 chunk 详情...</div>
          <div v-else-if="!chunkDetail?.chunk" class="drawer-empty">当前没有可展示的 chunk 详情。</div>
          <template v-else>
            <div class="drawer-summary">
              <div class="summary-chip summary-chip-child">
                <span>当前子块</span>
                <strong>C#{{ chunkDetail.chunk.chunkNo || '-' }}</strong>
              </div>
              <div class="summary-chip summary-chip-parent">
                <span>所属父块</span>
                <strong>P#{{ chunkDetail.parentBlock?.parentBlockNo || '-' }}</strong>
              </div>
              <div class="summary-chip">
                <span>同父子块</span>
                <strong>{{ chunkDetail.parentBlock?.childCount || chunkDetail.siblingChunks?.length || 0 }}</strong>
              </div>
            </div>

            <section class="chunk-detail-section chunk-detail-section-current">
              <div class="chunk-detail-head">
                <div class="chunk-detail-title-group">
                  <span class="chunk-kind-badge chunk-kind-badge-child">Child Evidence</span>
                  <h4>当前子块 C#{{ chunkDetail.chunk.chunkNo || '-' }}</h4>
                </div>
                <span>{{ buildChunkRelationText(chunkDetail.chunk) }}</span>
              </div>
              <div class="chunk-detail-meta">
                <span>章节：{{ chunkDetail.chunk.sectionPath || '未识别章节' }}</span>
                <span>字符：{{ formatCount(chunkDetail.chunk.charCount) }}</span>
                <span>Token：{{ formatCount(chunkDetail.chunk.tokenCount) }}</span>
              </div>
              <pre class="chunk-detail-text">{{ chunkDetail.chunk.chunkText }}</pre>
            </section>

            <section
              ref="parentBlockSectionRef"
              class="chunk-detail-section chunk-detail-section-parent"
              :class="{ 'chunk-detail-section-focused': chunkDetailFocusMode === 'parent' }"
              v-if="chunkDetail.parentBlock"
            >
              <div class="chunk-detail-head">
                <div class="chunk-detail-title-group">
                  <span class="chunk-kind-badge chunk-kind-badge-parent">Parent Context</span>
                  <h4>所属父块 P#{{ chunkDetail.parentBlock.parentBlockNo || '-' }}</h4>
                </div>
                <span>子块范围 C#{{ chunkDetail.parentBlock.startChunkNo || '-' }} - C#{{ chunkDetail.parentBlock.endChunkNo || '-' }}</span>
              </div>
              <div class="chunk-detail-meta">
                <span>章节：{{ chunkDetail.parentBlock.sectionPath || '未识别章节' }}</span>
                <span>字符：{{ formatCount(chunkDetail.parentBlock.charCount) }}</span>
                <span>Token：{{ formatCount(chunkDetail.parentBlock.tokenCount) }}</span>
              </div>
              <pre class="chunk-detail-text parent-block-text">{{ chunkDetail.parentBlock.parentText }}</pre>
            </section>

            <section class="chunk-detail-section" v-if="Array.isArray(chunkDetail.siblingChunks) && chunkDetail.siblingChunks.length">
              <div class="chunk-detail-head">
                <h4>同父子块关系</h4>
                <span>点击可切换查看其他子块</span>
              </div>
              <div class="chunk-relation-legend">
                <span>父块 P#{{ chunkDetail.parentBlock?.parentBlockNo || '-' }}</span>
                <span>当前命中子块 C#{{ chunkDetail.chunk.chunkNo || '-' }}</span>
                <span>同父共 {{ chunkDetail.siblingChunks.length }} 个子块</span>
              </div>
              <p class="chunk-relation-note">
                当前父块 P#{{ chunkDetail.parentBlock?.parentBlockNo || '-' }} 内包含
                {{ formatChunkCodeList(chunkDetail.siblingChunks) }} 这些子块，当前命中的是
                C#{{ chunkDetail.chunk.chunkNo || '-' }}。
              </p>
              <div class="chunk-relation-track">
                <template v-for="(item, index) in chunkDetail.siblingChunks" :key="`track-${item.chunkId}`">
                  <button
                    class="chunk-relation-node"
                    :class="{ active: isCurrentChunk(item) }"
                    type="button"
                    @click="openChunkDetail(item.chunkId)"
                  >
                    <strong>C#{{ item.chunkNo || '-' }}</strong>
                    <span>{{ buildSiblingOrderLabel(index, chunkDetail.siblingChunks.length) }}</span>
                  </button>
                  <div
                    v-if="index < chunkDetail.siblingChunks.length - 1"
                    class="chunk-relation-line"
                    :class="{ active: isCurrentChunk(item) || isCurrentChunk(chunkDetail.siblingChunks[index + 1]) }"
                  ></div>
                </template>
              </div>
              <div class="sibling-chunk-list">
                <button
                  v-for="item in chunkDetail.siblingChunks"
                  :key="`sibling-${item.chunkId}`"
                  class="sibling-chunk-card"
                  :class="{ active: normalizeCode(item.chunkId) === normalizeCode(chunkDetail.chunk.chunkId) }"
                  type="button"
                  @click="openChunkDetail(item.chunkId)"
                >
                  <div class="sibling-chunk-head">
                    <strong>子块 C#{{ item.chunkNo || '-' }}</strong>
                    <span>{{ buildChunkRelationText(item) }}</span>
                  </div>
                  <p>{{ item.sectionPath || '未识别章节' }}</p>
                  <span>{{ item.chunkText }}</span>
                </button>
              </div>
            </section>
          </template>
        </div>
      </aside>
    </transition>

    <div class="page-top">
      <div class="page-top-main">
        <div class="page-top-breadcrumb">
          <button class="ghost-button page-top-back" type="button" @click="goBack">
            <ArrowLeftIcon class="back-icon" />
            返回文档列表
          </button>
          <span>文档接入</span>
          <span>/</span>
          <strong>文档工作台</strong>
        </div>
        <p class="page-top-caption">围绕单个文档完成策略确认、索引构建、验证 Chunk 结果与任务追踪。</p>
      </div>
      <div class="page-top-actions">
        <button class="ghost-button" type="button" :disabled="loading" @click="loadAll">
          {{ loading ? '刷新中...' : '刷新详情' }}
        </button>
      </div>
    </div>

    <div v-if="pageNotice.message" class="page-notice" :class="`page-notice-${pageNotice.type}`">
      {{ pageNotice.message }}
    </div>

    <article v-if="documentDetail" class="panel-card detail-card">
      <div class="detail-content">
        <nav class="workbench-nav" aria-label="文档工作台章节导航">
          <button
            v-for="item in workbenchSections"
            :key="`workbench-nav-${item.key}`"
            class="workbench-nav-item"
            :class="[{ active: activeWorkbenchSection === item.key }, `workbench-nav-item-${item.key}`]"
            type="button"
            @click="scrollToWorkbenchSection(item.key)"
          >
            <span class="workbench-nav-step">{{ item.step }}</span>
            <span class="workbench-nav-copy">
              <strong>{{ item.label }}</strong>
              <span>{{ item.caption }}</span>
            </span>
            <em>{{ item.status }}</em>
          </button>
        </nav>

        <section v-show="activeWorkbenchSection === 'overview'" ref="overviewSectionRef" class="detail-section workbench-section" data-workbench-section="overview">
          <div class="workbench-section-head">
            <div class="workbench-section-heading">
              <span class="workbench-section-step-badge">Overview</span>
              <h2>文档概览</h2>
              <p>先确认文档状态、关键指标和当前工作焦点，再进入下方流程。</p>
            </div>
            <span class="workbench-section-pill">{{ workflowCurrentPhase.shortLabel }}</span>
          </div>

          <div class="overview-document-card">
            <div class="overview-document-main">
              <p class="overview-document-kicker">Current Document</p>
              <h3>{{ documentDetail.documentName }}</h3>
              <p v-if="showOriginalFileName" class="overview-document-subtitle">{{ documentDetail.originalFileName }}</p>
            </div>
            <div class="overview-document-side">
              <div class="detail-statuses">
                <AdminStatusBadge :label="documentDetail.parseStatusName" :code="documentDetail.parseStatus" type="parse" />
                <AdminStatusBadge :label="documentDetail.strategyStatusName" :code="documentDetail.strategyStatus" type="strategy" />
                <AdminStatusBadge :label="documentDetail.indexStatusName" :code="documentDetail.indexStatus" type="index" />
              </div>
              <span class="overview-document-phase">{{ workflowCurrentPhase.title }}</span>
            </div>
          </div>

          <div class="workspace-guidance-grid overview-guidance-grid">
            <article class="workspace-guidance-card" :class="`workspace-guidance-card-${workflowCurrentPhase.tone}`">
              <span class="workspace-guidance-kicker">当前阶段</span>
              <strong>{{ workflowCurrentPhase.title }}</strong>
              <p>{{ workflowCurrentPhase.description }}</p>
            </article>
            <article class="workspace-guidance-card workspace-guidance-card-next">
              <span class="workspace-guidance-kicker">下一步建议</span>
              <strong>{{ workflowNextAction.title }}</strong>
              <p>{{ workflowNextAction.description }}</p>
            </article>
          </div>

          <article class="workspace-subsection workspace-subsection-compact overview-routes-card">
            <div class="workspace-subsection-header">
              <div>
                <p class="workspace-subsection-kicker">Quick Routes</p>
                <h3>常用入口</h3>
              </div>
            </div>
            <div class="overview-action-row">
              <button class="ghost-button" type="button" @click="scrollToWorkbenchSection('strategy')">
                查看策略配置
              </button>
              <button class="ghost-button" type="button" @click="scrollToWorkbenchSection('execution')">
                前往确认与构建
              </button>
              <button class="ghost-button" type="button" @click="scrollToWorkbenchSection('chunk')">
                检查 Chunk 结果
              </button>
            </div>
          </article>
        </section>

        <section v-show="activeWorkbenchSection === 'strategy'" ref="strategySectionRef" class="detail-section workbench-section" data-workbench-section="strategy">
          <div class="workbench-section-head">
            <div class="workbench-section-heading">
              <span class="workbench-section-step-badge">Step 1</span>
              <h2>配置策略</h2>
              <p>先阅读系统推荐，再分别调整父块和子块流水线，形成最终执行方案。</p>
            </div>
            <span class="workbench-section-pill">{{ strategySectionStatusText }}</span>
          </div>

          <div v-if="documentDetail.parseErrorMsg" class="inline-notice inline-notice-danger">
            {{ documentDetail.parseErrorMsg }}
          </div>

          <div class="strategy-status-bar" v-if="strategySystemStages.length">
            <article
              v-for="item in strategySystemStages"
              :key="`strategy-stage-${item.code}`"
              class="strategy-status-step"
              :class="`strategy-status-step-${item.status}`"
            >
              <div class="strategy-status-index">{{ item.order }}</div>
              <div class="strategy-status-copy">
                <strong>{{ item.label }}</strong>
                <span>{{ item.description }}</span>
              </div>
            </article>
          </div>

          <div v-if="planLoading" class="empty-block compact-empty">正在读取策略详情...</div>
          <div v-else-if="!strategyPlan?.planReady" class="empty-block compact-empty">
            当前文档尚未生成策略方案，解析完成后可点击刷新查看。
          </div>
          <template v-else>
            <div class="strategy-section-shell">
              <div class="strategy-intro">
                <p class="strategy-intro-kicker">Recommendation Summary</p>
                <p class="strategy-intro-copy">
                  {{ strategyPlan.plan?.recommendReason || '系统已生成推荐策略，可以根据业务需要再做补充。' }}
                </p>
              </div>

              <div class="strategy-flow-stack">
                <section
                  v-for="pipeline in strategyPipelineLibrary"
                  :key="`recommended-${pipeline.key}`"
                  class="strategy-lane strategy-lane-recommended"
                  :class="`strategy-lane-${pipeline.key}`"
                >
                  <div class="strategy-lane-header">
                    <div class="strategy-lane-titlebox">
                      <p class="strategy-lane-kicker">{{ pipeline.key === 'parent' ? 'Answer Context Pipeline' : 'Retrieval Recall Pipeline' }}</p>
                      <h5>{{ pipeline.label }}</h5>
                    </div>
                    <p class="strategy-lane-description">{{ pipeline.description }}</p>
                  </div>

                  <div
                    v-if="resolvePlanPipeline(strategyPlan.plan, pipeline.key)?.steps?.length"
                    class="timeline-list"
                    :class="`timeline-list-${pipeline.key}`"
                  >
                    <template
                      v-for="(step, index) in resolvePlanPipeline(strategyPlan.plan, pipeline.key).steps"
                      :key="`${strategyPlan.plan.planId}-${pipeline.key}-${step.stepNo}`"
                    >
                      <article class="timeline-item">
                        <div class="timeline-index">{{ String(step.stepNo).padStart(2, '0') }}</div>
                        <div class="timeline-main">
                          <strong>{{ step.strategyName }}</strong>
                          <p>{{ step.recommendReason || step.strategyRoleName }}</p>
                        </div>
                      </article>
                      <div
                        v-if="index < resolvePlanPipeline(strategyPlan.plan, pipeline.key).steps.length - 1"
                        :key="`${strategyPlan.plan.planId}-${pipeline.key}-${step.stepNo}-arrow`"
                        class="flow-arrow"
                      >
                        <span class="flow-arrow-icon" aria-hidden="true">↓</span>
                      </div>
                    </template>
                  </div>
                  <div v-else class="empty-block compact-empty">
                    当前方案还没有 {{ pipeline.label }} 配置。
                  </div>
                </section>
              </div>

              <div class="strategy-adjust-shell">
                <div class="strategy-adjust-header">
                  <div class="strategy-adjust-titlebox">
                    <p class="strategy-adjust-kicker">Adjustment Workspace</p>
                    <h5>双流水线调整</h5>
                  </div>
                  <p class="strategy-adjust-description">分别配置父块回答流水线和子块召回流水线，并通过上移 / 下移调整顺序。</p>
                </div>

                <div class="strategy-flow-stack strategy-flow-stack-edit">
                  <section
                    v-for="pipeline in strategyPipelineLibrary"
                    :key="`editor-${pipeline.key}`"
                    class="strategy-lane strategy-lane-edit"
                    :class="`strategy-lane-${pipeline.key}`"
                  >
                    <div class="strategy-lane-header">
                      <div class="strategy-lane-titlebox">
                        <p class="strategy-lane-kicker">{{ pipeline.key === 'parent' ? 'Answer Context Pipeline' : 'Retrieval Recall Pipeline' }}</p>
                        <h5>{{ pipeline.label }}</h5>
                      </div>
                      <p class="strategy-lane-description">{{ pipeline.description }}</p>
                    </div>

                    <div class="selected-flow-board" :class="`selected-flow-board-${pipeline.key}`">
                      <span class="selected-flow-label" :class="`selected-flow-label-${pipeline.key}`">当前配置</span>

                      <div v-if="getSelectedStrategyPreview(pipeline.key).length" class="sequence-board selected-flow-sequence">
                        <template v-for="(row, rowIndex) in getSelectedStrategyRows(pipeline.key)" :key="`strategy-row-${pipeline.key}-${rowIndex}`">
                          <div class="sequence-row">
                            <article v-if="row.leftItem" class="selected-flow-card sequence-card">
                              <div class="selected-flow-order">{{ row.leftItem.order }}</div>
                              <div class="selected-flow-content">
                                <strong>{{ row.leftItem.label }}</strong>
                                <span>{{ row.leftItem.description }}</span>
                              </div>
                              <div class="selected-flow-actions">
                                <button class="flow-action-button" type="button" :disabled="row.leftItem.index === 0" @click="moveStrategy(row.leftItem.type, -1, pipeline.key)">
                                  上移
                                </button>
                                <button class="flow-action-button" type="button" :disabled="row.leftItem.index === getSelectedStrategyPreview(pipeline.key).length - 1" @click="moveStrategy(row.leftItem.type, 1, pipeline.key)">
                                  下移
                                </button>
                              </div>
                            </article>
                            <div v-else class="sequence-card-placeholder"></div>

                            <div v-if="row.leftItem && row.rightItem" class="sequence-inline-arrow">{{ row.direction === 'rtl' ? '←' : '→' }}</div>
                            <div v-else class="sequence-inline-arrow sequence-inline-arrow-empty"></div>

                            <article v-if="row.rightItem" class="selected-flow-card sequence-card">
                              <div class="selected-flow-order">{{ row.rightItem.order }}</div>
                              <div class="selected-flow-content">
                                <strong>{{ row.rightItem.label }}</strong>
                                <span>{{ row.rightItem.description }}</span>
                              </div>
                              <div class="selected-flow-actions">
                                <button class="flow-action-button" type="button" :disabled="row.rightItem.index === 0" @click="moveStrategy(row.rightItem.type, -1, pipeline.key)">
                                  上移
                                </button>
                                <button class="flow-action-button" type="button" :disabled="row.rightItem.index === getSelectedStrategyPreview(pipeline.key).length - 1" @click="moveStrategy(row.rightItem.type, 1, pipeline.key)">
                                  下移
                                </button>
                              </div>
                            </article>
                            <div v-else class="sequence-card-placeholder"></div>
                          </div>

                          <div v-if="rowIndex < getSelectedStrategyRows(pipeline.key).length - 1" class="sequence-down-row" :class="`sequence-down-row-${row.downColumn}`">
                            <span class="sequence-down-arrow">↓</span>
                          </div>
                        </template>
                      </div>

                      <div v-else class="selected-flow-empty">
                        {{ pipeline.label }}至少选择一个拆分策略，已选策略会在这里形成清晰的箭头处理链路。
                      </div>
                    </div>

                    <div class="strategy-picker" :class="`strategy-picker-${pipeline.key}`">
                      <button
                        v-for="item in strategyLibrary"
                        :key="`${pipeline.key}-${item.type}`"
                        class="strategy-chip"
                        :class="{ active: getSelectedStrategyTypes(pipeline.key).includes(item.type) }"
                        type="button"
                        @click="toggleStrategy(item.type, pipeline.key)"
                      >
                        <div class="strategy-chip-top">
                          <span class="strategy-chip-state">{{ getSelectedStrategyTypes(pipeline.key).includes(item.type) ? '已选中' : '点击添加' }}</span>
                          <CheckCircleIcon v-if="getSelectedStrategyTypes(pipeline.key).includes(item.type)" class="strategy-chip-check" />
                        </div>
                        <strong>{{ item.label }}</strong>
                        <span>{{ item.description }}</span>
                      </button>
                    </div>

                    <div class="preview-box" :class="`preview-box-${pipeline.key}`">
                      <span class="preview-box-title" :class="`preview-box-title-${pipeline.key}`">{{ pipeline.label }}最终提交顺序</span>
                      <div v-if="getSelectedStrategyPreview(pipeline.key).length" class="preview-flow">
                        <template v-for="(item, index) in getSelectedStrategyPreview(pipeline.key)" :key="`preview-${pipeline.key}-${item.type}`">
                          <span class="preview-tag">{{ item.label }}</span>
                          <ArrowRightIcon v-if="index < getSelectedStrategyPreview(pipeline.key).length - 1" class="preview-arrow" />
                        </template>
                      </div>
                      <p v-else class="preview-empty">还没有选中策略，无法生成当前流水线的最终提交顺序。</p>
                    </div>
                  </section>
                </div>
              </div>
            </div>

          </template>
        </section>

        <section v-show="activeWorkbenchSection === 'execution'" ref="executionSectionRef" class="detail-section workbench-section" data-workbench-section="execution">
          <div class="workbench-section-head">
            <div class="workbench-section-heading">
              <span class="workbench-section-step-badge">Step 2</span>
              <h2>确认并构建</h2>
              <p>先确认策略方案，再执行构建索引，并在同一处查看执行轨迹。</p>
            </div>
            <span class="workbench-section-pill">{{ executionSectionStatusText }}</span>
          </div>

          <div class="execution-summary-grid">
            <article class="execution-summary-card">
              <span>策略确认</span>
              <strong>{{ confirmStepBadge }}</strong>
              <p>{{ hasConfirmedStrategy ? '当前方案已进入确认流程。' : '还未完成最终确认。' }}</p>
            </article>
            <article class="execution-summary-card">
              <span>构建执行</span>
              <strong>{{ buildStepBadge }}</strong>
              <p>{{ hasBuildInFlightStatus ? '系统正在执行构建，请留意下方轨迹。' : '确认完成后即可发起构建。' }}</p>
            </article>
            <article class="execution-summary-card">
              <span>当前任务</span>
              <strong>{{ activeBuildTaskId || documentDetail.latestTaskId || '-' }}</strong>
              <p>{{ activeBuildStageLabel || '当前还没有正在执行的构建任务。' }}</p>
            </article>
          </div>

          <div class="confirm-actions">
            <input v-model="adjustNote" class="adjust-input" type="text" placeholder="补充说明，例如：增加大模型智能切块用于复杂段落" />
            <div class="strategy-submit-actions">
              <article class="action-stage-card" :class="`action-stage-${confirmStepState}`">
                <div class="action-stage-head">
                  <span class="action-stage-index">01</span>
                  <span class="action-stage-badge">{{ confirmStepBadge }}</span>
                </div>
                <strong>先确认策略方案</strong>
                <p>{{ confirmStepDescription }}</p>
                <button
                  class="action-button action-button-confirm"
                  type="button"
                  :disabled="!canConfirmStrategyAction"
                  @click="submitConfirmStrategy"
                >
                  <span>{{ confirmButtonLabel }}</span>
                  <CheckCircleIcon class="action-button-icon" aria-hidden="true" />
                </button>
              </article>

              <article class="action-stage-card" :class="`action-stage-${buildStepState}`">
                <div class="action-stage-head">
                  <span class="action-stage-index">02</span>
                  <span class="action-stage-badge">{{ buildStepBadge }}</span>
                </div>
                <strong>再执行构建索引</strong>
                <p>{{ buildStepDescription }}</p>
                <button
                  class="action-button action-button-build"
                  type="button"
                  :disabled="!canBuildIndexAction"
                  @click="submitBuildIndex"
                >
                  <span>{{ buildButtonLabel }}</span>
                  <ArrowRightIcon class="action-button-icon" aria-hidden="true" />
                </button>
              </article>
            </div>
          </div>

          <div v-if="showBuildTracker" ref="buildTrackerRef" class="build-progress-card build-progress-card-inline">
            <div class="build-progress-header">
              <div>
                <strong>{{ buildTrackerTitle }}</strong>
                <p class="build-progress-text">{{ buildTrackerDescription }}</p>
              </div>
              <span class="build-pulse" :class="{ 'build-pulse-static': !isBuildPolling }">
                {{ isBuildPolling ? '实时轮询中' : '轨迹已保留' }}
              </span>
            </div>

            <div class="sequence-board build-stage-board">
              <template v-for="(row, rowIndex) in buildStageRows" :key="`build-row-${rowIndex}`">
                <div class="sequence-row">
                  <article v-if="row.leftItem" class="stage-card sequence-card" :class="`stage-${row.leftItem.status}`">
                    <div class="stage-order">
                      <span v-if="row.leftItem.status === 'current'" class="stage-spinner" aria-hidden="true"></span>
                      <span v-else>{{ row.leftItem.order }}</span>
                    </div>
                    <div class="stage-body">
                      <strong>{{ row.leftItem.label }}</strong>
                      <span>{{ row.leftItem.description }}</span>
                      <em>{{ row.leftItem.statusLabel }}</em>
                    </div>
                  </article>
                  <div v-else class="sequence-card-placeholder"></div>

                  <div v-if="row.leftItem && row.rightItem" class="sequence-inline-arrow">{{ row.direction === 'rtl' ? '←' : '→' }}</div>
                  <div v-else class="sequence-inline-arrow sequence-inline-arrow-empty"></div>

                  <article v-if="row.rightItem" class="stage-card sequence-card" :class="`stage-${row.rightItem.status}`">
                    <div class="stage-order">
                      <span v-if="row.rightItem.status === 'current'" class="stage-spinner" aria-hidden="true"></span>
                      <span v-else>{{ row.rightItem.order }}</span>
                    </div>
                    <div class="stage-body">
                      <strong>{{ row.rightItem.label }}</strong>
                      <span>{{ row.rightItem.description }}</span>
                      <em>{{ row.rightItem.statusLabel }}</em>
                    </div>
                  </article>
                  <div v-else class="sequence-card-placeholder"></div>
                </div>

                <div v-if="rowIndex < buildStageRows.length - 1" class="sequence-down-row" :class="`sequence-down-row-${row.downColumn}`">
                  <span class="sequence-down-arrow">↓</span>
                </div>
              </template>
            </div>

            <div class="tracker-footer">
              <span>任务 {{ buildTaskSnapshot?.taskId || activeBuildTaskId || '-' }}</span>
              <span>状态 {{ buildTaskSnapshot?.taskStatusName || (hasCode(documentDetail.indexStatus, 3) ? '成功' : '未知') }}</span>
              <span>耗时 {{ formatDuration(buildTaskSnapshot?.costMillis) }}</span>
            </div>
          </div>
        </section>

        <section v-show="activeWorkbenchSection === 'chunk'" ref="chunkSectionRef" class="detail-section workbench-section" data-workbench-section="chunk">
          <div class="workbench-section-head">
            <div class="workbench-section-heading">
              <span class="workbench-section-step-badge">Step 3</span>
              <h2>验证 Chunk 结果</h2>
              <p>在这里检查父子分块结构、分页浏览内容，并抽样验证切块是否符合预期。</p>
            </div>
            <div class="chunk-section-actions">
              <span class="workbench-section-pill">{{ chunkSectionStatusText }}</span>
              <span v-if="chunkQuery?.taskId">任务 {{ chunkQuery.taskId }} · {{ chunkQuery.total || 0 }} 条</span>
              <span v-else>当前还没有可展示的 chunk</span>
              <div v-if="chunkRecords.length" class="chunk-view-switch">
                <button
                  class="chunk-view-button"
                  :class="{ active: chunkDisplayMode === 'grouped' }"
                  type="button"
                  @click="chunkDisplayMode = 'grouped'"
                >
                  按父块分组
                </button>
                <button
                  class="chunk-view-button"
                  :class="{ active: chunkDisplayMode === 'flat' }"
                  type="button"
                  @click="chunkDisplayMode = 'flat'"
                >
                  平铺列表
                </button>
                <button
                  v-if="chunkDisplayMode === 'grouped'"
                  class="chunk-view-button"
                  type="button"
                  @click="setAllChunkGroupsCollapsed(false)"
                >
                  展开全部
                </button>
                <button
                  v-if="chunkDisplayMode === 'grouped'"
                  class="chunk-view-button"
                  type="button"
                  @click="setAllChunkGroupsCollapsed(true)"
                >
                  折叠全部
                </button>
              </div>
            </div>
          </div>

          <div v-if="chunkLoading" class="empty-block compact-empty">正在加载 Chunk 列表...</div>
          <div v-else-if="!chunkRecords.length" class="empty-block compact-empty">
            当前文档还没有 Chunk 数据。请先完成索引构建，或等待构建任务继续执行。
          </div>

          <div v-else class="chunk-table-panel">
            <div class="chunk-toolbar">
              <article class="chunk-stat-card">
                <span>父块数</span>
                <strong>{{ formatCount(chunkParentCount) }}</strong>
              </article>
              <article class="chunk-stat-card">
                <span>总片段</span>
                <strong>{{ formatCount(chunkTotalCount) }}</strong>
              </article>
              <article class="chunk-stat-card">
                <span>向量可用</span>
                <strong>{{ formatCount(chunkVectorReadyCount) }}</strong>
              </article>
              <article class="chunk-stat-card">
                <span>待处理</span>
                <strong>{{ formatCount(chunkVectorPendingCount) }}</strong>
              </article>
              <article class="chunk-stat-card">
                <span>平均 Token</span>
                <strong>{{ formatCount(chunkAverageTokens) }}</strong>
              </article>
            </div>

            <div v-if="chunkDisplayMode === 'grouped'" class="chunk-group-list">
              <article
                v-for="group in chunkGroupedRecords"
                :key="`parent-group-${group.parentBlockId || group.parentBlockNo}`"
                class="chunk-group-card"
                :class="{ collapsed: isChunkGroupCollapsed(group.groupKey) }"
              >
                <div class="chunk-group-head">
                  <div class="chunk-group-head-main">
                    <strong>父块 P#{{ group.parentBlockNo || '-' }}</strong>
                    <p>{{ group.sectionPath || '未识别章节' }}</p>
                  </div>
                  <div class="chunk-group-head-side">
                    <div class="chunk-group-head-actions">
                      <button class="ghost-button chunk-group-detail-button" type="button" @click="openParentBlockDetail(group)">
                        查看父块上下文
                      </button>
                      <button class="ghost-button chunk-group-toggle-button" type="button" @click="toggleChunkGroup(group.groupKey)">
                        {{ isChunkGroupCollapsed(group.groupKey) ? '展开子块' : '折叠子块' }}
                      </button>
                    </div>
                    <div class="chunk-group-meta">
                      <span>子块 {{ group.items.length }}/{{ group.parentChildCount || group.items.length }}</span>
                      <span>子块范围 C#{{ group.parentStartChunkNo || '-' }} - C#{{ group.parentEndChunkNo || '-' }}</span>
                    </div>
                  </div>
                </div>

                <div v-if="!isChunkGroupCollapsed(group.groupKey)" class="chunk-group-track">
                  <button
                    v-for="item in group.items"
                    :key="`group-track-${item.chunkId}`"
                    class="chunk-group-node"
                    type="button"
                    @click="openChunkDetail(item.chunkId)"
                  >
                    <strong>#{{ item.chunkNo || '-' }}</strong>
                    <span>{{ formatCount(item.tokenCount) }} Token</span>
                  </button>
                </div>

                <div v-if="!isChunkGroupCollapsed(group.groupKey)" class="chunk-group-table">
                  <div class="chunk-table-head">
                    <span>Chunk</span>
                    <span>章节 / 标识</span>
                    <span>来源 / 状态</span>
                    <span>字符</span>
                    <span>Token</span>
                    <span>内容预览</span>
                  </div>
                  <article
                    v-for="item in group.items"
                    :key="`group-row-${item.chunkId}`"
                    class="chunk-row chunk-row-clickable"
                    @click="openChunkDetail(item.chunkId)"
                  >
                    <div class="chunk-cell chunk-cell-index" data-label="Chunk">
                      <strong>子块 C#{{ item.chunkNo }}</strong>
                      <span>{{ item.chunkId }}</span>
                      <span class="chunk-relation-hint">{{ buildChunkRelationText(item) }}</span>
                    </div>
                    <div class="chunk-cell chunk-cell-section" data-label="章节 / 标识">
                      <strong>{{ item.sectionPath || '未识别章节' }}</strong>
                      <span>父块 P#{{ item.parentBlockNo || '-' }} · 共 {{ item.parentChildCount || 0 }} 子块</span>
                    </div>
                    <div class="chunk-cell chunk-cell-status" data-label="来源 / 状态">
                      <span class="chunk-chip">{{ item.sourceTypeName || '未知来源' }}</span>
                      <span class="chunk-chip" :class="`chunk-chip-${normalizeCode(item.vectorStatus) || '0'}`">
                        {{ item.vectorStatusName || '未知状态' }}
                      </span>
                    </div>
                    <div class="chunk-cell" data-label="字符">
                      <strong>{{ formatCount(item.charCount) }}</strong>
                    </div>
                    <div class="chunk-cell" data-label="Token">
                      <strong>{{ formatCount(item.tokenCount) }}</strong>
                    </div>
                    <div class="chunk-cell chunk-cell-content" data-label="内容预览">
                      <p class="chunk-body">{{ item.chunkText }}</p>
                    </div>
                  </article>
                </div>
              </article>
            </div>

            <div v-else class="chunk-table">
              <div class="chunk-table-head">
                <span>Chunk</span>
                <span>章节 / 标识</span>
                <span>来源 / 状态</span>
                <span>字符</span>
                <span>Token</span>
                <span>内容预览</span>
              </div>

              <article
                v-for="item in chunkRecords"
                :key="item.chunkId"
                class="chunk-row chunk-row-clickable"
                @click="openChunkDetail(item.chunkId)"
              >
                <div class="chunk-cell chunk-cell-index" data-label="Chunk">
                  <strong>子块 C#{{ item.chunkNo }}</strong>
                  <span>{{ item.chunkId }}</span>
                  <span class="chunk-relation-hint">{{ buildChunkRelationText(item) }}</span>
                </div>
                <div class="chunk-cell chunk-cell-section" data-label="章节 / 标识">
                  <strong>{{ item.sectionPath || '未识别章节' }}</strong>
                  <span>父块 P#{{ item.parentBlockNo || '-' }} · 共 {{ item.parentChildCount || 0 }} 子块</span>
                </div>
                <div class="chunk-cell chunk-cell-status" data-label="来源 / 状态">
                  <span class="chunk-chip">{{ item.sourceTypeName || '未知来源' }}</span>
                  <span class="chunk-chip" :class="`chunk-chip-${normalizeCode(item.vectorStatus) || '0'}`">
                    {{ item.vectorStatusName || '未知状态' }}
                  </span>
                </div>
                <div class="chunk-cell" data-label="字符">
                  <strong>{{ formatCount(item.charCount) }}</strong>
                </div>
                <div class="chunk-cell" data-label="Token">
                  <strong>{{ formatCount(item.tokenCount) }}</strong>
                </div>
                <div class="chunk-cell chunk-cell-content" data-label="内容预览">
                  <p class="chunk-body">{{ item.chunkText }}</p>
                </div>
              </article>
            </div>

            <div class="pagination-bar chunk-pagination-bar">
              <button
                class="ghost-button"
                type="button"
                :disabled="chunkCurrentPage <= 1 || chunkLoading"
                @click="changeChunkPage(chunkCurrentPage - 1)"
              >
                上一页
              </button>
              <div class="pagination-status">
                <label class="page-size-control">
                  <span>每页显示</span>
                  <select
                    class="page-size-select"
                    :value="chunkCurrentPageSize"
                    :disabled="chunkLoading"
                    @change="changeChunkPageSize($event.target.value)"
                  >
                    <option v-for="size in chunkPageSizeOptions" :key="`chunk-page-size-${size}`" :value="size">
                      {{ size }} 条
                    </option>
                  </select>
                </label>
                <strong>第 {{ chunkCurrentPage }} / {{ chunkTotalPages }} 页</strong>
                <span>共 {{ chunkTotalCount }} 条 Chunk，当前页 {{ chunkRecords.length }} 条</span>
              </div>
              <button
                class="ghost-button"
                type="button"
                :disabled="chunkCurrentPage >= chunkTotalPages || chunkLoading"
                @click="changeChunkPage(chunkCurrentPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </section>

        <section v-show="activeWorkbenchSection === 'tasks'" ref="taskSectionRef" class="detail-section workbench-section" data-workbench-section="tasks">
          <div class="workbench-section-head">
            <div class="workbench-section-heading">
              <span class="workbench-section-step-badge">Step 4</span>
              <h2>查看任务记录</h2>
              <p>通过最近任务摘要和完整时间线快速复盘当前文档的执行过程与异常信息。</p>
            </div>
            <div class="task-section-actions">
              <span class="workbench-section-pill">{{ taskSectionStatusText }}</span>
              <button class="ghost-button" type="button" :disabled="!documentDetail.latestTaskId" @click="openLogDrawer">
                查看完整任务时间线
              </button>
            </div>
          </div>

          <div v-if="logLoading" class="empty-block compact-empty">正在加载任务日志...</div>
          <div v-else-if="!taskLogs.length" class="empty-block compact-empty">当前文档还没有可查看的任务日志。</div>

          <div v-else class="summary-log-list">
            <article v-for="log in taskLogs.slice(0, 3)" :key="log.id" class="summary-log-item">
              <div class="summary-log-head">
                <strong>{{ log.stageTypeName }} · {{ log.eventTypeName }}</strong>
                <span>{{ formatDateTime(log.createTime) }}</span>
              </div>
              <p>{{ log.content }}</p>
            </article>
          </div>
        </section>
      </div>
    </article>

    <div v-else class="empty-block">
      正在加载文档详情...
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftIcon, ArrowRightIcon, CheckCircleIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import { APIError, manageApi } from '../../api/api'
import AdminStatusBadge from '../../components/admin/AdminStatusBadge.vue'
import { formatCount, formatDateTime, hasCode, normalizeCode } from '../../utils/manageFormat'
import {
  STRATEGY_LIBRARY,
  STRATEGY_PIPELINE_LIBRARY,
  buildPipelineStepPayload,
  buildStrategyPreview,
  buildStrategySignature,
  extractPipelineStrategyTypes,
  normalizeStrategyTypeList,
  resolvePlanPipeline
} from '../../utils/documentStrategyPipeline'

const route = useRoute()
const router = useRouter()
const OPERATOR_ID = '10001'
const DEFAULT_CHUNK_PAGE_SIZE = 20
const CHUNK_PAGE_SIZE_OPTIONS = [10, 20, 50, 100]
const WORKBENCH_SECTION_KEYS = ['overview', 'strategy', 'execution', 'chunk', 'tasks']

const strategyLibrary = STRATEGY_LIBRARY
const strategyPipelineLibrary = STRATEGY_PIPELINE_LIBRARY

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
        parentStartChunkNo: item.parentStartChunkNo,
        parentEndChunkNo: item.parentEndChunkNo,
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
      tone: 'primary',
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
      description: '最近一次索引构建已经完成，可以开始验证 Chunk 和回看任务记录。'
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
    title: '验证 Chunk 与任务记录',
    description: '索引已经可用，建议先检查分块效果，再复盘任务时间线。'
  }
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
      label: '验证 Chunk 结果',
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

function buildChunkRelationText(chunk) {
  if (!chunk) {
    return '父子关系未知'
  }
  const parentNo = chunk.parentBlockNo || '-'
  const total = Number(chunk.parentChildCount || 0)
  const currentChunkNo = Number(chunk.chunkNo || 0)
  const startChunkNo = Number(chunk.parentStartChunkNo || 0)
  if (total > 0 && currentChunkNo > 0 && startChunkNo > 0) {
    const siblingIndex = currentChunkNo - startChunkNo + 1
    return `父块 P#${parentNo} · 同父第 ${siblingIndex}/${total} 子块`
  }
  return `父块 P#${parentNo} · 共 ${total || 0} 子块`
}

function isCurrentChunk(chunk) {
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

function isChunkGroupCollapsed(groupKey) {
  return Boolean(chunkGroupCollapsedMap.value[groupKey])
}

function toggleChunkGroup(groupKey) {
  chunkGroupCollapsedMap.value = {
    ...chunkGroupCollapsedMap.value,
    [groupKey]: !chunkGroupCollapsedMap.value[groupKey]
  }
}

function setAllChunkGroupsCollapsed(collapsed) {
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

async function loadDocumentChunks(page = chunkCurrentPage.value, options = {}) {
  const {
    resetCollapse = true,
    resetChunkDetail = false
  } = options

  chunkLoading.value = true
  try {
    if (resetChunkDetail) {
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

function changeChunkPage(page) {
  if (page < 1 || page > chunkTotalPages.value || page === chunkCurrentPage.value || chunkLoading.value) {
    return
  }
  loadDocumentChunks(page, {
    resetCollapse: true,
    resetChunkDetail: true
  })
}

function changeChunkPageSize(pageSize) {
  const nextPageSize = Number(pageSize || DEFAULT_CHUNK_PAGE_SIZE)
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === chunkCurrentPageSize.value || chunkLoading.value) {
    return
  }
  chunkPageSize.value = nextPageSize
  chunkPageNo.value = 1
  loadDocumentChunks(1, {
    resetCollapse: true,
    resetChunkDetail: true
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

  confirmLoading.value = true
  clearNotice()
  try {
    await manageApi.confirmStrategy({
      documentId: documentId.value,
      basePlanId: strategyPlan.value.plan.planId,
      adjustNote: adjustNote.value.trim(),
      operatorId: OPERATOR_ID,
      parentSteps: buildPipelineStepPayload(selectedParentStrategyTypes.value, strategyLibrary),
      childSteps: buildPipelineStepPayload(selectedChildStrategyTypes.value, strategyLibrary)
    })
    showNotice('策略方案已确认，接下来可以直接构建索引。', 'success')
    await loadAll()
  } catch (error) {
    console.error('确认策略失败', error)
    showNotice(normalizeError(error, '确认策略失败'), 'danger')
  } finally {
    confirmLoading.value = false
  }
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

  buildLoading.value = true
  clearNotice()
  try {
    const result = await manageApi.buildIndex({
      documentId: documentId.value,
      planId: documentDetail.value.currentPlanId,
      operatorId: OPERATOR_ID
    })
    showNotice(`索引任务 ${result.taskId} 已创建，系统正在异步构建中。`, 'success')
    await loadAll()
    startBuildPolling()
    focusBuildTracker()
  } catch (error) {
    console.error('构建索引失败', error)
    showNotice(normalizeError(error, '构建索引失败'), 'danger')
  } finally {
    buildLoading.value = false
  }
}

async function openChunkDetail(chunkId, focusMode = 'chunk') {
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
  openChunkDetail(group.items[0].chunkId, 'parent')
}

function openLogDrawer() {
  logDrawerOpen.value = true
  loadTaskLogs()
}

function closeLogDrawer() {
  logDrawerOpen.value = false
}

function closeChunkDetailDrawer() {
  chunkDetailDrawerOpen.value = false
  chunkDetailFocusMode.value = 'chunk'
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
  await nextTick()
})

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
</script>

<style scoped>
.document-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.panel-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.page-top-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.page-top-actions {
  display: flex;
  align-items: center;
}

.page-top-breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--color-muted-strong);
  font-size: 13px;
}

.page-top-breadcrumb strong {
  color: var(--color-text-strong);
}

.page-top-back {
  padding: 10px 14px;
}

.page-top-caption {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.workspace-guidance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
}

.workspace-guidance-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
}

.workspace-guidance-card strong {
  color: var(--color-text-strong);
  font-size: 18px;
  line-height: 1.3;
}

.workspace-guidance-card p {
  margin: 0;
  color: #607087;
  font-size: 13px;
  line-height: 1.6;
}

.workspace-guidance-kicker {
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.workspace-guidance-card-primary {
  border-color: rgba(37, 87, 214, 0.14);
  background: rgba(37, 87, 214, 0.04);
}

.workspace-guidance-card-warning {
  border-color: rgba(245, 158, 11, 0.2);
  background: rgba(245, 158, 11, 0.04);
}

.workspace-guidance-card-danger {
  border-color: rgba(179, 76, 47, 0.18);
  background: rgba(179, 76, 47, 0.04);
}

.workspace-guidance-card-success {
  border-color: rgba(21, 115, 91, 0.18);
  background: rgba(21, 115, 91, 0.04);
}

.workspace-guidance-card-neutral {
  background: var(--color-surface-soft);
}

.workspace-shortcut-group {
  margin-top: auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.workbench-nav {
  position: sticky;
  top: 0;
  z-index: 6;
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  overflow-x: auto;
  border-radius: var(--radius-md);
  border: 1px solid #e2e8f0;
  background: #f1f5f9;
  box-shadow: var(--shadow-sm);
}

.workbench-nav-item {
  --nav-accent-rgb: 37, 87, 214;
  --nav-accent-solid: #2557d6;
  --nav-accent-muted: #47627f;
  min-width: 220px;
  flex: 1;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--color-text);
  text-align: left;
}

.workbench-nav-item,
.strategy-chip,
.chunk-group-node,
.sibling-chunk-card,
.flow-action-button,
.icon-button,
.action-button {
  cursor: pointer;
}

.workbench-nav-item-overview {
  --nav-accent-rgb: 45, 74, 160;
  --nav-accent-solid: #2d4aa0;
  --nav-accent-muted: #52668f;
}

.workbench-nav-item-strategy {
  --nav-accent-rgb: 15, 118, 110;
  --nav-accent-solid: #0f766e;
  --nav-accent-muted: #3f6f67;
}

.workbench-nav-item-execution {
  --nav-accent-rgb: 245, 158, 11;
  --nav-accent-solid: #b45309;
  --nav-accent-muted: #996f26;
}

.workbench-nav-item-chunk {
  --nav-accent-rgb: 14, 116, 144;
  --nav-accent-solid: #0f6f85;
  --nav-accent-muted: #3f6f7b;
}

.workbench-nav-item-tasks {
  --nav-accent-rgb: 71, 85, 105;
  --nav-accent-solid: #475569;
  --nav-accent-muted: #5f6e81;
}

.workbench-nav-item:hover {
  border-color: rgba(var(--nav-accent-rgb), 0.2);
  background: rgba(var(--nav-accent-rgb), 0.04);
}

.workbench-nav-item:focus-visible,
.strategy-chip:focus-visible,
.chunk-group-node:focus-visible,
.sibling-chunk-card:focus-visible,
.flow-action-button:focus-visible,
.icon-button:focus-visible,
.action-button:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(37, 87, 214, 0.16),
    0 12px 22px rgba(15, 23, 42, 0.12);
}

.workbench-nav-step {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  background: rgba(var(--nav-accent-rgb), 0.1);
  color: rgb(var(--nav-accent-rgb));
  font-size: 12px;
  font-weight: 700;
}

.workbench-nav-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.workbench-nav-copy strong {
  color: var(--color-text-strong);
  font-size: 13px;
}

.workbench-nav-copy span {
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.5;
}

.workbench-nav-item em {
  padding: 5px 10px;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--color-muted-strong);
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.workbench-nav-item.active {
  border-color: rgb(var(--nav-accent-rgb));
  background: rgba(var(--nav-accent-rgb), 0.2);
  border-bottom: 3px solid rgb(var(--nav-accent-rgb));
}

.workbench-nav-item.active .workbench-nav-step,
.workbench-nav-item.active em {
  background: rgba(var(--nav-accent-rgb), 0.18);
  color: var(--nav-accent-solid);
}

.workbench-nav-item.active .workbench-nav-copy strong {
  color: var(--nav-accent-solid);
}

.workbench-nav-item.active .workbench-nav-copy span {
  color: var(--nav-accent-muted);
}

.workbench-section {
  --section-accent-rgb: 37, 87, 214;
  --section-accent-solid: #2557d6;
  --section-accent-muted: #47627f;
  position: relative;
  overflow: hidden;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.workbench-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgb(var(--section-accent-rgb));
}

.workbench-section[data-workbench-section='overview'] {
  --section-accent-rgb: 45, 74, 160;
  --section-accent-solid: #2d4aa0;
  --section-accent-muted: #52668f;
}

.workbench-section[data-workbench-section='strategy'] {
  --section-accent-rgb: 15, 118, 110;
  --section-accent-solid: #0f766e;
  --section-accent-muted: #3f6f67;
}

.workbench-section[data-workbench-section='execution'] {
  --section-accent-rgb: 245, 158, 11;
  --section-accent-solid: #b45309;
  --section-accent-muted: #996f26;
}

.workbench-section[data-workbench-section='chunk'] {
  --section-accent-rgb: 14, 116, 144;
  --section-accent-solid: #0f6f85;
  --section-accent-muted: #3f6f7b;
}

.workbench-section[data-workbench-section='tasks'] {
  --section-accent-rgb: 71, 85, 105;
  --section-accent-solid: #475569;
  --section-accent-muted: #5f6e81;
}

.workbench-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.workbench-section-heading {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.workbench-section-step-badge {
  width: fit-content;
  padding: 7px 12px;
  border-radius: 4px;
  background: rgba(var(--section-accent-rgb), 0.12);
  color: var(--section-accent-solid);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.workbench-section-heading h2 {
  margin: 0;
  color: #0f2742;
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 900;
  letter-spacing: -0.04em;
  line-height: 1.02;
}

.workbench-section-heading p {
  margin: 0;
  max-width: 660px;
  color: #5b6b7d;
  font-size: 15px;
  line-height: 1.7;
}

.workbench-section-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 4px;
  background: rgba(var(--section-accent-rgb), 0.1);
  color: var(--section-accent-solid);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.workbench-section .workspace-guidance-card,
.workbench-section .workspace-subsection,
.workbench-section .execution-summary-card,
.workbench-section .chunk-stat-card,
.workbench-section .chunk-group-card,
.workbench-section .chunk-table,
.workbench-section .build-progress-card,
.workbench-section .summary-log-item {
  border-color: var(--color-border);
  background: #fff;
  box-shadow: none;
}

.overview-document-card {
  margin-top: 18px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: #fff;
}

.overview-document-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.overview-document-kicker {
  margin: 0;
  color: var(--section-accent-solid);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.overview-document-card h3 {
  margin: 0;
  color: #0f2742;
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 900;
  letter-spacing: -0.04em;
  line-height: 1.02;
}

.overview-document-subtitle {
  margin: 0;
  color: var(--color-muted-strong);
  font-size: 15px;
  line-height: 1.7;
}

.overview-document-side {
  min-width: 280px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.overview-document-phase {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  background: rgba(var(--section-accent-rgb), 0.1);
  color: var(--section-accent-solid);
  font-size: 12px;
  font-weight: 800;
}

.overview-guidance-grid {
  margin-top: 12px;
}

.overview-routes-card {
  margin-top: 12px;
  padding: 16px 18px;
}

.overview-action-row {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.overview-action-row .ghost-button {
  justify-content: center;
  min-height: 48px;
  border-color: var(--color-border);
  background: #fff;
  color: var(--section-accent-solid);
  box-shadow: none;
}

.overview-action-row .ghost-button:hover:not(:disabled) {
  border-color: rgba(var(--section-accent-rgb), 0.3);
  background: rgba(var(--section-accent-rgb), 0.04);
  box-shadow: none;
}

.workspace-subsection {
  padding: 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
}

.workspace-subsection-surface {
  margin-top: 18px;
}

.workspace-subsection-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.workspace-subsection-header-spread {
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.workspace-subsection-kicker {
  margin: 0;
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.workspace-subsection h3 {
  margin: 8px 0 0;
  color: var(--color-text-strong);
  font-size: 26px;
  font-weight: 800;
}

.workspace-subsection-copy {
  margin: 14px 0 0;
  color: #607087;
  font-size: 14px;
  line-height: 1.7;
}

.workspace-subsection-copy-inline {
  margin: 0;
  max-width: 520px;
  text-align: right;
}

.overview-focus-list {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.overview-focus-list span {
  display: inline-flex;
  align-items: center;
  padding: 7px 10px;
  border-radius: 4px;
  background: rgba(37, 87, 214, 0.08);
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.overview-action-stack {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.execution-summary-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.execution-summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
}

.execution-summary-card span {
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.execution-summary-card strong {
  color: var(--color-text-strong);
  font-size: 24px;
  line-height: 1.2;
}

.execution-summary-card p {
  margin: 0;
  color: var(--color-muted-strong);
  font-size: 13px;
  line-height: 1.7;
}

.task-section-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-header,
.detail-statuses,
.build-progress-header,
.detail-secondary-actions,
.section-headline,
.summary-log-head,
.drawer-log-head,
.confirm-actions,
.strategy-chip-top,
.chunk-head,
.chunk-title-group,
.chunk-status-group,
.chunk-meta,
.drawer-summary,
.tracker-footer {
  display: flex;
  align-items: center;
}

.detail-header,
.build-progress-header,
.section-headline,
.chunk-head {
  justify-content: space-between;
  gap: 12px;
}

.section-headline-major {
  padding: 0 0 14px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.section-headline.section-headline-major h4 {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 900;
  letter-spacing: -0.03em;
  color: #0f2742;
  line-height: 1.08;
}

.section-headline.section-headline-major span {
  color: var(--color-muted-strong);
  font-size: 13px;
  font-weight: 700;
}

.section-headline-editor {
  margin-top: 30px;
}

.pipeline-headline {
  margin-top: 18px;
  padding: 6px 0 10px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.section-headline.pipeline-headline h4 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.03em;
  line-height: 1.12;
}

.section-headline.pipeline-headline span {
  font-size: 12px;
  font-weight: 700;
}

.pipeline-headline-parent {
  border-bottom-color: rgba(37, 87, 214, 0.18);
}

.section-headline.pipeline-headline-parent h4,
.section-headline.pipeline-headline-parent span {
  color: var(--color-primary-strong);
}

.pipeline-headline-child {
  border-bottom-color: rgba(15, 118, 110, 0.18);
}

.section-headline.pipeline-headline-child h4,
.section-headline.pipeline-headline-child span {
  color: #12644f;
}

.detail-statuses,
.chunk-title-group,
.chunk-status-group,
.chunk-meta,
.tracker-footer {
  gap: 10px;
  flex-wrap: wrap;
}

.detail-header h3,
.section-headline h4,
.drawer-header h3 {
  margin: 0;
  color: var(--color-text-strong);
}

.detail-header h3 {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
}

.section-headline h4,
.drawer-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.detail-subtitle,
.section-headline span,
.drawer-subtitle,
.summary-log-head span {
  color: var(--color-muted);
}

.detail-secondary-actions,
.confirm-actions {
  gap: 12px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-item,
.reason-card,
.preview-box,
.selected-flow-board {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
}

.meta-item span,
.reason-card span,
.preview-box span,
.selected-flow-label {
  display: block;
  font-size: 12px;
  color: var(--color-muted);
}

.meta-item strong {
  display: block;
  margin-top: 10px;
  color: var(--color-text-strong);
}

.detail-section {
  padding-top: 4px;
}

.strategy-status-bar {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.strategy-status-step {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: #fff;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.strategy-status-index {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-text);
  font-size: 13px;
  font-weight: 900;
}

.strategy-status-copy strong {
  display: block;
  color: var(--color-text-strong);
  font-size: 14px;
  font-weight: 800;
}

.strategy-status-copy span {
  display: block;
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.55;
}

.strategy-status-step-completed {
  border-color: rgba(15, 118, 110, 0.16);
  background: rgba(15, 118, 110, 0.04);
}

.strategy-status-step-completed .strategy-status-index {
  background: rgba(15, 118, 110, 0.14);
  color: #12644f;
}

.strategy-status-step-current {
  border-color: rgba(37, 87, 214, 0.16);
  background: rgba(37, 87, 214, 0.04);
}

.strategy-status-step-current .strategy-status-index {
  background: rgba(37, 87, 214, 0.14);
  color: var(--color-primary-strong);
}

.strategy-status-step-failed {
  border-color: rgba(179, 76, 47, 0.16);
  background: rgba(179, 76, 47, 0.04);
}

.strategy-status-step-failed .strategy-status-index {
  background: rgba(179, 76, 47, 0.14);
  color: #9f422b;
}

.strategy-section-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.strategy-intro {
  margin-top: 18px;
  padding: 2px 0 24px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.strategy-intro-kicker,
.strategy-adjust-kicker,
.strategy-lane-kicker {
  margin: 0;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.strategy-intro-kicker,
.strategy-adjust-kicker {
  color: var(--color-muted);
}

.strategy-intro-copy,
.strategy-adjust-description,
.strategy-lane-description {
  margin: 10px 0 0;
  color: #5f6f81;
  line-height: 1.75;
  font-size: 14px;
}

.strategy-flow-stack {
  display: flex;
  flex-direction: column;
  gap: 28px;
  margin-top: 24px;
}

.strategy-flow-stack-edit {
  margin-top: 26px;
}

.strategy-lane {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.strategy-lane + .strategy-lane {
  padding-top: 24px;
  border-top: 1px dashed rgba(17, 24, 39, 0.1);
}

.strategy-lane-recommended {
  padding-left: 2px;
}

.strategy-lane-recommended .timeline-list {
  margin-top: 10px;
}

.strategy-lane-recommended .timeline-item {
  box-shadow: none;
}

.strategy-lane-header,
.strategy-adjust-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.strategy-lane-titlebox,
.strategy-adjust-titlebox {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.strategy-adjust-titlebox h5 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 900;
  letter-spacing: -0.03em;
  line-height: 1.08;
}

.strategy-lane-titlebox h5 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.18;
}

.strategy-lane-parent .strategy-lane-kicker,
.strategy-lane-parent .strategy-lane-titlebox h5 {
  color: var(--color-primary-strong);
}

.strategy-lane-child .strategy-lane-kicker,
.strategy-lane-child .strategy-lane-titlebox h5 {
  color: #12644f;
}

.strategy-lane-description,
.strategy-adjust-description {
  max-width: 420px;
  text-align: right;
}

.strategy-adjust-shell {
  margin-top: 34px;
  padding: 26px 22px 22px;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
}

.strategy-adjust-shell .strategy-flow-stack-edit {
  gap: 32px;
}

.strategy-lane-edit {
  padding: 18px 18px 0;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 24, 39, 0.06);
}

.strategy-lane-edit .selected-flow-board,
.strategy-lane-edit .strategy-picker,
.strategy-lane-edit .preview-box {
  margin-top: 14px;
}

.strategy-lane-edit + .strategy-lane-edit {
  border-top: none;
}

.strategy-adjust-kicker {
  color: var(--color-text-strong);
}

.timeline-list,
.summary-log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.timeline-item,
.summary-log-item {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  background: #fff;
  border: 1px solid var(--color-border);
}

.timeline-index {
  width: 38px;
  height: 38px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: rgba(37, 87, 214, 0.1);
  color: var(--color-primary-strong);
  font-weight: 800;
}

.timeline-main strong,
.summary-log-head strong,
.chunk-title-group strong,
.drawer-log-head strong {
  color: var(--color-text-strong);
}

.timeline-main p,
.summary-log-item p,
.chunk-body {
  margin: 8px 0 0;
  color: #607087;
  line-height: 1.7;
}

.chunk-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 4px;
  background: rgba(17, 24, 39, 0.08);
  border: 1px solid rgba(17, 24, 39, 0.08);
  color: var(--color-text);
  font-size: 11px;
  font-weight: 700;
}

.chunk-chip-1 { background: rgba(17, 24, 39, 0.08); color: var(--color-text); }
.chunk-chip-2 { background: rgba(37, 87, 214, 0.1); color: var(--color-primary-strong); }
.chunk-chip-3 { background: rgba(21, 115, 91, 0.1); color: #12644f; }
.chunk-chip-4 { background: rgba(179, 76, 47, 0.1); color: #9f422b; }

.chunk-body {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.chunk-table-panel {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chunk-pagination-bar {
  margin-top: 4px;
  padding-top: 18px;
  border-top: 1px solid var(--color-border);
}

.pagination-status {
  text-align: center;
}

.pagination-status strong {
  display: block;
  color: var(--color-text-strong);
}

.pagination-status span {
  display: block;
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 13px;
}

.page-size-control {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 10px;
  color: var(--color-muted-strong);
  font-size: 13px;
  font-weight: 700;
}

.page-size-control span {
  margin: 0;
  color: inherit;
  font-size: inherit;
}

.page-size-select {
  min-width: 92px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  background: #fff;
  color: var(--color-text-strong);
  font-size: 13px;
  font-weight: 600;
  outline: none;
}

.page-size-select:focus {
  border-color: rgba(37, 87, 214, 0.28);
  box-shadow: 0 0 0 4px rgba(37, 87, 214, 0.08);
}

.chunk-toolbar {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.chunk-section-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.chunk-view-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border-radius: 4px;
  border: 1px solid var(--color-border);
  background: var(--color-surface-soft);
}

.chunk-view-button {
  padding: 8px 12px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: var(--color-muted-strong);
  font-size: 12px;
  font-weight: 700;
}

.chunk-view-button.active {
  background: #fff;
  color: var(--color-primary-strong);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.chunk-stat-card {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
}

.chunk-stat-card span {
  display: block;
  font-size: 11px;
  color: var(--color-muted);
}

.chunk-stat-card strong {
  display: block;
  margin-top: 10px;
  color: var(--color-text-strong);
  font-size: 24px;
  line-height: 1.15;
}

.chunk-table {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  background: #fff;
}

.chunk-group-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chunk-group-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
  overflow: hidden;
}

.chunk-group-card.collapsed {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.chunk-group-head-main {
  min-width: 0;
}

.chunk-group-head-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.chunk-group-head {
  padding: 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-soft);
}

.chunk-group-head strong {
  color: var(--color-text-strong);
  font-size: 16px;
}

.chunk-group-head p {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.chunk-group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.chunk-group-meta span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  background: rgba(37, 87, 214, 0.08);
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.chunk-group-detail-button {
  padding: 8px 12px;
}

.chunk-group-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.chunk-group-toggle-button {
  padding: 8px 12px;
}

.chunk-group-track {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  border-bottom: 1px solid var(--color-border);
}

.chunk-group-node {
  min-width: 96px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  flex: none;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.chunk-group-node strong {
  color: var(--color-text-strong);
}

.chunk-group-node span {
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;
}

.chunk-group-node:hover {
  border-color: rgba(var(--section-accent-rgb), 0.24);
  background: rgba(var(--section-accent-rgb), 0.04);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.chunk-group-table .chunk-row:last-child {
  border-bottom: none;
}

.chunk-table-head,
.chunk-row {
  display: grid;
  grid-template-columns: 112px minmax(180px, 1.4fr) 180px 84px 96px 96px minmax(260px, 2.2fr);
  gap: 14px;
  align-items: start;
}

.chunk-table-head {
  padding: 14px 16px;
  background: var(--color-surface-soft);
  border-bottom: 1px solid var(--color-border);
}

.chunk-table-head span {
  font-size: 11px;
  color: var(--color-muted);
  font-weight: 700;
}

.chunk-row {
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
}

.chunk-row-clickable {
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.chunk-row-clickable:hover {
  background: rgba(var(--section-accent-rgb), 0.04);
  box-shadow: inset 4px 0 0 rgb(var(--section-accent-rgb));
}

.chunk-row:last-child { border-bottom: none; }

.chunk-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chunk-cell strong { color: var(--color-text-strong); line-height: 1.35; }
.chunk-cell span { color: #66768a; font-size: 12px; line-height: 1.5; word-break: break-word; }
.chunk-cell-index strong { font-size: 20px; }
.chunk-relation-hint {
  color: var(--color-primary-strong) !important;
  font-size: 12px !important;
  font-weight: 700;
}
.chunk-cell-status { gap: 8px; }
.chunk-cell-status .chunk-chip { width: fit-content; }

.chunk-cell-content .chunk-body {
  display: -webkit-box;
  overflow: hidden;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.chunk-detail-drawer {
  width: min(720px, 100vw);
}

.chunk-detail-body {
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.chunk-detail-section {
  margin-top: 18px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-soft);
}

.chunk-detail-section-current {
  border-color: rgba(37, 87, 214, 0.18);
  background: rgba(37, 87, 214, 0.04);
}

.chunk-detail-section-parent {
  border-color: rgba(15, 118, 110, 0.18);
  background: rgba(15, 118, 110, 0.04);
}

.chunk-detail-section-focused {
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
}

.chunk-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chunk-detail-title-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chunk-detail-head h4 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 18px;
  font-weight: 800;
}

.chunk-detail-head span,
.chunk-detail-meta span {
  color: var(--color-muted);
  font-size: 12px;
}

.chunk-kind-badge {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chunk-kind-badge-child {
  background: rgba(37, 87, 214, 0.1);
  color: var(--color-primary-strong);
}

.chunk-kind-badge-parent {
  background: rgba(15, 118, 110, 0.12);
  color: #12644f;
}

.chunk-detail-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.chunk-relation-legend {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chunk-relation-legend span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  background: rgba(37, 87, 214, 0.08);
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.chunk-relation-note {
  margin: 12px 0 0;
  color: var(--color-muted-strong);
  font-size: 13px;
  line-height: 1.7;
}

.chunk-relation-track {
  margin-top: 14px;
  padding: 14px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
}

.chunk-relation-node {
  min-width: 86px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-surface-soft);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: none;
}

.chunk-relation-node.active {
  border-color: rgba(37, 87, 214, 0.36);
  background: rgba(37, 87, 214, 0.08);
}

.chunk-relation-node strong {
  color: var(--color-text-strong);
  font-size: 15px;
}

.chunk-relation-node span {
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;
}

.chunk-relation-line {
  height: 2px;
  min-width: 28px;
  background: rgba(17, 24, 39, 0.12);
  border-radius: 4px;
  flex: none;
}

.chunk-relation-line.active {
  background: rgba(37, 87, 214, 0.42);
}

.summary-chip-child {
  border-color: rgba(37, 87, 214, 0.16);
  background: rgba(37, 87, 214, 0.06);
}

.summary-chip-parent {
  border-color: rgba(15, 118, 110, 0.16);
  background: rgba(15, 118, 110, 0.06);
}

.chunk-detail-text {
  margin: 14px 0 0;
  padding: 14px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: #fff;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  font-size: 13px;
  max-height: 280px;
  overflow: auto;
}

.chunk-detail-section-current .chunk-detail-text {
  border-color: rgba(37, 87, 214, 0.12);
}

.chunk-detail-section-parent .chunk-detail-text {
  border-color: rgba(15, 118, 110, 0.12);
}

.parent-block-text {
  max-height: 360px;
}

.sibling-chunk-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sibling-chunk-card {
  width: 100%;
  text-align: left;
  padding: 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.sibling-chunk-card.active {
  border-color: rgba(37, 87, 214, 0.36);
  background: rgba(37, 87, 214, 0.04);
}

.sibling-chunk-card:hover {
  border-color: rgba(37, 87, 214, 0.24);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.sibling-chunk-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sibling-chunk-head strong {
  color: var(--color-text-strong);
}

.sibling-chunk-card p,
.sibling-chunk-card span {
  margin: 0;
  color: #66768a;
  font-size: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.sibling-chunk-card span {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.selected-flow-board { margin-top: 14px; }
.selected-flow-board {
  border: 1px solid rgba(17, 24, 39, 0.08);
}

.selected-flow-board-parent {
  background: rgba(37, 87, 214, 0.03);
}

.selected-flow-board-child {
  background: rgba(15, 118, 110, 0.03);
}

.timeline-list-parent .timeline-item {
  border-color: rgba(37, 87, 214, 0.12);
  background: rgba(37, 87, 214, 0.03);
}

.timeline-list-parent .timeline-index {
  background: rgba(37, 87, 214, 0.12);
  color: var(--color-primary-strong);
}

.timeline-list-child .timeline-item {
  border-color: rgba(15, 118, 110, 0.12);
  background: rgba(15, 118, 110, 0.03);
}

.timeline-list-child .timeline-index {
  background: rgba(15, 118, 110, 0.12);
  color: #12644f;
}

.selected-flow-label {
  font-family: var(--font-display);
  font-size: 24px !important;
  font-weight: 900;
  letter-spacing: -0.03em;
  text-transform: none !important;
}

.selected-flow-label-parent {
  color: var(--color-primary-strong) !important;
}

.selected-flow-label-child {
  color: #12644f !important;
}

.sequence-board {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sequence-card-placeholder { min-height: 1px; }

.selected-flow-sequence,
.build-stage-board { margin-top: 14px; }

.sequence-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.sequence-inline-arrow,
.sequence-down-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
}

.sequence-inline-arrow-empty { visibility: hidden; }

.sequence-down-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  min-height: 44px;
  align-items: center;
}

.sequence-down-row-left .sequence-down-arrow { grid-column: 1; }
.sequence-down-row-right .sequence-down-arrow { grid-column: 3; }

.selected-flow-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
}

.selected-flow-board-parent .selected-flow-card {
  border-color: rgba(37, 87, 214, 0.14);
  box-shadow: none;
}

.selected-flow-board-parent .selected-flow-order {
  background: #2557d6;
}

.selected-flow-board-child .selected-flow-card {
  border-color: rgba(15, 118, 110, 0.14);
  box-shadow: none;
}

.selected-flow-board-child .selected-flow-order {
  background: #0f766e;
}

.selected-flow-order {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  font-size: 18px;
  font-weight: 900;
}

.selected-flow-content strong { display: block; color: var(--color-text-strong); }
.selected-flow-content span { display: block; margin-top: 6px; color: #66768a; font-size: 12px; line-height: 1.55; }

.selected-flow-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-action-button {
  min-width: 58px;
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--color-text-strong);
  font-size: 12px;
  font-weight: 700;
  box-shadow: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.flow-action-button:disabled { opacity: 0.4; cursor: not-allowed; }

.flow-action-button:hover:not(:disabled) {
  border-color: rgba(37, 87, 214, 0.2);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.strategy-picker {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.strategy-picker-parent .strategy-chip.active {
  border-color: rgba(37, 87, 214, 0.4);
  background: rgba(37, 87, 214, 0.08);
  box-shadow: none;
}

.strategy-picker-parent .strategy-chip.active .strategy-chip-state {
  background: rgba(37, 87, 214, 0.16);
  color: var(--color-primary-strong);
}

.strategy-picker-parent .strategy-chip-check {
  color: var(--color-primary-strong);
}

.strategy-picker-child .strategy-chip.active {
  border-color: rgba(15, 118, 110, 0.4);
  background: rgba(15, 118, 110, 0.08);
  box-shadow: none;
}

.strategy-picker-child .strategy-chip.active .strategy-chip-state {
  background: rgba(15, 118, 110, 0.16);
  color: #12644f;
}

.strategy-picker-child .strategy-chip-check {
  color: #12644f;
}

.preview-box-parent {
  border: 1px solid rgba(37, 87, 214, 0.14);
  background: rgba(37, 87, 214, 0.03);
}

.preview-box-child {
  border: 1px solid rgba(15, 118, 110, 0.14);
  background: rgba(15, 118, 110, 0.03);
}

.preview-box .preview-box-title {
  font-family: var(--font-display);
  font-size: 22px !important;
  font-weight: 900;
  letter-spacing: -0.03em;
  text-transform: none !important;
}

.preview-box-title-parent {
  color: var(--color-primary-strong) !important;
}

.preview-box-title-child {
  color: #12644f !important;
}

.strategy-chip {
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  background: #fff;
  box-shadow: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.strategy-chip.active {
  border-color: rgba(37, 87, 214, 0.5);
  background: rgba(37, 87, 214, 0.06);
}

.strategy-chip:hover {
  border-color: rgba(37, 87, 214, 0.22);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.strategy-chip.active .strategy-chip-state {
  background: rgba(37, 87, 214, 0.16);
  color: #1a3fa0;
}

.strategy-chip strong { display: block; color: var(--color-text-strong); font-size: 15px; }
.strategy-chip span { display: block; margin-top: 6px; color: #66768a; font-size: 12px; line-height: 1.5; }

.strategy-chip-state {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 4px;
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-text);
  font-size: 10px;
  font-weight: 700;
}

.strategy-chip-check { width: 22px; height: 22px; color: var(--color-primary-strong); }
.strategy-chip-top { justify-content: space-between; }

.preview-box {
  margin-top: 16px;
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
}

.preview-box > span { font-weight: 700; color: var(--color-primary-strong); font-size: 14px; }

.reason-card p,
.preview-empty,
.selected-flow-empty {
  margin: 10px 0 0;
  color: var(--color-muted-strong);
  line-height: 1.7;
}

.preview-flow {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.preview-tag {
  padding: 7px 14px;
  border-radius: 4px;
  background: rgba(15, 118, 110, 0.08);
  border: 1px solid rgba(15, 118, 110, 0.18);
  color: #12644f;
  font-size: 12px;
  font-weight: 700;
}

.flow-arrow {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 52px;
  margin: 4px 0;
}

.flow-arrow::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 2px;
  bottom: 2px;
  width: 2px;
  transform: translateX(-50%);
  background: rgba(37, 87, 214, 0.2);
}

.preview-arrow,
.back-icon,
.drawer-icon { width: 18px; height: 18px; color: var(--color-primary-strong); }

.flow-arrow-icon {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-strong);
  background: transparent;
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
}

.confirm-actions {
  margin-top: 16px;
  flex-direction: column;
  align-items: stretch;
}

.adjust-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px 14px;
  background: #fff;
  outline: none;
  color: var(--color-text);
}

.adjust-input:focus {
  border-color: rgba(37, 87, 214, 0.28);
  box-shadow: 0 0 0 4px rgba(37, 87, 214, 0.08);
}

.strategy-submit-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-stage-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.action-stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.action-stage-index {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-text-strong);
  font-size: 14px;
  font-weight: 900;
}

.action-stage-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-text);
  font-size: 11px;
  font-weight: 700;
}

.action-stage-card strong { color: var(--color-text-strong); font-size: 18px; }
.action-stage-card p { margin: 0; color: var(--color-muted-strong); line-height: 1.7; }

.action-stage-ready {
  border-color: rgba(37, 87, 214, 0.18);
  box-shadow: var(--shadow-md);
}

.action-stage-ready .action-stage-index,
.action-stage-ready .action-stage-badge {
  background: rgba(37, 87, 214, 0.12);
  color: var(--color-primary-strong);
}

.action-stage-current {
  border-color: rgba(17, 24, 39, 0.1);
  background: var(--color-text-strong);
  box-shadow: var(--shadow-md);
}

.action-stage-current .action-stage-index,
.action-stage-current .action-stage-badge {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.action-stage-current strong,
.action-stage-current p { color: #fff; }

.action-stage-completed {
  border-color: rgba(21, 115, 91, 0.18);
  background: rgba(21, 115, 91, 0.04);
}

.action-stage-completed .action-stage-index,
.action-stage-completed .action-stage-badge {
  background: rgba(21, 115, 91, 0.14);
  color: #12644f;
}

.action-stage-locked {
  border-style: dashed;
  border-color: rgba(17, 24, 39, 0.14);
  background: var(--color-surface-soft);
}

.action-stage-locked .action-stage-badge {
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-muted-strong);
}

.action-stage-card .action-button { width: 100%; justify-content: center; }

.action-button,
.primary-button,
.ghost-button {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  font-weight: 700;
}

.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 56px;
  color: #fff;
  font-weight: 700;
  border: 1px solid transparent;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background 0.2s ease;
  letter-spacing: 0.01em;
}

.action-button-confirm {
  border-color: rgba(37, 87, 214, 0.24);
  background: #2557d6;
  box-shadow: var(--shadow-sm);
}

.action-button-build {
  border-color: rgba(15, 23, 42, 0.22);
  background: #0f172a;
  box-shadow: var(--shadow-sm);
}

.action-button:disabled { opacity: 0.5; cursor: not-allowed; }

.action-button:hover:not(:disabled) {
  transform: translateY(-1px) scale(1.01);
}

.action-button-confirm:hover:not(:disabled) {
  box-shadow: var(--shadow-md);
}

.action-button-build:hover:not(:disabled) {
  box-shadow: var(--shadow-md);
}

.action-button-icon {
  width: 18px;
  height: 18px;
  flex: none;
  opacity: 0.92;
}

.action-stage-completed .action-button-confirm {
  background: #12644f;
}

.action-stage-completed .action-button-confirm:disabled,
.action-stage-locked .action-button-build:disabled { opacity: 0.88; }

.action-stage-locked .action-button-build {
  border-color: var(--color-border);
  background: #fff;
  color: var(--color-text);
  box-shadow: none;
}

.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  cursor: pointer;
  color: var(--color-primary-strong);
  background: #fff;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ghost-button:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-soft);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.ghost-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 87, 214, 0.16);
}

.ghost-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
  transform: none;
}

.inline-notice {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
}

.inline-notice-danger {
  background: rgba(179, 76, 47, 0.08);
  border: 1px solid rgba(179, 76, 47, 0.12);
  color: #9f422b;
}

.empty-block {
  min-height: 260px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--color-muted);
  border-radius: var(--radius-md);
  border: 1px dashed rgba(17, 24, 39, 0.16);
  background: var(--color-surface-soft);
}

.compact-empty { min-height: 140px; margin-top: 14px; }

.build-progress-card {
  padding: 18px 20px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
}

.build-progress-card-inline { margin-top: 18px; scroll-margin-top: 24px; }

.build-pulse {
  padding: 6px 10px;
  border-radius: 4px;
  background: rgba(37, 87, 214, 0.1);
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.build-pulse-static { background: rgba(17, 24, 39, 0.08); color: var(--color-text); }

.build-progress-text { margin: 10px 0 0; color: var(--color-muted); line-height: 1.7; }

.stage-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
}

.stage-order {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  font-size: 16px;
  font-weight: 900;
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-text);
}

.stage-order > span { display: inline-flex; align-items: center; justify-content: center; }
.stage-body strong { display: block; color: var(--color-text-strong); }
.stage-body span { display: block; margin-top: 8px; color: #66768a; font-size: 12px; line-height: 1.55; }

.stage-body em {
  display: inline-flex;
  margin-top: 10px;
  padding: 4px 10px;
  border-radius: 4px;
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
}

.stage-current {
  border-color: transparent;
  background: var(--color-text-strong);
  box-shadow: var(--shadow-md);
}

.stage-current .stage-order,
.stage-current .stage-body strong,
.stage-current .stage-body span,
.stage-current .stage-body em { color: #fff; background: transparent; }

.stage-current .stage-order .stage-spinner { border-color: rgba(255, 255, 255, 0.32); border-top-color: #fff; }

.stage-completed .stage-order,
.stage-completed .stage-body em { background: rgba(21, 115, 91, 0.1); color: #12644f; }

.stage-failed { border-color: rgba(179, 76, 47, 0.14); background: rgba(255, 247, 237, 0.96); }
.stage-failed .stage-order,
.stage-failed .stage-body em { background: rgba(179, 76, 47, 0.1); color: #9f422b; }

.tracker-footer span {
  padding: 8px 12px;
  border-radius: 4px;
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-muted-strong);
  font-size: 12px;
  font-weight: 700;
}

.summary-log-button { align-self: flex-start; }

.stage-spinner,
.build-overlay-spinner {
  border-radius: 50%;
  animation: spin 0.86s linear infinite;
}

.stage-spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(17, 24, 39, 0.22);
  border-top-color: currentColor;
}

.build-overlay {
  position: fixed;
  inset: 0;
  z-index: 28;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 22, 35, 0.55);
}

.build-overlay-card {
  width: min(760px, 100%);
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 26px;
  border-radius: var(--radius-lg);
  background: #111827;
  box-shadow: 0 28px 54px rgba(10, 22, 35, 0.34);
  color: #fff;
}

.build-overlay-head {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.build-overlay-head h3 { margin: 0; font-size: 18px; font-weight: 600; line-height: 1.2; }

.build-overlay-spinner {
  width: 56px;
  height: 56px;
  border: 4px solid rgba(255, 255, 255, 0.22);
  border-top-color: #fff;
}

.build-overlay-text,
.build-overlay-tip { margin: 8px 0 0; color: rgba(255, 255, 255, 0.78); line-height: 1.7; }

.build-overlay-task-meta { display: flex; gap: 10px; flex-wrap: wrap; }

.build-overlay-task-meta span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.94);
  font-size: 12px;
  font-weight: 700;
}

.build-overlay-stage-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.build-overlay-stage {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
}

.build-overlay-stage-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 15px;
  font-weight: 900;
}

.build-overlay-stage-copy strong { display: block; color: #fff; }
.build-overlay-stage-copy span { display: block; margin-top: 6px; color: rgba(255, 255, 255, 0.74); font-size: 13px; }

.build-overlay-stage-current { border-color: rgba(255, 255, 255, 0.18); background: rgba(255, 255, 255, 0.14); }
.build-overlay-stage-current .build-overlay-stage-icon { background: rgba(255, 255, 255, 0.18); }
.build-overlay-stage-current .stage-spinner { border-color: rgba(255, 255, 255, 0.32); border-top-color: #fff; }
.build-overlay-stage-completed .build-overlay-stage-icon { background: rgba(94, 234, 212, 0.16); color: #8ff8df; }
.build-overlay-stage-failed .build-overlay-stage-icon { background: rgba(248, 113, 113, 0.16); color: #fecaca; }

.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(10, 22, 35, 0.42);
  z-index: 18;
}

.log-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: min(560px, 100vw);
  z-index: 19;
  border: none;
  border-left: 1px solid var(--color-border);
  border-radius: 0;
  background: #fff;
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.icon-button {
  width: 40px;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  background: #fff;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background-color 0.2s ease;
}

.icon-button:hover {
  border-color: rgba(37, 87, 214, 0.2);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.summary-chip {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-empty {
  margin-top: 20px;
  min-height: 180px;
  display: grid;
  place-items: center;
  text-align: center;
  color: #6e849c;
}

.drawer-timeline {
  margin-top: 20px;
  padding-right: 6px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-log-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
}

.drawer-log-node { position: relative; width: 18px; }

.drawer-log-node::before {
  content: '';
  position: absolute;
  top: 6px;
  left: 6px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 87, 214, 0.12);
}

.drawer-log-node::after {
  content: '';
  position: absolute;
  top: 18px;
  left: 8px;
  bottom: -18px;
  width: 2px;
  background: rgba(37, 87, 214, 0.15);
}

.drawer-log-item:last-child .drawer-log-node::after { display: none; }

.drawer-log-body {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
}

.drawer-log-body p { margin: 10px 0 0; }

.drawer-log-detail {
  margin: 12px 0 0;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: #0f1724;
  color: #dbe5f5;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active { transition: opacity 0.22s ease; }
.drawer-fade-enter-from,
.drawer-fade-leave-to { opacity: 0; }

.drawer-slide-enter-active,
.drawer-slide-leave-active { transition: transform 0.26s ease, opacity 0.26s ease; }
.drawer-slide-enter-from,
.drawer-slide-leave-to { opacity: 0; transform: translateX(24px); }

.build-mask-fade-enter-active,
.build-mask-fade-leave-active { transition: opacity 0.22s ease; }
.build-mask-fade-enter-from,
.build-mask-fade-leave-to { opacity: 0; }

.page-notice {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 960px) {
  .strategy-status-bar {
    grid-template-columns: 1fr;
  }

  .page-top,
  .overview-document-card,
  .overview-document-side,
  .build-progress-header,
  .workbench-section-head,
  .strategy-lane-header,
  .strategy-adjust-header,
  .task-section-actions,
  .confirm-actions,
  .drawer-log-head,
  .tracker-footer,
  .chunk-head {
    flex-direction: column;
    align-items: stretch;
  }

  .workspace-guidance-grid,
  .meta-grid,
  .execution-summary-grid,
  .strategy-picker { grid-template-columns: 1fr; }
  .workspace-shortcut-group { grid-template-columns: 1fr; }
  .overview-action-row { grid-template-columns: 1fr; }
  .workspace-subsection-copy-inline { text-align: left; }
  .overview-document-card h3 { font-size: 30px; }
  .workbench-section-heading h2 { font-size: 34px; }
  .workspace-subsection h3 { font-size: 24px; }
  .strategy-adjust-titlebox h5 { font-size: 28px; }
  .strategy-lane-titlebox h5 { font-size: 20px; }
  .chunk-toolbar { grid-template-columns: 1fr 1fr; }
  .pagination-bar { flex-direction: column; }
  .sequence-row { grid-template-columns: 1fr; }
  .selected-flow-card { grid-template-columns: 56px minmax(0, 1fr); }
  .selected-flow-actions { grid-column: span 2; flex-direction: row; }
  .sequence-inline-arrow,
  .sequence-down-row { justify-content: center; }
  .sequence-down-row { grid-template-columns: 1fr; }
  .sequence-down-row-left .sequence-down-arrow,
  .sequence-down-row-right .sequence-down-arrow { grid-column: 1; }
  .strategy-submit-actions,
  .build-overlay-stage-list { grid-template-columns: 1fr; }
  .chunk-table-head { display: none; }
  .chunk-row { grid-template-columns: 1fr 1fr; }
  .chunk-cell { padding-top: 2px; }
  .chunk-cell::before { content: attr(data-label); font-size: 11px; color: var(--color-muted); }
  .chunk-cell-content { grid-column: 1 / -1; }
  .build-overlay { padding: 16px; }
  .build-overlay-card { padding: 20px; }
  .build-overlay-head { grid-template-columns: 1fr; }
}
</style>
