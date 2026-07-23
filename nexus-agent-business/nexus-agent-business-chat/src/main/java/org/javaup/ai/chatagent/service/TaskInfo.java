package org.javaup.ai.chatagent.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.Data;
import org.javaup.ai.chatagent.model.debug.ChatDebugTrace;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.model.SearchReference;
import org.javaup.ai.chatagent.support.StreamEventMetadata;
import org.javaup.enums.ChatQueryMode;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Data
public class TaskInfo {
    private final String conversationId;
    private final long exchangeId;

    private final String question;
    private final ChatQueryMode chatMode;
    private final String traceId;
    private final Long selectedDocumentId;
    private final String selectedDocumentName;
    private final Long selectedTaskId;
    private final LocalDate currentDate;
    private final String currentDateText;

    private volatile ConversationExecutionPlan executionPlan;

    private volatile ChatDebugTrace debugTrace;

    private final RunnableConfig runnableConfig;
    private final ConversationTraceRecorder traceRecorder;

    private final Sinks.Many<String> sink;
    private final StreamEventMetadata eventMetadata;
    private final String leaseKey;
    private final String leaseOwnerToken;

    private final StringBuffer answerBuffer = new StringBuffer();

    private final List<String> thinkingSteps;
    private final List<SearchReference> references;
    private final Set<String> usedTools;

    private final long startTime;

    private final AtomicLong firstResponseTimeMs = new AtomicLong(0L);
    private final AtomicBoolean finalized = new AtomicBoolean(false);

    private volatile Disposable disposable;
    private volatile Disposable leaseRenewalDisposable;

    public TaskInfo(String conversationId,
                    long exchangeId,
                    String question,
                    ChatQueryMode chatMode,
                    String traceId,
                    Long selectedDocumentId,
                    String selectedDocumentName,
                    Long selectedTaskId,
                    LocalDate currentDate,
                    String currentDateText,
                    ConversationExecutionPlan executionPlan,
                    ChatDebugTrace debugTrace,
                    RunnableConfig runnableConfig,
                    ConversationTraceRecorder traceRecorder,
                    Sinks.Many<String> sink,
                    StreamEventMetadata eventMetadata,
                    String leaseKey,
                    String leaseOwnerToken,
                    List<String> thinkingSteps,
                    List<SearchReference> references,
                    Set<String> usedTools,
                    long startTime) {
        this.conversationId = conversationId;
        this.exchangeId = exchangeId;
        this.question = question;
        this.chatMode = chatMode;
        this.traceId = traceId;
        this.selectedDocumentId = selectedDocumentId;
        this.selectedDocumentName = selectedDocumentName;
        this.selectedTaskId = selectedTaskId;
        this.currentDate = currentDate;
        this.currentDateText = currentDateText;
        this.executionPlan = executionPlan;
        this.debugTrace = debugTrace;
        this.runnableConfig = runnableConfig;
        this.traceRecorder = traceRecorder;
        this.sink = sink;
        this.eventMetadata = eventMetadata;
        this.leaseKey = leaseKey;
        this.leaseOwnerToken = leaseOwnerToken;
        this.thinkingSteps = thinkingSteps;
        this.references = references;
        this.usedTools = usedTools;
        this.startTime = startTime;
    }

    public String conversationId() {
        return conversationId;
    }

    public long exchangeId() {
        return exchangeId;
    }

    public String question() {
        return question;
    }

    public ChatQueryMode chatMode() {
        return chatMode;
    }

    public String traceId() {
        return traceId;
    }

    public RunnableConfig runnableConfig() {
        return runnableConfig;
    }

    public ConversationTraceRecorder traceRecorder() {
        return traceRecorder;
    }

    public Long selectedDocumentId() {
        return selectedDocumentId;
    }

    public String selectedDocumentName() {
        return selectedDocumentName;
    }

    public Long selectedTaskId() {
        return selectedTaskId;
    }

    public LocalDate currentDate() {
        return currentDate;
    }

    public String currentDateText() {
        return currentDateText;
    }

    public ConversationExecutionPlan executionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(ConversationExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }

    public ChatDebugTrace debugTrace() {
        return debugTrace;
    }

    public void setDebugTrace(ChatDebugTrace debugTrace) {
        this.debugTrace = debugTrace;
    }

    public Sinks.Many<String> sink() {
        return sink;
    }

    public StreamEventMetadata eventMetadata() {
        return eventMetadata;
    }

    public String leaseKey() {
        return leaseKey;
    }

    public String leaseOwnerToken() {
        return leaseOwnerToken;
    }

    public StringBuffer answerBuffer() {
        return answerBuffer;
    }

    public List<String> thinkingSteps() {
        return thinkingSteps;
    }

    public List<SearchReference> references() {
        return references;
    }

    public Set<String> usedTools() {
        return usedTools;
    }

    public long startTime() {
        return startTime;
    }

    public AtomicLong firstResponseTimeMs() {
        return firstResponseTimeMs;
    }

    public AtomicBoolean finalized() {
        return finalized;
    }

    public Disposable disposable() {
        return disposable;
    }

    public Disposable leaseRenewalDisposable() {
        return leaseRenewalDisposable;
    }
}
