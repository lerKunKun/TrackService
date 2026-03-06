<template>
  <div class="product-media-page">
    <!-- 产品列表 -->
    <a-card :bordered="false" class="product-list-card">
      <div class="list-header">
        <a-input-search
          v-model:value="searchTitle"
          placeholder="搜索产品名称..."
          style="width:300px"
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
        :rowClassName="r => r.productId === selectedProduct?.productId ? 'selected-row' : ''"
        :customRow="r => ({ onClick: () => selectProduct(r) })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span class="product-name">{{ record.productName || '—' }}</span>
          </template>
          <template v-if="column.key === 'tags'">
            <a-space>
              <a-badge v-for="tag in TAG_LIST" :key="tag.value"
                :count="record.tagFileCounts?.[tag.value] || 0" :color="tag.color" show-zero>
                <a-tag :color="tag.color">{{ tag.label }}</a-tag>
              </a-badge>
            </a-space>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click.stop="selectProduct(record)">管理媒体</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 媒体管理抽屉 -->
    <a-drawer
      v-model:open="drawerOpen"
      :title="`产品媒体 — ${selectedProduct?.productName}`"
      width="960"
      destroyOnClose
    >
      <div class="drawer-body">
        <!-- 对标页链接（多个） -->
        <a-card size="small" title="对标页链接" style="margin-bottom:16px">
          <div v-for="(link, idx) in referenceLinks" :key="idx" class="ref-link-row">
            <a-input
              v-model:value="referenceLinks[idx]"
              placeholder="输入对标页 URL..."
              allow-clear style="flex:1"
            />
            <a-button type="text" danger @click="removeRefLink(idx)"><delete-outlined /></a-button>
          </div>
          <div class="ref-link-actions">
            <a-button type="dashed" block @click="addRefLink"><plus-outlined /> 添加链接</a-button>
            <a-button type="primary" :loading="savingLink" @click="saveReferenceLinks" style="margin-top:8px">
              保存对标链接
            </a-button>
          </div>
        </a-card>

        <!-- Tag 标签页 -->
        <a-tabs v-model:activeKey="activeTag" @change="onTagChange">
          <a-tab-pane v-for="tag in TAG_LIST" :key="tag.value" :tab="tag.label">

            <!-- 主图 tab：静默拉取入口 -->
            <div v-if="tag.value === 'main-image'" class="sync-banner">
              <span class="sync-desc">产品主图来自导入时的产品图及变体图片</span>
              <a-button
                size="small"
                type="primary"
                :loading="syncing"
                @click="doSyncImages"
              >
                <sync-outlined /> 同步主图到 MinIO
              </a-button>
              <div v-if="syncResult" class="sync-result">{{ syncResult }}</div>
            </div>


            <!-- URL 批量下载区 -->
            <a-card size="small" style="margin-bottom:12px">
              <template #title><link-outlined /> URL 批量下载</template>
              <a-textarea
                v-model:value="urlInputText[tag.value]"
                :placeholder="'每行一个 URL，例如:\nhttps://example.com/image1.jpg\nhttps://example.com/video.mp4'"
                :rows="4" style="font-size:12px;font-family:monospace" allow-clear
              />
              <div style="margin-top:8px;display:flex;gap:8px;align-items:center">
                <a-button
                  type="primary"
                  :loading="downloadingUrls[tag.value]"
                  :disabled="!urlInputText[tag.value]?.trim()"
                  @click="startUrlDownload(tag.value)"
                >
                  <cloud-download-outlined /> 开始下载并保存到 MinIO
                </a-button>
                <span v-if="downloadResultText[tag.value]" class="dl-result-text">
                  {{ downloadResultText[tag.value] }}
                </span>
              </div>
              <!-- 下载结果 -->
              <div v-if="downloadResults[tag.value]?.length" class="dl-result-list">
                <div v-for="r in downloadResults[tag.value]" :key="r.url"
                  :class="['dl-result-item', r.success ? 'dl-ok' : 'dl-fail']">
                  <check-circle-outlined v-if="r.success" class="dl-icon" />
                  <close-circle-outlined v-else class="dl-icon" />
                  <span class="dl-url">{{ r.url }}</span>
                  <span v-if="!r.success" class="dl-err">{{ r.error }}</span>
                </div>
              </div>
            </a-card>

            <!-- 手动上传区 -->
            <div class="upload-zone"
              @dragover.prevent @drop.prevent="handleDrop($event, tag.value)"
              @click="triggerFileInput(tag.value)">
              <upload-outlined />
              <p>拖拽或点击上传文件（图片/视频）</p>
              <p class="upload-hint">支持 jpg/png/gif/webp/mp4/mov，单文件最大 200MB</p>
            </div>
            <input :ref="el => fileInputRefs[tag.value] = el"
              type="file" multiple accept="image/*,video/*"
              style="display:none" @change="handleFileChange($event, tag.value)" />

            <!-- 上传进度 -->
            <div v-if="uploadQueue[tag.value]?.length" class="upload-progress-list">
              <div v-for="item in uploadQueue[tag.value]" :key="item.name" class="progress-item">
                <span class="file-name-mini">{{ item.name }}</span>
                <a-progress :percent="item.progress" size="small" style="flex:1;margin:0 8px" />
                <a-tag :color="item.status==='done'?'green':item.status==='error'?'red':'blue'">
                  {{ item.status==='done'?'完成':item.status==='error'?'失败':'上传中' }}
                </a-tag>
              </div>
            </div>

            <!-- 文件网格 -->
            <a-spin :spinning="filesLoading">
              <div class="media-grid" v-if="filesByTag[tag.value]?.length">
                <div class="media-item" v-for="file in filesByTag[tag.value]" :key="file.objectName">
                  <template v-if="file.mediaType==='video'">
                    <video :src="file.url" class="media-thumb" @click="openPreview(file)" />
                    <div class="media-type-badge video-badge">视频</div>
                  </template>
                  <template v-else>
                    <img :src="file.url" class="media-thumb" @click="openPreview(file)" />
                  </template>
                  <div class="media-overlay">
                    <a-tooltip title="删除">
                      <delete-outlined class="delete-btn" @click.stop="confirmDelete(file)" />
                    </a-tooltip>
                  </div>
                  <div class="media-filename">{{ file.fileName }}</div>
                </div>
              </div>
              <a-empty v-else description="暂无文件" style="margin-top:24px" />
            </a-spin>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-drawer>

    <!-- 预览 Modal -->
    <a-modal v-model:open="previewOpen" :title="previewFile?.fileName"
      :footer="null" width="80%" centered destroyOnClose>
      <div class="preview-container">
        <video v-if="previewFile?.mediaType==='video'" :src="previewFile?.url"
          controls autoplay style="width:100%;max-height:70vh;border-radius:8px" />
        <img v-else :src="previewFile?.url"
          style="width:100%;max-height:70vh;object-fit:contain;border-radius:8px" />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  UploadOutlined, DeleteOutlined, PlusOutlined, LinkOutlined,
  CloudDownloadOutlined, CheckCircleOutlined, CloseCircleOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import {
  getProductMediaList, getProductMediaFiles,
  uploadProductMediaFile, deleteProductMediaFile,
  getReferenceLink, updateReferenceLink,
  downloadFromUrls, syncProductImages
} from '@/api/product-media-template'

