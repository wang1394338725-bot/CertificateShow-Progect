<!-- BrowseView.vue -->
<template>
    <div class="browse-container">
        <!-- 检索区域（保持不变） -->
        <el-card class="search-card">
            <el-form :model="searchForm" inline>
                <el-form-item label="关键词">
                    <el-input v-model="searchForm.keyword" placeholder="奖状名称/获奖人员" />
                </el-form-item>
                <el-form-item label="获奖级别">
                    <el-select v-model="searchForm.level" placeholder="全部" clearable>
                        <el-option label="国家级" value="国家级" />
                        <el-option label="省级" value="省级" />
                        <el-option label="市级" value="市级" />
                        <el-option label="校级" value="校级" />
                        <el-option label="其他" value="其他" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="全部" clearable>
                        <!-- value 必须与数据库 status 编码一致：0-正常，1-待审核删除，2-已隐藏 -->
                        <el-option label="正常" value="0" />
                        <el-option label="待审核删除" value="1" />
                        <el-option label="已隐藏" value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="排序">
                    <el-select v-model="searchForm.sortBy" placeholder="默认（置顶优先）" @change="handleSearch">
                        <el-option label="按等级（国家→省→市→校）" value="level" />
                        <el-option label="按获奖时间（新→旧）" value="time" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">检索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                    <el-button type="success" :loading="exporting" @click="exportExcel">导出 Excel</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 列表 -->
        <el-card class="list-card">
            <el-table :data="tableData" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" class-name="hide-sm" />
                <el-table-column prop="title" label="奖状名称" />
                <el-table-column prop="recipient" label="获奖人员" class-name="hide-sm" />
                <el-table-column prop="eventName" label="赛事" class-name="hide-sm" />
                <el-table-column prop="awardLevel" label="级别" />
                <el-table-column prop="awardDate" label="获奖时间" width="120" class-name="hide-sm" />
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag v-if="row.status === 0" type="success">正常</el-tag>
                        <el-tag v-else-if="row.status === 1" type="warning">待审核删除</el-tag>
                        <el-tag v-else-if="row.status === 2" type="info">已隐藏</el-tag>
                        <el-tag v-else type="info">未知</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" :width="isMobile ? 160 : 360" fixed="right">
                    <template #default="{ row }">
                        <div class="op-cell">
                            <el-button size="small" :icon="isMobile ? View : undefined" @click="viewDetail(row)">
                                {{ isMobile ? '' : '查看' }}
                            </el-button>
                            <el-button size="small" type="primary" :icon="isMobile ? Edit : undefined"
                                @click="editRow(row)">
                                {{ isMobile ? '' : '修改' }}
                            </el-button>
                            <el-button size="small" type="danger" :icon="isMobile ? Delete : undefined"
                                @click="deleteRow(row)">
                                {{ isMobile ? '' : '删除' }}
                            </el-button>
                            <el-button size="small" type="warning" :icon="isMobile ? Top : undefined" @click="topRow(row)">
                                {{ isMobile ? (row.isPinned ? '取消' : '置顶') : (row.isPinned ? '取消置顶' : '置顶') }}
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
                layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />
        </el-card>

        <!-- 查看弹窗（优化：展示图片，样式更规范） -->
        <el-dialog v-model="viewDialogVisible" title="奖状详情" width="600px">
            <el-descriptions :column="1" border v-if="currentDetail">
                <el-descriptions-item label="奖状名称">{{ currentDetail.title }}</el-descriptions-item>
                <el-descriptions-item label="获奖人员">{{ currentDetail.recipient }}</el-descriptions-item>
                <el-descriptions-item label="获奖赛事">{{ currentDetail.eventName }}</el-descriptions-item>
                <el-descriptions-item label="获奖级别">{{ currentDetail.awardLevel }}</el-descriptions-item>
                <el-descriptions-item label="获奖项目">{{ currentDetail.projectName || '无' }}</el-descriptions-item>
                <el-descriptions-item label="获奖时间">{{ currentDetail.awardDate }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ currentDetail.status }}</el-descriptions-item>
                <el-descriptions-item label="证书图片" v-if="currentDetail.imageUrl">
                    <el-image :src="currentDetail.imageUrl" style="max-width: 100%; max-height: 300px" fit="contain" />
                </el-descriptions-item>
            </el-descriptions>
        </el-dialog>

        <!-- 删除理由弹窗 -->
        <el-dialog v-model="deleteDialogVisible" title="删除奖状" width="500px">
            <el-form ref="deleteFormRef" :model="deleteForm" :rules="deleteRules" label-width="80px">
                <el-form-item label="删除理由" prop="reason">
                    <el-input v-model="deleteForm.reason" type="textarea" rows="4" placeholder="请简要说明删除原因（必填）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="deleteDialogVisible = false">取消</el-button>
                <el-button type="danger" @click="confirmDelete" :loading="deleteLoading">提交</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="editDialogVisible" title="修改奖状" width="600px">
            <el-form :model="editForm" label-width="100px">
                <el-form-item label="奖状名称">
                    <el-input v-model="editForm.title" />
                </el-form-item>
                <el-form-item label="获奖人员">
                    <el-input v-model="editForm.recipient" />
                </el-form-item>
                <el-form-item label="获奖赛事">
                    <el-input v-model="editForm.eventName" />
                </el-form-item>
                <el-form-item label="获奖级别">
                    <el-select v-model="editForm.awardLevel">
                        <el-option label="国家级" value="国家级" />
                        <el-option label="省级" value="省级" />
                        <el-option label="市级" value="市级" />
                        <el-option label="校级" value="校级" />
                        <el-option label="其他" value="其他" />
                    </el-select>
                </el-form-item>
                <el-form-item label="获奖项目">
                    <el-input v-model="editForm.projectName" />
                </el-form-item>
                <el-form-item label="获奖时间">
                    <el-date-picker v-model="editForm.awardDate" type="date" value-format="YYYY-MM-DD" />
                </el-form-item>
                <el-form-item label="当前图片">
                    <el-image v-if="editForm.imageUrl" :src="editForm.imageUrl"
                        style="max-width: 160px; max-height: 160px" fit="contain" />
                    <span v-else class="no-image-tip">暂无图片（Excel 导入的奖状可在此补传）</span>
                </el-form-item>
                <el-form-item label="更换图片">
                    <div>
                        <input type="file" accept="image/*" @change="handleEditImageChange" />
                        <div v-if="editImagePreview" class="new-image-preview">
                            <el-image :src="editImagePreview" style="max-width: 160px; max-height: 160px"
                                fit="contain" />
                            <span class="preview-tip">已选择新图片，保存后生效</span>
                        </div>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="editDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmEdit" :loading="editLoading">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { View, Edit, Delete, Top } from '@element-plus/icons-vue'
