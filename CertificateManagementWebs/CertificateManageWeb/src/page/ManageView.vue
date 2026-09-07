<!-- ManageView.vue：超级管理员对普通管理员的管理（列表/搜索/新增/编辑/重置密码/删除） -->
<template>
    <div class="manage-container">
        <!-- 操作栏 -->
        <el-card class="toolbar-card">
            <el-row :gutter="20" align="middle">
                <el-col :span="16">
                    <el-input v-model="searchKeyword" placeholder="搜索管理员用户名/真实姓名" clearable
                        style="width: 300px; margin-right: 10px" @keyup.enter="handleSearch" />
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                </el-col>
                <el-col :span="8" style="text-align: right">
                    <el-button type="success" @click="openAddDialog">新增管理员</el-button>
                </el-col>
            </el-row>
        </el-card>

        <!-- 列表 -->
        <el-card class="list-card">
            <el-table :data="tableData" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="username" label="用户名" />
                <el-table-column prop="realName" label="真实姓名" />
                <el-table-column prop="role" label="角色" width="120">
                    <template #default="{ row }">
                        <el-tag v-if="row.role === 'SUPER_ADMIN'" type="danger">超级管理员</el-tag>
                        <el-tag v-else-if="row.role === 'ADMIN'" type="primary">普通管理员</el-tag>
                        <el-tag v-else type="info">未知</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="创建时间" width="180">
                    <template #default="{ row }">
                        {{ formatTime(row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="260" fixed="right">
                    <template #default="{ row }">
                        <!-- 超级管理员账号不可被管理 -->
                        <span v-if="row.role === 'SUPER_ADMIN'" class="sys-account">系统账号</span>
                        <template v-else>
                            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
                            <el-button size="small" type="warning" @click="openResetDialog(row)">重置密码</el-button>
                            <el-button size="small" type="danger" plain @click="deleteAdmin(row)">删除</el-button>
                        </template>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
                layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />
        </el-card>

        <!-- 新增弹窗（角色固定为普通管理员，由后端强制） -->
        <el-dialog v-model="addDialogVisible" title="新增管理员" width="500px" @close="addFormRef?.resetFields()">
            <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="addForm.username" placeholder="请输入用户名（3-20位）" />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="addForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="初始密码" prop="password">
                    <el-input v-model="addForm.password" placeholder="请输入初始密码（6-20位）" show-password />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitAdd" :loading="submitLoading">确定</el-button>
            </template>
        </el-dialog>

        <!-- 编辑弹窗（仅真实姓名） -->
        <el-dialog v-model="editDialogVisible" title="编辑管理员" width="500px" @close="editFormRef?.resetFields()">
            <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
                <el-form-item label="用户名">
                    <el-input :model-value="editForm.username" disabled />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="editDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitEdit" :loading="submitLoading">确定</el-button>
            </template>
        </el-dialog>

        <!-- 重置密码弹窗 -->
        <el-dialog v-model="resetDialogVisible" title="重置密码" width="400px" @close="resetFormRef?.resetFields()">
            <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="100px">
                <el-form-item label="新密码" prop="password">
                    <el-input v-model="resetForm.password" placeholder="请输入新密码（6-20位）" show-password />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="resetDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitReset" :loading="submitLoading">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

interface AdminRow {
    id: number
    username: string
    realName: string
    role: string
    createTime: string
}

// ---------- 列表与搜索 ----------
const tableData = ref<AdminRow[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const searchKeyword = ref('')

const formatTime = (t?: string) => (t ? t.replace('T', ' ') : '-')

const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.post('/api/certificate/admin-list', {
            current: page.current,
            size: page.size,
            keyword: searchKeyword.value || undefined
        })
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

const handleSearch = () => {
    page.current = 1
    fetchData()
}

const resetSearch = () => {
    searchKeyword.value = ''
    handleSearch()
}

// ---------- 新增 ----------
const addDialogVisible = ref(false)
const addFormRef = ref<FormInstance>()
const submitLoading = ref(false)
const addForm = reactive({ username: '', realName: '', password: '' })

const addRules: FormRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    password: [
        { required: true, message: '请输入初始密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ]
}

const openAddDialog = () => {
    addForm.username = ''
    addForm.realName = ''
    addForm.password = ''
    addDialogVisible.value = true
}

const submitAdd = async () => {
    if (!addFormRef.value) return
    await addFormRef.value.validate()
    submitLoading.value = true
    try {
        const res = await axios.post('/api/certificate/admin/add', { ...addForm })
        if (res.data.code === 200) {
            ElMessage.success('新增管理员成功')
            addDialogVisible.value = false
            fetchData()
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        submitLoading.value = false
    }
}

// ---------- 编辑（真实姓名） ----------
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({ id: 0, username: '', realName: '' })

const editRules: FormRules = {
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

const openEditDialog = (row: AdminRow) => {
    editForm.id = row.id
    editForm.username = row.username
    editForm.realName = row.realName
    editDialogVisible.value = true
}

const submitEdit = async () => {
    if (!editFormRef.value) return
    await editFormRef.value.validate()
    submitLoading.value = true
    try {
        const res = await axios.put('/api/certificate/admin/update', {
            id: editForm.id,
            realName: editForm.realName
        })
        if (res.data.code === 200) {
            ElMessage.success('更新成功')
            editDialogVisible.value = false
            fetchData()
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        submitLoading.value = false
    }
}

// ---------- 重置密码 ----------
const resetDialogVisible = ref(false)
const resetFormRef = ref<FormInstance>()
const resetForm = reactive({ id: 0, username: '', password: '' })

const resetRules: FormRules = {
    password: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ]
}

const openResetDialog = (row: AdminRow) => {
    resetForm.id = row.id
    resetForm.username = row.username
    resetForm.password = ''
    resetDialogVisible.value = true
}

const submitReset = async () => {
    if (!resetFormRef.value) return
    await resetFormRef.value.validate()
    submitLoading.value = true
    try {
        const res = await axios.put('/api/certificate/admin/update', {
            id: resetForm.id,
            password: resetForm.password
        })
        if (res.data.code === 200) {
            ElMessage.success(`已重置「${resetForm.username}」的密码`)
            resetDialogVisible.value = false
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        submitLoading.value = false
    }
}

// ---------- 删除 ----------
const deleteAdmin = (row: AdminRow) => {
    ElMessageBox.confirm(
        `确认删除管理员「${row.username}」吗？此操作不可恢复！`,
        '警告',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    ).then(async () => {
        try {
            const res = await axios.post('/api/certificate/admin/delete', { id: row.id })
            if (res.data.code === 200) {
                ElMessage.success('删除成功')
                // 当前页删空后回退一页
                if (tableData.value.length === 1 && page.current > 1) page.current--
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
.manage-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.toolbar-card,
.list-card {
    width: 100%;
}

.sys-account {
    color: #909399;
    font-size: 12px;
}
</style>
