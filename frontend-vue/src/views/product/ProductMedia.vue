<template>
  <div class="product-media-page">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">产品媒体管理</h3>
        <a-input-search
          v-model:value="searchTitle"
          placeholder="搜索产品名称"
          style="width:260px"
          @search="loadProducts"
          allow-clear
        />
      </div>

      <a-table
        :dataSource="productList"
        :columns="productCols"
        :loading="tableLoading"
        :pagination="pagination"
        rowKey="productId"
        size="middle"
        @change="handleTableChange"
        :customRow="r => ({ onClick: () => selectProduct(r) })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div class="product-cell">
              <div class="product-image">
                <a-image
                  :src="record.imageUrl || '/images/placeholder.png'"
                  :width="64"
                  :height="64"
                  :preview="false"
                  fallback="/images/placeholder.png"
                />
              </div>
              <div class="product-info">
                <div class="product-title" :title="record.productName">
                  {{ record.productName || '—' }}
                </div>
                <div class="product-meta">
                  <span class="meta-item">
                    <span class="meta-label">Handle:</span>
                    <span class="meta-value">{{ record.handle || '-' }}</span>
                  </span>
                  <span v-if="record.sku" class="meta-item">
                    <span class="meta-label">SKU:</span>
                    <span class="meta-value">{{ record.sku }}</span>
                  </span>
                </div>
              </div>
            </div>
          </template>
          <template v-if="column.key === 'counts'">
            <div class="file-counts">
              <span v-for="cat in CATEGORY_LIST" :key="cat.value" class="count-item">
                <span class="count-dot" :style="{ background: cat.dotColor }" />
                <span class="count-num">{{ record.tagFileCounts?.[cat.value] || 0 }}</span>
              </span>
            </div>
          </template>
          <template v-if="column.key === 'total'">
            <span class="total-count">{{ getTotalFiles(record) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click.stop="selectProduct(record)">
              <folder-open-outlined /> 管理
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 媒体管理抽屉 -->
    <a-drawer
      v-model:open="drawerOpen"
      :title="null"
      width="1020"
      destroyOnClose
      :headerStyle="{ display: 'none' }"
      :bodyStyle="{ padding: 0 }"
    >
      <div class="drawer-header">
        <div class="drawer-title-row">
          <h3 class="drawer-title">{{ selectedProduct?.productName }}</h3>
          <a-button type="text" @click="drawerOpen = false" class="drawer-close">
            <close-outlined />
          </a-button>
        </div>
        <!-- 对标链接折叠面板 -->
        <a-collapse ghost :bordered="false" class="ref-collapse">
          <a-collapse-panel key="ref">
            <template #header>
              <span>对标页链接</span>
              <span v-if="referenceLinks.filter(l => l.trim()).length" class="ref-count">
                {{ referenceLinks.filter(l => l.trim()).length }}
              </span>
            </template>
            <div v-for="(link, idx) in referenceLinks" :key="idx" class="ref-link-row">
              <!-- 展示态：链接样式 -->
              <template v-if="link.trim() && editingLinkIdx !== idx">
                <div class="ref-link-display">
                  <div class="ref-link-ops">
                    <a-tooltip title="复制链接"><copy-outlined class="ref-op" @click="copyRefLink(link)" /></a-tooltip>
                    <a-tooltip title="编辑"><edit-outlined class="ref-op" @click="editingLinkIdx = idx" /></a-tooltip>
                  </div>
                  <a :href="link" target="_blank" rel="noopener" class="ref-link-url">{{ link }}</a>
                  <delete-outlined class="ref-del" @click="removeRefLink(idx)" />
                </div>
              </template>
              <!-- 编辑态：输入框 -->
              <template v-else>
                <div class="ref-link-edit">
                  <a-input v-model:value="referenceLinks[idx]" placeholder="输入对标页 URL"
                    size="small" @blur="onRefLinkBlur(idx)" @pressEnter="onRefLinkBlur(idx)"
                    :ref="el => setEditInputRef(idx, el)" />
                  <delete-outlined class="ref-del" @click="removeRefLink(idx)" />
                </div>
              </template>
            </div>
            <a-button type="dashed" size="small" block @click="addRefLink" class="ref-add-btn">
              <plus-outlined /> 添加链接
            </a-button>
          </a-collapse-panel>
        </a-collapse>
      </div>

      <div class="drawer-body">
        <a-tabs v-model:activeKey="activeCategory" @change="onCategoryChange" class="media-tabs">
          <a-tab-pane v-for="cat in CATEGORY_LIST" :key="cat.value">
            <template #tab>
              <span class="tab-label">
                {{ cat.label }}
                <span class="tab-count" v-if="(allFiles[cat.value]?.length || 0) > 0">
                  {{ allFiles[cat.value]?.length }}
                </span>
              </span>
            </template>

            <!-- 主图同步入口 -->
            <div v-if="cat.value === 'main_image'" class="sync-bar">
              <span>同步产品主图</span>
              <a-button size="small" :loading="syncing" @click="doSyncImages">
                <sync-outlined /> 同步
              </a-button>
              <span v-if="syncResult" class="sync-msg" :class="syncResultType">{{ syncResult }}</span>
            </div>

            <!-- 文档自定义标签输入 -->
            <div v-if="cat.value === 'document'" class="doc-tags-bar">
              <tag-outlined style="color:#999;flex-shrink:0" />
              <a-select
                v-model:value="docTagsInput[cat.value]"
                mode="tags"
                placeholder="添加标签（回车确认，上传时自动关联）"
                style="flex:1"
                :token-separators="[',']"
              />
            </div>

            <!-- 操作区：上传 + URL下载并排 -->
            <div class="action-row">
              <div class="upload-zone" @dragover.prevent @drop.prevent="handleDrop($event, cat.value)"
                @click="triggerFileInput(cat.value)">
                <upload-outlined class="uz-icon" />
                <span class="uz-text">拖拽或点击上传</span>
                <span class="uz-hint">{{ cat.value === 'document' ? 'PDF / DOC / XLS / CSV / JSON' : '图片 / 视频' }}</span>
              </div>
              <div class="url-zone">
                <a-textarea v-model:value="urlInputText[cat.value]"
                  placeholder="粘贴 URL（每行一个）" :rows="2"
                  class="url-input" allow-clear />
                <a-button type="primary" size="small" block
                  :loading="downloadingUrls[cat.value]"
                  :disabled="!urlInputText[cat.value]?.trim()"
                  @click="startUrlDownload(cat.value)">
                  <cloud-download-outlined /> 下载到媒体库
                </a-button>
              </div>
            </div>
            <input :ref="el => fileInputRefs[cat.value] = el" type="file" multiple
              :accept="cat.value === 'document' ? '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.csv,.json' : 'image/*,video/*'"
              style="display:none" @change="handleFileChange($event, cat.value)" />

            <!-- 下载结果 -->
            <div v-if="downloadResultText[cat.value]" class="dl-summary">
              {{ downloadResultText[cat.value] }}
            </div>
            <div v-if="downloadResults[cat.value]?.length" class="dl-result-list">
              <div v-for="r in downloadResults[cat.value]" :key="r.url"
                :class="['dl-item', r.success ? 'dl-ok' : 'dl-fail']">
                <check-circle-outlined v-if="r.success" />
                <close-circle-outlined v-else />
                <span class="dl-url">{{ r.url }}</span>
                <span v-if="!r.success" class="dl-err">{{ r.error }}</span>
              </div>
            </div>

            <!-- 上传进度 -->
            <div v-if="uploadQueue[cat.value]?.length" class="upload-progress-list">
              <div v-for="item in uploadQueue[cat.value]" :key="item.name" class="progress-item">
                <span class="file-name-mini">{{ item.name }}</span>
                <a-progress :percent="item.progress" size="small" style="flex:1;margin:0 8px" :showInfo="false" />
                <span :class="['progress-status', 'ps-' + item.status]">
                  {{ item.status === 'done' ? '完成' : item.status === 'error' ? '失败' : item.progress + '%' }}
                </span>
              </div>
            </div>

            <!-- 批量操作条 -->
            <div v-if="selectedFileIds.length > 0" class="batch-bar">
              <span>已选 <b>{{ selectedFileIds.length }}</b> 项</span>
              <a-button size="small" type="text" @click="selectAllFiles">全选</a-button>
              <a-button size="small" type="text" @click="deselectAll">取消</a-button>
              <a-divider type="vertical" />
              <a-button size="small" type="text" :loading="batchDownloading" @click="doBatchDownload">
                <download-outlined /> 下载
              </a-button>
              <a-popconfirm title="确定删除选中文件？" ok-text="删除" cancel-text="取消" @confirm="doBatchDelete">
                <a-button size="small" danger type="text">删除</a-button>
              </a-popconfirm>
              <a-dropdown>
                <a-button size="small" type="text">移动到 <down-outlined /></a-button>
                <template #overlay>
                  <a-menu @click="handleBatchMove">
                    <a-menu-item v-for="c in CATEGORY_LIST.filter(c => c.value !== activeCategory)"
                      :key="c.value">{{ c.label }}</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>

            <!-- 文件筛选栏 -->
            <div class="file-filter-bar">
              <a-input
                v-model:value="fileSearch"
                placeholder="搜索文件名"
                allow-clear
                style="width:180px"
              >
                <template #prefix><search-outlined style="color:#bbb" /></template>
              </a-input>
              <a-select
                v-model:value="fileSourceFilter"
                placeholder="来源"
                allow-clear
                style="width:110px"
              >
                <a-select-option value="UPLOAD">上传</a-select-option>
                <a-select-option value="URL_DOWNLOAD">链接下载</a-select-option>
                <a-select-option value="SHOPIFY_SYNC">同步</a-select-option>
              </a-select>
              <a-select
                v-if="activeCategory === 'document'"
                v-model:value="fileTagFilter"
                placeholder="标签筛选"
                allow-clear
                style="width:130px"
              >
                <a-select-option v-for="tag in allDocTags" :key="tag" :value="tag">
                  {{ tag }}
                </a-select-option>
              </a-select>
              <span v-if="isFiltered" class="filter-count-hint">
                <filter-outlined /> {{ filteredFiles.length }} / {{ currentFiles.length }} 个
              </span>
            </div>

            <!-- 文件网格 -->
            <a-spin :spinning="filesLoading">
              <draggable
                v-if="currentFiles.length"
                v-model="currentFiles"
                item-key="id"
                class="media-grid"
                ghost-class="drag-ghost"
                :disabled="isFiltered"
                @end="onDragEnd"
              >
                <template #item="{ element: file }">
                  <div v-show="filteredFileIds.has(file.id)"
                    :class="['media-card', { selected: selectedFileIds.includes(file.id) }]"
                    @click.exact="toggleSelect(file)" @dblclick="openPreview(file)">
                    <div class="card-check" @click.stop="toggleSelect(file)">
                      <a-checkbox :checked="selectedFileIds.includes(file.id)" />
                    </div>
                    <!-- 缩略图 -->
                    <div class="card-thumb">
                      <template v-if="file.mediaType === 'video'">
                        <video :src="file.url" preload="metadata" />
                        <span class="type-tag">视频</span>
                      </template>
                      <template v-else-if="file.mediaType === 'document'">
                        <div class="doc-placeholder">
                          <file-pdf-outlined v-if="file.originalName?.endsWith('.pdf')" />
                          <file-excel-outlined v-else-if="file.originalName?.match(/\.xlsx?$/)" />
                          <file-word-outlined v-else-if="file.originalName?.match(/\.docx?$/)" />
                          <file-outlined v-else />
                        </div>
                        <span class="type-tag doc">文档</span>
                      </template>
                      <template v-else>
                        <img :src="file.url" loading="lazy" />
                      </template>
                      <!-- hover 操作 -->
                      <div class="card-actions">
                        <button @click.stop="openPreview(file)" title="预览"><eye-outlined /></button>
                        <button @click.stop="doDownloadFile(file)" title="下载"><download-outlined /></button>
                        <button @click.stop="copyUrl(file)" title="复制链接"><copy-outlined /></button>
                        <button @click.stop="confirmDelete(file)" title="删除" class="act-danger"><delete-outlined /></button>
                      </div>
                    </div>
                    <div class="card-info">
                      <span class="card-name" :title="file.originalName">{{ file.originalName || file.objectName }}</span>
                      <span class="card-meta">{{ formatSize(file.fileSize) }}</span>
                      <div v-if="file.tags" class="card-tags">
                        <a-tag v-for="t in file.tags.split(',')" :key="t" color="cyan" style="margin:1px 2px;font-size:10px;padding:0 4px;line-height:18px">{{ t.trim() }}</a-tag>
                      </div>
                    </div>
                  </div>
                </template>
              </draggable>
              <a-empty v-else-if="!currentFiles.length" description="暂无文件" style="margin:40px 0" />
              <a-empty v-else-if="isFiltered && !filteredFiles.length" description="无匹配文件" style="margin:40px 0" />
            </a-spin>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-drawer>

    <!-- 预览 -->
    <a-modal v-model:open="previewOpen" :title="previewFile?.originalName"
      :footer="null" width="80%" centered destroyOnClose>
      <div class="preview-box">
        <video v-if="previewFile?.mediaType === 'video'" :src="previewFile?.url"
          controls autoplay />
        <iframe v-else-if="previewFile?.mediaType === 'document' && previewFile?.originalName?.endsWith('.pdf')"
          :src="previewFile?.url" />
        <div v-else-if="previewFile?.mediaType === 'document'" class="doc-fallback">
          <file-outlined style="font-size:56px;color:#bbb" />
          <p>不支持在线预览</p>
          <a-button type="primary" size="small" :href="previewFile?.url" target="_blank">下载文件</a-button>
        </div>
        <img v-else :src="previewFile?.url" />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import draggable from 'vuedraggable'
import {
  UploadOutlined, DeleteOutlined, PlusOutlined, EditOutlined,
  CloudDownloadOutlined, CheckCircleOutlined, CloseCircleOutlined,
  SyncOutlined, EyeOutlined, CopyOutlined, DownOutlined, CloseOutlined,
  FolderOpenOutlined, DownloadOutlined, TagOutlined, SearchOutlined, FilterOutlined,
  FilePdfOutlined, FileExcelOutlined, FileWordOutlined, FileOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { useReferenceLinks } from '@/composables/useReferenceLinks'
import {
  getProductMediaList, getProductMediaFiles,
  uploadProductMediaFile, deleteProductMediaFile,
  batchDeleteFiles, updateFilesSort, moveFileCategory,
  downloadFromUrls, syncProductImages,
  downloadMediaFile, batchDownloadMediaFiles
} from '@/api/product-media-template'

const CATEGORY_LIST = [
  { value: 'main_image',   label: '主图',     dotColor: '#1677ff' },
  { value: 'detail_media', label: '详情素材', dotColor: '#722ed1' },
  { value: 'ad_media',     label: '广告素材', dotColor: '#fa8c16' },
  { value: 'document',     label: '文档',     dotColor: '#13c2c2' }
]

const SOURCE_LABELS = { URL_DOWNLOAD: '链接', SHOPIFY_SYNC: '同步', SUPPLIER: '供应商' }

// ─── 产品列表 ───
const productList  = ref([])
const tableLoading = ref(false)
const searchTitle  = ref('')
const pagination   = reactive({ current:1, pageSize:10, total:0, showSizeChanger:true })
const productCols  = [
  { title: '产品信息', key: 'product', width: 400 },
  { title: '主图 / 详情 / 广告 / 文档', key: 'counts', width: 200, align: 'center' },
  { title: '总计', key: 'total', width: 70, align: 'center' },
  { title: '操作', key: 'action', width: 80, align: 'center' }
]

function getTotalFiles(record) {
  const c = record.tagFileCounts || {}
  return Object.values(c).reduce((s, n) => s + (n || 0), 0)
}

async function loadProducts() {
  tableLoading.value = true
  try {
    const res = await getProductMediaList({
      current: pagination.current, size: pagination.pageSize,
      title: searchTitle.value || undefined
    })
    if (res.code === 200) {
      productList.value = res.data.records
      pagination.total  = res.data.total
    }
  } catch (err) { message.error('加载失败') }
  finally { tableLoading.value = false }
}

function handleTableChange(pag) {
  pagination.current  = pag.current
  pagination.pageSize = pag.pageSize
  loadProducts()
}

// ─── 抽屉 ───
const drawerOpen      = ref(false)
const selectedProduct = ref(null)
const activeCategory  = ref('main_image')
const allFiles        = reactive({})
const filesLoading    = ref(false)
const fileInputRefs   = reactive({})
const uploadQueue     = reactive({})
const selectedFileIds = ref([])

// ─── 文件筛选 ───
const fileSearch       = ref('')
const fileSourceFilter = ref('')
const fileTagFilter    = ref('')

const currentFiles = computed({
  get: () => allFiles[activeCategory.value] || [],
  set: (val) => { allFiles[activeCategory.value] = val }
})

const filteredFiles = computed(() => {
  let files = currentFiles.value
  const kw = fileSearch.value.trim().toLowerCase()
  if (kw) {
    files = files.filter(f => (f.originalName || f.objectName || '').toLowerCase().includes(kw))
  }
  if (fileSourceFilter.value) {
    files = files.filter(f => f.source === fileSourceFilter.value)
  }
  if (fileTagFilter.value) {
    files = files.filter(f =>
      f.tags && f.tags.split(',').map(t => t.trim()).includes(fileTagFilter.value)
    )
  }
  return files
})

const filteredFileIds = computed(() => new Set(filteredFiles.value.map(f => f.id)))
const isFiltered      = computed(() =>
  !!fileSearch.value.trim() || !!fileSourceFilter.value || !!fileTagFilter.value
)

// 当前 document tab 所有已用标签（去重）
const allDocTags = computed(() => {
  const tags = new Set()
  ;(allFiles['document'] || []).forEach(f => {
    if (f.tags) f.tags.split(',').forEach(t => { if (t.trim()) tags.add(t.trim()) })
  })
  return [...tags]
})

async function selectProduct(record) {
  selectedProduct.value = record
  drawerOpen.value = true
  activeCategory.value = 'main_image'
  syncResult.value = ''
  selectedFileIds.value = []
  for (const cat of CATEGORY_LIST) {
    downloadResults[cat.value] = []
    downloadResultText[cat.value] = ''
    allFiles[cat.value] = []
  }
  await Promise.all([loadRefLinks(), loadFiles()])
}

function onCategoryChange() {
  selectedFileIds.value = []
  fileSearch.value = ''
  fileSourceFilter.value = ''
  fileTagFilter.value = ''
  loadFiles()
}

async function loadFiles() {
  if (!selectedProduct.value) return
  filesLoading.value = true
  try {
    const res = await getProductMediaFiles(selectedProduct.value.productId, activeCategory.value)
    allFiles[activeCategory.value] = res.data || []
  } catch (err) {
    allFiles[activeCategory.value] = []
  } finally { filesLoading.value = false }
}

// ─── 选择 ───
function toggleSelect(file) {
  const idx = selectedFileIds.value.indexOf(file.id)
  if (idx >= 0) selectedFileIds.value.splice(idx, 1)
  else selectedFileIds.value.push(file.id)
}
function selectAllFiles() { selectedFileIds.value = filteredFiles.value.map(f => f.id) }
function deselectAll() { selectedFileIds.value = [] }

// ─── 批量删除 ───
async function doBatchDelete() {
  if (!selectedFileIds.value.length) return
  try {
    await batchDeleteFiles(selectedProduct.value.productId, selectedFileIds.value)
    message.success(`已删除 ${selectedFileIds.value.length} 个文件`)
    selectedFileIds.value = []
    await loadFiles(); await loadProducts()
  } catch (err) { message.error('批量删除失败') }
}

// ─── 批量移动 ───
async function handleBatchMove({ key }) {
  let moved = 0
  for (const fid of selectedFileIds.value) {
    try { await moveFileCategory(fid, key); moved++ } catch (_) {}
  }
  if (moved) {
    message.success(`已移动 ${moved} 个文件`)
    selectedFileIds.value = []
    await loadFiles(); await loadProducts()
  }
}

// ─── 拖拽排序 ───
async function onDragEnd() {
  try {
    await updateFilesSort(selectedProduct.value.productId, activeCategory.value,
      currentFiles.value.map(f => f.id))
  } catch (err) { await loadFiles() }
}

// ─── 对标链接 ───
const {
  referenceLinks, savingLink, editingLinkIdx,
  setEditInputRef, loadRefLinks, addRefLink,
  removeRefLink, onRefLinkBlur, saveRefLinks: autoSaveRefLinks, copyRefLink
} = useReferenceLinks({
  getProductId: () => selectedProduct.value?.productId,
  autoSave: true
})

// ─── 同步主图 ───
const syncing = ref(false)
const syncResult = ref('')
const syncResultType = ref('')
async function doSyncImages() {
  syncing.value = true
  syncResult.value = ''
  syncResultType.value = ''
  try {
    const res = await syncProductImages(selectedProduct.value.productId)
    const data = res.data || {}
    const synced = data.synced || 0
    const failed = data.failed || 0
    const skipped = data.skipped || 0
    if (failed > 0 && synced === 0) {
      syncResult.value = `同步失败（${failed} 个失败）`
      syncResultType.value = 'sync-error'
    } else if (failed > 0) {
      syncResult.value = `完成：${synced} 成功，${failed} 失败，${skipped} 跳过`
      syncResultType.value = 'sync-warn'
    } else {
      syncResult.value = `成功同步 ${synced} 张，${skipped} 已存在跳过`
      syncResultType.value = 'sync-ok'
    }
    if (synced > 0) {
      await loadFiles()
      await loadProducts()
    }
  } catch (e) {
    syncResult.value = '请求超时或网络错误'
    syncResultType.value = 'sync-error'
  } finally {
    syncing.value = false
  }
}

// ─── 文档标签 ───
const docTagsInput = reactive({})  // category -> string[]

// ─── URL 下载 ───
const urlInputText = reactive({})
const downloadingUrls = reactive({})
const downloadResults = reactive({})
const downloadResultText = reactive({})

async function startUrlDownload(category) {
  const raw = urlInputText[category]?.trim()
  if (!raw) return
  const urls = raw.split('\n').map(u => u.trim()).filter(Boolean)
  if (!urls.length) return

  downloadingUrls[category] = true
  downloadResults[category] = []
  downloadResultText[category] = `正在下载 ${urls.length} 个文件...`
  try {
    const tags = docTagsInput[category]?.length ? docTagsInput[category].join(',') : null
    const res = await downloadFromUrls(selectedProduct.value.productId, category, urls, tags)
    const results = res.data || []
    downloadResults[category] = results
    const ok = results.filter(r => r.success).length
    const fail = results.length - ok
    downloadResultText[category] = `${ok} 成功${fail ? '，' + fail + ' 失败' : ''}`
    if (ok) { urlInputText[category] = ''; await loadFiles(); await loadProducts() }
  } catch (e) {
    downloadResultText[category] = '请求失败'
  } finally { downloadingUrls[category] = false }
}

// ─── 上传 ───
function triggerFileInput(cat) { fileInputRefs[cat]?.click() }
function handleDrop(e, cat) {
  const files = [...e.dataTransfer.files]
  uploadFiles(cat === 'document' ? files : files.filter(f => f.type.startsWith('image/') || f.type.startsWith('video/')), cat)
}
function handleFileChange(e, cat) { uploadFiles([...e.target.files], cat); e.target.value = '' }

async function uploadFiles(files, category) {
  if (!files.length) return
  if (!uploadQueue[category]) uploadQueue[category] = []
  for (const file of files) {
    const item = reactive({ name: file.name, progress: 0, status: 'uploading' })
    uploadQueue[category].push(item)
    try {
      const tags = docTagsInput[category]?.length ? docTagsInput[category].join(',') : null
      await uploadProductMediaFile(selectedProduct.value.productId, category, file,
        e => { item.progress = Math.round((e.loaded / e.total) * 100) }, tags)
      item.status = 'done'; item.progress = 100
    } catch (err) { item.status = 'error' }
  }
  await loadFiles(); await loadProducts()
  setTimeout(() => { uploadQueue[category] = uploadQueue[category].filter(i => i.status !== 'done') }, 2000)
}

// ─── 删除 ───
function confirmDelete(file) {
  Modal.confirm({
    title: '删除文件', content: `确定删除 "${file.originalName}" ？`,
    okText: '删除', okType: 'danger', cancelText: '取消',
    async onOk() {
      await deleteProductMediaFile(file.id)
      message.success('已删除')
      await loadFiles(); await loadProducts()
    }
  })
}

// ─── 复制 ───
async function copyUrl(file) {
  try { await navigator.clipboard.writeText(file.url); message.success('已复制') }
  catch (_) { message.error('复制失败') }
}

// ─── 预览 ───
const previewOpen = ref(false)
const previewFile = ref(null)
function openPreview(file) { previewFile.value = file; previewOpen.value = true }

// ─── 下载文件 ───
function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function doDownloadFile(file) {
  try {
    message.loading({ content: '下载中...', key: 'dl' })
    const blob = await downloadMediaFile(file.id)
    triggerBlobDownload(blob, file.originalName || 'download')
    message.success({ content: '下载完成', key: 'dl' })
  } catch (e) {
    message.error({ content: '下载失败', key: 'dl' })
  }
}

const batchDownloading = ref(false)
async function doBatchDownload() {
  if (!selectedFileIds.value.length) return
  batchDownloading.value = true
  try {
    message.loading({ content: '正在打包下载...', key: 'bdl' })
    const blob = await batchDownloadMediaFiles(selectedFileIds.value)
    triggerBlobDownload(blob, `media-files-${Date.now()}.zip`)
    message.success({ content: '下载完成', key: 'bdl' })
  } catch (e) {
    message.error({ content: '批量下载失败', key: 'bdl' })
  } finally { batchDownloading.value = false }
}

// ─── 工具 ───
function formatSize(b) {
  if (!b) return ''
  if (b < 1024) return b + ' B'
  if (b < 1048576) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1048576).toFixed(1) + ' MB'
}

onMounted(loadProducts)
</script>

<style scoped>
/* ─── 页面 ─── */
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.page-title  { margin:0; font-size:16px; font-weight:600; color:#1d2129; }

:deep(.ant-table-row) { cursor:pointer; }
:deep(.ant-table-row:hover) td { background:#f0f5ff !important; }

/* 产品单元格 */
.product-cell {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.product-image {
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}
.product-image :deep(.ant-image-img) {
  object-fit: cover;
  display: block;
}
.product-info {
  flex: 1;
  min-width: 0;
}
.product-title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}
.product-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-item {
  display: flex;
  gap: 6px;
  font-size: 12px;
}
.meta-label {
  color: #8c8c8c;
  flex-shrink: 0;
}
.meta-value {
  color: #595959;
}

/* 文件计数 - 紧凑小圆点 */
.file-counts { display:flex; align-items:center; justify-content:center; gap:12px; }
.count-item  { display:flex; align-items:center; gap:3px; }
.count-dot   { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.count-num   { font-size:13px; color:#666; font-variant-numeric:tabular-nums; }
.total-count { font-size:13px; font-weight:600; color:#1d2129; }

/* ─── 抽屉 ─── */
.drawer-header {
  padding:16px 24px 0; border-bottom:1px solid #f0f0f0;
}
.drawer-title-row { display:flex; align-items:center; justify-content:space-between; }
.drawer-title { margin:0; font-size:16px; font-weight:600; color:#1d2129; }
.drawer-close { font-size:16px; color:#999; }

.ref-collapse { margin:8px -12px 0; }
:deep(.ref-collapse .ant-collapse-header) { padding:6px 12px !important; font-size:13px; color:#8c8c8c; }
:deep(.ref-collapse .ant-collapse-content-box) { padding:4px 12px 12px !important; }
.ref-count {
  display:inline-flex; align-items:center; justify-content:center;
  min-width:16px; height:16px; padding:0 4px; margin-left:4px;
  font-size:10px; font-weight:600; background:#f0f0f0; color:#666; border-radius:8px;
}
.ref-link-row { margin-bottom:4px; }

.ref-link-display {
  display:flex; align-items:center; gap:6px;
  padding:4px 8px; border-radius:6px; transition:background .15s;
}
.ref-link-display:hover { background:#f6f8fa; }
.ref-link-ops { display:flex; align-items:center; gap:2px; flex-shrink:0; }
.ref-op {
  font-size:12px; color:#bbb; cursor:pointer; padding:2px;
  border-radius:3px; transition:all .15s;
}
.ref-op:hover { color:#1677ff; background:#e6f4ff; }
.ref-link-url {
  flex:1; font-size:12px; color:#1677ff; overflow:hidden;
  text-overflow:ellipsis; white-space:nowrap; text-decoration:none;
}
.ref-link-url:hover { text-decoration:underline; }
.ref-del {
  font-size:12px; color:#ccc; cursor:pointer; flex-shrink:0;
  padding:2px; border-radius:3px; transition:all .15s; opacity:0;
}
.ref-link-display:hover .ref-del, .ref-link-edit:hover .ref-del { opacity:1; }
.ref-del:hover { color:#ff4d4f; background:#fff1f0; }

.ref-link-edit {
  display:flex; align-items:center; gap:6px;
  padding:2px 0;
}
.ref-link-edit .ant-input { flex:1; }

.ref-add-btn { margin-top:6px; }

.drawer-body { padding:0 24px 24px; }

/* ─── Tabs ─── */
.tab-label { display:inline-flex; align-items:center; gap:4px; }
.tab-count {
  display:inline-flex; align-items:center; justify-content:center;
  min-width:18px; height:18px; padding:0 5px;
  font-size:11px; font-weight:600; line-height:1;
  background:#f0f0f0; color:#666; border-radius:9px;
}
:deep(.ant-tabs-tab-active .tab-count) { background:#e6f4ff; color:#1677ff; }

/* ─── 同步条 ─── */
.sync-bar {
  display:flex; align-items:center; gap:10px;
  padding:6px 12px; margin-bottom:12px;
  background:#f6f8fa; border-radius:6px; font-size:13px; color:#666;
}
.sync-msg { font-size:12px; }
.file-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 0 4px;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.filter-count-hint {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}
.doc-tags-bar {
  display:flex; align-items:center; gap:8px;
  padding:6px 12px; margin-bottom:8px;
  background:#f6f8fa; border-radius:6px;
}
.card-tags { margin-top:3px; display:flex; flex-wrap:wrap; gap:0; }
.sync-ok   { color:#52c41a; }
.sync-warn { color:#faad14; }
.sync-error{ color:#ff4d4f; }

/* ─── 操作区 ─── */
.action-row { display:flex; gap:12px; margin-bottom:12px; }

.upload-zone {
  flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center;
  padding:16px; border:1.5px dashed #d9d9d9; border-radius:8px; cursor:pointer;
  background:#fafafa; transition:all .2s; min-height:88px;
}
.upload-zone:hover { border-color:#1677ff; background:#f0f7ff; }
.uz-icon { font-size:22px; color:#1677ff; }
.uz-text { font-size:13px; color:#333; margin-top:4px; }
.uz-hint { font-size:11px; color:#bbb; }

.url-zone { flex:1; display:flex; flex-direction:column; gap:6px; }
.url-input { font-size:12px; font-family:'Menlo','Consolas',monospace; resize:none; }

/* ─── 下载结果 ─── */
.dl-summary { font-size:12px; color:#666; margin-bottom:6px; }
.dl-result-list { max-height:100px; overflow-y:auto; margin-bottom:8px; }
.dl-item { display:flex; align-items:center; gap:4px; font-size:11px; padding:2px 0; }
.dl-ok   { color:#52c41a; }
.dl-fail { color:#ff4d4f; }
.dl-url  { flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#333; }
.dl-err  { color:#ff4d4f; flex-shrink:0; }

/* ─── 上传进度 ─── */
.upload-progress-list { margin-bottom:8px; }
.progress-item { display:flex; align-items:center; margin-bottom:4px; }
.file-name-mini { width:120px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:12px; color:#555; }
.progress-status { font-size:11px; width:36px; text-align:right; }
.ps-done  { color:#52c41a; }
.ps-error { color:#ff4d4f; }

/* ─── 批量操作 ─── */
.batch-bar {
  display:flex; align-items:center; gap:4px;
  padding:4px 10px; margin-bottom:8px;
  background:#f6f8fa; border-radius:6px; font-size:13px; color:#333;
}

/* ─── 文件网格 ─── */
.media-grid {
  display:grid; grid-template-columns:repeat(auto-fill, minmax(140px, 1fr));
  gap:10px; margin-top:8px;
}
.media-card {
  position:relative; border-radius:8px; overflow:hidden;
  background:#fff; border:1.5px solid #f0f0f0; cursor:pointer;
  transition:all .15s ease;
}
.media-card:hover { border-color:#d9d9d9; box-shadow:0 2px 8px rgba(0,0,0,.08); }
.media-card.selected { border-color:#1677ff; box-shadow:0 0 0 2px rgba(22,119,255,.15); }
.drag-ghost { opacity:.4; }

.card-check {
  position:absolute; top:5px; left:5px; z-index:3;
  opacity:0; transition:opacity .15s;
}
.media-card:hover .card-check, .media-card.selected .card-check { opacity:1; }

.card-thumb {
  position:relative; aspect-ratio:1; overflow:hidden; background:#f5f5f5;
}
.card-thumb img, .card-thumb video {
  width:100%; height:100%; object-fit:cover; display:block;
  transition:transform .2s;
}
.media-card:hover .card-thumb img,
.media-card:hover .card-thumb video { transform:scale(1.04); }

.doc-placeholder {
  width:100%; height:100%; display:flex; align-items:center; justify-content:center;
  background:#f0f5ff; font-size:36px; color:#1677ff;
}

.type-tag {
  position:absolute; top:5px; right:5px; font-size:10px; line-height:1;
  padding:2px 5px; border-radius:3px; background:rgba(0,0,0,.5); color:#fff;
}
.type-tag.doc { background:rgba(22,119,255,.7); }

.card-actions {
  position:absolute; inset:0; background:rgba(0,0,0,.4);
  display:flex; align-items:center; justify-content:center; gap:8px;
  opacity:0; transition:opacity .15s;
}
.media-card:hover .card-actions { opacity:1; }
.card-actions button {
  width:32px; height:32px; border:none; border-radius:50%;
  background:rgba(255,255,255,.2); color:#fff; font-size:15px;
  cursor:pointer; display:flex; align-items:center; justify-content:center;
  transition:background .15s;
}
.card-actions button:hover { background:rgba(255,255,255,.45); }
.card-actions .act-danger:hover { background:rgba(255,77,79,.8); }

.card-info { padding:6px 8px; }
.card-name {
  display:block; font-size:11px; color:#333; line-height:1.3;
  overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
}
.card-meta { font-size:10px; color:#bbb; }

/* ─── 预览 ─── */
.preview-box {
  display:flex; justify-content:center; align-items:center;
  min-height:200px; background:#0a0a0a; border-radius:8px; overflow:hidden;
}
.preview-box img  { max-width:100%; max-height:70vh; object-fit:contain; }
.preview-box video { width:100%; max-height:70vh; }
.preview-box iframe { width:100%; height:70vh; border:none; }
.doc-fallback {
  display:flex; flex-direction:column; align-items:center;
  padding:40px; background:#fafafa; border-radius:8px; width:100%;
}
.doc-fallback p { margin:8px 0 12px; color:#666; font-size:13px; }
</style>
