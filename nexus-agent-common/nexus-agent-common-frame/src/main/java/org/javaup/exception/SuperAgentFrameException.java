package org.javaup.exception;

import org.javaup.common.ApiResponse;
import org.javaup.enums.BaseCode;
import lombok.Data;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 异常类
 * @author: 阿星不是程序员
 **/

@Data
public class SuperAgentFrameException extends BaseException {

	private Integer code;

	private String message;

	public SuperAgentFrameException() {
		super();
	}

	public SuperAgentFrameException(String message) {
		super(message);
	}

	public SuperAgentFrameException(String code, String message) {
		super(message);
		this.code = Integer.parseInt(code);
		this.message = message;
	}

	public SuperAgentFrameException(Integer code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public SuperAgentFrameException(BaseCode baseCode) {
		super(baseCode.getMsg());
		this.code = baseCode.getCode();
		this.message = baseCode.getMsg();
	}

	public SuperAgentFrameException(ApiResponse apiResponse) {
		super(apiResponse.getMessage());
		this.code = apiResponse.getCode();
		this.message = apiResponse.getMessage();
	}

	public SuperAgentFrameException(Throwable cause) {
		super(cause);
	}

	public SuperAgentFrameException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}

	public SuperAgentFrameException(Integer code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.message = message;
	}
}
