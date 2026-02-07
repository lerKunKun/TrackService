<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="权限名称">
      <a-input v-model:value="formData.permissionName" placeholder="输入权限名称" @change="handleChange" />
    </a-form-item>
    
    <a-form-item label="权限编码">
      <a-input v-model:value="formData.permissionCode" disabled />
    </a-form-item>
    
    <a-form-item label="权限类型">
      <a-tag :color="typeColor">{{ typeLabel }}</a-tag>
    </a-form-item>
    
    <a-form-item label="权限描述">
      <a-textarea v-model:value="formData.description" :rows="2" placeholder="输入权限描述" @change="handleChange" />
    </a-form-item>
    
    <a-form-item label="状态">
      <a-switch 
        v-model:checked="formData.status" 
        :checked-value="1"
        :un-checked-value="0"
        checked-children="启用" 
        un-checked-children="禁用"
        @change="handleChange"
      />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const emit = defineEmits(['update'])

const formData = ref({ ...props.data })

const typeColor = computed(() => {
  const colors = { MENU: 'blue', BUTTON: 'green', DATA: 'orange' }
  return colors[formData.value.permissionType] || 'default'
})

const typeLabel = computed(() => {
  const labels = { MENU: '菜单权限', BUTTON: '按钮权限', DATA: '数据权限' }
  return labels[formData.value.permissionType] || formData.value.permissionType
})

watch(() => props.data, (newData) => {
  formData.value = { ...newData }
}, { deep: true })

const handleChange = () => {
  emit('update', { label: formData.value.permissionName })
}
</script>

<style scoped>
.node-form { padding: 8px 0; }
</style>
