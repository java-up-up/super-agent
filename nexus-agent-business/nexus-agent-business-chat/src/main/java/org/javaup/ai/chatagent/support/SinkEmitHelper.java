package org.javaup.ai.chatagent.support;

import reactor.core.publisher.Sinks;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/
public class SinkEmitHelper {

    private SinkEmitHelper() {
    }

    public static void emitNext(Sinks.Many<String> sink, String payload) {

        if (sink == null || payload == null) {
            return;
        }

        synchronized (sink) {
            Sinks.EmitResult result = sink.tryEmitNext(payload);

            if (result == Sinks.EmitResult.FAIL_CANCELLED
                || result == Sinks.EmitResult.FAIL_TERMINATED
                || result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                return;
            }
            if (result.isFailure()) {
                throw new IllegalStateException("流式事件发送失败: " + result);
            }
        }
    }

    public static void emitComplete(Sinks.Many<String> sink) {
        if (sink == null) {
            return;
        }
        synchronized (sink) {
            Sinks.EmitResult result = sink.tryEmitComplete();
            if (result == Sinks.EmitResult.FAIL_CANCELLED
                || result == Sinks.EmitResult.FAIL_TERMINATED
                || result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                return;
            }
            if (result.isFailure()) {
                throw new IllegalStateException("流式事件关闭失败: " + result);
            }
        }
    }
}
