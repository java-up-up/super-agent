package org.javaup.ai.auth.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前请求中的后台管理员上下文。
 */
public final class AdminRequestContext {

    public static final String ADMIN_USERNAME_ATTRIBUTE = "super.agent.admin.username";

    private AdminRequestContext() {
    }

    public static void storeUsername(HttpServletRequest request, String username) {
        request.setAttribute(ADMIN_USERNAME_ATTRIBUTE, username);
    }

    public static String resolveUsername(HttpServletRequest request) {
        Object username = request.getAttribute(ADMIN_USERNAME_ATTRIBUTE);
        return username == null ? "" : String.valueOf(username);
    }
}
