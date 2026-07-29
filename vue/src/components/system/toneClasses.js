const STATUS_BADGE_CLASSES = Object.freeze({
  default: 'border-[var(--status-default-border)] bg-[var(--status-default-bg)] text-[var(--status-default-fg)]',
  waiting: 'border-[var(--status-waiting-border)] bg-[var(--status-waiting-bg)] text-[var(--status-waiting-fg)]',
  running: 'border-[var(--status-running-border)] bg-[var(--status-running-bg)] text-[var(--status-running-fg)]',
  success: 'border-[var(--status-success-border)] bg-[var(--status-success-bg)] text-[var(--status-success-fg)]',
  danger: 'border-[var(--status-danger-border)] bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)]'
})

const ROUTE_MODE_BADGE_CLASSES = Object.freeze({
  auto: 'border-[var(--route-mode-auto-border)] bg-[var(--route-mode-auto-bg)] text-[var(--route-mode-auto-fg)]',
  shadow: 'border-[var(--route-mode-shadow-border)] bg-[var(--route-mode-shadow-bg)] text-[var(--route-mode-shadow-fg)]',
  neutral: 'border-border bg-secondary text-foreground'
})

export function resolveStatusBadgeClass(tone) {
  return STATUS_BADGE_CLASSES[tone] || STATUS_BADGE_CLASSES.default
}

export function resolveRouteModeBadgeClass(tone) {
  return ROUTE_MODE_BADGE_CLASSES[tone] || ROUTE_MODE_BADGE_CLASSES.neutral
}
