<template>
  <section class="document-page">
    <div class="top-grid">
      <article class="panel-card upload-card">
        <div class="panel-title">
          <div>
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

        <div class="upload-footer">
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
        </div>
      </article>

      <article class="panel-card tips-card">
        <div class="panel-title">
          <div>
            <h3>建议操作顺序</h3>
          </div>
        </div>

        <ul class="tips-list">
          <li>先上传文档，系统会异步解析并生成推荐切块策略。</li>
          <li>点击任意文档，进入单独详情页查看解析结果、Chunk 和任务轨迹。</li>
          <li>在详情页确认策略并构建索引，列表页专注浏览和筛选。</li>
        </ul>
      </article>
    </div>

    <div v-if="pageNotice.message" class="page-notice" :class="`page-notice-${pageNotice.type}`">
      {{ pageNotice.message }}
    </div>

    <article class="panel-card list-card">
      <div class="list-toolbar">
        <div>
          <h3>文档列表</h3>
          <p class="toolbar-caption">共 {{ total }} 份文档，当前第 {{ currentPage }} 页。</p>
        </div>

        <div class="list-actions">
          <input
            v-model="keyword"
            class="search-input"
            type="text"
            placeholder="搜索文档名称或原始文件名"
            @keydown.enter="submitSearch"
          />
          <button class="ghost-button" type="button" @click="submitSearch">搜索</button>
        </div>
      </div>

      <div class="table-summary">
        <article class="table-stat-card">
          <span>当前页文档</span>
          <strong>{{ documents.length }}</strong>
        </article>
        <article class="table-stat-card">
          <span>解析完成</span>
          <strong>{{ visibleParseReadyCount }}</strong>
        </article>
        <article class="table-stat-card">
          <span>策略确认</span>
          <strong>{{ visibleStrategyReadyCount }}</strong>
        </article>
        <article class="table-stat-card">
          <span>索引可用</span>
          <strong>{{ visibleIndexReadyCount }}</strong>
        </article>
      </div>

      <div class="document-table-shell">
        <div v-if="!listLoading && !documents.length" class="empty-block">
          还没有文档，先上传一份资料开始体验。
        </div>
        <div v-if="listLoading" class="empty-block">正在加载文档列表...</div>

        <div v-if="!listLoading && documents.length" class="document-table-scroll">
          <table class="document-table">
            <thead>
              <tr>
                <th>文档</th>
                <th>类型</th>
                <th>大小</th>
                <th>更新时间</th>
                <th>解析</th>
                <th>策略</th>
                <th>索引</th>
                <th class="document-table-action-head">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in documents" :key="item.documentId" class="document-table-row">
                <td class="document-cell document-cell-main">
                  <button class="document-link-button" type="button" @click="openDocumentDetail(item.documentId)">
                    <strong>{{ item.documentName }}</strong>
                    <span>{{ item.originalFileName }}</span>
                  </button>
                </td>
                <td class="document-cell">
                  <span class="table-chip">{{ item.fileTypeName || '-' }}</span>
                </td>
                <td class="document-cell">
                  <strong>{{ formatFileSize(item.fileSize) }}</strong>
                </td>
                <td class="document-cell">
                  <strong>{{ formatDateTime(item.editTime) }}</strong>
                </td>
                <td class="document-cell">
                  <AdminStatusBadge :label="item.parseStatusName" :code="item.parseStatus" type="parse" />
                </td>
                <td class="document-cell">
                  <AdminStatusBadge :label="item.strategyStatusName" :code="item.strategyStatus" type="strategy" />
                </td>
                <td class="document-cell">
                  <AdminStatusBadge :label="item.indexStatusName" :code="item.indexStatus" type="index" />
                </td>
                <td class="document-cell document-cell-action">
                  <div class="document-action-group">
                    <button class="detail-link" type="button" @click="openDocumentDetail(item.documentId)">查看详情</button>
                    <button
                      class="danger-link"
                      type="button"
                      :disabled="!canDeleteDocument(item)"
                      :title="buildDeleteTitle(item)"
                      @click="deleteDocument(item)"
                    >
                      {{ isDeletingDocument(item.documentId) ? '删除中...' : '删除' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="documents.length" class="pagination-bar">
        <button class="ghost-button" type="button" :disabled="currentPage <= 1 || listLoading" @click="changePage(currentPage - 1)">
          上一页
        </button>
        <div class="pagination-status">
          <strong>第 {{ currentPage }} / {{ totalPages }} 页</strong>
          <span>共 {{ total }} 条文档</span>
        </div>
        <button class="ghost-button" type="button" :disabled="currentPage >= totalPages || listLoading" @click="changePage(currentPage + 1)">
          下一页
        </button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { APIError, manageApi } from '../../api/api'
import AdminStatusBadge from '../../components/admin/AdminStatusBadge.vue'
import { formatDateTime, formatFileSize, hasCode } from '../../utils/manageFormat'

const router = useRouter()
const OPERATOR_ID = '10001'
const DEFAULT_PAGE_SIZE = 12

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
const keyword = ref('')
const documents = ref([])
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const total = ref(0)
const deletingDocumentId = ref('')
const pageNotice = reactive({
  type: 'info',
  message: ''
})

const totalPages = computed(() => {
  return Math.max(1, Math.ceil((total.value || 0) / pageSize.value))
})
const visibleParseReadyCount = computed(() => documents.value.filter((item) => hasCode(item.parseStatus, 3)).length)
const visibleStrategyReadyCount = computed(() => documents.value.filter((item) => hasCode(item.strategyStatus, 3)).length)
const visibleIndexReadyCount = computed(() => documents.value.filter((item) => hasCode(item.indexStatus, 3)).length)

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

async function loadDocuments(page = currentPage.value) {
  listLoading.value = true

  try {
    const data = await manageApi.queryDocumentPage({
      pageNo: page,
      pageSize: pageSize.value,
      keyword: keyword.value.trim()
    })
    documents.value = Array.isArray(data?.records) ? data.records : []
    currentPage.value = Number(data?.pageNo || page)
    pageSize.value = Number(data?.pageSize || pageSize.value)
    total.value = Number(data?.total || 0)
  } catch (error) {
    console.error('加载文档列表失败', error)
    showNotice(normalizeError(error, '加载文档列表失败'), 'danger')
    documents.value = []
  } finally {
    listLoading.value = false
  }
}

function submitSearch() {
  currentPage.value = 1
  loadDocuments(1)
}

function changePage(page) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) {
    return
  }
  loadDocuments(page)
}

