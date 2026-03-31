<template>
  <div class="table-skeleton">
    <el-skeleton :rows="rows" animated>
      <template #template>
        <div class="skeleton-header">
          <el-skeleton-item variant="text" style="width: 15%" />
          <el-skeleton-item variant="text" style="width: 20%" />
          <el-skeleton-item variant="text" style="width: 20%" />
          <el-skeleton-item variant="text" style="width: 25%" />
          <el-skeleton-item variant="text" style="width: 10%" />
        </div>
        <div v-for="i in rows" :key="i" class="skeleton-row">
          <el-skeleton-item variant="text" style="width: 15%" />
          <el-skeleton-item variant="text" style="width: 20%" />
          <el-skeleton-item variant="text" style="width: 20%" />
          <el-skeleton-item variant="text" style="width: 25%" />
          <el-skeleton-item variant="text" style="width: 10%" />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  rows?: number
}>(), {
  rows: 5
})
</script>

<style scoped>
.table-skeleton {
  padding: 16px 0;
}

.skeleton-header,
.skeleton-row {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.skeleton-header {
  font-weight: bold;
}
</style>
