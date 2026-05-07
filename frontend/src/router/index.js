import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: { requiresAuth: false }
  },
  { path: '/upload', redirect: '/publish' },
  { path: '/repair-list', redirect: '/profile' },

  // 管理后台（AdminLayout：左侧栏 + 顶栏）
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '首页概览' }
      },
      {
        path: 'workorders',
        redirect: '/admin/tickets'
      },
      {
        path: 'tickets',
        name: 'AdminTickets',
        component: () => import('@/views/admin/Tickets.vue'),
        meta: { title: '工单管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'stats',
        name: 'AdminStats',
        component: () => import('@/views/admin/Stats.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'feedback',
        name: 'FeedbackManage',
        component: () => import('@/views/admin/FeedbackManage.vue'),
        meta: { title: '反馈管理' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '系统设置' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },

  // 学生/维修工端（AppLayout：底部导航）
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    meta: { requiresAuth: true, showBottomNav: false },
    children: [
      { path: '', redirect: '/home' },
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/SmartHome.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/Publish.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'message',
        name: 'Message',
        component: () => import('@/views/Message.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'message/:id',
        name: 'MessageDetail',
        component: () => import('@/views/MessageDetail.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'repair/create',
        name: 'RepairCreate',
        component: () => import('@/views/Publish.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'repair/detail/:id',
        name: 'RepairDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'order-chat/:orderId',
        name: 'OrderChat',
        component: () => import('@/views/OrderChat.vue'),
        meta: { showBottomNav: false, title: '沟通' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'help',
        name: 'Help',
        component: () => import('@/views/Help.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'booking',
        name: 'Booking',
        component: () => import('@/views/Booking.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'staff/workbench',
        name: 'StaffWorkbench',
        component: () => import('@/views/StaffWorkbench.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'staff/message',
        name: 'StaffMessage',
        component: () => import('@/views/Message.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'staff/message/:id',
        name: 'StaffMessageDetail',
        component: () => import('@/views/MessageDetail.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'staff/notice',
        name: 'StaffNotice',
        component: () => import('@/views/Notice.vue'),
        meta: { showBottomNav: true }
      },
      {
        path: 'notice',
        name: 'Notice',
        component: () => import('@/views/Notice.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'profile/edit',
        name: 'ProfileEdit',
        component: () => import('@/views/ProfileEdit.vue'),
        meta: { showBottomNav: false }
      },
      {
        path: 'repair/history',
        name: 'RepairHistory',
        redirect: () => ({ path: '/profile', query: { scroll: 'records' } })
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 无需登录即可访问的路径（白名单）
const PUBLIC_PATHS = ['/login', '/register', '/forgot-password']

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isPublic = PUBLIC_PATHS.some((p) => to.path === p || to.path.startsWith(p + '/'))
  let roleName = 'STUDENT'
  try {
    const userStore = useUserStore()
    const role = userStore.userInfo?.role ?? 0
    const roleNames = { 0: 'STUDENT', 1: 'STAFF', 2: 'ADMIN' }
    roleName = roleNames[role] || 'STUDENT'
  } catch (_) {}

  const allowedRouteNames = {
    STUDENT: ['Home', 'Publish', 'Message', 'MessageDetail', 'Profile', 'OrderDetail', 'OrderChat', 'Booking', 'Notice', 'ProfileEdit', 'RepairDetail', 'Help'],
    STAFF: ['StaffWorkbench', 'StaffMessage', 'StaffMessageDetail', 'StaffNotice', 'Profile', 'OrderDetail', 'OrderChat', 'RepairDetail', 'Home', 'ProfileEdit', 'Help'],
    ADMIN: ['Home', 'AdminDashboard', 'AdminTickets', 'AdminUsers', 'AdminStats', 'AdminSettings', 'AdminProfile', 'FeedbackManage', 'Publish', 'Message', 'MessageDetail', 'Profile', 'OrderDetail', 'OrderChat', 'Notice', 'ProfileEdit', 'Booking', 'Help', 'RepairDetail']
  }

  // 未登录访问受保护页面 -> 强制跳转登录
  if (!isPublic && !token) {
    try {
      const userStore = useUserStore()
      userStore.setToken('')
      userStore.setUserInfo(null)
    } catch (_) {}
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录访问登录页 -> 根据角色重定向到对应首页
  if (to.path === '/login' && token) {
    try {
      const userStore = useUserStore()
      const role = userStore.userInfo?.role ?? 0
      const roleHomes = { 2: '/admin/dashboard', 1: '/staff/workbench', 0: '/home' }
      const target = roleHomes[role] ?? '/home'
      next(target)
    } catch (_) {
      next('/home')
    }
    return
  }

  // 登录页直接放行
  if (isPublic) {
    next()
    return
  }

  // 角色校验（基于 meta.roles 或角色允许列表）
  if (to.meta.roles?.length) {
    try {
      const userStore = useUserStore()
      const role = userStore.userInfo?.role ?? 0
      const roleNames = { 0: 'STUDENT', 1: 'STAFF', 2: 'ADMIN' }
      const userRole = roleNames[role]
      if (!to.meta.roles.includes(userRole)) {
        next('/home')
        return
      }
    } catch (_) {}
    next()
  } else {
    next()
  }
})

export default router
