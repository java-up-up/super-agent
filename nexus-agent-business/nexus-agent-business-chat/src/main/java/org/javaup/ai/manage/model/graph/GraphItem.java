package org.javaup.ai.manage.model.graph;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 结构图里的步骤/列表项节点视图
 * @author: 阿星不是程序员
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphItem {

    private Long nodeId;

    private Long documentId;

    private Long parseTaskId;

    private Integer nodeNo;

    private String nodeType;

    private Long sectionNodeId;

    private Long prevSiblingNodeId;

    private Long nextSiblingNodeId;

    private String title;

    private String anchorText;

    private String sectionPath;

    private String canonicalPath;

    private String contentText;

    private Integer itemIndex;

    public String displayText() {
        return StrUtil.blankToDefault(contentText, StrUtil.blankToDefault(anchorText, StrUtil.blankToDefault(title, "")));
    }
}
