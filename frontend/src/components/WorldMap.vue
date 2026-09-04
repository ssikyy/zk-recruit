<template>
  <!--
    全球布局的 P0 占位：内置静态 SVG，不引入地图库与 WebGL（§7.2.4）。
    节点闪烁属于 P1 视觉增强，且在 prefers-reduced-motion 下自动关闭。
  -->
  <div class="world-map">
    <svg viewBox="0 0 720 420" role="img" aria-label="全球布局示意图">
      <defs>
        <radialGradient id="glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#22d3ee" stop-opacity="0.9" />
          <stop offset="100%" stop-color="#22d3ee" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="ring" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#1c48d8" />
          <stop offset="100%" stop-color="#7b5cff" />
        </linearGradient>
      </defs>

      <circle cx="360" cy="210" r="168" fill="none" stroke="url(#ring)" stroke-opacity="0.35" />
      <circle cx="360" cy="210" r="120" fill="none" stroke="url(#ring)" stroke-opacity="0.22" />
      <circle cx="360" cy="210" r="66" fill="none" stroke="url(#ring)" stroke-opacity="0.16" />

      <ellipse cx="360" cy="210" rx="168" ry="58" fill="none" stroke="url(#ring)" stroke-opacity="0.2" />
      <ellipse cx="360" cy="210" rx="168" ry="112" fill="none" stroke="url(#ring)" stroke-opacity="0.14" />
      <line x1="192" y1="210" x2="528" y2="210" stroke="url(#ring)" stroke-opacity="0.18" />
      <line x1="360" y1="42" x2="360" y2="378" stroke="url(#ring)" stroke-opacity="0.12" />

      <g class="links" stroke="#1c48d8" stroke-opacity="0.35" stroke-dasharray="4 6">
        <path d="M250 150 Q 320 90 430 130" fill="none" />
        <path d="M430 130 Q 520 180 470 280" fill="none" />
        <path d="M250 150 Q 220 250 300 300" fill="none" />
        <path d="M300 300 Q 390 340 470 280" fill="none" />
      </g>

      <g class="nodes">
        <g v-for="(node, index) in nodes" :key="index" :transform="`translate(${node.x} ${node.y})`">
          <circle r="22" fill="url(#glow)" class="pulse" :style="{ animationDelay: index * 0.5 + 's' }" />
          <circle r="5" fill="#fff" stroke="#1c48d8" stroke-width="2" />
          <text x="12" y="4" class="node-label">{{ node.label }}</text>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup>
const nodes = [
  { x: 250, y: 150, label: '欧洲' },
  { x: 430, y: 130, label: '亚太' },
  { x: 470, y: 280, label: '中国总部' },
  { x: 300, y: 300, label: '美洲' }
]
</script>

<style scoped>
.world-map {
  width: 100%;
  border-radius: var(--zk-radius);
  background: linear-gradient(135deg, rgba(28, 72, 216, 0.06), rgba(123, 92, 255, 0.1));
  padding: 12px;
}

svg {
  width: 100%;
  height: auto;
  display: block;
}

.node-label {
  font-size: 12px;
  fill: var(--zk-primary-deep);
  font-weight: 600;
}

.pulse {
  animation: pulse 2.6s ease-in-out infinite;
  transform-origin: center;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 0.9;
    transform: scale(1.15);
  }
}

@media (prefers-reduced-motion: reduce) {
  .pulse {
    animation: none;
  }
}

/* 移动端关闭装饰动画，降低渲染压力（§16.4） */
@media (max-width: 768px) {
  .pulse {
    animation: none;
  }
}
</style>
