<!-- PublicBrowseView.vue：公开奖状一览页（访客视角）——仅供浏览，不含任何管理功能 -->
<template>
    <div class="list-page">
        <!-- 顶部栏 -->
        <header class="top-bar">
            <h1 class="page-title">奖状一览</h1>
            <div class="toolbar">
                <el-select v-model="sortBy" class="sort-select" @change="handleFilterChange">
                    <el-option label="按获奖时间（新→旧）" value="time" />
                    <el-option label="按等级（国家→省→市→校）" value="level" />
                </el-select>
                <el-select v-model="level" placeholder="全部等级" clearable class="level-select" @change="handleFilterChange">
                    <el-option v-for="lv in LEVELS" :key="lv" :label="lv" :value="lv" />
                </el-select>
                <el-button :icon="HomeFilled" @click="$router.push('/')">返回主页</el-button>
            </div>
        </header>

        <!-- 列表 -->
        <main class="content" v-loading="loading">
            <el-empty v-if="!records.length && !loading" description="暂无奖状" />
            <div v-else class="card-grid">
                <div v-for="c in records" :key="c.id" class="cert-card" @click="openDetail(c)">
                    <el-image :src="c.imageUrl" fit="cover" class="card-img" loading="lazy">
                        <template #error>
                            <div class="img-fallback">图片缺失</div>
                        </template>
                    </el-image>
                    <div class="card-info">
                        <div class="c-title">{{ c.title }}</div>
                        <div class="c-meta">
                            <el-tag size="small" :type="levelTagType(c.awardLevel)">{{ c.awardLevel }}</el-tag>
                            <span class="c-date">{{ c.awardDate }}</span>
                        </div>
                        <div class="c-sub">{{ c.recipient }}</div>
                    </div>
                </div>
            </div>
            <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
                layout="total, prev, pager, next" class="pager" @current-change="fetchData" />
        </main>

        <!-- 详情弹窗 -->
        <el-dialog v-model="detailVisible" :title="detail?.title" width="860px" align-center class="detail-dialog">
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
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import type { ApiResponse, Certificate, PageResult } from '../api/api'

const route = useRoute()

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

// ---------- 列表数据 ----------
// 公开页固定只看正常状态（status=0）：待审核删除/已隐藏的奖状仅管理员可见
const sortBy = ref<'time' | 'level'>((route.query.sortBy as 'level') || 'time')
const level = ref('')
const records = ref<Certificate[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 12, total: 0 })

const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.post<ApiResponse<PageResult<Certificate>>>('/api/certificate/view', {
            current: page.current,
            size: page.size,
            level: level.value || undefined,
            status: '0',
            sortBy: sortBy.value
        })
        if (res.data.code === 200) {
            records.value = res.data.data.records
            page.total = res.data.data.total
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        loading.value = false
    }
}

const handleFilterChange = () => {
    page.current = 1
    fetchData()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<Certificate | null>(null)

const openDetail = (c: Certificate) => {
    detail.value = c
    detailVisible.value = true
}

onMounted(fetchData)
</script>

<style scoped>
.list-page {
    min-height: 100vh;
    background: #f5f7fa;
    color: #303133;
}

.top-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 0 32px;
    height: 60px;
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    position: sticky;
    top: 0;
    z-index: 10;
}

.page-title {
    font-size: 20px;
    margin: 0;
    border-left: 4px solid #409eff;
    padding-left: 10px;
}

.toolbar {
    display: flex;
    align-items: center;
    gap: 10px;
}

.sort-select {
    width: 200px;
}

.level-select {
    width: 120px;
}

.content {
    max-width: 1280px;
    margin: 0 auto;
    padding: 24px 32px 48px;
}

.card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 14px;
}

.cert-card {
    display: flex;
    gap: 12px;
    align-items: center;
    background: #fff;
    border-radius: 8px;
    padding: 12px;
    cursor: pointer;
    transition: box-shadow 0.2s, transform 0.2s;
}

.cert-card:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
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
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.c-meta {
    display: flex;
    align-items: center;
    gap: 8px;
}

.c-date {
    font-size: 13px;
    color: #909399;
}

.c-sub {
    font-size: 13px;
    color: #909399;
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
    background: #dcdfe6;
    color: #909399;
    font-size: 13px;
}

.pager {
    margin-top: 20px;
    justify-content: flex-end;
}

/* 详情弹窗：图片在左，信息在右 */
.detail-body {
    display: flex;
    gap: 28px;
    align-items: center;
}

.detail-img {
    width: 480px;
    height: 400px;
    flex-shrink: 0;
    border-radius: 8px;
    background: #dcdfe6;
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
    color: #909399;
    width: 80px;
    display: inline-block;
    margin-right: 10px;
}
</style>
