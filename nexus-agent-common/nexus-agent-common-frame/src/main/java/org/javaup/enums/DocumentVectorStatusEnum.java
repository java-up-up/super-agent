package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentVectorStatusEnum {
    WAIT_VECTOR(1, "待向量化"),
    VECTORIZING(2, "向量化中"),
    VECTOR_SUCCESS(3, "向量化成功"),
    VECTOR_FAILED(4, "向量化失败");

    private final Integer code;

    private final String msg;

    DocumentVectorStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentVectorStatusEnum getRc(Integer code) {
        for (DocumentVectorStatusEnum item : DocumentVectorStatusEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
