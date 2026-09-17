package org.example.certificatemanagesystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.vo.ResultVO;
import org.example.certificatemanagesystem.service.GateService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 答题门禁：访客答对一题后签发 gate_pass Cookie，才能访问公开数据接口。
 * 两个路径均在 AuthInterceptor 白名单内（无需登录）。
 */
@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;

    /** 答题验证请求体 */
    public record GateAnswerDTO(String answer) {
    }

    /** 答题验证：答对签发凭证；答错/封禁按 GateService 策略返回 429 业务码（HTTP 仍 200，与 ResultVO 风格一致） */
    @PostMapping("/gate/verify")
    public ResultVO<Void> verify(@RequestBody GateAnswerDTO gateAnswerDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        String ip = GateService.clientIp(request);

        // 封禁期内直接拒绝（不消耗尝试次数）
        String banMessage = gateService.rejectIfBanned(ip);
        if (banMessage != null) {
            return ResultVO.error(429, banMessage);
        }

        // 答错：按策略累计失败次数或触发封禁
        if (!gateService.answerMatches(gateAnswerDTO == null ? null : gateAnswerDTO.answer())) {
            return ResultVO.error(429, gateService.recordFail(ip));
        }

        // 答对：清空状态并签发 HttpOnly 凭证 Cookie
        gateService.clear(ip);
        response.addHeader(HttpHeaders.SET_COOKIE, gateService.buildPassCookie(request, ip).toString());
        return new ResultVO<>(200, null, "验证通过");
    }

    /** 凭证自检：前端用它判断"已通过则直接放行进入主页"；未通过返回 403 + gate 标记 */
    @GetMapping("/gate/check")
    public Map<String, Object> check(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        String token = GateService.cookieValue(request, GateService.COOKIE_NAME);
        if (token != null && gateService.checkToken(token, GateService.clientIp(request))) {
            body.put("code", 200);
            body.put("data", null);
            body.put("message", "已通过");
        } else {
            body.put("code", 403);
            body.put("data", null);
            body.put("message", "未通过访问验证");
            body.put("gate", true);
        }
        return body;
    }
}
