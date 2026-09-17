<!-- UploadView.vue -->
<template>
    <div class="upload-page">
        <el-card class="upload-card">
            <template #header>
                <span>上传新奖状</span>
            </template>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="upload-form">
                <el-form-item label="奖状名称" prop="title">
                    <el-input v-model="form.title" placeholder="请输入奖状名称" />
                </el-form-item>
                <el-form-item label="获奖人员" prop="recipient">
                    <el-input v-model="form.recipient" placeholder="请输入获奖人员（多人用逗号分隔）" />
                </el-form-item>
                <el-form-item label="获奖赛事" prop="eventName">
                    <el-input v-model="form.eventName" placeholder="请输入获奖赛事" />
                </el-form-item>
                <el-form-item label="获奖级别" prop="awardLevel">
                    <el-select v-model="form.awardLevel" placeholder="请选择获奖级别">
                        <el-option label="国家级" value="国家级" />
                        <el-option label="省级" value="省级" />
                        <el-option label="市级" value="市级" />
                        <el-option label="校级" value="校级" />
                        <el-option label="其他" value="其他" />
                    </el-select>
                </el-form-item>
                <el-form-item label="获奖项目（可选）" prop="projectName">
                    <el-input v-model="form.projectName" placeholder="请输入获奖项目（可选）" />
                </el-form-item>
                <el-form-item label="获奖时间" prop="awardDate">
                    <el-date-picker v-model="form.awardDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
                </el-form-item>
                <el-form-item label="奖状图片" prop="image">
                    <input type="file" accept="image/*" @change="handleImageChange" ref="fileInput"
                        class="hidden-file-input" />
                    <div>
                        <el-button type="primary" plain :icon="Picture" @click="fileInput?.click()">选择图片</el-button>
                        <div class="tip">点击后在手机上可拍照或从相册选择；支持 JPG/PNG，单张不超过 5MB，图片保存在服务器本地</div>
                    </div>
                    <div v-if="imagePreview" class="image-preview">
                        <img :src="imagePreview" alt="奖状预览" style="max-width: 200px; max-height: 200px;" />
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="submitForm" :loading="submitting">提交</el-button>
                    <el-button @click="resetForm">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- Excel 批量导入 -->
        <el-card class="upload-card import-card">
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
    recipient: '',
    eventName: '',
    awardLevel: '',
    projectName: '',
    awardDate: '',
    image: null as File | null
})

const imagePreview = ref<string>('')
const fileInput = ref<HTMLInputElement>()

// 表单校验规则
const rules: FormRules = {
    title: [{ required: true, message: '请输入奖状名称', trigger: 'blur' }],
    recipient: [{ required: true, message: '请输入获奖人员', trigger: 'blur' }],
    eventName: [{ required: true, message: '请输入获奖赛事', trigger: 'blur' }],
    awardLevel: [{ required: true, message: '请选择获奖级别', trigger: 'change' }],
    awardDate: [{ required: true, message: '请选择获奖时间', trigger: 'change' }]
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
        ElMessage.warning('请选择奖状图片')
        return
    }
    submitting.value = true
    try {
        // 构建 FormData 对象
        const formData = new FormData()
        formData.append('file', form.image)          // 对应后端的 @RequestParam("file")
        formData.append('title', form.title)
        formData.append('recipient', form.recipient)
        formData.append('eventName', form.eventName)
        formData.append('awardLevel', form.awardLevel)
        formData.append('projectName', form.projectName || '')
        formData.append('awardDate', form.awardDate) // 格式为 YYYY-MM-DD，后端可直接解析为 LocalDate

        const res = await axios.post<ApiResponse<String>>('/api/certificate/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });

        if (res.data.code === 200) {
            ElMessage.success('奖状上传成功')
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
    // 重置表单数据（包括可选字段置空）
    form.title = ''
    form.recipient = ''
    form.eventName = ''
    form.awardLevel = ''
    form.projectName = ''
    form.awardDate = ''
    form.image = null // 清空文件对象
    imagePreview.value = ''
    if (fileInput.value) {
        fileInput.value.value = '' // 清空 input[type=file] 的 DOM 值
    }
}

// ---------- Excel 批量导入 ----------
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
