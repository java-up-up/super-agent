package org.javaup.ai.manage.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.manage.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeRouteElasticsearchIndexInitializer {

    private final ElasticsearchClient elasticsearchClient;
    private final DocumentManageProperties properties;

    public KnowledgeRouteElasticsearchIndexInitializer(
        @Qualifier("documentManageElasticsearchClient") ElasticsearchClient elasticsearchClient,
        DocumentManageProperties properties) {
        this.elasticsearchClient = elasticsearchClient;
        this.properties = properties;
    }

    @PostConstruct
    public void initIndex() {
        DocumentManageProperties.Elasticsearch elasticsearch = properties.getElasticsearch();
        String indexName = elasticsearch.getRouteIndexName();
        String analyzer = elasticsearch.getAnalyzer();
        String searchAnalyzer = elasticsearch.getSearchAnalyzer();
        try {
            if (indexExists(indexName)) {
                log.info("Elasticsearch 知识路由索引 [{}] 已存在，跳过创建。", indexName);
                return;
            }
            createIndex(indexName, analyzer, searchAnalyzer);
            log.info("Elasticsearch 知识路由索引 [{}] 创建完成，analyzer={}, searchAnalyzer={}",
                indexName, analyzer, searchAnalyzer);
        }
        catch (IOException exception) {
            if (isIkAnalyzer(analyzer) || isIkAnalyzer(searchAnalyzer)) {
                log.warn("使用 IK 分词器创建知识路由索引失败，准备回退到 standard。原因: {}", exception.getMessage());
                fallbackToStandard(indexName);
                return;
            }
            log.error("初始化知识路由索引失败: {}", exception.getMessage(), exception);
        }
    }

    private boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices().exists(ExistsRequest.of(exists -> exists.index(indexName))).value();
    }

    private void createIndex(String indexName, String analyzer, String searchAnalyzer) throws IOException {
        elasticsearchClient.indices().create(create -> create
            .index(indexName)
            .mappings(mapping -> mapping
                .properties("routeId", property -> property.keyword(keyword -> keyword))
                .properties("entityType", property -> property.keyword(keyword -> keyword))
                .properties("entityCode", property -> property.keyword(keyword -> keyword))
                .properties("documentId", property -> property.long_(number -> number))
                .properties("scopeCode", property -> property.keyword(keyword -> keyword))
                .properties("scopeName", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("topicCode", property -> property.keyword(keyword -> keyword))
                .properties("topicName", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("documentName", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("businessCategory", property -> property.keyword(keyword -> keyword))
                .properties("displayName", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("descriptionText", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("aliasesText", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("examplesText", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("summaryText", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("routeText", property -> property.text(text -> text.analyzer(analyzer).searchAnalyzer(searchAnalyzer)))
                .properties("entityTerms", property -> property.keyword(keyword -> keyword))
                .properties("tags", property -> property.keyword(keyword -> keyword))
            )
        );
    }

    private boolean isIkAnalyzer(String analyzer) {
        return analyzer != null && analyzer.startsWith("ik_");
    }

    private void fallbackToStandard(String indexName) {
        try {
            if (indexExists(indexName)) {
                return;
            }
            createIndex(indexName, "standard", "standard");
            log.info("Elasticsearch 知识路由索引 [{}] 已回退到 standard 分词器。", indexName);
        }
        catch (IOException exception) {
            log.error("回退创建知识路由索引失败: {}", exception.getMessage(), exception);
        }
    }
}
