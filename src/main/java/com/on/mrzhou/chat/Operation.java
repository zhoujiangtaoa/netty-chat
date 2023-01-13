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

    public void heartbeatToClient(ChannelHandlerContext ctx) {
        log.info("收到心跳回复，设置心跳响应时间,channel id" + ctx.channel().id());
        //设置心跳响应时间
        ctx.channel().attr(Constants.SERVER_SESSION_HEARBEAT).set(System.currentTimeMillis());
    }


    public void pushGroupMessage(SocketMessage message) {
        log.info("收到组消息,发送组消息");
        WebSocketServerHandler.channelGroup.writeAndFlush(message);
    }


    public void pushMessage(ChannelHandlerContext ctx, SocketMessage message) {
        log.info("收到消息,发送给接收方，channel id" + ctx.channel().id());
        String chatId = message.getChatId();
        if (WebSocketServerHandler.userMap.containsKey(chatId)) {
            ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message)));
        }
    }


    public void close(ChannelHandlerContext ctx) {
        log.info("收到关系连接消息,执行关闭连接，channel id" + ctx.channel().id());
        WebSocketServerHandler.channelGroup.remove(ctx.channel());
        ctx.close();
    }

    public void pushHeartApplyMessage(ChannelHandlerContext ctx, SocketMessage message) {
        log.info("给客户端发送心跳消息 channel id" + ctx.channel().id());
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message)));
    }
}
