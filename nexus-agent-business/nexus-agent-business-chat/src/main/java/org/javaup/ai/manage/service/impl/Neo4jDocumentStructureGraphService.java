package org.javaup.ai.manage.service.impl;

import lombok.AllArgsConstructor;
import cn.hutool.core.util.StrUtil;
import org.javaup.ai.manage.config.DocumentManageProperties;
import org.javaup.ai.manage.model.graph.GraphItem;
import org.javaup.ai.manage.model.graph.GraphSection;
import org.javaup.ai.manage.service.DocumentStructureGraphService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务实现层
 * @author: 阿星不是程序员
 **/

@AllArgsConstructor
@Service
@ConditionalOnBean(name = "documentManageNeo4jDriver")
public class Neo4jDocumentStructureGraphService implements DocumentStructureGraphService {

    private final Driver driver;
    private final DocumentManageProperties properties;

    @Override
    public boolean isGraphAvailable(Long documentId) {
        if (documentId == null) {
            return false;
        }
        try (Session session = openSession()) {
            return session.run("MATCH (d:Document {documentId: $documentId}) RETURN count(d) > 0 AS available",
                    Values.parameters("documentId", documentId))
                .single()
                .get("available")
                .asBoolean(false);
        }
        catch (Exception exception) {
            return false;
        }
    }

