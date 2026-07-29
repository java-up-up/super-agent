<template>
  <div :class="cn('admin-shell min-h-dvh bg-admin-bg', desktopCollapsed && 'admin-shell--collapsed')">
    <div class="ambient-field" aria-hidden="true"></div>

    <a
      href="#admin-main-content"
      class="fixed left-4 top-3 z-[var(--z-tooltip)] -translate-y-16 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground opacity-0 transition focus:translate-y-0 focus:opacity-100 motion-reduce:transition-none"
    >跳到主要内容</a>

    <!--
      No glass surface of its own. In the reference the sidebar tone is within ~2 points of
      the page behind it; it reads as a column because of the hairline on its right edge,
      not because it is a lighter panel.
    -->
    <aside class="admin-sidebar hidden min-h-dvh border-r border-admin-border lg:flex">
      <div class="flex h-14 flex-none items-center gap-2 border-b border-border px-3">
        <div class="grid size-8 flex-none place-items-center rounded-md bg-primary text-caption font-bold text-primary-foreground">NA</div>
        <strong v-if="!desktopCollapsed" class="min-w-0 flex-1 truncate text-body-sm font-semibold text-foreground">Nexus Agent</strong>
        <Button
          variant="ghost"
          size="icon"
          type="button"
          :aria-label="desktopCollapsed ? '展开侧边导航' : '收起侧边导航'"
          :title="desktopCollapsed ? '展开侧边导航' : '收起侧边导航'"
          @click="desktopCollapsed = !desktopCollapsed"
        >
          <ChevronRightIcon v-if="desktopCollapsed" aria-hidden="true" />
          <ChevronLeftIcon v-else aria-hidden="true" />
        </Button>
      </div>

      <nav class="flex min-h-0 flex-1 flex-col overflow-y-auto px-2 py-3" aria-label="后台主导航">
        <section v-for="group in navGroups" :key="group.label" class="mb-4 last:mb-0">
          <h2 :class="cn('mb-1 px-2 text-caption font-medium text-muted-foreground', desktopCollapsed && 'sr-only')">{{ group.label }}</h2>
          <div class="flex flex-col gap-1">
            <RouterLink
              v-for="item in group.items"
              :key="item.to"
              :to="item.to"
              :title="desktopCollapsed ? item.label : undefined"
              :aria-current="isNavItemActive(item) ? 'page' : undefined"
              :class="cn(
                'flex min-h-9 items-center gap-2 rounded-md px-2.5 text-body-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/40',
                desktopCollapsed && 'justify-center px-0',
                isNavItemActive(item) && 'bg-selection text-foreground'
              )"
            >
              <component :is="item.icon" class="size-[18px] flex-none" aria-hidden="true" />
              <span v-if="!desktopCollapsed" class="truncate">{{ item.label }}</span>
              <span v-else class="sr-only">{{ item.label }}</span>
            </RouterLink>
          </div>
        </section>
      </nav>

      <div class="flex flex-none flex-col gap-2 border-t border-border p-2">
        <RouterLink
          to="/chat"
          :class="cn('flex min-h-9 items-center gap-2 rounded-md px-2.5 text-body-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/40', desktopCollapsed && 'justify-center px-0')"
          :title="desktopCollapsed ? '返回会话端' : undefined"
        >
          <ArrowTopRightOnSquareIcon class="size-[18px] flex-none" aria-hidden="true" />
          <span v-if="!desktopCollapsed">返回会话端</span>
          <span v-else class="sr-only">返回会话端</span>
        </RouterLink>

        <div :class="cn('flex items-center gap-2 rounded-md bg-muted p-2', desktopCollapsed && 'justify-center p-1')">
          <div class="grid size-8 flex-none place-items-center rounded-md border border-border bg-card text-caption font-semibold text-foreground" aria-hidden="true">
            {{ usernameInitial }}
          </div>
          <div v-if="!desktopCollapsed" class="min-w-0 flex-1">
            <strong class="block truncate text-caption font-semibold text-foreground">{{ username }}</strong>
            <span class="block text-micro text-muted-foreground">管理员</span>
          </div>
          <Button
            v-if="!desktopCollapsed"
            variant="ghost"
            size="icon"
            type="button"
            aria-label="退出登录"
            title="退出登录"
            @click="logout"
          >
            <ArrowLeftOnRectangleIcon aria-hidden="true" />
          </Button>
        </div>
        <Button
          v-if="desktopCollapsed"
          variant="ghost"
          size="icon"
          class="self-center"
          type="button"
          aria-label="退出登录"
          title="退出登录"
          @click="logout"
        >
          <ArrowLeftOnRectangleIcon aria-hidden="true" />
        </Button>
        <IcpFooter v-if="!desktopCollapsed" class="px-1 pt-1 [--icp-footer-font-size:var(--text-micro)]" />
      </div>
    </aside>

    <Drawer v-model:open="mobileNavOpen" direction="left">
      <DrawerContent class="h-dvh max-h-dvh rounded-none" data-testid="admin-mobile-drawer">
        <DrawerHeader class="sr-only">
          <DrawerTitle>后台导航</DrawerTitle>
          <DrawerDescription>切换管理后台页面</DrawerDescription>
        </DrawerHeader>
        <aside class="flex min-h-0 flex-1 flex-col">
          <div class="flex h-14 flex-none items-center gap-2 border-b border-border px-4">
            <div class="grid size-8 place-items-center rounded-md bg-primary text-caption font-bold text-primary-foreground">NA</div>
            <strong class="text-body-sm font-semibold text-foreground">Nexus Agent</strong>
          </div>
          <nav class="min-h-0 flex-1 overflow-y-auto px-3 py-4" aria-label="移动端后台导航">
            <section v-for="group in navGroups" :key="group.label" class="mb-5 last:mb-0">
              <h2 class="mb-1.5 px-2 text-caption font-medium text-muted-foreground">{{ group.label }}</h2>
              <div class="flex flex-col gap-1">
                <RouterLink
                  v-for="item in group.items"
                  :key="item.to"
                  :to="item.to"
                  :aria-current="isNavItemActive(item) ? 'page' : undefined"
                  :class="cn(
                    'flex min-h-11 items-center gap-3 rounded-md px-3 text-body-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/40',
                    isNavItemActive(item) && 'bg-selection text-foreground'
                  )"
                  @click="mobileNavOpen = false"
                >
                  <component :is="item.icon" class="size-5 flex-none" aria-hidden="true" />
                  <span>{{ item.label }}</span>
                </RouterLink>
              </div>
            </section>
          </nav>
          <div class="flex flex-none flex-col gap-2 border-t border-border p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
            <RouterLink to="/chat" class="flex min-h-11 items-center gap-3 rounded-md px-3 text-body-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground" @click="mobileNavOpen = false">
              <ArrowTopRightOnSquareIcon class="size-5" aria-hidden="true" />
              返回会话端
            </RouterLink>
            <Button variant="outline" size="lg" class="w-full rounded-md" type="button" @click="logout">
              <ArrowLeftOnRectangleIcon data-icon="inline-start" aria-hidden="true" />
              退出 {{ username }}
            </Button>
          </div>
        </aside>
      </DrawerContent>
    </Drawer>

    <div class="flex min-w-0 flex-col">
      <!--
        Uses the panel tier, not the shell tier, so the whole content column reads as one
        continuous surface the way the reference does. At the shell's lower alpha there was
        a visible step between this bar and the content below it.
      -->
      <header class="glass-panel sticky top-0 z-[var(--z-sticky)] flex h-14 items-center gap-3 border-b border-admin-border px-4 sm:px-5 lg:px-6">
        <Button
          variant="ghost"
          size="icon-lg"
          class="lg:hidden"
          type="button"
          aria-label="打开后台导航"
          @click="mobileNavOpen = true"
        >
          <Bars3Icon aria-hidden="true" />
        </Button>
        <div class="min-w-0">
          <p class="m-0 truncate text-caption text-muted-foreground">管理后台</p>
          <h1 class="m-0 truncate text-body-sm font-semibold text-foreground">{{ pageTitle }}</h1>
        </div>
      </header>

      <!--
        L1 content tier. This works now that the tiers are tinted rather than white-alpha:
        the depth reads from the card's dim end being darker than the panel behind it, the
        same relationship the reference uses, instead of depending on ambient bleed-through.
      -->
      <main id="admin-main-content" tabindex="-1" class="glass-panel min-w-0 flex-1 outline-none">
        <div class="mx-auto w-full max-w-[1600px] px-4 py-5 sm:px-5 lg:px-6 lg:py-6">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  ArrowLeftOnRectangleIcon,
  ArrowTopRightOnSquareIcon,
  Bars3Icon,
  ChevronLeftIcon,
  ChevronRightIcon,
  ClipboardDocumentListIcon,
  CommandLineIcon,
  EyeIcon,
  HomeModernIcon,
  ShareIcon
} from '@heroicons/vue/24/outline'
import IcpFooter from '../../components/IcpFooter.vue'
import { Button } from '@/components/ui/button'
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { cn } from '@/lib/utils'
import { adminAuthApi } from '../../api/api'
import { clearAdminAuth, getAdminUsername } from '../../utils/adminAuth'

