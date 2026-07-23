package org.javaup.ai.auth.support;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.javaup.common.ApiResponse;
import org.javaup.exception.SuperAgentFrameException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台管理接口鉴权拦截器。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminJwtTokenService adminJwtTokenService;

    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(AdminJwtTokenService adminJwtTokenService,
                                ObjectMapper objectMapper) {
        this.adminJwtTokenService = adminJwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        String token = resolveToken(authorization);
        if (StrUtil.isBlank(token)) {
            writeUnauthorized(response, "请先登录后台管理台");
            return false;
        }

        try {
            Claims claims = adminJwtTokenService.parseToken(token);
            String username = claims.getSubject();
            if (StrUtil.isBlank(username)) {
                writeUnauthorized(response, "后台登录无效，请重新登录");
                return false;
            }
            AdminRequestContext.storeUsername(request, username);
            return true;
        } catch (SuperAgentFrameException exception) {
            writeUnauthorized(response, exception.getMessage());
            return false;
        }
    }

    private String resolveToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return null;
        }
        if (StrUtil.startWithIgnoreCase(authorization, "Bearer ")) {
            return StrUtil.trim(authorization.substring(7));
        }
        return StrUtil.trim(authorization);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(401, message)));
    }
}
