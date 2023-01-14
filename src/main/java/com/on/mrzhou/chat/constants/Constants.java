
package com.on.mrzhou.chat.constants;

import io.netty.util.AttributeKey;

public class Constants {

    public static final AttributeKey SERVER_SESSION_HEARBEAT = AttributeKey.valueOf("heartbeat");

    //连接空闲时间
    public static final int READ_IDLE_TIME = 60;//秒
    //发送心跳包循环时间
    public static final int WRITE_IDLE_TIME = 40;//秒
    //心跳响应 超时时间
    public static final int PING_TIME_OUT = 70; //秒   需大于空闲时间

    // 最大协议包长度
    public static final int MAX_FRAME_LENGTH = 1024 * 10; // 10k
    //
    public static final int MAX_AGGREGATED_CONTENT_LENGTH = 65536;


}