const TAG_LIST = [
  { value: 'main-image',   label: '主图',     color: 'blue'   },
  { value: 'detail-media', label: '详情素材', color: 'purple' },
  { value: 'ad-media',     label: '广告素材', color: 'orange' }
]

// ─── 产品列表 ───
const productList  = ref([])
const tableLoading = ref(false)
const searchTitle  = ref('')
const pagination   = reactive({ current:1, pageSize:10, total:0, showSizeChanger:true })
const productCols  = [
  { title:'产品名称', key:'name',   dataIndex:'productName', ellipsis:true },
  { title:'媒体文件', key:'tags',   width:280 },
  { title:'操作',    key:'action', width:100 }
]

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
  } catch (_) { message.error('加载失败') }
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
const activeTag       = ref('main-image')
const filesByTag      = reactive({})
const filesLoading    = ref(false)
const fileInputRefs   = reactive({})
const uploadQueue     = reactive({})

async function selectProduct(record) {
  selectedProduct.value = record
  drawerOpen.value = true
  activeTag.value = 'main-image'
  await Promise.all([loadRefLinks(), loadFiles()])
}

function onTagChange() { loadFiles() }

async function loadFiles() {
  if (!selectedProduct.value) return
  filesLoading.value = true
  try {
    const res = await getProductMediaFiles(selectedProduct.value.productId, activeTag.value)
    filesByTag[activeTag.value] = res.data || []
  } catch (_) {
    filesByTag[activeTag.value] = []
  } finally { filesLoading.value = false }
}

