package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentVectorStoreTypeEnum {
    MILVUS(1, "Milvus"),
    PG_VECTOR(2, "PGVector"),
    ELASTICSEARCH(3, "Elasticsearch");

    private final Integer code;

    private final String msg;

    DocumentVectorStoreTypeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentVectorStoreTypeEnum getRc(Integer code) {
        for (DocumentVectorStoreTypeEnum item : DocumentVectorStoreTypeEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