const route = useRoute()
const router = useRouter()
const mobileNavOpen = ref(false)
const desktopCollapsed = ref(false)

const navGroups = [
  {
    label: '总览',
    items: [
      { to: '/admin/dashboard', label: '运营总览', icon: HomeModernIcon, exact: true }
    ]
  },
  {
    label: '知识资产',
    items: [{ to: '/admin/documents', label: '文档接入', icon: ClipboardDocumentListIcon }]
  },
  {
    label: '路由与观测',
    items: [
      { to: '/admin/knowledge-route', label: '知识路由', icon: ShareIcon, exact: true },
      { to: '/admin/knowledge-route/traces', label: '路由追踪', icon: EyeIcon, exact: true },
      { to: '/admin/observability', label: '对话观测', icon: CommandLineIcon }
    ]
  }
]

const pageTitle = computed(() => route.meta?.title || '管理后台')
const username = computed(() => getAdminUsername())
const usernameInitial = computed(() => username.value.slice(0, 1).toUpperCase())

async function logout() {
  try {
    await adminAuthApi.logout()
  } catch {
    // 本地退出不依赖远端 token 仍然有效。
  } finally {
    mobileNavOpen.value = false
    clearAdminAuth()
    router.replace('/admin/login')
  }
}

function isNavItemActive(item) {
  if (route.path === item.to) return true
  return !item.exact && route.path.startsWith(`${item.to}/`)
}
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  /* Contains the ambient layer's negative z-index so it cannot escape behind the page. */
  isolation: isolate;
}

