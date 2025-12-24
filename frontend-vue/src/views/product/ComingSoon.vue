<template>
  <div class="coming-soon">
    <div class="icon">🚧</div>
    <h2>功能开发中</h2>
    <p>该功能正在紧张开发中，敬请期待...</p>
  </div>
</template>

<script>
export default {
  name: 'ComingSoon'
}
</script>

<style scoped>
.coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  text-align: center;
}

.icon {
  font-size: 80px;
  margin-bottom: 20px;
}

h2 {
  color: #409EFF;
  margin-bottom: 10px;
}

p {
  color: #909399;
}
</style>
