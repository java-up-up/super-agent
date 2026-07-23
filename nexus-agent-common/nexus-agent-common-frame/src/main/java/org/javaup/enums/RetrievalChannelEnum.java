package org.javaup.enums;

import lombok.Getter;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum RetrievalChannelEnum {

    KEYWORD(1, "keyword","关键词检索"),

    VECTOR(2, "vector","向量检索"),

    RERANK(3,"rerank","重排序");

    @Getter
    private final int code;
    @Getter
    private final String name;
    @Getter
    private final String desc;

    RetrievalChannelEnum(int code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public static RetrievalChannelEnum fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("检索通道类型 code 不能为空");
        }
        for (RetrievalChannelEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的检索通道类型 code: " + code);
    }
}
