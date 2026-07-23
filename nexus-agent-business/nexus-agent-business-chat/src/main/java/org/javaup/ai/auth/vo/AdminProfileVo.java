package org.javaup.ai.auth.vo;

/**
 * 当前后台管理员信息。
 */
public class AdminProfileVo {

    private String username;

    public AdminProfileVo(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
