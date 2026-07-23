package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStrategyPipelineTypeEnum {
    PARENT("PARENT", "父块流水线"),
    CHILD("CHILD", "子块流水线");

    private final String code;

    private final String msg;

    DocumentStrategyPipelineTypeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStrategyPipelineTypeEnum getRc(String code) {
        if (code == null) {
            return null;
        }
        for (DocumentStrategyPipelineTypeEnum item : DocumentStrategyPipelineTypeEnum.values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }
}
