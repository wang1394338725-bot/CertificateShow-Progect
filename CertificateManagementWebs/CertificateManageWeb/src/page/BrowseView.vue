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
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">检索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 列表 -->
        <el-card class="list-card">
            <el-table :data="tableData" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="title" label="奖状名称" />
                <el-table-column prop="recipient" label="获奖人员" />
                <el-table-column prop="eventName" label="赛事" />
                <el-table-column prop="awardLevel" label="级别" />
                <el-table-column prop="awardDate" label="获奖时间" width="120" />
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag v-if="row.status === 0" type="success">正常</el-tag>
                        <el-tag v-else-if="row.status === 1" type="warning">待审核删除</el-tag>
                        <el-tag v-else-if="row.status === 2" type="info">已隐藏</el-tag>
                        <el-tag v-else type="info">未知</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="360" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" @click="viewDetail(row)">查看</el-button>
                        <el-button size="small" type="primary" @click="editRow(row)">修改</el-button>
                        <el-button size="small" type="danger" @click="deleteRow(row)">删除</el-button>
                        <el-button size="small" type="warning" @click="topRow(row)">
                            {{ row.isPinned ? '取消置顶' : '置顶' }}
                        </el-button>
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import axios from 'axios'
import type { ApiResponse, Certificate, PageResult } from '../api/api'
import { loadUserInfo } from '../utils/storage'

// 当前用户角色：决定删除行为（超管直接删除，普通管理员提交申请）
const isSuperAdmin = loadUserInfo()?.role === 'SUPER_ADMIN'

// ---------- 数据 ----------
const tableData = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const searchForm = reactive({
    keyword: '',
    level: '',
    status: ''
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
            status: searchForm.status || undefined
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
    handleSearch()
}

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
</style>