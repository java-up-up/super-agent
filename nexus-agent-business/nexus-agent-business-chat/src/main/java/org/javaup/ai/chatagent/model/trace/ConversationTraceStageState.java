package org.javaup.ai.chatagent.model.trace;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 轮次阶段轨迹状态
 * @author: 阿星不是程序员
 **/

public enum ConversationTraceStageState {

    RUNNING(1, "进行中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败"),
    SKIPPED(4, "跳过");

    private final int code;
    private final String label;

    ConversationTraceStageState(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ConversationTraceStageState fromCode(Integer code) {
        if (code == null) {
            return RUNNING;
        }
        for (ConversationTraceStageState value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知的阶段状态 code: " + code);
    }
}
