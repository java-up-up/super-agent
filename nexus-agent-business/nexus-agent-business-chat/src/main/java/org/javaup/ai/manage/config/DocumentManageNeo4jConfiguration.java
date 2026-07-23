package org.javaup.ai.manage.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

@Configuration
@ConditionalOnProperty(prefix = "app.manage.neo4j", name = "enabled", havingValue = "true")
public class DocumentManageNeo4jConfiguration {

    @Bean(destroyMethod = "close")
    public Driver documentManageNeo4jDriver(DocumentManageProperties properties) {
        DocumentManageProperties.Neo4j neo4j = properties.getNeo4j();
        Config config = Config.builder()
            .withConnectionTimeout(neo4j.getQueryTimeoutSeconds(), TimeUnit.SECONDS)
            .build();
        return GraphDatabase.driver(
            neo4j.getUri(),
            AuthTokens.basic(neo4j.getUsername(), neo4j.getPassword()),
            config
        );
    }
}
