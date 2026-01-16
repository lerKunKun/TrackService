<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="角色名称">
      <a-input v-model:value="formData.roleName" placeholder="输入角色名称" @change="handleChange" />
    </a-form-item>
    
    <a-form-item label="角色编码">
      <a-input v-model:value="formData.roleCode" disabled />
    </a-form-item>
    
    <a-form-item label="角色描述">
      <a-textarea v-model:value="formData.description" :rows="3" placeholder="输入角色描述" @change="handleChange" />
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
    
    <a-form-item label="拥有权限">
      <div class="related-items">
        <a-tag v-for="perm in formData.permissions" :key="perm.id" color="orange">
          {{ perm.permissionName }}
        </a-tag>
        <span v-if="!formData.permissions?.length" class="no-data">暂无权限</span>
      </div>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const emit = defineEmits(['update'])

const formData = ref({ ...props.data })

watch(() => props.data, (newData) => {
  formData.value = { ...newData }
}, { deep: true })

const handleChange = () => {
  emit('update', { label: formData.value.roleName })
}
</script>

<style scoped>
.node-form { padding: 8px 0; }
.related-items { display: flex; flex-wrap: wrap; gap: 4px; }
.no-data { color: #999; font-size: 12px; }
</style>
