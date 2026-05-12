<template>
  <div class="order-detail-page">
    <div v-if="loading" class="detail-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <div class="detail-header">
        <div class="detail-header__left">
          <span class="detail-header__id">工单 #{{ order.ticketNo || order.id }}</span>
          <el-tag :type="getStatusTagType(order.status)" size="large">{{ getRepairStatusLabel(order.status) }}</el-tag>
          <el-tag v-if="order.isUrgent" type="danger" size="large" effect="dark">紧急</el-tag>
        </div>
        <span class="detail-header__time">提交于 {{ order.createTime || order.date }}</span>
      </div>

      <el-card shadow="never" class="section-card">
        <template #header><span class="card-title">工单详情</span></template>
        <div class="info-grid">
          <div class="info-grid__item">
            <span class="info-label">报修地点</span>
            <span class="info-value">{{ order.location }}</span>
          </div>
          <div class="info-grid__item">
            <span class="info-label">故障分类</span>
            <span class="info-value">{{ order.category || '其他' }}</span>
          </div>
          <div class="info-grid__item">
            <span class="info-label">紧急程度</span>
            <el-tag v-if="order.isUrgent || order.urgency === 'high'" type="danger" size="small">紧急</el-tag>
            <el-tag v-else-if="order.urgency === 'medium'" type="warning" size="small">中等</el-tag>
            <el-tag v-else type="info" size="small">普通</el-tag>
          </div>
          <div class="info-grid__item">
            <span class="info-label">报修人</span>
            <span class="info-value">{{ order.reporterName || '-' }}</span>
          </div>
          <div class="info-grid__item">
            <span class="info-label">联系电话</span>
            <span class="info-value">{{ order.phone || '-' }}</span>
          </div>
          <div class="info-grid__item">
            <span class="info-label">维修工</span>
            <span class="info-value">{{ order.worker }}</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header><span class="card-title">故障描述</span></template>
        <p class="desc-text">{{ order.desc }}</p>
        <div v-if="order.images && order.images.length" class="image-gallery">
          <el-image v-for="(img, i) in order.images" :key="i" :src="img" fit="cover" class="gallery-thumb" :preview-src-list="order.images" :initial-index="i" />
        </div>
        <div v-else-if="order.image" class="image-gallery">
          <el-image :src="order.image" fit="cover" class="gallery-thumb" :preview-src-list="[order.image]" />
        </div>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header><span class="card-title">处理进度</span></template>
        <div class="custom-steps">
          <div
            v-for="(step, idx) in stepItems"
            :key="idx"
            class="c-step"
            :class="{ 'is-active': idx === currentStep, 'is-done': idx < currentStep, 'is-fail': isRejected && idx === 1 }"
          >
            <div class="c-step__dot">
              <el-icon v-if="idx < currentStep"><Check /></el-icon>
              <el-icon v-else-if="isRejected && idx === 1"><Close /></el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="c-step__label">{{ step.label }}</div>
            <div v-if="step.time" class="c-step__time">{{ step.time }}</div>
          </div>
          <div class="c-step__line">
            <div class="c-step__line-fill" :style="{ width: stepProgress + '%' }"></div>
          </div>
        </div>
        <div v-if="order.status === 9" style="margin-top:16px;text-align:center;">
          <el-alert type="error" title="该工单已被管理员拒绝" :closable="false" show-icon />
        </div>
        <div v-if="order.status === 10" style="margin-top:16px;text-align:center;">
          <el-alert type="warning" title="该工单已取消" :closable="false" show-icon />
        </div>
      </el-card>

      <div v-if="order.repairmanId" class="contact-bar">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            已分配维修师傅
            <el-button type="primary" size="small" style="margin-left:12px" @click="goChat">
              <el-icon><ChatDotRound /></el-icon>
              联系沟通
            </el-button>
          </template>
        </el-alert>
      </div>

      <!-- 管理员操作 -->
      <el-card v-if="userRole === 2 && !isOwnOrder && (order.status === 0 || order.status === 1 || order.status === 3)" shadow="never" class="section-card action-card">
        <template #header><span class="card-title">管理员处理</span></template>
        <p class="admin-hint">{{ order.status === 3 ? '当前待派单，请选择维修工' : '请审核该工单' }}</p>

        <div v-if="order.status === 0 || order.status === 1 || order.status === 3" class="recommend-box">
          <div class="recommend-box__head">
            <span>智能派单推荐（综合评分 + 推荐理由）</span>
            <el-button link type="primary" size="small" :loading="recommendLoading" @click="loadRecommendations">刷新</el-button>
          </div>
          <div v-if="recommendations.length" class="recommend-list">
            <div v-for="(r, idx) in recommendations.slice(0, 3)" :key="r.repairmanId" class="recommend-item" @click="useRecommendation(r.repairmanId)">
              <div class="recommend-item__rank">
                <span class="recommend-item__rank-num" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
              </div>
              <div class="recommend-item__body">
                <div class="recommend-item__top">
                  <strong class="recommend-item__name">{{ r.realName || r.username }}</strong>
                  <span class="recommend-item__dept">{{ r.department || '' }}</span>
                  <el-tag size="small" :type="r.score >= 120 ? 'success' : r.score >= 100 ? 'warning' : 'info'">{{ r.score }} 分</el-tag>
                </div>
                <div class="recommend-item__stats">
                  <span>在办 <b>{{ r.activeCount }}</b> 单</span>
                  <span v-if="r.averageScore > 0" class="stat-sep">|</span>
                  <span v-if="r.averageScore > 0">历史评分 <b>{{ r.averageScore.toFixed(1) }}</b></span>
                </div>
                <div v-if="r.reasons && r.reasons.length" class="recommend-item__reasons">
                  <span v-for="(reason, ri) in r.reasons" :key="ri" class="recommend-item__reason">
                    <el-icon :size="12"><Check /></el-icon>
                    {{ reason }}
                  </span>
                </div>
              </div>
              <el-button type="primary" size="small" @click.stop="useRecommendation(r.repairmanId)">采用</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无推荐" :image-size="40" />
        </div>

        <el-divider />

        <el-form v-if="order.status === 3" :model="dispatchForm" label-width="100px">
          <el-form-item label="选择维修工" required>
            <el-select v-model="dispatchForm.repairmanId" filterable placeholder="搜索维修工" style="width:100%">
              <el-option v-for="u in repairmanOptions" :key="u.userId" :label="u.realName || u.username" :value="String(u.userId)" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="handleDispatch">确认派单</el-button></el-form-item>
        </el-form>

        <el-form v-else :model="auditForm" label-width="100px">
          <el-form-item label="审核结果">
            <el-radio-group v-model="auditForm.approved">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <template v-if="auditForm.approved">
            <el-form-item label="分配方式">
              <el-radio-group v-model="auditForm.dispatchMode">
                <el-radio value="later">待派单</el-radio>
                <el-radio value="now">立即派单</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="auditForm.dispatchMode === 'now'" label="维修工">
              <el-select v-model="auditForm.assignRepairmanId" filterable placeholder="选择维修工" style="width:100%">
                <el-option v-for="u in repairmanOptions" :key="u.userId" :label="u.realName || u.username" :value="String(u.userId)" />
              </el-select>
            </el-form-item>
          </template>
          <el-form-item label="备注">
            <el-input v-model="auditForm.remark" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="auditSubmitting" @click="handleAudit">提交审核</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 评价 -->
      <el-card v-if="showEvaluationForm" shadow="never" class="section-card">
        <template #header><span class="card-title">服务评价</span></template>
        <el-form ref="evaluationFormRef" :model="evaluationModel" :rules="evaluationRules" label-width="80px">
          <el-form-item label="评分" prop="score">
            <el-rate v-model="evaluationModel.score" :max="5" show-text />
          </el-form-item>
          <el-form-item label="评论">
            <el-input v-model="evaluationModel.comment" type="textarea" :rows="3" placeholder="分享您的体验（选填）" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="evaluationModel.isAnonymous">匿名评价</el-checkbox>
            <el-button type="primary" :loading="submitting" style="margin-left:24px" @click="submitEvaluation">提交评价</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="showEvaluationDetail" shadow="never" class="section-card">
        <template #header><span class="card-title">评价详情</span></template>
        <el-rate :model-value="Number(evaluation.score) || 0" :max="5" disabled show-text />
        <p v-if="evaluation.comment" class="eval-comment">{{ evaluation.comment }}</p>
        <div class="eval-meta">
          <el-tag size="small" :type="evaluation.isAnonymous ? 'info' : 'success'">{{ evaluation.isAnonymous ? '匿名' : '实名' }}</el-tag>
          <span style="font-size:12px;color:var(--el-text-color-placeholder)">{{ evaluation.createTime }}</span>
        </div>
      </el-card>

      <transition name="slide-up">
        <div v-if="showActionBar" class="action-bar">
          <div class="action-bar__inner">
            <div class="action-bar__info">
              <span>{{ actionBarText }}</span>
            </div>
            <div class="action-bar__btns">
              <el-button
                v-if="userRole === 1 && order.status === 4"
                type="success"
                size="large"
                @click="handleAcceptConfirm"
              >开始维修</el-button>
              <el-button
                v-if="userRole === 1 && order.status === 5"
                type="success"
                size="large"
                @click="handleCompleteConfirm"
              >完成维修</el-button>
              <el-button
                v-if="userRole === 0 && order.status === 6"
                type="primary"
                size="large"
                @click="handleConfirm"
              >确认完成</el-button>
            </div>
          </div>
        </div>
      </transition>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Check, Close } from '@element-plus/icons-vue'
