<script setup>
import {
  ChevronDoubleLeftIcon,
  ChevronDoubleRightIcon,
  PlusIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import { Button } from '@/components/ui/button'
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerHeader,
  DrawerTitle
} from '@/components/ui/drawer'
import ConversationHistoryList from './ConversationHistoryList.vue'

defineProps({
  sessions: { type: Array, default: () => [] },
  currentConversationId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  collapsed: { type: Boolean, default: false },
  mobileOpen: { type: Boolean, default: false }
})

const emit = defineEmits([
  'select',
  'delete',
  'new',
  'retry',
  'update:collapsed',
  'update:mobileOpen'
])
</script>

<template>
  <aside
    class="hidden min-h-0 flex-none flex-col border-r border-border bg-card transition-[width] duration-200 motion-reduce:transition-none lg:flex"
    :class="collapsed ? 'w-16' : 'w-72'"
    aria-label="会话历史"
  >
    <header class="flex h-16 flex-none items-center gap-2 border-b border-border px-3">
      <strong v-if="!collapsed" class="min-w-0 flex-1 truncate text-sm font-semibold text-foreground">会话列表</strong>
      <Button
        variant="ghost"
        size="icon-lg"
        type="button"
        :aria-label="collapsed ? '展开会话历史' : '收起会话历史'"
        :title="collapsed ? '展开会话历史' : '收起会话历史'"
        @click="emit('update:collapsed', !collapsed)"
      >
        <ChevronDoubleRightIcon v-if="collapsed" />
        <ChevronDoubleLeftIcon v-else />
      </Button>
    </header>

    <div class="flex flex-none p-3" :class="collapsed ? 'justify-center' : ''">
      <Button
        :size="collapsed ? 'icon-lg' : 'lg'"
        :class="collapsed ? '' : 'w-full'"
        type="button"
        :disabled="disabled"
        :aria-label="collapsed ? '新建对话' : undefined"
        title="新建对话"
        @click="emit('new')"
      >
        <PlusIcon data-icon="inline-start" />
        <span v-if="!collapsed">新建对话</span>
      </Button>
    </div>

    <ConversationHistoryList
      v-if="!collapsed"
      :sessions="sessions"
      :current-conversation-id="currentConversationId"
      :loading="loading"
      :error="error"
      :disabled="disabled"
      @select="emit('select', $event)"
      @delete="emit('delete', $event)"
      @retry="emit('retry')"
    />
  </aside>

  <Drawer
    :open="mobileOpen"
    direction="left"
    :should-scale-background="false"
    @update:open="emit('update:mobileOpen', $event)"
  >
    <DrawerContent class="lg:hidden">
      <DrawerHeader class="flex-none border-b border-border pr-14 text-left">
        <DrawerTitle>会话历史</DrawerTitle>
        <DrawerDescription>选择已有会话，或开始一段新对话。</DrawerDescription>
        <DrawerClose as-child>
          <Button class="absolute right-3 top-3 size-11" variant="ghost" size="icon-lg" type="button" aria-label="关闭会话历史">
            <XMarkIcon />
          </Button>
        </DrawerClose>
      </DrawerHeader>

      <div class="flex flex-none p-3">
        <Button size="lg" class="h-11 w-full" type="button" :disabled="disabled" @click="emit('new')">
          <PlusIcon data-icon="inline-start" />
          新建对话
        </Button>
      </div>

      <ConversationHistoryList
        :sessions="sessions"
        :current-conversation-id="currentConversationId"
        :loading="loading"
        :error="error"
        :disabled="disabled"
        @select="emit('select', $event)"
        @delete="emit('delete', $event)"
        @retry="emit('retry')"
      />
    </DrawerContent>
  </Drawer>
</template>
