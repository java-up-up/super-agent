package org.javaup.ai.chatagent.support;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

public record StreamEventMetadata(
    String conversationId,
    Long exchangeId
) {
}
