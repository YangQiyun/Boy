package cn.edu.seu.connection;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.config.ConfigManager;
import cn.edu.seu.config.Configs;
import cn.edu.seu.config.RpcConfigManager;
import cn.edu.seu.exception.RemotingException;
import cn.edu.seu.protocol.RpcRequestDecoder;
import cn.edu.seu.protocol.RpcRequestEncoder;
import cn.edu.seu.protocol.RpcResponseDecoder;
import cn.edu.seu.rpc.EndPoint;
import cn.edu.seu.util.NettyEventLoopUtil;
import exception.EmptyException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AbstractConnectionFactory implements ConnectionFactory {

    private EventLoopGroup workGroup = NettyEventLoopUtil.newEventLoopGroup(
            Runtime.getRuntime().availableProcessors() + 1,
            new NamedThreadFactory("netty-work-group", true));

    private ConfigManager configManager = RpcConfigManager.INSTANCE;
    protected Bootstrap bootstrap;
    private ChannelHandler workHandler;

    private int normal_time = 1000;

    public AbstractConnectionFactory(ChannelHandler workHandler) {
        this.workHandler = workHandler;
    }

    public void init() {
        try {
            this.bootstrap = new Bootstrap();
            this.bootstrap.group(workGroup)
                    .channel(NettyEventLoopUtil.getClientSocketChannelClass())
                    .option(ChannelOption.TCP_NODELAY, configManager.getDefaultValue(Configs.TCP_NODELAY))
                    .option(ChannelOption.SO_REUSEADDR, configManager.getDefaultValue(Configs.TCP_SO_REUSEADDR))
                    .option(ChannelOption.SO_KEEPALIVE, configManager.getDefaultValue(Configs.TCP_SO_KEEPALIVE));

            // init byte buf allocator
            if (configManager.getDefaultValue(Configs.NETTY_BUFFER_POOLED)) {
                this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            } else {
                this.bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
            }

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast("encoder", new RpcRequestEncoder())
                            .addLast("decoder", new RpcResponseDecoder())
                            .addLast("handler", workHandler);
                }
            });
        } catch (EmptyException e) {
            log.error("init error");
        }
    }

    @Override
    public Connection createConnection(EndPoint endPoint) throws RemotingException {
        Channel channel = createChannel(endPoint, normal_time);
        if (channel != null) {
            return new Connection(channel, endPoint.getIp(), endPoint.getPort(), endPoint.getUniqueKey());
        } else {
            throw new RemotingException("create connection is error! more infos: " + endPoint);
        }
    }

    @Override
    public Connection createConnection(String targetIP, int targetPort, int connectTimeout) throws Exception {
        Channel channel = createChannel(new EndPoint(targetIP, targetPort, -1), connectTimeout);
        if (channel != null) {
            return new Connection(channel, targetIP, targetPort, "");
        } else {
            throw new RemotingException(String.format("create connection is error ! the ip is %s and port is %s", targetIP, targetPort));
        }
    }

    private Channel createChannel(EndPoint endPoint, int timeout) {

        try {
            timeout = Math.min(timeout, RpcConfigManager.INSTANCE.getDefaultValue(Configs.NETTY_CONNECT_TIMEOUT));
        } catch (EmptyException e) {
            log.error("config of " + Configs.NETTY_CONNECT_TIMEOUT + " is not init");
        }
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);

        ChannelFuture connectFuture = this.bootstrap.connect(endPoint.getIp(), endPoint.getPort());
        connectFuture.awaitUninterruptibly();

        if (!connectFuture.isDone()) {
            log.warn(endPoint + " connect timeout");
            return null;
        }

        if (connectFuture.isCancelled()) {
            log.warn(endPoint + " connect is canceled");
            return null;
        }

        if (!connectFuture.isSuccess()) {
            log.warn(endPoint + " connect is fail");
            return null;
        }

        return connectFuture.channel();
    }

}
