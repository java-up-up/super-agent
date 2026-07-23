package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentParseStatusEnum {
    WAIT_PARSE(1, "待解析"),
    PARSING(2, "解析中"),
    PARSE_SUCCESS(3, "解析成功"),
    PARSE_FAILED(4, "解析失败");

    private final Integer code;

    private final String msg;

    DocumentParseStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentParseStatusEnum getRc(Integer code) {
        for (DocumentParseStatusEnum item : DocumentParseStatusEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
