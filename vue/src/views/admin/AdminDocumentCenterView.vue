<template>
  <section class="document-page">
    <transition name="drawer-fade">
      <div v-if="logDrawerOpen" class="drawer-overlay" @click="closeLogDrawer"></div>
    </transition>

    <transition name="build-mask-fade">
      <div v-if="showBuildBlockingOverlay" class="build-overlay">
        <div class="build-overlay-card">
          <div class="build-overlay-head">
            <span class="build-overlay-spinner" aria-hidden="true"></span>
            <div>
              <p class="section-eyebrow">Index Build Running</p>
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
            <p class="section-eyebrow">Task Timeline</p>
            <h3>任务执行详情</h3>
            <p class="drawer-subtitle">
              任务 {{ selectedDocument?.latestTaskId || '-' }} · {{ selectedDocument?.latestTaskTypeName || '暂无任务类型' }}
            </p>
          </div>
          <button class="icon-button" type="button" @click="closeLogDrawer">
            <XMarkIcon class="drawer-icon" />
          </button>
        </div>

        <div class="drawer-summary">
          <div class="summary-chip">
            <span>当前状态</span>
            <AdminStatusBadge
              :label="selectedDocument?.latestTaskStatusName || '暂无状态'"
              :code="selectedDocument?.latestTaskStatus"
              type="task"
            />
          </div>
          <div class="summary-chip">
            <span>索引状态</span>
            <AdminStatusBadge
              :label="selectedDocument?.indexStatusName || '暂无状态'"
              :code="selectedDocument?.indexStatus"
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

    <div class="top-grid">
      <article class="panel-card upload-card">
        <div class="panel-title">
          <div>
            <p class="section-eyebrow">Document Intake</p>
            <h3>上传资料并进入推荐流程</h3>
          </div>
        </div>

        <div class="upload-grid">
          <label class="field">
            <span>文档名称</span>
            <input v-model="uploadForm.documentName" type="text" placeholder="不填则使用原始文件名" />
          </label>

          <label class="field">
            <span>知识域编码</span>
            <input v-model="uploadForm.knowledgeScopeCode" type="text" placeholder="例如 operation_rule" />
          </label>

          <label class="field">
            <span>知识域名称</span>
            <input v-model="uploadForm.knowledgeScopeName" type="text" placeholder="例如 运营规则" />
          </label>

          <label class="field">
            <span>业务分类</span>
            <input v-model="uploadForm.businessCategory" type="text" placeholder="例如 手册 / 规则 / 介绍" />
          </label>

          <label class="field">
            <span>文档标签</span>
            <input v-model="uploadForm.documentTags" type="text" placeholder="多个标签用英文逗号分隔" />
          </label>

          <label class="field">
            <span>选择文件</span>
            <input ref="fileInputRef" type="file" class="file-input" @change="handleFileChange" />
          </label>
        </div>

        <div class="upload-hint">
          <span>支持 PDF / DOC / DOCX / TXT / MD / HTML</span>
          <strong>{{ uploadForm.file ? uploadForm.file.name : '尚未选择文件' }}</strong>
        </div>

        <div class="upload-actions">
          <button class="ghost-button" type="button" @click="clearSelectedFile">清空</button>
          <button class="primary-button" type="button" :disabled="uploading || !uploadForm.file" @click="submitUpload">
            {{ uploading ? '上传中...' : '上传并解析' }}
          </button>
        </div>
      </article>

      <article class="panel-card tips-card">
        <div class="panel-title">
          <div>
            <p class="section-eyebrow">Flow Hints</p>
            <h3>建议操作顺序</h3>
          </div>
        </div>

        <ul class="tips-list">
          <li>上传文档后，系统会异步解析内容并生成推荐切块策略。</li>
          <li>推荐策略出来以后，可以直接拖拽已选策略调整执行顺序。</li>
          <li>策略确认完成后，才能发起索引构建并进入 PGVector。</li>
          <li>索引构建过程会保留完整阶段轨迹，方便教学讲解和任务排查。</li>
        </ul>
      </article>
    </div>

    <div v-if="pageNotice.message" class="page-notice" :class="`page-notice-${pageNotice.type}`">
      {{ pageNotice.message }}
    </div>

    <div class="content-grid">
      <article class="panel-card list-card">
        <div class="list-toolbar">
          <div>
            <p class="section-eyebrow">Document Pool</p>
            <h3>文档列表</h3>
          </div>

          <div class="list-actions">
            <input
              v-model="keyword"
              class="search-input"
              type="text"
              placeholder="搜索文档名称或原始文件名"
              @keydown.enter="loadDocuments"
            />
            <button class="ghost-button" type="button" @click="loadDocuments">搜索</button>
          </div>
        </div>

        <div class="document-list">
          <button
            v-for="item in documents"
            :key="item.documentId"
            class="document-row"
            :class="{ active: normalizeCode(selectedDocumentId) === normalizeCode(item.documentId) }"
            type="button"
            @click="selectDocument(item.documentId)"
          >
            <div class="document-row-main">
              <div class="document-row-title">
                <strong>{{ item.documentName }}</strong>
                <span>{{ item.fileTypeName }}</span>
              </div>
              <p>{{ item.originalFileName }}</p>
              <div class="document-row-meta">
                <span>{{ formatFileSize(item.fileSize) }}</span>
                <span>{{ formatDateTime(item.editTime) }}</span>
              </div>
            </div>
            <div class="document-row-status">
              <AdminStatusBadge :label="item.parseStatusName" :code="item.parseStatus" type="parse" />
              <AdminStatusBadge :label="item.strategyStatusName" :code="item.strategyStatus" type="strategy" />
              <AdminStatusBadge :label="item.indexStatusName" :code="item.indexStatus" type="index" />
            </div>
          </button>

          <div v-if="!listLoading && !documents.length" class="empty-block">
            还没有文档，先上传一份资料开始体验。
          </div>
          <div v-if="listLoading" class="empty-block">正在加载文档列表...</div>
          <div v-if="!listLoading && documents.length && !selectedDocument" class="list-selection-hint">
            点击任意文档，查看解析详情、策略方案、构建轨迹和 Chunk 列表。
          </div>
        </div>
      </article>

      <article v-if="selectedDocument" class="panel-card detail-card">
        <div class="detail-content">
          <div class="detail-header">
            <div>
              <p class="section-eyebrow">Selected Document</p>
              <h3>{{ selectedDocument.documentName }}</h3>
              <p class="detail-subtitle">{{ selectedDocument.originalFileName }}</p>
            </div>

            <div class="detail-statuses">
              <AdminStatusBadge :label="selectedDocument.parseStatusName" :code="selectedDocument.parseStatus" type="parse" />
              <AdminStatusBadge :label="selectedDocument.strategyStatusName" :code="selectedDocument.strategyStatus" type="strategy" />
              <AdminStatusBadge :label="selectedDocument.indexStatusName" :code="selectedDocument.indexStatus" type="index" />
            </div>
          </div>

          <div class="meta-grid">
            <div class="meta-item">
              <span>文档 ID</span>
              <strong>{{ selectedDocument.documentId }}</strong>
            </div>
            <div class="meta-item">
              <span>当前方案</span>
              <strong>{{ selectedDocument.currentPlanId || '-' }}</strong>
            </div>
            <div class="meta-item">
              <span>最近任务</span>
              <strong>{{ selectedDocument.latestTaskId || '-' }}</strong>
            </div>
            <div class="meta-item">
              <span>字符 / Token</span>
              <strong>{{ formatCount(selectedDocument.charCount) }} / {{ formatCount(selectedDocument.tokenCount) }}</strong>
            </div>
          </div>

          <div class="detail-secondary-actions">
            <button class="ghost-button" type="button" :disabled="planLoading" @click="loadSelectedDocumentDetail">
              {{ planLoading ? '刷新中...' : '刷新详情' }}
            </button>
            <button class="ghost-button" type="button" :disabled="!selectedDocument.latestTaskId" @click="openLogDrawer">
              查看任务时间线
            </button>
          </div>

          <section class="detail-section">
            <div class="section-headline section-headline-major">
              <h4>策略推荐与确认</h4>
              <span v-if="strategyPlan?.planReady">方案已就绪</span>
              <span v-else>等待策略推荐</span>
            </div>

            <div v-if="selectedDocument.parseErrorMsg" class="inline-notice inline-notice-danger">
              {{ selectedDocument.parseErrorMsg }}
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
                          <ArrowDownIcon class="flow-arrow-icon" />
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
                                  <button
                                    class="flow-action-button"
                                    type="button"
                                    :disabled="row.leftItem.index === 0"
                                    @click="moveStrategy(row.leftItem.type, -1, pipeline.key)"
                                  >
                                    上移
                                  </button>
                                  <button
                                    class="flow-action-button"
                                    type="button"
                                    :disabled="row.leftItem.index === getSelectedStrategyPreview(pipeline.key).length - 1"
                                    @click="moveStrategy(row.leftItem.type, 1, pipeline.key)"
                                  >
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
                                  <button
                                    class="flow-action-button"
                                    type="button"
                                    :disabled="row.rightItem.index === 0"
                                    @click="moveStrategy(row.rightItem.type, -1, pipeline.key)"
                                  >
                                    上移
                                  </button>
                                  <button
                                    class="flow-action-button"
                                    type="button"
                                    :disabled="row.rightItem.index === getSelectedStrategyPreview(pipeline.key).length - 1"
                                    @click="moveStrategy(row.rightItem.type, 1, pipeline.key)"
                                  >
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
                      {{ confirmButtonLabel }}
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
                      {{ buildButtonLabel }}
                    </button>
                  </article>
                </div>
              </div>

              <div v-if="showBuildTracker" ref="buildTrackerRef" class="build-progress-card build-progress-card-inline">
                <div class="build-progress-header">
                  <div>
                    <p class="section-eyebrow">Index Build Tracker</p>
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
                  <span>状态 {{ buildTaskSnapshot?.taskStatusName || (hasCode(selectedDocument.indexStatus, 3) ? '成功' : '未知') }}</span>
                  <span>耗时 {{ formatDuration(buildTaskSnapshot?.costMillis) }}</span>
                </div>
              </div>
            </template>
          </section>

          <section class="detail-section">
            <div class="section-headline section-headline-major section-headline-chunk">
              <h4>解析后的 Chunk 列表</h4>
              <span v-if="chunkQuery?.taskId">任务 {{ chunkQuery.taskId }} · {{ chunkQuery.total || 0 }} 条</span>
              <span v-else>当前还没有可展示的 chunk</span>
            </div>

            <div v-if="chunkLoading" class="empty-block compact-empty">正在加载 Chunk 列表...</div>
            <div v-else-if="!chunkRecords.length" class="empty-block compact-empty">
              当前文档还没有 Chunk 数据。请先完成索引构建，或等待构建任务继续执行。
            </div>

            <div v-else class="chunk-list">
              <article v-for="item in chunkRecords" :key="item.chunkId" class="chunk-item">
                <div class="chunk-head">
                  <div class="chunk-title-group">
                    <strong>Chunk #{{ item.chunkNo }}</strong>
                    <span>{{ item.sectionPath || '未识别章节' }}</span>
                  </div>
                  <div class="chunk-status-group">
                    <span class="chunk-chip">{{ item.sourceTypeName || '未知来源' }}</span>
                    <span class="chunk-chip" :class="`chunk-chip-${normalizeCode(item.vectorStatus) || '0'}`">
                      {{ item.vectorStatusName || '未知状态' }}
                    </span>
                  </div>
                </div>
                <div class="chunk-meta">
                  <span>字符 {{ formatCount(item.charCount) }}</span>
                  <span>Token {{ formatCount(item.tokenCount) }}</span>
                </div>
                <p class="chunk-body">{{ item.chunkText }}</p>
              </article>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-headline">
              <h4>最近任务摘要</h4>
              <span>{{ taskLogs.length ? `${taskLogs.length} 条日志` : '暂无日志' }}</span>
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
              <button class="ghost-button summary-log-button" type="button" @click="openLogDrawer">
                查看完整任务时间线
              </button>
            </div>
          </section>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowDownIcon,
  ArrowRightIcon,
  CheckCircleIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import { manageApi, APIError } from '../../api/api'
