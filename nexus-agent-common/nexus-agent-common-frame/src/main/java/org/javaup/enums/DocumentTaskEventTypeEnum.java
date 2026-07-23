package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentTaskEventTypeEnum {
    START(1, "开始"),
    COMPLETE(2, "完成"),
    FAILED(3, "失败"),
    RECOMMEND_STRATEGY(4, "推荐策略"),
    USER_ADJUST(5, "用户调整"),
    USER_CONFIRM(6, "用户确认");

    private final Integer code;

    private final String msg;

    DocumentTaskEventTypeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentTaskEventTypeEnum getRc(Integer code) {
        for (DocumentTaskEventTypeEnum item : DocumentTaskEventTypeEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
