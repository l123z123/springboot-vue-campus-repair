<template>
  <component :is="resolvedComponent" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Home from '@/views/Home.vue'
import StaffWorkbench from '@/views/StaffWorkbench.vue'
import DashboardMobile from '@/views/admin/DashboardMobile.vue'

defineOptions({ name: 'Home' })

const router = useRouter()
const userStore = useUserStore()

const isMobile = ref(window.innerWidth < 768)

const resolvedComponent = computed(() => {
  const role = userStore.userInfo?.role ?? 0
  const isAdmin = role === 2
  const isStaff = role === 1

  if (isAdmin && !isMobile.value) {
    return null
  }
  if (isAdmin && isMobile.value) {
    return DashboardMobile
  }
  if (isStaff) {
    return StaffWorkbench
  }
  return Home
})

onMounted(() => {
  const role = userStore.userInfo?.role ?? 0
  if (role === 2 && !isMobile.value) {
    router.replace('/admin/dashboard')
  }
})
</script>
