<template>
  <div class="empty-state">
    <slot name="icon">
      <el-icon v-if="displayIcon" :size="iconSize" class="empty-state__icon" :class="iconClass">
        <component :is="displayIcon" />
      </el-icon>
    </slot>
    <p class="empty-state__text">{{ displayText }}</p>
    <p v-if="displayHint" class="empty-state__hint">{{ displayHint }}</p>
    <el-button
      v-if="buttonText && buttonTo"
      type="primary"
      class="empty-state__btn"
      @click="onClick"
    >
      {{ buttonText }}
    </el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpened, Clock, Connection } from '@element-plus/icons-vue'

defineOptions({ name: 'EmptyState' })

const props = defineProps({
  icon: { type: [String, Object], default: '' },
  text: { type: String, default: '' },
  type: { type: String, default: '' },
  hint: { type: String, default: '' },
  buttonText: { type: String, default: '' },
  buttonTo: { type: String, default: '' },
  iconSize: { type: Number, default: 48 }
})

const router = useRouter()

const typePresets = {
  'no-data':     { icon: FolderOpened, text: '暂无数据', hint: '' },
  'coming-soon': { icon: Clock, text: '功能开发中', hint: '该功能正在建设中，敬请期待' },
  'no-network':  { icon: Connection, text: '网络连接失败', hint: '请检查网络后重试' }
}

const preset = computed(() => props.type ? (typePresets[props.type] || null) : null)

const displayIcon = computed(() => props.icon || preset.value?.icon || FolderOpened)
const displayText = computed(() => props.text || preset.value?.text || '暂无数据')
const displayHint = computed(() => props.hint || preset.value?.hint || '')

const iconClass = computed(() => {
  if (props.type === 'coming-soon') return 'empty-state__icon--soon'
  if (props.type === 'no-network') return 'empty-state__icon--error'
  return ''
})

function onClick() {
  if (props.buttonTo) router.push(props.buttonTo)
}
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 40px 20px;
}
.empty-state__icon {
  color: var(--el-text-color-placeholder);
}
.empty-state__icon--soon {
  color: var(--el-color-warning);
}
.empty-state__icon--error {
  color: var(--el-color-danger);
}
.empty-state__text {
  color: #999;
  margin: 12px 0 6px;
  font-size: 14px;
}
.empty-state__hint {
  color: #bbb;
  font-size: 12px;
  margin: 0 0 16px;
}
.empty-state__btn {
  min-height: 44px;
  padding: 10px 24px;
  border-radius: 22px;
}
.empty-state__btn:active {
  opacity: 0.9;
}
</style>
