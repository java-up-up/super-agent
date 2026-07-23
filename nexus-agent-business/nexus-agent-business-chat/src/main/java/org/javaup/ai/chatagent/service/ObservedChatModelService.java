package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.model.debug.ChatModelUsageTrace;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
@Service
public class ObservedChatModelService {

    private final ChatModel chatModel;

    public ObservedChatModelService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String callText(String stageName,
                           String systemPrompt,
                           String userPrompt,
                           ConversationTraceRecorder traceRecorder) {
        return callText(stageName, systemPrompt, userPrompt, null, traceRecorder);
    }

    public String callText(String stageName,
                           String systemPrompt,
                           String userPrompt,
                           ChatOptions callOptions,
                           ConversationTraceRecorder traceRecorder) {
        long startTime = System.currentTimeMillis();
        String provider = resolveProvider();
        String model = resolveModel();
        try {
            ChatOptions effectiveOptions = mergeOptions(callOptions);
            logStageCallOptions(stageName, provider, model, effectiveOptions);
            ChatResponse response = chatModel.call(buildPrompt(systemPrompt, userPrompt, effectiveOptions));
            String responseText = response == null || response.getResult() == null || response.getResult().getOutput() == null
                ? ""
                : StrUtil.blankToDefault(response.getResult().getOutput().getText(), "");
            ChatModelUsageTrace usageTrace = buildUsageTrace(
                stageName,
                provider,
                model,
                response == null ? null : response.getMetadata(),
                System.currentTimeMillis() - startTime,
                "COMPLETED",
                systemPrompt,
                userPrompt,
                responseText
            );
            appendUsage(traceRecorder, usageTrace);
            return responseText;
        }
        catch (RuntimeException exception) {
            appendUsage(traceRecorder, ChatModelUsageTrace.builder()
                .stageName(stageName)
                .provider(provider)
                .model(model)
                .durationMs(System.currentTimeMillis() - startTime)
                .promptTokens(estimateTokens(systemPrompt) + estimateTokens(userPrompt))
                .status("FAILED")
                .build());
            throw exception;
        }
    }

