package com.hrms.dto;

import java.util.List;
import java.util.Map;

/**
 * 智能助手回复体。
 * chart / table 均可为空；suggestions 为推荐追问，前端渲染为可点击的快捷问题。
 */
public record AssistantReply(
        String intent,
        String title,
        String reply,
        Map<String, Object> chart,
        Map<String, Object> table,
        List<String> suggestions
) {
    public static AssistantReply text(String intent, String reply, List<String> suggestions) {
        return new AssistantReply(intent, null, reply, null, null, suggestions);
    }

    public static AssistantReply withChart(String intent, String title, String reply,
                                           Map<String, Object> chart, List<String> suggestions) {
        return new AssistantReply(intent, title, reply, chart, null, suggestions);
    }

    public static AssistantReply withTable(String intent, String title, String reply,
                                           Map<String, Object> table, List<String> suggestions) {
        return new AssistantReply(intent, title, reply, null, table, suggestions);
    }
}
