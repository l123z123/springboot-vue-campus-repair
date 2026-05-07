package com.campus.repair.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey getSecretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
    }

    /**
     * 生成 Token，有效期 24 小时
     */
    public String generateToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证 Token，返回 Claims；无效则返回 null
     */
    public Claims validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object u = claims.get("userId");
        if (u == null) {
            return null;
        }
        if (u instanceof Long) {
            return (Long) u;
        }
        if (u instanceof Integer) {
            return ((Integer) u).longValue();
        }
        if (u instanceof String) {
            try {
                return Long.parseLong(((String) u).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (u instanceof Number) {
            return ((Number) u).longValue();
        }
        return null;
    }

    public Integer getRole(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object r = claims.get("role");
        if (r == null) {
            return null;
        }
        if (r instanceof Integer) {
            return (Integer) r;
        }
        if (r instanceof Long) {
            return ((Long) r).intValue();
        }
        if (r instanceof String) {
            try {
                return Integer.parseInt(((String) r).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (r instanceof Number) {
            return ((Number) r).intValue();
        }
        return null;
    }
}
