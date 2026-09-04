<template>
  <div class="home">
    <!-- 第一屏：品牌主视觉 -->
    <section class="hero">
      <div class="hero-bg" :style="heroBgStyle"></div>
      <div class="hero-mask"></div>
      <div class="zk-container hero-inner zk-fade-in">
        <p class="hero-eyebrow">ZKTECO · CHANGE MAKER</p>
        <h1>{{ hero.slogan1 || 'AI认知让空间更智能，让世界更美好' }}</h1>
        <p class="hero-sub">{{ hero.slogan2 }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="go('social-jobs')">
            {{ hero.socialBtnText || '探索社会招聘' }}
          </el-button>
          <el-button size="large" round class="ghost-btn" @click="go('campus-jobs')">
            {{ hero.campusBtnText || '加入校园招聘' }}
          </el-button>
        </div>
      </div>
      <div class="hero-scroll">向下滚动了解更多</div>
    </section>

    <!-- 职位方向（按职位类别字典），紧接主视觉、位于企业介绍之上 -->
    <section class="tracks">
      <div class="zk-container">
        <h2 class="zk-section-title center">{{ tracks.title || '职位方向' }}</h2>
        <p class="zk-section-sub center">{{ tracks.subtitle }}</p>

        <div v-if="categoriesLoading" class="tracks-grid">
          <div v-for="n in 6" :key="n" class="track-card is-skeleton"></div>
        </div>
        <el-empty
          v-else-if="categories.length === 0"
          class="tracks-empty"
          description="暂无启用中的职位类别"
        />
        <div v-else class="tracks-grid">
          <button
            v-for="(item, index) in categories"
            :key="item.id"
            type="button"
            class="track-card"
            :class="{ 'is-empty': !item.publishedCount }"
            @click="openCategory(item)"
          >
            <span class="track-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="track-name">{{ item.name }}</span>
            <span class="track-en">{{ categoryMeta(item.name).en }}</span>
            <span class="track-hint">{{ categoryMeta(item.name).hint }}</span>
            <span class="track-foot">
              <span class="track-count">{{ item.publishedCount || 0 }} 个在招</span>
              <span class="track-go">进入该类别 →</span>
            </span>
          </button>
        </div>

        <div class="tracks-more">
          <el-button round @click="go('all-jobs')">
            {{ tracks.allBtnText || '浏览全部在招职位' }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- 认识熵基科技 -->
    <section class="about">
      <div class="zk-container about-inner">
        <div class="about-text">
          <h2 class="zk-section-title">{{ about.title || '认识熵基科技' }}</h2>
          <p class="zk-section-sub">{{ about.intro }}</p>
          <div class="stats">
            <div v-for="(item, index) in about.stats || []" :key="index" class="stat">
              <div class="stat-value">
                {{ item.value }}<span class="stat-unit">{{ item.unit }}</span>
              </div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
          <p v-if="about.dataNote" class="data-note">{{ about.dataNote }}</p>
        </div>
        <div class="about-media">
          <img v-if="about.image" :src="about.image" alt="企业环境" />
          <div v-else class="zk-placeholder media-placeholder">企业环境图占位</div>
        </div>
      </div>
    </section>

    <!-- 第三屏：四大业务方向 -->
    <section class="business">
      <div class="zk-container">
        <h2 class="zk-section-title center">{{ business.title || '四大业务方向' }}</h2>
        <p class="zk-section-sub center">从感知到决策，四条业务主线构成熵基的能力版图</p>
        <div class="business-grid">
          <article v-for="(item, index) in business.items || []" :key="index" class="business-card">
            <div class="card-media">
              <img v-if="item.image" :src="item.image" :alt="item.name" />
              <div v-else class="zk-placeholder card-placeholder">{{ item.name }}</div>
            </div>
            <div class="card-body">
              <h3>{{ item.name }}</h3>
              <p>{{ item.desc }}</p>
              <p v-if="item.jobDirections" class="job-directions">
                <el-icon><Briefcase /></el-icon>
                {{ item.jobDirections }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 第四屏：全球布局 -->
    <section class="global">
      <div class="zk-container global-inner">
        <div class="global-text">
          <h2 class="zk-section-title">{{ globalSection.title || '全球布局' }}</h2>
          <p class="zk-section-sub">{{ globalSection.desc }}</p>
          <div class="highlights">
            <div v-for="(item, index) in globalSection.highlights || []" :key="index" class="highlight">
              <span class="dot"></span>
              <span class="label">{{ item.label }}</span>
              <span class="value">{{ item.value }}</span>
            </div>
          </div>
          <el-button type="primary" round @click="go(globalSection.btnTarget === 'CAMPUS' ? 'campus-jobs' : 'social-jobs')">
            {{ globalSection.btnText || '探索招聘机会' }}
          </el-button>
        </div>
        <div class="global-map">
          <img v-if="globalSection.mapImage" :src="globalSection.mapImage" alt="全球布局" />
          <!-- 使用静态 SVG，不引入地图库与 WebGL（§7.2.4） -->
          <WorldMap v-else />
        </div>
      </div>
    </section>

    <!-- 企业文化与工作体验 -->
    <section class="culture">
      <div class="zk-container">
        <h2 class="zk-section-title center">{{ culture.title || '企业文化与工作体验' }}</h2>
        <div class="values">
          <div v-for="(item, index) in culture.values || []" :key="index" class="value">
            <div class="value-name">{{ item.name }}</div>
            <div class="value-desc">{{ item.desc }}</div>
          </div>
        </div>
        <div class="culture-cards">
          <article v-for="(item, index) in culture.cards || []" :key="index" class="culture-card">
            <div class="culture-media">
              <img v-if="item.image" :src="item.image" :alt="item.title" />
              <div v-else class="zk-placeholder culture-placeholder">{{ item.title }}</div>
            </div>
            <h4>{{ item.title }}</h4>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </div>
    </section>

    <!-- 第七屏：最终行动号召 -->
    <section class="cta">
      <div class="zk-container cta-inner">
        <h2>{{ cta.title || '逐梦AI，与熵基一起成为 Change-Maker' }}</h2>
        <p>{{ cta.subtitle }}</p>
        <div class="cta-actions">
          <el-button type="primary" size="large" round @click="go('social-jobs')">
            {{ cta.socialBtnText || '浏览社会招聘' }}
          </el-button>
          <el-button size="large" round class="ghost-btn" @click="go('campus-jobs')">
            {{ cta.campusBtnText || '浏览校园招聘' }}
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorldMap from '@/components/WorldMap.vue'
import { publicApi } from '@/api'
import { toast } from '@/api/http'
import { HOME_CONTENT } from '@/config/homeContent'

const router = useRouter()

// 首页文案为前端维护（§7.2）；职位类别来自启用中的字典
const hero = HOME_CONTENT.hero
const about = HOME_CONTENT.about
const business = HOME_CONTENT.business
const globalSection = HOME_CONTENT.global
const tracks = HOME_CONTENT.jobCategories
const culture = HOME_CONTENT.culture
const cta = HOME_CONTENT.cta

const categories = ref([])
// 首次渲染即显示骨架屏，避免挂载前短暂显示空类别状态
const categoriesLoading = ref(true)

const heroBgStyle = computed(() =>
  hero.bgImage ? { backgroundImage: `url(${hero.bgImage})` } : {}
)

function categoryMeta(name) {
  return tracks.meta?.[name] || tracks.fallback || { en: 'Open Roles', hint: '查看该方向正在招聘的职位' }
}

function go(name) {
  router.push({ name })
}

function openCategory(item) {
  router.push({
    name: 'all-jobs',
    query: { categoryId: String(item.id) }
  })
}

onMounted(async () => {
  categoriesLoading.value = true
  try {
    categories.value = (await publicApi.categories()) || []
  } catch (error) {
    toast(error, '职位类别加载失败')
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
})
</script>

<style scoped>
section {
  padding: 92px 0;
}

.center {
  text-align: center;
}

/* ---------- 第一屏 ---------- */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #081139 0%, #142c86 45%, #4a2ea8 100%);
}

.hero-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.55;
  transform: scale(1.04);
}

.hero-mask {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1000px 520px at 18% 28%, rgba(34, 211, 238, 0.22), transparent 60%),
    radial-gradient(900px 480px at 82% 72%, rgba(123, 92, 255, 0.28), transparent 62%),
    linear-gradient(180deg, rgba(6, 12, 40, 0.55) 0%, rgba(6, 12, 40, 0.82) 100%);
}

.hero-inner {
  position: relative;
  color: #fff;
  padding-top: var(--zk-header-height);
}

.hero-eyebrow {
  letter-spacing: 5px;
  font-size: 12px;
  color: var(--zk-cyan);
  margin: 0 0 18px;
}

.hero h1 {
  font-size: 52px;
  line-height: 1.25;
  margin: 0 0 18px;
  max-width: 900px;
  font-weight: 700;
}

.hero-sub {
  font-size: 19px;
  color: rgba(255, 255, 255, 0.82);
  margin: 0 0 36px;
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.5);
  color: #fff;
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.22);
  border-color: #fff;
  color: #fff;
}

