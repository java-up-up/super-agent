package org.javaup.ai.chatagent.rag.service;

import org.javaup.ai.chatagent.rag.model.DocumentNavigationAction;

/**
 * DocumentQuestionRouter 使用的结构图直答意图判断结果。
 */
final class GraphOnlyIntentDecision {

    // 是否已经明确命中 GRAPH_ONLY 意图。
    private final boolean matched;
    // 命中后要交给图查询执行器的结构导航动作。
    private final DocumentNavigationAction action;
    // 命中或未命中的原因，会写入路由决策摘要，便于排查问题。
    private final String reason;
    // 本次判断的置信度，本地规则通常是固定高分，LLM 结果会按阈值校验。
    private final double confidence;
    // 本次判断的来源，例如本地规则或 LLM 分类结果。
    private final String source;

    GraphOnlyIntentDecision(boolean matched,
                            DocumentNavigationAction action,
                            String reason,
                            double confidence,
                            String source) {
        this.matched = matched;
        this.action = action;
        this.reason = reason;
        this.confidence = confidence;
        this.source = source;
    }

    boolean matched() {
        return matched;
    }

    DocumentNavigationAction action() {
        return action;
    }

    String reason() {
        return reason;
    }

    double confidence() {
        return confidence;
    }

    String source() {
        return source;
    }
}
