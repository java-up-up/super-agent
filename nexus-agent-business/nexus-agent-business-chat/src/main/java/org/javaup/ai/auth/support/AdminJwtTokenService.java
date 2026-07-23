package org.javaup.ai.auth.support;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.javaup.ai.auth.config.AdminAuthProperties;
import org.javaup.exception.SuperAgentFrameException;
import org.springframework.stereotype.Component;

/**
 * 后台登录 JWT 服务。
 */
@Component
public class AdminJwtTokenService {

    private final AdminAuthProperties adminAuthProperties;

    public AdminJwtTokenService(AdminAuthProperties adminAuthProperties) {
        this.adminAuthProperties = adminAuthProperties;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(adminAuthProperties.getTokenExpireMinutes() * 60);
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expireAt))
            .signWith(
                SignatureAlgorithm.HS256,
                adminAuthProperties.getTokenSecret().getBytes(StandardCharsets.UTF_8)
            )
            .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(adminAuthProperties.getTokenSecret().getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException exception) {
            throw new SuperAgentFrameException(401, "后台登录已过期，请重新登录", exception);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new SuperAgentFrameException(401, "后台登录无效，请重新登录", exception);
        }
    }
}
