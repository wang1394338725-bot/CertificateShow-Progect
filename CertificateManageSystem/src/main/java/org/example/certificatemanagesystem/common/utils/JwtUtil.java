package org.example.certificatemanagesystem.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 续期专用：换发新 token 时继承原 token 的签发时间（iat），
     * 使"自首次登录起 N 天强制重登"的会话上限不会被续期重置。
     */
    public String generateToken(Long userId, Date originalIssuedAt) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(originalIssuedAt)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    private static final long GATE_EXPIRATION_MS = 7 * 24 * 3600 * 1000L; // 门禁 token 有效期 7 天

    /**
     * 门禁 token：答对门禁题后签发。subject 固定 "GATE" 且带 gate 标记，
     * 与管理员 token 天然隔离（getUserIdFromToken 解析 subject 为数字必然失败，无法冒充管理员）。
     */
    public String generateGateToken() {
        Date now = new Date();
        return Jwts.builder()
                .subject("GATE")
                .claim("gate", 1)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + GATE_EXPIRATION_MS))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** 校验门禁 token：签名有效、未过期且带 gate 标记 */
    public boolean validateGateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Integer.valueOf(1).equals(claims.get("gate", Integer.class));
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 解析 claims，容忍已过期但签名有效的 token（用于无感续期刷新）。
     * 已过期签名有效 → 返回 claims（iat 仍可读取）；签名无效/被篡改 → 返回 null（须重新登录）。
     */
    public Claims parseClaimsAllowExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 过期异常的 claims 仍可读取，签名已验证
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getUserIdAllowExpired(String token) {
        Claims claims = parseClaimsAllowExpired(token);
        return claims == null ? null : claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getUserIdFromToken(token);
            return true;
        } catch (Exception e) {
            return false;

        }
    }
}
