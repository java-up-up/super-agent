package org.javaup.ai.manage.config;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

@Configuration
@EnableConfigurationProperties(DocumentManageProperties.class)
@ConditionalOnProperty(prefix = "app.manage.pgvector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentManagePgVectorConfiguration {

    @Bean(name = "documentManagePgVectorJdbcSupport")
    public DocumentManagePgVectorJdbcSupport documentManagePgVectorJdbcSupport(DocumentManageProperties properties) {
        DocumentManageProperties.PgVector pg = properties.getPgVector();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(buildJdbcUrl(pg));
        dataSource.setUsername(pg.getUsername());
        dataSource.setPassword(pg.getPassword());
        dataSource.setPoolName(pg.getPoolName());
        dataSource.setMaximumPoolSize(pg.getMaximumPoolSize());
        dataSource.setMinimumIdle(pg.getMinimumIdle());
        return new DocumentManagePgVectorJdbcSupport(dataSource);
    }

    @Bean(name = "documentManagePgVectorJdbcTemplate")
    public JdbcTemplate documentManagePgVectorJdbcTemplate(
        @Qualifier("documentManagePgVectorJdbcSupport") DocumentManagePgVectorJdbcSupport jdbcSupport) {
        return jdbcSupport.getJdbcTemplate();
    }

    private String buildJdbcUrl(DocumentManageProperties.PgVector pg) {
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
            .append(pg.getHost())
            .append(":")
            .append(pg.getPort())
            .append("/")
            .append(pg.getDatabase())
            .append("?stringtype=unspecified");
        if (StrUtil.isNotBlank(pg.getSchema())) {
            jdbcUrl.append("&currentSchema=").append(pg.getSchema());
        }
        return jdbcUrl.toString();
    }

    public static class DocumentManagePgVectorJdbcSupport implements DisposableBean {

        private final HikariDataSource dataSource;

        private final JdbcTemplate jdbcTemplate;

        public DocumentManagePgVectorJdbcSupport(HikariDataSource dataSource) {
            this.dataSource = dataSource;
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }

        public JdbcTemplate getJdbcTemplate() {
            return jdbcTemplate;
        }

        @Override
        public void destroy() {
            dataSource.close();
        }
    }
}
