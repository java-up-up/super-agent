package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.model.SearchReference;
import org.javaup.ai.chatagent.rag.config.ChatRagProperties;
import org.javaup.ai.chatagent.rag.model.AnswerHistoryContext;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.RagPromptAssemblyResult;
import org.javaup.ai.chatagent.rag.model.RagRetrievalContext;
import org.javaup.ai.chatagent.rag.model.SubQuestionEvidence;
import org.javaup.ai.prompt.PromptTemplateNames;
import org.javaup.ai.prompt.PromptTemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Service
public class RagPromptAssemblyService {

    private final ChatRagProperties properties;
    private final PromptTemplateService promptTemplateService;

    public RagPromptAssemblyService(ChatRagProperties properties,
                                    PromptTemplateService promptTemplateService) {
        this.properties = properties;
        this.promptTemplateService = promptTemplateService;
    }

    public String buildSystemPrompt() {

        return StrUtil.isNotBlank(properties.getAnswerSystemPrompt())
            ? properties.getAnswerSystemPrompt().trim()
            : promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_SYSTEM, Map.of());
    }

    public String buildUserPrompt(ConversationExecutionPlan plan, RagRetrievalContext context) {
        return assemble(plan, context).getUserPrompt();
    }

    public RagPromptAssemblyResult assemble(ConversationExecutionPlan plan, RagRetrievalContext context) {
        PromptBudget promptBudget = new PromptBudget(
            Math.max(0, properties.getTotalEvidenceMaxChars()),
            Math.max(0, properties.getPerSubQuestionEvidenceMaxChars())
        );
        Set<String> renderedReferenceKeys = new LinkedHashSet<>();
        String userPrompt = promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_USER, Map.of(
            "currentDate", StrUtil.blankToDefault(plan.getCurrentDateText(), ""),
            "originalQuestion", StrUtil.blankToDefault(plan.getOriginalQuestion(), ""),
            "hasRetrievalQuestion", hasRetrievalQuestion(plan),
            "retrievalQuestion", StrUtil.blankToDefault(plan.getRetrievalQuestion(), ""),
            "hasHistoryContext", hasHistoryContext(plan),
            "historyContext", buildHistoryContext(plan),
            "hasSubQuestions", hasSubQuestions(plan),
            "subQuestions", buildSubQuestions(plan),
            "evidenceBlocks", buildEvidenceBlocks(context, renderedReferenceKeys, promptBudget)
        ));
        return new RagPromptAssemblyResult(
            buildSystemPrompt(),
            userPrompt,
            promptBudget.totalBudget,
            promptBudget.perSubQuestionBudget,
            promptBudget.renderedReferenceCount,
            promptBudget.omittedReferenceCount,
            promptBudget.renderedReferenceDetails,
            promptBudget.omittedReferenceDetails
        );
    }

    private boolean hasRetrievalQuestion(ConversationExecutionPlan plan) {
        return StrUtil.isNotBlank(plan.getRetrievalQuestion()) && !plan.getRetrievalQuestion().equals(plan.getOriginalQuestion());
    }

    private boolean hasHistoryContext(ConversationExecutionPlan plan) {
        AnswerHistoryContext answerHistoryContext = plan.getAnswerHistoryContext();
        return answerHistoryContext != null && !answerHistoryContext.isEmpty();
    }

    private String buildHistoryContext(ConversationExecutionPlan plan) {
        return hasHistoryContext(plan) ? plan.getAnswerHistoryContext().getRenderedText().trim() : "";
    }

    private boolean hasSubQuestions(ConversationExecutionPlan plan) {
        return plan.getRetrievalSubQuestions() != null && plan.getRetrievalSubQuestions().size() > 1;
    }

    private String buildSubQuestions(ConversationExecutionPlan plan) {
        if (!hasSubQuestions(plan)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < plan.getRetrievalSubQuestions().size(); index++) {
            builder.append(index + 1).append(". ").append(plan.getRetrievalSubQuestions().get(index)).append("\n");
        }
        return builder.toString().trim();
    }

    private String buildEvidenceBlocks(RagRetrievalContext context,
                                       Set<String> renderedReferenceKeys,
                                       PromptBudget promptBudget) {
        StringBuilder builder = new StringBuilder();
        for (SubQuestionEvidence evidence : context.getSubQuestionEvidenceList()) {
            StringBuilder referenceBuilder = new StringBuilder();
            appendReferences(referenceBuilder, evidence.getReferences(), renderedReferenceKeys, promptBudget);
            builder.append(promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_SUB_QUESTION_EVIDENCE, Map.of(
                "subQuestionIndex", evidence.getSubQuestionIndex(),
                "subQuestion", StrUtil.blankToDefault(evidence.getSubQuestion(), ""),
                "references", referenceBuilder.toString().trim()
            ))).append("\n\n");
        }
        return builder.toString().trim();
    }

    private void appendReferences(StringBuilder builder,
                                  List<SearchReference> references,
                                  Set<String> renderedReferenceKeys,
                                  PromptBudget promptBudget) {
        if (references == null || references.isEmpty()) {
            builder.append(promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_NO_EVIDENCE, Map.of())).append('\n');
            return;
        }
        promptBudget.resetSubQuestionBudget();
        boolean omitted = false;
        for (SearchReference reference : references) {
            String uniqueKey = reference.uniqueKey();
            if (renderedReferenceKeys.contains(uniqueKey)) {
                String reuseLine = promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_REUSE_REFERENCE, Map.of(
                    "referenceId", StrUtil.blankToDefault(reference.getReferenceId(), "")
                )) + "\n";
                if (promptBudget.tryConsume(reuseLine.length())) {
                    builder.append(reuseLine);
                }
                continue;
            }

            if ("WEB".equalsIgnoreCase(reference.getSourceType())) {
                String block = buildWebReferenceBlock(reference);
                if (promptBudget.tryConsume(block.length())) {
                    builder.append(block);
                    renderedReferenceKeys.add(uniqueKey);
                    promptBudget.markRendered(referenceSummary(reference, "已纳入 Prompt"));
                } else {
                    omitted = true;
                    promptBudget.markOmitted(referenceSummary(reference, "超出上下文预算，已省略"));
                    break;
                }
                continue;
            }
            String block = buildDocumentReferenceBlock(reference);
            if (promptBudget.tryConsume(block.length())) {
                builder.append(block);
                renderedReferenceKeys.add(uniqueKey);
                promptBudget.markRendered(referenceSummary(reference, "已纳入 Prompt"));
            } else {
                omitted = true;
                promptBudget.markOmitted(referenceSummary(reference, "超出上下文预算，已省略"));
                break;
            }
        }
        if (omitted) {
            builder.append(promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_OMITTED_EVIDENCE, Map.of())).append('\n');
        }
    }

    private String buildWebReferenceBlock(SearchReference reference) {
        return promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_WEB_REFERENCE, Map.of(
            "referenceId", StrUtil.blankToDefault(reference.getReferenceId(), ""),
            "title", StrUtil.blankToDefault(reference.getTitle(), "网页来源"),
            "url", StrUtil.blankToDefault(reference.getUrl(), "未知"),
            "snippet", trimSnippet(reference.getSnippet(), 900)
        )) + "\n\n";
    }

    private String buildDocumentReferenceBlock(SearchReference reference) {
        return promptTemplateService.render(PromptTemplateNames.RAG_ANSWER_DOCUMENT_REFERENCE, Map.of(
            "referenceId", StrUtil.blankToDefault(reference.getReferenceId(), ""),
            "documentName", StrUtil.blankToDefault(
                StrUtil.blankToDefault(reference.getDocumentName(), reference.getTitle()),
                "文档来源"
            ),
            "sectionPath", StrUtil.blankToDefault(reference.getSectionPath(), "未识别"),
            "snippet", trimSnippet(reference.getSnippet(), 1100)
        )) + "\n\n";
    }

    private String trimSnippet(String snippet, int maxChars) {
        if (StrUtil.isBlank(snippet)) {
            return "";
        }

        return snippet.length() <= maxChars ? snippet : snippet.substring(0, maxChars) + "...";
    }

    private String referenceSummary(SearchReference reference, String suffix) {
        if (reference == null) {
            return suffix;
        }
        String title = StrUtil.blankToDefault(reference.getDocumentName(), reference.getTitle());
        String path = StrUtil.blankToDefault(reference.getSectionPath(), reference.getUrl());
        String refId = StrUtil.blankToDefault(reference.getReferenceId(), "-");
        return "[" + refId + "] " + title + (StrUtil.isBlank(path) ? "" : " | " + path) + " | " + suffix;
    }

    private static final class PromptBudget {

        private final int totalBudget;
        private final int perSubQuestionBudget;
        private int remainingTotal;
        private int remainingSubQuestion;
        private int renderedReferenceCount;
        private int omittedReferenceCount;
        private final List<String> renderedReferenceDetails = new ArrayList<>();
        private final List<String> omittedReferenceDetails = new ArrayList<>();

        private PromptBudget(int totalBudget, int perSubQuestionBudget) {
            this.totalBudget = totalBudget;
            this.perSubQuestionBudget = perSubQuestionBudget;
            this.remainingTotal = totalBudget;
            this.remainingSubQuestion = perSubQuestionBudget;
        }

        private void resetSubQuestionBudget() {
            this.remainingSubQuestion = perSubQuestionBudget;
        }

        private boolean tryConsume(int size) {
            if (totalBudget <= 0 || perSubQuestionBudget <= 0) {
                return false;
            }
            if (size > remainingTotal || size > remainingSubQuestion) {
                return false;
            }
            remainingTotal -= size;
            remainingSubQuestion -= size;
            return true;
        }

        private void markRendered(String detail) {
            renderedReferenceCount++;
            if (StrUtil.isNotBlank(detail)) {
                renderedReferenceDetails.add(detail);
            }
        }

        private void markOmitted(String detail) {
            omittedReferenceCount++;
            if (StrUtil.isNotBlank(detail)) {
                omittedReferenceDetails.add(detail);
            }
        }
    }
}
