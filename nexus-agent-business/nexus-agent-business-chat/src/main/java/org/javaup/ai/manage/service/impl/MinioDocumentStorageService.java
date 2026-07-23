package org.javaup.ai.manage.service.impl;

import lombok.AllArgsConstructor;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.javaup.ai.manage.config.DocumentManageProperties;
import org.javaup.ai.manage.service.DocumentStorageService;
import org.javaup.ai.manage.support.StoredObjectInfo;
import org.javaup.enums.DocumentManageCode;
import org.javaup.exception.SuperAgentFrameException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务实现层
 * @author: 阿星不是程序员
 **/

@AllArgsConstructor
@Service
public class MinioDocumentStorageService implements DocumentStorageService {

    private final MinioClient minioClient;

    private final DocumentManageProperties properties;

    @Override
    public StoredObjectInfo uploadOriginalFile(Long documentId, String originalFileName, byte[] bytes, String contentType) {

        String objectName = properties.getMinio().getObjectPrefix() + "/" + documentId + "/" + System.currentTimeMillis() + "-" + originalFileName;
        upload(objectName, bytes, contentType);
        return new StoredObjectInfo(properties.getMinio().getBucketName(), objectName, buildObjectUrl(objectName));
    }

    @Override
    public String uploadParsedText(Long documentId, String parsedText) {

        String objectName = properties.getMinio().getParsedTextPrefix() + "/" + documentId + ".txt";
        upload(objectName, parsedText.getBytes(StandardCharsets.UTF_8), "text/plain;charset=UTF-8");
        return objectName;
    }

    @Override
    public byte[] downloadObject(String objectName) {
        try (InputStream inputStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(properties.getMinio().getBucketName())
                .object(objectName)
                .build())) {

            return inputStream.readAllBytes();
        }
        catch (Exception exception) {
            throw new SuperAgentFrameException(DocumentManageCode.DOCUMENT_STORAGE_FAILED.getCode(),
                "下载 MinIO 文件失败: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String downloadText(String objectName) {

        return new String(downloadObject(objectName), StandardCharsets.UTF_8);
    }

    @Override
    public void deleteObjects(List<String> objectNameList) {
        if (CollUtil.isEmpty(objectNameList)) {
            return;
        }

        List<String> validObjectNameList = objectNameList.stream()
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
        if (validObjectNameList.isEmpty()) {
            return;
        }

        try {

            if (!bucketExists()) {
                return;
            }

            for (String objectName : validObjectNameList) {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(properties.getMinio().getBucketName())
                        .object(objectName)
                        .build()
                );
            }
        }
        catch (Exception exception) {
            throw new SuperAgentFrameException(DocumentManageCode.DOCUMENT_STORAGE_FAILED.getCode(),
                "删除 MinIO 文件失败: " + exception.getMessage(), exception);
        }
    }

    private void upload(String objectName, byte[] bytes, String contentType) {
        try {

            ensureBucketExists();
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucketName())
                    .object(objectName)
                    .contentType(StrUtil.isNotBlank(contentType) ? contentType : "application/octet-stream")
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .build()
            );
        }
        catch (Exception exception) {
            throw new SuperAgentFrameException(DocumentManageCode.DOCUMENT_STORAGE_FAILED.getCode(),
                "上传 MinIO 文件失败: " + exception.getMessage(), exception);
        }
    }

    private void ensureBucketExists() throws Exception {
        if (!bucketExists()) {

            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getMinio().getBucketName()).build());
        }
    }

    private boolean bucketExists() throws Exception {
        String bucketName = properties.getMinio().getBucketName();
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    private String buildObjectUrl(String objectName) {
        String endpoint = properties.getMinio().getEndpoint();
        if (endpoint.endsWith("/")) {

            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + properties.getMinio().getBucketName() + "/" + objectName;
    }
}
