package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStrategyRoleEnum {
    PRIMARY(1, "主策略"),
    OPTIMIZE(2, "优化策略"),
    FALLBACK(3, "兜底策略"),
    ENHANCE(4, "增强策略");

    private final Integer code;

    private final String msg;

    DocumentStrategyRoleEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStrategyRoleEnum getRc(Integer code) {
        for (DocumentStrategyRoleEnum item : DocumentStrategyRoleEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
