<template>
  <div v-if="password" class="password-strength">
    <div class="strength-bars">
      <div
        v-for="i in 3"
        :key="i"
        class="strength-bar"
        :class="[i <= level ? strengthClass : 'strength-empty']"
      />
    </div>
    <span class="strength-text" :class="strengthClass">{{ strengthText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  password: string
}>()

const score = computed(() => {
  const pwd = props.password
  if (!pwd) return 0
  let s = 0
  if (pwd.length >= 8) s++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) s++
  if (/\d/.test(pwd)) s++
  if (/[^a-zA-Z0-9]/.test(pwd)) s++
  return s
})

const level = computed(() => {
  if (score.value <= 1) return 1
  if (score.value === 2) return 2
  return 3
})

const strengthClass = computed(() => {
  if (level.value === 1) return 'strength-weak'
  if (level.value === 2) return 'strength-medium'
  return 'strength-strong'
})

const strengthText = computed(() => {
  if (level.value === 1) return '弱'
  if (level.value === 2) return '中'
  return '强'
})
</script>

<style scoped lang="scss">
.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.strength-bars {
  display: flex;
  gap: 4px;
}

.strength-bar {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  transition: background-color 0.3s;
}

.strength-empty {
  background-color: #e4e7ed;
}

.strength-weak {
  background-color: #f56c6c;
}

.strength-medium {
  background-color: #e6a23c;
}

.strength-strong {
  background-color: #67c23a;
}

.strength-text {
  font-size: 12px;
  line-height: 1;
}
</style>
