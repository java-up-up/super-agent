package org.javaup.ai.chatagent.model.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 视图对象
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTraceStageView {

    private long stageId;

    private String traceId;

    private String stageCode;

    private String stageName;

    private Integer stageOrder;

    private Integer stageLevel;

    private Long parentStageId;

    private String executionMode;

    private String stageState;

    private Instant startTime;

    private Instant endTime;

    private Long durationMs;

    private String summaryText;

    private String errorMessage;

    private Map<String, Object> snapshot;
}
