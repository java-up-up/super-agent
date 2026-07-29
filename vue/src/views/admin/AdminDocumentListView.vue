<template>
  <section class="flex flex-col gap-5">
    <PageHeader title="文档接入">
      <template #actions>
        <Button size="lg" class="rounded-md" type="button" @click="openUploadModal">
          <ArrowUpTrayIcon data-icon="inline-start" aria-hidden="true" />
          上传文档
        </Button>
      </template>
    </PageHeader>

    <div
      v-if="pageNotice.message"
      class="rounded-md border px-4 py-3 text-body-sm font-medium"
      :class="{
        'border-primary/10 bg-primary/[0.08] text-primary': pageNotice.type === 'info',
        'glass-card text-foreground': pageNotice.type === 'success',
        'border-destructive/20 bg-destructive/10 text-destructive': pageNotice.type === 'danger'
      }"
      role="status"
    >
      {{ pageNotice.message }}
    </div>

    <ChildPageDialog
      :open="uploadModalVisible"
      title="上传资料并进入推荐流程"
      description="支持 PDF / DOC / DOCX / TXT / MD / HTML"
      close-label="关闭文档上传子页面"
      @update:open="handleUploadOpen"
    >
      <div class="grid gap-4">
            <div class="grid gap-3">
              <div class="grid gap-2">
                <Label class="field-label">知识域编码</Label>
                <Input v-model="uploadForm.knowledgeScopeCode" class="h-9 text-sm" placeholder="例如 operation_rule" />
              </div>
              <div class="grid gap-2">
                <Label class="field-label">知识域名称</Label>
                <Input v-model="uploadForm.knowledgeScopeName" class="h-9 text-sm" placeholder="例如 运营规则" />
              </div>
              <div class="grid gap-2">
                <Label class="field-label">文档名称</Label>
                <Input v-model="uploadForm.documentName" class="h-9 text-sm" placeholder="不填则使用原始文件名" />
              </div>
              <div class="grid gap-2">
                <Label class="field-label">业务分类</Label>
                <Input v-model="uploadForm.businessCategory" class="h-9 text-sm" placeholder="例如 手册 / 规则 / 介绍" />
              </div>
              <div class="grid gap-2">
                <Label class="field-label">文档标签</Label>
                <Input v-model="uploadForm.documentTags" class="h-9 text-sm" placeholder="多个标签用英文逗号分隔" />
              </div>
              <div class="grid gap-2">
                <Label class="field-label">选择文件</Label>
                <input
                  ref="fileInputRef"
                  type="file"
                  accept=".pdf,.doc,.docx,.txt,.md,.html,.htm"
                  class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm file:mr-3 file:rounded file:border-0 file:bg-primary/[0.08] file:px-3 file:py-1 file:text-xs file:font-medium file:text-primary"
                  @change="handleFileChange"
                />
              </div>
              <div v-if="uploadErrors.length" class="grid gap-1 rounded-md border border-destructive/20 bg-destructive/[0.06] px-3 py-2 text-xs text-destructive" role="alert">
                <span v-for="error in uploadErrors" :key="error">{{ error }}</span>
              </div>
              <div v-if="uploadForm.file" class="rounded-md border border-border bg-secondary/50 px-4 py-3">
                <strong class="block break-all text-sm text-foreground">{{ uploadForm.file.name }}</strong>
              </div>
            </div>
      </div>
      <template #footer>
        <div class="flex w-full items-center justify-end gap-2.5">
          <Button variant="outline" size="lg" class="rounded-md" type="button" :disabled="uploading" @click="closeUploadModal">取消</Button>
          <Button size="lg" class="rounded-md" type="button" :loading="uploading" loading-text="上传中" :disabled="!uploadForm.file" @click="submitUpload">上传并解析</Button>
        </div>
      </template>
    </ChildPageDialog>

    <FilterToolbar>
      <div class="flex min-w-[18rem] flex-1 flex-col gap-1.5 max-sm:min-w-0">
        <Input id="document-search" v-model="keyword" type="search" aria-label="搜索文档" placeholder="文档名称或原始文件名" @keydown.enter.prevent="submitSearch" />
      </div>
      <template #actions>
        <span class="mr-1 text-caption tabular-nums text-muted-foreground">共 {{ total }} 条</span>
        <Button v-if="hasActiveFilter" variant="ghost" size="sm" class="rounded-md" type="button" :disabled="listLoading" @click="resetSearch">清除筛选</Button>
        <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="listLoading" @click="submitSearch">搜索</Button>
        <Button variant="outline" size="sm" class="rounded-md" type="button" :loading="refreshing" loading-text="刷新中" @click="refreshDocuments">
          <ArrowPathIcon v-if="!refreshing" data-icon="inline-start" aria-hidden="true" />
          刷新
        </Button>
      </template>
    </FilterToolbar>

    <div v-if="listError && documents.length" class="glass-card rounded-md border px-4 py-3 text-body-sm text-foreground" role="status">
      {{ listError }}；已保留上一次成功的列表。
    </div>

    <AsyncState v-if="initialLoading" state="loading" title="正在加载文档" description="列表结构保持稳定，数据返回后即可扫描。" />
    <AsyncState v-else-if="listError && !documents.length" state="error" title="文档列表加载失败" :description="listError">
      <template #action><Button variant="outline" size="sm" class="rounded-md" type="button" @click="refreshDocuments">重新加载</Button></template>
    </AsyncState>
    <AsyncState v-else-if="!documents.length" :state="hasActiveFilter ? 'filtered' : 'empty'" :title="hasActiveFilter ? '没有匹配文档' : '还没有文档'" :description="hasActiveFilter ? '调整关键词或清除筛选。' : '上传一份资料后，处理状态会在这里更新。'" />

    <template v-else>
      <DataTableShell class="hidden md:block" :busy="refreshing" caption="文档处理列表">
        <thead>
          <tr class="bg-muted">
            <th scope="col" class="w-[36%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">文档</th>
            <th scope="col" class="w-[16%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">主状态</th>
            <th scope="col" class="w-[15%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">文件</th>
            <th scope="col" class="w-[18%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">更新时间</th>
            <th scope="col" class="w-[15%] border-b border-border px-4 py-3 text-right text-caption font-semibold text-muted-foreground">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in documents" :key="item.documentId" class="border-b border-border last:border-0 hover:bg-muted/60">
            <td class="px-4 py-3 align-top">
              <div class="block min-w-0" data-document-summary>
                <strong class="block truncate text-body-sm font-semibold text-foreground">{{ item.documentName || item.originalFileName || '未命名文档' }}</strong>
                <span class="mt-1 block truncate text-caption text-muted-foreground">{{ item.originalFileName || '-' }}</span>
                <span class="mt-1 block truncate text-caption text-primary">{{ item.knowledgeScopeName || item.knowledgeScopeCode || '未分配知识域' }}</span>
              </div>
            </td>
            <td class="px-4 py-3 align-top">
              <StatusBadge v-bind="resolveDocumentPrimaryStatus(item)" />
              <p class="mt-1.5 text-micro leading-relaxed text-muted-foreground">{{ documentStageDetail(item) }}</p>
            </td>
            <td class="px-4 py-3 align-top">
              <Badge variant="secondary">{{ item.fileTypeName || '-' }}</Badge>
              <span class="mt-1.5 block text-caption tabular-nums text-muted-foreground">{{ formatFileSize(item.fileSize) }}</span>
            </td>
            <td class="px-4 py-3 align-top text-body-sm tabular-nums text-foreground">{{ formatDateTime(item.editTime) }}</td>
            <td class="px-4 py-3 text-right align-top">
              <div class="inline-flex items-center justify-end gap-2">
                <Button variant="outline" size="sm" class="rounded-md" type="button" @click="openDocumentDetail(item.documentId)">查看</Button>
                <Button variant="destructive" size="sm" class="rounded-md" type="button" :loading="isDeletingDocument(item.documentId)" loading-text="删除中" :disabled="!canDeleteDocument(item)" :title="buildDeleteTitle(item)" @click="deleteDocument(item)">删除</Button>
              </div>
            </td>
          </tr>
        </tbody>
      </DataTableShell>

      <div class="glass-card glass-edge divide-y divide-border overflow-hidden rounded-glass border md:hidden" aria-label="移动端文档列表">
        <article v-for="item in documents" :key="item.documentId" class="py-4">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1">
              <div data-document-summary>
                <h2 class="m-0 break-words text-body-sm font-semibold text-foreground">{{ item.documentName || item.originalFileName || '未命名文档' }}</h2>
                <p class="mt-1 truncate text-caption text-muted-foreground">{{ item.originalFileName || '-' }}</p>
              </div>
            </div>
            <StatusBadge v-bind="resolveDocumentPrimaryStatus(item)" />
          </div>
          <dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-caption">
            <div><dt class="text-muted-foreground">知识域</dt><dd class="mt-0.5 truncate text-foreground">{{ item.knowledgeScopeName || item.knowledgeScopeCode || '未分配' }}</dd></div>
            <div><dt class="text-muted-foreground">更新时间</dt><dd class="mt-0.5 tabular-nums text-foreground">{{ formatDateTime(item.editTime) }}</dd></div>
            <div class="col-span-2"><dt class="text-muted-foreground">阶段详情</dt><dd class="mt-0.5 text-foreground">{{ documentStageDetail(item) }}</dd></div>
          </dl>
          <div class="mt-3 flex justify-end gap-2">
            <Button variant="outline" size="lg" class="rounded-md" type="button" @click="openDocumentDetail(item.documentId)">查看</Button>
            <Button variant="destructive" size="lg" class="rounded-md" type="button" :loading="isDeletingDocument(item.documentId)" loading-text="删除中" :disabled="!canDeleteDocument(item)" :title="buildDeleteTitle(item)" @click="deleteDocument(item)">删除</Button>
          </div>
        </article>
      </div>

      <nav class="flex items-center justify-between gap-4 border-t border-border pt-4 max-sm:flex-col max-sm:items-stretch" aria-label="文档列表分页">
        <Button variant="outline" size="sm" class="rounded-md max-sm:h-11" type="button" :disabled="currentPage <= 1 || listLoading" @click="changePage(currentPage - 1)">上一页</Button>
        <div class="text-center tabular-nums">
          <strong class="block text-body-sm text-foreground">第 {{ currentPage }} / {{ totalPages }} 页</strong>
          <span class="mt-1 block text-caption text-muted-foreground">共 {{ total }} 条文档</span>
        </div>
        <Button variant="outline" size="sm" class="rounded-md max-sm:h-11" type="button" :disabled="currentPage >= totalPages || listLoading" @click="changePage(currentPage + 1)">下一页</Button>
      </nav>
    </template>
  </section>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowPathIcon, ArrowUpTrayIcon } from '@heroicons/vue/24/outline'
