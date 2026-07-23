package org.javaup.ai.chatagent.service;

import org.javaup.ai.chatagent.model.ConversationExchangeView;
import org.javaup.ai.chatagent.model.SearchReference;
import org.javaup.ai.chatagent.model.debug.ChatDebugTrace;
import org.javaup.enums.ChatQueryMode;
import org.javaup.enums.ChatTurnStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

public interface ConversationArchiveStore {

    ConversationExchangeView startExchange(String conversationId,
                                           String question,
                                           ChatQueryMode chatMode,
                                           Long selectedDocumentId,
                                           String selectedDocumentName);

    void refreshSessionScope(String conversationId,
                             ChatQueryMode chatMode,
                             Long selectedDocumentId,
                             String selectedDocumentName);

    void completeExchange(String conversationId,
                          long exchangeId,
                          String answer,
                          List<String> thinkingSteps,
                          List<SearchReference> references,
                          List<String> recommendations,
                          List<String> usedTools,
                          ChatDebugTrace debugTrace,
                          ChatTurnStatus status,
                          String errorMessage,
                          Long firstResponseTimeMs,
                          Long totalResponseTimeMs);

    Optional<ConversationArchiveRecord> getSessionRecord(String conversationId);

    List<ConversationExchangeView> listExchanges(String conversationId);

    List<ConversationExchangeView> listExchangesAfter(String conversationId, long afterExchangeId);

    List<ConversationExchangeView> listRecentExchanges(String conversationId, int limit);

    List<ConversationArchiveRecord> listSessionRecords();

    ConversationArchivePage listSessionRecordPage(int pageNo,
                                                  int pageSize,
                                                  String keyword,
                                                  ChatQueryMode chatMode,
                                                  ChatTurnStatus latestTurnStatus);

    ConversationRemovalResult deleteSession(String conversationId);

    record ConversationArchiveRecord(
        String conversationId,
        boolean running,
        ChatQueryMode chatMode,
        Long selectedDocumentId,
        String selectedDocumentName,
        Instant createdAt,
        Instant updatedAt,
        List<ConversationExchangeView> exchanges
    ) {
    }

    record ConversationRemovalResult(
        int removedDialogueCount,
        int removedExchangeCount
    ) {
    }

    record ConversationArchivePage(
        long pageNo,
        long pageSize,
        long totalSize,
        List<ConversationArchiveRecord> records
    ) {
    }
}
