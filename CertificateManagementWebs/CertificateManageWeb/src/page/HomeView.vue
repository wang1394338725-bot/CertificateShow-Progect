<!-- HomeView.vue：公开门户主页（默认页面）——三区块横排 + 主题切换 + 详情弹窗 -->
<template>
    <div class="home-page" :class="`theme-${theme}`">
        <!-- 顶部栏：品牌 | 主题切换 | 登录按钮 -->
        <header class="top-bar">
            <div class="brand">
                <h1 class="brand-title">个人生活记录</h1>
                <span class="brand-sub">记录生活的点点滴滴</span>
            </div>
            <div class="theme-switch">
                <el-radio-group v-model="theme" size="small">
                    <el-radio-button value="light">白</el-radio-button>
                    <el-radio-button value="dark">黑</el-radio-button>
                    <el-radio-button value="eye">护眼</el-radio-button>
                </el-radio-group>
            </div>
            <div class="login-area">
                <el-button type="primary" plain @click="$router.push('/login')">管理员入口</el-button>
            </div>
        </header>

        <main class="content" v-loading="loading">
            <!-- ===== 三条横向区块，卡片纵排：想法在上、图片居中、名称日期在下 ===== -->
            <!-- 置顶推荐 -->
            <section class="section">
                <h2 class="section-title">置顶推荐</h2>
                <el-empty v-if="!home.pinned.length" description="暂无置顶记录" :image-size="60" />
                <div v-else class="card-row">
                    <div v-for="c in home.pinned" :key="'p' + c.id" class="cert-card pinned" @click="openDetail(c)">
                        <div class="c-idea">{{ ideaPreview(c) }}</div>
                        <div class="img-wrap">
                            <el-image :src="c.imageUrl" fit="cover" class="card-img" loading="lazy">
                                <template #error>
                                    <div class="img-fallback">图片缺失</div>
                                </template>
                            </el-image>
                            <span class="pin-badge">置顶</span>
                        </div>
                        <div class="card-info">
                            <div class="c-title">{{ c.title }}</div>
                            <div class="c-sub">{{ c.awardDate }}</div>
                        </div>
                    </div>
                </div>
            </section>

            <!-- 其他记录（时间序较旧的一批，取 /view 第二页） -->
            <section class="section">
                <h2 class="section-title clickable" @click="$router.push('/list')">
                    其他记录<span class="view-all">全部记录 →</span>
                </h2>
                <div class="card-row three">
                    <template v-for="i in 3" :key="'o' + i">
                        <div v-if="home.others[i - 1]" class="cert-card" @click="openDetail(home.others[i - 1])">
                            <div class="c-idea">{{ ideaPreview(home.others[i - 1]) }}</div>
                            <div class="img-wrap">
                                <el-image :src="home.others[i - 1].imageUrl" fit="cover" class="card-img" loading="lazy">
                                    <template #error>
                                        <div class="img-fallback">图片缺失</div>
                                    </template>
                                </el-image>
                            </div>
                            <div class="card-info">
                                <div class="c-title">{{ home.others[i - 1].title }}</div>
                                <div class="c-sub">{{ home.others[i - 1].awardDate }}</div>
                            </div>
                        </div>
                        <!-- 记录不足留空白栏位 -->
                        <div v-else class="cert-card empty">
                            <span>暂无记录</span>
                        </div>
                    </template>
                </div>
            </section>

            <!-- 最新记录（按时间倒序第一页） -->
            <section class="section">
                <h2 class="section-title clickable" @click="$router.push('/list')">
                    最新记录<span class="view-all">全部记录 →</span>
                </h2>
                <div class="card-row three">
                    <template v-for="i in 3" :key="'t' + i">
                        <div v-if="home.latest[i - 1]" class="cert-card" @click="openDetail(home.latest[i - 1])">
                            <div class="c-idea">{{ ideaPreview(home.latest[i - 1]) }}</div>
                            <div class="img-wrap">
                                <el-image :src="home.latest[i - 1].imageUrl" fit="cover" class="card-img" loading="lazy">
                                    <template #error>
                                        <div class="img-fallback">图片缺失</div>
                                    </template>
                                </el-image>
                            </div>
                            <div class="card-info">
                                <div class="c-title">{{ home.latest[i - 1].title }}</div>
                                <div class="c-sub">{{ home.latest[i - 1].awardDate }}</div>
                            </div>
                        </div>
                        <div v-else class="cert-card empty">
                            <span>暂无记录</span>
                        </div>
                    </template>
                </div>
            </section>
        </main>

        <!-- 备案页脚：工信部要求公示在网站底部，采用常见双行样式（版权 + 备案号） -->
        <footer class="icp-footer">
            <div class="footer-copy">© 2026 个人生活记录 · 保留所有权利</div>
            <a href="https://beian.miit.gov.cn/" target="_blank" rel="noreferrer">皖ICP备2026031376号-1</a>
        </footer>

        <!-- 详情弹窗：屏幕正中心，图片在左，具体信息在右 -->
        <el-dialog v-model="detailVisible" title="记录详情" width="920px" align-center class="detail-dialog">
            <div class="detail-body" v-if="detail">
                <el-image :src="detail.imageUrl" fit="contain" class="detail-img" loading="lazy"
                    :preview-src-list="[detail.imageUrl]" hide-on-click-modal>
                    <template #error>
                        <div class="img-fallback">图片缺失</div>
                    </template>
                </el-image>
                <div class="detail-info">
                    <div class="detail-row"><span class="label">记录名称</span><span>{{ detail.title }}</span></div>
                    <div class="detail-row"><span class="label">记录日期</span><span>{{ detail.awardDate }}</span></div>
                    <div class="detail-row"><span class="label">想法</span><span>{{ detail.organization?.trim() || '—' }}</span></div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import axios from 'axios'

