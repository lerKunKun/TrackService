<template>
  <div class="page-content">
    <a-card title="菜单管理" :bordered="false">
      <a-empty description="菜单管理页面开发中...">
        <template #description>
          <p>此功能正在开发中</p>
          <p>您可以通过 API 管理菜单：</p>
          <ul style="text-align: left; margin-top: 16px">
            <li>GET /api/v1/menus - 获取所有菜单</li>
            <li>GET /api/v1/menus/tree - 获取菜单树</li>
            <li>POST /api/v1/menus - 创建菜单</li>
            <li>PUT /api/v1/menus/:id - 更新菜单</li>
            <li>DELETE /api/v1/menus/:id - 删除菜单</li>
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
