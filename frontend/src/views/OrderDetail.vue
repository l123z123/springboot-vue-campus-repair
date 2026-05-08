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
        <template #header><span class="card-title">基本信息</span></template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="报修地点">{{ order.location }}</el-descriptions-item>
          <el-descriptions-item label="故障分类">{{ order.category || '其他' }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag v-if="order.isUrgent || order.urgency === 'high'" type="danger" size="small">紧急</el-tag>
            <el-tag v-else-if="order.urgency === 'medium'" type="warning" size="small">中等</el-tag>
            <span v-else>普通</span>
          </el-descriptions-item>
          <el-descriptions-item label="报修人">{{ order.reporterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ order.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修工">{{ order.worker }}</el-descriptions-item>
        </el-descriptions>
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
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="提交" :description="order.createTime || ''" />
          <el-step title="审核" :description="order.auditTime || ''" />
          <el-step title="派单" :description="order.dispatchTime || ''" />
          <el-step title="维修" :description="order.completedTime || ''" />
          <el-step title="确认" :description="order.confirmTime || ''" />
          <el-step title="评价" :description="evaluation?.createTime || ''" />
        </el-steps>
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
      <el-card v-if="userRole === 2 && (order.status === 0 || order.status === 1 || order.status === 3)" shadow="never" class="section-card action-card">
        <template #header><span class="card-title">管理员处理</span></template>
        <p class="admin-hint">{{ order.status === 3 ? '当前待派单，请选择维修工' : '请审核该工单' }}</p>

        <div v-if="order.status === 0 || order.status === 1 || order.status === 3" class="recommend-box">
          <div class="recommend-box__head">
            <span>智能推荐</span>
            <el-button link type="primary" size="small" :loading="recommendLoading" @click="loadRecommendations">刷新</el-button>
          </div>
          <div v-if="recommendations.length" class="recommend-list">
            <div v-for="r in recommendations.slice(0, 3)" :key="r.repairmanId" class="recommend-item" @click="useRecommendation(r.repairmanId)">
              <div>
                <strong>{{ r.realName || r.username }}</strong>
                <span> · 在办{{ r.activeCount }}单 · 评分{{ r.score }}</span>
              </div>
              <el-button type="primary" plain size="small">采用</el-button>
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

      <!-- 维修工操作 -->
      <el-card v-if="userRole === 1 && order.status === 4" shadow="never" class="section-card action-card">
        <div class="action-row">
          <div><h4>开始维修</h4><p>确认到场后请点击</p></div>
          <el-button type="success" size="large" @click="handleAccept">开始维修</el-button>
        </div>
      </el-card>
      <el-card v-if="userRole === 1 && order.status === 5" shadow="never" class="section-card action-card">
        <div class="action-row">
          <div><h4>完成维修</h4><p>维修完成后通知学生确认</p></div>
          <el-button type="success" size="large" @click="handleComplete">完成维修</el-button>
        </div>
      </el-card>

      <!-- 学生操作 -->
      <el-card v-if="userRole === 0 && order.status === 6" shadow="never" class="section-card action-card">
        <div class="action-row">
          <div><h4>确认维修完成</h4><p>确认无误后可评价</p></div>
          <el-button type="primary" size="large" @click="handleConfirm">确认完成</el-button>
        </div>
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
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getRepairDetail, submitEvaluation as apiSubmitEvaluation, getEvaluation,
  auditOrder, dispatchOrder, acceptOrder, completeOrder, confirmOrder, getDispatchRecommendations } from '@/api/repair'
import { getAdminUserList } from '@/api/adminUser'
import { useUserStore } from '@/stores/user'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'OrderDetail' })

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()

const userRole = computed(() => userStore.userInfo?.role != null ? Number(userStore.userInfo.role) : 0)
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
  return 5
})

async function loadDetail() {
  const id = orderId.value; if (!id) return
  loading.value = true; evaluation.value = null
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
        repairmanId: data.repairmanId, isUrgent: !!data.isUrgent,
        category: data.category || '', reporterName: data.reporterName || '',
        phone: data.phone || data.phoneNumber || '',
        createTime: data.createTime || '', auditTime: data.auditTime || '',
        dispatchTime: data.dispatchTime || '', completedTime: data.completedTime || '',
        confirmTime: data.confirmTime || ''
      }
      if (sc === 7 || sc === 8) await loadEvaluation(id)
    }
  } catch (e) { ElMessage.error(e?.message || '加载失败') }
  finally { loading.value = false }
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

.recommend-box { padding:14px; border:1px solid var(--el-border-color-lighter); border-radius:8px; background:var(--el-fill-color-lighter); margin-bottom:4px; }
.recommend-box__head { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; font-weight:600; }
.recommend-list { display:grid; gap:8px; }
.recommend-item { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:10px 12px; background:#fff; border:1px solid var(--el-border-color-lighter); border-radius:8px; cursor:pointer; }

.eval-comment { margin:12px 0; padding:12px; background:var(--el-fill-color-lighter); border-radius:8px; font-size:14px; line-height:1.7; }
.eval-meta { display:flex; justify-content:space-between; align-items:center; padding-top:10px; border-top:1px solid var(--el-border-color-lighter); margin-top:8px; }
</style>
