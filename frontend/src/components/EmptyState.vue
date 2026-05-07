<template>
  <div class="empty-state">
    <slot name="icon">
      <el-icon v-if="icon" :size="iconSize" class="empty-state__icon">
        <component :is="icon" />
      </el-icon>
    </slot>
    <p class="empty-state__text">{{ text }}</p>
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
import { useRouter } from 'vue-router'

defineOptions({ name: 'EmptyState' })

const props = defineProps({
  /** 图标组件名（Element Plus 图标） */
  icon: { type: [String, Object], default: '' },
  /** 说明文案 */
  text: { type: String, default: '暂无数据' },
  /** 按钮文案 */
  buttonText: { type: String, default: '' },
  /** 按钮跳转路径 */
  buttonTo: { type: String, default: '' },
  iconSize: { type: Number, default: 48 }
})

const router = useRouter()

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
.empty-state__text {
  color: #999;
  margin: 12px 0 16px;
  font-size: 14px;
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
