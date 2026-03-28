<template>
  <div class="error-page">
    <h1>404</h1>
    <p>抱歉，您访问的页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  color: $text-regular;
}

.error-page h1 {
  font-size: 120px;
  color: $primary-color;
  margin: 0;
}

.error-page p {
  font-size: $font-size-xl;
  margin: $spacing-lg 0 32px;
}
</style>
