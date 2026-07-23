package org.javaup.ai.chatagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javaup.ai.chatagent.model.debug.ChatDebugTrace;
import org.javaup.enums.ChatTurnStatus;

import java.util.Date;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 视图对象
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationExchangeView {

    private long exchangeId;
    private String question;
    private String answer;
    private List<String> thinkingSteps;
    private List<SearchReference> references;
    private List<String> recommendations;
    private List<String> usedTools;
    private ChatDebugTrace debugTrace;
    private ChatTurnStatus status;
    private String errorMessage;
    private Long firstResponseTimeMs;
    private Long totalResponseTimeMs;
    private Date createTime;
    private Date editTime;
}