import { APIError, manageApi } from '../../api/api'
import { formatDateTime, formatFileSize, hasCode } from '../../utils/manageFormat'
import { useConfirm } from '@/composables/useConfirm'
const { confirm } = useConfirm()
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import AsyncState from '@/components/system/AsyncState.vue'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import DataTableShell from '@/components/system/DataTableShell.vue'
import FilterToolbar from '@/components/system/FilterToolbar.vue'
import PageHeader from '@/components/system/PageHeader.vue'
import StatusBadge from '@/components/system/StatusBadge.vue'
import { createLatestRequestGuard, resolveDocumentPrimaryStatus } from '@/features/admin/adminBehavior'
import { buildUploadRequest, validateUploadDraft } from '@/features/admin/documentWorkflow'

const router = useRouter()
const requestGuard = createLatestRequestGuard()
const OPERATOR_ID = '10001'
const DEFAULT_PAGE_SIZE = 12

const uploadModalVisible = ref(false)
const uploadErrors = ref([])
const uploadForm = reactive({
  documentName: '',
  knowledgeScopeCode: '',
  knowledgeScopeName: '',
  businessCategory: '',
  documentTags: '',
  file: null
})

function openUploadModal() {
  uploadErrors.value = []
  uploadModalVisible.value = true
}

function closeUploadModal() {
  uploadModalVisible.value = false
  uploadErrors.value = []
  clearSelectedFile()
}

