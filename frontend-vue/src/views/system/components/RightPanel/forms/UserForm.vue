<template>
  <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }" class="node-form">
    <a-form-item label="用户名">
      <a-input v-model:value="formData.username" disabled />
    </a-form-item>
    
    <a-form-item label="真实姓名">
      <a-input v-model:value="formData.realName" placeholder="输入真实姓名" @change="handleChange" />
    </a-form-item>
    
    <a-form-item label="邮箱">
      <a-input v-model:value="formData.email" placeholder="输入邮箱地址" @change="handleChange" />
    </a-form-item>
    
    <a-form-item label="手机号">
      <a-input v-model:value="formData.phone" placeholder="输入手机号" @change="handleChange" />
    </a-form-item>
    
    <a-form-item label="状态">
      <a-switch 
        v-model:checked="formData.enabled" 
        checked-children="启用" 
        un-checked-children="禁用"
        @change="handleChange"
      />
    </a-form-item>
    
    <a-form-item label="关联角色">
      <div class="related-items">
        <a-tag v-for="role in formData.roles" :key="role.id" color="purple">
          {{ role.roleName }}
        </a-tag>
        <span v-if="!formData.roles?.length" class="no-data">暂无关联角色</span>
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
  emit('update', { label: formData.value.realName || formData.value.username })
}
</script>

<style scoped>
.node-form { padding: 8px 0; }
.related-items { display: flex; flex-wrap: wrap; gap: 4px; }
.no-data { color: #999; font-size: 12px; }
</style>
