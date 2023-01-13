package com.on.mrzhou.chat;


import com.alibaba.fastjson.JSON;
import com.on.mrzhou.chat.constants.Constants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Operation {

    public void heartbeatToClient(ChannelHandlerContext hander) {
        //设置心跳响应时间
        hander.channel().attr(Constants.SERVER_SESSION_HEARBEAT).set(System.currentTimeMillis());
    }


    public void pushGroupMessage(SocketMessage message)
            throws RuntimeException {
        WebSocketServerHandler.channelGroup.writeAndFlush(message);
    }


    public void pushMessage(ChannelHandlerContext ctx, SocketMessage message) throws RuntimeException {
        try {
            String chatId = message.getChatId();
            if (WebSocketServerHandler.userMap.containsKey(chatId)) {
                ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message)));
            }
        } catch (Exception e) {
            log.error("connector pushMessage  Exception.", e);
            throw new RuntimeException(e.getCause());
        }
    }


    public void close(ChannelHandlerContext hander, SocketMessage message) {
        close(hander);
        log.warn("connector close channel sessionId -> " + ", ctx -> " + hander.toString());
    }


    public void close(ChannelHandlerContext hander) {
        WebSocketServerHandler.channelGroup.remove(hander.channel());
        hander.close();
    }

}