import { getRepairDetail, submitEvaluation as apiSubmitEvaluation, getEvaluation,
  auditOrder, dispatchOrder, acceptOrder, completeOrder, confirmOrder, getDispatchRecommendations } from '@/api/repair'
import { getAdminUserList } from '@/api/adminUser'
import { useUserStore } from '@/stores/user'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'OrderDetail' })

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()

const userRole = computed(() => userStore.userInfo?.role != null ? Number(userStore.userInfo.role) : 0)
const currentUserId = computed(() => String(userStore.userInfo?.userId ?? ''))
const isOwnOrder = computed(() => currentUserId.value && String(order.value.userId) === currentUserId.value)
const orderId = computed(() => route.params.id)
const loading = ref(true); const submitting = ref(false)

const order = ref({
  id: '', ticketNo: '', location: '', desc: '', status: 0, statusText: '',
  image: '', images: [], date: '', worker: '待分配', repairmanId: null,
  isUrgent: false, category: '', reporterName: '', phone: '',
  createTime: '', auditTime: '', dispatchTime: '', completedTime: '', confirmTime: ''
})

const evaluation = ref(null)
const evaluationFormRef = ref(null)
const evaluationModel = ref({ score: 5, comment: '', isAnonymous: false })
const showEvaluationDetail = computed(() => !!(evaluation.value?.score != null && Number(evaluation.value.score) > 0))
const showEvaluationForm = computed(() => userRole.value === 0 && Number(order.value.status) === 7 && !showEvaluationDetail.value)
const evaluationRules = { score: [{ required: true, message: '请选择评分', trigger: 'change' }] }