interface Certificate {
    id: number
    title: string
    recipient: string
    eventName: string
    awardLevel: string
    projectName: string
    awardDate: string
    organization?: string
    isPinned: boolean
    status: number
    imageUrl: string
}

// ---------- 主题切换（白/黑/护眼，记忆在 localStorage） ----------
type Theme = 'light' | 'dark' | 'eye'
const theme = ref<Theme>((localStorage.getItem('home-theme') as Theme) || 'light')

watch(theme, (t) => {
    localStorage.setItem('home-theme', t)
    // 黑色模式交给 element-plus 的 dark 变量（main.ts 已引入 dark/css-vars.css）
    document.documentElement.classList.toggle('dark', t === 'dark')
}, { immediate: true })

// ---------- 展示数据 ----------
const loading = ref(false)
const home = reactive<{ pinned: Certificate[]; latest: Certificate[]; others: Certificate[] }>({
    pinned: [], latest: [], others: []
})

// 卡片想法预览：organization 截断若干字加省略号；为空回退为记录名称
const IDEA_PREVIEW_LEN = 30
const ideaPreview = (c: Certificate) => {
    const org = (c.organization ?? '').trim()
    if (!org) return c.title
    return org.length > IDEA_PREVIEW_LEN ? org.slice(0, IDEA_PREVIEW_LEN) + '…' : org
}

const fetchHome = async () => {
    loading.value = true
    try {
        const res = await axios.post('/api/certificate/home')
        if (res.data.code === 200) {
            home.pinned = res.data.data.pinned || []
        }
        // 最新记录：/view 按时间倒序第一页；其他记录：同接口第二页
        const baseParams = { status: '0', sortBy: 'time', size: 3 }
        const [latestRes, othersRes] = await Promise.all([
            axios.post('/api/certificate/view', { ...baseParams, current: 1 }),
            axios.post('/api/certificate/view', { ...baseParams, current: 2 })
        ])
        if (latestRes.data.code === 200) {
            home.latest = latestRes.data.data.records || []
        }
        if (othersRes.data.code === 200) {
            home.others = othersRes.data.data.records || []
        }
    } finally {
        loading.value = false
    }
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<Certificate | null>(null)

const openDetail = (c: Certificate) => {
    detail.value = c
    detailVisible.value = true
}

onMounted(() => {
    fetchHome()
})
</script>

<style scoped>
/* ===== 主题变量（默认白） ===== */
.home-page {
    --hp-bg: #f5f7fa;
    --hp-bar: #ffffff;
    --hp-card: #ffffff;
    --hp-text: #303133;
    --hp-sub: #909399;
    --hp-border: #dcdfe6;
    --el-fill-color-blank: var(--hp-card);
    min-height: 100vh;
    background: var(--hp-bg);
    color: var(--hp-text);
}

/* 黑色模式：Element Plus dark 变量已由 html.dark 生效，这里补页面自己的颜色 */
.home-page.theme-dark {
    --hp-bg: #101014;
    --hp-bar: #1d1d22;
    --hp-card: #1d1d22;
    --hp-text: #e5e7eb;
    --hp-sub: #9ca3af;
    --hp-border: #363640;
}

/* 护眼模式：豆沙绿背景，卡片淡绿 */
.home-page.theme-eye {
    --hp-bg: #cce8cf;
    --hp-bar: #d8eeda;
    --hp-card: #e6f2e7;
    --hp-text: #2f3e33;
    --hp-sub: #6b7f70;
    --hp-border: #b8d4bb;
    --el-fill-color-blank: #e6f2e7;
    --el-bg-color: #e6f2e7;
    --el-bg-color-overlay: #e6f2e7;
}

/* 顶部栏 */
.top-bar {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 0 32px;
    height: 64px;
    background: var(--hp-bar);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    position: sticky;
    top: 0;
    z-index: 10;
}

/* 品牌区 */
.brand {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
}

.brand-title {
    font-size: 20px;
    font-weight: 700;
    margin: 0;
    line-height: 1.2;
    letter-spacing: 1px;
    background: linear-gradient(120deg, #409eff, #79bbff);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
}

.brand-sub {
    font-size: 12px;
    color: var(--hp-sub);
}

.login-area {
    white-space: nowrap;
}

/* 内容区 */
.content {
    max-width: 1280px;
    margin: 0 auto;
    padding: 24px 32px 48px;
}

.section {
    margin-bottom: 28px;
}

.section-title {
    font-size: 18px;
    margin: 0 0 14px;
    padding-left: 10px;
    border-left: 4px solid #409eff;
}

/* 可点击区块标题：点击跳转到对应排序的完整列表 */
.section-title.clickable {
    cursor: pointer;
    transition: color 0.2s;
}

.section-title.clickable:hover {
    color: #409eff;
}

.view-all {
    float: right;
    font-size: 13px;
    font-weight: normal;
    color: var(--hp-sub);
    line-height: 26px;
    transition: color 0.2s;
}

.section-title.clickable:hover .view-all {
    color: #409eff;
}

/* 块内卡片横向并排（卡片纵排：想法在上、图片居中、名称日期在下） */
.card-row {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 14px;
}

/* 两个三卡区块固定三列横排 */
.card-row.three {
    grid-template-columns: repeat(3, 1fr);
}

.cert-card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    background: var(--hp-card);
    border: 1px solid var(--hp-border);
    border-radius: 10px;
    padding: 14px;
    cursor: pointer;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    transition: box-shadow 0.25s, transform 0.25s, border-color 0.25s;
}

.cert-card:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    transform: translateY(-3px);
    border-color: transparent;
}

