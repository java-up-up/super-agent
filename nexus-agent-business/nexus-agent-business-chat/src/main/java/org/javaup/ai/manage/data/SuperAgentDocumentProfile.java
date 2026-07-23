package org.javaup.ai.manage.data;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("nexus_agent_document_profile")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentDocumentProfile extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long documentId;

    private Integer profileVersion;

    private String documentSummary;

    private String documentType;

    private String coreTopics;

    private String exampleQuestions;

    private Integer graphFriendly;

    private Integer supportsGraphOutline;

    private Integer supportsItemLookup;

    private Integer supportsGraphAssist;

    private String profileSource;

    private Integer profileStatus;

    private String errorMsg;
}
