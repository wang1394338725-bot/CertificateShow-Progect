package org.example.certificatemanagesystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.certificatemanagesystem.common.utils.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 统一登录拦截器：默认拒绝，白名单放行。
 * 校验通过时把 userId 写入 request attribute（key: userId）供业务代码取用；
 * 公开接口即使无 token 也放行，但不设置 userId，业务代码可据此区分访客视角
 * （如 /view 对未登录请求强制只返回公开状态奖状）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTR = "userId";

    /** 无需登录即可访问的路径（登录换发、公开浏览、静态图片），其余路径一律要求有效 token */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/uploads/",
            "/certificate/login",
            "/certificate/refresh-token",
            "/certificate/home",
            "/certificate/search",
            "/certificate/view"
    );

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            Long userId = tryParseUserId(request);
            if (userId != null) {
                request.setAttribute(USER_ID_ATTR, userId);
            }
            return true;
        }

        Long userId = tryParseUserId(request);
        if (userId == null) {
            // HTTP 200 + 业务码 401：与项目 ResultVO 风格一致，前端全局拦截器按 message 弹窗
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"data\":null,\"message\":\"未登录或 token 无效\"}");
            return false;
        }
        request.setAttribute(USER_ID_ATTR, userId);
        return true;
    }

    private Long tryParseUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return Long.parseLong(jwtUtil.getUserIdFromToken(authHeader.substring(7)));
        } catch (Exception e) {
            return null;
        }
    }
}
