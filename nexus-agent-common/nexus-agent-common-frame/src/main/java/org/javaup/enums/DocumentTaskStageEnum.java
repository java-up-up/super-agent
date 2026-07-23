package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentTaskStageEnum {
    FILE_UPLOAD(1, "文件上传"),
    CONTENT_PARSE(2, "内容解析"),
    STRATEGY_ROUTE(3, "策略路由"),
    STRATEGY_CONFIRM(4, "策略确认"),
    CHUNK_EXECUTE(5, "切块执行"),
    CHUNK_POST_PROCESS(6, "切块后处理"),
    VECTORIZE(7, "向量化"),
    STORE_COMPLETE(8, "入库完成");

    private final Integer code;

    private final String msg;

    DocumentTaskStageEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static DocumentTaskStageEnum getRc(Integer code) {
        for (DocumentTaskStageEnum item : DocumentTaskStageEnum.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
