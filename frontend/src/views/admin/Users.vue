<template>
  <div class="admin-users">
    <div class="user-filter-bar">
      <div class="filter-item filter-item--keyword">
        <el-input
          v-model="filters.keyword"
          placeholder="用户名 / 姓名"
          clearable
          @keyup.enter="fetchList"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="filter-item">
        <el-select
          v-model="filters.role"
          placeholder="角色"
          clearable
          @change="fetchList"
        >
          <el-option label="全部" value="" />
          <el-option label="管理员" :value="2" />
          <el-option label="维修工" :value="1" />
          <el-option label="学生" :value="0" />
        </el-select>
      </div>
      <div class="filter-item">
        <el-select
          v-model="filters.status"
          placeholder="状态"
          clearable
          @change="fetchList"
        >
          <el-option label="全部" value="" />
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div class="filter-item filter-item--actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增学生/维修工
        </el-button>
        <el-button type="primary" @click="fetchList">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <span class="filter-hint">支持按用户名、姓名搜索</span>
      </div>
    </div>

    <el-table :data="list" stripe v-loading="loading" :row-class-name="userRowClassName">
      <template #empty>
        <el-empty description="暂无用户数据" />
      </template>
      <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
      <el-table-column prop="realName" label="姓名" width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ row.realName || row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column prop="department" label="部门/学院" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.department || '-' }}</template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="90">
        <template #default="{ row }">
          <el-tag
            size="small"
            class="role-tag"
            :class="`role-tag--${row.role}`"
          >
            {{ roleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            :disabled="row.role === 2"
            @change="() => handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册日期" width="100">
        <template #default="{ row }">{{ shortDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" align="left">
        <template #default="{ row }">
          <div class="user-actions">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.role !== 2"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增学生/维修工'"
      width="480px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择" style="width: 100%;">
            <el-option label="维修工（由管理员创建）" :value="1" />
            <el-option label="学生" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item v-else-if="form.isTargetAdmin" label="角色">
          <span>管理员</span>
          <span class="form-muted">（仅可改资料，不可改角色）</span>
        </el-form-item>
        <el-form-item v-else label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择" style="width: 100%;">
            <el-option label="学生" :value="0" />
            <el-option label="维修工" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  getAdminUserList,
  createAdminUser,
  updateAdminUser,
  updateAdminUserStatus,
  deleteAdminUser
} from '@/api/adminUser'

defineOptions({ name: 'AdminUsers' })

const loading = ref(false)
const list = ref([])
const total = ref(0)
const filters = reactive({ keyword: '', role: '', status: '' })
const pagination = reactive({ page: 1, size: 10 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  userId: null,
  username: '',
  password: '',
  realName: '',
  role: 1,
  department: '',
  phone: '',
  isTargetAdmin: false
})

const PHONE_REG = /^1[3-9]\d{9}$/
const formRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: isEdit.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [
    {
      validator: (_r, v, cb) => {
        if (isEdit.value && form.isTargetAdmin) return cb()
        if (v === 0 || v === 1) return cb()
        return cb(new Error('请选择角色'))
      },
      trigger: 'change'
    }
  ],
  department: [{ required: true, message: '请输入部门/学院', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (!PHONE_REG.test(v)) return cb(new Error('请输入有效的手机号'))
        cb()
      },
      trigger: ['blur', 'change']
    }
  ]
}))

function roleLabel(r) {
  const map = { 0: '学生', 1: '维修工', 2: '管理员' }
  return map[r] ?? '-'
}

function shortDate(v) {
  if (!v) return '-'
  if (typeof v === 'string') return v.slice(0, 10)
  if (Array.isArray(v) && v.length >= 3) {
    const [y, m, d] = v
    const p = (n) => String(n).padStart(2, '0')
    return `${y}-${p(m)}-${p(d)}`
  }
  return '-'
}

function userRowClassName() {
  return 'user-table-row'
}

function resetFilters() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  pagination.page = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const role = filters.role === '' || filters.role == null ? undefined : Number(filters.role)
    const status = filters.status === '' || filters.status == null ? undefined : Number(filters.status)
    const data = await getAdminUserList({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword?.trim() || undefined,
      role,
      status
    })
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  pagination.page = p
  fetchList()
}

function onSizeChange(s) {
  pagination.size = s
  pagination.page = 1
  fetchList()
}

async function handleStatusChange(row) {
  const v = row.status
  try {
    await updateAdminUserStatus(row.userId, v)
    ElMessage.success(v === 1 ? '已启用' : '已禁用')
  } catch {
    row.status = v === 1 ? 0 : 1
  }
}

function openAddDialog() {
  isEdit.value = false
  resetForm()
  form.password = ''
  form.role = 1
  form.isTargetAdmin = false
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.userId = row.userId
  form.username = row.username
  form.realName = row.realName || row.nickname || ''
  form.role = row.role
  form.department = row.department || ''
  form.phone = row.phone || ''
  form.password = ''
  form.isTargetAdmin = row.role === 2
  dialogVisible.value = true
}

function resetForm() {
  form.userId = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.role = 1
  form.department = ''
  form.phone = ''
  form.isTargetAdmin = false
  formRef.value?.clearValidate()
}

function onSubmit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        const body = {
          realName: form.realName,
          department: form.department,
          phone: form.phone
        }
        if (!form.isTargetAdmin) {
          body.role = form.role
        }
        await updateAdminUser(form.userId, body)
        ElMessage.success('保存成功')
      } else {
        await createAdminUser({
          username: form.username,
          password: form.password,
          realName: form.realName,
          role: form.role,
          department: form.department,
          phone: form.phone
        })
        ElMessage.success('创建成功（维修工/学生可立即使用账号登录，若已配置密码策略）')
        pagination.page = 1
      }
      dialogVisible.value = false
      await fetchList()
    } catch (e) {
      /* 全局已提示 */
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」？将做逻辑删除。`, '删除确认', {
      type: 'warning'
    })
    await deleteAdminUser(row.userId)
    ElMessage.success('已删除')
    await fetchList()
  } catch (_) {}
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.admin-users {
  min-height: 400px;
}


.form-muted {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: 6px;
}

.user-filter-bar {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px 16px;
  align-items: center;
}

.filter-item :deep(.el-input),
.filter-item :deep(.el-select) {
  width: 100%;
}

.filter-item--keyword {
  min-width: 200px;
}

.filter-item--actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.role-tag,
.role-tag--0,
.role-tag--1,
.role-tag--2 {
  border-radius: 999px;
  border: none;
  padding: 0 10px;
  font-size: 12px;
}

.role-tag--0 {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.role-tag--1 {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.role-tag--2 {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.user-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

:deep(.user-table-row:hover) {
  background-color: var(--el-table-row-hover-bg-color, #f5f7fa) !important;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
