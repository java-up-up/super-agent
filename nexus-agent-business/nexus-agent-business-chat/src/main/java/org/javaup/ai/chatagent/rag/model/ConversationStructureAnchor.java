package org.javaup.ai.chatagent.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 会话结构锚点
 * @author: 阿星不是程序员
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationStructureAnchor {

    private String rootSectionCode;

    private String rootSectionTitle;

    private String targetSectionHint;

    private Long structureNodeId;

    private String canonicalPath;

    private String scopeMode;

    public boolean isEmpty() {
        return (rootSectionCode == null || rootSectionCode.isBlank())
            && (rootSectionTitle == null || rootSectionTitle.isBlank())
            && (targetSectionHint == null || targetSectionHint.isBlank())
            && structureNodeId == null
            && (canonicalPath == null || canonicalPath.isBlank());
    }
}
