package org.example.certificatemanagesystem.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 答题门禁：纯内存实现（不建表），每个 IP 一个状态，重启即清零。
 *
 * 策略：
 * - 答错 5 次触发封禁：时长 = 5 分钟 × 2^(banCount-1)，上限 24 小时；
 * 距上次封禁超过 24 小时则 banCount 重新从 5 分钟起步；
 * 第 4 次封禁起永久封禁；封禁期内请求直接拒绝且不消耗尝试次数；
 * - 答对：清空该 IP 状态，签发 HMAC-SHA256 凭证（7 天有效，绑定 IP 指纹，IP 变了需重新答题）。
 */
@Service
public class GateService {

    public static final String COOKIE_NAME = "gate_pass";

    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 24 * 60 * MINUTE_MS;
    private static final int MAX_FAIL = 5; // 答错 5 次触发封禁
    private static final long BASE_BAN_MS = 5 * MINUTE_MS; // 首次封禁 5 分钟
    private static final long MAX_BAN_MS = 720 * MINUTE_MS; // 封禁上限 24 小时
    private static final int PERMANENT_BAN_COUNT = 4; // 第 4 次封禁起永久
    private static final long TOKEN_TTL_MS = 7 * DAY_MS; // 凭证有效期 7 天

    /** 来自配置的答案（比对时 trim） */
    @Value("${gate.answer}")
    private String answer;

    private final String secret;

    /** 每 IP 的门禁状态（synchronized 保证并发安全） */
    private static final class IpState {
        int failCount;
        long banUntil;
        int banCount;
        long lastBanAt;
        boolean permanent;

        /** 封禁期内返回提示 message，未封禁返回 null（不消耗尝试次数） */
        synchronized String checkBanned(long now) {
            if (permanent) {
                return "该 IP 已被永久封禁";
            }
            if (now < banUntil) {
                return "请 " + remainMinutes(now) + " 分钟后再试";
            }
            return null;
        }

        /** 答错一次：按策略递增失败次数/触发封禁，返回给前端的提示 message */
        synchronized String recordFail(long now) {
            String banned = checkBanned(now);
            if (banned != null) {
                return banned;
            }
            failCount++;
            if (failCount < MAX_FAIL) {
                return "回答错误，还可尝试 " + (MAX_FAIL - failCount) + " 次";
            }
            // 达到上限触发封禁：距上次封禁超过 24h 则重新从 5 分钟起步；
            // 触发封禁后失败计数清零（下一次封禁周期重新给满 5 次机会，升级由 banCount 驱动）
            if (lastBanAt > 0 && (now - lastBanAt) > DAY_MS) {
                banCount = 0;
            }
            banCount++;
            lastBanAt = now;
            if (banCount >= PERMANENT_BAN_COUNT) {
                permanent = true;
                return "该 IP 已被永久封禁";
            }
            long banMs = Math.min(BASE_BAN_MS << (banCount - 1), MAX_BAN_MS);
            banUntil = now + banMs;
            failCount = 0;
            return "失败次数过多，请 " + remainMinutes(now) + " 分钟后再试";
        }

        private long remainMinutes(long now) {
            // 向上取整，避免"请 0 分钟后再试"
            return Math.max((banUntil - now) / MINUTE_MS + 1, 1);
        }
    }

    private final ConcurrentHashMap<String, IpState> states = new ConcurrentHashMap<>();

    public GateService(@Value("${gate.secret:}") String configuredSecret,
            @Value("${jwt.secret:}") String jwtSecret) {
        // GATE_SECRET 为空（未配置或留空）时复用 JWT_SECRET（生产环境由 relaxed binding 注入）
        this.secret = (configuredSecret == null || configuredSecret.isBlank()) ? jwtSecret : configuredSecret;
    }

    // ---------- 状态机 ----------

    /** 封禁期内返回提示 message；未封禁返回 null */
    public String rejectIfBanned(String ip) {
        IpState state = states.get(ip);
        return state == null ? null : state.checkBanned(System.currentTimeMillis());
    }

    /** 答错一次，返回提示 message */
    public String recordFail(String ip) {
        return states.computeIfAbsent(ip, k -> new IpState()).recordFail(System.currentTimeMillis());
    }

    /** 答对：清空该 IP 状态 */
    public void clear(String ip) {
        states.remove(ip);
    }

    /** 答案比对：trim 后 equals；门禁未配置答案时一律拒绝（fail-closed，防"空答案+空输入"直接放行） */
    public boolean answerMatches(String input) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        return input != null && input.trim().equals(answer.trim());
    }

    // ---------- 凭证签发/校验 ----------

    /**
     * 构建 gate_pass Cookie：HttpOnly、Path=/、7 天、SameSite=Lax；Secure 仅在 HTTPS
     * 请求时开启（兼容本地 http 测试）
     */
    public ResponseCookie buildPassCookie(HttpServletRequest request, String ip) {
        return ResponseCookie.from(COOKIE_NAME, issueToken(ip))
                .httpOnly(true)
                .path("/")
                .maxAge(604800)
                .sameSite("Lax")
                .secure(request.isSecure())
                .build();
    }

    /**
     * 签发凭证：base64url(payload) + "." + base64url(HMAC-SHA256)，payload 含 ipHash 与过期时间
     */
    public String issueToken(String ip) {
        long exp = System.currentTimeMillis() + TOKEN_TTL_MS;
        String payload = "{\"ipHash\":\"" + ipHash(ip) + "\",\"exp\":" + exp + "}";
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + base64Url(hmacSha256(encodedPayload));
    }

    /** 校验凭证：签名一致（常量时间比较）且未过期且 ipHash 匹配当前请求 IP */
    public boolean checkToken(String token, String ip) {
        if (token == null || token.isBlank()) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return false;
        }
        try {
            String encodedPayload = token.substring(0, dot);
            byte[] givenSig = Base64.getUrlDecoder().decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(givenSig, hmacSha256(encodedPayload))) {
                return false;
            }
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String ipHash = extractField(payload, "\"ipHash\":\"", "\"");
            long exp = Long.parseLong(extractField(payload, "\"exp\":", "}").trim());
            return System.currentTimeMillis() < exp && ipHash(ip).equals(ipHash);
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- 工具 ----------

    /**
     * 获取客户端 IP：优先 X-Real-IP（由本站 nginx 从 $remote_addr 强制写入，客户端伪造无效）；
     * XFF 首段可被客户端伪造，仅作无 nginx 场景（本地 dev）的兜底，绝不可优先信任
     */
    public static String clientIp(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** 按名取 Cookie 值，不存在返回 null */
    public static String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 不可用", e);
        }
    }

    /** ipHash = sha256(ip + secret) 截取前 16 个 hex 字符（不落明文 IP） */
    private String ipHash(String ip) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((ip + secret).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String extractField(String json, String startMark, String endMark) {
        int start = json.indexOf(startMark);
        if (start < 0) {
            throw new IllegalArgumentException("payload 缺少字段");
        }
        start += startMark.length();
        int end = json.indexOf(endMark, start);
        if (end < 0) {
            throw new IllegalArgumentException("payload 缺少字段");
        }
        return json.substring(start, end);
    }
}
