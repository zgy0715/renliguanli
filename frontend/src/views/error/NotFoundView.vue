<template>
  <div class="not-found">
    <div class="code">404</div>
    <div class="text">页面走丢了，您访问的地址不存在</div>
    <el-button type="primary" @click="$router.push('/')">返回工作台</el-button>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: var(--hrms-bg);
}

.code {
  font-size: 96px;
  font-weight: 800;
  background: linear-gradient(135deg, #2f6bf0, #7b61ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.text {
  font-size: 15px;
  color: var(--hrms-text-3);
  margin-bottom: 8px;
}
</style>
