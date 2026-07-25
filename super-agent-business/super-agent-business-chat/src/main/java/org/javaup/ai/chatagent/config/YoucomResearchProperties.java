package org.javaup.ai.chatagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: You.com Research API 配置属性
 * @author: Claude
 **/
@ConfigurationProperties(prefix = "app.youcom.research")
public class YoucomResearchProperties {

    private boolean enabled = true;
    private String baseUrl = "https://ydc-index.io/v1";
    private String researchPath = "/research";
    private String apiKey;
    private String researchEffort = "standard";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 60000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getResearchPath() {
        return researchPath;
    }

    public void setResearchPath(String researchPath) {
        this.researchPath = researchPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getResearchEffort() {
        return researchEffort;
    }

    public void setResearchEffort(String researchEffort) {
        this.researchEffort = researchEffort;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
