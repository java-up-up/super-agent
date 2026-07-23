package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.rag.model.ConversationItemAnchor;
import org.javaup.ai.chatagent.rag.model.ConversationStructureAnchor;
import org.javaup.ai.chatagent.rag.model.DocumentNavigationAction;
import org.javaup.ai.chatagent.rag.model.DocumentNavigationDecision;
import org.javaup.ai.chatagent.rag.model.ExecutionMode;
import org.javaup.ai.chatagent.rag.model.RagRewriteResult;
import org.javaup.ai.chatagent.rag.model.RetrievalQuestionPlan;
import org.javaup.ai.chatagent.service.ObservedChatModelService;
import org.javaup.ai.manage.model.graph.GraphSection;
import org.javaup.ai.manage.service.DocumentNavigationIndexService;
import org.javaup.ai.manage.service.DocumentStructureGraphService;
import org.javaup.ai.prompt.PromptTemplateNames;
import org.javaup.ai.prompt.PromptTemplateService;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
@Service
public class DocumentQuestionRouter {

    // 匹配 1.2 / 3.4.5 这类章节编号，用于识别用户给出的结构锚点。
    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)+)");
    // 匹配“第 3 章 / 第三节 / 第 4 小节”，补齐非小数编号的章节锚点表达。
    private static final Pattern CHINESE_SECTION_REFERENCE_PATTERN = Pattern.compile("第\\s*([0-9一二三四五六七八九十百]+)\\s*(章|节|小节)");
    // 匹配“第几步”，该类问题应交给 GRAPH_THEN_EVIDENCE，而不是 GRAPH_ONLY。
    private static final Pattern STEP_REFERENCE_PATTERN = Pattern.compile("第\\s*([0-9一二三四五六七八九十百]+)\\s*步");
    // 匹配“第几条/点/项/个”，用于保留原有编号项定位能力。
    private static final Pattern ORDINAL_REFERENCE_PATTERN = Pattern.compile("第\\s*([0-9一二三四五六七八九十百]+)\\s*(条|点|项|个)");
    // 匹配用户用引号包住的标题短语，例如“上线观察”，用于结构节点定位。
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("[“\"']([^”\"']{2,40})[”\"']");
    // 兼容 LLM 偶尔输出代码块或解释文字时，从返回文本里抽取 JSON 对象。
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);

    // LLM 兜底分类只有达到该阈值，才允许进入 GRAPH_ONLY，低置信统一回退 RAG。
    private static final double GRAPH_ONLY_INTENT_CONFIDENCE_THRESHOLD = 0.75D;

    private static final List<String> ADJACENCY_HINTS = List.of(
        "上一节", "下一节", "前一节", "后一节", "上一章", "下一章", "前一章", "后一章",
        "上章", "下章", "上一个章节", "下一个章节", "属于哪个章节", "章节位置"
    );

    private static final List<String> OUTLINE_HINTS = List.of(
        "包含哪些章节", "都包含哪些章节", "有哪些章节", "有哪些小节", "包含哪些小节", "章节列表", "目录"
    );

    // 强目录展开表达可以直接判定为 outline，比单独的“目录/下面/有哪些”更不容易误伤正文问题。
    private static final List<String> OUTLINE_EXPLICIT_HINTS = List.of(
        "包含哪些章节", "都包含哪些章节", "有哪些章节", "有哪些小节", "包含哪些小节",
        "章节列表", "小节列表", "子章节", "子小节", "下级章节", "展开目录", "列出目录"
    );

    private static final List<String> ITEM_HINTS = List.of(
        "哪一步", "哪一项", "第几步", "第几项", "具体步骤", "步骤中的"
    );

    // 强分析词基本都要求解释、推理或对比，命中后通常不适合直接图直答。
    private static final List<String> ANALYTIC_STRONG_HINTS = List.of(
        "为什么", "原因", "可能原因", "影响", "区别", "对比", "比较",
        "如何理解", "怎么理解", "说明了什么", "是否意味着", "是否说明", "分析", "解释"
    );

    // “关系/关联”这类词有歧义：可能是内容关系，也可能只是章节前后或上下级关系。
    private static final List<String> ANALYTIC_WEAK_RELATION_HINTS = List.of(
        "关系", "关联", "联系", "相关"
    );

    // 这些短语说明用户问的是目录结构关系，不应被“关系/关联”误判为分析型问题。
    private static final List<String> STRUCTURAL_RELATION_HINTS = List.of(
        "前后关系", "相邻关系", "上下级关系", "父子关系", "目录关系", "章节关系",
        "所属关系", "位置关系", "顺序关系", "属于哪个章节", "上级章节", "下级章节",
        "同级章节", "父章节", "子章节"
    );

    // 这些分析型词会要求模型解释原因、影响或对比，不适合直接用结构图回答。
    private static final List<String> GRAPH_ONLY_BLOCKING_ANALYTIC_HINTS = List.of(
        "为什么", "原因", "可能原因", "影响", "区别", "对比", "比较",
        "如何理解", "怎么理解", "说明了什么", "是否意味着", "是否说明", "分析", "解释"
    );

    // 这些正文诉求词说明用户想看内容证据，而不是只看章节前后或目录层级。
    private static final List<String> GRAPH_ONLY_CONTENT_HINTS = List.of(
        "内容", "要求", "规定", "流程", "步骤", "处理", "执行",
        "怎么做", "讲了什么", "写了什么", "说了什么"
    );

    // 方向/层级线索用于决定是否值得调用 LLM 兜底分类，其中包含一些较模糊的自然表达。
    private static final List<String> GRAPH_ONLY_DIRECTION_HINTS = List.of(
        "前面", "后面", "上面", "下面", "之前", "之后", "此前", "随后", "后续", "接着", "紧接着",
        "往前", "往后",
        "前一个", "后一个", "上一个", "下一个", "上一", "下一", "相邻", "前后",
        "顺序", "位置", "属于", "上级", "父章节", "同级"
    );

    // 明确相邻关系词才用于本地高置信规则，避免“后面是什么”这类歧义问题被规则过早截断。
    private static final List<String> GRAPH_ONLY_EXPLICIT_ADJACENCY_HINTS = List.of(
        "前一个", "后一个", "上一个", "下一个", "前一", "后一", "上一", "下一", "相邻",
        "前后", "顺序", "位置", "属于", "上级", "父章节", "同级"
    );

    // 这些结构对象词说明问题对象可能是章节、标题、目录节点，而不是普通正文片段。
    private static final List<String> GRAPH_ONLY_STRUCTURE_OBJECT_HINTS = List.of(
        "章节", "小节", "这章", "这节", "这部分", "这一章", "该章", "本章", "标题", "目录", "部分", "模块", "节点", "条目"
    );

    // 这些动作词用于识别“展开目录 / 查看下级章节”类 GRAPH_ONLY 问题。
    private static final List<String> GRAPH_ONLY_OUTLINE_ACTION_HINTS = List.of(
        "下面", "下级", "子章节", "子小节", "子项", "展开", "包含哪些", "包括哪些",
        "有哪些", "列出", "列一下", "组成", "目录"
    );

    // 这些代词本身不够判断结构意图，但可以和方向/层级词一起触发 LLM 兜底分类。
    private static final List<String> GRAPH_ONLY_PRONOUN_ANCHOR_HINTS = List.of(
        "这个", "该", "它", "刚才", "上述", "上面"
    );

    // 当问题问“哪一节/哪个章节”等目标时，歧义方向词可以提升为高置信相邻章节判断。
    private static final List<String> GRAPH_ONLY_ADJACENCY_ANSWER_HINTS = List.of(
        "哪一节", "哪一章", "哪个章节", "哪个小节", "哪个标题", "哪部分", "哪块"
    );

    private final DocumentStructureGraphService graphService;
    private final ObjectProvider<DocumentNavigationIndexService> navigationIndexServiceProvider;
    private final ObservedChatModelService observedChatModelService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public DocumentQuestionRouter(DocumentStructureGraphService graphService,
                                  ObjectProvider<DocumentNavigationIndexService> navigationIndexServiceProvider,
                                  ObservedChatModelService observedChatModelService,
                                  PromptTemplateService promptTemplateService,
                                  ObjectMapper objectMapper) {
        this.graphService = graphService;
        this.navigationIndexServiceProvider = navigationIndexServiceProvider;
        this.observedChatModelService = observedChatModelService;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
    }

    public DocumentNavigationDecision route(Long documentId,
                                            String originalQuestion,
                                            RagRewriteResult rewriteResult) {
        String rewrittenQuestion = firstNonBlank(
            rewriteResult == null ? "" : rewriteResult.getRewrittenQuestion(),
            originalQuestion
        );
        List<String> subQuestions = normalizeSubQuestions(rewriteResult, rewrittenQuestion);
        RetrievalQuestionPlan retrievalPlan = new RetrievalQuestionPlan(rewrittenQuestion, subQuestions);
        String routeText = (safeText(originalQuestion) + " " + rewrittenQuestion).trim();
        DocumentQuestionIntentDecision questionIntent = detectQuestionIntent(
            routeText,
            originalQuestion,
            rewrittenQuestion,
            subQuestions
        );
        GraphOnlyIntentDecision graphOnlyIntent = questionIntent.graphOnlyIntent();
        boolean analyticQuestion = questionIntent.analytic();
        
        boolean singleQuestionGraphOnlyMatched = graphOnlyIntent.matched() && subQuestions.size() <= 1;
        if (singleQuestionGraphOnlyMatched) {
            GraphSection section = resolveSection(documentId, originalQuestion, rewrittenQuestion);
            return buildDecision(
                ExecutionMode.GRAPH_ONLY,
                graphOnlyIntent.action(),
                section,
                null,
                retrievalPlan,
                graphOnlyIntent.reason()
            );
        }

        Integer itemIndex = resolveExplicitItemIndex(routeText);
        boolean itemLookupMatched = itemIndex != null || questionIntent.itemLookup();
        boolean shouldUseGraphThenEvidence = itemLookupMatched && !analyticQuestion;
        if (shouldUseGraphThenEvidence) {
            GraphSection section = resolveSection(documentId, originalQuestion, rewrittenQuestion);
            return buildDecision(
                ExecutionMode.GRAPH_THEN_EVIDENCE,
                DocumentNavigationAction.ITEM_REFERENCE,
                section,
                itemIndex,
                retrievalPlan,
                "编号项或步骤型问题走图定位取证"
            );
        }

        GraphSection assistedSection = null;
        boolean needsStructureAssistedRetrieval = analyticQuestion
            || questionIntent.outline()
            || itemIndex != null
            || questionIntent.structureHint();
        if (needsStructureAssistedRetrieval) {
            assistedSection = resolveSection(documentId, originalQuestion, rewrittenQuestion);
        }
        return buildDecision(
            ExecutionMode.RETRIEVAL,
            itemIndex != null ? DocumentNavigationAction.ITEM_REFERENCE : DocumentNavigationAction.FRESH_TOPIC,
            assistedSection,
            itemIndex,
            retrievalPlan,
            assistedSection == null
                ? "普通文档问题走混合检索"
                : "结构线索仅作为软提示辅助混合检索"
        );
    }

    private DocumentNavigationDecision buildDecision(ExecutionMode mode,
                                                     DocumentNavigationAction action,
                                                     GraphSection section,
                                                     Integer itemIndex,
                                                     RetrievalQuestionPlan retrievalPlan,
                                                     String reason) {
        ConversationStructureAnchor structureAnchor = section == null
            ? ConversationStructureAnchor.builder().scopeMode(mode == ExecutionMode.RETRIEVAL ? "NONE" : "GRAPH_UNRESOLVED").build()
            : ConversationStructureAnchor.builder()
                .rootSectionCode(section.getNodeCode())
                .rootSectionTitle(section.getTitle())
                .targetSectionHint(section.displayTitle())
                .structureNodeId(section.getNodeId())
                .canonicalPath(section.getCanonicalPath())
                .scopeMode(mode == ExecutionMode.RETRIEVAL ? "SOFT" : "GRAPH")
                .build();
        ConversationItemAnchor itemAnchor = itemIndex == null
            ? null
            : ConversationItemAnchor.builder().itemIndex(itemIndex).build();
        List<String> queryHints = buildQueryHints(retrievalPlan, section, itemIndex);
        String summaryText = "mode=" + mode.name()
            + "; reason=" + reason
            + "; section=" + (section == null ? "" : section.displayTitle())
            + "; itemIndex=" + (itemIndex == null ? "" : itemIndex);
        log.info("文档问答路由完成: mode={}, action={}, section='{}', itemIndex={}, reason='{}'",
            mode,
            action,
            section == null ? "" : section.displayTitle(),
            itemIndex,
            reason);
        return DocumentNavigationDecision.builder()
            .navigationAction(action)
            .executionMode(mode)
            .structureAnchor(structureAnchor)
            .itemAnchor(itemAnchor)
            .retrievalPlan(retrievalPlan)
            .summaryText(summaryText)
            .queryContextHints(queryHints)
            .softSectionHints(section == null ? List.of() : List.of(section.displayTitle()))
            .build();
    }

    /**
     * 统一判断当前问题在 route 阶段需要的多个意图维度，避免 route 同时散落多个关键词函数。
     */
    private DocumentQuestionIntentDecision detectQuestionIntent(String routeText,
                                                                String originalQuestion,
                                                                String rewrittenQuestion,
                                                                List<String> subQuestions) {
        String normalized = safeText(routeText);
        if (normalized.isBlank()) {
            return noQuestionIntent("问题为空，跳过路由意图判断。");
        }
        
        boolean itemLookup = looksExplicitItemQuestion(normalized);
        boolean analyticQuestion = looksAnalyticQuestion(normalized);
        boolean outlineQuestion = asksOutline(normalized);
        boolean contentQuestion = looksGraphOnlyExcludedContentQuestion(normalized);
        boolean structureHint = mentionsStructure(normalized) || hasGraphOnlyAnchor(normalized) || outlineQuestion;
        GraphOnlyIntentDecision graphOnlyIntent = noGraphOnlyIntent("本地规则未命中结构图直答意图。");
        
        boolean hasMultipleSubQuestions = subQuestions != null && subQuestions.size() > 1;
        boolean canTryGraphOnlyRules = !hasMultipleSubQuestions
            && !itemLookup
            && !contentQuestion
            && !(analyticQuestion && looksGraphOnlyBlockingAnalyticQuestion(normalized));
        if (canTryGraphOnlyRules) {
            graphOnlyIntent = detectGraphOnlyIntentByRules(normalized);
        }
        
        if (graphOnlyIntent.matched()) {
            return buildQuestionIntentDecision(
                graphOnlyIntent,
                analyticQuestion,
                outlineQuestion || graphOnlyIntent.action() == DocumentNavigationAction.CHILD_SECTION_DESCEND,
                itemLookup,
                true,
                contentQuestion,
                graphOnlyIntent.confidence(),
                graphOnlyIntent.reason(),
                graphOnlyIntent.source()
            );
        }
        
        DocumentQuestionIntentDecision localDecision = buildQuestionIntentDecision(
            graphOnlyIntent,
            analyticQuestion,
            outlineQuestion,
            itemLookup,
            structureHint,
            contentQuestion,
            0.65D,
            "本地路由意图规则判断完成。",
            "local-rules"
        );
       
        if (!shouldUseLlmQuestionIntent(normalized, subQuestions, localDecision)) {
            return localDecision;
        }
 
        return classifyQuestionIntentWithModel(originalQuestion, rewrittenQuestion, normalized, localDecision);
    }


    private GraphOnlyIntentDecision detectGraphOnlyIntentByRules(String question) {
        if (asksAdjacency(question)) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP,
                "命中明确相邻章节表达，结构型问题直接走图查询。",
                1.0D,
                "rule-adjacency-hint"
            );
        }
        boolean hasSectionCode = SECTION_CODE_PATTERN.matcher(question).find();
        boolean hasChineseSectionReference = CHINESE_SECTION_REFERENCE_PATTERN.matcher(question).find();
        boolean hasSectionReference = hasSectionCode || hasChineseSectionReference;
        boolean hasExplicitAdjacencyCue = containsAny(question, GRAPH_ONLY_EXPLICIT_ADJACENCY_HINTS);
        boolean hasAdjacencyAnswerTarget = containsAny(question, GRAPH_ONLY_ADJACENCY_ANSWER_HINTS);
        boolean hasAdjacencyIntentCue = hasExplicitAdjacencyCue || hasAdjacencyAnswerTarget;
        boolean sectionReferenceAdjacencyMatched = hasSectionReference && hasAdjacencyIntentCue;
        if (sectionReferenceAdjacencyMatched) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP,
                "命中章节编号与方向词组合，结构相邻关系问题走图查询。",
                0.92D,
                "rule-section-code-direction"
            );
        }
        boolean hasQuotedTitle = QUOTED_TEXT_PATTERN.matcher(question).find();
        boolean quotedTitleAdjacencyMatched = hasQuotedTitle && hasAdjacencyIntentCue;
        if (quotedTitleAdjacencyMatched) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP,
                "命中标题锚点与方向词组合，结构相邻关系问题走图查询。",
                0.9D,
                "rule-quoted-title-direction"
            );
        }
        if (containsAny(question, GRAPH_ONLY_PRONOUN_ANCHOR_HINTS)
            && containsAny(question, GRAPH_ONLY_DIRECTION_HINTS)
            && containsAny(question, GRAPH_ONLY_ADJACENCY_ANSWER_HINTS)) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP,
                "命中指代锚点、方向词和章节答案目标，结构相邻关系问题走图查询。",
                0.88D,
                "rule-pronoun-direction-answer"
            );
        }
        if (containsAny(question, GRAPH_ONLY_STRUCTURE_OBJECT_HINTS)
            && containsAny(question, GRAPH_ONLY_EXPLICIT_ADJACENCY_HINTS)) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP,
                "命中结构对象与方向关系组合，结构相邻关系问题走图查询。",
                0.86D,
                "rule-structure-direction"
            );
        }
        if (asksOutline(question)) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.CHILD_SECTION_DESCEND,
                "命中明确章节展开表达，结构型问题直接走图查询。",
                1.0D,
                "rule-outline-hint"
            );
        }
        if (hasGraphOnlyAnchor(question) && containsAny(question, GRAPH_ONLY_OUTLINE_ACTION_HINTS)) {
            return new GraphOnlyIntentDecision(
                true,
                DocumentNavigationAction.CHILD_SECTION_DESCEND,
                "命中章节锚点与目录展开动作，结构型问题直接走图查询。",
                0.86D,
                "rule-outline-action"
            );
        }
        return noGraphOnlyIntent("本地规则未命中结构图直答意图。");
    }
    
    private boolean looksExplicitItemQuestion(String question) {
        if (asksItemLookup(question)) {
            return true;
        }
        return resolveExplicitItemIndex(question) != null;
    }
    
    private boolean looksGraphOnlyBlockingAnalyticQuestion(String question) {
        return containsAny(question, GRAPH_ONLY_BLOCKING_ANALYTIC_HINTS);
    }
    
    private boolean looksGraphOnlyExcludedContentQuestion(String question) {
        return containsAny(question, GRAPH_ONLY_CONTENT_HINTS);
    }
    
    private DocumentQuestionIntentDecision buildQuestionIntentDecision(GraphOnlyIntentDecision graphOnlyIntent,
                                                                       boolean analytic,
                                                                       boolean outline,
                                                                       boolean itemLookup,
                                                                       boolean structureHint,
                                                                       boolean contentQuestion,
                                                                       double confidence,
                                                                       String reason,
                                                                       String source) {
        boolean effectiveStructureHint = structureHint || (graphOnlyIntent != null && graphOnlyIntent.matched());
        boolean graphOnlyOutline = graphOnlyIntent != null
            && graphOnlyIntent.action() == DocumentNavigationAction.CHILD_SECTION_DESCEND;
        return new DocumentQuestionIntentDecision(
            graphOnlyIntent == null ? noGraphOnlyIntent("未提供 GRAPH_ONLY 判断结果。") : graphOnlyIntent,
            analytic,
            outline || graphOnlyOutline,
            itemLookup,
            effectiveStructureHint,
            contentQuestion,
            confidence,
            StrUtil.blankToDefault(reason, ""),
            StrUtil.blankToDefault(source, "")
        );
    }
    
    private boolean shouldUseLlmQuestionIntent(String question,
                                               List<String> subQuestions,
                                               DocumentQuestionIntentDecision localDecision) {
        boolean hasMultipleSubQuestions = subQuestions != null && subQuestions.size() > 1;
        if (hasMultipleSubQuestions) {
            return false;
        }
        boolean localDecisionAlreadyEvidenceBased = localDecision.itemLookup() || localDecision.contentQuestion();
        if (localDecisionAlreadyEvidenceBased) {
            return false;
        }
        boolean localDecisionIsStrongAnalytic = localDecision.analytic() && looksGraphOnlyBlockingAnalyticQuestion(question);
        if (localDecisionIsStrongAnalytic) {
            return false;
        }
        boolean hasStructuralNavigationClue = hasGraphOnlyAnchor(question) && hasGraphOnlyNavigationCue(question);
        if (hasStructuralNavigationClue) {
            return true;
        }
        return containsAny(question, ANALYTIC_WEAK_RELATION_HINTS)
            && localDecision.structureHint();
    }
    
    private boolean hasGraphOnlyAnchor(String question) {
        if (SECTION_CODE_PATTERN.matcher(question).find()) {
            return true;
        }
        if (CHINESE_SECTION_REFERENCE_PATTERN.matcher(question).find()) {
            return true;
        }
        if (QUOTED_TEXT_PATTERN.matcher(question).find()) {
            return true;
        }
        if (containsAny(question, GRAPH_ONLY_STRUCTURE_OBJECT_HINTS)) {
            return true;
        }
        return containsAny(question, GRAPH_ONLY_PRONOUN_ANCHOR_HINTS);
    }
    
    private boolean hasGraphOnlyNavigationCue(String question) {
        if (containsAny(question, GRAPH_ONLY_DIRECTION_HINTS)) {
            return true;
        }
        return containsAny(question, GRAPH_ONLY_OUTLINE_ACTION_HINTS);
    }
    
    private DocumentQuestionIntentDecision classifyQuestionIntentWithModel(String originalQuestion,
                                                                           String rewrittenQuestion,
                                                                           String routeText,
                                                                           DocumentQuestionIntentDecision localDecision) {
        try {
            // prompt 模板仍然复用 document-graph-only-intent，只是扩展为路由意图 JSON，避免新增第二次 LLM 调用。
            String prompt = promptTemplateService.render(PromptTemplateNames.DOCUMENT_GRAPH_ONLY_INTENT, Map.of(
                "originalQuestion", StrUtil.blankToDefault(originalQuestion, ""),
                "rewrittenQuestion", StrUtil.blankToDefault(rewrittenQuestion, ""),
                "routeText", StrUtil.blankToDefault(routeText, "")
            ));
            String raw = observedChatModelService.callText(
                "document_question_intent",
                null,
                prompt,
                buildGraphOnlyIntentCallOptions(),
                null
            );
            DocumentQuestionIntentDecision decision = parseQuestionIntentResult(raw, localDecision);
            log.info("文档路由 LLM 兜底判断完成: graphOnly={}, action={}, analytic={}, outline={}, itemLookup={}, structureHint={}, confidence={}, source={}, reason={}, raw={}",
                decision.graphOnlyIntent().matched(),
                decision.graphOnlyIntent().action(),
                decision.analytic(),
                decision.outline(),
                decision.itemLookup(),
                decision.structureHint(),
                decision.confidence(),
                decision.source(),
                decision.reason(),
                StrUtil.blankToDefault(raw, ""));
            return decision;
        }
        catch (Exception exception) {
            log.warn("文档路由 LLM 兜底判断失败，回退本地路由意图: question='{}', message={}",
                routeText,
                exception.getMessage());
            return localDecision;
        }
    }


    private ChatOptions buildGraphOnlyIntentCallOptions() {
        return OpenAiChatOptions.builder()
            .temperature(0.0D)
            .topP(0.1D)
            .extraBody(Map.of("thinking", false))
            .build();
    }


    private DocumentQuestionIntentDecision parseQuestionIntentResult(String raw,
                                                                     DocumentQuestionIntentDecision localDecision) {
        if (StrUtil.isBlank(raw)) {
            return localDecision;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(raw));
            String intentType = root.path("intent_type").asText("").trim().toUpperCase(Locale.ROOT);
            double confidence = normalizeConfidence(root.path("confidence").asDouble(0D));
            if (confidence < GRAPH_ONLY_INTENT_CONFIDENCE_THRESHOLD) {
                return localDecision;
            }
            String reason = StrUtil.blankToDefault(root.path("reason").asText(""), "LLM 判定完成。");
            boolean graphOnly = root.path("graph_only").asBoolean(false);
            DocumentNavigationAction action = resolveModelGraphOnlyAction(root.path("action").asText(""), intentType);
            boolean analytic = root.path("analytic").asBoolean("ANALYTIC".equals(intentType));
            boolean outline = root.path("outline").asBoolean("OUTLINE".equals(intentType));
            boolean itemLookup = root.path("item_lookup").asBoolean("ITEM_LOOKUP".equals(intentType));
            boolean contentQuestion = root.path("content_qa").asBoolean("CONTENT_QA".equals(intentType));
            boolean structureHint = root.path("structure_hint").asBoolean(
                localDecision.structureHint() || "ADJACENCY".equals(intentType) || "OUTLINE".equals(intentType)
            );
            boolean modelGraphOnlyAccepted = graphOnly && action != null
                && ("ADJACENCY".equals(intentType) || "OUTLINE".equals(intentType));
            GraphOnlyIntentDecision graphOnlyIntent = modelGraphOnlyAccepted
                ? new GraphOnlyIntentDecision(
                    true,
                    action,
                    "LLM 兜底判定为结构图直答: " + reason,
                    confidence,
                    "llm-" + intentType
                )
                : noGraphOnlyIntent("LLM 判定不适合结构图直答: " + reason);
            return buildQuestionIntentDecision(
                graphOnlyIntent,
                analytic,
                outline,
                itemLookup,
                structureHint,
                contentQuestion,
                confidence,
                "LLM 兜底路由意图判断: " + reason,
                "llm-" + intentType
            );
        }
        catch (Exception exception) {
            log.warn("解析文档路由 LLM 输出失败: raw='{}', message={}", raw, exception.getMessage());
            return localDecision;
        }
    }
    
    private DocumentNavigationAction resolveModelGraphOnlyAction(String rawAction, String intentType) {
        String action = StrUtil.blankToDefault(rawAction, "").trim().toUpperCase(Locale.ROOT);
        if (DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP.name().equals(action)) {
            return DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP;
        }
        if (DocumentNavigationAction.CHILD_SECTION_DESCEND.name().equals(action)) {
            return DocumentNavigationAction.CHILD_SECTION_DESCEND;
        }
        if ("ADJACENCY".equals(intentType)) {
            return DocumentNavigationAction.SECTION_ADJACENCY_LOOKUP;
        }
        if ("OUTLINE".equals(intentType)) {
            return DocumentNavigationAction.CHILD_SECTION_DESCEND;
        }
        return null;
    }
    
    private String extractJsonObject(String raw) {
        // 正常情况下 prompt 已要求只返回 JSON；这里是为了防御模型偶发的格式漂移。
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(raw.trim());
        if (matcher.find()) {
            return matcher.group();
        }
        return raw.trim();
    }
    
    private double normalizeConfidence(double confidence) {
        // 有些模型可能输出 86 表示 86%，这里统一折算成 0.86。
        if (confidence > 1D) {
            return confidence / 100D;
        }
        return confidence;
    }
    
    private boolean containsAny(String text, List<String> candidates) {
        String normalized = safeText(text);
        if (normalized.isBlank() || candidates == null || candidates.isEmpty()) {
            return false;
        }
        return candidates.stream()
            .filter(StrUtil::isNotBlank)
            .anyMatch(normalized::contains);
    }
    
    private GraphOnlyIntentDecision noGraphOnlyIntent(String reason) {
        return new GraphOnlyIntentDecision(false, null, StrUtil.blankToDefault(reason, ""), 0D, "none");
    }
    
    private DocumentQuestionIntentDecision noQuestionIntent(String reason) {
        return buildQuestionIntentDecision(
            noGraphOnlyIntent(reason),
            false,
            false,
            false,
            false,
            false,
            0D,
            reason,
            "none"
        );
    }

    private GraphSection resolveSection(Long documentId, String originalQuestion, String rewrittenQuestion) {
        if (documentId == null) {
            return null;
        }
        GraphSection byCode = resolveBySectionCode(documentId, originalQuestion, rewrittenQuestion);
        if (byCode != null) {
            return byCode;
        }

        GraphSection indexedMatch = resolveByNavigationIndex(documentId, originalQuestion, rewrittenQuestion);
        if (indexedMatch != null) {
            return indexedMatch;
        }
        List<String> phrases = buildSectionPhrases(originalQuestion, rewrittenQuestion);
        GraphSection localMatch = resolveByLocalStructure(documentId, phrases);
        if (localMatch != null) {
            return localMatch;
        }
        return graphService.findBestSection(documentId, rewrittenQuestion, "");
    }

    private GraphSection resolveBySectionCode(Long documentId, String originalQuestion, String rewrittenQuestion) {
        Matcher matcher = SECTION_CODE_PATTERN.matcher((safeText(originalQuestion) + " " + safeText(rewrittenQuestion)).trim());
        while (matcher.find()) {
            GraphSection section = graphService.findSectionByCode(documentId, matcher.group(1));
            if (section != null) {
                return section;
            }
        }
        return null;
    }

    private GraphSection resolveByLocalStructure(Long documentId, List<String> phrases) {
        if (phrases.isEmpty()) {
            return null;
        }
        List<GraphSection> sections = graphService.listSections(documentId);
        if (sections == null || sections.isEmpty()) {
            return null;
        }
        return sections.stream()
            .map(section -> new SectionScore(section, scoreSection(section, phrases)))
            .filter(score -> score.score() >= 45D)
            .max(Comparator.comparingDouble(SectionScore::score))
            .map(SectionScore::section)
            .orElse(null);
    }

    private GraphSection resolveByNavigationIndex(Long documentId, String originalQuestion, String rewrittenQuestion) {
        DocumentNavigationIndexService navigationIndexService = navigationIndexServiceProvider.getIfAvailable();
        if (navigationIndexService == null) {
            return null;
        }
        String query = firstNonBlank(rewrittenQuestion, originalQuestion);
        List<DocumentNavigationIndexService.NavigationSectionHit> hits = navigationIndexService.searchSections(
            documentId,
            query,
            detectFacet(query),
            "",
            query,
            5
        );
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        return graphService.findSectionById(documentId, hits.get(0).nodeId());
    }

    private double scoreSection(GraphSection section, List<String> phrases) {
        String title = normalize(section.getTitle());
        String path = normalize(section.getSectionPath());
        String anchor = normalize(section.getAnchorText());
        String content = normalize(section.getContentText());
        double best = 0D;
        for (String phrase : phrases) {
            String normalized = normalize(phrase);
            if (normalized.length() < 2) {
                continue;
            }
            if (path.contains(normalized)) {
                best = Math.max(best, 100D + normalized.length());
            }
            if (title.contains(normalized)) {
                best = Math.max(best, 90D + normalized.length());
            }
            if (anchor.contains(normalized)) {
                best = Math.max(best, 80D + normalized.length());
            }
            if (content.contains(normalized)) {
                best = Math.max(best, 45D + Math.min(normalized.length(), 20));
            }
        }
        return best;
    }

    private List<String> buildSectionPhrases(String originalQuestion, String rewrittenQuestion) {
        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        addCleanPhrase(phrases, originalQuestion);
        addCleanPhrase(phrases, rewrittenQuestion);
        addQuotedPhrases(phrases, originalQuestion);
        addQuotedPhrases(phrases, rewrittenQuestion);
        for (String marker : ADJACENCY_HINTS) {
            addTextBefore(phrases, originalQuestion, marker);
            addTextBefore(phrases, rewrittenQuestion, marker);
        }
        for (String marker : OUTLINE_HINTS) {
            addTextBefore(phrases, originalQuestion, marker);
            addTextBefore(phrases, rewrittenQuestion, marker);
        }
        Matcher stepMatcher = STEP_REFERENCE_PATTERN.matcher(safeText(originalQuestion) + " " + safeText(rewrittenQuestion));
        while (stepMatcher.find()) {
            String all = stepMatcher.group();
            addTextBefore(phrases, originalQuestion, all);
            addTextBefore(phrases, rewrittenQuestion, all);
        }
        return new ArrayList<>(phrases).stream()
            .filter(item -> normalize(item).length() >= 2)
            .limit(8)
            .toList();
    }

    private void addTextBefore(LinkedHashSet<String> phrases, String text, String marker) {
        String normalized = safeText(text);
        if (normalized.isBlank() || marker == null || marker.isBlank()) {
            return;
        }
        int index = normalized.indexOf(marker);
        if (index > 0) {
            addCleanPhrase(phrases, normalized.substring(0, index));
        }
    }

    private void addQuotedPhrases(LinkedHashSet<String> phrases, String text) {
        Matcher matcher = QUOTED_TEXT_PATTERN.matcher(safeText(text));
        while (matcher.find()) {
            addCleanPhrase(phrases, matcher.group(1));
        }
    }

    private void addCleanPhrase(LinkedHashSet<String> phrases, String text) {
        String cleaned = cleanPhrase(text);
        if (StrUtil.isNotBlank(cleaned)) {
            phrases.add(cleaned);
        }
    }

    private String cleanPhrase(String text) {
        return safeText(text)
            .replace("刚才说的", "")
            .replace("请问", "")
            .replace("帮我", "")
            .replace("这个", "")
            .replace("那个", "")
            .replace("所属的具体章节", "")
            .replace("所属章节", "")
            .replace("具体章节", "")
            .replace("章节", "")
            .replace("小节", "")
            .replace("目录", "")
            .replace("上一节", "")
            .replace("下一节", "")
            .replace("分别是什么", "")
            .replace("是什么", "")
            .replace("有哪些", "")
            .replace("都有哪些", "")
            .replace("包含哪些", "")
            .replace("中的", "")
            .replace("里面的", "")
            .replace("里的", "")
            .replace("中", "")
            .replace("“", "")
            .replace("”", "")
            .replace("?", "")
            .replace("？", "")
            .trim();
    }

    private boolean asksAdjacency(String question) {
        return ADJACENCY_HINTS.stream().anyMatch(question::contains);
    }

    private boolean asksOutline(String question) {
        String normalized = safeText(question);
        if (normalized.isBlank()) {
            return false;
        }
        if (looksGraphOnlyExcludedContentQuestion(normalized)) {
            return false;
        }
        boolean hasExplicitOutlineExpression = containsAny(normalized, OUTLINE_EXPLICIT_HINTS);
        boolean hasStructureAnchor = hasGraphOnlyAnchor(normalized);
        boolean hasOutlineAction = containsAny(normalized, GRAPH_ONLY_OUTLINE_ACTION_HINTS);
        return hasExplicitOutlineExpression || (hasStructureAnchor && hasOutlineAction);
    }

    private boolean asksItemLookup(String question) {
        return ITEM_HINTS.stream().anyMatch(question::contains);
    }

    private boolean looksAnalyticQuestion(String question) {
        String normalized = safeText(question);
        if (normalized.isBlank()) {
            return false;
        }
        if (containsAny(normalized, ANALYTIC_STRONG_HINTS)) {
            return true;
        }
        if (!containsAny(normalized, ANALYTIC_WEAK_RELATION_HINTS)) {
            return false;
        }
        return !looksStructuralRelationQuestion(normalized);
    }

    private boolean looksStructuralRelationQuestion(String question) {
        String normalized = safeText(question);
        if (containsAny(normalized, STRUCTURAL_RELATION_HINTS)) {
            return true;
        }
        if (!hasGraphOnlyAnchor(normalized)) {
            return false;
        }
        return containsAny(normalized, GRAPH_ONLY_EXPLICIT_ADJACENCY_HINTS)
            || containsAny(normalized, GRAPH_ONLY_DIRECTION_HINTS);
    }

    private boolean mentionsStructure(String question) {
        String normalized = safeText(question);
        return normalized.contains("章节")
            || normalized.contains("小节")
            || normalized.contains("条目")
            || normalized.contains("步骤")
            || normalized.contains("项")
            || QUOTED_TEXT_PATTERN.matcher(normalized).find()
            || SECTION_CODE_PATTERN.matcher(normalized).find();
    }

    private Integer resolveExplicitItemIndex(String question) {
        Matcher stepMatcher = STEP_REFERENCE_PATTERN.matcher(safeText(question));
        if (stepMatcher.find()) {
            return parseChineseNumber(stepMatcher.group(1));
        }
        Matcher ordinalMatcher = ORDINAL_REFERENCE_PATTERN.matcher(safeText(question));
        if (ordinalMatcher.find()) {
            return parseChineseNumber(ordinalMatcher.group(1));
        }
        return null;
    }

    private Integer parseChineseNumber(String text) {
        String normalized = safeText(text);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(normalized);
        }
        Map<Character, Integer> digitMap = Map.of(
            '一', 1, '二', 2, '三', 3, '四', 4, '五', 5,
            '六', 6, '七', 7, '八', 8, '九', 9
        );
        if ("十".equals(normalized)) {
            return 10;
        }
        if (normalized.startsWith("十") && normalized.length() == 2) {
            return 10 + digitMap.getOrDefault(normalized.charAt(1), 0);
        }
        if (normalized.endsWith("十") && normalized.length() == 2) {
            return digitMap.getOrDefault(normalized.charAt(0), 0) * 10;
        }
        if (normalized.contains("十") && normalized.length() == 3) {
            return digitMap.getOrDefault(normalized.charAt(0), 0) * 10 + digitMap.getOrDefault(normalized.charAt(2), 0);
        }
        return digitMap.getOrDefault(normalized.charAt(0), null);
    }

    private List<String> normalizeSubQuestions(RagRewriteResult rewriteResult, String fallbackQuestion) {
        if (rewriteResult == null || rewriteResult.getSubQuestions() == null || rewriteResult.getSubQuestions().isEmpty()) {
            return List.of(fallbackQuestion);
        }
        return rewriteResult.getSubQuestions().stream()
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private List<String> extractQueryHints(String question) {
        String normalized = safeText(question);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("[\\s、，,；;：:（）()\\-的和及与或]+"))
            .map(String::trim)
            .filter(item -> item.length() >= 2)
            .distinct()
            .limit(6)
            .toList();
    }

    private List<String> buildQueryHints(RetrievalQuestionPlan retrievalPlan,
                                         GraphSection section,
                                         Integer itemIndex) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        if (retrievalPlan != null) {
            hints.addAll(extractQueryHints(retrievalPlan.getRetrievalQuestion()));
        }
        if (section != null) {
            addHint(hints, section.displayTitle());
            addHint(hints, section.getTitle());
            addHint(hints, section.getNodeCode());
        }
        if (itemIndex != null) {
            addHint(hints, "第" + itemIndex + "步");
            addHint(hints, "第" + itemIndex + "项");
        }
        return hints.stream()
            .filter(StrUtil::isNotBlank)
            .limit(10)
            .toList();
    }

    private void addHint(LinkedHashSet<String> hints, String hint) {
        String normalized = safeText(hint);
        if (normalized.isBlank()) {
            return;
        }
        hints.add(normalized);
    }

    private String detectFacet(String question) {
        if (asksAdjacency(question)) {
            return "章节位置";
        }
        if (asksOutline(question)) {
            return "章节";
        }
        if (asksItemLookup(question)) {
            return "步骤";
        }
        return "";
    }

    private String normalize(String text) {
        return safeText(text).replaceAll("[\\s>`*#_\\-，,。；;：:（）()“”\"']+", "").toLowerCase();
    }

    private String firstNonBlank(String left, String right) {
        if (StrUtil.isNotBlank(left)) {
            return left.trim();
        }
        return StrUtil.blankToDefault(right, "");
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
