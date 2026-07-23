package org.javaup.ai.manage.service.impl;

import lombok.AllArgsConstructor;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.manage.data.SuperAgentDocumentChunk;
import org.javaup.ai.manage.service.DocumentVectorGateway;
import org.javaup.ai.manage.support.DocumentPgVectorConstants;
import org.javaup.enums.DocumentManageCode;
import org.javaup.enums.DocumentVectorStatusEnum;
import org.javaup.enums.DocumentVectorStoreTypeEnum;
import org.javaup.exception.SuperAgentFrameException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务实现层
 * @author: 阿星不是程序员
 **/

@Slf4j
@AllArgsConstructor
@Service
public class DefaultDocumentVectorGateway implements DocumentVectorGateway {

    public static final int EMBEDDING_BATCH_SIZE_LIMIT = 10;

    private static final String UPSERT_SQL_TEMPLATE = """
        INSERT INTO %s
        (id, document_id, task_id, plan_id, parent_block_id, chunk_no, source_type, section_path, structure_node_id,
         structure_node_type, canonical_path, item_index, chunk_text, char_count, token_count, embedding_model,
         metadata_json, embedding, create_time, edit_time, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS vector), NOW(), NOW(), ?)
        ON CONFLICT (id) DO UPDATE SET
            document_id = EXCLUDED.document_id,
            task_id = EXCLUDED.task_id,
            plan_id = EXCLUDED.plan_id,
            parent_block_id = EXCLUDED.parent_block_id,
            chunk_no = EXCLUDED.chunk_no,
            source_type = EXCLUDED.source_type,
            section_path = EXCLUDED.section_path,
            structure_node_id = EXCLUDED.structure_node_id,
            structure_node_type = EXCLUDED.structure_node_type,
            canonical_path = EXCLUDED.canonical_path,
            item_index = EXCLUDED.item_index,
            chunk_text = EXCLUDED.chunk_text,
            char_count = EXCLUDED.char_count,
            token_count = EXCLUDED.token_count,
            embedding_model = EXCLUDED.embedding_model,
            metadata_json = EXCLUDED.metadata_json,
            embedding = EXCLUDED.embedding,
            edit_time = NOW(),
            status = EXCLUDED.status
        """;

    private static final String DELETE_BY_DOCUMENT_SQL_TEMPLATE = "DELETE FROM %s WHERE document_id = ?";

