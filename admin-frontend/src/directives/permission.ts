import type { App, Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/modules/user'

const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const userStore = useUserStore()
    const value = binding.value
    const perms = Array.isArray(value) ? value : [value]

    const hasPermission = perms.some(p => userStore.hasPermission(p))

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function setupPermissionDirective(app: App) {
  app.directive('permission', permissionDirective)
}
