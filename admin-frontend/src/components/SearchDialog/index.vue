<template>
  <el-dialog
    v-model="search.visible.value"
    :show-close="false"
    :close-on-click-modal="true"
    width="600px"
    top="15vh"
    class="search-dialog"
    @opened="inputRef?.focus()"
  >
    <div class="search-header">
      <el-icon class="search-icon"><Search /></el-icon>
      <input
        ref="inputRef"
        v-model="search.keyword.value"
        class="search-input"
        :placeholder="$t('common.search.placeholder')"
        @keydown="search.handleKeydown"
      />
      <kbd class="search-kbd">ESC</kbd>
    </div>

    <el-scrollbar max-height="400px" class="search-body">
      <!-- 搜索历史 -->
      <div v-if="!search.keyword.value && search.history.value.length" class="search-section">
        <div class="section-header">
          <span>{{ $t('common.search.history') }}</span>
          <el-button link size="small" @click="search.clearHistory()">
            {{ $t('common.search.clearHistory') }}
          </el-button>
        </div>
        <div
          v-for="(item, i) in search.history.value"
          :key="i"
          class="history-item"
          @click="search.keyword.value = item"
        >
          <el-icon><Clock /></el-icon>
          <span>{{ item }}</span>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="search.loading.value" class="search-empty">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>

      <!-- 无结果 -->
      <div v-else-if="search.keyword.value && !search.results.value.length" class="search-empty">
        {{ $t('common.search.noResult') }}
      </div>

      <!-- 搜索结果 -->
      <template v-else>
        <template v-for="category in groupedResults" :key="category.name">
          <div class="search-section">
            <div class="section-header">
              <span>{{ $t(`common.search.category.${category.name}`) }}</span>
              <span class="section-count">{{ category.items.length }}</span>
            </div>
            <SearchResultItem
              v-for="(result, i) in category.items"
              :key="result.path + i"
              :result="result"
              :keyword="search.keyword.value"
              :selected="flatIndex(category.name, i) === search.selectedIndex.value"
              @select="search.selectResult(result)"
            />
          </div>
        </template>
      </template>
    </el-scrollbar>

    <div class="search-footer">
      <span><kbd>&uarr;</kbd><kbd>&darr;</kbd> {{ $t('common.search.placeholder').includes('搜索') ? '导航' : 'Navigate' }}</span>
      <span><kbd>&crarr;</kbd> {{ $t('common.action.confirm') }}</span>
      <span><kbd>ESC</kbd> {{ $t('common.action.close') }}</span>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useGlobalSearch } from '@/composables/useGlobalSearch'
import SearchResultItem from './SearchResultItem.vue'
import type { SearchResult } from '@/composables/useGlobalSearch'

const search = useGlobalSearch()
const inputRef = ref<HTMLInputElement>()

interface GroupedCategory {
  name: string
  items: SearchResult[]
}

const groupedResults = computed<GroupedCategory[]>(() => {
  const groups = new Map<string, SearchResult[]>()
  for (const result of search.results.value) {
    const list = groups.get(result.category) || []
    list.push(result)
    groups.set(result.category, list)
  }
  return Array.from(groups.entries()).map(([name, items]) => ({ name, items }))
})

function flatIndex(category: string, indexInCategory: number): number {
  let offset = 0
  for (const group of groupedResults.value) {
    if (group.name === category) return offset + indexInCategory
    offset += group.items.length
  }
  return -1
}

defineExpose({ search })
</script>

<style lang="scss">
.search-dialog {
  .el-dialog__header { display: none; }
  .el-dialog__body { padding: 0; }
}
</style>

<style scoped lang="scss">
.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.search-icon {
  font-size: 20px;
  color: var(--el-text-color-secondary);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--el-text-color-primary);

  &::placeholder { color: var(--el-text-color-placeholder); }
}

.search-kbd {
  font-size: 12px;
  padding: 2px 6px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
}

.search-body {
  padding: 8px;
}

.search-section {
  margin-bottom: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  text-transform: uppercase;
}

.section-count {
  font-size: 11px;
  background: var(--el-fill-color);
  padding: 1px 6px;
  border-radius: 10px;
}

.search-empty {
  padding: 40px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  color: var(--el-text-color-regular);

  &:hover { background: var(--el-fill-color-light); }
}

.search-footer {
  display: flex;
  gap: 16px;
  padding: 8px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);

  kbd {
    font-size: 11px;
    padding: 1px 4px;
    border: 1px solid var(--el-border-color);
    border-radius: 3px;
    margin-right: 2px;
    background: var(--el-fill-color-lighter);
  }
}
</style>
