package com.baidu.fsg.uid.exception;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 异常类
 * @author: 阿星不是程序员
 **/

public class UidGenerateException extends RuntimeException {

    private static final long serialVersionUID = -27048199131316992L;

    public UidGenerateException() {
        super();
    }

    public UidGenerateException(String message, Throwable cause) {
        super(message, cause);
    }

    public UidGenerateException(String message) {
        super(message);
    }

    public UidGenerateException(String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
    }

    public UidGenerateException(Throwable cause) {
        super(cause);
    }

}
