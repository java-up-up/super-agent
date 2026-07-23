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
import org.javaup.ai.manage.model.graph.GraphSectionWithChildren;
import org.javaup.ai.manage.model.graph.GraphSectionWithSiblings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 结构图直接回答执行器
 * @author: 阿星不是程序员
 **/

@Component
@Slf4j
public class GraphOnlyExecutor implements ConversationExecutor {

    private final StructureGraphQueryEngine structureGraphQueryEngine;
    private final GraphAnswerRenderer graphAnswerRenderer;
    private final StreamEventWriter streamEventWriter;

    public GraphOnlyExecutor(StructureGraphQueryEngine structureGraphQueryEngine,
                             GraphAnswerRenderer graphAnswerRenderer,
                             StreamEventWriter streamEventWriter) {
        this.structureGraphQueryEngine = structureGraphQueryEngine;
        this.graphAnswerRenderer = graphAnswerRenderer;
        this.streamEventWriter = streamEventWriter;
    }

    @Override
    public ExecutionMode mode() {
        return ExecutionMode.GRAPH_ONLY;
    }

    @Override
    public Flux<String> execute(TaskInfo taskInfo) {
        ConversationExecutionPlan plan = taskInfo.executionPlan();
        DocumentNavigationDecision decision = plan == null ? null : plan.getNavigationDecision();
        if (plan == null || decision == null || decision.getStructureAnchor() == null || decision.getStructureAnchor().getStructureNodeId() == null) {
            log.info("GRAPH_ONLY 执行器直接返回无证据: planPresent={}, decisionPresent={}, structureNodeId={}",
                plan != null,
                decision != null,
                decision == null || decision.getStructureAnchor() == null ? null : decision.getStructureAnchor().getStructureNodeId());
            return Flux.just(StrUtil.blankToDefault(plan == null ? "" : plan.getNoEvidenceReply(), "当前没有足够证据支持明确回答。"));
        }
        ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, "正在通过结构图直接查询章节关系。");
        ConversationTraceRecorder.StageHandle graphStage = taskInfo.traceRecorder() == null
            ? null
            : taskInfo.traceRecorder().startStage(ConversationTraceStageCode.GRAPH_QUERY, mode().name(), "正在执行结构图查询。", null);
        Long documentId = plan.getSelectedDocumentId();
        Long sectionNodeId = decision.getStructureAnchor().getStructureNodeId();
        log.info("GRAPH_ONLY 执行开始: documentId={}, sectionNodeId={}, action={}, navigationSummary='{}'",
            documentId,
            sectionNodeId,
            decision.getNavigationAction(),
            decision.getSummaryText());
        GraphQueryResult graphResult;
        if (decision.getNavigationAction() == org.javaup.ai.chatagent.rag.model.DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP) {
            GraphSectionWithSiblings result = structureGraphQueryEngine.findSectionWithSiblings(documentId, sectionNodeId);
            graphResult = GraphQueryResult.builder()
                .targetSection(result.getSection())
                .parentSection(result.getParent())
                .previousSibling(result.getPreviousSibling())
                .nextSibling(result.getNextSibling())
                .build();
        }
        else {
            GraphSectionWithChildren result = structureGraphQueryEngine.findSectionWithChildren(documentId, sectionNodeId);
            graphResult = GraphQueryResult.builder()
                .targetSection(result.getSection())
                .children(result.getChildren())
                .build();
        }
        String answer = graphAnswerRenderer.renderGraphAnswer(mode(), decision, graphResult);
        log.info("GRAPH_ONLY 执行完成: documentId={}, sectionNodeId={}, targetSection='{}', answerLength={}",
            documentId,
            sectionNodeId,
            graphResult.getTargetSection() == null ? "" : graphResult.getTargetSection().displayTitle(),
            answer == null ? 0 : answer.length());
        if (taskInfo.traceRecorder() != null) {
            taskInfo.traceRecorder().completeStage(graphStage, "结构图查询完成。", Map.of(
                "targetSection", graphResult.getTargetSection() == null ? "" : StrUtil.blankToDefault(graphResult.getTargetSection().displayTitle(), ""),
                "parentSection", graphResult.getParentSection() == null ? "" : StrUtil.blankToDefault(graphResult.getParentSection().displayTitle(), ""),
                "childCount", graphResult.getChildren() == null ? 0 : graphResult.getChildren().size(),
                "previousSibling", graphResult.getPreviousSibling() == null ? "" : StrUtil.blankToDefault(graphResult.getPreviousSibling().displayTitle(), ""),
                "nextSibling", graphResult.getNextSibling() == null ? "" : StrUtil.blankToDefault(graphResult.getNextSibling().displayTitle(), ""),
                "answer", StrUtil.blankToDefault(answer, "")
            ));
        }
        return Flux.fromIterable(answer.isBlank() ? List.of(StrUtil.blankToDefault(plan.getNoEvidenceReply(), "当前没有足够证据支持明确回答。")) : List.of(answer));
    }
}
