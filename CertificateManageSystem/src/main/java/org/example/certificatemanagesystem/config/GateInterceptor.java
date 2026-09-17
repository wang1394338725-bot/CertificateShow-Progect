package org.example.certificatemanagesystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.service.GateService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 答题门禁拦截器：只拦截三个公开数据接口（/home /view /search），
 * 在 AuthInterceptor 之前执行（注册 order 更小）。
 * 有合法 gate_pass Cookie（签名有效、未过期、IP 匹配）才放行，否则统一返回
 * HTTP 200 + {"code":403,"gate":true}，前端全局拦截器据此跳回验证页。
 * 管理端接口（需登录的那些）不在拦截范围，保持原样。
 */
@Component
@RequiredArgsConstructor
public class GateInterceptor implements HandlerInterceptor {

    private final GateService gateService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = GateService.cookieValue(request, GateService.COOKIE_NAME);
        if (token != null && gateService.checkToken(token, GateService.clientIp(request))) {
            return true;
        }

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"data\":null,\"message\":\"请先完成访问验证\",\"gate\":true}");
        return false;
    }
}
