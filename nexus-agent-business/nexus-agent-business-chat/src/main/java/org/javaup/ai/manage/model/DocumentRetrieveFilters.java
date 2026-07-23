package org.javaup.ai.manage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 文档检索过滤提示
 * @author: 阿星不是程序员
 **/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRetrieveFilters {

    @Builder.Default
    private List<String> documentNameHints = new ArrayList<>();

    @Builder.Default
    private List<String> businessCategoryHints = new ArrayList<>();

    @Builder.Default
    private List<String> documentTagHints = new ArrayList<>();

    @Builder.Default
    private List<String> sectionPathHints = new ArrayList<>();

    @Builder.Default
    private List<String> canonicalPathHints = new ArrayList<>();

    @Builder.Default
    private List<Long> structureNodeIdHints = new ArrayList<>();

    @Builder.Default
    private List<Integer> itemIndexHints = new ArrayList<>();

    @Builder.Default
    private List<String> yearHints = new ArrayList<>();

    public boolean isEmpty() {
        return documentNameHints.isEmpty()
            && businessCategoryHints.isEmpty()
            && documentTagHints.isEmpty()
            && sectionPathHints.isEmpty()
            && canonicalPathHints.isEmpty()
            && structureNodeIdHints.isEmpty()
            && itemIndexHints.isEmpty()
            && yearHints.isEmpty();
    }
}
