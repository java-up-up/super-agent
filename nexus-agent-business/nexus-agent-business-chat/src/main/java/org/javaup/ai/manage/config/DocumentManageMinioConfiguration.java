package org.javaup.ai.manage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.javaup.enums.DocumentManageCode;
import org.javaup.exception.SuperAgentFrameException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 配置类
 * @author: 阿星不是程序员
 **/

@Slf4j
@Configuration
@EnableConfigurationProperties(DocumentManageProperties.class)
public class DocumentManageMinioConfiguration {

    @Bean
    public MinioClient documentMinioClient(DocumentManageProperties properties) {
        return MinioClient.builder()
            .endpoint(properties.getMinio().getEndpoint())
            .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
            .build();
    }

    @Bean
    public CommandLineRunner documentMinioBucketInitializer(MinioClient documentMinioClient,
                                                            DocumentManageProperties properties) {
        return args -> {
            String bucketName = properties.getMinio().getBucketName();
            try {
                boolean exists = documentMinioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!exists) {
                    documentMinioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("文档管理模块 MinIO bucket 不存在，已自动创建，bucket={}", bucketName);
                }
                else {
                    log.info("文档管理模块 MinIO bucket 已存在，bucket={}", bucketName);
                }
            }
            catch (Exception exception) {
                throw new SuperAgentFrameException(DocumentManageCode.DOCUMENT_STORAGE_FAILED.getCode(),
                    "初始化 MinIO bucket 失败: " + exception.getMessage(), exception);
            }
        };
    }
}
