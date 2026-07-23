package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStrategyStatusEnum {
    WAIT_RECOMMEND(1, "待推荐"),
    RECOMMENDED(2, "已推荐"),
    CONFIRMED(3, "已确认"),
    EXPIRED(4, "已失效");

    private final Integer code;

    private final String msg;

    DocumentStrategyStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStrategyStatusEnum getRc(Integer code) {
        for (DocumentStrategyStatusEnum item : DocumentStrategyStatusEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
