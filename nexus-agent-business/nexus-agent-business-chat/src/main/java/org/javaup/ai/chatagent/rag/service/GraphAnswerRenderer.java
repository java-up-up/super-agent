package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.rag.model.DocumentNavigationAction;
import org.javaup.ai.chatagent.rag.model.DocumentNavigationDecision;
import org.javaup.ai.chatagent.rag.model.ExecutionMode;
import org.javaup.ai.manage.model.graph.GraphItem;
import org.javaup.ai.manage.model.graph.GraphQueryResult;
import org.javaup.ai.manage.model.graph.GraphSection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Service
public class GraphAnswerRenderer {

    public String renderGraphAnswer(ExecutionMode mode,
                                    DocumentNavigationDecision decision,
                                    GraphQueryResult graphResult) {
        if (graphResult == null || graphResult.getTargetSection() == null) {
            return "";
        }
        if (mode == ExecutionMode.GRAPH_THEN_EVIDENCE) {
            return renderGraphThenEvidence(decision, graphResult);
        }
        return renderGraphOnly(decision, graphResult);
    }

    private String renderGraphOnly(DocumentNavigationDecision decision, GraphQueryResult graphResult) {
        DocumentNavigationAction action = decision == null ? null : decision.getNavigationAction();
        String question = decision == null || decision.getRetrievalPlan() == null
            ? ""
            : StrUtil.blankToDefault(decision.getRetrievalPlan().getRetrievalQuestion(), "");
        if (action == DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP || asksAdjacency(question)) {
            return renderAdjacency(graphResult);
        }
        if (asksChildren(question) || !graphResult.getChildren().isEmpty()) {
            return renderChildren(graphResult.getTargetSection(), graphResult.getChildren());
        }
        return graphResult.getTargetSection().displayTitle();
    }

    private String renderGraphThenEvidence(DocumentNavigationDecision decision, GraphQueryResult graphResult) {
        if (graphResult.getTargetItem() != null) {
            GraphItem item = graphResult.getTargetItem();
            return "“" + graphResult.getTargetSection().displayTitle() + "”中的第" + item.getItemIndex() + "步是：\n"
                + formatItem(item);
        }
        if (graphResult.getMatchedItems() != null && !graphResult.getMatchedItems().isEmpty()) {
            StringJoiner joiner = new StringJoiner("\n");
            joiner.add("在“" + graphResult.getTargetSection().displayTitle() + "”中命中了以下步骤：");
            for (GraphItem item : graphResult.getMatchedItems()) {
                joiner.add(formatItem(item));
            }
            return joiner.toString();
        }
        GraphSection targetSection = graphResult.getTargetSection();
        if (StrUtil.isNotBlank(targetSection.getContentText())) {
            return "“" + targetSection.displayTitle() + "”中的相关内容如下：\n" + targetSection.getContentText().trim();
        }
        return targetSection.displayTitle();
    }

    private String renderAdjacency(GraphQueryResult graphResult) {
        StringJoiner joiner = new StringJoiner("\n");
        GraphSection targetSection = graphResult.getTargetSection();
        GraphSection parentSection = graphResult.getParentSection();
        joiner.add("目标章节是：“" + targetSection.displayTitle() + "”。");
        if (parentSection != null) {
            joiner.add("它属于：“" + parentSection.displayTitle() + "”。");
        }
        joiner.add("上一节：" + formatSectionOrFallback(graphResult.getPreviousSibling()));
        joiner.add("下一节：" + formatSectionOrFallback(graphResult.getNextSibling()));
        return joiner.toString();
    }

    private String renderChildren(GraphSection targetSection, List<GraphSection> children) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("“" + targetSection.displayTitle() + "”包含以下章节：");
        if (children == null || children.isEmpty()) {
            joiner.add("未找到直接子章节。");
            return joiner.toString();
        }
        for (GraphSection child : children) {
            joiner.add("- " + child.displayTitle());
        }
        return joiner.toString();
    }

    private String formatItem(GraphItem item) {
        if (item == null) {
            return "";
        }
        if (item.getItemIndex() != null) {
            return "第" + item.getItemIndex() + "步：" + StrUtil.blankToDefault(item.displayText(), "");
        }
        return StrUtil.blankToDefault(item.displayText(), "");
    }

    private String formatSectionOrFallback(GraphSection section) {
        return section == null ? "未找到相邻章节" : "“" + section.displayTitle() + "”";
    }

    private boolean asksAdjacency(String question) {
        return question.contains("上一节")
            || question.contains("下一节")
            || question.contains("前一节")
            || question.contains("后一节")
            || question.contains("属于哪个章节");
    }

    private boolean asksChildren(String question) {
        return question.contains("包含哪些章节")
            || question.contains("都包含哪些章节")
            || question.contains("有哪些小节")
            || question.contains("有哪些章节");
    }
}
