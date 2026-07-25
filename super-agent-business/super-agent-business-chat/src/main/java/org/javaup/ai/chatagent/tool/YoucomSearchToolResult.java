package org.javaup.ai.chatagent.tool;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javaup.ai.chatagent.model.SearchReference;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: You.com Search API 返回结果
 * @author: Claude
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoucomSearchToolResult {

    private String query;
    private List<SearchReference> results;
}