const isRejected = computed(() => order.value.status === 9)
const stepItems = computed(() => [
  { label: '提交', time: order.value.createTime },
  { label: '审核', time: order.value.auditTime },
  { label: '派单', time: order.value.dispatchTime },
  { label: '维修', time: order.value.completedTime },
  { label: '确认', time: order.value.confirmTime },
  { label: '评价', time: evaluation.value?.createTime || '' }
])
const stepProgress = computed(() => {
  if (order.value.status >= 8) return 100
  if (order.value.status >= 6) return 83
  if (order.value.status >= 4) return 66
  if (order.value.status >= 3) return 50
  if (order.value.status >= 1) return 33
  return 16
})
const showActionBar = computed(() => {
  return (userRole.value === 1 && (order.value.status === 4 || order.value.status === 5))
    || (userRole.value === 0 && order.value.status === 6)
})
const actionBarText = computed(() => {
  if (userRole.value === 1 && order.value.status === 4) return '确认到场后请点击开始维修'
  if (userRole.value === 1 && order.value.status === 5) return '维修完成后请点击完成维修'
  if (userRole.value === 0 && order.value.status === 6) return '维修已完成，请确认'
  return ''
})

const auditForm = ref({ approved: true, remark: '', dispatchMode: 'later', assignRepairmanId: null })
const auditSubmitting = ref(false)
const dispatchForm = ref({ repairmanId: null })
const repairmanOptions = ref([])
const recommendations = ref([])
const recommendLoading = ref(false)

