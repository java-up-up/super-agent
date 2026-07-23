package org.javaup.ai.chatagent.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: RAG Prompt 装配结果
 * @author: 阿星不是程序员
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagPromptAssemblyResult {

    private String systemPrompt;

    private String userPrompt;

    private int totalBudget;

    private int perSubQuestionBudget;

    private int renderedReferenceCount;

    private int omittedReferenceCount;

    private List<String> renderedReferenceDetails;

    private List<String> omittedReferenceDetails;
}
