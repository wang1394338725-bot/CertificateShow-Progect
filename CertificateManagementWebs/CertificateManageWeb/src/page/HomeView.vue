<!-- HomeView.vue：公开门户主页（默认页面）——三区块横排 + 主题切换 + 搜索 + 详情弹窗 -->
<template>
    <div class="home-page" :class="`theme-${theme}`">
        <!-- 顶部栏：主题切换 | 搜索区（居中偏右） | 登录按钮 -->
        <header class="top-bar">
            <div class="theme-switch">
                <el-radio-group v-model="theme" size="small">
                    <el-radio-button value="light">白</el-radio-button>
                    <el-radio-button value="dark">黑</el-radio-button>
                    <el-radio-button value="eye">护眼</el-radio-button>
                </el-radio-group>
            </div>

            <div class="search-area">
                <el-input v-model="keyword" placeholder="搜索奖状名称 / 获奖成员 / 赛事名称" clearable class="keyword-input"
                    @keyup.enter="handleSearch" @clear="backToShowcase" />
                <el-select v-model="level" placeholder="奖状等级" clearable class="level-select">
                    <el-option v-for="lv in LEVELS" :key="lv" :label="lv" :value="lv" />
                </el-select>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
                <el-button v-if="searchMode" @click="backToShowcase">返回展示</el-button>
            </div>

            <div class="login-area">
                <el-button type="primary" plain @click="$router.push('/login')">管理员登录</el-button>
            </div>
        </header>

        <main class="content" v-loading="loading">
            <!-- ===== 展示模式：三条横向区块，块内卡片横排并列 ===== -->
            <template v-if="!searchMode">
                <!-- 置顶推荐 -->
                <section class="section">
                    <h2 class="section-title">置顶推荐</h2>
                    <el-empty v-if="!home.pinned.length" description="暂无置顶奖状" :image-size="60" />
                    <div v-else class="card-row">
                        <div v-for="c in home.pinned" :key="'p' + c.id" class="cert-card pinned" @click="openDetail(c)">
                            <el-image :src="c.imageUrl" fit="cover" class="card-img" loading="lazy">
                                <template #error>
                                    <div class="img-fallback">图片缺失</div>
                                </template>
                            </el-image>
                            <div class="card-info">
                                <div class="c-title">{{ c.title }}</div>
                                <el-tag type="danger" size="small">{{ c.awardLevel }}</el-tag>
                                <div class="c-sub">{{ c.recipient }}</div>
                            </div>
                        </div>
                    </div>
                </section>

                <!-- 等级前三 -->
                <section class="section">
                    <h2 class="section-title clickable" @click="$router.push('/list?sortBy=level')">
                        等级前三<span class="view-all">查看全部 →</span>
                    </h2>
                    <div class="card-row three">
                        <template v-for="i in 3" :key="'l' + i">
                            <div v-if="home.byLevel[i - 1]" class="cert-card" @click="openDetail(home.byLevel[i - 1])">
                                <el-image :src="home.byLevel[i - 1].imageUrl" fit="cover" class="card-img"
                                    loading="lazy">
                                    <template #error>
                                        <div class="img-fallback">图片缺失</div>
                                    </template>
                                </el-image>
                                <div class="card-info">
                                    <div class="c-title">{{ home.byLevel[i - 1].title }}</div>
                                    <div class="c-sub">
                                        <el-tag size="small">{{ home.byLevel[i - 1].awardLevel }}</el-tag>
                                        {{ home.byLevel[i - 1].recipient }}
                                    </div>
                                </div>
                            </div>
                            <!-- 信息不足留空白栏位 -->
                            <div v-else class="cert-card empty">
                                <span>虚位以待</span>
                            </div>
                        </template>
                    </div>
                </section>

                <!-- 最新荣誉（按时间前三） -->
                <section class="section">
                    <h2 class="section-title clickable" @click="$router.push('/list?sortBy=time')">
                        最新荣誉<span class="view-all">查看全部 →</span>
                    </h2>
                    <div class="card-row three">
                        <template v-for="i in 3" :key="'t' + i">
                            <div v-if="home.byTime[i - 1]" class="cert-card" @click="openDetail(home.byTime[i - 1])">
                                <el-image :src="home.byTime[i - 1].imageUrl" fit="cover" class="card-img"
                                    loading="lazy">
                                    <template #error>
                                        <div class="img-fallback">图片缺失</div>
                                    </template>
                                </el-image>
                                <div class="card-info">
                                    <div class="c-title">{{ home.byTime[i - 1].title }}</div>
                                    <div class="c-sub">
                                        <el-tag size="small">{{ home.byTime[i - 1].awardLevel }}</el-tag>
                                        {{ home.byTime[i - 1].awardDate }}
                                    </div>
                                </div>
                            </div>
                            <div v-else class="cert-card empty">
                                <span>虚位以待</span>
                            </div>
                        </template>
                    </div>
                </section>
            </template>

            <!-- ===== 搜索模式：独立结果列表 ===== -->
            <template v-else>
                <section class="section">
                    <h2 class="section-title">
                        搜索结果
                        <span class="result-count">共 {{ page.total }} 条</span>
                    </h2>
                    <el-empty v-if="!results.length && !loading" description="没有找到相关奖状" />
                    <div v-else class="result-list">
                        <div v-for="c in results" :key="'r' + c.id" class="result-row" @click="openDetail(c)">
                            <el-tag size="small" :type="levelTagType(c.awardLevel)">{{ c.awardLevel }}</el-tag>
                            <span class="c-title">{{ c.title }}</span>
                            <span class="c-sub">{{ c.recipient }}</span>
                            <span class="c-sub">{{ c.eventName }}</span>
                            <span class="c-sub date">{{ c.awardDate }}</span>
                        </div>
                    </div>
                    <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
                        layout="total, prev, pager, next" class="pager" @current-change="fetchResults" />
                </section>
            </template>
        </main>

        <!-- 详情弹窗：屏幕正中心，图片在左，具体信息在右 -->
        <el-dialog v-model="detailVisible" :title="detail?.title" width="920px" align-center class="detail-dialog">
            <div class="detail-body" v-if="detail">
                <el-image :src="detail.imageUrl" fit="contain" class="detail-img" loading="lazy"
                    :preview-src-list="[detail.imageUrl]" hide-on-click-modal>
                    <template #error>
                        <div class="img-fallback">图片缺失</div>
                    </template>
                </el-image>
                <div class="detail-info">
                    <div class="detail-row"><span class="label">获奖成员</span><span>{{ detail.recipient }}</span></div>
                    <div class="detail-row"><span class="label">赛事名称</span><span>{{ detail.eventName }}</span></div>
                    <div class="detail-row"><span class="label">所属项目</span><span>{{ detail.projectName }}</span></div>
                    <div class="detail-row">
                        <span class="label">奖状等级</span>
                        <el-tag :type="levelTagType(detail.awardLevel)" size="large">{{ detail.awardLevel }}</el-tag>
                    </div>
                    <div class="detail-row"><span class="label">获奖日期</span><span>{{ detail.awardDate }}</span></div>
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
    isPinned: boolean
    status: number
    imageUrl: string
}

