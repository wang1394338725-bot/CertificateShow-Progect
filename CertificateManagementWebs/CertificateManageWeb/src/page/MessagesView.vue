<!-- MessagesView.vue - 消息栏：以 delete_audit 为消息源，超管可审核，全员可见广播消息 -->
<template>
    <el-card class="messages-card">
        <template #header>
            <span>消息栏</span>
        </template>

        <el-table :data="tableData" style="width: 100%" v-loading="loading">
            <el-table-column prop="certificateTitle" label="奖状名称" show-overflow-tooltip />
            <el-table-column prop="requesterName" label="提交人" width="110" />
            <el-table-column prop="reason" label="删除理由" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                    <el-tag v-if="row.status === 0" type="warning">待审核</el-tag>
                    <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
                    <el-tag v-else-if="row.status === 3" type="info">直接删除</el-tag>
                    <el-tag v-else type="danger">已驳回</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="approverName" label="审核人" width="110">
                <template #default="{ row }">{{ row.approverName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="rejectReason" label="驳回原因" show-overflow-tooltip>
                <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="170" />

            <!-- 操作列：仅超管可审核待审核记录 -->
            <el-table-column v-if="isSuperAdmin" label="操作" width="200" fixed="right">
                <template #default="{ row }">
                    <template v-if="row.status === 0">
                        <el-button size="small" type="success" @click="approveDelete(row)">审核通过</el-button>
                        <el-button size="small" type="danger" @click="rejectDelete(row)">驳回</el-button>
                    </template>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
            layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />

        <!-- ====== 驳回弹窗 ====== -->
        <el-dialog v-model="rejectDialogVisible" title="驳回删除申请" width="500px">
            <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
                <el-form-item label="驳回原因" prop="reason">
                    <el-input v-model="rejectForm.reason" type="textarea" rows="4" placeholder="请说明驳回原因（必填）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="rejectDialogVisible = false">取消</el-button>
                <el-button type="danger" @click="confirmReject" :loading="rejectLoading">确定驳回</el-button>
            </template>
        </el-dialog>
    </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import axios from 'axios'
import { loadUserInfo } from '../utils/storage'

// ---------- 用户状态 ----------
const userInfo = loadUserInfo()
const isSuperAdmin = computed(() => userInfo?.role === 'SUPER_ADMIN')

// ---------- 列表数据 ----------
const tableData = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

// 统一携带 token
const authHeaders = () => {
    const token = localStorage.getItem('token')
    return { headers: { Authorization: `Bearer ${token}` } }
}

// ---------- 获取数据 ----------
const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.post(
            `/api/certificate/audit-list?current=${page.current}&size=${page.size}`,
            {},
            authHeaders()
        )
        if (res.data.code === 200) {
            tableData.value = res.data.data.records
            page.total = res.data.data.total
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        loading.value = false
    }
}

// ---------- 审核通过 ----------
const approveDelete = async (row: any) => {
    try {
        const res = await axios.post(
            '/api/certificate/audit',
            { auditId: row.id, approved: true },
            authHeaders()
        )
        if (res.data.code === 200) {
            ElMessage.success('审核通过，奖状已删除')
        }
        fetchData()
    } catch {
        // 典型场景：另一个超管已先审核（后端条件更新拦截，拦截器已弹错），刷新同步状态
        fetchData()
    }
}

// ---------- 驳回 ----------
const rejectDialogVisible = ref(false)
const rejectForm = reactive({ reason: '' })
const rejectFormRef = ref<FormInstance>()
const rejectLoading = ref(false)
let rejectRowId: number | null = null

const rejectRules: FormRules = {
    reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }]
}

const rejectDelete = (row: any) => {
    rejectRowId = row.id
    rejectForm.reason = ''
    rejectDialogVisible.value = true
}

const confirmReject = async () => {
    if (!rejectFormRef.value) return
    await rejectFormRef.value.validate()
    if (rejectRowId === null) return

    rejectLoading.value = true
    try {
        const res = await axios.post(
            '/api/certificate/audit',
            { auditId: rejectRowId, approved: false, rejectReason: rejectForm.reason },
            authHeaders()
        )
        if (res.data.code === 200) {
            ElMessage.success('已驳回该删除申请')
            rejectDialogVisible.value = false
        }
        fetchData()
    } catch {
        // 错误提示由全局拦截器统一处理，刷新同步状态
        fetchData()
    } finally {
        rejectLoading.value = false
    }
}

// ---------- 生命周期 ----------
onMounted(() => {
    fetchData()
})
</script>

<style scoped>
.messages-card {
    width: 100%;
}
</style>
