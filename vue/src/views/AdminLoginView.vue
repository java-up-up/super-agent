<template>
  <section class="login-shell">
    <main class="login-panel">
      <div class="login-brand">
        <div class="brand-mark" aria-hidden="true">NA</div>
        <div>
          <p class="brand-kicker">Nexus Agent</p>
          <h1>管理后台</h1>
        </div>
      </div>
      <form class="login-form" @submit.prevent="submitLogin">
        <div class="form-header">
          <h2>登录工作台</h2>
          <p>使用当前部署环境配置的后台账号，进入文档、知识路由与对话观测页面。</p>
        </div>

        <label class="field">
          <span>账号</span>
          <input v-model="form.username" type="text" placeholder="请输入后台账号" autocomplete="username" />
        </label>

        <label class="field">
          <span>密码</span>
          <input v-model="form.password" type="password" placeholder="请输入后台密码" autocomplete="current-password" />
        </label>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <div class="form-actions">
          <button class="secondary-button" type="button" @click="goBackChat">返回聊天</button>
          <button class="primary-button" type="submit" :disabled="submitting">
            {{ submitting ? '登录中...' : '进入管理台' }}
          </button>
        </div>
      </form>
    </main>

    <IcpFooter class="login-icp" />
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IcpFooter from '../components/IcpFooter.vue'
import { adminAuthApi, APIError } from '../api/api'
import { saveAdminAuth } from '../utils/adminAuth'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: 'admin',
  password: 'admin123456'
})
const errorMessage = ref('')
const submitting = ref(false)

async function submitLogin() {
  errorMessage.value = ''
  if (!form.username.trim() || !form.password.trim()) {
    errorMessage.value = '请输入账号和密码。'
    return
  }

  submitting.value = true
  try {
    const result = await adminAuthApi.login({
      username: form.username.trim(),
      password: form.password
    })
    saveAdminAuth({
      username: result?.username || form.username.trim(),
      token: result?.token || ''
    })
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin')
      ? route.query.redirect
      : '/admin/dashboard'
    router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof APIError || error instanceof Error
      ? error.message
      : '登录失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

function goBackChat() {
  router.push('/chat')
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100dvh;
  padding: 64px 16px 76px;
  display: grid;
  place-items: center;
  background: var(--admin-bg);
}

.login-panel {
  width: min(440px, 100%);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 4px;
  margin-bottom: 20px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  flex: none;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: var(--primary-foreground);
  font-size: 12px;
  font-weight: 700;
}

.login-brand h1 {
  margin: 0;
  font-size: 18px;
  line-height: 1.25;
  font-weight: 700;
  color: var(--foreground);
}

.brand-kicker {
  margin: 0 0 2px;
  color: var(--muted-foreground);
  font-size: 12px;
}

.login-form {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--card);
  box-shadow: var(--shadow-control);
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.form-header p {
  margin: 8px 0 0;
  color: var(--muted-foreground);
  font-size: 13px;
  line-height: 1.65;
}

.form-header h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
  color: var(--foreground);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
}

.field span {
  font-size: 14px;
  font-weight: 600;
  color: var(--foreground);
}

.field input {
  width: 100%;
  border: 1px solid var(--input);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--card);
  color: var(--foreground);
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus {
  border-color: var(--ring);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ring) 16%, transparent);
}

.error-message {
  margin: 16px 0 0;
  color: var(--destructive);
  font-size: 14px;
  line-height: 1.5;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.primary-button,
.secondary-button {
  flex: 1;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.primary-button {
  color: var(--primary-foreground);
  background: var(--primary);
}

.primary-button:hover {
  opacity: 0.9;
}

.secondary-button {
  color: var(--foreground);
  background: var(--card);
  border-color: var(--border);
}

.secondary-button:hover {
  background: var(--secondary);
}

.login-icp {
  position: absolute;
  left: 32px;
  right: 32px;
  bottom: 22px;
}

@media (max-width: 960px) {
  .login-shell {
    padding: 32px 16px 68px;
  }

  .login-form {
    padding: 20px;
  }

  .login-icp {
    left: 18px;
    right: 18px;
    bottom: 18px;
  }
}
</style>
