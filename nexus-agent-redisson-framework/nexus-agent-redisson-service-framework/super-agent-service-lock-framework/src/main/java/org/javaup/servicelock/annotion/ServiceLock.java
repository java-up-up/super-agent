package org.javaup.servicelock.annotion;

import org.javaup.servicelock.LockType;
import org.javaup.servicelock.info.LockTimeOutStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 分布式锁 注解
 * @author: 阿星不是程序员
 **/
@Target(value= {ElementType.TYPE, ElementType.METHOD})
@Retention(value= RetentionPolicy.RUNTIME)
public @interface ServiceLock {

    LockType lockType() default LockType.Reentrant;

    String name() default "";

    String [] keys();

    long waitTime() default 10;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    LockTimeOutStrategy lockTimeoutStrategy() default LockTimeOutStrategy.FAIL;

    String customLockTimeoutStrategy() default "";
}