.hero-scroll {
  position: absolute;
  bottom: 26px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.55);
}

/* ---------- 第二屏 ---------- */
.about-inner {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 56px;
  align-items: center;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
  margin-top: 34px;
}

.data-note {
  margin: 14px 0 0;
  color: var(--zk-muted);
  font-size: 12px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--zk-primary);
}

.stat-unit {
  font-size: 14px;
  margin-left: 3px;
  color: var(--zk-text-muted);
  font-weight: 500;
}

.stat-label {
  font-size: 13px;
  color: var(--zk-text-muted);
  margin-top: 2px;
}

.about-media img,
.media-placeholder {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: var(--zk-radius);
  object-fit: cover;
  box-shadow: var(--zk-shadow);
}

/* ---------- 第三屏 ---------- */
.business {
  background: var(--zk-bg-soft);
}

.business-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
  margin-top: 44px;
}

.business-card {
  background: #fff;
  border-radius: var(--zk-radius);
  overflow: hidden;
  box-shadow: var(--zk-shadow);
  transition: transform 0.25s ease;
}

.business-card:hover {
  transform: translateY(-6px);
}

.card-media img,
.card-placeholder {
  width: 100%;
  aspect-ratio: 3 / 2;
  object-fit: cover;
}

.card-body {
  padding: 18px 20px 22px;
}

