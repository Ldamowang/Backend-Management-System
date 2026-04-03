import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElNotification } from 'element-plus'
import { getUnreadCount, markNoticeRead, markAllNoticesRead } from '@/api/modules/notice'
import { useWebSocket } from '@/composables/useWebSocket'

export interface NoticePayload {
  id: number
  title: string
  noticeType: number
  createdTime: string
}

export const useNoticeStore = defineStore('notice', () => {
  const unreadCount = ref(0)
  const latestNotices = ref<NoticePayload[]>([])
  const { connect, subscribe, disconnect } = useWebSocket()

  async function fetchUnreadCount() {
    try {
      const { data } = await getUnreadCount()
      unreadCount.value = data
    } catch { /* ignore */ }
  }

  function onNewNotice(notice: NoticePayload) {
    unreadCount.value++
    latestNotices.value = [notice, ...latestNotices.value].slice(0, 10)
    ElNotification({
      title: notice.noticeType === 2 ? '新公告' : '新通知',
      message: notice.title,
      type: 'info',
      duration: 5000
    })
  }

  function initWebSocket(userId: number) {
    connect()
    const checkAndSubscribe = () => {
      const ws = useWebSocket()
      if (ws.connected.value) {
        subscribe('/topic/notice/broadcast', (data) => onNewNotice(data as NoticePayload))
        subscribe(`/user/${userId}/queue/notice`, (data) => onNewNotice(data as NoticePayload))
      } else {
        setTimeout(checkAndSubscribe, 500)
      }
    }
    checkAndSubscribe()
  }

  async function markRead(noticeId: number) {
    await markNoticeRead(noticeId)
    if (unreadCount.value > 0) unreadCount.value--
  }

  async function markAllRead() {
    await markAllNoticesRead()
    unreadCount.value = 0
  }

  function cleanup() {
    disconnect()
    unreadCount.value = 0
    latestNotices.value = []
  }

  return {
    unreadCount,
    latestNotices,
    fetchUnreadCount,
    initWebSocket,
    markRead,
    markAllRead,
    cleanup
  }
})
