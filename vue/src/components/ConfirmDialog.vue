<template>
  <AlertDialog :open="state.open" @update:open="onOpenChange">
    <AlertDialogContent class="sm:max-w-[420px]">
      <AlertDialogHeader>
        <AlertDialogTitle>{{ state.title }}</AlertDialogTitle>
        <AlertDialogDescription class="whitespace-pre-line">{{ state.message }}</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <Button variant="outline" @click="onCancel">取消</Button>
        <Button @click="onConfirm">确定</Button>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>

<script setup>
import { useConfirm } from '@/composables/useConfirm'
import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogFooter,
  AlertDialogTitle,
  AlertDialogDescription
} from '@/components/ui/alert-dialog'

const { state, onConfirm, onCancel } = useConfirm()

function onOpenChange(open) {
  if (!open) onCancel()
}
</script>
