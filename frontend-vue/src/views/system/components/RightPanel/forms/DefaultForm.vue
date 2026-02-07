<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="节点名称">
      <a-input v-model:value="formData.label" placeholder="输入节点名称" @change="handleChange" />
    </a-form-item>
    <a-form-item label="节点类型">
      <a-tag>{{ data.nodeType }}</a-tag>
    </a-form-item>
    <a-form-item label="原始数据">
      <pre class="json-preview">{{ JSON.stringify(data, null, 2) }}</pre>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ data: { type: Object, required: true } })
const emit = defineEmits(['update'])
const formData = ref({ label: props.data.label })
watch(() => props.data, (newData) => { formData.value = { label: newData.label } }, { deep: true })
const handleChange = () => { emit('update', formData.value) }
</script>

<style scoped>
.node-form { padding: 8px 0; }
.json-preview { 
  background: #f5f5f5; 
  padding: 8px; 
  border-radius: 4px; 
  font-size: 11px; 
  max-height: 200px; 
  overflow: auto; 
}
</style>
