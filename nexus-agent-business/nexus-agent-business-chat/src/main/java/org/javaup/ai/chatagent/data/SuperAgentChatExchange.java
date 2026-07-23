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

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 数据实体
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nexus_agent_chat_exchange")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentChatExchange extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("dialogue_code")
    private String conversationId;

    @TableField("user_prompt")
    private String question;

    @TableField("reply_content")
    private String answer;

    @TableField("reasoning_note_list")
    private String thinkingSteps;

    @TableField("source_snapshot_list")
    private String referenceList;

    @TableField("followup_suggestion_list")
    private String recommendationList;

    @TableField("tool_trace_list")
    private String usedToolList;

    @TableField("debug_trace_json")
    private String debugTraceJson;

    @TableField("exchange_state")
    private Integer turnStatus;

    @TableField("finish_note")
    private String errorMessage;

    @TableField("first_token_latency_ms")
    private Long firstResponseTimeMs;

    @TableField("total_latency_ms")
    private Long totalResponseTimeMs;
}
