package org.javaup.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStrategyTypeEnum {
    STRUCTURE(1, "基于文档结构切块"),
    RECURSIVE(2, "递归分块"),
    SEMANTIC(3, "语义分块"),
    LLM(4, "大模型智能切块");

    private final Integer code;

    private final String msg;

    DocumentStrategyTypeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStrategyTypeEnum getRc(Integer code) {
        for (DocumentStrategyTypeEnum item : DocumentStrategyTypeEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }

    public static List<DocumentStrategyTypeEnum> orderedValues() {
        return Arrays.stream(DocumentStrategyTypeEnum.values())
            .sorted(Comparator.comparingInt(DocumentStrategyTypeEnum::getCode))
            .toList();
    }
}
