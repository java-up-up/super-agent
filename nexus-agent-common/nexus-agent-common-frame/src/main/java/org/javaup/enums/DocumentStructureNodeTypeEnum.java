package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentStructureNodeTypeEnum {
    DOCUMENT(1, "文档根节点"),
    SECTION(2, "章节节点"),
    STEP(3, "步骤节点"),
    LIST_ITEM(4, "列表项节点");

    private final Integer code;

    private final String msg;

    DocumentStructureNodeTypeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentStructureNodeTypeEnum getRc(Integer code) {
        if (code == null) {
            return null;
        }
        for (DocumentStructureNodeTypeEnum item : values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
