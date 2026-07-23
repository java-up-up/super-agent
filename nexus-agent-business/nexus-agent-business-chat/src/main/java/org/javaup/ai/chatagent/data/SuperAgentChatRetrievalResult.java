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

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 数据实体
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nexus_agent_chat_retrieval_result")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentChatRetrievalResult extends BaseTableData {

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

    @TableField("channel_rank")
    private Integer channelRank;

    @TableField("rrf_rank")
    private Integer rrfRank;

    @TableField("final_rank")
    private Integer finalRank;

    @TableField("original_score")
    private BigDecimal originalScore;

    @TableField("rrf_score")
    private BigDecimal rrfScore;

    @TableField("rerank_score")
    private BigDecimal rerankScore;

    @TableField("gate_passed")
    private Integer gatePassed;

    @TableField("is_elevated")
    private Integer isElevated;

    @TableField("is_selected")
    private Integer isSelected;

    @TableField("selection_reason")
    private String selectionReason;

    @TableField("document_id")
    private Long documentId;

    @TableField("document_name")
    private String documentName;

    @TableField("chunk_id")
    private Long chunkId;

    @TableField("chunk_no")
    private Integer chunkNo;

    @TableField("parent_block_id")
    private Long parentBlockId;

    @TableField("parent_block_no")
    private Integer parentBlockNo;

    @TableField("section_path")
    private String sectionPath;

    @TableField("chunk_text_preview")
    private String chunkTextPreview;

    @TableField("chunk_char_count")
    private Integer chunkCharCount;
}
