package org.javaup.ai.manage.service;

import org.javaup.ai.manage.data.SuperAgentDocumentProfile;
import org.javaup.ai.manage.data.SuperAgentDocumentStructureNode;
import org.javaup.ai.manage.support.DocumentAnalysisResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/
public interface DocumentProfileService {

    SuperAgentDocumentProfile generateProfile(Long documentId,
                                              DocumentAnalysisResult analysisResult,
                                              List<SuperAgentDocumentStructureNode> structureNodes);

    SuperAgentDocumentProfile regenerateProfile(Long documentId);

    List<SuperAgentDocumentProfile> batchRegenerateProfiles(Collection<Long> documentIds);

    Optional<SuperAgentDocumentProfile> getByDocumentId(Long documentId);
}
