package org.javaup.ai.chatagent.rag.executor;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.model.trace.ConversationTraceStageCode;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.ExecutionMode;
import org.javaup.ai.chatagent.rag.model.RagPromptAssemblyResult;
import org.javaup.ai.chatagent.rag.model.RagRetrievalContext;
import org.javaup.ai.chatagent.rag.service.RagPromptAssemblyService;
import org.javaup.ai.chatagent.rag.service.RagRetrievalEngine;
import org.javaup.ai.chatagent.rag.support.ExecutorEventSupport;
import org.javaup.ai.chatagent.service.ConversationTraceRecorder;
import org.javaup.ai.chatagent.service.ObservedChatModelService;
import org.javaup.ai.chatagent.service.TaskInfo;
import org.javaup.ai.chatagent.support.StreamEventWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 知识问答执行器
 * @author: 阿星不是程序员
 **/

@Component
public class RagChatExecutor implements ConversationExecutor {

    private final RagRetrievalEngine ragRetrievalEngine;
    private final RagPromptAssemblyService ragPromptAssemblyService;
    private final StreamEventWriter streamEventWriter;
    private final ObservedChatModelService observedChatModelService;

    public RagChatExecutor(RagRetrievalEngine ragRetrievalEngine,
                           RagPromptAssemblyService ragPromptAssemblyService,
                           StreamEventWriter streamEventWriter,
                           ObservedChatModelService observedChatModelService) {
        this.ragRetrievalEngine = ragRetrievalEngine;
        this.ragPromptAssemblyService = ragPromptAssemblyService;
        this.streamEventWriter = streamEventWriter;
        this.observedChatModelService = observedChatModelService;
    }

    @Override
    public ExecutionMode mode() {
        return ExecutionMode.RETRIEVAL;
    }

    @Override
    public Flux<String> execute(TaskInfo taskInfo) {
        ConversationExecutionPlan plan = taskInfo.executionPlan();

        ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, "正在根据问题规划知识检索范围。");

        ConversationTraceRecorder.StageHandle retrieveStage = taskInfo.traceRecorder() == null
            ? null
            : taskInfo.traceRecorder().startStage(ConversationTraceStageCode.RAG_RETRIEVE, mode().name(), "正在执行双通道混合检索。", null);

