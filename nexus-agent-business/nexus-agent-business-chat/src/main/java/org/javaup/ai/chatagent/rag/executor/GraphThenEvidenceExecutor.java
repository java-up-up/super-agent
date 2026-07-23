package org.javaup.ai.chatagent.rag.executor;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.model.trace.ConversationTraceStageCode;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.DocumentNavigationDecision;
import org.javaup.ai.chatagent.rag.model.ExecutionMode;
import org.javaup.ai.chatagent.rag.service.GraphAnswerRenderer;
import org.javaup.ai.chatagent.rag.service.StructureGraphQueryEngine;
import org.javaup.ai.chatagent.rag.support.ExecutorEventSupport;
import org.javaup.ai.chatagent.service.ConversationTraceRecorder;
import org.javaup.ai.chatagent.service.TaskInfo;
import org.javaup.ai.chatagent.support.StreamEventWriter;
import org.javaup.ai.manage.model.graph.GraphQueryResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 先走结构图定位，再读取章节/item 证据的执行器
 * @author: 阿星不是程序员
 **/

@Component
@Slf4j
public class GraphThenEvidenceExecutor implements ConversationExecutor {

    private final StructureGraphQueryEngine structureGraphQueryEngine;
    private final GraphAnswerRenderer graphAnswerRenderer;
    private final StreamEventWriter streamEventWriter;

    public GraphThenEvidenceExecutor(StructureGraphQueryEngine structureGraphQueryEngine,
                                     GraphAnswerRenderer graphAnswerRenderer,
                                     StreamEventWriter streamEventWriter) {
        this.structureGraphQueryEngine = structureGraphQueryEngine;
        this.graphAnswerRenderer = graphAnswerRenderer;
        this.streamEventWriter = streamEventWriter;
    }

    @Override
    public ExecutionMode mode() {
        return ExecutionMode.GRAPH_THEN_EVIDENCE;
    }

