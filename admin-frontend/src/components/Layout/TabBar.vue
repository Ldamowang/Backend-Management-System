<template>
  <div class="tab-bar">
    <el-icon class="tab-scroll-btn" @click="scrollLeft">
      <ArrowLeft />
    </el-icon>

    <div ref="scrollContainer" class="tab-scroll" @wheel.prevent="handleWheel">
      <draggable
        v-model="tabStore.tabs"
        item-key="path"
        class="tab-list"
        ghost-class="tab-ghost"
        :animation="200"
        @end="onDragEnd"
      >
        <template #item="{ element: tab, index }">
          <div
            class="tab-item"
            :class="{ active: tabStore.activeTab === tab.path, pinned: tab.pinned }"
            @click="handleClick(tab)"
            @contextmenu.prevent="openContextMenu($event, tab, index)"
          >
            <el-icon v-if="tab.pinned" class="pin-icon"><Lock /></el-icon>
            <span class="tab-title">{{ tab.title }}</span>
            <el-icon
              v-if="tab.closable"
              class="tab-close"
              @click.stop="handleClose(tab.path)"
            >
              <Close />
            </el-icon>
          </div>
        </template>
      </draggable>
    </div>

    <el-icon class="tab-scroll-btn" @click="scrollRight">
      <ArrowRight />
    </el-icon>

    <TabContextMenu
      :visible="contextMenu.visible"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :is-pinned="contextMenu.isPinned"
      :closable="contextMenu.closable"
      @refresh="handleRefresh"
      @pin="handlePin"
      @unpin="handleUnpin"
      @close="handleContextClose"
      @close-other="handleCloseOther"
      @close-left="handleCloseLeft"
      @close-right="handleCloseRight"
      @close-all="handleCloseAll"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { useTabStore } from '@/stores/modules/tab'
import TabContextMenu from './TabContextMenu.vue'
import type { Tab } from '@/stores/modules/tab'

const router = useRouter()
const tabStore = useTabStore()
const scrollContainer = ref<HTMLElement>()

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  tabPath: '',
  isPinned: false,
  closable: false
})

function handleClick(tab: Tab) {
  tabStore.setActiveTab(tab.path)
  router.push(tab.path)
}

function handleClose(path: string) {
  const nextPath = tabStore.closeTab(path)
  if (nextPath !== router.currentRoute.value.path) {
    router.push(nextPath)
  }
}

function openContextMenu(e: MouseEvent, tab: Tab, _index: number) {
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.tabPath = tab.path
  contextMenu.isPinned = tab.pinned
  contextMenu.closable = tab.closable
}

function closeContextMenu() {
  contextMenu.visible = false
}

function handleRefresh() {
  tabStore.refreshTab(contextMenu.tabPath)
  closeContextMenu()
}

function handlePin() {
  tabStore.pinTab(contextMenu.tabPath)
  closeContextMenu()
}

function handleUnpin() {
  tabStore.unpinTab(contextMenu.tabPath)
  closeContextMenu()
}

function handleContextClose() {
  const nextPath = tabStore.closeTab(contextMenu.tabPath)
  if (nextPath !== router.currentRoute.value.path) {
    router.push(nextPath)
  }
  closeContextMenu()
}

function handleCloseOther() {
  tabStore.closeOtherTabs(contextMenu.tabPath)
  router.push(contextMenu.tabPath)
  closeContextMenu()
}

function handleCloseLeft() {
  tabStore.closeLeftTabs(contextMenu.tabPath)
  closeContextMenu()
}

function handleCloseRight() {
  tabStore.closeRightTabs(contextMenu.tabPath)
  closeContextMenu()
}

function handleCloseAll() {
  tabStore.closeAllTabs()
  router.push('/dashboard')
  closeContextMenu()
}

function onDragEnd(e: { oldIndex: number; newIndex: number }) {
  if (e.oldIndex !== e.newIndex) {
    tabStore.reorderTabs(e.oldIndex, e.newIndex)
  }
}

function scrollLeft() {
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft -= 200
  }
}

function scrollRight() {
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft += 200
  }
}

function handleWheel(e: WheelEvent) {
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft += e.deltaY
  }
}

function handleClickOutside() {
  closeContextMenu()
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.tab-bar {
  display: flex;
  align-items: center;
  background: var(--header-bg, #fff);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 4px;
  height: 38px;
}

.tab-scroll-btn {
  flex-shrink: 0;
  cursor: pointer;
  padding: 4px;
  color: var(--el-text-color-secondary);
  &:hover { color: var(--el-color-primary); }
}

.tab-scroll {
  flex: 1;
  overflow-x: hidden;
  overflow-y: hidden;
  scroll-behavior: smooth;
}

.tab-list {
  display: flex;
  gap: 4px;
  padding: 4px 0;
  white-space: nowrap;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  border: 1px solid transparent;
  transition: all 0.2s;
  user-select: none;

  &:hover {
    color: var(--el-color-primary);
  }

  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
  }

  &.pinned .tab-title {
    font-weight: 500;
  }
}

.pin-icon {
  font-size: 10px;
}

.tab-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 1px;
  &:hover {
    background: var(--el-color-danger-light-7);
    color: var(--el-color-danger);
  }
}

.tab-ghost {
  opacity: 0.5;
}
</style>
