package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentPlanStatusEnum {
    WAIT_CONFIRM(1, "待确认"),
    CONFIRMED(2, "已确认"),
    EXECUTED(3, "已执行"),
    DISCARDED(4, "已废弃");

    private final Integer code;

    private final String msg;

    DocumentPlanStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentPlanStatusEnum getRc(Integer code) {
        for (DocumentPlanStatusEnum item : DocumentPlanStatusEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
