import { ref, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { getReferenceLink, updateReferenceLink } from '@/api/product-media-template'

/**
 * 对标链接编辑 composable
 * @param {Object} options
 * @param {Function} options.getProductId - 获取当前产品ID的函数
 * @param {boolean} [options.autoSave=true] - 是否在失焦时自动保存
 */
export function useReferenceLinks({ getProductId, autoSave = true } = {}) {
  const referenceLinks = ref([])
  const savingLink = ref(false)
  const editingLinkIdx = ref(-1)
  const editInputRefs = {}

  function setEditInputRef(idx, el) {
    editInputRefs[idx] = el
  }

  async function loadRefLinks(productId) {
    const pid = productId || getProductId?.()
    if (!pid) return
    try {
      const res = await getReferenceLink(pid)
      referenceLinks.value = Array.isArray(res.data) && res.data.length ? [...res.data] : []
      editingLinkIdx.value = -1
    } catch (_) {
      referenceLinks.value = []
    }
  }

  function addRefLink() {
    referenceLinks.value.push('')
    editingLinkIdx.value = referenceLinks.value.length - 1
    nextTick(() => {
      const inp = editInputRefs[editingLinkIdx.value]
      if (inp?.$el) inp.focus()
    })
  }

  function removeRefLink(idx) {
    referenceLinks.value.splice(idx, 1)
    editingLinkIdx.value = -1
    if (autoSave) saveRefLinks()
  }

  async function onRefLinkBlur(idx) {
    if (!autoSave && !referenceLinks.value[idx]?.trim()) {
      referenceLinks.value.splice(idx, 1)
    }
    editingLinkIdx.value = -1
    if (autoSave) await saveRefLinks()
  }

  async function saveRefLinks(productId) {
    const pid = productId || getProductId?.()
    if (!pid) return
    const cleaned = referenceLinks.value.filter(l => l.trim())
    referenceLinks.value = cleaned.length ? cleaned : []
    savingLink.value = true
    try {
      await updateReferenceLink(pid, cleaned)
    } catch (err) {
      message.error('保存失败')
    } finally {
      savingLink.value = false
    }
  }

  async function copyRefLink(link) {
    try {
      await navigator.clipboard.writeText(link)
      message.success('已复制')
    } catch (_) {
      message.error('复制失败')
    }
  }

  return {
    referenceLinks,
    savingLink,
    editingLinkIdx,
    setEditInputRef,
    loadRefLinks,
    addRefLink,
    removeRefLink,
    onRefLinkBlur,
    saveRefLinks,
    copyRefLink
  }
}
