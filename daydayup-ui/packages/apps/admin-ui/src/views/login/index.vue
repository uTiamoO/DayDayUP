<template>
  <div class="login-wrapper">
    <!-- 装饰元素：漂浮的多彩 Mesh 渐变球 -->
    <div class="blob-container">
      <div class="blob blob-blue"></div>
      <div class="blob blob-purple"></div>
      <div class="blob blob-cyan"></div>
    </div>
    
    <!-- 登录极客磨砂玻璃卡片 -->
    <div class="glass-card login-box">
      <div class="login-header">
        <h2 class="title">DayDayUP</h2>
        <p class="subtitle">Log in to your workspace</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username" class="form-item">
          <el-input
            v-model="loginForm.username"
            placeholder="Username"
            size="large"
            :prefix-icon="User"
            class="apple-input"
          />
        </el-form-item>
        
        <el-form-item prop="password" class="form-item">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="Password"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
            class="apple-input"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="authStore.loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ authStore.loading ? 'Authenticating...' : 'Sign In' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { User, Lock } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

// 路由与身份验证状态初始化
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 表单引用实例
const formRef = ref<FormInstance>();

// 登录数据绑定
const loginForm = reactive({
  username: '',
  password: '',
});

// 表单校验规则
const rules: FormRules = {
  username: [
    { required: true, message: 'Please enter your username', trigger: 'blur' },
    { min: 4, max: 16, message: 'Must be 4-16 characters', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please enter your password', trigger: 'blur' },
    { min: 6, message: 'Must be at least 6 characters', trigger: 'blur' },
  ],
};

/**
 * 触发用户登录请求
 * 执行表单前端验证，验证通过后调用仓库登录方法并完成跳转
 */
async function handleLogin() {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const success = await authStore.login(loginForm.username, loginForm.password);
      if (success) {
        const redirect = (route.query.redirect as string) || '/admin/dashboard';
        router.push(redirect);
      }
    }
  });
}
</script>

<style scoped>
/* 登录主背景：应用微小透明度 SVG 沙粒噪点与浅灰蓝渐变的双重背景，呈现印刷实体质感 */
.login-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--apple-bg);
  position: relative;
  overflow: hidden;
  background-image: 
    url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.018'/%3E%3C/svg%3E"),
    linear-gradient(135deg, var(--apple-login-gradient-start) 0%, var(--apple-login-gradient-end) 100%);
}

/* ==========================================================================
   超大尺寸 Mesh 渐变彩色球动画
   ========================================================================== */
.blob-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

/* 渐变气泡基类：提升不透明度，使颜色饱满且互相折射 */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(110px);
  opacity: 0.65;
  mix-blend-mode: multiply;
  pointer-events: none;
}

:global(html.dark) .blob {
  mix-blend-mode: screen;
  opacity: 0.45;
}

/* 魅惑蓝渐变球 */
.blob-blue {
  top: 8%;
  left: 12%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(0, 122, 255, 0.45) 0%, rgba(0, 122, 255, 0) 70%);
  animation: float-slow 28s infinite alternate ease-in-out;
}

/* 幻紫渐变球 */
.blob-purple {
  bottom: 8%;
  right: 12%;
  width: 650px;
  height: 650px;
  background: radial-gradient(circle, rgba(162, 89, 255, 0.38) 0%, rgba(162, 89, 255, 0) 70%);
  animation: float-medium 24s infinite alternate-reverse ease-in-out;
}

/* 湖青渐变球 */
.blob-cyan {
  top: 38%;
  left: 38%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(0, 210, 255, 0.32) 0%, rgba(0, 210, 255, 0) 70%);
  animation: float-fast 20s infinite alternate ease-in-out;
}

/* 慢速漂浮轨迹定义 */
@keyframes float-slow {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(90px, -70px) scale(1.1);
  }
  100% {
    transform: translate(-50px, 50px) scale(0.9);
  }
}

@keyframes float-medium {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-80px, 90px) scale(0.95);
  }
  100% {
    transform: translate(60px, -60px) scale(1.05);
  }
}

@keyframes float-fast {
  0% {
    transform: translate(-40px, -40px) scale(1);
  }
  50% {
    transform: translate(50px, -70px) scale(1.15);
  }
  100% {
    transform: translate(-60px, 30px) scale(0.85);
  }
}

/* ==========================================================================
   极致磨砂卡片与表单高光拟物
   ========================================================================== */
.login-box {
  width: 420px;
  padding: 54px 44px;
  border-radius: 28px;
  background: var(--apple-login-card-bg) !important; /* 降低白色配比，折射更多的底层动态光影 */
  backdrop-filter: blur(40px) !important; /* 高阶模糊 */
  -webkit-backdrop-filter: blur(40px) !important;
  border: 1px solid var(--apple-login-card-border) !important;
  /* 双层软阴影，融合微弱的蓝色环境发光 */
  box-shadow: var(--apple-login-card-shadow) !important;
  z-index: 2; /* 浮于渐变球之上 */
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

/* 品牌文字：高清晰黑蓝渐变，更具视觉质感 */
.title {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -1.2px;
  background: linear-gradient(135deg, var(--apple-login-title-start) 30%, var(--apple-login-title-end) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 10px 0;
}

.subtitle {
  font-size: 15px;
  color: var(--apple-text-secondary);
  margin: 0;
}

.form-item {
  margin-bottom: 24px;
}

/* 输入框：透明底加毛玻璃，柔和融入卡片 */
:deep(.apple-input .el-input__wrapper) {
  border-radius: 14px;
  padding: 5px 18px;
  box-shadow: 0 0 0 1px var(--apple-login-input-border) inset !important;
  background-color: var(--apple-login-input-bg) !important;
  backdrop-filter: blur(10px);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

:deep(.apple-input .el-input__wrapper.is-focus) {
  background-color: var(--apple-login-input-focus-bg) !important;
  box-shadow: 0 0 0 2px var(--apple-login-input-focus-ring) inset, 0 8px 20px rgba(0, 122, 255, 0.08) !important;
}

/* 登录按钮：金属渐变，加入内嵌高光线条，展现苹果拟物化玻璃按钮质感 */
.login-button {
  width: 100%;
  margin-top: 16px;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 25px;
  border: none !important;
  background: linear-gradient(180deg, #3594ff 0%, #007aff 100%) !important;
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.25),
    0 4px 15px rgba(0, 122, 255, 0.2) !important;
  letter-spacing: 0.5px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  color: #ffffff !important;
}

.login-button:hover {
  background: linear-gradient(180deg, #4f9fff 0%, #006ee6 100%) !important;
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.35),
    0 6px 20px rgba(0, 122, 255, 0.3) !important;
  transform: translateY(-1.5px);
}

.login-button:active {
  background: #0062cc !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.2) !important;
  transform: translateY(0);
}
</style>