.cert-card.empty {
    justify-content: center;
    align-items: center;
    min-height: 200px;
    border: 1px dashed var(--hp-border);
    color: var(--hp-sub);
    cursor: default;
    background: transparent;
}

.cert-card.empty:hover {
    box-shadow: none;
    transform: none;
}

/* 想法预览：图片上方一行，超出省略 */
.c-idea {
    font-size: 13px;
    color: var(--hp-sub);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 图片容器：圆角裁剪 + 悬停缩放；置顶角标叠在图上 */
.img-wrap {
    position: relative;
    overflow: hidden;
    border-radius: 8px;
}

.card-img {
    width: 100%;
    height: 170px;
    display: block;
}

.img-wrap :deep(img) {
    transition: transform 0.35s ease;
}

.cert-card:hover .img-wrap :deep(img) {
    transform: scale(1.06);
}

.pin-badge {
    position: absolute;
    top: 8px;
    left: 8px;
    z-index: 2;
    background: rgba(230, 162, 60, 0.95);
    color: #fff;
    font-size: 12px;
    line-height: 1;
    padding: 4px 8px;
    border-radius: 4px;
    letter-spacing: 1px;
}

.card-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.c-title {
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.c-sub {
    font-size: 13px;
    color: var(--hp-sub);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.img-fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--hp-border);
    color: var(--hp-sub);
    font-size: 13px;
}

/* 备案页脚：常见样式——顶部分隔线、居中灰字、版权与备案号双行 */
.icp-footer {
    border-top: 1px solid var(--hp-border);
    background: var(--hp-bar);
    text-align: center;
    padding: 18px 12px 22px;
    font-size: 12px;
    line-height: 1.9;
    color: var(--hp-sub);
}

.icp-footer a {
    color: var(--hp-sub);
    text-decoration: none;
}

.icp-footer a:hover {
    color: #409eff;
}

/* 详情弹窗：更大、居中 */
.detail-body {
    display: flex;
    gap: 28px;
    align-items: center;
}

.detail-img {
    width: 520px;
    height: 430px;
    flex-shrink: 0;
    border-radius: 8px;
    background: var(--hp-border);
}

.detail-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 22px;
    justify-content: center;
}

.detail-row {
    font-size: 16px;
}

.detail-row .label {
    color: var(--hp-sub);
    width: 80px;
    display: inline-block;
    margin-right: 10px;
}

/* ===== 移动端适配（≤768px）===== */
@media (max-width: 768px) {
    /* 顶栏改两行：第一行品牌+登录，第二行主题切换 */
    .top-bar {
        flex-wrap: wrap;
        height: auto;
        padding: 8px 12px;
        row-gap: 8px;
    }

    .brand-title {
        font-size: 16px;
    }

    .login-area {
        margin-left: auto;
    }

    .content {
        padding: 16px 12px 40px;
    }

    .section-title {
        font-size: 16px;
    }

    /* 三卡区块固定三列 → 单列纵排 */
    .card-row.three {
        grid-template-columns: 1fr;
    }

    .card-img {
        height: 140px;
    }

    /* 详情弹窗：图上文下 */
    .detail-body {
        flex-direction: column;
        gap: 16px;
    }

    .detail-img {
        width: 100%;
        height: auto;
        aspect-ratio: 4 / 3;
    }

    .detail-row {
        font-size: 14px;
    }

    .detail-row .label {
        width: 72px;
        margin-right: 8px;
    }
}
</style>
