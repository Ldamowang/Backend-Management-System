<template>
  <div class="error-page">
    <div class="error-visual animate-fade-in-up">
      <!-- CSS 插画：锁盾牌 -->
      <div class="shield">
        <div class="shield-body">
          <div class="lock-icon">
            <div class="lock-body"></div>
            <div class="lock-shackle"></div>
          </div>
        </div>
      </div>
      <div class="shield-rays">
        <div v-for="i in 4" :key="i" class="ray" :class="`ray--${i}`"></div>
      </div>
    </div>
    <h1 class="error-code animate-fade-in-up stagger-2">403</h1>
    <p class="error-message animate-fade-in-up stagger-3">Access Denied</p>
    <p class="error-desc animate-fade-in-up stagger-4">抱歉，您没有权限访问该页面</p>
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

// ===== CSS 盾牌插画 =====
.error-visual {
  position: relative;
  width: 160px;
  height: 160px;
  margin-bottom: 24px;
}

.shield {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 4s ease-in-out infinite;
}

.shield-body {
  width: 72px;
  height: 84px;
  background: linear-gradient(180deg, #F59E0B, #D97706);
  border-radius: 8px 8px 36px 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.3);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 4px;
    border-radius: 6px 6px 32px 32px;
    background: linear-gradient(180deg, #FBBF24, #F59E0B);
  }
}

.lock-icon {
  position: relative;
  z-index: 1;
}

.lock-body {
  width: 22px;
  height: 18px;
  background: #fff;
  border-radius: 4px;
  position: relative;

  &::after {
    content: '';
    width: 6px;
    height: 8px;
    background: #D97706;
    border-radius: 2px;
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
}

.lock-shackle {
  width: 14px;
  height: 12px;
  border: 3px solid #fff;
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  margin: 0 auto -2px;
}

// ===== 光线 =====
.shield-rays {
  position: absolute;
  inset: 0;
}

.ray {
  position: absolute;
  background: rgba(245, 158, 11, 0.15);
  border-radius: 2px;

  &--1 {
    width: 30px; height: 3px;
    top: 45%; left: 5%;
    animation: pulse-ring 2s ease-in-out infinite;
  }
  &--2 {
    width: 24px; height: 3px;
    top: 45%; right: 5%;
    animation: pulse-ring 2s ease-in-out infinite 0.5s;
  }
  &--3 {
    width: 3px; height: 24px;
    top: 5%; left: 48%;
    animation: pulse-ring 2s ease-in-out infinite 1s;
  }
  &--4 {
    width: 3px; height: 20px;
    bottom: 10%; left: 48%;
    animation: pulse-ring 2s ease-in-out infinite 1.5s;
  }
}

// ===== 文字 =====
.error-code {
  font-family: $font-family-heading;
  font-size: 72px;
  font-weight: 800;
  background: linear-gradient(135deg, #F59E0B, #D97706);
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
  background: linear-gradient(135deg, #F59E0B, #D97706);
  border: none;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
  transition: all $transition-base;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(245, 158, 11, 0.35);
  }
}
</style>
