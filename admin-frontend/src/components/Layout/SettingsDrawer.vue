<template>
  <el-drawer v-model="visible" :title="$t('common.settings.title')" :size="300" direction="rtl">
    <div class="settings-section">
      <h4>{{ $t('common.settings.themeMode') }}</h4>
      <el-radio-group :model-value="appStore.themeMode" @change="(val: string) => appStore.setThemeMode(val as 'light' | 'dark' | 'auto')">
        <el-radio-button value="light">{{ $t('common.settings.light') }}</el-radio-button>
        <el-radio-button value="dark">{{ $t('common.settings.dark') }}</el-radio-button>
        <el-radio-button value="auto">{{ $t('common.settings.auto') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="settings-section">
      <h4>{{ $t('common.settings.themeColor') }}</h4>
      <div class="color-list">
        <div
          v-for="color in colorPresets"
          :key="color"
          class="color-item"
          :style="{ backgroundColor: color }"
          :class="{ active: appStore.themeColor === color }"
          @click="appStore.setThemeColor(color)"
        />
        <el-color-picker
          :model-value="appStore.themeColor"
          @change="(val: string | null) => val && appStore.setThemeColor(val)"
          size="small"
        />
      </div>
    </div>

    <div class="settings-section">
      <h4>{{ $t('common.settings.layoutMode') }}</h4>
      <el-radio-group :model-value="appStore.layoutMode" @change="(val: string) => appStore.setLayoutMode(val as 'sidebar' | 'top')">
        <el-radio-button value="sidebar">{{ $t('common.settings.sidebarLayout') }}</el-radio-button>
        <el-radio-button value="top">{{ $t('common.settings.topLayout') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="settings-section">
      <h4>{{ $t('common.settings.display') }}</h4>
      <div class="settings-item">
        <span>{{ $t('common.settings.fixedHeader') }}</span>
        <el-switch v-model="appStore.fixedHeader" />
      </div>
      <div class="settings-item">
        <span>{{ $t('common.settings.showTagsView') }}</span>
        <el-switch v-model="appStore.showTagsView" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/modules/app'

const visible = defineModel<boolean>({ default: false })
const appStore = useAppStore()

const colorPresets = [
  '#409eff', '#67C23A', '#722ED1',
  '#F56C6C', '#E6A23C', '#13c2c2'
]
</script>

<style scoped lang="scss">
.settings-section {
  margin-bottom: 24px;

  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}

.color-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.color-item {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;

  &.active {
    border-color: var(--el-text-color-primary);
  }
}

.settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}
</style>