import axios from 'axios'
import type { ApiResponse, Certificate, PageResult } from '../api/api'
import { loadUserInfo } from '../utils/storage'
import { useIsMobile } from '../utils/responsive'

const route = useRoute()
const { isMobile } = useIsMobile()

// 当前用户角色：决定删除行为（超管直接删除，普通管理员提交申请）
const isSuperAdmin = loadUserInfo()?.role === 'SUPER_ADMIN'

// ---------- 数据 ----------
const tableData = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const searchForm = reactive({
    keyword: '',
    level: '',
    status: '',
    // 排序方式：level/time；主页「等级前三」「最新荣誉」标题跳转时经 URL query 带入
    sortBy: (route.query.sortBy as string) || ''
})

// 查看
const viewDialogVisible = ref(false)
const currentDetail = ref<any>(null)

const deleteDialogVisible = ref(false)
const deleteForm = reactive({ reason: '' })
const deleteFormRef = ref<FormInstance>() // 已绑定模板
const deleteLoading = ref(false)
const deleteRules: FormRules = {
    reason: [{ required: true, message: '请输入删除理由', trigger: 'blur' }]
}
let deleteRowId: number | null = null

const editDialogVisible = ref(false)
const editForm = reactive({
    id: 0,
    title: '',
    recipient: '',
    eventName: '',
    awardLevel: '',
    projectName: '',
    awardDate: '',
    imageUrl: ''
})
const editLoading = ref(false)

// 更换图片：新图单独走 /update-image 接口，不混入文字更新
const editImageFile = ref<File | null>(null)
const editImagePreview = ref('')

const handleEditImageChange = (e: Event) => {
    const target = e.target as HTMLInputElement
    editImageFile.value = target.files?.[0] || null
    if (editImagePreview.value) URL.revokeObjectURL(editImagePreview.value)
    editImagePreview.value = editImageFile.value ? URL.createObjectURL(editImageFile.value) : ''
}

// ---------- 方法 ----------
const fetchData = async () => {
    loading.value = true
    try {
        const params = {
            current: page.current,
            size: page.size,
            keyword: searchForm.keyword || undefined,
            level: searchForm.level || undefined,
            status: searchForm.status || undefined,
            sortBy: searchForm.sortBy || undefined
        };

        const res = await axios.post<ApiResponse<PageResult<Certificate>>>('/api/certificate/view', params);

        if (res.data.code === 200) {
            const pageResult = res.data.data;
            tableData.value = pageResult.records;
            page.total = pageResult.total;
        }
        // 失败分支已由全局拦截器统一弹错并 reject
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        loading.value = false
    }
}

const handleSearch = () => {
    page.current = 1
    fetchData()
}

const resetSearch = () => {
    searchForm.keyword = ''
    searchForm.level = ''
    searchForm.status = ''
    searchForm.sortBy = ''
    handleSearch()
}

// ---------- 导出 Excel ----------
// responseType: 'blob' 时响应拦截器直接放行（Blob 无 code 字段），错误分支由全局拦截器兜底
const exporting = ref(false)

