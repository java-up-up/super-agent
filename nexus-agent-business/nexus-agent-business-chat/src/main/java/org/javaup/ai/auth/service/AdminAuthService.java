package org.javaup.ai.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.javaup.ai.auth.dto.AdminLoginRequest;
import org.javaup.ai.auth.vo.AdminLoginVo;
import org.javaup.ai.auth.vo.AdminProfileVo;

/**
 * 后台登录认证服务。
 */
public interface AdminAuthService {

    AdminLoginVo login(AdminLoginRequest request);

    AdminProfileVo currentProfile(HttpServletRequest request);
}
