<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="部门名称">
      <a-input v-model:value="formData.deptName" placeholder="输入部门名称" @change="handleChange" />
    </a-form-item>
    <a-form-item label="部门编码">
      <a-input v-model:value="formData.deptCode" placeholder="输入部门编码" @change="handleChange" />
    </a-form-item>
    <a-form-item label="上级部门">
      <a-input v-model:value="formData.parentDeptName" disabled />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ data: { type: Object, required: true } })
const emit = defineEmits(['update'])
const formData = ref({ ...props.data })
watch(() => props.data, (newData) => { formData.value = { ...newData } }, { deep: true })
const handleChange = () => { emit('update', { label: formData.value.deptName }) }
</script>

<style scoped>.node-form { padding: 8px 0; }</style>
