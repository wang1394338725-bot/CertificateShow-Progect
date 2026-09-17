<!-- MessagesView.vue - 消息栏：以 delete_audit 为消息源，超管可审核，全员可见广播消息 -->
<template>
    <el-card class="messages-card">
        <template #header>
            <div class="card-head">
                <span>消息栏</span>
                <el-button v-if="isSuperAdmin" size="small" type="danger" plain :icon="Delete"
                    @click="clearProcessed">
                    清除已处理
                </el-button>
            </div>
        </template>

        <el-table :data="tableData" style="width: 100%" v-loading="loading" @row-click="showDetail">
            <el-table-column prop="certificateTitle" label="奖状名称" show-overflow-tooltip />
            <el-table-column prop="requesterName" label="提交人" width="90" />
            <el-table-column prop="reason" label="删除理由" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }">
                    <el-tag v-if="row.status === 0" type="warning">待审核</el-tag>
                    <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
                    <el-tag v-else-if="row.status === 3" type="info">直接删除</el-tag>
                    <el-tag v-else type="danger">已驳回</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="approverName" label="审核人" width="110" class-name="hide-sm">
                <template #default="{ row }">{{ row.approverName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="rejectReason" label="驳回原因" show-overflow-tooltip class-name="hide-sm">
                <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="170" class-name="hide-sm" />

            <!-- 操作列：仅超管可审核待审核记录；手机端图标化 -->
            <el-table-column v-if="isSuperAdmin" label="操作" :width="isMobile ? 130 : 200" fixed="right">
                <template #default="{ row }">
                    <div class="op-cell">
                        <template v-if="row.status === 0">
                            <el-button size="small" type="success" :icon="isMobile ? CircleCheck : undefined"
                                @click.stop="approveDelete(row)">
                                {{ isMobile ? '' : '审核通过' }}
                            </el-button>
                            <el-button size="small" type="danger" :icon="isMobile ? Close : undefined"
                                @click.stop="rejectDelete(row)">
                                {{ isMobile ? '' : '驳回' }}
                            </el-button>
                        </template>
                        <!-- 已处理（含超管直删）的消息可删除，保持消息栏整洁 -->
                        <el-button v-else size="small" type="info" plain :icon="Delete"
                            @click.stop="removeAudit(row)">
                            {{ isMobile ? '' : '删除' }}
                        </el-button>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
            layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />

        <!-- ====== 消息详情弹窗（点击行查看全部字段，手机端隐藏列的信息在这里补齐）====== -->
        <el-dialog v-model="detailVisible" title="消息详情" width="min(560px, 94vw)">
            <el-descriptions :column="1" border v-if="detailRow">
                <el-descriptions-item label="奖状名称">{{ detailRow.certificateTitle }}</el-descriptions-item>
                <el-descriptions-item label="提交人">{{ detailRow.requesterName }}</el-descriptions-item>
                <el-descriptions-item label="删除理由">{{ detailRow.reason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                    <el-tag v-if="detailRow.status === 0" type="warning">待审核</el-tag>
                    <el-tag v-else-if="detailRow.status === 1" type="success">已通过</el-tag>
                    <el-tag v-else-if="detailRow.status === 3" type="info">直接删除</el-tag>
                    <el-tag v-else type="danger">已驳回</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="审核人">{{ detailRow.approverName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="驳回原因">{{ detailRow.rejectReason || '—' }}</el-descriptions-item>
                <el-descriptions-item label="申请时间">{{ detailRow.createTime }}</el-descriptions-item>
            </el-descriptions>
        </el-dialog>

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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CircleCheck, Close, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { loadUserInfo } from '../utils/storage'
import { useIsMobile } from '../utils/responsive'

// ---------- 用户状态 ----------
const userInfo = loadUserInfo()
const isSuperAdmin = computed(() => userInfo?.role === 'SUPER_ADMIN')
const { isMobile } = useIsMobile()

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

// ---------- 消息详情（点击行查看全部字段）----------
const detailVisible = ref(false)
const detailRow = ref<any>(null)

const showDetail = (row: any) => {
    detailRow.value = row
    detailVisible.value = true
}

// ---------- 删除已处理消息（仅超管）----------
const removeAudit = (row: any) => {
    ElMessageBox.confirm(`确认删除「${row.certificateTitle}」的这条消息记录吗？`, '删除消息', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await axios.delete(`/api/certificate/audit/${row.id}`, authHeaders())
        ElMessage.success('已删除该消息')
        fetchData()
    }).catch(() => { })
}

// 一键清除全部已处理消息（status 1/2/3），待审核的不受影响
const clearProcessed = () => {
    ElMessageBox.confirm('确认清除全部已处理（已通过/已驳回/直接删除）的消息吗？待审核消息不受影响。', '清除已处理', {
        confirmButtonText: '清除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        const res = await axios.delete('/api/certificate/audit-processed', authHeaders())
        if (res.data.code === 200) {
            ElMessage.success(`已清除 ${res.data.data} 条已处理消息`)
        }
        fetchData()
    }).catch(() => { })
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

/* 卡片头部：标题左、清除按钮右 */
.card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

/* 操作列按钮容器：flex 排布换行对齐 */
.op-cell {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.op-cell .el-button+.el-button {
    margin-left: 0;
}

/* ===== 移动端适配（≤768px）：隐藏次要列（审核人/驳回原因/时间，详情弹窗可看），只留 名称/提交人/删除理由/状态/操作 ===== */
@media (max-width: 768px) {
    .messages-card :deep(th.hide-sm),
    .messages-card :deep(td.hide-sm) {
        display: none;
    }
}
</style>
