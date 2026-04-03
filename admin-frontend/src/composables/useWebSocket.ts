import { ref } from 'vue'
import { Client, type StompSubscription } from '@stomp/stompjs'
import { getToken } from '@/utils/auth'

const client = ref<Client | null>(null)
const connected = ref(false)

export function useWebSocket() {
  function connect() {
    if (client.value?.connected) return

    const wsUrl = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/websocket`

    const stompClient = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${getToken()}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected.value = true
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
        connected.value = false
      }
    })

    stompClient.activate()
    client.value = stompClient
  }

  function subscribe(destination: string, callback: (body: unknown) => void): StompSubscription | undefined {
    if (!client.value) return
    return client.value.subscribe(destination, (message) => {
      try {
        callback(JSON.parse(message.body))
      } catch {
        callback(message.body)
      }
    })
  }

  function disconnect() {
    if (client.value) {
      client.value.deactivate()
      client.value = null
      connected.value = false
    }
  }

  return { connect, subscribe, disconnect, connected, client }
}
