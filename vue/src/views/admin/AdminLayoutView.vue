<template>
  <section class="admin-shell">
    <aside class="admin-sidebar" :class="{ 'admin-sidebar-open': sidebarOpen }">
      <div class="sidebar-brand">
        <div class="brand-mark">SA</div>
        <strong class="brand-name">Super Agent</strong>
      </div>

      <nav class="sidebar-nav">
        <span class="nav-group-label">主要功能</span>
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: isNavItemActive(item.to) }"
          @click="sidebarOpen = false"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-user-card">
          <div class="sidebar-user-info">
            <div class="sidebar-avatar">{{ username.slice(0, 1).toUpperCase() }}</div>
            <div>
              <strong>{{ username }}</strong>
              <span class="user-role">管理员</span>
            </div>
          </div>
          <button class="logout-btn" type="button" title="退出登录" @click="logout">
            <ArrowLeftOnRectangleIcon class="nav-icon" />
          </button>
        </div>
        <IcpFooter class="admin-icp" />
      </div>
    </aside>

    <div v-if="sidebarOpen" class="sidebar-mask" @click="sidebarOpen = false"></div>

    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <button class="menu-button mobile-only" type="button" @click="sidebarOpen = true">
            <Bars3Icon class="nav-icon" />
          </button>
          <h2 class="page-title">{{ pageTitle }}</h2>
          <nav class="breadcrumb">
            <RouterLink to="/admin/dashboard">首页</RouterLink>
            <span class="breadcrumb-sep">/</span>
            <span>{{ pageTitle }}</span>
          </nav>
        </div>

        <div class="header-actions">
          <RouterLink to="/chat" class="back-link">返回聊天</RouterLink>
          <span class="header-divider"></span>
          <div class="header-user">
            <span>{{ username }}</span>
            <div class="header-avatar">{{ username.slice(0, 1).toUpperCase() }}</div>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  ArrowLeftOnRectangleIcon,
  Bars3Icon,
  ClipboardDocumentListIcon,
  CommandLineIcon,
  HomeModernIcon,
  ShareIcon,
  EyeIcon
} from '@heroicons/vue/24/outline'
import IcpFooter from '../../components/IcpFooter.vue'
import { adminAuthApi } from '../../api/api'
import { clearAdminAuth, getAdminUsername } from '../../utils/adminAuth'

const route = useRoute()
const router = useRouter()
const sidebarOpen = ref(false)

const navItems = [
  { to: '/admin/dashboard', label: '运营总览', icon: HomeModernIcon },
  { to: '/admin/documents', label: '文档接入', icon: ClipboardDocumentListIcon },
  { to: '/admin/knowledge-route', label: '知识路由', icon: ShareIcon },
  { to: '/admin/knowledge-route/traces', label: '路由追踪', icon: EyeIcon },
  { to: '/admin/observability', label: '对话观测', icon: CommandLineIcon }
]

const pageTitle = computed(() => route.meta?.title || '管理后台')
const username = computed(() => getAdminUsername())

async function logout() {
  try {
    await adminAuthApi.logout()
  } catch {
    // token 失效或网络异常时，前端仍然要允许本地退出。
  } finally {
    clearAdminAuth()
    router.replace('/admin/login')
  }
}

function isNavItemActive(targetPath) {
  if (!targetPath) {
    return false
  }
  if (targetPath === '/admin/knowledge-route') {
    return route.path === targetPath
  }
  return route.path === targetPath || route.path.startsWith(`${targetPath}/`)
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
}

/* ── Sidebar: 白底 + 右侧border ── */
.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0;
  background: var(--card);
  border-right: 1px solid var(--admin-border);
  overflow-y: auto;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 0 12px;
  border-bottom: 1px solid var(--border);
}

.brand-mark {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  font-weight: 700;
  font-size: 11px;
  letter-spacing: 0.02em;
}

.brand-name {
  font-size: 15px;
  color: var(--color-text-strong);
}

/* ── 导航 ── */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 8px;
  flex: 1;
}

.nav-group-label {
  font-size: 11px;
  color: var(--color-muted);
  padding: 8px 8px 4px;
  font-weight: 500;
}

.nav-item {
  text-decoration: none;
  display: flex;
  align-items: center;
  min-height: 36px;
  gap: 10px;
  padding: 0 10px;
  border-radius: var(--radius-md);
  color: var(--muted-foreground);
  font-size: 14px;
  transition: background 0.15s ease, color 0.15s ease;
  position: relative;
}

.nav-item:hover {
  color: var(--foreground);
  background: var(--muted);
}

.nav-item.active {
  color: var(--foreground);
  background: var(--brand-50);
  font-weight: 600;
}

.nav-icon {
  width: 18px;
  height: 18px;
  flex: none;
}

/* ── Sidebar 底部用户 ── */
.sidebar-footer {
  padding: 8px;
  border-top: 1px solid var(--border);
}

.admin-icp {
  margin-top: 10px;
  --icp-footer-font-size: 11px;
}

.sidebar-user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px;
  border-radius: var(--radius-md);
  background: var(--muted);
}

.sidebar-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.sidebar-avatar {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
  font-size: 12px;
  flex: none;
}

.sidebar-user-info strong {
  display: block;
  font-size: 13px;
  color: var(--color-text);
  font-weight: 500;
}

.user-role {
  display: block;
  font-size: 11px;
  color: var(--color-muted);
  margin-top: 1px;
}

.logout-btn {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-muted);
  flex: none;
}

.logout-btn:hover {
  color: var(--color-danger);
  background: rgba(177, 47, 38, 0.08);
}

/* ── 主区域 ── */
.admin-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--admin-bg);
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 4;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 0 20px;
  height: 56px;
  background: color-mix(in srgb, var(--card) 95%, transparent);
  border-bottom: 1px solid var(--admin-border);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-strong);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted);
}

.breadcrumb a {
  color: var(--color-muted);
  text-decoration: none;
}

.breadcrumb a:hover {
  color: var(--color-primary);
}

.breadcrumb-sep {
  color: var(--color-border-strong);
}

.breadcrumb span:last-child {
  color: var(--color-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-link {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--color-border);
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text);
}

.header-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  color: var(--color-text);
  font-weight: 600;
  font-size: 12px;
}

.admin-content {
  width: min(1600px, 100%);
  padding: 24px 20px 32px;
  margin: 0 auto;
}

.menu-button {
  border: 1px solid var(--border);
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  color: var(--color-text);
  background: var(--card);
}

.mobile-only,
.sidebar-mask {
  display: none;
}

@media (max-width: 1040px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    width: 240px;
    z-index: 10;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
    box-shadow: var(--shadow-md);
  }

  .admin-sidebar.admin-sidebar-open {
    transform: translateX(0);
  }

  .sidebar-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 9;
  }

  .mobile-only {
    display: inline-flex;
  }

  .page-title {
    display: none;
  }

  .admin-content {
    padding: 16px;
  }
}

@media (min-width: 1041px) {
  .breadcrumb {
    display: none;
  }
}
</style>
