package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum ChatSessionStatus {

    IDLE(1, "空闲"),

    RUNNING(2, "进行中");

    private final int code;
    private final String desc;

    ChatSessionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ChatSessionStatus fromCode(Integer code) {
        if (code == null) {
            return IDLE;
        }
        for (ChatSessionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的会话状态 code: " + code);
    }

    public static boolean isRunning(Integer code) {
        return RUNNING.code == (code == null ? IDLE.code : code);
    }
}