    @Qualifier("documentManagePgVectorJdbcTemplate")
    private final JdbcTemplate pgVectorJdbcTemplate;

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.embedding.options.model:}")
    private String embeddingModelName;

    @Override
    public void vectorize(List<SuperAgentDocumentChunk> chunkList) {

        if (CollUtil.isEmpty(chunkList)) {
            return;
        }

        EmbeddingModel embeddingModel = requireEmbeddingModel();

        List<SuperAgentDocumentChunk> validChunkList = chunkList.stream()
            .filter(chunk -> chunk != null && StrUtil.isNotBlank(chunk.getChunkText()))
            .toList();
        if (validChunkList.isEmpty()) {
            return;
        }

        String upsertSql = UPSERT_SQL_TEMPLATE.formatted(DocumentPgVectorConstants.EMBEDDING_TABLE_NAME);
        int batchSize = EMBEDDING_BATCH_SIZE_LIMIT;
        String currentEmbeddingModelName = resolveEmbeddingModelName();
        int totalBatchCount = (validChunkList.size() + batchSize - 1) / batchSize;

        log.info("开始执行文档向量化，chunkCount={}, batchSize={}, batchCount={}, embeddingModel={}",
            validChunkList.size(), batchSize, totalBatchCount, currentEmbeddingModelName);

        for (int startIndex = 0; startIndex < validChunkList.size(); startIndex += batchSize) {
            int endIndex = Math.min(startIndex + batchSize, validChunkList.size());
            List<SuperAgentDocumentChunk> currentBatch = validChunkList.subList(startIndex, endIndex);
            int currentBatchIndex = (startIndex / batchSize) + 1;

            log.info("开始处理 embedding 批次，batchIndex={}/{}, chunkRange=[{}, {}], currentBatchSize={}",
                currentBatchIndex, totalBatchCount, startIndex + 1, endIndex, currentBatch.size());

            List<float[]> embeddingList = embeddingModel.embed(currentBatch.stream()
                .map(SuperAgentDocumentChunk::getChunkText)
                .toList());
            if (embeddingList.size() != currentBatch.size()) {
                throw new IllegalStateException("EmbeddingModel 返回的向量数量与 chunk 数量不一致。");
            }

            batchUpsert(upsertSql, currentBatch, embeddingList, currentEmbeddingModelName);
            markSuccess(currentBatch);

            log.info("embedding 批次处理完成，batchIndex={}/{}, currentBatchSize={}",
                currentBatchIndex, totalBatchCount, currentBatch.size());
        }

        log.info("文档向量化执行完成，chunkCount={}, batchSize={}, batchCount={}, embeddingModel={}",
            validChunkList.size(), batchSize, totalBatchCount, currentEmbeddingModelName);
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }

        try {
            String deleteSql = DELETE_BY_DOCUMENT_SQL_TEMPLATE.formatted(DocumentPgVectorConstants.EMBEDDING_TABLE_NAME);
            pgVectorJdbcTemplate.update(deleteSql, documentId);
        }
        catch (Exception exception) {
            throw new SuperAgentFrameException(DocumentManageCode.DOCUMENT_VECTOR_FAILED.getCode(),
                "删除 PGVector 数据失败: " + exception.getMessage(), exception);
        }
    }

    private void batchUpsert(String upsertSql,
                             List<SuperAgentDocumentChunk> chunkBatch,
                             List<float[]> embeddingBatch,
                             String embeddingModelName) {
        pgVectorJdbcTemplate.batchUpdate(upsertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int index) throws SQLException {
                SuperAgentDocumentChunk chunk = chunkBatch.get(index);
                float[] embedding = embeddingBatch.get(index);

                chunk.setVectorStatus(DocumentVectorStatusEnum.VECTORIZING.getCode());
                String metadataJson = buildMetadataJson(chunk, embeddingModelName);

                ps.setLong(1, chunk.getId());
                ps.setLong(2, chunk.getDocumentId());
                ps.setLong(3, chunk.getTaskId());
                if (chunk.getPlanId() == null) {
                    ps.setNull(4, Types.BIGINT);
                }
                else {
                    ps.setLong(4, chunk.getPlanId());
                }
                if (chunk.getParentBlockId() == null) {
                    ps.setNull(5, Types.BIGINT);
                }
                else {
                    ps.setLong(5, chunk.getParentBlockId());
                }
                ps.setInt(6, chunk.getChunkNo());
                ps.setInt(7, defaultInteger(chunk.getSourceType()));
                ps.setString(8, chunk.getSectionPath());
                if (chunk.getStructureNodeId() == null) {
                    ps.setNull(9, Types.BIGINT);
                }
                else {
                    ps.setLong(9, chunk.getStructureNodeId());
                }
                ps.setInt(10, defaultInteger(chunk.getStructureNodeType()));
                ps.setString(11, chunk.getCanonicalPath());
                ps.setInt(12, defaultInteger(chunk.getItemIndex()));
                ps.setString(13, chunk.getChunkText());
                ps.setInt(14, defaultInteger(chunk.getCharCount()));
                ps.setInt(15, defaultInteger(chunk.getTokenCount()));
                ps.setString(16, embeddingModelName);
                ps.setString(17, metadataJson);

                ps.setString(18, toVectorLiteral(embedding));
                ps.setInt(19, 1);
            }

            @Override
            public int getBatchSize() {
                return chunkBatch.size();
            }
        });
    }

    private void markSuccess(List<SuperAgentDocumentChunk> chunkBatch) {
        for (SuperAgentDocumentChunk chunk : chunkBatch) {

            chunk.setVectorId(String.valueOf(chunk.getId()));
            chunk.setVectorStoreType(DocumentVectorStoreTypeEnum.PG_VECTOR.getCode());
            chunk.setVectorStatus(DocumentVectorStatusEnum.VECTOR_SUCCESS.getCode());
        }
    }

    private String buildMetadataJson(SuperAgentDocumentChunk chunk, String embeddingModelName) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("taskId", chunk.getTaskId());
        metadata.put("planId", chunk.getPlanId());
        metadata.put("parentBlockId", chunk.getParentBlockId());
        metadata.put("chunkNo", chunk.getChunkNo());
        metadata.put("sourceType", chunk.getSourceType());
        metadata.put("sectionPath", chunk.getSectionPath());
        metadata.put("structureNodeId", chunk.getStructureNodeId());
        metadata.put("structureNodeType", chunk.getStructureNodeType());
        metadata.put("canonicalPath", chunk.getCanonicalPath());
        metadata.put("itemIndex", chunk.getItemIndex());
        metadata.put("charCount", chunk.getCharCount());
        metadata.put("tokenCount", chunk.getTokenCount());
        metadata.put("embeddingModel", embeddingModelName);
        try {
            return objectMapper.writeValueAsString(metadata);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化 PGVector metadata 失败。", exception);
        }
    }

    private String toVectorLiteral(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalStateException("EmbeddingModel 返回了空向量。");
        }
        StringBuilder vectorBuilder = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {

            if (index > 0) {
                vectorBuilder.append(",");
            }
            vectorBuilder.append(embedding[index]);
        }
        vectorBuilder.append("]");
        return vectorBuilder.toString();
    }

    private EmbeddingModel requireEmbeddingModel() {

        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw new IllegalStateException("当前未找到可用的 EmbeddingModel，无法执行向量化。");
        }
        return embeddingModel;
    }

    private String resolveEmbeddingModelName() {

        return StrUtil.isNotBlank(embeddingModelName)
            ? embeddingModelName
            : "default";
    }

    private int defaultInteger(Integer value) {

        return Objects.requireNonNullElse(value, 0);
    }
}
