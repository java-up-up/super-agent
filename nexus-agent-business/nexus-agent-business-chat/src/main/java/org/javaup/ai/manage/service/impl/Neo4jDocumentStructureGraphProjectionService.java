package org.javaup.ai.manage.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.manage.config.DocumentManageProperties;
import org.javaup.ai.manage.data.SuperAgentDocument;
import org.javaup.ai.manage.data.SuperAgentDocumentStructureNode;
import org.javaup.ai.manage.data.SuperAgentDocumentTask;
import org.javaup.ai.manage.mapper.SuperAgentDocumentMapper;
import org.javaup.ai.manage.mapper.SuperAgentDocumentTaskMapper;
import org.javaup.ai.manage.service.DocumentStructureGraphProjectionService;
import org.javaup.ai.manage.service.DocumentStructureNodeService;
import org.javaup.enums.DocumentStructureNodeTypeEnum;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务实现层
 * @author: 阿星不是程序员
 **/

@Slf4j
@AllArgsConstructor
@Service
@ConditionalOnBean(name = "documentManageNeo4jDriver")
public class Neo4jDocumentStructureGraphProjectionService implements DocumentStructureGraphProjectionService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Driver driver;
    private final DocumentManageProperties properties;
    private final DocumentStructureNodeService documentStructureNodeService;
    private final SuperAgentDocumentMapper documentMapper;
    private final SuperAgentDocumentTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        initializeSchema();
    }

    @Override
    public boolean enabled() {
        return Boolean.TRUE.equals(properties.getNeo4j().getEnabled());
    }

    @Override
    public void projectToGraph(Long documentId, Long parseTaskId) {
        if (!enabled() || documentId == null) {
            log.info("跳过 Neo4j 结构图投影: enabled={}, documentId={}, parseTaskId={}", enabled(), documentId, parseTaskId);
            return;
        }
        log.info("开始 Neo4j 结构图投影: documentId={}, parseTaskId={}, database={}",
            documentId,
            parseTaskId,
            properties.getNeo4j().getDatabase());
        updateTaskGraphStatus(parseTaskId, "RUNNING", null);
        List<SuperAgentDocumentStructureNode> nodes = documentStructureNodeService.listDocumentNodes(documentId, null);
        SuperAgentDocument document = documentMapper.selectById(documentId);
        log.info("Neo4j 结构图投影读取结构节点: documentId={}, parseTaskId={}, nodeCount={}, documentName='{}'",
            documentId,
            parseTaskId,
            nodes.size(),
            document == null ? "" : StrUtil.blankToDefault(document.getDocumentName(), ""));
        try (Session session = openSession()) {
            session.executeWrite(tx -> {
                tx.run("""
                        MATCH (n)
                        WHERE (n:Document OR n:Section OR n:Item) AND n.documentId = $documentId
                        DETACH DELETE n
                        """,
                    Values.parameters("documentId", documentId));
                tx.run("""
                        CREATE (d:Document {
                          documentId: $documentId,
                          documentName: $documentName,
                          parseTaskId: $parseTaskId,
                          currentVersion: $parseTaskId
                        })
                        """,
                    Values.parameters(
                        "documentId", documentId,
                        "documentName", document == null ? "" : StrUtil.blankToDefault(document.getDocumentName(), ""),
                        "parseTaskId", parseTaskId
                    ));
                for (SuperAgentDocumentStructureNode node : nodes) {
                    if (node == null || node.getId() == null) {
                        continue;
                    }
                    DocumentStructureNodeTypeEnum nodeType = DocumentStructureNodeTypeEnum.getRc(node.getNodeType());
                    if (nodeType == DocumentStructureNodeTypeEnum.SECTION) {
                        tx.run("""
                                CREATE (s:Section {
                                  nodeId: $nodeId,
                                  documentId: $documentId,
                                  parseTaskId: $parseTaskId,
                                  nodeNo: $nodeNo,
                                  depth: $depth,
                                  parentNodeId: $parentNodeId,
                                  prevSiblingNodeId: $prevSiblingNodeId,
                                  nextSiblingNodeId: $nextSiblingNodeId,
                                  nodeCode: $nodeCode,
                                  title: $title,
                                  anchorText: $anchorText,
                                  sectionPath: $sectionPath,
                                  canonicalPath: $canonicalPath,
                                  contentText: $contentText,
                                  normalizedTitle: $normalizedTitle,
                                  normalizedPath: $normalizedPath
                                })
                                """, sectionParams(node));
                    }
                    else if (nodeType == DocumentStructureNodeTypeEnum.STEP || nodeType == DocumentStructureNodeTypeEnum.LIST_ITEM) {
                        tx.run("""
                                CREATE (i:Item {
                                  nodeId: $nodeId,
                                  documentId: $documentId,
                                  parseTaskId: $parseTaskId,
                                  nodeNo: $nodeNo,
                                  nodeType: $nodeType,
                                  sectionNodeId: $sectionNodeId,
                                  prevSiblingNodeId: $prevSiblingNodeId,
                                  nextSiblingNodeId: $nextSiblingNodeId,
                                  title: $title,
                                  anchorText: $anchorText,
                                  sectionPath: $sectionPath,
                                  canonicalPath: $canonicalPath,
                                  contentText: $contentText,
                                  itemIndex: $itemIndex,
                                  normalizedTitle: $normalizedTitle
                                })
                                """, itemParams(node, nodeType));
                    }
                }
                for (SuperAgentDocumentStructureNode node : nodes) {
                    if (node == null || node.getId() == null) {
                        continue;
                    }
                    DocumentStructureNodeTypeEnum nodeType = DocumentStructureNodeTypeEnum.getRc(node.getNodeType());
                    if (nodeType == DocumentStructureNodeTypeEnum.SECTION) {
                        if (node.getParentNodeId() == null) {
                            tx.run("""
                                    MATCH (d:Document {documentId: $documentId}), (s:Section {documentId: $documentId, nodeId: $nodeId})
                                    MERGE (d)-[:HAS_SECTION]->(s)
                                    MERGE (s)-[:BELONGS_TO_DOCUMENT]->(d)
                                    """,
                                Values.parameters("documentId", documentId, "nodeId", node.getId()));
                        }
                        else {
                            tx.run("""
                                    MATCH (p:Section {documentId: $documentId, nodeId: $parentNodeId}),
                                          (s:Section {documentId: $documentId, nodeId: $nodeId})
                                    MERGE (p)-[:HAS_CHILD]->(s)
                                    """,
                                Values.parameters("documentId", documentId, "parentNodeId", node.getParentNodeId(), "nodeId", node.getId()));
                        }
                        mergeSectionSiblingEdges(tx, node);
                    }
                    else if (nodeType == DocumentStructureNodeTypeEnum.STEP || nodeType == DocumentStructureNodeTypeEnum.LIST_ITEM) {
                        tx.run("""
                                MATCH (s:Section {documentId: $documentId, nodeId: $sectionNodeId}),
                                      (i:Item {documentId: $documentId, nodeId: $nodeId})
                                MERGE (s)-[:HAS_ITEM]->(i)
                                MERGE (i)-[:BELONGS_TO_SECTION]->(s)
                                """,
                            Values.parameters("documentId", documentId, "sectionNodeId", node.getParentNodeId(), "nodeId", node.getId()));
                        mergeItemSiblingEdges(tx, node);
                    }
                }
                return null;
            });
            updateTaskGraphStatus(parseTaskId, "SUCCESS", null);
            log.info("文档结构图已投影到 Neo4j: documentId={}, parseTaskId={}, nodeCount={}", documentId, parseTaskId, nodes.size());
        }
        catch (Exception exception) {
            updateTaskGraphStatus(parseTaskId, "FAILED", exception.getMessage());
            log.error("Neo4j 结构图投影失败: documentId={}, parseTaskId={}, database={}, error={}",
                documentId,
                parseTaskId,
                properties.getNeo4j().getDatabase(),
                exception.getMessage(),
                exception);
            throw new IllegalStateException("投影文档结构图到 Neo4j 失败", exception);
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!enabled() || documentId == null) {
            log.info("跳过删除 Neo4j 结构图: enabled={}, documentId={}", enabled(), documentId);
            return;
        }
        log.info("开始删除 Neo4j 结构图: documentId={}, database={}", documentId, properties.getNeo4j().getDatabase());
        try (Session session = openSession()) {
            session.executeWrite(tx -> {
                tx.run("""
                        MATCH (n)
                        WHERE (n:Document OR n:Section OR n:Item) AND n.documentId = $documentId
                        DETACH DELETE n
                        """,
                    Values.parameters("documentId", documentId));
                return null;
            });
            log.info("删除 Neo4j 结构图完成: documentId={}", documentId);
        }
    }

    private void initializeSchema() {
        if (!enabled()) {
            return;
        }
        try (Session session = openSession()) {
            log.info("初始化 Neo4j 结构图索引: database={}", properties.getNeo4j().getDatabase());
            session.run("CREATE INDEX document_document_id IF NOT EXISTS FOR (d:Document) ON (d.documentId)").consume();
            session.run("CREATE INDEX section_node_id IF NOT EXISTS FOR (s:Section) ON (s.nodeId)").consume();
            session.run("CREATE INDEX section_document_id IF NOT EXISTS FOR (s:Section) ON (s.documentId)").consume();
            session.run("CREATE INDEX section_document_node_id IF NOT EXISTS FOR (s:Section) ON (s.documentId, s.nodeId)").consume();
            session.run("CREATE INDEX section_node_code IF NOT EXISTS FOR (s:Section) ON (s.documentId, s.nodeCode)").consume();
            session.run("CREATE INDEX section_title IF NOT EXISTS FOR (s:Section) ON (s.documentId, s.normalizedTitle)").consume();
            session.run("CREATE INDEX item_node_id IF NOT EXISTS FOR (i:Item) ON (i.nodeId)").consume();
            session.run("CREATE INDEX item_document_node_id IF NOT EXISTS FOR (i:Item) ON (i.documentId, i.nodeId)").consume();
            session.run("CREATE INDEX item_index IF NOT EXISTS FOR (i:Item) ON (i.documentId, i.sectionPath, i.itemIndex)").consume();
            log.info("初始化 Neo4j 结构图索引完成: database={}", properties.getNeo4j().getDatabase());
        }
        catch (Exception exception) {
            log.warn("初始化 Neo4j 结构图索引失败: {}", exception.getMessage());
        }
    }

    private Session openSession() {
        return driver.session(SessionConfig.forDatabase(properties.getNeo4j().getDatabase()));
    }

    private org.neo4j.driver.Value sectionParams(SuperAgentDocumentStructureNode node) {
        return Values.parameters(
            "nodeId", node.getId(),
            "documentId", node.getDocumentId(),
            "parseTaskId", node.getParseTaskId(),
            "nodeNo", node.getNodeNo(),
            "depth", node.getDepth(),
            "parentNodeId", node.getParentNodeId(),
            "prevSiblingNodeId", node.getPrevSiblingNodeId(),
            "nextSiblingNodeId", node.getNextSiblingNodeId(),
            "nodeCode", safeText(node.getNodeCode()),
            "title", safeText(node.getTitle()),
            "anchorText", safeText(node.getAnchorText()),
            "sectionPath", safeText(node.getSectionPath()),
            "canonicalPath", safeText(node.getCanonicalPath()),
            "contentText", safeText(node.getContentText()),
            "normalizedTitle", normalize(node.getTitle()),
            "normalizedPath", normalize(node.getSectionPath())
        );
    }

    private org.neo4j.driver.Value itemParams(SuperAgentDocumentStructureNode node, DocumentStructureNodeTypeEnum nodeType) {
        return Values.parameters(
            "nodeId", node.getId(),
            "documentId", node.getDocumentId(),
            "parseTaskId", node.getParseTaskId(),
            "nodeNo", node.getNodeNo(),
            "nodeType", nodeType == null ? "" : nodeType.name(),
            "sectionNodeId", node.getParentNodeId(),
            "prevSiblingNodeId", node.getPrevSiblingNodeId(),
            "nextSiblingNodeId", node.getNextSiblingNodeId(),
            "title", safeText(node.getTitle()),
            "anchorText", safeText(node.getAnchorText()),
            "sectionPath", safeText(node.getSectionPath()),
            "canonicalPath", safeText(node.getCanonicalPath()),
            "contentText", safeText(node.getContentText()),
            "itemIndex", node.getItemIndex(),
            "normalizedTitle", normalize(node.getTitle())
        );
    }

    private void mergeSectionSiblingEdges(org.neo4j.driver.TransactionContext tx, SuperAgentDocumentStructureNode node) {
        if (node.getNextSiblingNodeId() != null) {
            tx.run("""
                    MATCH (a:Section {documentId: $documentId, nodeId: $nodeId}),
                          (b:Section {documentId: $documentId, nodeId: $nextNodeId})
                    MERGE (a)-[:NEXT_SIBLING]->(b)
                    MERGE (b)-[:PREV_SIBLING]->(a)
                    """,
                Values.parameters("documentId", node.getDocumentId(), "nodeId", node.getId(), "nextNodeId", node.getNextSiblingNodeId()));
        }
        else if (node.getPrevSiblingNodeId() != null) {
            tx.run("""
                    MATCH (a:Section {documentId: $documentId, nodeId: $prevNodeId}),
                          (b:Section {documentId: $documentId, nodeId: $nodeId})
                    MERGE (a)-[:NEXT_SIBLING]->(b)
                    MERGE (b)-[:PREV_SIBLING]->(a)
                    """,
                Values.parameters("documentId", node.getDocumentId(), "prevNodeId", node.getPrevSiblingNodeId(), "nodeId", node.getId()));
        }
    }

    private void mergeItemSiblingEdges(org.neo4j.driver.TransactionContext tx, SuperAgentDocumentStructureNode node) {
        if (node.getNextSiblingNodeId() != null) {
            tx.run("""
                    MATCH (a:Item {documentId: $documentId, nodeId: $nodeId}),
                          (b:Item {documentId: $documentId, nodeId: $nextNodeId})
                    MERGE (a)-[:NEXT_ITEM]->(b)
                    MERGE (b)-[:PREV_ITEM]->(a)
                    """,
                Values.parameters("documentId", node.getDocumentId(), "nodeId", node.getId(), "nextNodeId", node.getNextSiblingNodeId()));
        }
        else if (node.getPrevSiblingNodeId() != null) {
            tx.run("""
                    MATCH (a:Item {documentId: $documentId, nodeId: $prevNodeId}),
                          (b:Item {documentId: $documentId, nodeId: $nodeId})
                    MERGE (a)-[:NEXT_ITEM]->(b)
                    MERGE (b)-[:PREV_ITEM]->(a)
                    """,
                Values.parameters("documentId", node.getDocumentId(), "prevNodeId", node.getPrevSiblingNodeId(), "nodeId", node.getId()));
        }
    }

    private void updateTaskGraphStatus(Long parseTaskId, String status, String errorMessage) {
        if (parseTaskId == null) {
            return;
        }
        SuperAgentDocumentTask task = taskMapper.selectById(parseTaskId);
        if (task == null) {
            return;
        }
        try {
            Map<String, Object> extJson = StrUtil.isBlank(task.getExtJson())
                ? new LinkedHashMap<>()
                : objectMapper.readValue(task.getExtJson(), new TypeReference<LinkedHashMap<String, Object>>() {
                });
            extJson.put("graph_index_status", status);
            extJson.put("last_graph_index_time", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            if (StrUtil.isNotBlank(errorMessage)) {
                extJson.put("graph_index_error_msg", errorMessage);
            }
            else {
                extJson.remove("graph_index_error_msg");
            }
            task.setExtJson(objectMapper.writeValueAsString(extJson));
            taskMapper.updateById(task);
        }
        catch (Exception exception) {
            log.warn("更新图索引状态失败: taskId={}, error={}", parseTaskId, exception.getMessage());
        }
    }

    private String normalize(String text) {
        return safeText(text)
            .replaceAll("[\\s>`*#_\\-]+", "")
            .toLowerCase();
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
