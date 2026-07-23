package org.javaup.ai.chatagent.model.trace;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 轮次链路阶段定义
 * @author: 阿星不是程序员
 **/

public enum ConversationTraceStageCode {

    MEMORY("MEMORY", "会话记忆", 10),
    INTENT("INTENT", "意图分析", 20),
    REWRITE("REWRITE", "问题改写", 30),
    ROUTE("ROUTE", "路由判定", 40),
    GRAPH_QUERY("GRAPH_QUERY", "结构图查询", 45),
    RAG_RETRIEVE("RAG_RETRIEVE", "RAG 检索", 50),
    EVIDENCE_BUDGET("EVIDENCE_BUDGET", "证据评估与预算控制", 60),
    ANSWER_GENERATE("ANSWER_GENERATE", "回答生成", 70),
    REACT_AGENT("REACT_AGENT", "ReAct Agent", 75),
    RECOMMENDATION("RECOMMENDATION", "推荐问题", 80),
    FINALIZE("FINALIZE", "收尾归档", 90);

    private final String code;
    private final String label;
    private final int order;

    ConversationTraceStageCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }
}
