package org.javaup.ai.manage.service;

import org.javaup.enums.DocumentFileTypeEnum;
import org.javaup.ai.manage.support.DocumentAnalysisResult;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

public interface DocumentParserService {

    DocumentAnalysisResult parse(byte[] bytes, String originalFileName, String mimeType, DocumentFileTypeEnum fileType);
}
