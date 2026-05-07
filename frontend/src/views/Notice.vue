<template>
  <div class="notice-page">
    <el-card shadow="never" class="notice-card">
      <template #header>
        <span class="notice-card__title">{{ isStaffTab ? '工作公告' : '公告通知' }}</span>
      </template>
      <div v-if="noticeList.length" class="notice-list">
        <div v-for="(item, i) in noticeList" :key="i" class="notice-item" :class="{'is-pinned': item.pinned}">
          <div class="notice-item__head">
            <el-tag v-if="item.pinned" type="danger" size="small" effect="dark">置顶</el-tag>
            <h4 class="notice-item__title">{{ item.title }}</h4>
          </div>
          <p class="notice-item__content">{{ item.content }}</p>
          <p class="notice-item__time">{{ item.time }} · {{ item.author || '后勤管理处' }}</p>
        </div>
      </div>
      <el-empty v-else description="暂无公告" :image-size="64" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'

defineOptions({ name: 'Notice' })

const STORAGE_KEY = 'noticeList'
const DEFAULT_NOTICES = [
  { title: '关于北苑供水管道检修的通知', content: '因供水管道老化，北苑区域将于5月10日上午8:00-12:00进行停水检修。请北苑各位同学提前做好储水准备。检修期间如发现漏水等异常情况，请立即通过本系统报修。', time: '2026-05-07 14:00', pinned: true, author: '后勤管理处' },
  { title: '五月份校园设施集中维护计划', content: '本月将对全校教学楼、宿舍楼公共设施进行集中巡检维护，包括空调清洗、照明检修、门窗加固等项目。各楼栋具体维护时间将提前张贴通知，请留意公告栏。', time: '2026-05-05 09:00', pinned: true, author: '后勤管理处' },
  { title: '后勤维修服务范围说明', content: '本系统受理范围包括：水电设施维修、门窗修缮、网络故障、家具维修、电器维修等。不属于维修范围的：宿舍个人电器维修、装饰性改造、网络账号问题（请联系信息中心）。', time: '2026-05-01 10:00', pinned: false, author: '后勤管理处' },
  { title: '夏季空调使用及报修注意事项', content: '夏季来临，请合理设置空调温度（建议26℃以上）。如遇空调不制冷、漏水、异响等问题，请通过本系统提交报修，维修人员将在24小时内响应。请勿私自拆卸空调设备。', time: '2026-04-28 15:30', pinned: false, author: '后勤管理处' }
]

function loadList() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) { const arr = JSON.parse(raw); if (Array.isArray(arr) && arr.length) return arr }
  } catch {}
  return [...DEFAULT_NOTICES]
}

const route = useRoute()
const isStaffTab = computed(() => route.path.startsWith('/staff/'))
const noticeList = ref([])

onMounted(() => { noticeList.value = loadList() })
</script>

<style scoped>
.notice-card {
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  max-width: 800px;
}

.notice-card__title {
  font-size: 16px;
  font-weight: 600;
}

.notice-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.notice-item:last-child { border-bottom: none; }
.notice-item.is-pinned { background: var(--el-color-danger-light-9); margin:0 -12px; padding:16px 12px; border-radius:8px; }

.notice-item__head { display:flex; align-items:center; gap:8px; margin-bottom:8px; }
.notice-item__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-item__content {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.notice-item__time {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
