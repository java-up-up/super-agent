package org.javaup.ai.chatagent.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 统一引用来源模型
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
public class SearchReference {

    private String referenceId;

    private String sourceType;

    private String title;

    private String url;

    private String snippet;

    private Long documentId;

    private String documentName;

    private Long chunkId;

    private Long parentBlockId;

    private Integer parentBlockNo;

    private Integer chunkNo;

    private String sectionPath;

    private Long structureNodeId;

    private Integer structureNodeType;

    private String canonicalPath;

    private Integer itemIndex;

    private Double score;

    private Integer subQuestionIndex;

    private String subQuestion;

    private String channel;

    private String toolName;

    private String knowledgeScopeCode;

    private String knowledgeScopeName;

    public SearchReference(String title, String url, String snippet) {
        this.sourceType = "WEB";
        this.title = title;
        this.url = url;
        this.snippet = snippet;
        this.channel = "web-search";
        this.toolName = "tavily_search";
    }

    public String uniqueKey() {
        if (parentBlockId != null) {
            return "PARENT:" + parentBlockId;
        }
        if (chunkId != null) {
            return "DOCUMENT:" + chunkId;
        }
        if (url != null && !url.isBlank()) {
            return "WEB:" + url;
        }
        return (sourceType == null ? "UNKNOWN" : sourceType)
            + ":" + (title == null ? "" : title)
            + ":" + (snippet == null ? "" : snippet);
    }
}
