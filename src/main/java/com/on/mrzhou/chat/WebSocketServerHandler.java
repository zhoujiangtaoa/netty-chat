
package com.on.mrzhou.chat;

import com.alibaba.fastjson.JSON;
import com.on.mrzhou.chat.constants.Constants;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 通道组
     */
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 存储用户id和用户的channelId绑定
     */
    public static ConcurrentHashMap<String, ChannelId> userMap = new ConcurrentHashMap<>();

    Operation connertor;

    public WebSocketServerHandler(Operation connertor) {
        this.connertor = connertor;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object o) {

        //发送心跳消息
        if (o instanceof IdleStateEvent && ((IdleStateEvent) o).state().equals(IdleState.WRITER_IDLE)) {

            SocketMessage socketMessage = new SocketMessage();
            socketMessage.setMessageType(MessageProtocol.HEART_BEAT);
            receiveMessages(ctx,socketMessage);

            log.info(IdleState.WRITER_IDLE + "... from " + "-->" + ctx.channel().remoteAddress() + " id:" + ctx.channel().id().asShortText());
        }

        //如果心跳请求发出70秒内没收到响应，则关闭连接
        if (o instanceof IdleStateEvent && ((IdleStateEvent) o).state().equals(IdleState.READER_IDLE)) {
            log.info(IdleState.READER_IDLE + "... from " + " nid:" + ctx.channel().id().asShortText());
            Long lastTime = (Long) ctx.channel().attr(Constants.SERVER_SESSION_HEARBEAT).get();
            if (lastTime == null || ((System.currentTimeMillis() - lastTime) / 1000 >= Constants.PING_TIME_OUT)) {
                ctx.close();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg != null && msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            //获取用户参数
            String userId = getUrlParams(request.uri());
            if (Strings.isNotBlank(userId)) {
                userMap.put(userId, ctx.channel().id());
            }
            //如果url包含参数，需要处理
            if (request.uri().contains("?")) {
                String newUri = request.uri().substring(0, request.uri().indexOf("?"));
                request.setUri(newUri);
            }
        } else if (msg instanceof TextWebSocketFrame) {
            try {
                SocketMessage message = JSON.parseObject(((TextWebSocketFrame) msg).text(), SocketMessage.class);
                log.info(message.toString());
                receiveMessages(ctx, message);
            } catch (Exception e) {
                log.error("发送消息错误");
                throw e;
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) {}

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //添加到group
        channelGroup.add(ctx.channel());
        log.info("客户端连接成功");
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        channelGroup.remove(ctx.channel());
        log.info("客户端连接断开");
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Unexpected exception from downstream." + cause);
    }

    private static String getUrlParams(String url) {
        if (!url.contains("=")) {
            return null;
        }
        String userId = url.substring(url.indexOf("=") + 1);
        return userId;
    }

    private void receiveMessages(ChannelHandlerContext handler, SocketMessage message) {
        switch (message.getMessageType()) {
            case CONNECT:
                break;
            case CLOSE:
                connertor.close(handler);
                break;
            case HEART_BEAT:
                connertor.pushHeartApplyMessage(handler, message);
                break;
            case GROUP:
                connertor.pushGroupMessage(message);
                break;
            case SEND:
                connertor.pushMessage(handler, message);
                break;
            case HEART_BEAT_APPLY:
                connertor.heartbeatToClient(handler);
                break;
        }
    }
}