// ─── 对标链接 ───
const referenceLinks = ref([])
const savingLink     = ref(false)

async function loadRefLinks() {
  try {
    const res = await getReferenceLink(selectedProduct.value.productId)
    referenceLinks.value = Array.isArray(res.data) && res.data.length ? [...res.data] : ['']
  } catch (_) { referenceLinks.value = [''] }
}
function addRefLink()       { referenceLinks.value.push('') }
function removeRefLink(idx) {
  referenceLinks.value.splice(idx, 1)
  if (!referenceLinks.value.length) referenceLinks.value = ['']
}
async function saveReferenceLinks() {
  savingLink.value = true
  try {
    const cleaned = referenceLinks.value.filter(l => l.trim())
    await updateReferenceLink(selectedProduct.value.productId, cleaned)
    message.success('对标链接保存成功')
  } catch (_) { message.error('保存失败') }
  finally { savingLink.value = false }
}

// ─── 静默拉取主图 ───
const syncing    = ref(false)
const syncResult = ref('')

async function doSyncImages() {
  syncing.value    = true
  syncResult.value = '同步中...'
  try {
    const res = await syncProductImages(selectedProduct.value.productId)
    const d   = res.data || {}
    syncResult.value = d.message || '完成'
    if ((d.synced || 0) > 0) {
      await loadFiles()
      await loadProducts()
    }
  } catch (e) {
    syncResult.value = '同步失败'
    message.error('图片同步失败: ' + e.message)
  } finally { syncing.value = false }
}

// ─── URL 批量下载 ───
const urlInputText       = reactive({})
const downloadingUrls    = reactive({})
const downloadResults    = reactive({})
const downloadResultText = reactive({})

async function startUrlDownload(tag) {
  const raw  = urlInputText[tag]?.trim()
  if (!raw) return
  const urls = raw.split('\n').map(u => u.trim()).filter(Boolean)
  if (!urls.length) { message.warn('请至少输入一个 URL'); return }

  downloadingUrls[tag]    = true
  downloadResults[tag]    = []
  downloadResultText[tag] = `正在下载 ${urls.length} 个文件...`
  try {
    const res       = await downloadFromUrls(selectedProduct.value.productId, tag, urls)
    const results   = res.data || []
    downloadResults[tag] = results
    const ok   = results.filter(r => r.success).length
    const fail = results.filter(r => !r.success).length
    downloadResultText[tag] = `完成：${ok} 个成功${fail ? '，' + fail + ' 个失败' : ''}`
    if (ok) { urlInputText[tag] = ''; await loadFiles(); await loadProducts() }
  } catch (e) {
    message.error('下载请求失败')
    downloadResultText[tag] = '请求失败'
  } finally { downloadingUrls[tag] = false }
}

// ─── 手动上传 ───
function triggerFileInput(tag) { fileInputRefs[tag]?.click() }
function handleDrop(e, tag) {
  uploadFiles([...e.dataTransfer.files].filter(f =>
    f.type.startsWith('image/') || f.type.startsWith('video/')), tag)
}
function handleFileChange(e, tag) { uploadFiles([...e.target.files], tag); e.target.value = '' }

async function uploadFiles(files, tag) {
  if (!files.length) return
  if (!uploadQueue[tag]) uploadQueue[tag] = []
  for (const file of files) {
    const item = reactive({ name:file.name, progress:0, status:'uploading' })
    uploadQueue[tag].push(item)
    try {
      await uploadProductMediaFile(selectedProduct.value.productId, tag, file,
        e => { item.progress = Math.round((e.loaded / e.total) * 100) })
      item.status = 'done'; item.progress = 100
      message.success(`${file.name} 上传成功`)
    } catch (_) { item.status = 'error'; message.error(`${file.name} 上传失败`) }
  }
  await loadFiles(); await loadProducts()
  setTimeout(() => { uploadQueue[tag] = uploadQueue[tag].filter(i => i.status !== 'done') }, 3000)
}

