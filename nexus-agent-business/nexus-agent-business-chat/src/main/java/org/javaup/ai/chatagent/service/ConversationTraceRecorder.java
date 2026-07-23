package org.javaup.ai.chatagent.service;

import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.model.ChannelExecutionView;
import org.javaup.ai.chatagent.model.RetrievalResultView;
import org.javaup.ai.chatagent.model.debug.ChatLimitStats;
import org.javaup.ai.chatagent.model.debug.ChatModelUsageTrace;
import org.javaup.ai.chatagent.model.trace.ConversationTraceStageCode;
import org.javaup.ai.chatagent.model.trace.ConversationTraceStageState;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
public class ConversationTraceRecorder {

    private final ConversationTraceStageStore traceStageStore;
    private final RetrievalObserveStore retrievalObserveStore;
    private final String conversationId;
    private final long exchangeId;
    private final String traceId;
    private final List<ChatModelUsageTrace> modelUsageTraces = Collections.synchronizedList(new ArrayList<>());
    private final ChatLimitStats limitStats = new ChatLimitStats();

    public ConversationTraceRecorder(ConversationTraceStageStore traceStageStore,
                                     RetrievalObserveStore retrievalObserveStore,
                                     String conversationId,
                                     long exchangeId,
                                     String traceId) {
        this.traceStageStore = traceStageStore;
        this.retrievalObserveStore = retrievalObserveStore;
        this.conversationId = conversationId;
        this.exchangeId = exchangeId;
        this.traceId = traceId;
    }

    public String conversationId() {
        return conversationId;
    }

    public long exchangeId() {
        return exchangeId;
    }

    public String traceId() {
        return traceId;
    }

    public StageHandle startStage(ConversationTraceStageCode stageCode,
                                  String executionMode,
                                  String summaryText,
                                  Object snapshot) {
        long stageId = traceStageStore.startStage(
            conversationId,
            exchangeId,
            traceId,
            stageCode,
            1,
            null,
            executionMode,
            summaryText,
            snapshot
        );
        return new StageHandle(stageId, System.currentTimeMillis(), stageCode);
    }

    public void completeStage(StageHandle stageHandle,
                              String summaryText,
                              Object snapshot) {
        if (stageHandle == null) {
            return;
        }
        traceStageStore.finishStage(
            stageHandle.stageId(),
            ConversationTraceStageState.COMPLETED,
            summaryText,
            "",
            snapshot,
            System.currentTimeMillis() - stageHandle.startTimeMs()
        );
    }

    public void failStage(StageHandle stageHandle,
                          String summaryText,
                          String errorMessage,
                          Object snapshot) {
        if (stageHandle == null) {
            return;
        }
        traceStageStore.finishStage(
            stageHandle.stageId(),
            ConversationTraceStageState.FAILED,
            summaryText,
            errorMessage,
            snapshot,
            System.currentTimeMillis() - stageHandle.startTimeMs()
        );
    }

    public void failStage(StageHandle stageHandle,
                          String summaryText,
                          Throwable throwable,
                          Object snapshot) {
        if (stageHandle == null) {
            return;
        }
        String errorMessage = throwable == null ? "" : throwable.getMessage();
        String stackTrace = throwable == null ? "" : getStackTraceAsString(throwable);

        Object enhancedSnapshot = snapshot;
        if (throwable != null && snapshot instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> snapshotMap = new LinkedHashMap<>((Map<String, Object>) snapshot);
            snapshotMap.put("exceptionClass", throwable.getClass().getName());
            snapshotMap.put("stackTrace", stackTrace);
            enhancedSnapshot = snapshotMap;
        } else if (throwable != null) {
            enhancedSnapshot = Map.of(
                "exceptionClass", throwable.getClass().getName(),
                "errorMessage", errorMessage,
                "stackTrace", stackTrace
            );
        }

        traceStageStore.finishStage(
            stageHandle.stageId(),
            ConversationTraceStageState.FAILED,
            summaryText,
            errorMessage,
            enhancedSnapshot,
            System.currentTimeMillis() - stageHandle.startTimeMs()
        );
    }

    private String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    public void addModelUsageTrace(ChatModelUsageTrace trace) {
        if (trace != null) {
            modelUsageTraces.add(trace);
        }
    }

    public List<ChatModelUsageTrace> snapshotModelUsageTraces() {
        return new ArrayList<>(modelUsageTraces);
    }

    public ChatLimitStats limitStats() {
        return limitStats;
    }

    public void recordRetrievalResults(List<RetrievalResultView> results) {
        if (retrievalObserveStore == null || results == null || results.isEmpty()) {
            return;
        }
        try {
            retrievalObserveStore.batchSaveResults(conversationId, exchangeId, results);
        } catch (RuntimeException exception) {
            log.warn("记录检索结果快照失败, conversationId={}, exchangeId={}", conversationId, exchangeId, exception);
        }
    }

    public void recordChannelExecutions(List<ChannelExecutionView> executions) {
        if (retrievalObserveStore == null || executions == null || executions.isEmpty()) {
            return;
        }
        try {
            retrievalObserveStore.batchSaveChannelExecutions(conversationId, exchangeId, executions);
        } catch (RuntimeException exception) {
            log.warn("记录通道执行详情失败, conversationId={}, exchangeId={}", conversationId, exchangeId, exception);
        }
    }

    public record StageHandle(long stageId, long startTimeMs, ConversationTraceStageCode stageCode) {
    }
}
