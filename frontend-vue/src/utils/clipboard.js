import { message } from 'ant-design-vue'

export async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch (_) {
    message.error('复制失败')
  }
}
