package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStrategyExecuteStatusEnum {
    WAIT_EXECUTE(1, "待执行"),
    EXECUTING(2, "执行中"),
    EXECUTE_SUCCESS(3, "执行成功"),
    EXECUTE_FAILED(4, "执行失败"),
    SKIPPED(5, "已跳过");

    private final Integer code;

    private final String msg;

    DocumentStrategyExecuteStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStrategyExecuteStatusEnum getRc(Integer code) {
        for (DocumentStrategyExecuteStatusEnum item : DocumentStrategyExecuteStatusEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
