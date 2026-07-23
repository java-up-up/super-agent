package org.javaup.ai.manage.service;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

public interface DocumentTaskLogService {

    void saveLog(Long taskId,
                 Long documentId,
                 Integer stageType,
                 Integer eventType,
                 Integer logLevel,
                 Integer operatorType,
                 Long operatorId,
                 String content,
                 Object detail);
}
