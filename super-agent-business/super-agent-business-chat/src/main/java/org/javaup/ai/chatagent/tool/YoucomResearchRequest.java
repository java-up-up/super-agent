package org.javaup.ai.chatagent.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: You.com Research API 请求参数
 * @author: Claude
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoucomResearchRequest {

    private String input;
    private String researchEffort;
}
