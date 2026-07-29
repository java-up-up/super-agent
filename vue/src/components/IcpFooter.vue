<template>
  <footer class="icp-footer" aria-label="网站备案信息">
    <a class="icp-link" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
      京ICP备2024068560号-7
    </a>
  </footer>
</template>

<style scoped>
.icp-footer {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--icp-footer-color, var(--muted-foreground));
  font-size: var(--icp-footer-font-size, 12px);
  line-height: 1.5;
}

.icp-link {
  color: inherit;
  text-decoration: none;
}

.icp-link:hover {
  color: var(--primary);
  text-decoration: underline;
}
</style>
