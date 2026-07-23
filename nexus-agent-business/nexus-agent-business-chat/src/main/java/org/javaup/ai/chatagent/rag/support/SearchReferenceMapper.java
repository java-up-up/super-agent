package org.javaup.ai.chatagent.rag.support;

import org.javaup.ai.chatagent.model.SearchReference;
import org.javaup.ai.manage.support.DocumentKnowledgeMetadataKeys;
import org.springframework.ai.document.Document;

import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: Mapper层
 * @author: 阿星不是程序员
 **/

public final class SearchReferenceMapper {

    private SearchReferenceMapper() {
    }

    public static SearchReference fromDocument(Document document,
                                               int subQuestionIndex,
                                               String subQuestion,
                                               int referenceNumber) {
        Map<String, Object> metadata = document.getMetadata();
        String sourceType = asText(metadata.get(DocumentKnowledgeMetadataKeys.SOURCE_TYPE), "DOCUMENT");
        SearchReference reference = new SearchReference();
        reference.setReferenceId(String.valueOf(referenceNumber));
        reference.setSourceType(sourceType);
        reference.setSnippet(document.getText());
        reference.setSubQuestionIndex(subQuestionIndex);
        reference.setSubQuestion(subQuestion);
        reference.setChannel(asText(metadata.get(DocumentKnowledgeMetadataKeys.CHANNEL), "vector"));
        reference.setScore(asDouble(metadata.get(DocumentKnowledgeMetadataKeys.SCORE)));

        if ("WEB".equalsIgnoreCase(sourceType)) {
            reference.setTitle(asText(metadata.get(DocumentKnowledgeMetadataKeys.TITLE), "网页来源"));
            reference.setUrl(asText(metadata.get(DocumentKnowledgeMetadataKeys.URL), ""));
            reference.setToolName(asText(metadata.get(DocumentKnowledgeMetadataKeys.TOOL_NAME), "tavily_search"));
            return reference;
        }

        reference.setTitle(asText(metadata.get(DocumentKnowledgeMetadataKeys.DOCUMENT_NAME), "文档片段"));
        reference.setDocumentId(asLong(metadata.get(DocumentKnowledgeMetadataKeys.DOCUMENT_ID)));
        reference.setDocumentName(asText(metadata.get(DocumentKnowledgeMetadataKeys.DOCUMENT_NAME), ""));
        reference.setParentBlockId(asLong(metadata.get(DocumentKnowledgeMetadataKeys.PARENT_BLOCK_ID)));
        reference.setParentBlockNo(asInteger(metadata.get(DocumentKnowledgeMetadataKeys.PARENT_BLOCK_NO)));
        reference.setChunkId(asLong(metadata.get(DocumentKnowledgeMetadataKeys.CHUNK_ID)));
        reference.setChunkNo(asInteger(metadata.get(DocumentKnowledgeMetadataKeys.CHUNK_NO)));
        reference.setSectionPath(asText(metadata.get(DocumentKnowledgeMetadataKeys.SECTION_PATH), ""));
        reference.setStructureNodeId(asLong(metadata.get(DocumentKnowledgeMetadataKeys.STRUCTURE_NODE_ID)));
        reference.setStructureNodeType(asInteger(metadata.get(DocumentKnowledgeMetadataKeys.STRUCTURE_NODE_TYPE)));
        reference.setCanonicalPath(asText(metadata.get(DocumentKnowledgeMetadataKeys.CANONICAL_PATH), ""));
        reference.setItemIndex(asInteger(metadata.get(DocumentKnowledgeMetadataKeys.ITEM_INDEX)));
        reference.setKnowledgeScopeCode(asText(metadata.get(DocumentKnowledgeMetadataKeys.KNOWLEDGE_SCOPE_CODE), ""));
        reference.setKnowledgeScopeName(asText(metadata.get(DocumentKnowledgeMetadataKeys.KNOWLEDGE_SCOPE_NAME), ""));
        return reference;
    }

    private static String asText(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
