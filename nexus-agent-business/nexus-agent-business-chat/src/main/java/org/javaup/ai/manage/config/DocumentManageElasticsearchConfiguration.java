package org.javaup.ai.manage.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

@Configuration
@EnableConfigurationProperties(DocumentManageProperties.class)
@ConditionalOnProperty(prefix = "app.manage.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentManageElasticsearchConfiguration {

    @Bean(name = "documentManageElasticsearchRestClient", destroyMethod = "close")
    public RestClient documentManageElasticsearchRestClient(DocumentManageProperties properties) {
        DocumentManageProperties.Elasticsearch elasticsearch = properties.getElasticsearch();
        if (CollUtil.isEmpty(elasticsearch.getUris())) {
            throw new IllegalStateException("app.manage.elasticsearch.uris 不能为空");
        }

        HttpHost[] hosts = elasticsearch.getUris().stream()
            .filter(StrUtil::isNotBlank)
            .map(HttpHost::create)
            .toArray(HttpHost[]::new);

        org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(hosts)
            .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(elasticsearch.getConnectTimeoutMillis())
                .setSocketTimeout(elasticsearch.getSocketTimeoutMillis()));

        if (StrUtil.isNotBlank(elasticsearch.getUsername())) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                    elasticsearch.getUsername(),
                    StrUtil.blankToDefault(elasticsearch.getPassword(), "")
                )
            );
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }

    @Bean(name = "documentManageElasticsearchTransport", destroyMethod = "close")
    public ElasticsearchTransport documentManageElasticsearchTransport(
        @Qualifier("documentManageElasticsearchRestClient") RestClient restClient,
        com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Bean(name = "documentManageElasticsearchClient")
    public ElasticsearchClient documentManageElasticsearchClient(
        @Qualifier("documentManageElasticsearchTransport") ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
