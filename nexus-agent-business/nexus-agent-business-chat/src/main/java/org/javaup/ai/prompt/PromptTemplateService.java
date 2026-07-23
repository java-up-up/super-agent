package org.javaup.ai.prompt;

import org.springframework.ai.template.ValidationMode;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: Prompt 模板渲染组件
 * @author: 阿星不是程序员
 **/
@Component
public class PromptTemplateService {

    private static final String PROMPT_DIR = "prompt/";
    private static final String TEMPLATE_SUFFIX = ".st";

    private final ResourceLoader resourceLoader;
    private final StTemplateRenderer templateRenderer;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public PromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.templateRenderer = StTemplateRenderer.builder()
            .startDelimiterToken('<')
            .endDelimiterToken('>')
            .validationMode(ValidationMode.THROW)
            .build();
    }

    public String render(String templateName, Map<String, ?> variables) {
        String templatePath = normalizeTemplatePath(templateName);
        String template = templateCache.computeIfAbsent(templatePath, this::loadTemplate);
        return templateRenderer.apply(template, normalizeVariables(variables)).trim();
    }

    private Map<String, Object> normalizeVariables(Map<String, ?> variables) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (variables == null || variables.isEmpty()) {
            return normalized;
        }
        variables.forEach((key, value) -> normalized.put(key, value == null ? "" : value));
        return normalized;
    }

    private String normalizeTemplatePath(String templateName) {
        String normalized = templateName == null ? "" : templateName.trim();
        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith(PROMPT_DIR)) {
            normalized = PROMPT_DIR + normalized;
        }
        if (!normalized.endsWith(TEMPLATE_SUFFIX)) {
            normalized = normalized + TEMPLATE_SUFFIX;
        }
        return normalized;
    }

    private String loadTemplate(String templatePath) {
        Resource resource = resourceLoader.getResource("classpath:" + templatePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt 模板不存在: classpath:" + templatePath);
        }
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
        catch (Exception exception) {
            throw new IllegalStateException("读取 Prompt 模板失败: classpath:" + templatePath, exception);
        }
    }
}
