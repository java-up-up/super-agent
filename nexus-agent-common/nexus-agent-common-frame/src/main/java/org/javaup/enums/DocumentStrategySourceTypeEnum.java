package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStrategySourceTypeEnum {
    SYSTEM_RECOMMEND(1, "系统推荐"),
    USER_ADD(2, "用户新增"),
    USER_KEEP(3, "用户保留");

    private final Integer code;

    private final String msg;

    DocumentStrategySourceTypeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStrategySourceTypeEnum getRc(Integer code) {
        for (DocumentStrategySourceTypeEnum item : DocumentStrategySourceTypeEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