    public Flux<String> streamText(String stageName,
                                   String systemPrompt,
                                   String userPrompt,
                                   ConversationTraceRecorder traceRecorder) {
        String provider = resolveProvider();
        String model = resolveModel();
        long startTime = System.currentTimeMillis();
        AtomicReference<ChatResponseMetadata> metadataRef = new AtomicReference<>();
        AtomicLong durationRef = new AtomicLong(0L);
        StringBuilder outputBuilder = new StringBuilder();

        return chatModel.stream(buildPrompt(systemPrompt, userPrompt))
            .map(response -> {
                if (response != null && response.getMetadata() != null) {
                    metadataRef.set(response.getMetadata());
                }
                if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                    return "";
                }
                return StrUtil.blankToDefault(response.getResult().getOutput().getText(), "");
            })
            .filter(StrUtil::isNotBlank)
            .doOnNext(outputBuilder::append)
            .doOnComplete(() -> {
                long durationMs = System.currentTimeMillis() - startTime;
                durationRef.set(durationMs);
                appendUsage(traceRecorder, buildUsageTrace(stageName, provider, model, metadataRef.get(), durationMs, "COMPLETED", systemPrompt, userPrompt, outputBuilder.toString()));
            })
            .doOnError(error -> appendUsage(traceRecorder, ChatModelUsageTrace.builder()
                .stageName(stageName)
                .provider(provider)
                .model(model)
                .promptTokens(estimateTokens(systemPrompt) + estimateTokens(userPrompt))
                .completionTokens(estimateTokens(outputBuilder.toString()))
                .totalTokens(estimateTokens(systemPrompt) + estimateTokens(userPrompt) + estimateTokens(outputBuilder.toString()))
                .estimatedCost(estimateCost(model, estimateTokens(systemPrompt) + estimateTokens(userPrompt), estimateTokens(outputBuilder.toString())))
                .durationMs(durationRef.get() > 0 ? durationRef.get() : System.currentTimeMillis() - startTime)
                .status("FAILED")
                .build()));
    }

    private Prompt buildPrompt(String systemPrompt, String userPrompt) {
        return buildPrompt(systemPrompt, userPrompt, null);
    }

    private Prompt buildPrompt(String systemPrompt, String userPrompt, ChatOptions callOptions) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(StrUtil.blankToDefault(userPrompt, "")));
        ChatOptions mergedOptions = mergeOptions(callOptions);
        return mergedOptions == null ? new Prompt(messages) : new Prompt(messages, mergedOptions);
    }

    private ChatOptions mergeOptions(ChatOptions callOptions) {
        if (callOptions == null) {
            return null;
        }
        ChatOptions defaultOptions = chatModel.getDefaultOptions();
        if (defaultOptions instanceof OpenAiChatOptions defaultOpenAi
            && callOptions instanceof OpenAiChatOptions overrideOpenAi) {
            OpenAiChatOptions merged = defaultOpenAi.copy();
            if (overrideOpenAi.getModel() != null) {
                merged.setModel(overrideOpenAi.getModel());
            }
            if (overrideOpenAi.getTemperature() != null) {
                merged.setTemperature(overrideOpenAi.getTemperature());
            }
            if (overrideOpenAi.getTopP() != null) {
                merged.setTopP(overrideOpenAi.getTopP());
            }
            if (overrideOpenAi.getReasoningEffort() != null) {
                merged.setReasoningEffort(overrideOpenAi.getReasoningEffort());
            }
            if (overrideOpenAi.getVerbosity() != null) {
                merged.setVerbosity(overrideOpenAi.getVerbosity());
            }
            if (overrideOpenAi.getExtraBody() != null && !overrideOpenAi.getExtraBody().isEmpty()) {
                Map<String, Object> mergedExtraBody = new LinkedHashMap<>();
                if (merged.getExtraBody() != null && !merged.getExtraBody().isEmpty()) {
                    mergedExtraBody.putAll(merged.getExtraBody());
                }
                mergedExtraBody.putAll(overrideOpenAi.getExtraBody());
                merged.setExtraBody(mergedExtraBody);
            }
            return merged;
        }
        return callOptions;
    }

    private void logStageCallOptions(String stageName,
                                     String provider,
                                     String fallbackModel,
                                     ChatOptions effectiveOptions) {
        if (!(effectiveOptions instanceof OpenAiChatOptions openAiOptions)) {
            if (effectiveOptions != null) {
                log.info("模型调用参数: stage={}, provider={}, model={}, optionsClass={}",
                    StrUtil.blankToDefault(stageName, ""),
                    provider,
                    fallbackModel,
                    effectiveOptions.getClass().getName());
            }
            return;
        }
        log.info("模型调用参数: stage={}, provider={}, model={}, temperature={}, topP={}, reasoningEffort={}, verbosity={}, extraBody={}",
            StrUtil.blankToDefault(stageName, ""),
            provider,
            StrUtil.blankToDefault(openAiOptions.getModel(), fallbackModel),
            openAiOptions.getTemperature(),
            openAiOptions.getTopP(),
            StrUtil.blankToDefault(openAiOptions.getReasoningEffort(), ""),
            StrUtil.blankToDefault(openAiOptions.getVerbosity(), ""),
            openAiOptions.getExtraBody() == null ? Map.of() : openAiOptions.getExtraBody());
    }

    private void appendUsage(ConversationTraceRecorder traceRecorder, ChatModelUsageTrace trace) {
        if (traceRecorder != null && trace != null) {
            traceRecorder.addModelUsageTrace(trace);
        }
    }

    private ChatModelUsageTrace buildUsageTrace(String stageName,
                                                String provider,
                                                String model,
                                                ChatResponseMetadata metadata,
                                                long durationMs,
                                                String status,
                                                String systemPrompt,
                                                String userPrompt,
                                                String responseText) {
        Usage usage = metadata == null ? null : metadata.getUsage();
        Integer promptTokens = usage == null ? null : usage.getPromptTokens();
        Integer completionTokens = usage == null ? null : usage.getCompletionTokens();
        Integer totalTokens = usage == null ? null : usage.getTotalTokens();
        if (promptTokens == null || promptTokens <= 0) {
            promptTokens = estimateTokens(systemPrompt) + estimateTokens(userPrompt);
        }
        if (completionTokens == null || completionTokens <= 0) {
            completionTokens = estimateTokens(responseText);
        }
        if (totalTokens == null || totalTokens <= 0) {
            totalTokens = (promptTokens == null ? 0 : promptTokens) + (completionTokens == null ? 0 : completionTokens);
        }
        return ChatModelUsageTrace.builder()
            .stageName(stageName)
            .provider(provider)
            .model(StrUtil.blankToDefault(metadata == null ? model : metadata.getModel(), model))
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .totalTokens(totalTokens)
            .estimatedCost(estimateCost(model, promptTokens, completionTokens))
            .durationMs(durationMs)
            .status(status)
            .build();
    }

    private Integer estimateTokens(String content) {
        if (StrUtil.isBlank(content)) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(content.trim().length() / 4.0));
    }

    private String resolveProvider() {
        String className = chatModel.getClass().getName().toLowerCase();
        if (className.contains("deepseek")) {
            return "deepseek";
        }
        if (className.contains("openai")) {
            return "openai-compatible";
        }
        if (className.contains("ollama")) {
            return "ollama";
        }
        return "unknown";
    }

    private String resolveModel() {
        ChatOptions options = chatModel.getDefaultOptions();
        return options == null ? "" : StrUtil.blankToDefault(options.getModel(), "");
    }

    private Double estimateCost(String model, Integer promptTokens, Integer completionTokens) {
        if ((promptTokens == null || promptTokens <= 0) && (completionTokens == null || completionTokens <= 0)) {
            return null;
        }
        String normalizedModel = StrUtil.blankToDefault(model, "").toLowerCase();
        double promptRatePer1k;
        double completionRatePer1k;
        if (normalizedModel.contains("qwen-plus")) {
            promptRatePer1k = 0.004;
            completionRatePer1k = 0.012;
        }
        else if (normalizedModel.contains("deepseek")) {
            promptRatePer1k = 0.002;
            completionRatePer1k = 0.008;
        }
        else {
            promptRatePer1k = 0.0;
            completionRatePer1k = 0.0;
        }
        double promptCost = (promptTokens == null ? 0D : promptTokens / 1000D) * promptRatePer1k;
        double completionCost = (completionTokens == null ? 0D : completionTokens / 1000D) * completionRatePer1k;
        double total = promptCost + completionCost;
        return total > 0D ? total : null;
    }
}
