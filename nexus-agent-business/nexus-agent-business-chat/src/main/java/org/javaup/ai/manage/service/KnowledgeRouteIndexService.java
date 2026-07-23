package org.javaup.ai.manage.service;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

public interface KnowledgeRouteIndexService {

    void refreshIfNeeded();

    List<RouteLexicalHit> search(String routingText, String entityType, int size);

    void deleteDocumentRoute(Long documentId);

    record RouteLexicalHit(
        String routeId,
        String entityCode,
        String entityType,
        Long documentId,
        String scopeCode,
        String topicCode,
        String documentName,
        double score
    ) {
    }
}