import AdminStatusBadge from '../../components/admin/AdminStatusBadge.vue'
import { formatCount, formatDateTime, formatFileSize, hasCode, normalizeCode } from '../../utils/manageFormat'
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

const strategyLibrary = STRATEGY_LIBRARY
const strategyPipelineLibrary = STRATEGY_PIPELINE_LIBRARY

const BUILD_STAGE_LIBRARY = [
  {
    code: '5',
    order: '01',
    label: '切块执行',
    description: '按照当前策略链路生成原始 chunk'
  },
  {
    code: '6',
    order: '02',
    label: '切块后处理',
    description: '清洗空块并整理最终可入库片段'
  },
  {
    code: '7',
    order: '03',
    label: '向量化',
    description: '生成 embedding 并写入 PGVector'
  },
  {
    code: '8',
    order: '04',
    label: '入库完成',
    description: '回写状态并将本次索引标记为可用'
  }
]

const BUILD_STAGE_CODE_SET = new Set(BUILD_STAGE_LIBRARY.map((item) => item.code))
const OPERATOR_ID = '10001'

const uploadForm = reactive({
  documentName: '',
  knowledgeScopeCode: '',
  knowledgeScopeName: '',
  businessCategory: '',
  documentTags: '',
  file: null
})
const fileInputRef = ref(null)
const uploading = ref(false)
const listLoading = ref(false)
const planLoading = ref(false)
const confirmLoading = ref(false)
const buildLoading = ref(false)
const logLoading = ref(false)
const chunkLoading = ref(false)
const planPollTimer = ref(null)
const buildPollTimer = ref(null)
const keyword = ref('')
const documents = ref([])
const selectedDocumentId = ref('')
const strategyPlan = ref(null)
const selectedParentStrategyTypes = ref([])
const selectedChildStrategyTypes = ref([])
const adjustNote = ref('')
const taskLogs = ref([])
const taskLogSnapshot = ref(null)
const buildTaskSnapshot = ref(null)
const chunkQuery = ref(null)
const logDrawerOpen = ref(false)
const buildTrackerRef = ref(null)
const pageNotice = reactive({
  type: 'info',
  message: ''
})

const selectedDocument = computed(() => {
  return documents.value.find((item) => normalizeCode(item.documentId) === normalizeCode(selectedDocumentId.value)) || null
})

const selectedParentStrategyPreview = computed(() => buildStrategyPreview(selectedParentStrategyTypes.value, strategyLibrary))
const selectedChildStrategyPreview = computed(() => buildStrategyPreview(selectedChildStrategyTypes.value, strategyLibrary))
const selectedParentStrategyRows = computed(() => buildSequenceRows(selectedParentStrategyPreview.value))
const selectedChildStrategyRows = computed(() => buildSequenceRows(selectedChildStrategyPreview.value))
const confirmedParentStrategyTypes = computed(() => extractPipelineStrategyTypes(strategyPlan.value?.plan, 'parent', strategyLibrary))
const confirmedChildStrategyTypes = computed(() => extractPipelineStrategyTypes(strategyPlan.value?.plan, 'child', strategyLibrary))

