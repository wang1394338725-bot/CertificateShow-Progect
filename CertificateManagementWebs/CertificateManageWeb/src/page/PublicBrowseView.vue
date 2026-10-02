<!-- PublicBrowseView.vue：公开记录一览页（访客视角）——仅供浏览，不含任何管理功能 -->
<template>
    <div class="list-page">
        <!-- 顶部栏 -->
        <header class="top-bar">
            <h1 class="page-title">记录一览</h1>
            <div class="toolbar">
                <el-button :icon="HomeFilled" class="home-btn" @click="$router.push('/')">返回主页</el-button>
            </div>
        </header>

        <!-- 列表 -->
        <main class="content" v-loading="loading">
            <el-empty v-if="!records.length && !loading" description="暂无记录" />
            <div v-else class="card-grid">
                <div v-for="c in records" :key="c.id" class="cert-card" @click="openDetail(c)">
                    <el-image :src="c.imageUrl" fit="cover" class="card-img" loading="lazy">
                        <template #error>
                            <div class="img-fallback">图片缺失</div>
                        </template>
                    </el-image>
                    <div class="card-info">
                        <div class="c-title">{{ c.title }}</div>
                        <div class="c-date">{{ c.awardDate }}</div>
                        <div v-if="c.organization?.trim()" class="c-sub">{{ c.organization }}</div>
                    </div>
                </div>
            </div>
            <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
                layout="total, prev, pager, next" class="pager" @current-change="fetchData" />
        </main>

        <!-- 详情弹窗 -->
        <el-dialog v-model="detailVisible" title="记录详情" width="860px" align-center class="detail-dialog">
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
import { reactive, ref, onMounted } from 'vue'
import { HomeFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import type { ApiResponse, Certificate, PageResult } from '../api/api'

// ---------- 列表数据 ----------
// 公开页固定只看正常状态（status=0）且按时间倒序：待审核删除/已隐藏的记录仅管理员可见
const records = ref<Certificate[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 12, total: 0 })

const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.post<ApiResponse<PageResult<Certificate>>>('/api/certificate/view', {
            current: page.current,
            size: page.size,
            status: '0',
            sortBy: 'time'
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
    border: 1px solid #e4e7ed;
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

.card-img {
    width: 150px;
    height: 104px;
    border-radius: 8px;
    flex-shrink: 0;
    overflow: hidden;
}

.card-img :deep(img) {
    transition: transform 0.35s ease;
}

.cert-card:hover .card-img :deep(img) {
    transform: scale(1.08);
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

/* ===== 移动端适配（≤768px）===== */
@media (max-width: 768px) {
    /* 顶栏改两行：标题一行，工具栏全宽换行 */
    .top-bar {
        flex-wrap: wrap;
        height: auto;
        padding: 8px 12px;
        row-gap: 8px;
    }

    .page-title {
        font-size: 17px;
    }

    .toolbar {
        flex: 1 1 100%;
        flex-wrap: wrap;
    }

    /* 返回主页只留图标，省空间 */
    .home-btn :deep(span) {
        display: none;
    }

    .content {
        padding: 16px 12px 40px;
    }

    .card-img {
        width: 110px;
        height: 80px;
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

    .pager {
        justify-content: center;
    }
}
</style>
