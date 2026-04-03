<template>
  <div class="error-page">
    <div class="error-visual animate-fade-in-up">
      <!-- CSS 插画：迷失的宇航员 -->
      <div class="astronaut">
        <div class="helmet">
          <div class="visor"></div>
        </div>
        <div class="body"></div>
        <div class="arm arm--left"></div>
        <div class="arm arm--right"></div>
      </div>
      <div class="stars">
        <div v-for="i in 6" :key="i" class="star" :class="`star--${i}`"></div>
      </div>
    </div>
    <h1 class="error-code animate-fade-in-up stagger-2">404</h1>
    <p class="error-message animate-fade-in-up stagger-3">Page lost in space</p>
    <p class="error-desc animate-fade-in-up stagger-4">抱歉，您访问的页面不存在或已被移除</p>
    <el-button type="primary" size="large" class="error-btn animate-fade-in-up stagger-5" @click="$router.push('/')">
      返回首页
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: $content-bg;
  padding: 24px;
}

// ===== CSS 宇航员插画 =====
.error-visual {
  position: relative;
  width: 160px;
  height: 160px;
  margin-bottom: 24px;
}

.astronaut {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 4s ease-in-out infinite;
}

.helmet {
  width: 56px;
  height: 56px;
  background: #E2E8F0;
  border-radius: 50%;
  position: relative;
  margin: 0 auto;
  box-shadow: inset -4px -4px 0 #CBD5E1;
}

.visor {
  width: 36px;
  height: 24px;
  background: linear-gradient(135deg, #6366F1, #818CF8);
  border-radius: 50%;
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.4);
}

.body {
  width: 44px;
  height: 36px;
  background: #E2E8F0;
  border-radius: 8px 8px 12px 12px;
  margin: -4px auto 0;
  box-shadow: inset -4px -4px 0 #CBD5E1;
}

.arm {
  width: 14px;
  height: 28px;
  background: #E2E8F0;
  border-radius: 7px;
  position: absolute;
  top: 58px;
  box-shadow: inset -2px -2px 0 #CBD5E1;

  &--left {
    left: 24px;
    transform: rotate(20deg);
  }
  &--right {
    right: 24px;
    transform: rotate(-20deg);
  }
}

// ===== 星星 =====
.stars {
  position: absolute;
  inset: 0;
}

.star {
  position: absolute;
  width: 4px;
  height: 4px;
  background: $primary-color;
  border-radius: 50%;
  opacity: 0.6;
  animation: pulse-ring 2s ease-in-out infinite;

  &--1 { top: 10%; left: 15%; animation-delay: 0s; }
  &--2 { top: 20%; right: 20%; animation-delay: 0.5s; width: 3px; height: 3px; }
  &--3 { bottom: 30%; left: 10%; animation-delay: 1s; }
  &--4 { bottom: 15%; right: 15%; animation-delay: 1.5s; width: 5px; height: 5px; }
  &--5 { top: 40%; left: 5%; animation-delay: 0.8s; width: 3px; height: 3px; }
  &--6 { top: 5%; right: 30%; animation-delay: 1.2s; }
}

// ===== 文字 =====
.error-code {
  font-family: $font-family-heading;
  font-size: 72px;
  font-weight: 800;
  background: linear-gradient(135deg, $primary-color, #8B5CF6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  line-height: 1;
  letter-spacing: -0.04em;
}

.error-message {
  font-family: $font-family-heading;
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 8px 0 4px;
}

.error-desc {
  font-size: $font-size-base;
  color: $text-secondary;
  margin: 0 0 28px;
}

.error-btn {
  font-family: $font-family-heading;
  font-weight: 600;
  border-radius: $border-radius-base;
  padding: 12px 32px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  border: none;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
  transition: all $transition-base;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
  }
}
</style>