const isBuildPolling = computed(() => buildPollTimer.value != null)
const hasSelectedStrategy = computed(() => selectedParentStrategyPreview.value.length > 0 && selectedChildStrategyPreview.value.length > 0)
const hasConfirmedStrategy = computed(() => Boolean(selectedDocument.value?.currentPlanId) && hasCode(selectedDocument.value?.strategyStatus, 3))
const hasUnconfirmedStrategyChanges = computed(() => {
  return buildStrategySignature(selectedParentStrategyTypes.value, strategyLibrary) !== buildStrategySignature(confirmedParentStrategyTypes.value, strategyLibrary)
    || buildStrategySignature(selectedChildStrategyTypes.value, strategyLibrary) !== buildStrategySignature(confirmedChildStrategyTypes.value, strategyLibrary)
    || Boolean(adjustNote.value.trim())
})

const hasBuildTaskSnapshot = computed(() => {
  return hasCode(buildTaskSnapshot.value?.taskType, 2)
})

const activeBuildTaskId = computed(() => {
  if (hasCode(selectedDocument.value?.latestTaskType, 2)) {
    return selectedDocument.value?.latestTaskId || ''
  }
  return selectedDocument.value?.lastIndexTaskId || ''
})
const hasBuildInFlightStatus = computed(() => {
  const taskStatus = normalizeCode(buildTaskSnapshot.value?.taskStatus)
  return buildLoading.value
    || taskStatus === '1'
    || taskStatus === '2'
    || hasCode(selectedDocument.value?.indexStatus, 2)
    || (hasCode(selectedDocument.value?.latestTaskType, 2) && ['1', '2'].includes(normalizeCode(selectedDocument.value?.latestTaskStatus)))
})
const showBuildBlockingOverlay = computed(() => hasBuildInFlightStatus.value)

const showBuildTracker = computed(() => {
  if (!selectedDocument.value) {
    return false
  }
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
  if ((hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 3)) || hasCode(selectedDocument.value?.indexStatus, 3)) {
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
  const parseStatus = normalizeCode(selectedDocument.value?.parseStatus)
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
  return hasCode(selectedDocument.value?.indexStatus, 3) ? '可再次执行' : '已解锁'
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
  if (hasCode(selectedDocument.value?.indexStatus, 3)) {
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

const buildTrackerTitle = computed(() => {
  if (!showBuildTracker.value) {
    return ''
  }

  if (hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 4)) {
    return `最近一次构建在「${buildTaskSnapshot.value?.currentStageName || '未知阶段'}」失败`
  }

  if ((hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 3)) || hasCode(selectedDocument.value?.indexStatus, 3)) {
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

  if ((hasBuildTaskSnapshot.value && hasCode(buildTaskSnapshot.value?.taskStatus, 3)) || hasCode(selectedDocument.value?.indexStatus, 3)) {
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

    return {
      ...stage,
      order: stage.order,
      status,
      statusLabel
    }
  })
})

const buildStageRows = computed(() => {
  return buildSequenceRows(buildStageItems.value)
})

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

const chunkRecords = computed(() => {
  return Array.isArray(chunkQuery.value?.records) ? chunkQuery.value.records : []
})

function showNotice(message, type = 'info') {
  pageNotice.type = type
  pageNotice.message = message
}

function clearNotice() {
  pageNotice.message = ''
}

function handleFileChange(event) {
  uploadForm.file = event.target.files?.[0] || null
}

function clearSelectedFile() {
  uploadForm.file = null
  uploadForm.documentName = ''
  uploadForm.knowledgeScopeCode = ''
  uploadForm.knowledgeScopeName = ''
  uploadForm.businessCategory = ''
  uploadForm.documentTags = ''
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

async function loadDocuments(preferredDocumentId) {
  listLoading.value = true

  try {
    const data = await manageApi.queryDocumentPage({
      pageNo: 1,
      pageSize: 30,
      keyword: keyword.value.trim()
    })
    documents.value = Array.isArray(data?.records) ? data.records : []

    const targetId = resolveValidDocumentId(preferredDocumentId) || resolveValidDocumentId(selectedDocumentId.value) || ''
    selectedDocumentId.value = targetId ? String(targetId) : ''
  } catch (error) {
    console.error('加载文档列表失败', error)
    showNotice(normalizeError(error, '加载文档列表失败'), 'danger')
  } finally {
    listLoading.value = false
  }
}

async function loadSelectedDocumentDetail() {
  const effectiveDocumentId = resolveValidDocumentId(selectedDocumentId.value)
  if (!effectiveDocumentId) {
    strategyPlan.value = null
    selectedParentStrategyTypes.value = []
    selectedChildStrategyTypes.value = []
    taskLogs.value = []
    taskLogSnapshot.value = null
    buildTaskSnapshot.value = null
    chunkQuery.value = null
    return
  }

  if (normalizeCode(selectedDocumentId.value) !== normalizeCode(effectiveDocumentId)) {
    selectedDocumentId.value = String(effectiveDocumentId)
    return
  }

  planLoading.value = true
  clearNotice()

  try {
    strategyPlan.value = await manageApi.queryStrategyPlan(effectiveDocumentId)
    selectedParentStrategyTypes.value = extractPipelineStrategyTypes(strategyPlan.value?.plan, 'parent', strategyLibrary)
    selectedChildStrategyTypes.value = extractPipelineStrategyTypes(strategyPlan.value?.plan, 'child', strategyLibrary)
    adjustNote.value = ''
  } catch (error) {
    console.error('读取策略详情失败', error)
    showNotice(normalizeError(error, '读取策略详情失败'), 'danger')
  } finally {
    planLoading.value = false
  }

  await Promise.all([loadTaskLogs(), loadBuildTaskLogs(), loadDocumentChunks()])
}

async function selectDocument(documentId) {
  selectedDocumentId.value = String(documentId || '')
}

function openLogDrawer() {
  logDrawerOpen.value = true
  loadTaskLogs()
}

function closeLogDrawer() {
  logDrawerOpen.value = false
}

async function submitUpload() {
  if (!uploadForm.file) {
    showNotice('请先选择要上传的文档。', 'danger')
    return
  }

  uploading.value = true
  clearNotice()

  try {
    const result = await manageApi.uploadDocument({
      file: uploadForm.file,
      documentName: uploadForm.documentName.trim(),
      operatorId: OPERATOR_ID,
      knowledgeScopeCode: uploadForm.knowledgeScopeCode.trim(),
      knowledgeScopeName: uploadForm.knowledgeScopeName.trim(),
      businessCategory: uploadForm.businessCategory.trim(),
      documentTags: uploadForm.documentTags.trim()
    })
    showNotice(`文档已上传，任务 ${result.taskId} 已进入解析与策略推荐队列。`, 'success')
    const nextDocumentId = result.documentId
    clearSelectedFile()
    await loadDocuments()
    startPlanPolling(nextDocumentId)
  } catch (error) {
    console.error('上传文档失败', error)
    showNotice(normalizeError(error, '上传文档失败'), 'danger')
  } finally {
    uploading.value = false
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
      documentId: selectedDocumentId.value,
      basePlanId: strategyPlan.value.plan.planId,
      adjustNote: adjustNote.value.trim(),
      operatorId: OPERATOR_ID,
      parentSteps: buildPipelineStepPayload(selectedParentStrategyTypes.value, strategyLibrary),
      childSteps: buildPipelineStepPayload(selectedChildStrategyTypes.value, strategyLibrary)
    })
    showNotice('策略方案已确认，接下来可以直接构建索引。', 'success')
    await loadDocuments(selectedDocumentId.value)
    await loadSelectedDocumentDetail()
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
  if (!hasConfirmedStrategy.value || !selectedDocument.value?.currentPlanId) {
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
      documentId: selectedDocumentId.value,
      planId: selectedDocument.value.currentPlanId,
      operatorId: OPERATOR_ID
    })
    showNotice(`索引任务 ${result.taskId} 已创建，系统正在异步构建中。`, 'success')
    await loadDocuments(selectedDocumentId.value)
    await loadSelectedDocumentDetail()
    startBuildPolling(selectedDocumentId.value)
    focusBuildTracker()
  } catch (error) {
    console.error('构建索引失败', error)
    showNotice(normalizeError(error, '构建索引失败'), 'danger')
  } finally {
    buildLoading.value = false
  }
}

