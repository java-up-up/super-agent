package org.javaup.ai.auth.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.javaup.ai.auth.config.PreviewModeProperties;
import org.javaup.ai.chatagent.support.StreamEventWriter;
import org.javaup.common.ApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 线上只读展示模式拦截器。
 */
@Component
public class PreviewModeInterceptor implements HandlerInterceptor {

    private static final Set<String> BLOCKED_PATHS = Set.of(
        "/api/chat/stream",
        "/api/chat/session/stop",
        "/api/chat/session/reset",
        "/api/chat/session/summary/rebuild",
        "/manage/document/upload",
        "/manage/document/delete",
        "/manage/document/strategy/confirm",
        "/manage/document/index/build",
        "/manage/knowledge/scope/save",
        "/manage/knowledge/scope/delete",
        "/manage/knowledge/topic/save",
        "/manage/knowledge/topic/delete",
        "/manage/knowledge/document/profile/regenerate",
        "/manage/knowledge/document/profile/batch/regenerate",
        "/manage/knowledge/topic/document/save",
        "/manage/knowledge/topic/document/remove"
    );

    private final PreviewModeProperties previewModeProperties;

    private final ObjectMapper objectMapper;

    private final StreamEventWriter streamEventWriter;

    public PreviewModeInterceptor(PreviewModeProperties previewModeProperties,
                                  ObjectMapper objectMapper,
                                  StreamEventWriter streamEventWriter) {
        this.previewModeProperties = previewModeProperties;
        this.objectMapper = objectMapper;
        this.streamEventWriter = streamEventWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!Boolean.TRUE.equals(previewModeProperties.getEnabled())) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!BLOCKED_PATHS.contains(path)) {
            return true;
        }

        if ("/api/chat/stream".equals(path)) {
            writeStreamReject(response);
        } else {
            writeJsonReject(response);
        }
        return false;
    }

    private void writeStreamReject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.getWriter().write("data: " + streamEventWriter.error(previewModeProperties.getMessage()) + "\n\n");
        response.getWriter().flush();
    }

    private void writeJsonReject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(previewModeProperties.getMessage())));
    }
}
