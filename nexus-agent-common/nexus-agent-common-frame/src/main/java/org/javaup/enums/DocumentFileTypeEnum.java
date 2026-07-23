package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentFileTypeEnum {
    PDF(1, "PDF"),
    DOC(2, "DOC"),
    DOCX(3, "DOCX"),
    TXT(4, "TXT"),
    MD(5, "MD"),
    HTML(6, "HTML");

    private final Integer code;

    private final String msg;

    DocumentFileTypeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentFileTypeEnum getRc(Integer code) {
        for (DocumentFileTypeEnum item : DocumentFileTypeEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }

    public static DocumentFileTypeEnum fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        String suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (suffix) {
            case "pdf" -> PDF;
            case "doc" -> DOC;
            case "docx" -> DOCX;
            case "txt" -> TXT;
            case "md", "markdown" -> MD;
            case "html", "htm" -> HTML;
            default -> null;
        };
    }
}
