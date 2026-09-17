<!-- Login.vue - 登录页面组件 -->
<template>
    <div class="login-container">
        <el-card class="login-card">
            <template #header>
                <div class="card-header">
                    <h2>奖状管理系统</h2>
                    <span>管理员登录</span>
                </div>
            </template>

            <!-- 登录表单 -->
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0"
                @keyup.enter="handleLoginSubmit">
                <el-form-item prop="username">
                    <el-input v-model="loginForm.username" placeholder="请输入管理员账号" prefix-icon="User" size="large" />
                </el-form-item>

                <el-form-item prop="password">
                    <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                        size="large" show-password />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" size="large" :loading="loading" @click="handleLoginSubmit"
                        style="width: 100%">
                        登 录
                    </el-button>
                </el-form-item>

                <el-form-item>
                    <el-button link type="primary" @click="router.push('/')" class="back-home-btn">
                        ← 返回主页
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { type ApiResponse, type LoginData, type LoginRequest } from '../api/api'
import axios from 'axios'
import { saveAuthData } from '../utils/storage'
import { useUserStore } from '../stores/user.ts'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute();
const router = useRouter();

// ---------- 组件用途 ----------
// 本组件提供管理员登录界面，账号密码由后端手动维护，无需注册。
// 登录逻辑需自行实现（例如调用 /api/auth/login 接口），成功后跳转至首页。

// ---------- 表单数据 ----------
const loginForm = reactive<LoginRequest>({
    username: '',
    password: ''
})

// ---------- 表单引用（用于校验） ----------
const loginFormRef = ref<FormInstance>()

// ---------- 加载状态 ----------
const loading = ref(false)

// ---------- 表单校验规则（仅非空） ----------
const loginRules: FormRules = {
    username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLoginSubmit = async () => {
    if (!loginFormRef.value) return;

    try {
        await loginFormRef.value.validate();

        let redirectPath = route.query.redirect as string;
        if (!redirectPath || !redirectPath.startsWith('/')) {
            redirectPath = '/admin'; // 不合法则默认去后台首页
        }

        loading.value = true;

        const res = await axios.post<ApiResponse<LoginData>>('/api/certificate/login', loginForm);

        // 业务失败（如用户名或密码错误 405）已由全局拦截器统一弹错并 reject，走到这里必然是 200
        const { token, ...userInfo } = res.data.data;

        const userStore = useUserStore();

        userStore.setLoginData(token, userInfo);

        saveAuthData(token, userInfo);

        // 首次登录/被清理过时建立"消息已读基线"=登录时刻：此前的消息不再计红点（退出时该时间戳会被保留）
        if (!localStorage.getItem('auditLastReadTime')) {
            localStorage.setItem('auditLastReadTime', String(Date.now()))
        }

        ElMessage.success('登录成功');

        router.replace(redirectPath);
    } catch {
        // 错误提示由全局拦截器统一处理
    } finally { loading.value = false; }
}
</script>

<style scoped>
/* 浅蓝色背景 */
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #e6f2ff;
    /* 浅蓝色 */
    margin: 0;
}

/* 卡片样式（max-width 钳制保证手机端不溢出） */
.login-card {
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

.back-home-btn {
    /* 返回主页链接在表单内居中 */
    width: 100%;
    text-align: center;
}
</style>