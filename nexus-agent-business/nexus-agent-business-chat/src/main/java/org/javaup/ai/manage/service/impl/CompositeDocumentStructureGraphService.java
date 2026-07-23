package org.javaup.ai.manage.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.manage.model.graph.GraphItem;
import org.javaup.ai.manage.model.graph.GraphSection;
import org.javaup.ai.manage.service.DocumentStructureGraphService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务实现层
 * @author: 阿星不是程序员
 **/

@Slf4j
@AllArgsConstructor
@Primary
@Service
public class CompositeDocumentStructureGraphService implements DocumentStructureGraphService {

    private final MysqlDocumentStructureGraphService mysqlGraphService;
    private final ObjectProvider<Neo4jDocumentStructureGraphService> neo4jGraphServiceProvider;

    @Override
    public boolean isGraphAvailable(Long documentId) {
        return delegate(documentId).isGraphAvailable(documentId);
    }

    @Override
    public GraphSection findSectionById(Long documentId, Long sectionNodeId) {
        return delegate(documentId).findSectionById(documentId, sectionNodeId);
    }

    @Override
    public GraphSection findSectionByCode(Long documentId, String nodeCode) {
        return delegate(documentId).findSectionByCode(documentId, nodeCode);
    }

    @Override
    public GraphSection findSectionByTitle(Long documentId, String title) {
        return delegate(documentId).findSectionByTitle(documentId, title);
    }

    @Override
    public GraphSection findSectionByCanonicalPath(Long documentId, String canonicalPath) {
        return delegate(documentId).findSectionByCanonicalPath(documentId, canonicalPath);
    }

    @Override
    public GraphSection findBestSection(Long documentId, String topic, String facet) {
        return delegate(documentId).findBestSection(documentId, topic, facet);
    }

    @Override
    public List<GraphSection> listSections(Long documentId) {
        return delegate(documentId).listSections(documentId);
    }

    @Override
    public List<GraphSection> listChildren(Long documentId, Long sectionNodeId) {
        return delegate(documentId).listChildren(documentId, sectionNodeId);
    }

    @Override
    public GraphSection parentSection(Long documentId, Long sectionNodeId) {
        return delegate(documentId).parentSection(documentId, sectionNodeId);
    }

    @Override
    public GraphSection previousSibling(Long documentId, Long sectionNodeId) {
        return delegate(documentId).previousSibling(documentId, sectionNodeId);
    }

    @Override
    public GraphSection nextSibling(Long documentId, Long sectionNodeId) {
        return delegate(documentId).nextSibling(documentId, sectionNodeId);
    }

    @Override
    public GraphItem findItemByIndex(Long documentId, Long sectionNodeId, Integer itemIndex) {
        return delegate(documentId).findItemByIndex(documentId, sectionNodeId, itemIndex);
    }

    @Override
    public List<GraphItem> listItems(Long documentId, Long sectionNodeId) {
        return delegate(documentId).listItems(documentId, sectionNodeId);
    }

    @Override
    public List<GraphItem> searchItemsInSection(Long documentId, Long sectionNodeId, String keyword) {
        return delegate(documentId).searchItemsInSection(documentId, sectionNodeId, keyword);
    }

    private DocumentStructureGraphService delegate(Long documentId) {
        Neo4jDocumentStructureGraphService neo4jGraphService = neo4jGraphServiceProvider.getIfAvailable();
        if (neo4jGraphService != null && neo4jGraphService.isGraphAvailable(documentId)) {
            log.info("结构图服务选择 Neo4j: documentId={}", documentId);
            return neo4jGraphService;
        }
        log.info("结构图服务选择 MySQL fallback: documentId={}, neo4jBeanAvailable={}", documentId, neo4jGraphService != null);
        return mysqlGraphService;
    }
}
