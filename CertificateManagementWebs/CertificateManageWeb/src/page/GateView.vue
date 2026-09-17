<!-- GateView.vue - 访问验证门禁页：答对一题才能进入公开主页，防止爬虫与陌生人直接浏览 -->
<template>
    <div class="gate-container">
        <el-card class="gate-card">
            <template #header>
                <div class="card-header">
                    <h2>访问验证</h2>
                    <span>请回答该网站创作者的微信/QQ昵称</span>
                </div>
            </template>

            <el-input v-model="answerInput" placeholder="请输入答案" size="large" :disabled="loading"
                @keyup.enter="handleVerify" />

            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleVerify">
                提 交
            </el-button>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

// ---------- 表单状态 ----------
const answerInput = ref('')
const loading = ref(false)

// 独立实例：绕开全局拦截器（/gate/check 未通过返回 code 403 + gate 标记，
// 若走全局拦截器会弹错误提示并触发跳转，在门禁页自身形成回环）
const http = axios.create()

// ---------- 已通过者直接放行 ----------
onMounted(async () => {
    try {
        const res = await http.get('/api/certificate/gate/check')
        if (res.data.code === 200) {
            localStorage.setItem('gatePassed', '1')
            router.replace('/home')
        }
    } catch {
        // 未通过：停留本页等待作答
    }
})

// ---------- 提交答案 ----------
const handleVerify = async () => {
    if (loading.value) return
    if (!answerInput.value.trim()) {
        ElMessage.warning('请输入答案')
        return
    }
    loading.value = true
    try {
        const res = await http.post('/api/certificate/gate/verify', { answer: answerInput.value })
        if (res.data.code === 200) {
            localStorage.setItem('gatePassed', '1')
            router.replace('/home')
        } else {
            // 失败提示（错误次数 / 封禁倒计时）由后端 message 给出
            ElMessage.error(String(res.data.message || '回答错误'))
        }
    } catch {
        ElMessage.error('网络异常，请稍后重试')
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
/* 居中卡片，视觉风格与 Login.vue 一致 */
.gate-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #e6f2ff;
    margin: 0;
}

.gate-card {
    width: 400px;
    max-width: 92vw;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
    text-align: center;
}

.card-header h2 {
    margin: 0;
    font-size: 24px;
    color: #303133;
}

.card-header span {
    font-size: 14px;
    color: #909399;
    display: block;
    margin-top: 4px;
}

.submit-btn {
    width: 100%;
    margin-top: 16px;
}
</style>
