package org.javaup.ai.manage.model.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 模型对象
 * @author: 阿星不是程序员
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRouteDecision {

    private List<ScopeRouteCandidate> scopes = new ArrayList<>();

    private List<TopicRouteCandidate> topics = new ArrayList<>();

    private List<DocumentRouteCandidate> documents = new ArrayList<>();

    private BigDecimal confidence = BigDecimal.ZERO;

    private String routeStatus = "SUCCESS";

    private String reason = "";

    public DocumentRouteCandidate topDocument() {
        return documents == null || documents.isEmpty() ? null : documents.get(0);
    }
}