function handleUploadOpen(value) {
  if (value) uploadModalVisible.value = true
  else closeUploadModal()
}

const fileInputRef = ref(null)
const uploading = ref(false)
const listLoading = ref(false)
const listInitialized = ref(false)
const listError = ref('')
const keyword = ref('')
const appliedKeyword = ref('')
const documents = ref([])
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const total = ref(0)
const deletingDocumentId = ref('')
const pageNotice = reactive({ type: 'info', message: '' })

const totalPages = computed(() => Math.max(1, Math.ceil((total.value || 0) / pageSize.value)))
const initialLoading = computed(() => listLoading.value && !listInitialized.value)
const refreshing = computed(() => listLoading.value && listInitialized.value)
const hasActiveFilter = computed(() => Boolean(appliedKeyword.value))

function showNotice(message, type = 'info') { pageNotice.type = type; pageNotice.message = message }
function clearNotice() { pageNotice.message = '' }

function handleFileChange(event) {
  uploadForm.file = event.target.files?.[0] || null
  uploadErrors.value = validateUploadDraft(uploadForm).errors
}

function clearSelectedFile() {
  uploadForm.file = null
  uploadForm.documentName = ''
  uploadForm.knowledgeScopeCode = ''
  uploadForm.knowledgeScopeName = ''
  uploadForm.businessCategory = ''
  uploadForm.documentTags = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

async function loadDocuments(page = currentPage.value) {
  const requestId = requestGuard.begin()
  listLoading.value = true
  listError.value = ''
  try {
    const data = await manageApi.queryDocumentPage({ pageNo: page, pageSize: pageSize.value, keyword: appliedKeyword.value })
    if (!requestGuard.isCurrent(requestId)) return
    documents.value = Array.isArray(data?.records) ? data.records : []
    currentPage.value = Number(data?.pageNo || page)
    pageSize.value = Number(data?.pageSize || pageSize.value)
    total.value = Number(data?.total || 0)
    listInitialized.value = true
  } catch (error) {
    if (!requestGuard.isCurrent(requestId)) return
    console.error('加载文档列表失败', error)
    listError.value = normalizeError(error, '加载文档列表失败')
    listInitialized.value = true
  } finally {
    if (requestGuard.isCurrent(requestId)) listLoading.value = false
  }
}

function submitSearch() {
  appliedKeyword.value = keyword.value.trim()
  currentPage.value = 1
  loadDocuments(1)
}

function resetSearch() {
  keyword.value = ''
  appliedKeyword.value = ''
  currentPage.value = 1
  loadDocuments(1)
}

function refreshDocuments() {
  loadDocuments(currentPage.value)
}

function changePage(page) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  loadDocuments(page)
}

