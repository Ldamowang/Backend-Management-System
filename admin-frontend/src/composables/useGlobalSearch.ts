import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'
import type { RouteRecordRaw } from 'vue-router'
import { usePermissionStore } from '@/stores/modules/permission'
import { getUserList } from '@/api/modules/user'
import { getRoleList } from '@/api/modules/role'
import { getConfigs } from '@/api/modules/config'
import { getDictTypes } from '@/api/modules/dict'

export interface SearchResult {
  category: 'menu' | 'user' | 'role' | 'config' | 'dict'
  title: string
  path: string
  icon?: string
}

const HISTORY_KEY = 'search-history'
const MAX_HISTORY = 10
const MAX_PER_CATEGORY = 5

/** 从路由树中模糊匹配菜单 */
export function filterMenus(routes: RouteRecordRaw[], keyword: string): SearchResult[] {
  if (!keyword) return []
  const results: SearchResult[] = []
  const lowerKeyword = keyword.toLowerCase()

  function walk(items: RouteRecordRaw[]) {
    for (const route of items) {
      const title = (route.meta?.title as string) || ''
      if (title.toLowerCase().includes(lowerKeyword)) {
        results.push({
          category: 'menu',
          title,
          path: route.path,
          icon: route.meta?.icon as string
        })
      }
      if (route.children?.length) {
        walk(route.children)
      }
    }
  }

  walk(routes)
  return results.slice(0, MAX_PER_CATEGORY)
}

export function useGlobalSearch() {
  const router = useRouter()
  const permissionStore = usePermissionStore()

  const visible = ref(false)
  const keyword = ref('')
  const results = ref<SearchResult[]>([])
  const loading = ref(false)
  const selectedIndex = ref(0)
  const history = ref<string[]>(loadHistory())

  function loadHistory(): string[] {
    try {
      return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    } catch {
      return []
    }
  }

  function saveHistory(query: string) {
    if (!query.trim()) return
    history.value = [query, ...history.value.filter(h => h !== query)].slice(0, MAX_HISTORY)
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
  }

  function clearHistory() {
    history.value = []
    localStorage.removeItem(HISTORY_KEY)
  }

  async function search(query: string) {
    if (!query.trim()) {
      results.value = []
      return
    }

    loading.value = true
    selectedIndex.value = 0

    try {
      // 本地搜索：菜单
      const staticRoutes = [{ path: '/dashboard', meta: { title: '仪表盘', icon: 'Monitor' }, children: [] }]
      const menuResults = filterMenus(
        [...staticRoutes, ...permissionStore.dynamicRoutes] as RouteRecordRaw[],
        query
      )

      // API 搜索：并行请求
      const [userRes, roleRes, configRes, dictRes] = await Promise.allSettled([
        getUserList({ username: query, page: 1, size: MAX_PER_CATEGORY }),
        getRoleList(),
        getConfigs(),
        getDictTypes()
      ])

      const apiResults: SearchResult[] = []
      const lowerQuery = query.toLowerCase()

      if (userRes.status === 'fulfilled' && userRes.value?.data?.list) {
        userRes.value.data.list.forEach((u: any) => {
          apiResults.push({ category: 'user', title: `${u.username} (${u.nickname || ''})`, path: '/system/user', icon: 'User' })
        })
      }

      if (roleRes.status === 'fulfilled' && roleRes.value?.data) {
        const roles = Array.isArray(roleRes.value.data) ? roleRes.value.data : []
        roles.filter((r: any) => r.roleName?.toLowerCase().includes(lowerQuery))
          .slice(0, MAX_PER_CATEGORY)
          .forEach((r: any) => {
            apiResults.push({ category: 'role', title: r.roleName, path: '/system/role', icon: 'UserFilled' })
          })
      }

      if (configRes.status === 'fulfilled' && configRes.value?.data) {
        const configs = Array.isArray(configRes.value.data) ? configRes.value.data : []
        configs.filter((c: any) => c.configName?.toLowerCase().includes(lowerQuery) || c.configKey?.toLowerCase().includes(lowerQuery))
          .slice(0, MAX_PER_CATEGORY)
          .forEach((c: any) => {
            apiResults.push({ category: 'config', title: `${c.configName}: ${c.configValue}`, path: '/system/config', icon: 'Tools' })
          })
      }

      if (dictRes.status === 'fulfilled' && dictRes.value?.data) {
        const dicts = Array.isArray(dictRes.value.data) ? dictRes.value.data : []
        dicts.filter((d: any) => d.dictName?.toLowerCase().includes(lowerQuery) || d.dictType?.toLowerCase().includes(lowerQuery))
          .slice(0, MAX_PER_CATEGORY)
          .forEach((d: any) => {
            apiResults.push({ category: 'dict', title: d.dictName, path: '/system/dict', icon: 'Collection' })
          })
      }

      results.value = [...menuResults, ...apiResults]
    } catch {
      results.value = []
    } finally {
      loading.value = false
    }
  }

  const debouncedSearch = useDebounceFn(search, 300)

  watch(keyword, (val) => {
    debouncedSearch(val)
  })

  function open() {
    visible.value = true
    keyword.value = ''
    results.value = []
    selectedIndex.value = 0
  }

  function close() {
    visible.value = false
  }

  function selectResult(result: SearchResult) {
    saveHistory(keyword.value)
    router.push(result.path)
    close()
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, results.value.length - 1)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
    } else if (e.key === 'Enter' && results.value[selectedIndex.value]) {
      selectResult(results.value[selectedIndex.value])
    }
  }

  // 全局快捷键 Ctrl+K / Cmd+K
  function setupShortcut() {
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault()
        open()
      }
    })
  }

  return {
    visible,
    keyword,
    results,
    loading,
    selectedIndex,
    history,
    open,
    close,
    selectResult,
    handleKeydown,
    clearHistory,
    setupShortcut
  }
}
