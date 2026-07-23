package org.javaup.ai.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线上演示只读模式配置。
 */
@Data
@ConfigurationProperties(prefix = "app.preview-mode")
public class PreviewModeProperties {

    /**
     * 是否开启只读展示模式。
     */
    private Boolean enabled = Boolean.FALSE;

    /**
     * 只读模式提示语。
     */
    private String message = "当前环境为只读展示模式，仅开放浏览与检索能力";
}