const LEVELS = ['国家级', '省级', '市级', '校级', '其他']

const levelTagType = (level?: string) => {
    switch (level) {
        case '国家级': return 'danger'
        case '省级': return 'warning'
        case '市级': return 'success'
        case '校级': return 'primary'
        default: return 'info'
    }
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
const home = reactive<{ pinned: Certificate[]; byLevel: Certificate[]; byTime: Certificate[] }>({
    pinned: [], byLevel: [], byTime: []
})

const fetchHome = async () => {
    loading.value = true
    try {
        const res = await axios.post('/api/certificate/home')
        if (res.data.code === 200) {
            home.pinned = res.data.data.pinned || []
            home.byLevel = res.data.data.byLevel || []
            home.byTime = res.data.data.byTime || []
        }
    } finally {
        loading.value = false
    }
}

// ---------- 搜索 ----------
const searchMode = ref(false)
const keyword = ref('')
const level = ref('')
const results = ref<Certificate[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })

const fetchResults = async () => {
    loading.value = true
    try {
        const res = await axios.post('/api/certificate/search', {
            current: page.current,
            size: page.size,
            keyword: keyword.value || undefined,
            level: level.value || undefined
        })
        if (res.data.code === 200) {
            results.value = res.data.data.records
            page.total = res.data.data.total
        }
    } finally {
        loading.value = false
    }
}

const handleSearch = () => {
    searchMode.value = true
    page.current = 1
    fetchResults()
}

const backToShowcase = () => {
    searchMode.value = false
    keyword.value = ''
    level.value = ''
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

/* 搜索区居中偏右 */
.search-area {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 10px;
}

.keyword-input {
    width: 320px;
}

.level-select {
    width: 130px;
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

.result-count {
    font-size: 13px;
    color: var(--hp-sub);
    font-weight: normal;
    margin-left: 8px;
}

/* 块内卡片横向并排 */
.card-row {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 14px;
}

/* 前三名区块固定三列横排 */
.card-row.three {
    grid-template-columns: repeat(3, 1fr);
}

.cert-card {
    display: flex;
    gap: 12px;
    align-items: center;
    background: var(--hp-card);
    border-radius: 8px;
    padding: 12px;
    cursor: pointer;
    transition: box-shadow 0.2s, transform 0.2s;
}

.cert-card:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
}

.cert-card.empty {
    justify-content: center;
    min-height: 104px;
    border: 1px dashed var(--hp-border);
    color: var(--hp-sub);
    cursor: default;
    background: transparent;
}

.cert-card.empty:hover {
    box-shadow: none;
    transform: none;
}

.card-img {
    width: 150px;
    height: 104px;
    border-radius: 6px;
    flex-shrink: 0;
}

.card-info {
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.c-title {
    font-weight: 600;
    margin-bottom: 4px;
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

/* 搜索结果 */
.result-list {
    background: var(--hp-card);
    border-radius: 8px;
    overflow: hidden;
}

.result-row {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 20px;
    border-bottom: 1px solid var(--hp-border);
    cursor: pointer;
    transition: background 0.15s;
}

.result-row:hover {
    background: var(--hp-bg);
}

.result-row .c-title {
    flex: 1;
}

.result-row .date {
    margin-left: auto;
}

.pager {
    margin-top: 16px;
    justify-content: flex-end;
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
</style>