function openDocumentDetail(documentId) {
  router.push({
    name: 'AdminDocumentDetail',
    params: {
      documentId: String(documentId)
    }
  })
}

function isDeletingDocument(documentId) {
  return String(deletingDocumentId.value || '') === String(documentId || '')
}

function hasRunningDocumentTask(item) {
  return hasCode(item?.latestTaskStatus, 1)
    || hasCode(item?.latestTaskStatus, 2)
    || hasCode(item?.parseStatus, 2)
    || hasCode(item?.indexStatus, 2)
}

function canDeleteDocument(item) {
  if (!item?.documentId) {
    return false
  }
  return !listLoading.value && !deletingDocumentId.value && !hasRunningDocumentTask(item)
}

function buildDeleteTitle(item) {
  if (hasRunningDocumentTask(item)) {
    return '请等待当前任务完成后再删除'
  }
  if (deletingDocumentId.value) {
    return '当前有文档正在删除'
  }
  return '删除文档以及关联的索引、存储文件'
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
    clearSelectedFile()
    showNotice(`文档已上传，任务 ${result.taskId} 已进入解析与策略推荐队列。`, 'success')
    await loadDocuments(1)
    openDocumentDetail(result.documentId)
  } catch (error) {
    console.error('上传文档失败', error)
    showNotice(normalizeError(error, '上传文档失败'), 'danger')
  } finally {
    uploading.value = false
  }
}

async function deleteDocument(item) {
  if (!item?.documentId) {
    return
  }

  if (hasRunningDocumentTask(item)) {
    showNotice('当前文档存在进行中的任务，请等待任务完成后再删除。', 'danger')
    return
  }

  const documentId = String(item.documentId)
  const documentName = item.documentName || item.originalFileName || documentId
  const confirmed = window.confirm(
    `确认删除文档《${documentName}》吗？\n\n将同时删除 MySQL 记录、向量库数据和 MinIO 存储文件，删除后不可恢复。`
  )
  if (!confirmed) {
    return
  }

  deletingDocumentId.value = documentId
  clearNotice()

  try {
    await manageApi.deleteDocument({
      documentId
    })
    const nextPage = documents.value.length === 1 && currentPage.value > 1
      ? currentPage.value - 1
      : currentPage.value
    await loadDocuments(nextPage)
    showNotice(`文档《${documentName}》已删除，关联数据已同步清理。`, 'success')
  } catch (error) {
    console.error('删除文档失败', error)
    showNotice(normalizeError(error, '删除文档失败'), 'danger')
  } finally {
    deletingDocumentId.value = ''
  }
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

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.document-page {
  --color-border: var(--border);
  --color-surface-soft: var(--muted);
  --radius-lg: var(--radius-token-card);
  --radius-md: var(--radius-token-md);
  --radius-sm: var(--radius-token-sm);
  --shadow-sm: var(--shadow-control);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.top-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 16px;
  align-items: stretch;
}

.panel-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.panel-title,
.list-toolbar,
.upload-actions,
.list-actions,
.pagination-bar {
  display: flex;
  align-items: center;
}

.panel-title,
.list-toolbar,
.pagination-bar {
  justify-content: space-between;
  gap: 12px;
}

.panel-title h3,
.list-toolbar h3 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 16px;
  font-weight: 600;
}

