package com.hrms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expire;

    public JwtUtil(@Value("${hrms.jwt.secret}") String secret,
                   @Value("${hrms.jwt.expire}") long expire) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
    }

    public String generate(Long userId, String username, String role, Long employeeId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire))
                .signWith(key);
        if (employeeId != null) {
            builder.claim("employeeId", employeeId);
        }
        return builder.compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpire() {
        return expire;
    }
}
