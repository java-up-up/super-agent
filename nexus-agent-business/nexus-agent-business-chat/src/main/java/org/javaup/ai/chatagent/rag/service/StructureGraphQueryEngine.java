package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.manage.model.graph.GraphItem;
import org.javaup.ai.manage.model.graph.GraphItemWithContext;
import org.javaup.ai.manage.model.graph.GraphQueryResult;
import org.javaup.ai.manage.model.graph.GraphSection;
import org.javaup.ai.manage.model.graph.GraphSectionWithChildren;
import org.javaup.ai.manage.model.graph.GraphSectionWithSiblings;
import org.javaup.ai.manage.service.DocumentStructureGraphService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Service
@Slf4j
public class StructureGraphQueryEngine {

    private final DocumentStructureGraphService graphService;

    public StructureGraphQueryEngine(DocumentStructureGraphService graphService) {
        this.graphService = graphService;
    }

    public GraphSectionWithChildren findSectionWithChildren(Long documentId, String topic) {
        GraphSection section = graphService.findBestSection(documentId, topic, "");
        return findSectionWithChildren(documentId, section == null ? null : section.getNodeId());
    }

    public GraphSectionWithChildren findSectionWithChildren(Long documentId, Long sectionNodeId) {
        GraphSection section = graphService.findSectionById(documentId, sectionNodeId);
        List<GraphSection> children = section == null ? List.of() : graphService.listChildren(documentId, section.getNodeId());
        log.info("结构图查询子章节: documentId={}, sectionNodeId={}, targetSection='{}', childCount={}",
            documentId,
            sectionNodeId,
            section == null ? "" : section.displayTitle(),
            children.size());
        return GraphSectionWithChildren.builder()
            .section(section)
            .children(children)
            .build();
    }

    public GraphSectionWithSiblings findSectionWithSiblings(Long documentId, Long sectionNodeId) {
        GraphSection section = graphService.findSectionById(documentId, sectionNodeId);
        GraphSection parent = section == null ? null : graphService.parentSection(documentId, section.getNodeId());
        GraphSection previousSibling = section == null ? null : graphService.previousSibling(documentId, section.getNodeId());
        GraphSection nextSibling = section == null ? null : graphService.nextSibling(documentId, section.getNodeId());
        log.info("结构图查询相邻章节: documentId={}, sectionNodeId={}, targetSection='{}', parent='{}', previous='{}', next='{}'",
            documentId,
            sectionNodeId,
            section == null ? "" : section.displayTitle(),
            parent == null ? "" : parent.displayTitle(),
            previousSibling == null ? "" : previousSibling.displayTitle(),
            nextSibling == null ? "" : nextSibling.displayTitle());
        return GraphSectionWithSiblings.builder()
            .section(section)
            .parent(parent)
            .previousSibling(previousSibling)
            .nextSibling(nextSibling)
            .build();
    }

    public GraphItemWithContext findItemInSection(Long documentId, String sectionTopic, Integer itemIndex) {
        GraphSection section = graphService.findBestSection(documentId, sectionTopic, "");
        return findItemInSection(documentId, section == null ? null : section.getNodeId(), itemIndex);
    }

    public GraphItemWithContext findItemInSection(Long documentId, Long sectionNodeId, Integer itemIndex) {
        GraphSection section = graphService.findSectionById(documentId, sectionNodeId);
        GraphItem item = findItemInSectionTree(documentId, sectionNodeId, itemIndex);
        List<GraphItem> siblingItems = sectionNodeId == null ? List.of() : listItemsInSectionTree(documentId, sectionNodeId);
        log.info("结构图查询编号项: documentId={}, sectionNodeId={}, itemIndex={}, targetSection='{}', itemFound={}, siblingItemCount={}",
            documentId,
            sectionNodeId,
            itemIndex,
            section == null ? "" : section.displayTitle(),
            item != null,
            siblingItems.size());
        return GraphItemWithContext.builder()
            .section(section)
            .item(item)
            .siblingItems(siblingItems)
            .build();
    }

    public List<GraphItem> searchItemsInSection(Long documentId, Long sectionNodeId, String keyword) {
        List<GraphItem> items = searchItemsInSectionTree(documentId, sectionNodeId, StrUtil.blankToDefault(keyword, ""));
        log.info("结构图搜索章节内 item: documentId={}, sectionNodeId={}, keyword='{}', matchedCount={}",
            documentId,
            sectionNodeId,
            StrUtil.blankToDefault(keyword, ""),
            items.size());
        return items;
    }

