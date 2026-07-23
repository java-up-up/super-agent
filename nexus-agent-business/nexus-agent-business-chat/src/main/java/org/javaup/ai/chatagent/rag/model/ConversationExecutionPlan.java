package org.javaup.ai.chatagent.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javaup.enums.ChatQueryMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 单轮对话执行计划
 * @author: 阿星不是程序员
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationExecutionPlan {

    private ExecutionMode mode;

    private ChatQueryMode chatMode;

    private String originalQuestion;

    private String agentQuestion;

    private String rewriteQuestion;

    @Builder.Default
    private List<String> rewriteSubQuestions = new ArrayList<>();

    private String retrievalQuestion;

    @Builder.Default
    private List<String> retrievalSubQuestions = new ArrayList<>();

    private String historySummary;

    private String longTermSummary;

    @Builder.Default
    private HistoryPlanningContext historyPlanningContext = new HistoryPlanningContext();

    private String recentHistoryTranscript;

    private String answerRecentTranscript;

    private AnswerHistoryContext answerHistoryContext;

    private DocumentNavigationDecision navigationDecision;

    private boolean historyCompressionApplied;

    private Long historyCoveredExchangeId;

    private Integer historyCoveredExchangeCount;

    private Integer historyCompressionCount;

    private LocalDate currentDate;

    private String currentDateText;

    private boolean requiresFreshSearch;

    private boolean requiresCurrentDateAnchoring;

    private Long selectedDocumentId;

    private String selectedDocumentName;

    private Long selectedTaskId;

    @Builder.Default
    private List<Long> retrievalDocumentIds = new ArrayList<>();

    @Builder.Default
    private List<Long> retrievalTaskIds = new ArrayList<>();

    private String clarificationReply;

    @Builder.Default
    private List<String> clarificationOptions = new ArrayList<>();

    private String clarificationReason;

    private String noEvidenceReply;
}