// ─── 删除 ───
function confirmDelete(file) {
  Modal.confirm({
    title:'确认删除', content:`确定删除 "${file.fileName}" 吗？`,
    okText:'删除', okType:'danger', cancelText:'取消',
    async onOk() {
      await deleteProductMediaFile(file.objectName)
      message.success('已删除')
      await loadFiles(); await loadProducts()
    }
  })
}

// ─── 预览 ───
const previewOpen = ref(false)
const previewFile = ref(null)
function openPreview(file) { previewFile.value = file; previewOpen.value = true }

onMounted(loadProducts)
</script>

<style scoped>
.product-media-page { padding: 0; }
.product-list-card  { margin-bottom: 16px; }
.list-header        { display:flex; justify-content:space-between; margin-bottom:16px; }
.product-name       { font-weight: 500; }
:deep(.selected-row)  { background:#e6f4ff; }
:deep(.ant-table-row) { cursor: pointer; }

.sync-banner {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  background: #e6f4ff; border: 1px solid #91caff; border-radius: 6px;
  padding: 8px 14px; margin-bottom: 14px;
}
.sync-desc   { font-size: 13px; color: #1677ff; flex: 1; min-width: 160px; }
.sync-result {
  width: 100%; font-size: 12px; color: #389e0d;
  background: #f6ffed; border: 1px solid #b7eb8f;
  border-radius: 4px; padding: 4px 10px; margin-top: 4px;
}

.ref-link-row     { display:flex; align-items:center; gap:8px; margin-bottom:8px; }

.ref-link-actions { margin-top:4px; display:flex; flex-direction:column; }

.dl-result-text  { font-size:13px; color:#666; }
.dl-result-list  { margin-top:8px; max-height:140px; overflow-y:auto; }
.dl-result-item  { display:flex; align-items:center; gap:6px; padding:3px 0; font-size:12px; border-bottom:1px solid #f0f0f0; }
.dl-ok           { color:#52c41a; }
.dl-fail         { color:#ff4d4f; }
.dl-icon         { font-size:14px; flex-shrink:0; }
.dl-url          { flex:1; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; color:#333; }
.dl-err          { color:#ff4d4f; flex-shrink:0; }

.upload-zone {
  border:2px dashed #d9d9d9; border-radius:8px; padding:20px;
  text-align:center; cursor:pointer; background:#fafafa;
  transition:border-color .3s, background .3s; margin-bottom:12px;
}
.upload-zone:hover { border-color:#1677ff; background:#f0f7ff; }
.upload-zone .anticon { font-size:28px; color:#1677ff; }
.upload-zone p   { margin:4px 0; color:#666; font-size:13px; }
.upload-hint     { color:#999 !important; font-size:12px !important; }

.upload-progress-list { margin-bottom:12px; }
.progress-item        { display:flex; align-items:center; margin-bottom:6px; }
.file-name-mini       { width:140px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:12px; color:#555; }

.media-grid {
  display:grid; grid-template-columns:repeat(auto-fill, minmax(150px,1fr));
  gap:12px; margin-top:12px;
}
.media-item        { position:relative; border-radius:8px; overflow:hidden; background:#f5f5f5; aspect-ratio:1; cursor:pointer; }
.media-thumb       { width:100%; height:100%; object-fit:cover; display:block; transition:transform .2s; }
.media-item:hover .media-thumb   { transform:scale(1.04); }
.media-item:hover .media-overlay { opacity:1; }
.media-overlay     {
  position:absolute; inset:0; background:rgba(0,0,0,.4);
  display:flex; align-items:center; justify-content:center;
  opacity:0; transition:opacity .2s;
}
.delete-btn        { font-size:22px; color:#fff; padding:8px; border-radius:50%; background:rgba(255,77,79,.85); cursor:pointer; }
.delete-btn:hover  { background:#ff4d4f; }
.media-type-badge  { position:absolute; top:6px; left:6px; font-size:11px; padding:1px 6px; border-radius:4px; }
.video-badge       { background:rgba(0,0,0,.55); color:#fff; }
.media-filename    {
  position:absolute; bottom:0; left:0; right:0;
  background:linear-gradient(transparent, rgba(0,0,0,.5));
  color:#fff; font-size:11px; padding:4px 6px;
  overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
}
.preview-container {
  display:flex; justify-content:center; align-items:center;
  min-height:200px; background:#000; border-radius:8px; overflow:hidden;
}
</style>
