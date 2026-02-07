<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="菜单名称">
      <a-input v-model:value="formData.menuName" placeholder="输入菜单名称" @change="handleChange" />
    </a-form-item>
    <a-form-item label="菜单编码">
      <a-input v-model:value="formData.menuCode" disabled />
    </a-form-item>
    <a-form-item label="菜单图标">
      <a-input v-model:value="formData.icon" placeholder="输入图标名称" @change="handleChange" />
    </a-form-item>
    <a-form-item label="菜单路径">
      <a-input v-model:value="formData.path" placeholder="输入菜单路径" @change="handleChange" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ data: { type: Object, required: true } })
const emit = defineEmits(['update'])
const formData = ref({ ...props.data })
watch(() => props.data, (newData) => { formData.value = { ...newData } }, { deep: true })
const handleChange = () => { emit('update', { label: formData.value.menuName }) }
</script>

<style scoped>.node-form { padding: 8px 0; }</style>
