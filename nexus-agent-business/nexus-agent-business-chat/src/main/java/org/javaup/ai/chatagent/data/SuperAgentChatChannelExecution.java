package org.javaup.ai.chatagent.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.javaup.database.data.BaseTableData;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 数据实体
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nexus_agent_chat_channel_execution")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentChatChannelExecution extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("dialogue_code")
    private String conversationId;

    @TableField("exchange_id")
    private Long exchangeId;

    @TableField("trace_id")
    private String traceId;

    @TableField("sub_question_index")
    private Integer subQuestionIndex;

    @TableField("sub_question")
    private String subQuestion;

    @TableField("channel_type")
    private String channelType;

    @TableField("execution_state")
    private Integer executionState;

    @TableField("start_time")
    private Date startTime;

    @TableField("end_time")
    private Date endTime;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("recalled_count")
    private Integer recalledCount;

    @TableField("accepted_count")
    private Integer acceptedCount;

    @TableField("final_selected_count")
    private Integer finalSelectedCount;

    @TableField("avg_score")
    private BigDecimal avgScore;

    @TableField("max_score")
    private BigDecimal maxScore;

    @TableField("min_score")
    private BigDecimal minScore;

    @TableField("config_snapshot")
    private String configSnapshot;

    @TableField("error_message")
    private String errorMessage;
}
