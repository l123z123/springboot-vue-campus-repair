<template>
  <div class="order-detail-page" :class="{ 'order-detail-page--staff': userRole === 1 }">
    <div v-if="loading" class="detail-content">
      <el-skeleton animated :rows="6" />
    </div>
    <div v-else class="detail-content">
      <div class="detail-image">
        <el-image v-if="order.image" :src="order.image" fit="contain" class="detail-image__img" />
        <div v-else class="detail-image__placeholder">
          <el-icon><Picture /></el-icon>
          <span>暂无图片</span>
        </div>
      </div>

      <div class="detail-block">
        <div class="detail-row">
          <span class="detail-label">地点</span>
          <span class="detail-value">{{ order.location }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="statusTagType(order.status)" size="small">{{ order.statusText }}</el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">报修时间</span>
          <span class="detail-value">{{ order.date }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">维修师傅</span>
          <span class="detail-value">{{ order.worker }}</span>
        </div>
      </div>

      <div class="detail-block">
        <h4 class="detail-block__title">故障描述</h4>
        <p class="detail-desc">{{ order.desc }}</p>
      </div>

      <!-- 处理进度：根据真实状态动态生成 -->
      <div class="detail-block">
        <h4 class="detail-block__title">处理进度</h4>
        <el-timeline>
          <el-timeline-item 
            v-for="(item, index) in timelineItems" 
            :key="index" 
            :timestamp="item.time" 
            placement="top"
            :type="item.type"
          >
            {{ item.content }}
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 联系师傅：只有分配了维修工才能显示 -->
      <el-button
        v-if="order.repairmanId && (userRole === 0 || userRole === 1)"
        type="primary"
        class="detail-chat-btn"
        :class="{ 'detail-chat-btn--staff': userRole === 1 }"
        @click="goChat"
      >
        {{ userRole === 1 ? '联系报修人' : '联系维修工' }}
      </el-button>

      <!-- 管理员处理面板 -->
      <div v-if="userRole === 2 && (order.status === 0 || order.status === 1 || order.status === 3)" class="detail-block detail-block--audit">
        <h4 class="detail-block__title">管理员处理</h4>
        <p class="admin-panel__hint">
          {{ order.status === 3 ? '当前为待派单，请选择维修工并派单。' : '先审核工单；审核通过后可进入待派单，或直接采用推荐维修工派单。' }}
        </p>
        <div v-if="order.status === 0 || order.status === 1 || order.status === 3" class="recommend-panel">
          <div class="recommend-panel__head">
            <span>智能派单推荐</span>
            <el-button link type="primary" size="small" :loading="recommendLoading" @click="loadRecommendations">刷新推荐</el-button>
          </div>
          <el-empty v-if="!recommendLoading && !recommendations.length" description="暂无可推荐维修工" :image-size="60" />
          <div v-else class="recommend-list">
            <div v-for="r in recommendations.slice(0, 3)" :key="String(r.repairmanId)" class="recommend-item">
              <div class="recommend-item__main">
                <strong>{{ r.realName || r.username }}</strong>
                <span>推荐分 {{ r.score }} · 在办 {{ r.activeCount }} 单</span>
                <p>{{ (r.reasons || []).join('；') }}</p>
              </div>
              <el-button type="primary" plain size="small" @click="useRecommendation(r.repairmanId)">采用</el-button>
            </div>
          </div>
        </div>
        <el-divider class="admin-panel__divider" />
        <el-form v-if="order.status === 3" :model="dispatchForm" label-position="top">
          <el-form-item label="选择维修工" required>
            <el-select
              v-model="dispatchForm.repairmanId"
              filterable
              clearable
              placeholder="请搜索并选择启用的维修工账号"
              style="width: 100%;"
            >
              <el-option
                v-for="u in repairmanOptions"
                :key="String(u.userId)"
                :label="formatStaffLabel(u)"
                :value="u.userId != null ? String(u.userId) : ''"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="admin-panel__primary-btn" @click="handleDispatch">确认派单</el-button>
          </el-form-item>
        </el-form>
        <el-form v-else :model="auditForm" label-position="top">
          <el-form-item label="审核结果">
            <el-radio-group v-model="auditForm.approved">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="auditForm.approved" label="通过后如何分配">
            <el-radio-group v-model="auditForm.dispatchMode" class="audit-alloc">
              <el-radio value="later">审核通过，进入待派单</el-radio>
              <el-radio value="now">审核通过并立即派单</el-radio>
            </el-radio-group>
            <p class="audit-alloc__hint">校园后勤以管理员统一派单为主；系统提供智能推荐，最终由管理员确认。</p>
          </el-form-item>
          <el-form-item
            v-if="auditForm.approved && auditForm.dispatchMode === 'now'"
            label="选择维修工"
            required
          >
            <el-select
              v-model="auditForm.assignRepairmanId"
              filterable
              clearable
              placeholder="请搜索并选择启用的维修工账号"
              style="width: 100%;"
            >
              <el-option
                v-for="u in repairmanOptions"
                :key="String(u.userId)"
                :label="formatStaffLabel(u)"
                :value="u.userId != null ? String(u.userId) : ''"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input v-model="auditForm.remark" type="textarea" placeholder="内部备注/给学生说明等（选填）" :rows="3" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="admin-panel__primary-btn" :loading="auditSubmitting" @click="handleAudit">提交审核</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 维修工接单：已派单 -->
      <div
        v-if="userRole === 1 && order.status === 4"
        class="detail-block detail-block--staff"
      >
        <div class="detail-block--staff__head">
          <div class="detail-block--staff__icon detail-block--staff__icon--start">
            <el-icon :size="22"><VideoPlay /></el-icon>
          </div>
          <div>
            <h4 class="detail-block--staff__title">开始现场维修</h4>
            <p class="detail-block--staff__tip">已分配给你，请到场后点击开始</p>
          </div>
        </div>
        <el-button class="detail-block--staff__action" type="primary" size="large" @click="handleAccept">开始维修</el-button>
      </div>

      <!-- 维修工完成维修操作 -->
      <div v-if="userRole === 1 && order.status === 5" class="detail-block detail-block--staff">
        <div class="detail-block--staff__head">
          <div class="detail-block--staff__icon detail-block--staff__icon--done">
            <el-icon :size="22"><CircleCheck /></el-icon>
          </div>
          <div>
            <h4 class="detail-block--staff__title">完成本次维修</h4>
            <p class="detail-block--staff__tip">处理完毕并留言说明后，请点完成以通知学生</p>
          </div>
        </div>
        <el-button class="detail-block--staff__action" type="primary" size="large" @click="handleComplete">完成维修</el-button>
      </div>

      <!-- 学生确认维修完成操作 -->
      <div v-if="userRole === 0 && order.status === 6" class="detail-block detail-block--student-action">
        <div class="student-action__head">
          <h4 class="detail-block__title">维修确认</h4>
          <p class="student-action__hint">维修工已提交完工，确认后即可进行服务评价。</p>
        </div>
        <el-button type="primary" class="student-action__btn" @click="handleConfirm">确认维修完成</el-button>
      </div>

      <!-- 评价：学生已确认(7)后可评价，提交后后端置为 8 -->
      <div v-if="showEvaluationForm" class="detail-block detail-block--student-action">
        <h4 class="detail-block__title">评价服务</h4>
        <p class="student-action__hint">请根据本次维修体验进行评分，帮助我们持续改进服务质量。</p>
        <el-form ref="evaluationFormRef" :model="evaluationModel" :rules="evaluationRules" label-position="top" @submit.prevent="submitEvaluation">
          <el-form-item label="评分">
            <div class="star-rating">
              <el-rate v-model="evaluationModel.score" :max="5" show-text />
            </div>
          </el-form-item>
          <el-form-item label="评论">
            <el-input v-model="evaluationModel.comment" type="textarea" placeholder="请输入您的评价（选填）" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="evaluationModel.isAnonymous">匿名评价</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="student-action__btn" :loading="submitting" @click="submitEvaluation">提交评价</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 已评价显示 -->
      <div v-if="showEvaluationDetail" class="detail-block">
        <h4 class="detail-block__title">评价详情</h4>
        <div class="evaluation-detail">
          <div class="evaluation-score">
            <el-rate :model-value="Number(evaluation.score) || 0" :max="5" disabled show-text />
          </div>
          <div v-if="evaluation.comment" class="evaluation-comment">
            <p>{{ evaluation.comment }}</p>
          </div>
          <div class="evaluation-info">
            <span>{{ evaluation.isAnonymous ? '匿名' : '实名' }}评价</span>
            <span>{{ evaluation.createTime }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture, VideoPlay, CircleCheck } from '@element-plus/icons-vue'
import { getRepairDetail, submitEvaluation as apiSubmitEvaluation, getEvaluation, auditOrder, dispatchOrder, acceptOrder, completeOrder, confirmOrder, getDispatchRecommendations } from '@/api/repair'
import { getAdminUserList } from '@/api/adminUser'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userRole = computed(() =>
  userStore.userInfo && userStore.userInfo.role != null ? Number(userStore.userInfo.role) : 0
)

const orderId = computed(() => route.params.id)
const loading = ref(true)
const submitting = ref(false)

const order = ref({
  id: '',
  location: '',
  desc: '',
  status: 0,
  statusText: '待处理',
  image: '',
  date: '',
  worker: '待分配',
  repairmanId: null,
  isUrgent: false
})

const evaluation = ref(null)
const evaluationFormRef = ref(null)

const evaluationModel = ref({
  score: 5,
  comment: '',
  isAnonymous: false
})

const showEvaluationDetail = computed(() => {
  const ev = evaluation.value
  return !!(ev && ev.score != null && Number(ev.score) > 0)
})

const showEvaluationForm = computed(() =>
  userRole.value === 0 && Number(order.value.status) === 7 && !showEvaluationDetail.value
)

const evaluationRules = {
  score: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ]
}

function resetEvaluationModel() {
  evaluationModel.value = {
    score: 5,
    comment: '',
    isAnonymous: false
  }
  evaluationFormRef.value?.clearValidate?.()
}

// 管理员审核（通过时：待派单 / 直接派单）
const auditForm = ref({
  approved: true,
  remark: '',
  dispatchMode: 'later',
  assignRepairmanId: null
})
const auditSubmitting = ref(false)
const repairmanOptions = ref([])
const recommendations = ref([])
const recommendLoading = ref(false)

function formatStaffLabel(u) {
  if (!u) {
    return ''
  }
  const n = (u.nickname || u.realName || u.username || '用户') + ` (#${u.userId})`
  return n
}

async function loadRepairmanList() {
  if (userRole.value !== 2) {
    return
  }
  try {
    const p = await getAdminUserList({ page: 1, size: 200, role: 1, status: 1 })
    repairmanOptions.value = p?.records || []
  } catch {
    repairmanOptions.value = []
  }
}

// 管理员派单相关
const dispatchForm = ref({
  repairmanId: null
})

async function loadRecommendations() {
  if (userRole.value !== 2 || !orderId.value) {
    return
  }
  recommendLoading.value = true
  try {
    const data = await getDispatchRecommendations(orderId.value)
    recommendations.value = Array.isArray(data) ? data : []
  } catch {
    recommendations.value = []
  } finally {
    recommendLoading.value = false
  }
}

function useRecommendation(repairmanId) {
  const val = repairmanId != null ? String(repairmanId) : null
  if (!val) return
  dispatchForm.value.repairmanId = val
  auditForm.value.assignRepairmanId = val
  auditForm.value.dispatchMode = 'now'
}

const STATUS_TEXT = {
  0: '待审核',
  1: '审核中',
  2: '已审核',
  3: '待派单',
  4: '已派单',
  5: '维修中',
  6: '维修完成',
  7: '学生确认',
  8: '已评价',
  9: '已拒绝',
  10: '已取消'
}

/** 将详情页时间字符串/数组用于排序，仅内部使用 */
function timeToSortKey(t) {
  if (t == null || t === '') {
    return null
  }
  if (Array.isArray(t) && t.length >= 3) {
    const [y, m, d, h = 0, mm = 0, s = 0, ns] = t
    const n = ns != null ? Math.floor(ns / 1e6) : 0
    return +new Date(y, m - 1, d, h, mm, s, n)
  }
  if (typeof t === 'string') {
    const s0 = t.trim()
    if (!s0) {
      return null
    }
    const p = s0.length <= 10 ? s0 : s0.replace(' ', 'T')
    const d = new Date(p.length === 10 ? p + 'T00:00:00' : p)
    return isNaN(d.getTime()) ? null : d.getTime()
  }
  return null
}

const timelineItems = computed(() => {
  const s = order.value.status
  const o = order.value
  const out = []
  let seq = 0
  const push = (timeStr, content, type) => {
    if (!timeStr) {
      return
    }
    const k = timeToSortKey(timeStr) ?? 0
    out.push({ _k: k, _seq: seq++, time: timeStr, content, type })
  }

  const tSubmit = o.createTime || o.date
  if (tSubmit) {
    push(tSubmit, '提交报修', 'primary')
  }
  if (s === 9) {
    if (o.auditTime) {
      push(o.auditTime, '审核不通过/已拒绝', 'danger')
    }
    return sortOut(out)
  }
  if (s === 10) {
    if (o.updateTime) {
      push(o.updateTime, '工单已取消', 'warning')
    }
    return sortOut(out)
  }

  if (o.auditTime && s >= 1) {
    let am = '审核/处理'
    if (s === 1) {
      am = '已提交，进入审核/处理中'
    } else if (s === 4) {
      am = '审核通过，已派单给维修工'
    } else if (s === 2 || s === 3) {
      am = o.isUrgent && s === 3 ? '审核通过（待派单/急修）' : '审核通过'
    } else {
      am = '审核通过'
    }
    push(o.auditTime, am, 'success')
  }

  if (o.dispatchTime && s >= 4) {
    if (s === 4) {
      push(o.dispatchTime, '已派单给维修工', 'info')
    } else if (s >= 5) {
      push(
        o.dispatchTime,
        '维修人员已分配',
        'info'
      )
    }
  }

  if (o.startTime && s >= 5) {
    push(o.startTime, '维修中', 'primary')
  }
  if (o.completedTime && s >= 6) {
    push(o.completedTime, '维修工已完工', 'success')
  }
  if (o.confirmTime && s >= 7) {
    push(o.confirmTime, '学生已确认', 'success')
  }
  if (s >= 8) {
    const tEval = formatEvalTime(evaluation.value)
    if (tEval) {
      push(tEval, '已评价', 'success')
    }
  }

  return sortOut(out)
})

function formatEvalTime(ev) {
  if (!ev) {
    return null
  }
  if (ev.createTime) {
    if (Array.isArray(ev.createTime) && ev.createTime.length >= 3) {
      const [y, m, d, h = 0, mm = 0, s = 0] = ev.createTime
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    }
    if (typeof ev.createTime === 'string') {
      return ev.createTime.slice(0, 19).replace('T', ' ')
    }
  }
  return null
}

function sortOut(out) {
  if (!out.length) {
    return out
  }
  out.sort((a, b) => {
    if (a._k !== b._k) {
      return a._k - b._k
    }
    return a._seq - b._seq
  })
  return out.map(({ time, content, type }) => ({ time, content, type }))
}

// 加载工单详情
const loadDetail = async () => {
  const id = orderId.value
  if (!id) return
  loading.value = true
  evaluation.value = null
  try {
    const data = await getRepairDetail(id)
    if (data && (data.id != null || data.orderId != null)) {
      const sc = data.statusCode != null ? data.statusCode : 0
      const wName = data.repairmanName
      const wId = data.repairmanId
      const workerLine =
        wName && String(wName).trim()
          ? String(wName).trim()
          : wId
            ? `维修工 #${wId}`
            : '待分配'
      order.value = {
        id: data.id ?? data.orderId,
        location: data.location || '',
        desc: data.description || data.title || '',
        status: sc,
        statusText: STATUS_TEXT[sc] || '待处理',
        image: data.image || '',
        date: data.date || '',
        worker: workerLine,
        repairmanId: wId ?? null,
        isUrgent: !!data.isUrgent,
        auditTime: data.auditTime,
        dispatchTime: data.dispatchTime,
        startTime: data.startTime,
        createTime: data.createTime,
        updateTime: data.updateTime,
        completedTime: data.completedTime,
        confirmTime: data.confirmTime
      }
      if (sc === 7 || sc === 8) {
        await loadEvaluation(id)
      }
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 加载评价详情
const loadEvaluation = async (id) => {
  try {
    const data = await getEvaluation(id)
    if (data && data.score != null) {
      evaluation.value = data
    } else {
      evaluation.value = null
    }
  } catch (e) {
    console.error('加载评价失败:', e)
    evaluation.value = null
  }
}

onMounted(() => {
  loadDetail()
  loadRepairmanList()
  loadRecommendations()
})

watch(orderId, () => {
  resetEvaluationModel()
  loadDetail()
  loadRecommendations()
})

async function submitEvaluation() {
  const id = orderId.value
  if (!id) return
  const formIns = evaluationFormRef.value
  if (formIns?.validate) {
    try {
      await formIns.validate()
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    await apiSubmitEvaluation(id, {
      score: evaluationModel.value.score,
      comment: evaluationModel.value.comment,
      isAnonymous: evaluationModel.value.isAnonymous ? 1 : 0
    })
    ElMessage.success('评价成功')
    // 重新加载评价详情
    await loadEvaluation(id)
    // 更新工单状态为已评价
    order.value.status = 8
    order.value.statusText = STATUS_TEXT[8]
    resetEvaluationModel()
    await loadDetail()
  } catch (e) {
    ElMessage.error(e?.message || '评价失败')
  } finally {
    submitting.value = false
  }
}

function statusTagType(s) {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'warning',
    5: 'primary',
    6: 'success',
    7: 'success',
    8: 'success',
    9: 'danger',
    10: 'danger',
    11: 'warning',
    12: 'info'
  }
  return map[s] || 'info'
}

// 管理员审核工单
const handleAudit = async () => {
  const id = orderId.value
  if (!id) {
    return
  }
  if (auditForm.value.approved && auditForm.value.dispatchMode === 'now') {
    if (auditForm.value.assignRepairmanId == null || auditForm.value.assignRepairmanId === '') {
      ElMessage.warning('已选择「立即派单」，请从下拉框中选择维修工')
      return
    }
  }
  auditSubmitting.value = true
  try {
    const body = {
      approved: auditForm.value.approved,
      remark: auditForm.value.remark
    }
    if (body.approved && auditForm.value.dispatchMode === 'now') {
      body.assignRepairmanId = auditForm.value.assignRepairmanId
    }
    await auditOrder(id, body)
    if (body.approved) {
      ElMessage.success(
        body.assignRepairmanId
          ? '已审核通过并派单给指定维修工'
          : '已审核通过，工单已进入待派单'
      )
    } else {
      ElMessage.success('已拒绝该工单')
    }
    await loadDetail()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    auditSubmitting.value = false
  }
}

// 管理员派单
const handleDispatch = async () => {
  const id = orderId.value
  if (!id) return
  if (!dispatchForm.value.repairmanId) {
    ElMessage.warning('请选择维修工')
    return
  }
  
  try {
    await dispatchOrder(id, dispatchForm.value.repairmanId)
    ElMessage.success('派单成功')
    await loadDetail()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// 维修工开始维修（已派单）
const handleAccept = async () => {
  const id = orderId.value
  if (!id) return
  
  try {
    await acceptOrder(id)
    ElMessage.success('接单成功')
    await loadDetail()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// 维修工完成维修
const handleComplete = async () => {
  const id = orderId.value
  if (!id) return
  
  try {
    await completeOrder(id)
    ElMessage.success('完成维修')
    await loadDetail()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// 学生确认维修完成
const handleConfirm = async () => {
  const id = orderId.value
  if (!id) return
  
  try {
    await confirmOrder(id)
    ElMessage.success('确认成功')
    await loadDetail()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function goChat() {
  const id = order.value.id ?? orderId.value
  if (!id) {
    ElMessage.warning('无法打开沟通：缺少工单 ID')
    return
  }
  router.push({ name: 'OrderChat', params: { orderId: String(id) } })
}
</script>

<style scoped>
.order-detail-page {
  min-height: 400px;
}
.order-detail-page--staff .detail-block {
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.06);
}
/* 维修工操作卡 */
.detail-block--staff {
  background: linear-gradient(165deg, #f0fdfa 0%, #fff 45%) !important;
  border: 1px solid rgba(13, 148, 136, 0.2) !important;
}
.detail-block--staff__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}
.detail-block--staff__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.detail-block--staff__icon--grab {
  background: linear-gradient(135deg, #ea580c, #f97316);
  box-shadow: 0 4px 14px rgba(234, 88, 12, 0.35);
}
.detail-block--staff__icon--start {
  background: linear-gradient(135deg, #0d9488, #14b8a6);
  box-shadow: 0 4px 14px rgba(13, 148, 136, 0.35);
}
.detail-block--staff__icon--done {
  background: linear-gradient(135deg, #059669, #10b981);
  box-shadow: 0 4px 14px rgba(5, 150, 105, 0.3);
}
.detail-block--staff__title {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}
.detail-block--staff__title::before {
  display: none;
}
.detail-block--staff__tip {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #64748b;
}
.detail-block--staff__action {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #0d9488, #0f766e) !important;
  border: none !important;
  box-shadow: 0 6px 16px rgba(13, 148, 136, 0.35) !important;
}
.detail-block--staff__action:hover {
  filter: brightness(1.04);
  box-shadow: 0 8px 20px rgba(13, 148, 136, 0.4) !important;
}

.detail-content {
  padding: 0;
}

/* 图片展示区域 */
.detail-image {
  width: 100%;
  max-height: 320px;
  border-radius: 20px;
  overflow: hidden;
  background: white;
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.detail-image__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-image__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: var(--el-text-color-placeholder);
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
}

.detail-image__placeholder .el-icon {
  font-size: 60px;
  margin-bottom: 12px;
  color: var(--el-color-primary-light-4);
}

/* 信息块样式 */
.detail-block {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s ease;
}

.detail-block:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.detail-block__title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
}

.detail-block__title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, var(--el-color-primary), var(--el-color-primary-light-4));
  border-radius: 2px;
}

/* 信息行样式 */
.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background 0.3s ease;
}

.detail-row:hover {
  background: rgba(102, 126, 234, 0.02);
  margin: 0 -12px;
  padding: 12px;
  border-radius: 8px;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.detail-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.detail-desc {
  margin: 0;
  font-size: 15px;
  color: var(--el-text-color-regular);
  line-height: 1.7;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
}

/* 联系师傅按钮 */
.detail-chat-btn {
  width: 100%;
  max-width: 320px;
  margin-top: 20px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.detail-chat-btn--staff {
  background: linear-gradient(135deg, #0d9488, #0f766e) !important;
  border: none !important;
}

/* 评价相关样式 */
.star-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.evaluation-detail {
  padding: 10px 0 0;
}

.evaluation-score {
  margin-bottom: 14px;
  padding: 16px;
  background: linear-gradient(135deg, #fffbeb, #fff7ed);
  border: 1px solid rgba(249, 115, 22, 0.14);
  border-radius: 12px;
}

.evaluation-comment {
  margin-bottom: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f0fdf4, #ecfdf5);
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
}

.evaluation-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.detail-block--student-action {
  border: 1px solid rgba(59, 130, 246, 0.16);
  background: linear-gradient(165deg, #eff6ff 0%, #ffffff 52%);
}
.student-action__head {
  margin-bottom: 10px;
}
.student-action__hint {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
.student-action__btn {
  min-width: 130px;
  height: 42px;
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #6366f1) !important;
  border: none !important;
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.25) !important;
}
.student-action__btn:hover {
  filter: brightness(1.04);
}

.detail-block--audit :deep(.el-radio) {
  display: flex;
  margin-right: 0;
  margin-bottom: 6px;
  height: auto;
  white-space: normal;
  align-items: flex-start;
}
.admin-panel__hint {
  margin: 2px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.55;
}
.admin-panel__divider {
  margin: 14px 0 18px;
}
.recommend-panel {
  margin-top: 12px;
  padding: 14px;
  border: 1px solid rgba(59, 130, 246, 0.16);
  border-radius: 12px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
}
.recommend-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-weight: 700;
  color: #1e3a8a;
}
.recommend-list {
  display: grid;
  gap: 10px;
}
.recommend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
}
.recommend-item__main {
  min-width: 0;
}
.recommend-item__main strong {
  display: block;
  color: #0f172a;
}
.recommend-item__main span,
.recommend-item__main p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.admin-panel__primary-btn {
  min-width: 120px;
  border-radius: 10px;
  font-weight: 600;
}
.audit-alloc {
  display: block;
  width: 100%;
}
.audit-alloc__hint {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

/* 时间轴样式优化 */
:deep(.el-timeline-item__timestamp) {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

:deep(.el-timeline-item__content) {
  font-size: 15px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

:deep(.el-timeline-item__node) {
  width: 16px;
  height: 16px;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 10px 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.25);
}

:deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.25);
}

:deep(.el-button) {
  border-radius: 10px;
  font-weight: 600;
  padding: 10px 20px;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--el-color-primary), #7c66f0);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

@media (min-width: 960px) {
  .detail-content {
    display: grid;
    grid-template-columns: 340px minmax(0, 1fr);
    gap: 24px;
    align-items: start;
  }

  .detail-image {
    position: sticky;
    top: 24px;
    max-height: none;
    min-height: 360px;
    margin-bottom: 0;
    border: 1px solid rgba(148, 163, 184, 0.18);
  }

  .detail-image__placeholder {
    min-height: 360px;
  }

  .detail-block {
    margin-bottom: 18px;
    border: 1px solid rgba(148, 163, 184, 0.14);
    border-radius: 18px;
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
  }

  .detail-block:not(:has(+ .detail-block)) {
    margin-bottom: 0;
  }
}
</style>
