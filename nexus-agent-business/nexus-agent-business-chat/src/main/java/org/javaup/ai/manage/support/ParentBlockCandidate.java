package org.javaup.ai.manage.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentBlockCandidate {

    private String sectionPath;

    private Long structureNodeId;

    private Integer structureNodeType;

    private String canonicalPath;

    private Integer itemIndex;

    private String text;

    private Integer sourceType;

    private List<ChunkCandidate> childChunks = new ArrayList<>();

    public ParentBlockCandidate(String sectionPath,
                                String text,
                                Integer sourceType,
                                List<ChunkCandidate> childChunks) {
        this(sectionPath, null, null, "", null, text, sourceType, childChunks);
    }
}
