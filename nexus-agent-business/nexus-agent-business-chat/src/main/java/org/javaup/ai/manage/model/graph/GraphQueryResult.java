package org.javaup.ai.manage.model.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 结构图查询执行结果
 * @author: 阿星不是程序员
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQueryResult {

    private GraphSection targetSection;

    private GraphSection parentSection;

    private GraphSection previousSibling;

    private GraphSection nextSibling;

    private GraphItem targetItem;

    @Builder.Default
    private List<GraphSection> children = new ArrayList<>();

    @Builder.Default
    private List<GraphItem> matchedItems = new ArrayList<>();

    @Builder.Default
    private List<GraphItem> allItems = new ArrayList<>();
}
