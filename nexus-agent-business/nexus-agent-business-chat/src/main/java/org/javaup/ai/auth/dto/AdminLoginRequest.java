package org.javaup.ai.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 后台登录请求。
 */
public class AdminLoginRequest {

    @NotBlank(message = "请输入账号")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