    public GraphQueryResult buildGraphResult(Long documentId,
                                             Long targetSectionNodeId,
                                             Integer targetItemIndex,
                                             String itemKeyword) {
        GraphSection section = graphService.findSectionById(documentId, targetSectionNodeId);
        List<GraphSection> children = section == null ? List.of() : graphService.listChildren(documentId, section.getNodeId());
        List<GraphItem> allItems = section == null ? List.of() : listItemsInSectionTree(documentId, section.getNodeId());
        GraphItem targetItem = targetItemIndex == null || section == null
            ? null
            : findItemInSectionTree(documentId, section.getNodeId(), targetItemIndex);
        List<GraphItem> matchedItems = StrUtil.isNotBlank(itemKeyword) && section != null
            ? searchItemsInSection(documentId, section.getNodeId(), itemKeyword)
            : List.of();
        GraphSection resolvedSection = section;
        if (targetItem != null && targetItem.getSectionNodeId() != null) {
            GraphSection itemOwnerSection = graphService.findSectionById(documentId, targetItem.getSectionNodeId());
            if (itemOwnerSection != null) {
                resolvedSection = itemOwnerSection;
            }
        }
        else if (matchedItems.size() == 1 && matchedItems.get(0).getSectionNodeId() != null) {
            GraphSection itemOwnerSection = graphService.findSectionById(documentId, matchedItems.get(0).getSectionNodeId());
            if (itemOwnerSection != null) {
                resolvedSection = itemOwnerSection;
                targetItem = matchedItems.get(0);
            }
        }
        GraphQueryResult.GraphQueryResultBuilder builder = GraphQueryResult.builder()
            .targetSection(resolvedSection)
            .children(children)
            .allItems(allItems)
            .targetItem(targetItem)
            .matchedItems(matchedItems);
        if (resolvedSection != null) {
            builder.parentSection(graphService.parentSection(documentId, resolvedSection.getNodeId()))
                .previousSibling(graphService.previousSibling(documentId, resolvedSection.getNodeId()))
                .nextSibling(graphService.nextSibling(documentId, resolvedSection.getNodeId()));
        }
        GraphQueryResult result = builder.build();
        log.info("结构图结果汇总: documentId={}, targetSectionNodeId={}, targetItemIndex={}, itemKeyword='{}', targetSection='{}', targetItemFound={}, childCount={}, allItemCount={}, matchedItemCount={}",
            documentId,
            targetSectionNodeId,
            targetItemIndex,
            StrUtil.blankToDefault(itemKeyword, ""),
            result.getTargetSection() == null ? "" : result.getTargetSection().displayTitle(),
            result.getTargetItem() != null,
            result.getChildren() == null ? 0 : result.getChildren().size(),
            result.getAllItems() == null ? 0 : result.getAllItems().size(),
            result.getMatchedItems() == null ? 0 : result.getMatchedItems().size());
        return result;
    }

    private GraphItem findItemInSectionTree(Long documentId, Long sectionNodeId, Integer itemIndex) {
        if (documentId == null || sectionNodeId == null || itemIndex == null) {
            return null;
        }
        GraphItem item = graphService.findItemByIndex(documentId, sectionNodeId, itemIndex);
        if (item != null) {
            return item;
        }
        for (GraphSection child : graphService.listChildren(documentId, sectionNodeId)) {
            GraphItem descendant = findItemInSectionTree(documentId, child.getNodeId(), itemIndex);
            if (descendant != null) {
                return descendant;
            }
        }
        return null;
    }

    private List<GraphItem> listItemsInSectionTree(Long documentId, Long sectionNodeId) {
        if (documentId == null || sectionNodeId == null) {
            return List.of();
        }
        List<GraphItem> items = new ArrayList<>(graphService.listItems(documentId, sectionNodeId));
        for (GraphSection child : graphService.listChildren(documentId, sectionNodeId)) {
            items.addAll(listItemsInSectionTree(documentId, child.getNodeId()));
        }
        return items.stream()
            .sorted(Comparator.comparing(GraphItem::getNodeNo, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    private List<GraphItem> searchItemsInSectionTree(Long documentId, Long sectionNodeId, String keyword) {
        if (documentId == null || sectionNodeId == null) {
            return List.of();
        }
        List<GraphItem> items = new ArrayList<>(graphService.searchItemsInSection(documentId, sectionNodeId, keyword));
        for (GraphSection child : graphService.listChildren(documentId, sectionNodeId)) {
            items.addAll(searchItemsInSectionTree(documentId, child.getNodeId(), keyword));
        }
        return items.stream()
            .distinct()
            .sorted(Comparator.comparing(GraphItem::getNodeNo, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }
}
