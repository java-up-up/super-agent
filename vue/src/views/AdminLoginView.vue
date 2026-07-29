<template>
  <div class="relative grid min-h-dvh place-items-center bg-admin-bg px-4 py-16 sm:px-6">
    <main class="w-full max-w-[440px]">
      <div class="mb-5 flex items-center gap-3 px-1">
        <div class="grid size-9 place-items-center rounded-md bg-primary text-caption font-bold text-primary-foreground">NA</div>
        <div>
          <p class="m-0 text-caption text-muted-foreground">Nexus Agent</p>
          <h1 class="m-0 text-title-sm font-semibold text-foreground">管理后台</h1>
        </div>
      </div>

      <form class="rounded-lg border border-border bg-card p-5 shadow-control sm:p-6" novalidate @submit.prevent="submitLogin">
        <div class="border-b border-border pb-4">
          <h2 class="m-0 text-title font-semibold text-foreground">登录工作台</h2>
          <p class="mt-2 text-body-sm leading-relaxed text-muted-foreground">
            使用当前部署环境配置的后台账号，进入文档、知识库和对话观测页面。
          </p>
          <p v-if="redirectHint" class="mt-2 text-caption text-muted-foreground">登录后将返回：{{ redirectHint }}</p>
        </div>

        <div class="mt-5 flex flex-col gap-4">
          <div class="flex flex-col gap-2">
            <Label for="login-username">账号</Label>
            <Input
              id="login-username"
              v-model="form.username"
              type="text"
              placeholder="请输入后台账号"
              autocomplete="username"
              :aria-invalid="Boolean(errorMessage) || undefined"
              :aria-describedby="errorMessage ? 'login-error' : undefined"
            />
          </div>

          <div class="flex flex-col gap-2">
            <div class="flex items-center justify-between gap-3">
              <Label for="login-password">密码</Label>
              <Button
                variant="ghost"
                size="icon-sm"
                type="button"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                :title="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <EyeSlashIcon v-if="showPassword" aria-hidden="true" />
                <EyeIcon v-else aria-hidden="true" />
              </Button>
            </div>
            <Input
              id="login-password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入后台密码"
              autocomplete="current-password"
              :aria-invalid="Boolean(errorMessage) || undefined"
              :aria-describedby="errorMessage ? 'login-error' : undefined"
            />
          </div>
        </div>

        <p v-if="errorMessage" id="login-error" role="alert" class="mt-4 rounded-md border border-destructive/20 bg-destructive/10 px-3 py-2 text-body-sm text-destructive">
          {{ errorMessage }}
        </p>

        <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row">
          <Button class="flex-1 rounded-md" size="lg" variant="outline" type="button" :disabled="submitting" @click="goBackChat">返回会话端</Button>
          <Button class="flex-1 rounded-md" size="lg" type="submit" :loading="submitting" loading-text="登录中">进入管理台</Button>
        </div>
      </form>
    </main>

    <IcpFooter class="absolute inset-x-0 bottom-[max(1rem,env(safe-area-inset-bottom))] px-4 sm:px-6" />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import { useRoute, useRouter } from 'vue-router'
import IcpFooter from '../components/IcpFooter.vue'
import { adminAuthApi, APIError } from '../api/api'
import { saveAdminAuth } from '../utils/adminAuth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const router = useRouter()
const route = useRoute()

const form = reactive({ username: '', password: '' })
const errorMessage = ref('')
const submitting = ref(false)
const showPassword = ref(false)
const safeRedirect = computed(() => typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin')
  ? route.query.redirect
  : '/admin/dashboard')
const redirectHint = computed(() => safeRedirect.value === '/admin/dashboard' ? '' : safeRedirect.value)

async function submitLogin() {
  errorMessage.value = ''
  if (!form.username.trim() || !form.password.trim()) {
    errorMessage.value = '请输入账号和密码。'
    return
  }

  submitting.value = true
  try {
    const username = form.username.trim()
    const result = await adminAuthApi.login({ username, password: form.password })
    saveAdminAuth({ username: result?.username || username, token: result?.token || '' })
    router.replace(safeRedirect.value)
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
