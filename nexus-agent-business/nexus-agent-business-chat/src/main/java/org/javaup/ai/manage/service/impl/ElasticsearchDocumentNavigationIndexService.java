package org.javaup.ai.manage.service.impl;

import lombok.AllArgsConstructor;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.manage.config.DocumentManageProperties;
import org.javaup.ai.manage.data.SuperAgentDocumentStructureNode;
import org.javaup.ai.manage.model.es.DocumentNavigationIndexRecord;
import org.javaup.ai.manage.service.DocumentNavigationIndexService;
import org.javaup.enums.DocumentStructureNodeTypeEnum;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务实现层
 * @author: 阿星不是程序员
 **/

@Slf4j
@AllArgsConstructor
@Service
@ConditionalOnProperty(prefix = "app.manage.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchDocumentNavigationIndexService implements DocumentNavigationIndexService {

    private static final int DEFAULT_SEARCH_SIZE = 8;

    @Qualifier("documentManageElasticsearchClient")
    private final ElasticsearchClient elasticsearchClient;
    private final DocumentManageProperties properties;

    @Override
    public void reindexDocumentNodes(Long documentId, Long parseTaskId, List<SuperAgentDocumentStructureNode> nodes) {
        if (documentId == null) {
            return;
        }
        log.info("开始重建导航索引: documentId={}, parseTaskId={}, nodeCount={}, index={}",
            documentId,
            parseTaskId,
            nodes == null ? 0 : nodes.size(),
            properties.getElasticsearch().getNavigationIndexName());
        deleteByDocumentId(documentId);
        if (CollUtil.isEmpty(nodes)) {
            log.info("导航索引重建跳过写入，因为结构节点为空: documentId={}, parseTaskId={}", documentId, parseTaskId);
            return;
        }
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder()
            .index(properties.getElasticsearch().getNavigationIndexName())
            .refresh(Refresh.WaitFor);
        for (SuperAgentDocumentStructureNode node : nodes) {
            if (node == null || node.getId() == null) {
                continue;
            }
            DocumentNavigationIndexRecord record = toIndexRecord(node, parseTaskId);
            bulkBuilder.operations(operation -> operation
                .index(index -> index
                    .id(String.valueOf(record.getNodeId()))
                    .document(record)
                )
            );
        }
        try {
            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                String errorMessage = response.items().stream()
                    .filter(item -> item.error() != null)
                    .map(item -> item.id() + ":" + item.error().reason())
                    .collect(Collectors.joining("; "));
                throw new IllegalStateException("批量写入导航索引失败: " + errorMessage);
            }
            log.info("文档结构节点已同步写入导航索引: documentId={}, parseTaskId={}, nodeCount={}, index={}",
                documentId, parseTaskId, nodes.size(), properties.getElasticsearch().getNavigationIndexName());
        }
        catch (IOException exception) {
            throw new IllegalStateException("写入导航索引失败", exception);
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }
        try {
            log.info("删除导航索引文档数据: documentId={}, index={}", documentId, properties.getElasticsearch().getNavigationIndexName());
            elasticsearchClient.deleteByQuery(delete -> delete
                .index(properties.getElasticsearch().getNavigationIndexName())
                .refresh(true)
                .query(query -> query.term(term -> term
                    .field("documentId")
                    .value(documentId)
                ))
            );
        }
        catch (IOException exception) {
            throw new IllegalStateException("删除导航索引失败", exception);
        }
    }

    @Override
    public List<NavigationSectionHit> searchSections(Long documentId,
                                                     String topic,
                                                     String facet,
                                                     String informationNeed,
                                                     String question,
                                                     int size) {
        if (documentId == null) {
            return List.of();
        }
        List<String> queries = buildQueries(topic, facet, informationNeed, question);
        if (queries.isEmpty()) {
            return List.of();
        }
        int searchSize = size <= 0 ? DEFAULT_SEARCH_SIZE : Math.min(size, 20);
        log.info("导航索引搜索请求: documentId={}, topic='{}', facet='{}', informationNeed='{}', question='{}', size={}, queries={}",
            documentId,
            safeText(topic),
            safeText(facet),
            safeText(informationNeed),
            safeText(question),
            searchSize,
            queries);
        try {
            SearchResponse<DocumentNavigationIndexRecord> response = elasticsearchClient.search(search -> search
                    .index(properties.getElasticsearch().getNavigationIndexName())
                    .size(searchSize)
                    .query(query -> query.bool(bool -> {
                        bool.filter(filter -> filter.term(term -> term
                            .field("documentId")
                            .value(documentId)
                        ));
                        bool.filter(filter -> filter.term(term -> term
                            .field("nodeType")
                            .value(DocumentStructureNodeTypeEnum.SECTION.name())
                        ));
                        for (String queryText : queries) {
                            addSectionShouldQueries(bool, queryText);
                        }
                        bool.minimumShouldMatch("1");
                        return bool;
                    })),
                DocumentNavigationIndexRecord.class);
            List<NavigationSectionHit> hits = new ArrayList<>();
            for (Hit<DocumentNavigationIndexRecord> hit : response.hits().hits()) {
                DocumentNavigationIndexRecord source = hit.source();
                if (source == null || source.getNodeId() == null) {
                    continue;
                }
                hits.add(new NavigationSectionHit(
                    source.getNodeId(),
                    safeText(source.getNodeCode()),
                    safeText(source.getTitle()),
                    safeText(source.getSectionPath()),
                    safeText(source.getCanonicalPath()),
                    hit.score() == null ? 0D : hit.score()
                ));
            }
            log.info("导航索引搜索完成: documentId={}, hitCount={}, topHits={}",
                documentId,
                hits.size(),
                hits.stream().limit(3).map(hit -> hit.nodeId() + ":" + hit.sectionPath() + ":" + hit.score()).toList());
            return hits;
        }
        catch (IOException exception) {
            log.warn("导航索引章节搜索失败，自动回退到结构图兜底匹配: documentId={}, question='{}', error={}",
                documentId, safeText(question), exception.getMessage());
            return List.of();
        }
    }

    private void addSectionShouldQueries(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder bool,
                                         String queryText) {
        if (StrUtil.isBlank(queryText)) {
            return;
        }
        bool.should(should -> should.matchPhrase(matchPhrase -> matchPhrase
            .field("title")
            .query(queryText)
            .boost(20.0f)
        ));
        bool.should(should -> should.matchPhrase(matchPhrase -> matchPhrase
            .field("sectionPath")
            .query(queryText)
            .boost(15.0f)
        ));
        bool.should(should -> should.multiMatch(multiMatch -> multiMatch
            .query(queryText)
            .fields("title^10", "sectionPath^8", "anchorText^5", "contentText")
            .type(TextQueryType.BestFields)
        ));
    }

    private List<String> buildQueries(String topic, String facet, String informationNeed, String question) {
        List<String> queries = new ArrayList<>();
        addNonBlank(queries, topic);
        addNonBlank(queries, facet);
        addNonBlank(queries, informationNeed);
        addNonBlank(queries, question);
        return queries.stream().distinct().toList();
    }

    private void addNonBlank(List<String> queries, String value) {
        if (StrUtil.isNotBlank(value)) {
            queries.add(value.trim());
        }
    }

    private DocumentNavigationIndexRecord toIndexRecord(SuperAgentDocumentStructureNode node, Long parseTaskId) {
        DocumentStructureNodeTypeEnum nodeType = DocumentStructureNodeTypeEnum.getRc(node.getNodeType());
        return DocumentNavigationIndexRecord.builder()
            .nodeId(node.getId())
            .documentId(node.getDocumentId())
            .parseTaskId(node.getParseTaskId() == null ? parseTaskId : node.getParseTaskId())
            .nodeType(nodeType == null ? "" : nodeType.name())
            .nodeCode(safeText(node.getNodeCode()))
            .nodeNo(node.getNodeNo())
            .depth(node.getDepth())
            .parentNodeId(node.getParentNodeId())
            .title(safeText(node.getTitle()))
            .anchorText(safeText(node.getAnchorText()))
            .sectionPath(safeText(node.getSectionPath()))
            .canonicalPath(safeText(node.getCanonicalPath()))
            .contentText(safeText(node.getContentText()))
            .itemIndex(node.getItemIndex())
            .build();
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
