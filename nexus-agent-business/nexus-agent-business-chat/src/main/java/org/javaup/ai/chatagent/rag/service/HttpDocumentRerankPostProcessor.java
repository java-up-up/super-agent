package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.rag.config.ChatRagProperties;
import org.javaup.ai.chatagent.support.RestClientFactorySupport;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
@Component
public class HttpDocumentRerankPostProcessor implements DocumentPostProcessor {

    private final ChatRagProperties properties;
    private final RestClient restClient;

    public HttpDocumentRerankPostProcessor(ChatRagProperties properties) {
        this.properties = properties;
        this.restClient = RestClientFactorySupport.create(
            null,
            properties.getRerank().getConnectTimeoutMs(),
            properties.getRerank().getReadTimeoutMs()
        );
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public List<Document> process(@NotNull Query query, @NotNull List<Document> documents) {
        if (CollectionUtil.isEmpty(documents)) {
            return documents;
        }

        ChatRagProperties.RerankProperties rerankProperties = properties.getRerank();
        int topN = Math.min(Math.max(rerankProperties.getTopN(), 1), documents.size());

        if (!rerankProperties.isEnabled()) {
            return documents;
        }
        if (rerankProperties.getApiKey() == null || rerankProperties.getApiKey().isBlank()) {
            log.warn("Rerank 已开启但未配置 apiKey，自动回退为原始排序");
            return documents;
        }

        List<String> texts = documents.stream().map(Document::getText).toList();
        Map<String, Object> body = Map.of(
            "model", rerankProperties.getModel(),
            "query", query.text(),
            "documents", texts,
            "top_n", topN,
            "return_documents", false
        );

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> response = restClient.post()
                .uri(rerankProperties.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + rerankProperties.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
            long durationMs = System.currentTimeMillis() - startTime;

            if (response == null || !(response.get("results") instanceof List<?> resultList)) {
                log.warn("Rerank 返回结果为空或格式错误，自动回退为原始排序");
                return documents;
            }

            return ((List<Map<String, Object>>) resultList).stream()
                .sorted(Comparator.comparingDouble(result -> -((Number) result.get("relevance_score")).doubleValue()))
                .map(result -> {
                    int index = ((Number) result.get("index")).intValue();
                    double score = ((Number) result.get("relevance_score")).doubleValue();
                    Document document = documents.get(index);
                    document.getMetadata().put("rerankScore", score);
                    document.getMetadata().put("rerankModel", rerankProperties.getModel());
                    document.getMetadata().put("rerankQuery", query.text());
                    document.getMetadata().put("rerankDurationMs", durationMs);
                    document.getMetadata().put("rerankOriginalIndex", index);
                    return document;
                })
                .limit(topN)
                .collect(Collectors.toList());
        }
        catch (Exception exception) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("Rerank 调用失败（耗时 {} ms），自动回退为原始排序: {}", durationMs, exception.getMessage());
            return documents;
        }
    }
}