const exportExcel = async () => {
    exporting.value = true
    try {
        const res = await axios.get('/api/certificate/export', { responseType: 'blob' })
        const url = URL.createObjectURL(res.data as Blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `奖状导出_${new Date().toISOString().slice(0, 10)}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        ElMessage.success('导出成功')
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        exporting.value = false
    }
}

// 页面被 keepAlive 缓存后再次从主页跳入：URL query 变化时同步排序并刷新
watch(() => route.query.sortBy, (v) => {
    const next = (v as string) || ''
    if (next === searchForm.sortBy) return
    searchForm.sortBy = next
    page.current = 1
    fetchData()
})

// 查看
const viewDetail = (row: any) => {
    currentDetail.value = row
    viewDialogVisible.value = true
}

const editRow = (row: any) => {
    editForm.id = row.id || 0
    editForm.title = row.title || ''
    editForm.recipient = row.recipient || ''
    editForm.eventName = row.eventName || ''
    editForm.awardLevel = row.awardLevel || ''
    editForm.projectName = row.projectName || '';
    editForm.awardDate = row.awardDate || ''
    editForm.imageUrl = row.imageUrl || ''
    // 清空上一次的换图选择
    editImageFile.value = null
    if (editImagePreview.value) URL.revokeObjectURL(editImagePreview.value)
    editImagePreview.value = ''
    editDialogVisible.value = true
}

const confirmEdit = async () => {
    editLoading.value = true
    try {
        // 先换图（选了新图时），成功后再保存文字信息
        if (editImageFile.value) {
            const fd = new FormData()
            fd.append('id', String(editForm.id))
            fd.append('file', editImageFile.value)
            const imgRes = await axios.put<ApiResponse<string>>('/api/certificate/update-image', fd)
            if (imgRes.data.code !== 200) return
        }
        // 文字更新不携带 imageUrl（专用 DTO 无此字段）
        const { imageUrl, ...textForm } = editForm
        const res = await axios.put<ApiResponse<String>>('/api/certificate/update', textForm);

        if (res.data.code == 200) {
            ElMessage.success('修改成功');
            editDialogVisible.value = false;
            fetchData();
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        editLoading.value = false
    }
}

const deleteRow = (row: any) => {
    deleteRowId = row.id
    deleteForm.reason = ''
    deleteDialogVisible.value = true
}

const confirmDelete = async () => {
    if (!deleteFormRef.value) return
    try {
        await deleteFormRef.value.validate()
        if (deleteRowId === null) return

        deleteLoading.value = true

        const token = localStorage.getItem('token');
        if (!token) {
            ElMessage.warning('请先登录');
            deleteLoading.value = false;
            return;
        }

        const res = await axios.post<ApiResponse<string>>(
            '/api/certificate/delete-request',
            {
                certificateId: deleteRowId,
                reason: deleteForm.reason
            },
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        );

        if (res.data.code == 200) {
            ElMessage.success(isSuperAdmin ? '已直接删除该奖状' : '已提交删除申请，等待审核');
            deleteDialogVisible.value = false;
            fetchData();
        }
    } catch {
        // 失败分支已由全局拦截器统一弹错；表单校验失败也无需额外提示
    } finally {
        deleteLoading.value = false
    }
}

// 置顶 / 取消置顶
const topRow = (row: any) => {
    const target = !row.isPinned
    const action = target ? '置顶' : '取消置顶'
    ElMessageBox.confirm(`确认${action}奖状“${row.title}”吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            const res = await axios.put<ApiResponse<string>>('/api/certificate/pin', {
                id: row.id,
                isPinned: target
            });
            if (res.data.code === 200) {
                ElMessage.success(`${action}成功`)
                fetchData()
            }
        } catch {
            // 错误提示由全局拦截器统一处理
        }
    }).catch(() => { })
}

onMounted(() => {
    fetchData()
})
</script>

<style scoped>
.browse-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.search-card,
.list-card {
    width: 100%;
}

.no-image-tip {
    color: #909399;
    font-size: 13px;
}

.new-image-preview {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 10px;
}

.preview-tip {
    color: #67c23a;
    font-size: 12px;
}

/* 操作列按钮容器：flex 均匀排布，换行也对齐（el-button 相邻默认 margin-left 会错位，重置后用 gap） */
.op-cell {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.op-cell .el-button+.el-button {
    margin-left: 0;
}

/* ===== 移动端适配（≤768px）===== */
@media (max-width: 768px) {
    /* 检索表单：每个字段独占一行，控件全宽 */
    .search-card :deep(.el-form--inline .el-form-item) {
        width: 100%;
        margin-right: 0;
        margin-bottom: 12px;
    }

    .search-card :deep(.el-form-item__content) {
        flex: 1;
    }

    .search-card :deep(.el-form-item .el-input),
    .search-card :deep(.el-form-item .el-select) {
        width: 100% !important;
    }

    /* 手机隐藏次要列（class-name="hide-sm"）：只留 名称/级别/状态/操作 */
    .list-card :deep(th.hide-sm),
    .list-card :deep(td.hide-sm) {
        display: none;
    }
}
</style>