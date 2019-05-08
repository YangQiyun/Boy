package cn.edu.seu.rpc.server;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.config.ConfigManager;
import cn.edu.seu.config.Configs;
import cn.edu.seu.config.RpcConfigManager;
import cn.edu.seu.protocol.RpcRequestDecoder;
import cn.edu.seu.protocol.RpcResponseEncoder;
import cn.edu.seu.rpc.EndPoint;
import cn.edu.seu.rpc.RpcMeta;
import cn.edu.seu.util.NettyEventLoopUtil;
import exception.EmptyException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

@Slf4j
public class RpcServer {

    private EventLoopGroup workGroup;

    private EventLoopGroup boosGroup;

    private ServerBootstrap bootstrap;

    private ConfigManager configManager = RpcConfigManager.INSTANCE;

    private ServerInfoManager serverInfoManager = ServerInfoManager.INSTANCE;

    private EndPoint serverPoint;

    public RpcServer(EndPoint endPoint) {
        this.serverPoint = endPoint;
        init();
    }

    public void init() {

        try {
            bootstrap = new ServerBootstrap();

            boosGroup = NettyEventLoopUtil.newEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1,
                    new NamedThreadFactory("netty-boos-group", true));
            workGroup = NettyEventLoopUtil.newEventLoopGroup(configManager.getDefaultValue(Configs.RPC_SERVER_WORK_POOL),
                    new NamedThreadFactory("netty_server_work_loop", true));

            bootstrap
                    .channel(NettyEventLoopUtil.getServerSocketChannelClass())
                    .option(ChannelOption.SO_BACKLOG, configManager.getDefaultValue(Configs.TCP_SO_BACKLOG))
                    .childOption(ChannelOption.TCP_NODELAY, configManager.getDefaultValue(Configs.TCP_NODELAY))
                    .childOption(ChannelOption.SO_KEEPALIVE, configManager.getDefaultValue(Configs.TCP_SO_KEEPALIVE))
                    .childOption(ChannelOption.SO_REUSEADDR, configManager.getDefaultValue(Configs.TCP_SO_REUSEADDR));

            bootstrap.childOption(ChannelOption.SO_LINGER, 5);
            bootstrap.childOption(ChannelOption.SO_SNDBUF, 1024 * 64);
            bootstrap.childOption(ChannelOption.SO_RCVBUF, 1024 * 64);

            NettyEventLoopUtil.enableTriggeredMode(bootstrap);

            ChannelHandler workHandler = new RpcServerHandler(this);
            bootstrap.group(boosGroup, workGroup).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(60, 60, 0))
                            .addLast(new RPCServerChannelIdleHandler())
                            .addLast("encode", new RpcResponseEncoder())
                            .addLast("decoder", new RpcRequestDecoder())
                            .addLast("work", workHandler);
                }
            });

        } catch (EmptyException e) {
            log.error(e.getMessage());
        }

    }

    public void start() {
        try {
            bootstrap.bind(serverPoint.getPort()).sync();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    public void registServer(Object service) {
        Class[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length < 1) {
            log.warn("this service %s has not interface here", service.getClass().getName());
            return;
        }

        Class serviceInterface = service.getClass().getInterfaces()[0];
        for (Method method : serviceInterface.getDeclaredMethods()) {
            String servicName;
            String methodName;

            RpcMeta annotation = method.getAnnotation(RpcMeta.class);
            servicName = method.getDeclaringClass().getName();
            methodName = method.getName();
            if (null != annotation) {
                if (StringUtils.isNoneBlank(annotation.serviceName())) {
                    servicName = annotation.serviceName();
                }
                if (StringUtils.isNoneBlank(annotation.methodName())) {
                    methodName = annotation.methodName();
                }
            }

            ServerInfo serverInfo = new ServerInfo(service, servicName, methodName, method, method.getParameterCount(), method.getParameterTypes());
            serverInfoManager.addServerInfo(servicName, methodName, serverInfo);
            log.info("register service, serviceName={}, methodName={}",
                    serverInfo.getServerName(), serverInfo.getMethodName());
        }

    }

    public ServerInfoManager getServerInfoManager() {
        return serverInfoManager;
    }
}