    @Override
    public Flux<String> execute(TaskInfo taskInfo) {
        ConversationExecutionPlan plan = taskInfo.executionPlan();
        DocumentNavigationDecision decision = plan == null ? null : plan.getNavigationDecision();
        if (plan == null || decision == null || decision.getStructureAnchor() == null || decision.getStructureAnchor().getStructureNodeId() == null) {
            log.info("GRAPH_THEN_EVIDENCE 执行器直接返回无证据: planPresent={}, decisionPresent={}, structureNodeId={}",
                plan != null,
                decision != null,
                decision == null || decision.getStructureAnchor() == null ? null : decision.getStructureAnchor().getStructureNodeId());
            return Flux.just(StrUtil.blankToDefault(plan == null ? "" : plan.getNoEvidenceReply(), "当前没有足够证据支持明确回答。"));
        }
        ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, "正在通过结构图定位目标章节和编号项。");
        ConversationTraceRecorder.StageHandle graphStage = taskInfo.traceRecorder() == null
            ? null
            : taskInfo.traceRecorder().startStage(ConversationTraceStageCode.GRAPH_QUERY, mode().name(), "正在执行结构图定位与取证。", null);
        log.info("GRAPH_THEN_EVIDENCE 执行开始: documentId={}, sectionNodeId={}, itemIndex={}, navigationSummary='{}'",
            plan.getSelectedDocumentId(),
            decision.getStructureAnchor().getStructureNodeId(),
            decision.getItemAnchor() == null ? null : decision.getItemAnchor().getItemIndex(),
            decision.getSummaryText());
        GraphQueryResult graphResult = buildGraphResult(plan, decision);
        if (!hasGraphEvidence(graphResult, decision)) {
            log.info("GRAPH_THEN_EVIDENCE 证据校验失败: documentId={}, sectionNodeId={}, notes={}",
                plan.getSelectedDocumentId(),
                decision.getStructureAnchor().getStructureNodeId(),
                List.of("结构图未定位到满足条件的章节或编号项。"));
            if (taskInfo.traceRecorder() != null) {
                taskInfo.traceRecorder().completeStage(graphStage, "结构图定位完成，但证据不满足约束。", Map.of(
                    "targetSection", graphResult == null || graphResult.getTargetSection() == null ? "" : StrUtil.blankToDefault(graphResult.getTargetSection().displayTitle(), ""),
                    "targetItemIndex", graphResult == null || graphResult.getTargetItem() == null || graphResult.getTargetItem().getItemIndex() == null ? "" : String.valueOf(graphResult.getTargetItem().getItemIndex()),
                    "notes", List.of("结构图未定位到满足条件的章节或编号项。")
                ));
            }
            return Flux.just(StrUtil.blankToDefault(plan.getNoEvidenceReply(), "当前没有足够证据支持明确回答。"));
        }
        String answer = graphAnswerRenderer.renderGraphAnswer(mode(), decision, graphResult);
        log.info("GRAPH_THEN_EVIDENCE 执行完成: documentId={}, sectionNodeId={}, targetSection='{}', targetItemIndex={}, answerLength={}",
            plan.getSelectedDocumentId(),
            decision.getStructureAnchor().getStructureNodeId(),
            graphResult.getTargetSection() == null ? "" : graphResult.getTargetSection().displayTitle(),
            graphResult.getTargetItem() == null ? null : graphResult.getTargetItem().getItemIndex(),
            answer == null ? 0 : answer.length());
        if (taskInfo.traceRecorder() != null) {
            taskInfo.traceRecorder().completeStage(graphStage, "结构图取证完成。", Map.of(
                "targetSection", graphResult.getTargetSection() == null ? "" : StrUtil.blankToDefault(graphResult.getTargetSection().displayTitle(), ""),
                "targetItemIndex", graphResult.getTargetItem() == null || graphResult.getTargetItem().getItemIndex() == null ? "" : String.valueOf(graphResult.getTargetItem().getItemIndex()),
                "matchedItemCount", graphResult.getMatchedItems() == null ? 0 : graphResult.getMatchedItems().size(),
                "answer", StrUtil.blankToDefault(answer, "")
            ));
        }
        return Flux.fromIterable(answer.isBlank() ? List.of(StrUtil.blankToDefault(plan.getNoEvidenceReply(), "当前没有足够证据支持明确回答。")) : List.of(answer));
    }

    private GraphQueryResult buildGraphResult(ConversationExecutionPlan plan, DocumentNavigationDecision decision) {
        Long documentId = plan.getSelectedDocumentId();
        Long sectionNodeId = decision.getStructureAnchor().getStructureNodeId();
        Integer itemIndex = decision.getItemAnchor() == null ? null : decision.getItemAnchor().getItemIndex();
        String itemKeyword = extractItemKeyword(plan.getOriginalQuestion(), decision);
        return structureGraphQueryEngine.buildGraphResult(documentId, sectionNodeId, itemIndex, itemKeyword);
    }

    private String extractItemKeyword(String question, DocumentNavigationDecision decision) {
        String normalized = StrUtil.blankToDefault(question, "");
        if (normalized.contains("哪一步") || normalized.contains("哪一项")) {
            String keyword = normalized.contains("哪一步")
                ? StrUtil.subAfter(normalized, "哪一步", false)
                : StrUtil.subAfter(normalized, "哪一项", false);
            keyword = keyword
                .replace("要求", "")
                .replace("需要", "")
                .replace("执行", "")
                .replace("进行", "")
                .replace("包含", "")
                .replace("的是", "")
                .replace("是什么", "")
                .replace("什么", "")
                .replace("？", "")
                .replace("?", "")
                .replace("。", "")
                .replace("，", "")
                .trim();
            if (StrUtil.isNotBlank(keyword)) {
                return keyword;
            }
        }
        return "";
    }

    private boolean hasGraphEvidence(GraphQueryResult graphResult, DocumentNavigationDecision decision) {
        if (graphResult == null || graphResult.getTargetSection() == null) {
            return false;
        }
        if (decision != null && decision.getItemAnchor() != null && decision.getItemAnchor().getItemIndex() != null) {
            return graphResult.getTargetItem() != null
                || (graphResult.getMatchedItems() != null && !graphResult.getMatchedItems().isEmpty());
        }
        return StrUtil.isNotBlank(graphResult.getTargetSection().getContentText())
            || (graphResult.getMatchedItems() != null && !graphResult.getMatchedItems().isEmpty());
    }
}