.card-body h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.card-body p {
  margin: 0;
  font-size: 13px;
  color: var(--zk-text-muted);
  line-height: 1.75;
}

.job-directions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px !important;
  color: var(--zk-primary) !important;
  font-size: 12px !important;
}

/* ---------- 第四屏 ---------- */
.global-inner {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 48px;
  align-items: center;
}

.highlights {
  margin: 28px 0;
  display: grid;
  gap: 12px;
}

.highlight {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--zk-primary), var(--zk-accent));
}

.highlight .label {
  color: var(--zk-text-muted);
  min-width: 76px;
}

.highlight .value {
  font-weight: 600;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
}

.global-map img {
  width: 100%;
  border-radius: var(--zk-radius);
}

/* ---------- 职位方向（主视觉后，版式对齐业务方向 / 企业文化） ---------- */
.tracks {
  background: var(--zk-bg-soft);
}

.tracks .zk-section-sub {
  max-width: 560px;
  margin-left: auto;
  margin-right: auto;
}

.tracks-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
  margin-top: 44px;
}

.track-card {
  appearance: none;
  font: inherit;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  min-height: 228px;
  padding: 24px 22px 20px;
  text-align: left;
  color: var(--zk-text);
  cursor: pointer;
  overflow: hidden;
  border: none;
  border-radius: var(--zk-radius);
  background: #fff;
  box-shadow: var(--zk-shadow);
  transition: transform 0.25s ease;
}

