import { useUserStore } from '@/stores/modules/user'

export function usePermission() {
  const userStore = useUserStore()

  function hasPermission(perm: string | string[]): boolean {
    const perms = Array.isArray(perm) ? perm : [perm]
    return perms.some(p => userStore.hasPermission(p))
  }

  function hasRole(role: string): boolean {
    return userStore.roles.includes(role)
  }

  return { hasPermission, hasRole }
}
