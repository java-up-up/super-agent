package org.javaup.servicelock.info;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 分布式锁 处理失败抽象
 * @author: 阿星不是程序员
 **/
public interface LockTimeOutHandler {

    void handler(String lockName);
}
