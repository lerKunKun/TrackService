<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="组织名称">
      <a-input v-model:value="formData.name" placeholder="输入组织名称" @change="handleChange" />
    </a-form-item>
    <a-form-item label="组织编码">
      <a-input v-model:value="formData.code" placeholder="输入组织编码" @change="handleChange" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ data: { type: Object, required: true } })
const emit = defineEmits(['update'])
const formData = ref({ ...props.data })
watch(() => props.data, (newData) => { formData.value = { ...newData } }, { deep: true })
const handleChange = () => { emit('update', { label: formData.value.name }) }
</script>

<style scoped>.node-form { padding: 8px 0; }</style>