        return Mono.fromCallable(() -> ragRetrievalEngine.retrieve(plan, taskInfo.traceRecorder()))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(error -> {
                if (taskInfo.traceRecorder() != null) {
                    taskInfo.traceRecorder().failStage(retrieveStage, "RAG 检索失败。", error.getMessage(), null);
                }
            })
            .doOnSuccess(context -> {
                if (taskInfo.traceRecorder() != null && context != null) {
                    taskInfo.traceRecorder().completeStage(retrieveStage, "RAG 检索完成。", Map.of(
                        "retrievalQuestion", StrUtil.blankToDefault(context.getRetrievalQuestion(), ""),
                        "usedChannels", context.getUsedChannels() == null ? List.of() : context.getUsedChannels(),
                        "retrievalNotes", context.getRetrievalNotes() == null ? List.of() : context.getRetrievalNotes(),
                        "referenceCount", context.flattenReferences().size(),
                        "subQuestionCount", context.getSubQuestionEvidenceList() == null ? 0 : context.getSubQuestionEvidenceList().size(),
                        "subQuestions", context.getSubQuestionEvidenceList() == null
                            ? List.of()
                            : context.getSubQuestionEvidenceList().stream().map(item -> Map.of(
                                "index", item.getSubQuestionIndex(),
                                "question", StrUtil.blankToDefault(item.getSubQuestion(), ""),
                                "referenceCount", item.getReferences() == null ? 0 : item.getReferences().size(),
                                "documentCount", item.getDocuments() == null ? 0 : item.getDocuments().size(),
                                "fusedCandidateCount", item.getFusedCandidateCount() == null ? 0 : item.getFusedCandidateCount(),
                                "parentCandidateCount", item.getParentCandidateCount() == null ? 0 : item.getParentCandidateCount(),
                                "rerankedCandidateCount", item.getRerankedCandidateCount() == null ? 0 : item.getRerankedCandidateCount(),
                                "channelTraces", item.getChannelTraces() == null
                                    ? List.of()
                                    : item.getChannelTraces().stream().map(trace -> Map.of(
                                        "channelName", StrUtil.blankToDefault(trace.getChannelName(), ""),
                                        "recalledCount", trace.getRecalledCount(),
                                        "acceptedCount", trace.getAcceptedCount()
                                    )).toList(),
                                "references", item.getReferences() == null
                                    ? List.of()
                                    : item.getReferences().stream().map(reference -> Map.of(
                                        "referenceId", StrUtil.blankToDefault(reference.getReferenceId(), ""),
                                        "documentName", StrUtil.blankToDefault(reference.getDocumentName(), reference.getTitle()),
                                        "sectionPath", StrUtil.blankToDefault(reference.getSectionPath(), ""),
                                        "channel", StrUtil.blankToDefault(reference.getChannel(), "")
                                    )).toList()
                            )).toList(),
                        "references", context.flattenReferences().stream().map(reference -> Map.of(
                            "referenceId", StrUtil.blankToDefault(reference.getReferenceId(), ""),
                            "documentName", StrUtil.blankToDefault(reference.getDocumentName(), reference.getTitle()),
                            "sectionPath", StrUtil.blankToDefault(reference.getSectionPath(), ""),
                            "channel", StrUtil.blankToDefault(reference.getChannel(), "")
                        )).toList()
                    ));
                }
            })
            .flatMapMany(context -> streamFromRetrievalContext(taskInfo, plan, context));
    }

    private Flux<String> streamFromRetrievalContext(TaskInfo taskInfo,
                                                    ConversationExecutionPlan plan,
                                                    RagRetrievalContext context) {

        context.getRetrievalNotes().forEach(note -> ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, note));

        taskInfo.usedTools().addAll(context.getUsedChannels());
        taskInfo.debugTrace().setRetrievalNotes(new ArrayList<>(context.getRetrievalNotes()));
        taskInfo.debugTrace().setUsedChannels(new ArrayList<>(context.getUsedChannels()));

        if (context.isEmpty()) {

            ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, "当前没有足够证据，直接返回无证据兜底回复。");
            return Flux.just(StrUtil.blankToDefault(plan.getNoEvidenceReply(), "当前没有足够证据支持明确回答。"));
        }

        taskInfo.references().addAll(context.flattenReferences());
        ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, "证据整理完成，正在基于证据生成回答。");

        ConversationTraceRecorder.StageHandle budgetStage = taskInfo.traceRecorder() == null
            ? null
            : taskInfo.traceRecorder().startStage(ConversationTraceStageCode.EVIDENCE_BUDGET, mode().name(), "正在组装证据与 Prompt 预算。", null);
        RagPromptAssemblyResult promptAssemblyResult = ragPromptAssemblyService.assemble(plan, context);
        String systemPrompt = promptAssemblyResult.getSystemPrompt();
        String userPrompt = promptAssemblyResult.getUserPrompt();
        taskInfo.debugTrace().setRagSystemPrompt(systemPrompt);
        taskInfo.debugTrace().setRagUserPrompt(userPrompt);
        if (taskInfo.traceRecorder() != null) {
            taskInfo.traceRecorder().completeStage(budgetStage, "证据预算与 Prompt 组装完成。", Map.of(
                "totalBudget", promptAssemblyResult.getTotalBudget(),
                "perSubQuestionBudget", promptAssemblyResult.getPerSubQuestionBudget(),
                "renderedReferenceCount", promptAssemblyResult.getRenderedReferenceCount(),
                "omittedReferenceCount", promptAssemblyResult.getOmittedReferenceCount(),
                "renderedReferenceDetails", promptAssemblyResult.getRenderedReferenceDetails() == null ? List.of() : promptAssemblyResult.getRenderedReferenceDetails(),
                "omittedReferenceDetails", promptAssemblyResult.getOmittedReferenceDetails() == null ? List.of() : promptAssemblyResult.getOmittedReferenceDetails(),
                "systemPrompt", StrUtil.blankToDefault(systemPrompt, ""),
                "userPrompt", StrUtil.blankToDefault(userPrompt, "")
            ));
        }

        ConversationTraceRecorder.StageHandle answerStage = taskInfo.traceRecorder() == null
            ? null
            : taskInfo.traceRecorder().startStage(ConversationTraceStageCode.ANSWER_GENERATE, mode().name(), "正在基于证据生成回答。", null);
        return observedChatModelService.streamText("rag_answer", systemPrompt, userPrompt, taskInfo.traceRecorder())
            .doOnComplete(() -> {
                if (taskInfo.traceRecorder() != null) {
                    taskInfo.traceRecorder().completeStage(answerStage, "答案生成完成。", Map.of(
                        "firstResponseTimeMs", taskInfo.firstResponseTimeMs().get(),
                        "answerLength", taskInfo.answerBuffer().length()
                    ));
                }
            })
            .doOnError(error -> {
                if (taskInfo.traceRecorder() != null) {
                    taskInfo.traceRecorder().failStage(answerStage, "答案生成失败。", error.getMessage(), null);
                }
            });
    }
}
