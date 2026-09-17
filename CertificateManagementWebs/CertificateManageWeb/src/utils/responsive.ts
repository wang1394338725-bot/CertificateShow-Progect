import { ref, onMounted, onUnmounted } from 'vue'

/** 移动端断点，与各页面 CSS 媒体查询保持一致 */
const MOBILE_QUERY = '(max-width: 768px)'

/** 是否移动端视口（响应式 ref，随窗口尺寸实时变化）。用于 JS 侧控制列宽/按钮形态等组件属性 */
export function useIsMobile() {
    const mql = window.matchMedia(MOBILE_QUERY)
    const isMobile = ref(mql.matches)
    const handler = (e: MediaQueryListEvent) => { isMobile.value = e.matches }
    onMounted(() => mql.addEventListener('change', handler))
    onUnmounted(() => mql.removeEventListener('change', handler))
    return { isMobile }
}
