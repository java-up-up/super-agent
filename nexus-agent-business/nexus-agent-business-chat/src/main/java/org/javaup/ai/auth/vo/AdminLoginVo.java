package org.javaup.ai.auth.vo;

/**
 * 后台登录返回值。
 */
public class AdminLoginVo {

    private String username;

    private String token;

    private Long expireMinutes;

    public AdminLoginVo(String username, String token, Long expireMinutes) {
        this.username = username;
        this.token = token;
        this.expireMinutes = expireMinutes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(Long expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}