async function loadTaskLogs() {
  const latestTaskId = selectedDocument.value?.latestTaskId
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
    showNotice(normalizeError(error, '读取任务日志失败'), 'danger')
    taskLogs.value = []
    taskLogSnapshot.value = null
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

async function loadDocumentChunks() {
  const documentId = resolveValidDocumentId(selectedDocumentId.value)
  if (!documentId) {
    chunkQuery.value = null
    return
  }

  chunkLoading.value = true
  try {
    chunkQuery.value = await manageApi.queryDocumentChunks({
      documentId,
      pageNo: 1,
      pageSize: 20
    })
  } catch (error) {
    console.error('读取 chunk 列表失败', error)
    showNotice(normalizeError(error, '读取 chunk 列表失败'), 'danger')
    chunkQuery.value = null
  } finally {
    chunkLoading.value = false
  }
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

function resolveValidDocumentId(candidateId) {
  if (!candidateId) {
    return ''
  }

  const matched = documents.value.find((item) => normalizeCode(item.documentId) === normalizeCode(candidateId))
  return matched?.documentId ? String(matched.documentId) : ''
}

function focusBuildTracker() {
  nextTick(() => {
    buildTrackerRef.value?.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
  })
}

function startPlanPolling(documentId) {
  if (planPollTimer.value) {
    window.clearInterval(planPollTimer.value)
  }

  let pollCount = 0
  planPollTimer.value = window.setInterval(async () => {
    pollCount += 1
    try {
      const result = await manageApi.queryStrategyPlan(documentId)
      if (normalizeCode(selectedDocumentId.value) === normalizeCode(documentId)) {
        strategyPlan.value = result
        selectedParentStrategyTypes.value = extractPipelineStrategyTypes(result?.plan, 'parent', strategyLibrary)
        selectedChildStrategyTypes.value = extractPipelineStrategyTypes(result?.plan, 'child', strategyLibrary)
      }

      await loadDocuments(documentId)
      if (result?.planReady || normalizeCode(result?.parseStatus) === '4' || pollCount >= 8) {
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

function clearBuildPolling() {
  if (buildPollTimer.value) {
    window.clearInterval(buildPollTimer.value)
    buildPollTimer.value = null
  }
}

function startBuildPolling(documentId) {
  clearBuildPolling()

  let pollCount = 0
  buildPollTimer.value = window.setInterval(async () => {
    pollCount += 1
    try {
      await loadDocuments(documentId)
      if (normalizeCode(selectedDocumentId.value) === normalizeCode(documentId)) {
        await loadSelectedDocumentDetail()
      }

      const currentItem = documents.value.find((item) => normalizeCode(item.documentId) === normalizeCode(documentId))
      const indexDone = currentItem && normalizeCode(currentItem.indexStatus) !== '2'
      const taskDone = currentItem && !['1', '2'].includes(normalizeCode(currentItem.latestTaskStatus))

      if (indexDone && taskDone) {
        clearBuildPolling()
        return
      }

      if (pollCount >= 30) {
        clearBuildPolling()
      }
    } catch (error) {
      console.error('轮询索引构建状态失败', error)
      clearBuildPolling()
    }
  }, 3000)
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

watch(selectedDocumentId, async (value, oldValue) => {
  if (!value || value === oldValue) {
    return
  }
  await loadSelectedDocumentDetail()
})

watch(selectedDocument, (value) => {
  if (!value) {
    clearBuildPolling()
    return
  }

  const latestTaskBuilding = hasCode(value.latestTaskType, 2) && ['1', '2'].includes(normalizeCode(value.latestTaskStatus))
  const building = hasCode(value.indexStatus, 2) || latestTaskBuilding
  if (building && !buildPollTimer.value) {
    startBuildPolling(value.documentId)
    return
  }

  if (!building && buildPollTimer.value) {
    clearBuildPolling()
  }
})

onMounted(async () => {
  await loadDocuments()
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
.document-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.top-grid,
.content-grid {
  display: grid;
  gap: 18px;
}

.top-grid {
  grid-template-columns: 1.05fr 0.95fr;
}

.content-grid {
  grid-template-columns: 1fr;
}

.panel-card {
  border: 1px solid rgba(21, 49, 75, 0.08);
  background: var(--color-admin-panel);
  border-radius: 28px;
  box-shadow: 0 18px 42px rgba(21, 49, 75, 0.06);
  padding: 24px 26px;
}

.panel-title,
.list-toolbar,
.panel-title > div,
.detail-header,
.detail-statuses,
.build-progress-header,
.detail-secondary-actions,
.section-headline,
.summary-log-head,
.drawer-log-head,
.confirm-actions,
.upload-actions,
.document-row-title,
.strategy-chip-top,
.document-row-meta,
.document-row-status,
.preview-tags,
.preview-flow,
.drawer-summary,
.tracker-footer {
  display: flex;
  align-items: center;
}

.panel-title,
.list-toolbar,
.detail-header,
.section-headline {
  justify-content: space-between;
  gap: 12px;
}

.section-headline-major {
  padding: 0 0 14px;
  border-bottom: 1px solid rgba(23, 48, 79, 0.08);
}

.section-headline.section-headline-major h4 {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 900;
  letter-spacing: -0.03em;
  color: #262e3a;
  line-height: 1.08;
}

.section-headline.section-headline-major span {
  color: #5c738b;
  font-size: 13px;
  font-weight: 700;
}

.section-headline-editor {
  margin-top: 30px;
}

.pipeline-headline {
  margin-top: 18px;
  padding: 10px 4px 8px;
  border-bottom: 2px solid rgba(23, 48, 79, 0.08);
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
  border-bottom-color: rgba(178, 31, 196, 0.22);
}

.section-headline.pipeline-headline-parent h4,
.section-headline.pipeline-headline-parent span {
  color: #b21fc4;
}

.pipeline-headline-child {
  border-bottom-color: rgba(13, 124, 124, 0.22);
}

.section-headline.pipeline-headline-child h4,
.section-headline.pipeline-headline-child span {
  color: #0d7c7c;
}

.panel-title h3,
.list-toolbar h3,
.detail-header h3,
.section-headline h4 {
  margin: 0;
  color: #13283f;
}

.section-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #6b839d;
}

.upload-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field span {
  font-size: 13px;
  font-weight: 700;
  color: #47627b;
}

.field input {
  width: 100%;
  border: 1px solid rgba(21, 49, 75, 0.12);
  border-radius: 16px;
  padding: 13px 14px;
  background: #ffffff;
  outline: none;
}

.field input:focus,
.search-input:focus,
.adjust-input:focus {
  border-color: rgba(13, 124, 124, 0.34);
  box-shadow: 0 0 0 4px rgba(13, 124, 124, 0.08);
}

.upload-hint {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(245, 248, 252, 0.92);
}

.upload-hint span {
  display: block;
  color: #67809b;
  font-size: 13px;
}

.upload-hint strong {
  display: block;
  margin-top: 8px;
  color: #13283f;
  word-break: break-all;
}

.upload-actions,
.list-actions,
.detail-secondary-actions,
.confirm-actions {
  gap: 12px;
}

.upload-actions {
  margin-top: 18px;
  justify-content: flex-end;
}

.tips-list {
  margin: 14px 0 0;
  padding-left: 20px;
  color: #4b6279;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-notice {
  padding: 14px 18px;
  border-radius: 20px;
  font-weight: 600;
}

.page-notice-info {
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
}

.page-notice-success {
  background: rgba(10, 109, 122, 0.1);
  color: #0a6d7a;
}

.page-notice-danger {
  background: rgba(194, 65, 12, 0.12);
  color: #c2410c;
}

.list-actions {
  display: flex;
}

.search-input,
.adjust-input {
  border: 1px solid rgba(21, 49, 75, 0.12);
  border-radius: 16px;
  padding: 12px 14px;
  background: #ffffff;
  outline: none;
}

.search-input {
  min-width: 260px;
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
  min-height: 420px;
}

.list-selection-hint {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(23, 48, 79, 0.05);
  color: #5f7891;
  text-align: center;
  line-height: 1.7;
}

.document-row {
  width: 100%;
  position: relative;
  overflow: hidden;
  border: 1px solid transparent;
  border-radius: 22px;
  padding: 16px 18px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  text-align: left;
  background: rgba(245, 248, 252, 0.9);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.document-row:hover {
  transform: translateY(-1px);
  border-color: rgba(13, 124, 124, 0.2);
  box-shadow: 0 14px 30px rgba(23, 48, 79, 0.08);
}

.document-row.active {
  transform: translateY(-2px);
  border-color: rgba(13, 124, 124, 0.42);
  background: linear-gradient(135deg, rgba(13, 124, 124, 0.14), rgba(23, 48, 79, 0.08));
  box-shadow:
    0 18px 34px rgba(13, 124, 124, 0.14),
    inset 0 0 0 1px rgba(13, 124, 124, 0.16);
}

.document-row.active::before {
  content: '';
  position: absolute;
  inset: 10px auto 10px 10px;
  width: 5px;
  border-radius: 999px;
  background: linear-gradient(180deg, #0d7c7c, #17304f);
  box-shadow: 0 8px 18px rgba(13, 124, 124, 0.24);
}

.document-row.active .document-row-main {
  padding-left: 12px;
}

.document-row-main {
  min-width: 0;
}

.document-row-title {
  gap: 10px;
  justify-content: flex-start;
}

.document-row-title strong {
  color: #13283f;
}

.document-row.active .document-row-title strong {
  color: #0f3d56;
}

.document-row-title span,
.document-row-main p,
.document-row-meta {
  color: #677f97;
}

.document-row.active .document-row-title span,
.document-row.active .document-row-main p,
.document-row.active .document-row-meta {
  color: #4f6b83;
}

.document-row-main p {
  margin: 8px 0;
  word-break: break-all;
}

.document-row-meta,
.document-row-status,
.detail-statuses,
.preview-tags {
  gap: 8px;
  flex-wrap: wrap;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-subtitle {
  margin: 10px 0 0;
  color: #69819b;
  word-break: break-all;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(245, 248, 252, 0.92);
}

.meta-item span,
.reason-card span,
.preview-box span,
.selected-flow-label {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #69839d;
}

.meta-item strong {
  display: block;
  margin-top: 10px;
  color: #13283f;
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
  border-radius: 20px;
  border: 1px solid rgba(23, 48, 79, 0.08);
  background: rgba(255, 255, 255, 0.9);
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.strategy-status-index {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
  font-size: 13px;
  font-weight: 900;
}

.strategy-status-copy strong {
  display: block;
  color: #13283f;
  font-size: 14px;
  font-weight: 800;
}

.strategy-status-copy span {
  display: block;
  margin-top: 6px;
  color: #64798f;
  font-size: 12px;
  line-height: 1.55;
}

.strategy-status-step-completed {
  border-color: rgba(10, 109, 122, 0.16);
  background: linear-gradient(135deg, rgba(10, 109, 122, 0.06), rgba(255, 255, 255, 0.96));
}

.strategy-status-step-completed .strategy-status-index {
  background: rgba(10, 109, 122, 0.14);
  color: #0a6d7a;
}

.strategy-status-step-current {
  border-color: rgba(178, 31, 196, 0.16);
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.06), rgba(255, 255, 255, 0.96));
}

.strategy-status-step-current .strategy-status-index {
  background: rgba(178, 31, 196, 0.14);
  color: #b21fc4;
}

.strategy-status-step-failed {
  border-color: rgba(194, 65, 12, 0.16);
  background: linear-gradient(135deg, rgba(194, 65, 12, 0.06), rgba(255, 255, 255, 0.96));
}

.strategy-status-step-failed .strategy-status-index {
  background: rgba(194, 65, 12, 0.14);
  color: #c2410c;
}

.strategy-section-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.strategy-intro {
  margin-top: 18px;
  padding: 2px 0 24px;
  border-bottom: 1px solid rgba(23, 48, 79, 0.08);
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
  color: #6b839d;
}

.strategy-intro-copy,
.strategy-adjust-description,
.strategy-lane-description {
  margin: 10px 0 0;
  color: #4f647b;
  line-height: 1.85;
  font-size: 15px;
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
  border-top: 1px dashed rgba(23, 48, 79, 0.1);
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

.strategy-lane-titlebox h5,
.strategy-adjust-titlebox h5 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.03em;
  line-height: 1.08;
}

.strategy-lane-parent .strategy-lane-kicker,
.strategy-lane-parent .strategy-lane-titlebox h5 {
  color: #b21fc4;
}

.strategy-lane-child .strategy-lane-kicker,
.strategy-lane-child .strategy-lane-titlebox h5 {
  color: #0d7c7c;
}

.strategy-lane-description,
.strategy-adjust-description {
  max-width: 460px;
  text-align: right;
}

.strategy-adjust-shell {
  margin-top: 34px;
  padding: 26px 22px 22px;
  border-top: 1px solid rgba(23, 48, 79, 0.08);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(23, 48, 79, 0.03), rgba(23, 48, 79, 0.015));
}

.strategy-adjust-shell .strategy-flow-stack-edit {
  gap: 32px;
}

.strategy-lane-edit {
  padding: 18px 18px 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(23, 48, 79, 0.06);
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
  color: #13283f;
}

.strategy-section-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.strategy-intro {
  margin-top: 18px;
  padding: 0 0 22px;
  border-bottom: 1px solid rgba(23, 48, 79, 0.08);
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
  color: #6b839d;
}

.strategy-intro-copy,
.strategy-adjust-description,
.strategy-lane-description {
  margin: 10px 0 0;
  color: #4f647b;
  line-height: 1.85;
  font-size: 15px;
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
  border-top: 1px dashed rgba(23, 48, 79, 0.1);
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

.strategy-lane-titlebox h5,
.strategy-adjust-titlebox h5 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.03em;
  line-height: 1.08;
}

.strategy-lane-parent .strategy-lane-kicker,
.strategy-lane-parent .strategy-lane-titlebox h5 {
  color: #b21fc4;
}

.strategy-lane-child .strategy-lane-kicker,
.strategy-lane-child .strategy-lane-titlebox h5 {
  color: #0d7c7c;
}

.strategy-lane-description,
.strategy-adjust-description {
  max-width: 460px;
  text-align: right;
}

.strategy-adjust-shell {
  margin-top: 34px;
  padding-top: 26px;
  border-top: 1px solid rgba(23, 48, 79, 0.08);
}

.section-headline span {
  color: #6d8299;
  font-size: 13px;
}

.reason-card,
.preview-box,
.selected-flow-board {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(245, 248, 252, 0.92);
}

.reason-card p {
  margin: 10px 0 0;
  color: #405972;
  line-height: 1.75;
}

.timeline-list,
.summary-log-list,
.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.timeline-item,
.summary-log-item,
.chunk-item {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(21, 49, 75, 0.08);
}

.chunk-item {
  flex-direction: column;
}

.chunk-head,
.chunk-title-group,
.chunk-status-group,
.chunk-meta {
  display: flex;
  align-items: center;
}

.chunk-head {
  justify-content: space-between;
  gap: 12px;
}

.chunk-title-group,
.chunk-status-group,
.chunk-meta {
  gap: 10px;
  flex-wrap: wrap;
}

.chunk-title-group strong {
  color: #13283f;
}

.chunk-title-group span,
.chunk-meta span {
  color: #627b94;
  font-size: 13px;
}

.chunk-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.chunk-chip-1 {
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
}

.chunk-chip-2 {
  background: rgba(13, 124, 124, 0.12);
  color: #0d7c7c;
}

.chunk-chip-3 {
  background: rgba(10, 109, 122, 0.12);
  color: #0a6d7a;
}

.chunk-chip-4 {
  background: rgba(194, 65, 12, 0.12);
  color: #c2410c;
}

.chunk-body {
  margin: 0;
  color: #3f576f;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.timeline-index {
  width: 38px;
  height: 38px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(13, 124, 124, 0.1);
  color: #0d7c7c;
  font-weight: 800;
}

.timeline-main strong,
.summary-log-head strong,
.drawer-log-head strong {
  color: #13283f;
}

.timeline-main p,
.summary-log-item p,
.summary-log-head span,
.drawer-log-head span,
.drawer-subtitle {
  color: #64798f;
}

.timeline-main p,
.summary-log-item p {
  margin: 8px 0 0;
}

.editor-headline {
  margin-top: 20px;
}

.selected-flow-board {
  margin-top: 14px;
  border: 1px solid rgba(21, 49, 75, 0.08);
}

.selected-flow-board-parent {
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.05), rgba(178, 31, 196, 0.015));
}

.selected-flow-board-child {
  background: linear-gradient(135deg, rgba(13, 124, 124, 0.05), rgba(13, 124, 124, 0.015));
}

.timeline-list-parent .timeline-item {
  border-color: rgba(178, 31, 196, 0.12);
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.04), rgba(255, 255, 255, 0.96));
}

.timeline-list-parent .timeline-index {
  background: rgba(178, 31, 196, 0.12);
  color: #b21fc4;
}

.timeline-list-child .timeline-item {
  border-color: rgba(13, 124, 124, 0.12);
  background: linear-gradient(135deg, rgba(13, 124, 124, 0.04), rgba(255, 255, 255, 0.96));
}

.timeline-list-child .timeline-index {
  background: rgba(13, 124, 124, 0.12);
  color: #0d7c7c;
}

.selected-flow-label {
  font-family: var(--font-display);
  font-size: 24px !important;
  font-weight: 900;
  letter-spacing: -0.03em !important;
  text-transform: none !important;
}

.selected-flow-label-parent {
  color: #b21fc4 !important;
}

.selected-flow-label-child {
  color: #0d7c7c !important;
}

.sequence-board {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-flow-sequence {
  margin-top: 14px;
}

.build-stage-board {
  margin-top: 18px;
}

.sequence-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.sequence-inline-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  color: #0b7f7f;
  font-size: 42px;
  font-weight: 900;
  line-height: 1;
  text-shadow: 0 4px 12px rgba(11, 127, 127, 0.18);
}

.sequence-inline-arrow-empty {
  visibility: hidden;
}

.sequence-down-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  align-items: center;
  min-height: 44px;
}

.sequence-down-row-left .sequence-down-arrow {
  grid-column: 1;
}

.sequence-down-row-right .sequence-down-arrow {
  grid-column: 3;
}

.sequence-down-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0b7f7f;
  font-size: 46px;
  font-weight: 900;
  line-height: 1;
  text-shadow: 0 4px 12px rgba(11, 127, 127, 0.18);
}

.sequence-card {
  width: 100%;
}

.sequence-card-placeholder {
  min-height: 1px;
}

.selected-flow-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border: 1px solid rgba(23, 48, 79, 0.14);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 12px 26px rgba(23, 48, 79, 0.08);
}

.selected-flow-board-parent .selected-flow-card {
  border-color: rgba(178, 31, 196, 0.14);
  box-shadow: 0 12px 24px rgba(178, 31, 196, 0.07);
}

.selected-flow-board-parent .selected-flow-order {
  background: linear-gradient(135deg, #680f67, #b21fc4);
}

.selected-flow-board-child .selected-flow-card {
  border-color: rgba(13, 124, 124, 0.14);
  box-shadow: 0 12px 24px rgba(13, 124, 124, 0.07);
}

.selected-flow-board-child .selected-flow-order {
  background: linear-gradient(135deg, #0a6d7a, #14b8a6);
}

.selected-flow-order {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #17304f, #0d7c7c);
  color: #ffffff;
}

.selected-flow-content strong {
  display: block;
  font-size: 18px;
  color: #17304f;
}

.selected-flow-content span {
  display: block;
  margin-top: 8px;
  color: #637a91;
  font-size: 13px;
  line-height: 1.6;
}

.selected-flow-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.flow-action-button {
  min-width: 58px;
  padding: 8px 10px;
  border: 1px solid rgba(23, 48, 79, 0.12);
  border-radius: 12px;
  background: rgba(245, 248, 252, 0.96);
  color: #17304f;
  font-size: 12px;
  font-weight: 700;
}

.flow-action-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.selected-flow-empty,
.preview-empty {
  margin: 14px 0 0;
  color: #64798f;
  line-height: 1.7;
}

.strategy-picker {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.strategy-picker-parent .strategy-chip.active {
  border-color: rgba(178, 31, 196, 0.4);
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.14), rgba(178, 31, 196, 0.04));
  box-shadow: 0 4px 16px rgba(178, 31, 196, 0.16);
}

.strategy-picker-parent .strategy-chip.active .strategy-chip-state {
  background: rgba(178, 31, 196, 0.16);
  color: #b21fc4;
}

.strategy-picker-parent .strategy-chip-check {
  color: #b21fc4;
}

.strategy-picker-child .strategy-chip.active {
  border-color: rgba(13, 124, 124, 0.4);
  background: linear-gradient(135deg, rgba(13, 124, 124, 0.14), rgba(13, 124, 124, 0.04));
  box-shadow: 0 4px 16px rgba(13, 124, 124, 0.16);
}

.strategy-picker-child .strategy-chip.active .strategy-chip-state {
  background: rgba(13, 124, 124, 0.16);
  color: #0d7c7c;
}

.strategy-picker-child .strategy-chip-check {
  color: #0d7c7c;
}

.strategy-chip {
  text-align: left;
  border: 1px solid rgba(21, 49, 75, 0.1);
  border-radius: 20px;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.92);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.strategy-chip.active {
  border-color: rgba(13, 124, 124, 0.5);
  background: linear-gradient(135deg, rgba(13, 124, 124, 0.16), rgba(23, 48, 79, 0.1));
  box-shadow: 0 4px 16px rgba(13, 124, 124, 0.2);
}

.strategy-chip strong {
  display: block;
  color: #17304f;
}

.strategy-chip span {
  display: block;
  margin-top: 8px;
  color: #637a91;
  font-size: 13px;
  line-height: 1.6;
}

.strategy-chip-top {
  justify-content: space-between;
  gap: 10px;
}

.strategy-chip-state {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.strategy-chip-check {
  width: 22px;
  height: 22px;
  color: #0d7c7c;
}

.strategy-chip.active strong,
.strategy-chip.active span,
.strategy-chip.active .strategy-chip-state {
  color: #17304f;
}

.strategy-chip.active .strategy-chip-state {
  background: rgba(13, 124, 124, 0.2);
  color: #075e5e;
}

.preview-box {
  margin-top: 16px;
  background: rgba(13, 124, 124, 0.06);
  border: 1px solid rgba(13, 124, 124, 0.18);
  border-radius: 14px;
  padding: 16px 18px;
}

.preview-box-parent {
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.05), rgba(178, 31, 196, 0.015));
  border-color: rgba(178, 31, 196, 0.18);
}

.preview-box-child {
  background: linear-gradient(135deg, rgba(13, 124, 124, 0.05), rgba(13, 124, 124, 0.015));
  border-color: rgba(13, 124, 124, 0.18);
}

.preview-box .preview-box-title {
  font-weight: 800;
  font-family: var(--font-display);
  font-size: 22px !important;
  font-weight: 900;
  letter-spacing: -0.03em;
  text-transform: none !important;
}

.preview-box-title-parent {
  color: #b21fc4 !important;
}

.preview-box-title-child {
  color: #0d7c7c !important;
}

.preview-flow {
  margin-top: 12px;
  gap: 10px;
  flex-wrap: wrap;
}

.preview-tag {
  padding: 8px 16px;
  border-radius: 999px;
  background: #fff7ed;
  border: 1px solid rgba(194, 120, 3, 0.35);
  color: #92400e;
  font-size: 13px;
  font-weight: 700;
}

.preview-arrow {
  width: 18px;
  height: 18px;
  color: #0d7c7c;
  flex: none;
}

.flow-arrow-icon {
  width: 30px;
  height: 30px;
  color: #0b7f7f;
  flex: none;
  stroke-width: 2.6;
  filter: drop-shadow(0 6px 14px rgba(11, 127, 127, 0.18));
}

.flow-arrow {
  display: flex;
  justify-content: center;
  margin: 6px 0;
}

.confirm-actions {
  margin-top: 16px;
  flex-direction: column;
  align-items: stretch;
}

.adjust-input {
  flex: 1;
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
  border-radius: 20px;
  border: 1px solid rgba(21, 49, 75, 0.1);
  background: rgba(255, 255, 255, 0.92);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
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
  border-radius: 14px;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.action-stage-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.action-stage-card strong {
  color: #17304f;
  font-size: 18px;
}

.action-stage-card p {
  margin: 0;
  color: #4f6880;
  line-height: 1.7;
}

.action-stage-ready {
  border-color: rgba(13, 124, 124, 0.18);
  box-shadow: 0 12px 24px rgba(13, 124, 124, 0.08);
}

.action-stage-ready .action-stage-index,
.action-stage-ready .action-stage-badge {
  background: rgba(13, 124, 124, 0.12);
  color: #0d7c7c;
}

.action-stage-current {
  border-color: rgba(13, 124, 124, 0.24);
  background: linear-gradient(135deg, rgba(23, 48, 79, 0.94), rgba(13, 124, 124, 0.92));
  box-shadow: 0 18px 30px rgba(23, 48, 79, 0.16);
}

.action-stage-current .action-stage-index,
.action-stage-current .action-stage-badge {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

.action-stage-current strong,
.action-stage-current p {
  color: #ffffff;
}

.action-stage-completed {
  border-color: rgba(10, 109, 122, 0.18);
  background: linear-gradient(135deg, rgba(10, 109, 122, 0.12), rgba(255, 255, 255, 0.96));
}

.action-stage-completed .action-stage-index,
.action-stage-completed .action-stage-badge {
  background: rgba(10, 109, 122, 0.14);
  color: #0a6d7a;
}

.action-stage-locked {
  border-style: dashed;
  border-color: rgba(21, 49, 75, 0.16);
  background: rgba(245, 248, 252, 0.88);
}

.action-stage-locked .action-stage-badge {
  background: rgba(23, 48, 79, 0.08);
  color: #5d7389;
}

.action-stage-card .action-button {
  width: 100%;
  justify-content: center;
}

.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 18px;
  padding: 13px 20px;
  font-weight: 800;
  color: #ffffff;
  box-shadow: 0 16px 28px rgba(23, 48, 79, 0.14);
}

.action-button-confirm {
  background: linear-gradient(135deg, #0a6d7a, #075563);
}

.action-button-build {
  background: linear-gradient(135deg, #c2410c, #ea580c);
}

.action-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.action-stage-completed .action-button-confirm {
  background: linear-gradient(135deg, #0a6d7a, #0c7f8f);
  box-shadow: 0 12px 20px rgba(10, 109, 122, 0.18);
}

.action-stage-completed .action-button-confirm:disabled,
.action-stage-locked .action-button-build:disabled {
  opacity: 0.88;
}

.action-stage-locked .action-button-build {
  border: 1px dashed rgba(23, 48, 79, 0.16);
  background: rgba(255, 255, 255, 0.9);
  color: #17304f;
}

.primary-button,
.ghost-button {
  border: none;
  border-radius: 16px;
  padding: 12px 18px;
  font-weight: 700;
}

.primary-button {
  color: #ffffff;
  background: linear-gradient(135deg, #17304f, #0d7c7c);
}

.ghost-button {
  color: #17304f;
  background: rgba(23, 48, 79, 0.08);
}

.inline-notice {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 16px;
}

.inline-notice-danger {
  background: rgba(194, 65, 12, 0.1);
  color: #c2410c;
}

.empty-block {
  min-height: 260px;
  display: grid;
  place-items: center;
  text-align: center;
  color: #6e849c;
  border-radius: 22px;
  border: 1px dashed rgba(21, 49, 75, 0.14);
}

.compact-empty {
  min-height: 140px;
  margin-top: 14px;
}

.build-progress-card {
  padding: 18px 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(23, 48, 79, 0.08), rgba(13, 124, 124, 0.08));
  border: 1px solid rgba(13, 124, 124, 0.14);
}

.build-progress-card-inline {
  margin-top: 18px;
  scroll-margin-top: 24px;
}

.build-progress-header {
  justify-content: space-between;
  gap: 16px;
}

.build-progress-header strong {
  display: block;
  margin-top: 6px;
  color: #17304f;
}

.build-pulse {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(13, 124, 124, 0.14);
  color: #0d7c7c;
  font-size: 12px;
  font-weight: 800;
}

.build-pulse-static {
  background: rgba(23, 48, 79, 0.1);
  color: #17304f;
}

.build-progress-text {
  margin: 10px 0 0;
  color: #58708a;
  line-height: 1.7;
}

.stage-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(21, 49, 75, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.stage-order {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.08em;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
}

.stage-order > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.stage-body strong {
  display: block;
  color: #17304f;
}

.stage-body span {
  display: block;
  margin-top: 8px;
  color: #64798f;
  font-size: 13px;
  line-height: 1.6;
}

.stage-body em {
  display: inline-flex;
  margin-top: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-style: normal;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stage-current {
  border-color: rgba(13, 124, 124, 0.28);
  background: linear-gradient(135deg, rgba(23, 48, 79, 0.94), rgba(13, 124, 124, 0.92));
  box-shadow: 0 16px 30px rgba(23, 48, 79, 0.14);
}

.stage-current .stage-order {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

.stage-current .stage-body strong,
.stage-current .stage-body span {
  color: #ffffff;
}

.stage-current .stage-body em {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

.stage-current .stage-order .stage-spinner {
  border-color: rgba(255, 255, 255, 0.32);
  border-top-color: #ffffff;
}

.stage-completed .stage-order {
  background: rgba(13, 124, 124, 0.12);
  color: #0d7c7c;
}

.stage-completed .stage-body em {
  background: rgba(13, 124, 124, 0.1);
  color: #0d7c7c;
}

.stage-failed {
  border-color: rgba(194, 65, 12, 0.18);
  background: rgba(255, 247, 237, 0.96);
}

.stage-failed .stage-order {
  background: rgba(194, 65, 12, 0.1);
  color: #c2410c;
}

.stage-failed .stage-body em {
  background: rgba(194, 65, 12, 0.1);
  color: #c2410c;
}

.tracker-footer {
  margin-top: 16px;
  gap: 10px;
  flex-wrap: wrap;
}

.tracker-footer span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #58708a;
  font-size: 12px;
  font-weight: 700;
}

.summary-log-button {
  align-self: flex-start;
}

.stage-spinner,
.build-overlay-spinner {
  border-radius: 50%;
  animation: spin 0.86s linear infinite;
}

.stage-spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(23, 48, 79, 0.22);
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
  background: rgba(10, 22, 35, 0.58);
  backdrop-filter: blur(6px);
}

.build-overlay-card {
  width: min(760px, 100%);
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(23, 48, 79, 0.96), rgba(13, 124, 124, 0.92));
  box-shadow: 0 28px 54px rgba(12, 30, 48, 0.32);
  color: #ffffff;
}

.build-overlay-head {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.build-overlay-head h3 {
  margin: 0;
  font-size: clamp(28px, 3vw, 34px);
  line-height: 1.1;
}

.build-overlay-spinner {
  width: 56px;
  height: 56px;
  border: 4px solid rgba(255, 255, 255, 0.22);
  border-top-color: #ffffff;
}

.build-overlay-text,
.build-overlay-tip {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.7;
}

.build-overlay-task-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.build-overlay-task-meta span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.94);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
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
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
}

.build-overlay-stage-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  font-size: 15px;
  font-weight: 900;
}

.build-overlay-stage-copy strong {
  display: block;
  color: #ffffff;
}

.build-overlay-stage-copy span {
  display: block;
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
}

.build-overlay-stage-current {
  border-color: rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.14);
}

.build-overlay-stage-current .build-overlay-stage-icon {
  background: rgba(255, 255, 255, 0.18);
}

.build-overlay-stage-current .stage-spinner {
  border-color: rgba(255, 255, 255, 0.32);
  border-top-color: #ffffff;
}

.build-overlay-stage-completed .build-overlay-stage-icon {
  background: rgba(94, 234, 212, 0.16);
  color: #99f6e4;
}

.build-overlay-stage-failed .build-overlay-stage-icon {
  background: rgba(248, 113, 113, 0.16);
  color: #fecaca;
}

.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(10, 22, 35, 0.42);
  z-index: 18;
}

.log-drawer {
  position: fixed;
  top: 18px;
  right: 18px;
  bottom: 18px;
  width: min(560px, calc(100vw - 36px));
  z-index: 19;
  border: 1px solid rgba(21, 49, 75, 0.08);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 54px rgba(12, 30, 48, 0.16);
  backdrop-filter: blur(20px);
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

.drawer-header h3 {
  margin: 0;
  color: #13283f;
}

.icon-button {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(23, 48, 79, 0.08);
}

.drawer-icon {
  width: 20px;
  height: 20px;
  color: #17304f;
}

.drawer-summary {
  margin-top: 18px;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-chip {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(245, 248, 252, 0.98);
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-chip span {
  color: #6a8199;
  font-size: 12px;
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

.drawer-log-node {
  position: relative;
  width: 18px;
}

.drawer-log-node::before {
  content: '';
  position: absolute;
  top: 6px;
  left: 6px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0d7c7c;
  box-shadow: 0 0 0 4px rgba(13, 124, 124, 0.12);
}

.drawer-log-node::after {
  content: '';
  position: absolute;
  top: 18px;
  left: 8px;
  bottom: -18px;
  width: 2px;
  background: linear-gradient(180deg, rgba(13, 124, 124, 0.22), rgba(23, 48, 79, 0.08));
}

.drawer-log-item:last-child .drawer-log-node::after {
  display: none;
}

.drawer-log-body {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(245, 248, 252, 0.96);
}

.drawer-log-head {
  justify-content: space-between;
  gap: 12px;
}

.drawer-log-body p {
  margin: 10px 0 0;
  color: #4d647c;
  line-height: 1.7;
}

.drawer-log-detail {
  margin: 12px 0 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: #ffffff;
  color: #4d647c;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.26s ease, opacity 0.26s ease;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
  transform: translateX(24px);
}

.build-mask-fade-enter-active,
.build-mask-fade-leave-active {
  transition: opacity 0.22s ease;
}

.build-mask-fade-enter-from,
.build-mask-fade-leave-to {
  opacity: 0;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

.file-input::file-selector-button {
  border: none;
  border-radius: 12px;
  padding: 8px 12px;
  margin-right: 12px;
  background: rgba(23, 48, 79, 0.08);
  color: #17304f;
}

@media (max-width: 1180px) {
  .top-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .strategy-status-bar {
    grid-template-columns: 1fr;
  }

  .upload-grid,
  .strategy-picker,
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .list-toolbar,
  .detail-header,
  .build-progress-header,
  .section-headline,
  .strategy-lane-header,
  .strategy-adjust-header,
  .detail-secondary-actions,
  .confirm-actions,
  .document-row,
  .document-row-status,
  .drawer-log-head,
  .tracker-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    min-width: 0;
    width: 100%;
  }

  .chunk-head {
    flex-direction: column;
    align-items: stretch;
  }

  .selected-flow-card,
  .stage-card {
    min-width: 100%;
  }

  .sequence-row {
    grid-template-columns: 1fr;
  }

  .sequence-inline-arrow,
  .sequence-down-row {
    justify-content: center;
  }

  .sequence-down-row {
    grid-template-columns: 1fr;
  }

  .sequence-down-row-left .sequence-down-arrow,
  .sequence-down-row-right .sequence-down-arrow {
    grid-column: 1;
  }

  .strategy-submit-actions,
  .build-overlay-stage-list {
    grid-template-columns: 1fr;
  }

  .sequence-card-placeholder {
    display: none;
  }

  .log-drawer {
    top: 10px;
    right: 10px;
    bottom: 10px;
    width: calc(100vw - 20px);
    padding: 18px;
  }

  .build-overlay {
    padding: 16px;
  }

  .build-overlay-card {
    padding: 20px;
  }

  .build-overlay-head {
    grid-template-columns: 1fr;
  }
}
</style>