function documentTarget(documentId, query = {}) {
  return { name: 'AdminDocumentDetail', params: { documentId: String(documentId) }, query }
}

function openDocumentDetail(documentId, query = {}) {
  router.push(documentTarget(documentId, query))
}

function documentStageDetail(item) {
  return [item.parseStatusName || '解析未开始', item.strategyStatusName || '策略未确认', item.indexStatusName || '索引未构建'].join(' · ')
}

function isDeletingDocument(documentId) { return String(deletingDocumentId.value || '') === String(documentId || '') }

function hasRunningDocumentTask(item) {
  return hasCode(item?.latestTaskStatus, 1) || hasCode(item?.latestTaskStatus, 2)
    || hasCode(item?.parseStatus, 2) || hasCode(item?.indexStatus, 2)
}

function canDeleteDocument(item) {
  return !!item?.documentId && !listLoading.value && !deletingDocumentId.value && !hasRunningDocumentTask(item)
}

function buildDeleteTitle(item) {
  if (hasRunningDocumentTask(item)) return '请等待当前任务完成后再删除'
  if (deletingDocumentId.value) return '当前有文档正在删除'
  return '删除文档以及关联的索引、存储文件'
}

async function submitUpload() {
  if (uploading.value) return
  const validation = validateUploadDraft(uploadForm)
  if (!validation.valid) {
    uploadErrors.value = validation.errors
    showNotice(validation.errors[0], 'danger')
    return
  }
  uploading.value = true; clearNotice()
  try {
    const result = await manageApi.uploadDocument(buildUploadRequest(uploadForm, OPERATOR_ID))
    closeUploadModal()
    showNotice(`文档已上传，任务 ${result.taskId} 已进入解析与策略推荐队列。`, 'success')
    keyword.value = ''
    appliedKeyword.value = ''
    await loadDocuments(1)
    openDocumentDetail(result.documentId)
  } catch (error) {
    console.error('上传文档失败', error)
    uploadErrors.value = [normalizeError(error, '上传文档失败')]
    showNotice(uploadErrors.value[0], 'danger')
  } finally {
    uploading.value = false
  }
}

async function deleteDocument(item) {
  if (!item?.documentId) return
  if (hasRunningDocumentTask(item)) { showNotice('当前文档存在进行中的任务，请等待任务完成后再删除。', 'danger'); return }
  const documentId = String(item.documentId)
  const documentName = item.documentName || item.originalFileName || documentId
  if (!await confirm(`确认删除文档《${documentName}》吗？\n\n将同时删除 MySQL 记录、向量库数据和 MinIO 存储文件，删除后不可恢复。`, '确认删除')) return
  deletingDocumentId.value = documentId; clearNotice()
  try {
    await manageApi.deleteDocument({ documentId })
    const nextPage = documents.value.length === 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value
    await loadDocuments(nextPage)
    showNotice(`文档《${documentName}》已删除，关联数据已同步清理。`, 'success')
  } catch (error) {
    console.error('删除文档失败', error); showNotice(normalizeError(error, '删除文档失败'), 'danger')
  } finally {
    deletingDocumentId.value = ''
  }
}

function normalizeError(error, fallbackMessage) {
  if (error instanceof APIError && error.message) return error.message
  if (error instanceof Error && error.message) return error.message
  return fallbackMessage
}

onMounted(() => {
  loadDocuments()
})
</script>