.track-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(135deg, var(--zk-primary), var(--zk-accent));
  opacity: 0;
  transition: opacity 0.22s ease;
}

.track-card:hover {
  transform: translateY(-6px);
}

.track-card:focus-visible {
  outline: 2px solid var(--zk-primary);
  outline-offset: 3px;
}

.track-card:hover::before {
  opacity: 1;
}

.track-card.is-skeleton {
  min-height: 228px;
  pointer-events: none;
  box-shadow: none;
  background: linear-gradient(90deg, #e8ebf4, #f4f6fb, #e8ebf4);
  background-size: 200% 100%;
  animation: track-shimmer 1.2s linear infinite;
}

.track-card.is-skeleton::before {
  display: none;
}

.track-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 28px;
  margin-bottom: 16px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--zk-primary);
  background: rgba(28, 72, 216, 0.08);
  border-radius: 999px;
}

.track-name {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  width: 100%;
  min-height: 26px;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--zk-ink);
}

.track-en {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  width: 100%;
  min-height: 16px;
  margin-top: 6px;
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--zk-text-muted);
}

.track-hint {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  width: 100%;
  min-height: 46px;
  margin-top: 10px;
  font-size: 13px;
  color: var(--zk-text-muted);
  line-height: 1.75;
}

.track-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--zk-border);
}

.track-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--zk-primary);
}

.track-go {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--zk-text-muted);
}

.track-card:hover .track-go {
  color: var(--zk-primary);
}

.track-card.is-empty .track-count {
  color: var(--zk-text-muted);
  font-weight: 500;
}

.tracks-more {
  display: flex;
  justify-content: center;
  margin-top: 36px;
}

.tracks-empty {
  padding: 48px 0 8px;
}

@keyframes track-shimmer {
  from { background-position: 100% 0; }
  to { background-position: -100% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .track-card.is-skeleton {
    animation: none;
  }
}

/* ---------- 第六屏 ---------- */
.values {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 18px;
  margin: 40px 0 44px;
}

.value {
  padding: 22px 20px;
  border: 1px solid var(--zk-border);
  border-radius: var(--zk-radius);
  background: #fff;
}

.value-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--zk-primary);
  margin-bottom: 6px;
}

.value-desc {
  font-size: 13px;
  color: var(--zk-text-muted);
  line-height: 1.75;
}

.culture-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 22px;
}

.culture-card h4 {
  margin: 14px 0 6px;
  font-size: 17px;
}

.culture-card p {
  margin: 0;
  font-size: 13px;
  color: var(--zk-text-muted);
  line-height: 1.75;
}

.culture-media img,
.culture-placeholder {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: var(--zk-radius);
  object-fit: cover;
}

/* ---------- 第七屏 ---------- */
.cta {
  background: linear-gradient(135deg, #081139, #2b1a6e 60%, #4a2ea8);
  color: #fff;
  text-align: center;
}

.cta h2 {
  font-size: 34px;
  margin: 0 0 14px;
}

.cta p {
  margin: 0 0 30px;
  color: rgba(255, 255, 255, 0.8);
}

.cta-actions {
  display: flex;
  gap: 14px;
  justify-content: center;
  flex-wrap: wrap;
}

@media (max-width: 1024px) {
  .business-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .about-inner,
  .global-inner {
    grid-template-columns: 1fr;
  }
  .tracks-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  section {
    padding: 60px 0;
  }
  .hero h1 {
    font-size: 32px;
  }
  .hero-sub {
    font-size: 16px;
  }
  .business-grid {
    grid-template-columns: 1fr;
  }
  .tracks-grid {
    grid-template-columns: 1fr;
  }
  .cta h2 {
    font-size: 24px;
  }
}
</style>
