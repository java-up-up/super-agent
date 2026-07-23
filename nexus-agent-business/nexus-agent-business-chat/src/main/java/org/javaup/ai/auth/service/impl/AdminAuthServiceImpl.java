package org.javaup.ai.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.javaup.ai.auth.config.AdminAuthProperties;
import org.javaup.ai.auth.dto.AdminLoginRequest;
import org.javaup.ai.auth.service.AdminAuthService;
import org.javaup.ai.auth.support.AdminJwtTokenService;
import org.javaup.ai.auth.support.AdminRequestContext;
import org.javaup.ai.auth.vo.AdminLoginVo;
import org.javaup.ai.auth.vo.AdminProfileVo;
import org.javaup.exception.SuperAgentFrameException;
import org.springframework.stereotype.Service;

/**
 * 后台登录认证实现。
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminAuthProperties adminAuthProperties;

    private final AdminJwtTokenService adminJwtTokenService;

    public AdminAuthServiceImpl(AdminAuthProperties adminAuthProperties,
                                AdminJwtTokenService adminJwtTokenService) {
        this.adminAuthProperties = adminAuthProperties;
        this.adminJwtTokenService = adminJwtTokenService;
    }

    @Override
    public AdminLoginVo login(AdminLoginRequest request) {
        String username = StrUtil.trim(request.getUsername());
        String password = StrUtil.trim(request.getPassword());
        if (!StrUtil.equals(username, adminAuthProperties.getUsername())
            || !StrUtil.equals(password, adminAuthProperties.getPassword())) {
            throw new SuperAgentFrameException(401, "账号或密码不正确");
        }

        String token = adminJwtTokenService.generateToken(username);
        return new AdminLoginVo(username, token, adminAuthProperties.getTokenExpireMinutes());
    }

    @Override
    public AdminProfileVo currentProfile(HttpServletRequest request) {
        String username = AdminRequestContext.resolveUsername(request);
        return new AdminProfileVo(username);
    }
}