function currentStatusLabel() { return getRepairStatusLabel(order.value.status) }

const currentStep = computed(() => {
  const s = order.value.status
  if (s === 9 || s === 10) return 0
  if (s <= 0) return 0
  if (s <= 2) return 1
  if (s <= 3) return 2
  if (s <= 5) return 3
  if (s <= 6) return 4
  if (s <= 7) return 5
  return 6
})

async function loadDetail() {
  const id = orderId.value; if (!id) return
  loading.value = true
  recommendations.value = []
  evaluation.value = null
  try {
    const data = await getRepairDetail(id)
    if (data?.id != null || data?.orderId != null) {
      const sc = data.statusCode != null ? data.statusCode : 0
      order.value = {
        id: data.id || data.orderId, ticketNo: data.ticketNo || '',
        location: data.location || '', desc: data.description || '',
        status: sc,
        image: data.image || '', images: data.images || [],
        date: data.date || '', worker: data.repairmanName || '待分配',
        repairmanId: data.repairmanId, isUrgent: !!data.isUrgent, urgency: data.urgency,
        category: data.category || '', reporterName: data.reporterName || '',
        phone: data.phone || data.phoneNumber || '',
        createTime: data.createTime || '', auditTime: data.auditTime || '',
        dispatchTime: data.dispatchTime || '', completedTime: data.completedTime || '',
        confirmTime: data.confirmTime || ''
      }
      if (sc === 7 || sc === 8) await loadEvaluation(id)
    }
  } catch (e) { ElMessage.error(e?.message || '加载失败') }
  finally {
    loading.value = false
    auditForm.value = { approved: true, remark: '', dispatchMode: 'later', assignRepairmanId: null }
  }
}

async function loadEvaluation(id) {
  try { const data = await getEvaluation(id); evaluation.value = (data?.score != null) ? data : null }
  catch { evaluation.value = null }
}

async function loadRepairmanList() {
  if (userRole.value !== 2) return
  try { const p = await getAdminUserList({ page:1, size:200, role:1, status:1 }); repairmanOptions.value = p?.records || [] }
  catch { repairmanOptions.value = [] }
}

async function loadRecommendations() {
  if (userRole.value !== 2 || !orderId.value) return
  recommendLoading.value = true
  try { const data = await getDispatchRecommendations(orderId.value); recommendations.value = Array.isArray(data) ? data : [] }
  catch { recommendations.value = [] }
  finally { recommendLoading.value = false }
}

function useRecommendation(id) { dispatchForm.value.repairmanId = String(id); auditForm.value.assignRepairmanId = String(id); auditForm.value.dispatchMode = 'now' }

