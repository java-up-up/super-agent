package org.javaup.ai.manage.service;

import org.javaup.ai.manage.dto.DocumentProfileBatchRegenerateDto;
import org.javaup.ai.manage.dto.DocumentProfileDetailQueryDto;
import org.javaup.ai.manage.dto.DocumentProfileRegenerateDto;
import org.javaup.ai.manage.dto.KnowledgeRouteTraceQueryDto;
import org.javaup.ai.manage.dto.KnowledgeScopeDeleteDto;
import org.javaup.ai.manage.dto.KnowledgeScopeSaveDto;
import org.javaup.ai.manage.dto.KnowledgeTopicDeleteDto;
import org.javaup.ai.manage.dto.KnowledgeTopicQueryDto;
import org.javaup.ai.manage.dto.KnowledgeTopicSaveDto;
import org.javaup.ai.manage.dto.TopicDocumentRelationListQueryDto;
import org.javaup.ai.manage.dto.TopicDocumentRelationRemoveDto;
import org.javaup.ai.manage.dto.TopicDocumentRelationSaveDto;
import org.javaup.ai.manage.vo.DocumentProfileVo;
import org.javaup.ai.manage.vo.KnowledgeRouteTracePageVo;
import org.javaup.ai.manage.vo.KnowledgeScopeItemVo;
import org.javaup.ai.manage.vo.KnowledgeTopicItemVo;
import org.javaup.ai.manage.vo.TopicDocumentRelationItemVo;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/
public interface KnowledgeManageService {

    KnowledgeScopeItemVo saveScope(KnowledgeScopeSaveDto dto);

    boolean deleteScope(KnowledgeScopeDeleteDto dto);

    List<KnowledgeScopeItemVo> listScopes();

    KnowledgeTopicItemVo saveTopic(KnowledgeTopicSaveDto dto);

    boolean deleteTopic(KnowledgeTopicDeleteDto dto);

    List<KnowledgeTopicItemVo> listTopics(KnowledgeTopicQueryDto dto);

    DocumentProfileVo queryProfile(DocumentProfileDetailQueryDto dto);

    DocumentProfileVo regenerateProfile(DocumentProfileRegenerateDto dto);

    List<DocumentProfileVo> batchRegenerateProfiles(DocumentProfileBatchRegenerateDto dto);

    List<TopicDocumentRelationItemVo> listTopicDocuments(TopicDocumentRelationListQueryDto dto);

    TopicDocumentRelationItemVo saveTopicDocumentRelation(TopicDocumentRelationSaveDto dto);

    boolean removeTopicDocumentRelation(TopicDocumentRelationRemoveDto dto);

    KnowledgeRouteTracePageVo queryRouteTracePage(KnowledgeRouteTraceQueryDto dto);
}
