<script setup lang="ts">
</script>

<template>
    <router-view />
</template>

<style>
/* ===== 全局移动端适配（≤768px），对整个应用生效 ===== */
@media (max-width: 768px) {
    /* 所有 el-dialog 的固定宽度（920/860/600/500px）一律钳到屏宽内，max-width 优先于内联 width */
    .el-dialog {
        max-width: 94vw;
    }

    .el-message-box {
        max-width: 92vw;
    }

    /* 分页：手机端居中、换行、隐藏"每页条数/总数"省空间 */
    .el-pagination {
        justify-content: center;
        flex-wrap: wrap;
        row-gap: 6px;
    }

    .el-pagination .el-pagination__sizes,
    .el-pagination .el-pagination__total {
        display: none;
    }
}
</style>
