package org.javaup.ai.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 后台管理登录配置。
 */
@Data
@ConfigurationProperties(prefix = "app.admin-auth")
public class AdminAuthProperties {

    /**
     * 后台登录用户名。
     */
    private String username = "admin";

    /**
     * 后台登录密码。
     */
    private String password = "admin123456";

    /**
     * JWT 签名密钥。
     */
    private String tokenSecret = "super-agent-admin-token-secret-change-me";

    /**
     * token 有效期，单位分钟。
     */
    private Long tokenExpireMinutes = 720L;
}
