<template>
    <el-container class="admin-container">
        <el-aside width="200px" class="admin-aside">
            <div class="logo">奖状管理</div>
            <el-menu :default-active="activeMenu" router class="menu" background-color="#304156" text-color="#bfcbd9"
                active-text-color="#409EFF">
                <el-menu-item index="/admin/upload">
                    <el-icon>
                        <Upload />
                    </el-icon>
                    <span>上传奖状</span>
                </el-menu-item>
                <el-menu-item index="/admin/browse">
                    <el-icon>
                        <Document />
                    </el-icon>
                    <span>奖状浏览</span>
                </el-menu-item>
                <el-menu-item index="/admin/messages"> <!-- 改为复数，匹配路由 -->
                    <el-icon class="msg-icon">
                        <Message />
                        <span v-if="unreadCount > 0" class="msg-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
                    </el-icon>
                    <span>消息栏</span>
                </el-menu-item>
                <el-menu-item index="/admin/manage" v-if="isSuperAdmin">
                    <el-icon>
                        <User />
                    </el-icon>
                    <span>管理员管理</span>
                </el-menu-item>
            </el-menu>
        </el-aside>

        <el-container class="main-container">
            <el-header class="admin-header">
                <div class="header-left">管理员界面</div>
                <div class="header-right">
                    <el-button :icon="HomeFilled" text bg class="home-btn" @click="router.push('/')">
                        返回主页
                    </el-button>
                    <el-dropdown @command="handleCommand">
                        <span class="admin-name">
                            <span class="name-text">{{ adminName }}</span>
                            <el-icon class="dropdown-icon">
                                <ArrowDown />
                            </el-icon>
                        </span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </el-header>

            <el-main class="admin-main">
                <router-view v-slot="{ Component }">
                    <keep-alive>
                        <component :is="Component" />
                    </keep-alive>
                </router-view>
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { loadUserInfo } from '../utils/storage'
import { Upload, Document, Message, ArrowDown, User, HomeFilled } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userInfo = loadUserInfo()

const activeMenu = computed(() => route.path)
const adminName = computed(() => {
    const info = userInfo;
    return info?.realName || info?.username || '管理员'
})
const unreadCount = ref(0)

// 已读标记：localStorage 记录"最后查看消息栏时间"，未读数 = 该时间之后的新消息（无需消息表）
const AUDIT_READ_KEY = 'auditLastReadTime'

// 未读消息数：进入后台时与每次路由切换后刷新
const fetchUnread = async () => {
    try {
        const since = localStorage.getItem(AUDIT_READ_KEY) || '0'
        const res = await axios.get(`/api/certificate/audit-pending-count?since=${since}`)
        if (res.data.code === 200) unreadCount.value = Number(res.data.data) || 0
    } catch { /* 角标失败静默，不打扰主流程 */ }
}
onMounted(fetchUnread)
watch(() => route.path, (path) => {
    // 进入消息栏即视为已读：记录时间戳，角标立即归零（新消息到达时重新计数）
    if (path === '/admin/messages') {
        localStorage.setItem(AUDIT_READ_KEY, String(Date.now()))
    }
    fetchUnread()
})

const handleCommand = (cmd: string) => {
    if (cmd === 'logout') {
        ElMessageBox.confirm('确认退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => {
            // 退出逻辑
            localStorage.clear()
            window.location.href = '/login'
        }).catch(() => { })
    }
}

const isSuperAdmin = computed(() => userInfo?.role === "SUPER_ADMIN");
</script>

<style scoped>
/* 重置容器，移除所有可能的边距 */
.admin-container {
    display: flex;
    height: 100vh;
    width: 100%;
    overflow: hidden;
    margin: 0;
    padding: 0;
}

.admin-aside {
    background-color: #304156;
    color: #fff;
    flex-shrink: 0;
    height: 100vh;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
}

.logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    font-size: 18px;
    font-weight: bold;
    color: #fff;
    border-bottom: 1px solid #1f2d3d;
    flex-shrink: 0;
}

.menu {
    border-right: none;
    flex: 1;
}

/* 消息红点：挂在菜单图标右上角的圆角徽章 */
.msg-icon {
    position: relative;
}

.msg-badge {
    position: absolute;
    top: -5px;
    right: -9px;
    min-width: 16px;
    height: 16px;
    line-height: 14px;
    padding: 0 5px;
    border-radius: 8px;
    background-color: #f56c6c;
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    text-align: center;
    white-space: nowrap;
    /* 与侧边栏同色描边，避免红点贴着图标显得脏 */
    box-shadow: 0 0 0 2px #304156;
    box-sizing: border-box;
}

.main-container {
    display: flex;
    flex-direction: column;
    flex: 1;
    height: 100vh;
    overflow: hidden;
}

.admin-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fff;
    border-bottom: 1px solid #e6e6e6;
    padding: 0 20px;
    height: 60px;
    flex-shrink: 0;
}

.header-left {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 12px;
}

.admin-name {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    border-radius: 6px;
    cursor: pointer;
    color: #303133;
    font-size: 14px;
    transition: background-color 0.2s;
    outline: none;
    /* 去除默认的黑边 */
    border: none;
    /* 确保无边框 */
    background: transparent;
    /* 背景透明 */
}

.admin-name:hover {
    background-color: #f0f2f5;
    /* 悬停浅灰背景，更柔和 */
}

.dropdown-icon {
    font-size: 16px;
    /* 图标稍大一点，与文字平衡 */
    color: #909399;
    transition: transform 0.2s;
    /* （可选）配合下拉展开旋转 */
}


.admin-main {
    background: #f0f2f5;
    padding: 20px;
    flex: 1;
    overflow-y: auto;
}

/* 响应式：小屏幕下侧边栏可折叠，或隐藏 */
@media screen and (max-width: 768px) {
    .admin-aside {
        width: 60px !important;
        /* 折叠为窄条 */
    }

    .admin-aside .logo {
        font-size: 14px;
        padding: 0 10px;
    }

    .admin-aside .el-menu-item span {
        display: none;
        /* 隐藏文字，只显示图标 */
    }

    .admin-main {
        padding: 10px;
    }
}
</style>