.toolbar-caption {
  margin: 8px 0 0;
  color: var(--color-muted);
  font-size: 14px;
}

.upload-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field span {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-muted-strong);
}

.field input,
.search-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  background: #ffffff;
  outline: none;
  color: var(--color-text);
}

.field input:focus,
.search-input:focus {
  border-color: rgba(178, 31, 196, 0.28);
  box-shadow: 0 0 0 3px rgba(178, 31, 196, 0.08);
}

.upload-hint {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  flex: 1;
  min-width: 0;
}

.upload-hint span {
  display: block;
  color: var(--color-muted);
  font-size: 13px;
}

.upload-hint strong {
  display: block;
  margin-top: 8px;
  color: var(--color-text-strong);
  word-break: break-all;
}

.upload-actions,
.list-actions {
  gap: 12px;
}

.upload-footer {
  margin-top: 14px;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 12px;
}

.upload-actions {
  margin-top: 0;
  justify-content: flex-end;
  flex: none;
  align-self: center;
}

.tips-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--color-muted-strong);
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 14px;
  line-height: 1.7;
}

.page-notice {
  padding: 14px 18px;
  border-radius: var(--radius-md);
  font-weight: 600;
  border: 1px solid transparent;
}

.page-notice-info {
  background: rgba(178, 31, 196, 0.08);
  color: #8c168d;
  border-color: rgba(178, 31, 196, 0.1);
}

.page-notice-success {
  background: rgba(18, 125, 74, 0.1);
  color: #127d4a;
  border-color: rgba(18, 125, 74, 0.12);
}

.page-notice-danger {
  background: rgba(177, 47, 38, 0.1);
  color: #b12f26;
  border-color: rgba(177, 47, 38, 0.12);
}

.table-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.table-stat-card {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
}

.table-stat-card span {
  display: block;
  font-size: 12px;
  color: var(--color-muted);
}

.table-stat-card strong {
  display: block;
  margin-top: 8px;
  color: var(--color-text-strong);
  font-size: 24px;
  line-height: 1.15;
}

.document-table-shell {
  margin-top: 14px;
  min-height: 420px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #fff;
}

.document-table-scroll {
  overflow-x: auto;
}

.document-table {
  width: 100%;
  min-width: 1080px;
  border-collapse: collapse;
}

.document-table thead th {
  padding: 14px 16px;
  background: var(--color-surface-soft);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 600;
  text-align: left;
  white-space: nowrap;
}

.document-table-row {
  transition: background-color 0.22s ease;
}

.document-table-row:hover {
  background: rgba(178, 31, 196, 0.04);
}

.document-table-row td {
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: top;
}

.document-table-row:last-child td {
  border-bottom: none;
}

.document-cell {
  color: var(--color-muted-strong);
}

.document-cell strong {
  display: block;
  color: var(--color-text-strong);
  line-height: 1.45;
}

.document-cell-main {
  min-width: 0;
}

.document-link-button {
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  color: inherit;
}

.document-link-button strong {
  font-size: 16px;
}

.document-link-button span {
  display: block;
  margin-top: 6px;
  color: var(--color-muted);
  line-height: 1.65;
  word-break: break-all;
}

.table-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.08);
  color: var(--color-text);
  font-size: 12px;
  font-weight: 700;
}

.document-cell-action,
.document-table-action-head {
  text-align: right;
}

.document-action-group {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 108px;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  background: rgba(178, 31, 196, 0.08);
  border: 1px solid rgba(178, 31, 196, 0.12);
  color: #8c168d;
  font-size: 12px;
  font-weight: 500;
}

.danger-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  background: rgba(177, 47, 38, 0.08);
  border: 1px solid rgba(177, 47, 38, 0.12);
  color: #b12f26;
  font-size: 12px;
  font-weight: 500;
}

.detail-link:hover,
.document-link-button:hover strong {
  color: var(--color-primary-strong);
}

.danger-link:hover:not(:disabled) {
  color: #7f331f;
}

.danger-link:disabled,
.detail-link:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.pagination-bar {
  margin-top: 18px;
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

.empty-block {
  min-height: 260px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--color-muted);
  padding: 36px 20px;
}

.primary-button,
.ghost-button {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 8px 16px;
  font-weight: 600;
}

.primary-button {
  color: #fff;
  background: var(--color-primary);
}

.ghost-button {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: transparent;
}

.ghost-button:hover:not(:disabled) {
  background: rgba(178, 31, 196, 0.14);
}

.file-input::file-selector-button {
  border: none;
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  margin-right: 12px;
  background: rgba(178, 31, 196, 0.08);
  color: #8c168d;
}

@media (max-width: 860px) {
  .upload-grid {
    grid-template-columns: 1fr;
  }

  .upload-footer {
    flex-direction: column;
  }

  .list-toolbar,
  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .table-summary {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
