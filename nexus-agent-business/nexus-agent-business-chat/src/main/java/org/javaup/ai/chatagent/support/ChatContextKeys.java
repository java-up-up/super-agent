package org.javaup.ai.chatagent.support;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/
public final class ChatContextKeys {

    public static final String EVENT_SINK = "chat.event.sink";
    public static final String EVENT_METADATA = "chat.event.metadata";
    public static final String DEBUG_TRACE = "chat.debug.trace";
    public static final String TRACE_ID = "chat.trace.id";
    public static final String REFERENCES = "chat.references";
    public static final String USED_TOOLS = "chat.used.tools";
    public static final String THINKING_STEPS = "chat.thinking.steps";
    public static final String QUESTION = "chat.question";
    public static final String CHAT_MODE = "chat.mode";
    public static final String CURRENT_DATE = "chat.current.date";
    public static final String CURRENT_DATE_TEXT = "chat.current.date.text";
    public static final String SELECTED_DOCUMENT_ID = "chat.selected.document.id";
    public static final String SELECTED_DOCUMENT_NAME = "chat.selected.document.name";
    public static final String SELECTED_TASK_ID = "chat.selected.task.id";

    private ChatContextKeys() {
    }
}
