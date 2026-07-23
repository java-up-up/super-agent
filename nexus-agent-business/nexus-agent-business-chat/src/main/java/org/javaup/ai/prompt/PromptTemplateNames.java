package org.javaup.ai.prompt;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: Prompt 模板名称常量
 * @author: 阿星不是程序员
 **/
public final class PromptTemplateNames {

    public static final String AGENT_QUESTION = "agent-question";
    public static final String CHAT_QUERY_REWRITE = "chat-query-rewrite";
    public static final String CONVERSATION_SUMMARY_MERGE = "conversation-summary-merge";
    public static final String CONVERSATION_SUMMARY_SYSTEM = "conversation-summary-system";
    public static final String DOCUMENT_GRAPH_ONLY_INTENT = "document-graph-only-intent";
    public static final String DOCUMENT_LLM_SPLIT = "document-llm-split";
    public static final String DOCUMENT_STRUCTURE_AMBIGUITY = "document-structure-ambiguity";
    public static final String DOCUMENT_STRUCTURE_AMBIGUITY_CANDIDATE = "document-structure-ambiguity-candidate";
    public static final String RAG_ANSWER_DOCUMENT_REFERENCE = "rag-answer-document-reference";
    public static final String RAG_ANSWER_NO_EVIDENCE = "rag-answer-no-evidence";
    public static final String RAG_ANSWER_OMITTED_EVIDENCE = "rag-answer-omitted-evidence";
    public static final String RAG_ANSWER_REUSE_REFERENCE = "rag-answer-reuse-reference";
    public static final String RAG_ANSWER_SUB_QUESTION_EVIDENCE = "rag-answer-sub-question-evidence";
    public static final String RAG_ANSWER_SYSTEM = "rag-answer-system";
    public static final String RAG_ANSWER_USER = "rag-answer-user";
    public static final String RAG_ANSWER_WEB_REFERENCE = "rag-answer-web-reference";
    public static final String RECOMMENDATION_USER = "recommendation-user";

    private PromptTemplateNames() {
    }
}
