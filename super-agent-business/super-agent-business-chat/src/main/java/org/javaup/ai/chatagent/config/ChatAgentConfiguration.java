package org.javaup.ai.chatagent.config;

import javax.sql.DataSource;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolerror.ToolErrorInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.CreateOption;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import org.javaup.ai.chatagent.support.DashScopeCompatibilityInterceptor;
import org.javaup.ai.chatagent.support.TavilyToolInputFallbackInterceptor;
import org.javaup.ai.chatagent.tool.TavilySearchRequest;
import org.javaup.ai.chatagent.tool.TavilySearchTool;
import org.javaup.ai.chatagent.tool.YoucomResearchRequest;
import org.javaup.ai.chatagent.tool.YoucomResearchTool;
import org.javaup.ai.chatagent.tool.YoucomSearchRequest;
import org.javaup.ai.chatagent.tool.YoucomSearchTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/
@Configuration
@EnableConfigurationProperties({ChatAgentProperties.class, TavilySearchProperties.class, YoucomSearchProperties.class, YoucomResearchProperties.class})
public class ChatAgentConfiguration {

    @Bean
    public MysqlSaver mysqlCheckpointSaver(DataSource dataSource) {

        return MysqlSaver.builder()
            .dataSource(dataSource)
            .createOption(CreateOption.CREATE_IF_NOT_EXISTS)
            .build();
    }

    @Bean
    public ToolCallback tavilySearchToolCallback(TavilySearchTool tavilySearchTool) {

        return FunctionToolCallback
            .builder("tavily_search", tavilySearchTool::search)
            .description("联网搜索最新信息、事实资料和网页来源。调用时必须传 JSON 参数，且至少包含非空 query；可选 topic 和 maxResults，其中 topic 仅允许 general、news、finance。")
            .inputType(TavilySearchRequest.class)
            .build();
    }

    @Bean
    public ToolCallback youcomSearchToolCallback(YoucomSearchTool youcomSearchTool) {

        return FunctionToolCallback
            .builder("youcom_search", youcomSearchTool::search)
            .description("通过 You.com Search API 联网搜索最新信息、事实资料和网页来源。调用时必须传 JSON 参数，至少包含非空 query；可选 maxResults 指定返回结果数量。")
            .inputType(YoucomSearchRequest.class)
            .build();
    }

    @Bean
    public ToolCallback youcomResearchToolCallback(YoucomResearchTool youcomResearchTool) {

        return FunctionToolCallback
            .builder("youcom_research", youcomResearchTool::research)
            .description("通过 You.com Research API 进行深度研究，返回带引用的高质量综述答案。调用时必须传 JSON 参数，至少包含非空 input（研究问题）；可选 researchEffort 指定研究深度（lite/standard/deep/exhaustive）。")
            .inputType(YoucomResearchRequest.class)
            .build();
    }

    @Bean
    public ReactAgent businessChatReactAgent(ChatModel chatModel,
                                             MysqlSaver mysqlCheckpointSaver,
                                             ToolCallback tavilySearchToolCallback,
                                             ToolCallback youcomSearchToolCallback,
                                             ToolCallback youcomResearchToolCallback,
                                             ChatAgentProperties chatAgentProperties,
                                             DashScopeCompatibilityInterceptor dashScopeCompatibilityInterceptor,
                                             TavilyToolInputFallbackInterceptor tavilyToolInputFallbackInterceptor) {
        return ReactAgent.builder()

            .name("business_chat_agent")
            .model(chatModel)
            .instruction(chatAgentProperties.getSystemPrompt())

            .tools(tavilySearchToolCallback, youcomSearchToolCallback, youcomResearchToolCallback)
            .saver(mysqlCheckpointSaver)

            .parallelToolExecution(true)
            .maxParallelTools(4)

            .hooks(
                ModelCallLimitHook.builder()
                    .runLimit(chatAgentProperties.getMaxModelCallsPerRun())
                    .threadLimit(chatAgentProperties.getMaxModelCallsPerThread())
                    .exitBehavior(ModelCallLimitHook.ExitBehavior.END)
                    .build(),
                ToolCallLimitHook.builder()
                    .toolName("tavily_search")
                    .runLimit(chatAgentProperties.getMaxToolCallsPerRun())
                    .threadLimit(chatAgentProperties.getMaxToolCallsPerThread())
                    .exitBehavior(ToolCallLimitHook.ExitBehavior.END)
                    .build(),
                ToolCallLimitHook.builder()
                    .toolName("youcom_search")
                    .runLimit(chatAgentProperties.getMaxToolCallsPerRun())
                    .threadLimit(chatAgentProperties.getMaxToolCallsPerThread())
                    .exitBehavior(ToolCallLimitHook.ExitBehavior.END)
                    .build(),
                ToolCallLimitHook.builder()
                    .toolName("youcom_research")
                    .runLimit(chatAgentProperties.getMaxToolCallsPerRun())
                    .threadLimit(chatAgentProperties.getMaxToolCallsPerThread())
                    .exitBehavior(ToolCallLimitHook.ExitBehavior.END)
                    .build()
            )

            .interceptors(
                dashScopeCompatibilityInterceptor,
                tavilyToolInputFallbackInterceptor,
                ToolRetryInterceptor.builder()
                    .toolName("tavily_search")
                    .maxRetries(2)
                    .initialDelay(200L)
                    .maxDelay(1200L)
                    .jitter(true)
                    .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                    .build(),
                ToolRetryInterceptor.builder()
                    .toolName("youcom_search")
                    .maxRetries(2)
                    .initialDelay(200L)
                    .maxDelay(1200L)
                    .jitter(true)
                    .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                    .build(),
                ToolRetryInterceptor.builder()
                    .toolName("youcom_research")
                    .maxRetries(2)
                    .initialDelay(200L)
                    .maxDelay(1200L)
                    .jitter(true)
                    .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                    .build(),
                ToolErrorInterceptor.builder().build()
            )
            .build();
    }
}
