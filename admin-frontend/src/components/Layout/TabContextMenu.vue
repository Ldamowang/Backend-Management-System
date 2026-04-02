<template>
  <div
    v-show="visible"
    class="tab-context-menu"
    :style="{ left: x + 'px', top: y + 'px' }"
  >
    <ul>
      <li @click="emit('refresh')">
        <el-icon><Refresh /></el-icon>{{ $t('common.tabs.refresh') }}
      </li>
      <li v-if="!isPinned" @click="emit('pin')">
        <el-icon><Lock /></el-icon>{{ $t('common.tabs.pin') }}
      </li>
      <li v-if="isPinned && closable" @click="emit('unpin')">
        <el-icon><Unlock /></el-icon>{{ $t('common.tabs.unpin') }}
      </li>
      <li v-if="closable" @click="emit('close')" class="divider">
        <el-icon><Close /></el-icon>{{ $t('common.tabs.close') }}
      </li>
      <li @click="emit('closeOther')">
        <el-icon><FolderDelete /></el-icon>{{ $t('common.tabs.closeOther') }}
      </li>
      <li @click="emit('closeLeft')">
        <el-icon><DArrowLeft /></el-icon>{{ $t('common.tabs.closeLeft') }}
      </li>
      <li @click="emit('closeRight')">
        <el-icon><DArrowRight /></el-icon>{{ $t('common.tabs.closeRight') }}
      </li>
      <li @click="emit('closeAll')">
        <el-icon><CircleClose /></el-icon>{{ $t('common.tabs.closeAll') }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  x: number
  y: number
  isPinned: boolean
  closable: boolean
}>()

const emit = defineEmits<{
  refresh: []
  pin: []
  unpin: []
  close: []
  closeOther: []
  closeLeft: []
  closeRight: []
  closeAll: []
}>()
</script>

<style scoped lang="scss">
.tab-context-menu {
  position: fixed;
  z-index: 3001;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    font-size: 13px;
    cursor: pointer;
    color: var(--el-text-color-regular);
    white-space: nowrap;

    &:hover {
      background: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }

    &.divider {
      border-bottom: 1px solid var(--el-border-color-lighter);
      margin-bottom: 4px;
      padding-bottom: 12px;
    }
  }
}
</style>
