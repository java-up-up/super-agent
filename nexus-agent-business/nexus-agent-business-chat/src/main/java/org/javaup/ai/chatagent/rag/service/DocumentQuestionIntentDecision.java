package org.javaup.ai.chatagent.rag.service;

/**
 * DocumentQuestionRouter 使用的统一问题意图判断结果。
 */
final class DocumentQuestionIntentDecision {

    // GRAPH_ONLY 是否成立以及成立后要执行的图导航动作。
    private final GraphOnlyIntentDecision graphOnlyIntent;
    // 当前问题是否需要解释、分析、对比或原因推理。
    private final boolean analytic;
    // 当前问题是否在询问章节目录展开或子章节列表。
    private final boolean outline;
    // 当前问题是否在询问步骤、条目、编号项定位。
    private final boolean itemLookup;
    // 当前问题是否带有章节、目录、标题、编号等结构线索，可用于普通检索前辅助定位章节。
    private final boolean structureHint;
    // 当前问题是否明显在询问正文内容、要求、流程、处理方式等证据。
    private final boolean contentQuestion;
    // 本次意图判断的置信度，便于日志或后续调试判断本地规则/LLM 的可靠程度。
    private final double confidence;
    // 本次意图判断的原因说明，作为 route 调试信息的补充。
    private final String reason;
    // 本次意图判断来自本地规则还是 LLM 兜底。
    private final String source;

    DocumentQuestionIntentDecision(GraphOnlyIntentDecision graphOnlyIntent,
                                   boolean analytic,
                                   boolean outline,
                                   boolean itemLookup,
                                   boolean structureHint,
                                   boolean contentQuestion,
                                   double confidence,
                                   String reason,
                                   String source) {
        this.graphOnlyIntent = graphOnlyIntent;
        this.analytic = analytic;
        this.outline = outline;
        this.itemLookup = itemLookup;
        this.structureHint = structureHint;
        this.contentQuestion = contentQuestion;
        this.confidence = confidence;
        this.reason = reason;
        this.source = source;
    }

    GraphOnlyIntentDecision graphOnlyIntent() {
        return graphOnlyIntent;
    }

    boolean analytic() {
        return analytic;
    }

    boolean outline() {
        return outline;
    }

    boolean itemLookup() {
        return itemLookup;
    }

    boolean structureHint() {
        return structureHint;
    }

    boolean contentQuestion() {
        return contentQuestion;
    }

    double confidence() {
        return confidence;
    }

    String reason() {
        return reason;
    }

    String source() {
        return source;
    }
}
