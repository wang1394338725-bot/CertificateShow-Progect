<!-- UploadView.vue -->
<template>
    <div class="upload-page">
        <el-card class="upload-card">
            <template #header>
                <span>上传新记录</span>
            </template>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="upload-form">
                <el-form-item label="记录名称" prop="title">
                    <el-input v-model="form.title" placeholder="给这条记录起个名字" />
                </el-form-item>
                <el-form-item label="记录日期" prop="awardDate">
                    <el-date-picker v-model="form.awardDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
                </el-form-item>
                <el-form-item label="想法（选填）" prop="organization">
                    <el-input v-model="form.organization" type="textarea" rows="3" placeholder="记录此刻的想法或吐槽…" />
                </el-form-item>
                <el-form-item label="记录图片" prop="image">
                    <input type="file" accept="image/*" @change="handleImageChange" ref="fileInput"
                        class="hidden-file-input" />
                    <div>
                        <el-button type="primary" plain :icon="Picture" @click="fileInput?.click()">选择图片</el-button>
                        <div class="tip">点击后在手机上可拍照或从相册选择；支持 JPG/PNG，单张不超过 5MB，图片保存在服务器本地</div>
                    </div>
                    <div v-if="imagePreview" class="image-preview">
                        <img :src="imagePreview" alt="记录预览" style="max-width: 200px; max-height: 200px;" />
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="submitForm" :loading="submitting">提交</el-button>
                    <el-button @click="resetForm">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 后端接口保留，前端暂不展示（showImportCard 恒为 false；不用字面量 v-if="false" 是因为它会触发 vue-tsc 对模板类型收窄的缺陷） -->
        <el-card v-if="showImportCard" class="upload-card import-card">
            <template #header>
                <span>Excel 批量导入</span>
            </template>
            <div class="import-tip">
                表头需包含：奖状名称（必需）、获奖成员、赛事名称、奖状等级、所属项目、获奖日期；列顺序不限，支持常见写法。
                仅支持 .xlsx / .xls，不含图片；导入后在「奖状浏览」页点「修改」即可补传图片。
            </div>
            <div class="import-actions">
                <input type="file" accept=".xlsx,.xls" @change="handleExcelChange" />
                <el-button type="success" :loading="importing" :disabled="!excelFile" @click="submitImport">
                    开始导入
                </el-button>
            </div>
            <template v-if="importResult">
                <el-alert :type="importResult.failCount > 0 ? 'warning' : 'success'" :closable="false"
                    class="import-alert"
                    :title="`共 ${importResult.total} 条：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`" />
                <div v-if="importResult.failures.length" class="failure-list">
                    <div v-for="(f, i) in importResult.failures" :key="i" class="failure-item">{{ f }}</div>
                </div>
            </template>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import axios from 'axios'
import type { ApiResponse } from '../api/api'

// 表单数据
const form = reactive({
    title: '',
    organization: '',
    awardDate: '',
    image: null as File | null
})

const imagePreview = ref<string>('')
const fileInput = ref<HTMLInputElement>()

// 表单校验规则（仅记录名称与记录日期）
const rules: FormRules = {
    title: [{ required: true, message: '请输入记录名称', trigger: 'blur' }],
    awardDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }]
}

const formRef = ref<FormInstance>()
const submitting = ref(false)

// 图片选择
const handleImageChange = (e: Event) => {
    const target = e.target as HTMLInputElement
    const file = target.files?.[0]

    if (file) {
        // 限制图片大小（最大 5MB = 5 * 1024 * 1024 字节）
        const MAX_SIZE = 5 * 1024 * 1024 // 5MB
        if (file.size > MAX_SIZE) {
            ElMessage.warning('图片大小不能超过 5MB,请重新选择')
            // 清空文件框，防止残留
            target.value = ''
            form.image = null
            imagePreview.value = ''
            return
        }

        form.image = file
        // 生成预览 Base64
        const reader = new FileReader()
        reader.onload = (ev) => {
            imagePreview.value = ev.target?.result as string
        }
        reader.readAsDataURL(file)
    }
}