    @Override
    public GraphSection findSectionById(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (s:Section {documentId: $documentId, nodeId: $nodeId})
                    RETURN s
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "nodeId", sectionNodeId))
                .list(record -> toGraphSection(record.get("s").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphSection findSectionByCode(Long documentId, String nodeCode) {
        if (documentId == null || StrUtil.isBlank(nodeCode)) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (s:Section {documentId: $documentId, nodeCode: $nodeCode})
                    RETURN s
                    ORDER BY s.nodeNo ASC
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "nodeCode", nodeCode.trim()))
                .list(record -> toGraphSection(record.get("s").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphSection findSectionByTitle(Long documentId, String title) {
        if (documentId == null || StrUtil.isBlank(title)) {
            return null;
        }
        String normalized = normalize(title);
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (s:Section {documentId: $documentId})
                    WHERE s.normalizedTitle = $normalized OR s.normalizedPath = $normalized
                    RETURN s
                    ORDER BY s.nodeNo ASC
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "normalized", normalized))
                .list(record -> toGraphSection(record.get("s").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphSection findSectionByCanonicalPath(Long documentId, String canonicalPath) {
        if (documentId == null || StrUtil.isBlank(canonicalPath)) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (s:Section {documentId: $documentId, canonicalPath: $canonicalPath})
                    RETURN s
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "canonicalPath", canonicalPath.trim()))
                .list(record -> toGraphSection(record.get("s").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphSection findBestSection(Long documentId, String topic, String facet) {
        String normalizedTopic = normalize(topic);
        String normalizedFacet = normalize(facet);
        if (documentId == null || (normalizedTopic.isBlank() && normalizedFacet.isBlank())) {
            return null;
        }
        GraphSection bestSection = null;
        int bestScore = 0;
        for (GraphSection section : listSections(documentId)) {
            int score = 0;
            String sectionPath = normalize(section.getSectionPath());
            String title = normalize(section.getTitle());
            String anchorText = normalize(section.getAnchorText());
            String content = normalize(section.getContentText());
            if (StrUtil.isNotBlank(normalizedTopic)) {
                if (title.contains(normalizedTopic) || sectionPath.contains(normalizedTopic)) {
                    score += 8;
                }
                else if (anchorText.contains(normalizedTopic)) {
                    score += 6;
                }
                else if (content.contains(normalizedTopic)) {
                    score += 2;
                }
            }
            if (StrUtil.isNotBlank(normalizedFacet)) {
                if (title.contains(normalizedFacet) || sectionPath.contains(normalizedFacet)) {
                    score += 5;
                }
                else if (content.contains(normalizedFacet)) {
                    score += 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestSection = section;
            }
        }
        return bestScore > 0 ? bestSection : null;
    }

    @Override
    public List<GraphSection> listSections(Long documentId) {
        if (documentId == null) {
            return List.of();
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (s:Section {documentId: $documentId})
                    RETURN s
                    ORDER BY s.nodeNo ASC
                    """,
                    Values.parameters("documentId", documentId))
                .list(record -> toGraphSection(record.get("s").asNode()));
        }
    }

    @Override
    public List<GraphSection> listChildren(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return List.of();
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (:Section {documentId: $documentId, nodeId: $nodeId})-[:HAS_CHILD]->(c:Section {documentId: $documentId})
                    RETURN c
                    ORDER BY c.nodeNo ASC
                    """,
                    Values.parameters("documentId", documentId, "nodeId", sectionNodeId))
                .list(record -> toGraphSection(record.get("c").asNode()));
        }
    }

    @Override
    public GraphSection parentSection(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (p:Section {documentId: $documentId})-[:HAS_CHILD]->(:Section {documentId: $documentId, nodeId: $nodeId})
                    RETURN p
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "nodeId", sectionNodeId))
                .list(record -> toGraphSection(record.get("p").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphSection previousSibling(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (p:Section {documentId: $documentId})-[:NEXT_SIBLING]->(:Section {documentId: $documentId, nodeId: $nodeId})
                    RETURN p
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "nodeId", sectionNodeId))
                .list(record -> toGraphSection(record.get("p").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphSection nextSibling(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (:Section {documentId: $documentId, nodeId: $nodeId})-[:NEXT_SIBLING]->(n:Section {documentId: $documentId})
                    RETURN n
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "nodeId", sectionNodeId))
                .list(record -> toGraphSection(record.get("n").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public GraphItem findItemByIndex(Long documentId, Long sectionNodeId, Integer itemIndex) {
        if (documentId == null || sectionNodeId == null || itemIndex == null) {
            return null;
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (:Section {documentId: $documentId, nodeId: $sectionNodeId})-[:HAS_ITEM]->(i:Item {documentId: $documentId, itemIndex: $itemIndex})
                    RETURN i
                    ORDER BY i.nodeNo ASC
                    LIMIT 1
                    """,
                    Values.parameters("documentId", documentId, "sectionNodeId", sectionNodeId, "itemIndex", itemIndex))
                .list(record -> toGraphItem(record.get("i").asNode()))
                .stream()
                .findFirst()
                .orElse(null);
        }
    }

    @Override
    public List<GraphItem> listItems(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return List.of();
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (:Section {documentId: $documentId, nodeId: $sectionNodeId})-[:HAS_ITEM]->(i:Item {documentId: $documentId})
                    RETURN i
                    ORDER BY i.nodeNo ASC
                    """,
                    Values.parameters("documentId", documentId, "sectionNodeId", sectionNodeId))
                .list(record -> toGraphItem(record.get("i").asNode()));
        }
    }

    @Override
    public List<GraphItem> searchItemsInSection(Long documentId, Long sectionNodeId, String keyword) {
        if (documentId == null || sectionNodeId == null) {
            return List.of();
        }
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isBlank()) {
            return listItems(documentId, sectionNodeId);
        }
        try (Session session = openSession()) {
            return session.run("""
                    MATCH (:Section {documentId: $documentId, nodeId: $sectionNodeId})-[:HAS_ITEM]->(i:Item {documentId: $documentId})
                    WHERE i.normalizedTitle CONTAINS $keyword OR toLower(coalesce(i.contentText, '')) CONTAINS $keyword
                    RETURN i
                    ORDER BY i.nodeNo ASC
                    """,
                    Values.parameters("documentId", documentId, "sectionNodeId", sectionNodeId, "keyword", normalizedKeyword))
                .list(record -> toGraphItem(record.get("i").asNode()));
        }
    }

    private Session openSession() {
        return driver.session(SessionConfig.forDatabase(properties.getNeo4j().getDatabase()));
    }

    private GraphSection toGraphSection(org.neo4j.driver.types.Node node) {
        if (node == null) {
            return null;
        }
        return GraphSection.builder()
            .nodeId(asLong(node, "nodeId"))
            .documentId(asLong(node, "documentId"))
            .parseTaskId(asLong(node, "parseTaskId"))
            .nodeNo(asInteger(node, "nodeNo"))
            .depth(asInteger(node, "depth"))
            .parentNodeId(asLong(node, "parentNodeId"))
            .prevSiblingNodeId(asLong(node, "prevSiblingNodeId"))
            .nextSiblingNodeId(asLong(node, "nextSiblingNodeId"))
            .nodeCode(asText(node, "nodeCode"))
            .title(asText(node, "title"))
            .anchorText(asText(node, "anchorText"))
            .sectionPath(asText(node, "sectionPath"))
            .canonicalPath(asText(node, "canonicalPath"))
            .contentText(asText(node, "contentText"))
            .build();
    }

    private GraphItem toGraphItem(org.neo4j.driver.types.Node node) {
        if (node == null) {
            return null;
        }
        return GraphItem.builder()
            .nodeId(asLong(node, "nodeId"))
            .documentId(asLong(node, "documentId"))
            .parseTaskId(asLong(node, "parseTaskId"))
            .nodeNo(asInteger(node, "nodeNo"))
            .nodeType(asText(node, "nodeType"))
            .sectionNodeId(asLong(node, "sectionNodeId"))
            .prevSiblingNodeId(asLong(node, "prevSiblingNodeId"))
            .nextSiblingNodeId(asLong(node, "nextSiblingNodeId"))
            .title(asText(node, "title"))
            .anchorText(asText(node, "anchorText"))
            .sectionPath(asText(node, "sectionPath"))
            .canonicalPath(asText(node, "canonicalPath"))
            .contentText(asText(node, "contentText"))
            .itemIndex(asInteger(node, "itemIndex"))
            .build();
    }

    private Long asLong(org.neo4j.driver.types.Node node, String key) {
        return node.containsKey(key) && !node.get(key).isNull() ? node.get(key).asLong() : null;
    }

    private Integer asInteger(org.neo4j.driver.types.Node node, String key) {
        return node.containsKey(key) && !node.get(key).isNull() ? node.get(key).asInt() : null;
    }

    private String asText(org.neo4j.driver.types.Node node, String key) {
        return node.containsKey(key) && !node.get(key).isNull() ? node.get(key).asString("") : "";
    }

    private String normalize(String text) {
        return StrUtil.blankToDefault(text, "")
            .replaceAll("[\\s>`*#_\\-]+", "")
            .toLowerCase(Locale.ROOT);
    }
}
