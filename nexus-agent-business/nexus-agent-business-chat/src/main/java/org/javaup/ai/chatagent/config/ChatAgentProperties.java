package org.javaup.ai.chatagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置属性
 * @author: 阿星不是程序员
 **/
@ConfigurationProperties(prefix = "app.chat")
public class ChatAgentProperties {

    private boolean recommendationEnabled = true;
    private int maxModelCallsPerRun = 8;
    private int maxModelCallsPerThread = 40;
    private int maxToolCallsPerRun = 6;
    private int maxToolCallsPerThread = 30;
    private int historyPreviewTurns = 4;
    private long recommendationTimeoutMs = 3000L;
    private String systemPrompt = "";
    private String recommendationPrompt = "";

    public boolean isRecommendationEnabled() {
        return recommendationEnabled;
    }

    public void setRecommendationEnabled(boolean recommendationEnabled) {
        this.recommendationEnabled = recommendationEnabled;
    }

    public int getMaxModelCallsPerRun() {
        return maxModelCallsPerRun;
    }

    public void setMaxModelCallsPerRun(int maxModelCallsPerRun) {
        this.maxModelCallsPerRun = maxModelCallsPerRun;
    }

    public int getMaxModelCallsPerThread() {
        return maxModelCallsPerThread;
    }

    public void setMaxModelCallsPerThread(int maxModelCallsPerThread) {
        this.maxModelCallsPerThread = maxModelCallsPerThread;
    }

    public int getMaxToolCallsPerRun() {
        return maxToolCallsPerRun;
    }

    public void setMaxToolCallsPerRun(int maxToolCallsPerRun) {
        this.maxToolCallsPerRun = maxToolCallsPerRun;
    }

    public int getMaxToolCallsPerThread() {
        return maxToolCallsPerThread;
    }

    public void setMaxToolCallsPerThread(int maxToolCallsPerThread) {
        this.maxToolCallsPerThread = maxToolCallsPerThread;
    }

    public int getHistoryPreviewTurns() {
        return historyPreviewTurns;
    }

    public void setHistoryPreviewTurns(int historyPreviewTurns) {
        this.historyPreviewTurns = historyPreviewTurns;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getRecommendationPrompt() {
        return recommendationPrompt;
    }

    public void setRecommendationPrompt(String recommendationPrompt) {
        this.recommendationPrompt = recommendationPrompt;
    }

    public long getRecommendationTimeoutMs() {
        return recommendationTimeoutMs;
    }

    public void setRecommendationTimeoutMs(long recommendationTimeoutMs) {
        this.recommendationTimeoutMs = recommendationTimeoutMs;
    }
}