// 提交
const submitForm = async () => {
    if (!formRef.value) return
    await formRef.value.validate()
    if (!form.image) {
        ElMessage.warning('请选择记录图片')
        return
    }
    submitting.value = true
    try {
        // 构建 FormData 对象
        const formData = new FormData()
        formData.append('file', form.image)          // 对应后端的 @RequestParam("file")
        formData.append('title', form.title)
        formData.append('organization', form.organization)
        // 以下字段后端仍接收：人员/赛事/项目前端不再收集，固定传空；级别固定「其他」
        formData.append('recipient', '')
        formData.append('eventName', '')
        formData.append('awardLevel', '其他')
        formData.append('projectName', '')
        formData.append('awardDate', form.awardDate) // 格式为 YYYY-MM-DD，后端可直接解析为 LocalDate

        const res = await axios.post<ApiResponse<String>>('/api/certificate/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });

        if (res.data.code === 200) {
            ElMessage.success('记录上传成功')
            resetForm()
        }
    } catch {
        // 错误提示由全局拦截器统一处理（如图片超过 20MB）
    } finally {
        submitting.value = false
    }
}

// 重置
const resetForm = () => {
    formRef.value?.resetFields()
    // 重置表单数据
    form.title = ''
    form.organization = ''
    form.awardDate = ''
    form.image = null // 清空文件对象
    imagePreview.value = ''
    if (fileInput.value) {
        fileInput.value.value = '' // 清空 input[type=file] 的 DOM 值
    }
}

// ---------- Excel 批量导入 ----------
// 后端 /api/certificate/import 接口保留，前端暂不展示该卡片
const showImportCard = false

interface ImportResult {
    total: number
    successCount: number
    failCount: number
    failures: string[]
}

const excelFile = ref<File | null>(null)
const importing = ref(false)
const importResult = ref<ImportResult | null>(null)

const handleExcelChange = (e: Event) => {
    const target = e.target as HTMLInputElement
    excelFile.value = target.files?.[0] || null
    importResult.value = null
}

const submitImport = async () => {
    if (!excelFile.value) return
    importing.value = true
    try {
        const fd = new FormData()
        fd.append('file', excelFile.value)
        const res = await axios.post<ApiResponse<ImportResult>>('/api/certificate/import', fd, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.data.code === 200) {
            importResult.value = res.data.data
        }
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally {
        importing.value = false
    }
}
</script>

<style scoped>
.upload-page {
    display: flex;
    flex-direction: column;
}

.upload-card {
    max-width: 800px;
    margin: 0 auto;
}

.upload-form {
    margin-top: 10px;
}

.image-preview {
    margin-top: 10px;
}

.tip {
    color: #909399;
    font-size: 12px;
    margin-top: 5px;
}

.import-card {
    margin-top: 20px;
}

.import-tip {
    font-size: 13px;
    color: #909399;
    line-height: 1.6;
    margin-bottom: 12px;
}

.import-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.import-alert {
    margin-top: 14px;
}

.failure-list {
    margin-top: 10px;
    max-height: 200px;
    overflow-y: auto;
    font-size: 13px;
    color: #f56c6c;
}

.failure-item {
    padding: 4px 0;
    border-bottom: 1px dashed #ebeef5;
}

.hidden-file-input {
    display: none; /* 隐藏原生 file input，由"选择图片"按钮触发；手机浏览器 accept=image/* 自动弹出拍照/相册 */
}

/* ===== 移动端适配（≤768px）===== */
@media (max-width: 768px) {
    /* 表单 label 从 120px 收窄，给输入控件让出宽度 */
    .upload-form :deep(.el-form-item__label) {
        width: 96px !important;
    }

    .import-actions {
        flex-wrap: wrap;
    }
}
</style>
