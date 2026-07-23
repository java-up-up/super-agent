package org.javaup.ai.manage.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
import org.javaup.ai.manage.service.KnowledgeManageService;
import org.javaup.ai.manage.vo.DocumentProfileVo;
import org.javaup.ai.manage.vo.KnowledgeRouteTracePageVo;
import org.javaup.ai.manage.vo.KnowledgeScopeItemVo;
import org.javaup.ai.manage.vo.KnowledgeTopicItemVo;
import org.javaup.ai.manage.vo.TopicDocumentRelationItemVo;
import org.javaup.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 控制层
 * @author: 阿星不是程序员
 **/
@RestController
@RequestMapping("/manage/knowledge")
public class KnowledgeManageController {

    private final KnowledgeManageService knowledgeManageService;

    public KnowledgeManageController(KnowledgeManageService knowledgeManageService) {
        this.knowledgeManageService = knowledgeManageService;
    }

    @Operation(summary = "保存知识范围节点")
    @PostMapping("/scope/save")
    public ApiResponse<KnowledgeScopeItemVo> saveScope(@Valid @RequestBody KnowledgeScopeSaveDto dto) {
        return ApiResponse.ok(knowledgeManageService.saveScope(dto));
    }

    @Operation(summary = "删除知识范围节点")
    @PostMapping("/scope/delete")
    public ApiResponse<Boolean> deleteScope(@Valid @RequestBody KnowledgeScopeDeleteDto dto) {
        return ApiResponse.ok(knowledgeManageService.deleteScope(dto));
    }

    @Operation(summary = "查询知识范围列表")
    @PostMapping("/scope/list")
    public ApiResponse<List<KnowledgeScopeItemVo>> listScopes() {
        return ApiResponse.ok(knowledgeManageService.listScopes());
    }

    @Operation(summary = "保存知识主题节点")
    @PostMapping("/topic/save")
    public ApiResponse<KnowledgeTopicItemVo> saveTopic(@Valid @RequestBody KnowledgeTopicSaveDto dto) {
        return ApiResponse.ok(knowledgeManageService.saveTopic(dto));
    }

    @Operation(summary = "删除知识主题节点")
    @PostMapping("/topic/delete")
    public ApiResponse<Boolean> deleteTopic(@Valid @RequestBody KnowledgeTopicDeleteDto dto) {
        return ApiResponse.ok(knowledgeManageService.deleteTopic(dto));
    }

    @Operation(summary = "查询知识主题列表")
    @PostMapping("/topic/list")
    public ApiResponse<List<KnowledgeTopicItemVo>> listTopics(@RequestBody(required = false) KnowledgeTopicQueryDto dto) {
        return ApiResponse.ok(knowledgeManageService.listTopics(dto == null ? new KnowledgeTopicQueryDto() : dto));
    }

    @Operation(summary = "查询文档画像详情")
    @PostMapping("/document/profile/detail")
    public ApiResponse<DocumentProfileVo> queryProfile(@Valid @RequestBody DocumentProfileDetailQueryDto dto) {
        return ApiResponse.ok(knowledgeManageService.queryProfile(dto));
    }

    @Operation(summary = "重新生成文档画像")
    @PostMapping("/document/profile/regenerate")
    public ApiResponse<DocumentProfileVo> regenerateProfile(@Valid @RequestBody DocumentProfileRegenerateDto dto) {
        return ApiResponse.ok(knowledgeManageService.regenerateProfile(dto));
    }

    @Operation(summary = "批量重新生成文档画像")
    @PostMapping("/document/profile/batch/regenerate")
    public ApiResponse<List<DocumentProfileVo>> batchRegenerateProfiles(@Valid @RequestBody DocumentProfileBatchRegenerateDto dto) {
        return ApiResponse.ok(knowledgeManageService.batchRegenerateProfiles(dto));
    }

    @Operation(summary = "查询主题文档关联")
    @PostMapping("/topic/document/list")
    public ApiResponse<List<TopicDocumentRelationItemVo>> listTopicDocuments(@RequestBody(required = false) TopicDocumentRelationListQueryDto dto) {
        return ApiResponse.ok(knowledgeManageService.listTopicDocuments(dto == null ? new TopicDocumentRelationListQueryDto() : dto));
    }

    @Operation(summary = "保存主题文档关联")
    @PostMapping("/topic/document/save")
    public ApiResponse<TopicDocumentRelationItemVo> saveTopicDocumentRelation(@Valid @RequestBody TopicDocumentRelationSaveDto dto) {
        return ApiResponse.ok(knowledgeManageService.saveTopicDocumentRelation(dto));
    }

    @Operation(summary = "移除主题文档关联")
    @PostMapping("/topic/document/remove")
    public ApiResponse<Boolean> removeTopicDocumentRelation(@Valid @RequestBody TopicDocumentRelationRemoveDto dto) {
        return ApiResponse.ok(knowledgeManageService.removeTopicDocumentRelation(dto));
    }

    @Operation(summary = "分页查询知识路由追踪")
    @PostMapping("/route/trace/page/query")
    public ApiResponse<KnowledgeRouteTracePageVo> queryRouteTracePage(@RequestBody(required = false) KnowledgeRouteTraceQueryDto dto) {
        return ApiResponse.ok(knowledgeManageService.queryRouteTracePage(dto == null ? new KnowledgeRouteTraceQueryDto() : dto));
    }
}
