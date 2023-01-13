/**
 * **************************************************************************************
 *
 * @Author 1044053532@qq.com
 * @License http://www.apache.org/licenses/LICENSE-2.0
 * **************************************************************************************
 */
package com.on.mrzhou.chat;

import com.on.mrzhou.chat.constants.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class NettyServer {

    private final int port = 9000;

    @Resource
    Operation connertor;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void start() throws Exception {
        log.info("start netty server ...");

        // Server 服务启动
        ServerBootstrap bootstrap = new ServerBootstrap();
        //将netty的log 设置为 slf4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        //初始化服务器链接队列大小，服务端处理客户端链接请求是顺序处理的，所以同一时间只能处理一个客户端链接
        //多个客户端同时来的时候，服务端将不能处理的客户端链接请求放在队列中等待处理
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                // HTTP请求的解码和编码
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                // 把多个消息转换为一个单一的FullHttpRequest或是FullHttpResponse，
                // 原因是HTTP解码器会在每个HTTP消息中生成多个消息对象HttpRequest/HttpResponse,HttpContent,LastHttpContent
                pipeline.addLast(new HttpObjectAggregator(Constants.MAX_AGGREGATED_CONTENT_LENGTH));
                // 主要用于处理大数据流，比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的; 增加之后就不用考虑这个问题了
                pipeline.addLast(new ChunkedWriteHandler());
                // WebSocket数据压缩
                pipeline.addLast(new WebSocketServerCompressionHandler());
                // 协议包长度限制

                pipeline.addLast(new IdleStateHandler(Constants.READ_IDLE_TIME, Constants.WRITE_IDLE_TIME, 0));
                // 业务处理器
                pipeline.addLast(new WebSocketServerHandler(connertor));
                pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, Constants.MAX_FRAME_LENGTH));
            }
        });

        // 可选参数
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        // 绑定接口，同步等待成功
        log.info("start nettyserver at port[" + port + "].");
        ChannelFuture future = bootstrap.bind(port).sync();

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("nettyserver have success bind to " + port);
                } else {
                    log.error("nettyserver fail bind to " + port);
                }
            }
        });
        future.channel().closeFuture().syncUninterruptibly();
    }

}
