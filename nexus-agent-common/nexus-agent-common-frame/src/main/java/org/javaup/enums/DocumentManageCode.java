package org.javaup.enums;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 枚举定义
 * @author: 阿星不是程序员
 **/

public enum DocumentManageCode {

    DOCUMENT_NOT_FOUND(20001, "文档不存在"),

    UNSUPPORTED_FILE_TYPE(20002, "当前文件类型暂不支持"),

    EMPTY_FILE_CONTENT(20003, "文件内容不能为空"),

    DOCUMENT_STATUS_INVALID(20004, "文档当前状态不允许执行该操作"),

    STRATEGY_PLAN_NOT_FOUND(20005, "策略方案不存在"),

    STRATEGY_STEP_EMPTY(20006, "当前没有可执行的策略步骤"),

    INDEX_TASK_RUNNING(20007, "当前文档已有索引任务正在执行"),

    KAFKA_SEND_FAILED(20008, "异步任务投递失败"),

    DOCUMENT_PARSE_FAILED(20009, "文件解析失败"),

    DOCUMENT_STORAGE_FAILED(20010, "文件存储失败"),

    DOCUMENT_VECTOR_FAILED(20011, "向量化处理失败"),

    DOCUMENT_INDEX_UNAVAILABLE(20012, "文档当前没有可用索引"),

    DOCUMENT_RETRIEVE_EMPTY(20013, "未检索到可用资料");

    private final Integer code;

    private final String msg;

    DocumentManageCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public static String getMsg(Integer code) {
        for (DocumentManageCode item : DocumentManageCode.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item.msg;
            }
        }
        return "";
    }

    public static DocumentManageCode getRc(Integer code) {
        for (DocumentManageCode item : DocumentManageCode.values()) {
            if (item.code.intValue() == code.intValue()) {
                return item;
            }
        }
        return null;
    }
}
