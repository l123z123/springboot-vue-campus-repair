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
        meta: { title: '首页概览', roles: ['ADMIN'] }
      },
      {
        path: 'tickets',
        name: 'AdminTickets',
        component: () => import('@/views/admin/Tickets.vue'),
        meta: { title: '工单管理', roles: ['ADMIN'] }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'stats',
        name: 'AdminStats',
        component: () => import('@/views/admin/Stats.vue'),
        meta: { title: '数据统计', roles: ['ADMIN'] }
      },
      {
        path: 'feedback',
        name: 'FeedbackManage',
        component: () => import('@/views/admin/FeedbackManage.vue'),
        meta: { title: '反馈管理', roles: ['ADMIN'] }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '系统设置', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/Profile.vue'),
        meta: { title: '个人中心', roles: ['ADMIN'] }
      }
    ]
  },

  // 学生/维修工端（AppLayout：底部导航）
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/home' },
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/SmartHome.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/Publish.vue'),
        meta: { title: '发布报修' }
      },
      {
        path: 'repair/create',
        name: 'RepairCreate',
        component: () => import('@/views/Publish.vue'),
        meta: { title: '发布报修' }
      },
      {
        path: 'message',
        name: 'Message',
        component: () => import('@/views/Message.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'message/:id',
        redirect: (to) => `/order-chat/${to.params.id}`
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'profile/edit',
        name: 'ProfileEdit',
        component: () => import('@/views/ProfileEdit.vue'),
        meta: { title: '编辑资料' }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { title: '工单详情' }
      },
      {
        path: 'repair/detail/:id',
        name: 'RepairDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { title: '工单详情' }
      },
      {
        path: 'order-chat/:orderId',
        name: 'OrderChat',
        component: () => import('@/views/OrderChat.vue'),
        meta: { title: '沟通' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '设置' }
      },
      {
        path: 'help',
        name: 'Help',
        component: () => import('@/views/Help.vue'),
        meta: { title: '帮助反馈' }
      },
      {
        path: 'booking',
        name: 'Booking',
        component: () => import('@/views/Booking.vue'),
        meta: { title: '预约维修' }
      },
      {
        path: 'notice',
        name: 'Notice',
        component: () => import('@/views/Notice.vue'),
        meta: { title: '公告通知' }
      },
      {
        path: 'repair/history',
        name: 'RepairHistory',
        redirect: () => ({ path: '/profile', query: { scroll: 'records' } })
      },

      // 维修工端路由
      {
        path: 'staff/workbench',
        name: 'StaffWorkbench',
        component: () => import('@/views/StaffWorkbench.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'staff/message',
        name: 'StaffMessage',
        component: () => import('@/views/Message.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'staff/message/:id',
        redirect: (to) => `/order-chat/${to.params.id}`
      },
      {
        path: 'staff/notice',
        name: 'StaffNotice',
        component: () => import('@/views/Notice.vue'),
        meta: { title: '公告通知' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const PUBLIC_PATHS = ['/login', '/register', '/forgot-password']

const ROLE_HOMES = { 2: '/admin/dashboard', 1: '/staff/workbench', 0: '/home' }
const ROLE_NAMES = { 0: 'STUDENT', 1: 'STAFF', 2: 'ADMIN' }

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isPublic = PUBLIC_PATHS.includes(to.path)

  // 未登录访问受保护页面 -> 跳转登录
  if (!isPublic && !token) {
    try {
      const userStore = useUserStore()
      userStore.setToken('')
      userStore.setUserInfo(null)
    } catch (_) {}
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录访问登录页 -> 根据角色重定向
  if (to.path === '/login' && token) {
    try {
      const userStore = useUserStore()
      const role = userStore.userInfo?.role ?? 0
      next(ROLE_HOMES[role] || '/home')
    } catch (_) {
      next('/home')
    }
    return
  }

  // 公开页面直接放行
  if (isPublic) {
    next()
    return
  }

  // 角色权限校验（基于 meta.roles）
  if (to.meta.roles?.length) {
    try {
      const userStore = useUserStore()
      const role = userStore.userInfo?.role ?? 0
      const userRole = ROLE_NAMES[role] || 'STUDENT'
      if (!to.meta.roles.includes(userRole)) {
        next(ROLE_HOMES[role] || '/home')
        return
      }
    } catch (_) {}
  }

  next()
})

export default router
