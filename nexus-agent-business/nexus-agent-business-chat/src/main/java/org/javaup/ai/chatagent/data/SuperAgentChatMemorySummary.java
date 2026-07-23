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

import java.util.Date;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 数据实体
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nexus_agent_chat_memory_summary")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentChatMemorySummary extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("dialogue_code")
    private String conversationId;

    @TableField("covered_exchange_id")
    private Long coveredExchangeId;

    @TableField("covered_exchange_count")
    private Integer coveredExchangeCount;

    @TableField("compression_count")
    private Integer compressionCount;

    @TableField("summary_version")
    private Integer summaryVersion;

    @TableField("summary_text")
    private String summaryText;

    @TableField("summary_json")
    private String summaryJson;

    @TableField("last_source_edit_time")
    private Date lastSourceEditTime;
}
