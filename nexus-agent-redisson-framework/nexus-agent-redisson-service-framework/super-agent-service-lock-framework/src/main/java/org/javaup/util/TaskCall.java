package org.javaup.util;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 分布式锁 方法类型执行 有返回值的业务
 * @author: 阿星不是程序员
 **/
@FunctionalInterface
public interface TaskCall<V> {

    V call();
}