/*
 * Negative z-index paints above the shell's own background but below every child,
 * so no sibling needs a stacking override. Fixed also keeps it out of grid flow.
 */
.ambient-field {
  z-index: -1;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100dvh;
  flex-direction: column;
}

/*
 * The header uses the panel tier for colour continuity with the content below it, but it
 * is also the only sticky glass surface, so it needs the blur that .glass-panel omits —
 * without it, content scrolling underneath reads straight through the translucent bar.
 */
@supports (backdrop-filter: blur(1px)) {
  header.glass-panel {
    backdrop-filter: blur(var(--glass-blur)) saturate(150%);
  }
}

/*
 * The app is full-bleed. There is no inset frame and no gutter between the sidebar and
 * the content: sampling the reference with its decorative background removed shows a
 * single colour jump from sidebar to content panel, with the page tone running edge to
 * edge behind both. The blurred orbs in the reference sit *behind* a full-bleed app —
 * they are a decorative layer, not a canvas the app floats on.
 */
@media (min-width: 64rem) {
  .admin-shell {
    grid-template-columns: 15rem minmax(0, 1fr);
    transition: grid-template-columns var(--motion-standard) var(--motion-ease);
  }

  .admin-shell--collapsed {
    grid-template-columns: 4.5rem minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .admin-shell {
    transition: none;
  }
}
</style>
