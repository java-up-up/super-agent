package org.javaup.ai.chatagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 视图对象
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResultView {

    private Long id;
    private String traceId;
    private int subQuestionIndex;
    private String subQuestion;
    private String channelType;
    private Integer channelRank;
    private Integer rrfRank;
    private Integer finalRank;
    private BigDecimal originalScore;
    private BigDecimal rrfScore;
    private BigDecimal rerankScore;
    private boolean gatePassed;
    private boolean isElevated;
    private boolean isSelected;
    private String selectionReason;
    private Long documentId;
    private String documentName;
    private Long chunkId;
    private Integer chunkNo;
    private Long parentBlockId;
    private Integer parentBlockNo;
    private String sectionPath;
    private String chunkTextPreview;
    private Integer chunkCharCount;
    private Instant createTime;
}
