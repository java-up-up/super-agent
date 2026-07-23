package org.javaup.ai.manage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 视图对象
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkDetailVo {

    private Long documentId;

    private Long taskId;

    private Long planId;

    private DocumentChunkItemVo chunk;

    private DocumentParentBlockItemVo parentBlock;

    private List<DocumentChunkItemVo> siblingChunks;
}
