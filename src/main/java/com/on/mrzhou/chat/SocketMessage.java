package com.on.mrzhou.chat;

import lombok.Data;

/**
 * @author zhoujiangtao
 */
@Data
public class SocketMessage {

    /**
     * 消息类型
     */
    private MessageProtocol messageType;
    /**
     * 消息发送者id
     */
    private String userId;
    /**
     * 消息接受者id或群聊id
     */
    private String chatId;
    /**
     * 消息内容
     */
    private String message;

    @Override
    public String toString() {
        return "用户" + this.getUserId() + "->接受者" + this.getChatId() + ",内容:" + this.getMessage() + ",类型:" + this.getMessageType() + "";
    }
}
