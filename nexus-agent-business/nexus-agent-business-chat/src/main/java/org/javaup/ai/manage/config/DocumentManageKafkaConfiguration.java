package org.javaup.ai.manage.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.javaup.ai.manage.config.DocumentManageProperties.Kafka;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

@EnableKafka
@Configuration
@EnableConfigurationProperties(DocumentManageProperties.class)
public class DocumentManageKafkaConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.manage.kafka", name = "auto-create-topics", havingValue = "true", matchIfMissing = true)
    public NewTopic documentParseTopic(DocumentManageProperties properties) {
        Kafka kafka = properties.getKafka();
        return TopicBuilder.name(kafka.getParseTopic()).partitions(1).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.manage.kafka", name = "auto-create-topics", havingValue = "true", matchIfMissing = true)
    public NewTopic documentIndexTopic(DocumentManageProperties properties) {
        Kafka kafka = properties.getKafka();
        return TopicBuilder.name(kafka.getIndexTopic()).partitions(1).replicas(1).build();
    }
}
