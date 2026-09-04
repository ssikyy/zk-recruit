<template>
  <footer class="site-footer">
    <div class="zk-container inner">
      <div class="col">
        <div class="brand">
          <img class="brand-logo" src="/favicon.svg" alt="熵基科技" />
          <span>· 招聘</span>
        </div>
        <p class="address">{{ footer.address }}</p>
      </div>

      <div class="col">
        <h4>公司联系方式</h4>
        <p v-if="footer.contactEmail">{{ footer.contactEmail }}</p>
        <p v-if="footer.contactPhone">公司总机：{{ footer.contactPhone }}</p>
      </div>

      <div class="col">
        <h4>条款</h4>
        <p><a :href="footer.privacyUrl" target="_blank" rel="noopener noreferrer">隐私政策</a></p>
        <p><a :href="footer.termsUrl" target="_blank" rel="noopener noreferrer">使用条款</a></p>
      </div>

      <div class="col qr">
        <h4>官方公众号</h4>
        <img
          v-if="footer.qrcodeImage && !qrcodeFailed"
          :src="footer.qrcodeImage"
          alt="公众号二维码"
          @error="qrcodeFailed = true"
        />
        <!-- 素材缺失时保持同尺寸占位，替换素材不需要改布局（§17.2） -->
        <div v-else class="zk-placeholder qr-placeholder">二维码占位</div>
      </div>
    </div>
    <div class="copyright">
      <div class="zk-container">© {{ year }} 熵基科技</div>
    </div>
  </footer>
</template>

<script setup>
import { ref } from 'vue'
import { HOME_CONTENT } from '@/config/homeContent'

const footer = HOME_CONTENT.footer
const year = new Date().getFullYear()
const qrcodeFailed = ref(false)
</script>

<style scoped>
.site-footer {
  background: var(--zk-ink);
  color: rgba(255, 255, 255, 0.72);
  padding-top: 52px;
  font-size: 13px;
  line-height: 1.9;
}

.inner {
  display: grid;
  grid-template-columns: 1.6fr 1fr 0.8fr 0.9fr;
  gap: 32px;
  padding-bottom: 36px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 14px;
}

.brand-logo {
  height: 22px;
  width: auto;
  display: block;
}

h4 {
  color: #fff;
  font-size: 14px;
  margin: 0 0 10px;
}

p {
  margin: 0 0 6px;
}

a:hover {
  color: var(--zk-cyan);
}

.address {
  max-width: 320px;
}

.icp {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}

.qr img,
.qr-placeholder {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  object-fit: cover;
}

.copyright {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

@media (max-width: 900px) {
  .inner {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .inner {
    grid-template-columns: 1fr;
    gap: 22px;
  }
}
</style>