async function handleAudit() {
  if (auditForm.value.approved && auditForm.value.dispatchMode === 'now' && !auditForm.value.assignRepairmanId) { ElMessage.warning('请选择维修工'); return }
  auditSubmitting.value = true
  try {
    const body = { approved: auditForm.value.approved, remark: auditForm.value.remark }
    if (body.approved && auditForm.value.dispatchMode === 'now') body.assignRepairmanId = auditForm.value.assignRepairmanId
    await auditOrder(orderId.value, body)
    ElMessage.success(body.approved ? (body.assignRepairmanId ? '审核通过并派单' : '审核通过，进入待派单') : '已拒绝')
    await loadDetail()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
  finally { auditSubmitting.value = false }
}

async function handleDispatch() {
  if (!dispatchForm.value.repairmanId) { ElMessage.warning('请选择维修工'); return }
  try { await dispatchOrder(orderId.value, dispatchForm.value.repairmanId); ElMessage.success('派单成功'); await loadDetail() }
  catch (e) { ElMessage.error(e?.message || '操作失败') }
}

async function handleAcceptConfirm() {
  try {
    await ElMessageBox.confirm('确认开始维修此工单？', '确认操作', { type: 'info' })
  } catch { return }
  await handleAccept()
}

async function handleCompleteConfirm() {
  try {
    await ElMessageBox.confirm('确认该工单已维修完成？完成后将通知学生进行确认。', '确认操作', { type: 'info' })
  } catch { return }
  await handleComplete()
}

async function handleAccept() { try { await acceptOrder(orderId.value); ElMessage.success('已开始维修'); await loadDetail() } catch (e) { ElMessage.error(e?.message || '操作失败') } }
async function handleComplete() { try { await completeOrder(orderId.value); ElMessage.success('维修完成'); await loadDetail() } catch (e) { ElMessage.error(e?.message || '操作失败') } }
async function handleConfirm() { try { await confirmOrder(orderId.value); ElMessage.success('已确认'); await loadDetail() } catch (e) { ElMessage.error(e?.message || '操作失败') } }

async function submitEvaluation() {
  if (evaluationFormRef.value?.validate) { try { await evaluationFormRef.value.validate() } catch { return } }
  submitting.value = true
  try {
    await apiSubmitEvaluation(orderId.value, { score:evaluationModel.value.score, comment:evaluationModel.value.comment, isAnonymous:evaluationModel.value.isAnonymous?1:0 })
    ElMessage.success('评价成功'); await loadDetail()
  } catch (e) { ElMessage.error(e?.message || '评价失败') }
  finally { submitting.value = false }
}

function goChat() { router.push({ name:'OrderChat', params:{ orderId:String(order.value.id) } }) }

onMounted(() => { loadDetail(); loadRepairmanList(); loadRecommendations() })
watch(orderId, () => { loadDetail(); loadRecommendations() })
</script>

<style scoped>
.detail-loading { padding:20px 0; }

.detail-header {
  display:flex; justify-content:space-between; align-items:center;
  margin-bottom:20px; padding:16px 20px; background:#fff; border-radius:12px;
  box-shadow:0 2px 12px rgba(0,0,0,0.05);
}
.detail-header__left { display:flex; align-items:center; gap:12px; }
.detail-header__id { font-size:18px; font-weight:700; color:var(--el-text-color-primary); font-family:'SF Mono','Cascadia Code',monospace; }
.detail-header__time { font-size:13px; color:var(--el-text-color-placeholder); }

.section-card { margin-bottom:16px; border-radius:12px; border:1px solid var(--el-border-color-lighter); box-shadow:0 2px 12px rgba(0,0,0,0.05); }
.card-title { font-size:15px; font-weight:600; }

.desc-text { margin:0 0 12px 0; font-size:14px; color:var(--el-text-color-regular); line-height:1.7; }

.image-gallery { display:flex; flex-wrap:wrap; gap:10px; }
.gallery-thumb { width:120px; height:120px; border-radius:8px; object-fit:cover; cursor:pointer; border:1px solid var(--el-border-color-lighter); }

.contact-bar { margin-bottom:16px; }
.contact-bar :deep(.el-alert__title) { display:flex; align-items:center; }

.action-card { border-left:4px solid var(--el-color-primary); }
.action-row { display:flex; justify-content:space-between; align-items:center; gap:20px; }
.action-row h4 { margin:0 0 4px 0; font-size:15px; font-weight:600; }
.action-row p { margin:0; font-size:13px; color:var(--el-text-color-secondary); }

.admin-hint { margin:0 0 14px 0; font-size:13px; color:var(--el-text-color-secondary); }

.recommend-box { padding:16px; border:1px solid var(--el-border-color-lighter); border-radius:8px; background:var(--el-fill-color-lighter); margin-bottom:4px; }
.recommend-box__head { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; font-size:14px; font-weight:600; }
.recommend-list { display:grid; gap:10px; max-height:500px; overflow-y:auto; }
.recommend-item { display:flex; align-items:flex-start; gap:14px; padding:14px 16px; background:#fff; border:1px solid var(--el-border-color-lighter); border-radius:10px; cursor:pointer; transition:all 0.2s; }
.recommend-item:hover { border-color:var(--el-color-primary); box-shadow:0 2px 8px rgba(var(--el-color-primary-rgb),0.12); }
.recommend-item__rank { flex-shrink:0; }
.recommend-item__rank-num { display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; border-radius:50%; font-size:13px; font-weight:700; color:#fff; }
.rank-1 { background:linear-gradient(135deg,#f56c6c,#e74c3c); }
.rank-2 { background:linear-gradient(135deg,#e6a23c,#f39c12); }
.rank-3 { background:linear-gradient(135deg,#67c23a,#27ae60); }
.recommend-item__body { flex:1; min-width:0; }
.recommend-item__top { display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:6px; }
.recommend-item__name { font-size:15px; color:var(--el-text-color-primary); }
.recommend-item__dept { font-size:12px; color:var(--el-text-color-placeholder); }
.recommend-item__stats { font-size:12px; color:var(--el-text-color-secondary); margin-bottom:6px; }
.recommend-item__stats b { color:var(--el-text-color-primary); }
.stat-sep { margin:0 6px; color:var(--el-border-color); }
.recommend-item__reasons { display:flex; flex-wrap:wrap; gap:4px 8px; }
.recommend-item__reason { display:inline-flex; align-items:center; gap:3px; font-size:12px; color:var(--el-text-color-secondary); background:var(--el-fill-color); padding:2px 8px; border-radius:4px; }

.eval-comment { margin:12px 0; padding:12px; background:var(--el-fill-color-lighter); border-radius:8px; font-size:14px; line-height:1.7; }
.eval-meta { display:flex; justify-content:space-between; align-items:center; padding-top:10px; border-top:1px solid var(--el-border-color-lighter); margin-top:8px; }

/* Custom Steps */
.custom-steps {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 20px 0 32px;
}
.c-step {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.c-step__dot {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--el-fill-color);
  border: 2px solid var(--el-border-color);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600;
  color: var(--el-text-color-placeholder);
  transition: all 0.3s;
}
.c-step.is-active .c-step__dot {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 0 0 6px rgba(37,99,235,0.15);
  animation: stepPulse 2s infinite;
}
.c-step.is-done .c-step__dot {
  background: var(--el-color-success);
  border-color: var(--el-color-success);
  color: #fff;
}
.c-step.is-fail .c-step__dot {
  background: var(--el-color-danger);
  border-color: var(--el-color-danger);
  color: #fff;
}
.c-step__label {
  font-size: 12px; font-weight: 500;
  color: var(--el-text-color-regular);
}
.c-step__time {
  font-size: 11px; color: var(--el-text-color-placeholder);
  max-width: 80px; text-align: center;
}
.c-step__line {
  position: absolute; top: 36px; left: 12%; right: 12%; height: 3px;
  background: var(--el-fill-color);
  border-radius: 2px;
  z-index: 0;
}
.c-step__line-fill {
  height: 100%; border-radius: 2px;
  background: var(--el-color-success);
  transition: width 0.5s ease;
}

@keyframes stepPulse {
  0%, 100% { box-shadow: 0 0 0 6px rgba(37,99,235,0.15); }
  50% { box-shadow: 0 0 0 12px rgba(37,99,235,0.05); }
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.info-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-label {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.info-value {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

/* Action Bar */
.action-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  background: #fff;
  border-top: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 -4px 20px rgba(0,0,0,0.08);
  padding: 16px 24px;
  z-index: 100;
}
.action-bar__inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.action-bar__info { font-size: 14px; color: var(--el-text-color-secondary); }

.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.3s ease; }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); }

@media (max-width: 640px) {
  .info-grid { grid-template-columns: repeat(2, 1fr); }
  .custom-steps { flex-wrap: wrap; gap: 12px; }
  .c-step { flex: 0 0 auto; }
}
</style>
