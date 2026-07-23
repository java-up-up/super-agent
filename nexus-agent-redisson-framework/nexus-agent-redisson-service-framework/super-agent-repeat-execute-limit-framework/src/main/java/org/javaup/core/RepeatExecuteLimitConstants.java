package org.javaup.core;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 防重复幂等业务名管理
 * @author: 阿星不是程序员
 **/
public class RepeatExecuteLimitConstants {

    public static final String CONSUMER_API_DATA_MESSAGE = "consumer_api_data_message";

    public static final String CREATE_PROGRAM_ORDER = "create_program_order";

    public final static String CANCEL_PROGRAM_ORDER = "cancel_program_order";

    public static final String CREATE_PROGRAM_ORDER_MQ = "create_program_order_mq";

    public static final String PROGRAM_CACHE_REVERSE_MQ = "program_cache_reverse_mq";

    public final static String PAY_OR_CANCEL_PROGRAM_ORDER = "pay_or_cancel_program_order";

    public final static String REDUCE_REMAIN_NUMBER = "reduce_remain_number";
}
