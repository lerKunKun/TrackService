<template>
  <div class="page-content">
    <a-card title="权限管理" :bordered="false">
      <a-empty description="权限管理页面开发中...">
        <template #description>
          <p>此功能正在开发中</p>
          <p>您可以通过 API 管理权限：</p>
          <ul style="text-align: left; margin-top: 16px">
            <li>GET /api/v1/permissions - 获取所有权限</li>
            <li>POST /api/v1/permissions - 创建权限</li>
            <li>PUT /api/v1/permissions/:id - 更新权限</li>
            <li>DELETE /api/v1/permissions/:id - 删除权限</li>
          </ul>
        </template>
      </a-empty>
    </a-card>
  </div>
</template>

<script setup>
// 暂时空实现，待开发
</script>

<style scoped>
.page-content {
  padding: 24px;
}
ul {
  list-style: disc;
  padding-left: 20px;
}
</style>
