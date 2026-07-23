package org.javaup.ai.chatagent.rag.service;

import org.javaup.ai.manage.model.graph.GraphSection;

/**
 * 本地章节候选匹配分数，用于从文档结构中挑选最像用户问题锚点的章节。
 */
final class SectionScore {

    // 当前参与打分的图章节节点。
    private final GraphSection section;
    // 当前章节和用户问题短语之间的匹配分数。
    private final double score;

    SectionScore(GraphSection section, double score) {
        this.section = section;
        this.score = score;
    }

    GraphSection section() {
        return section;
    }

    double score() {
        return score;
    }
}
