import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 根据角色做权限判断
 * role: 0=STUDENT, 1=STAFF, 2=ADMIN
 */
export function useAuth() {
  const userStore = useUserStore()

  const role = computed(() => userStore.userInfo?.role ?? 0)

  const isStudent = computed(() => role.value === 0)
  const isStaff = computed(() => role.value === 1)
  const isAdmin = computed(() => role.value === 2)

  function hasRole(name) {
    const r = role.value
    if (name === 'STUDENT') return r === 0
    if (name === 'STAFF') return r === 1
    if (name === 'ADMIN') return r === 2
    return false
  }

  function hasAnyRole(...names) {
    return names.some((n) => hasRole(n))
  }

  return { role, isStudent, isStaff, isAdmin, hasRole, hasAnyRole }
}
