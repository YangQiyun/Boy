package cn.edu.seu.rpc.server;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.config.ConfigManager;
import cn.edu.seu.config.Configs;
import cn.edu.seu.config.RpcConfigManager;
import cn.edu.seu.protocol.RpcRequestDecoder;
import cn.edu.seu.protocol.RpcResponseEncoder;
import cn.edu.seu.rpc.RpcMeta;
import cn.edu.seu.util.NettyEventLoopUtil;
import exception.EmptyException;
import io.netty.bootstrap.Bootstrap;
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

    public void init() {

        try {
            bootstrap = new ServerBootstrap();

            workGroup = NettyEventLoopUtil.newEventLoopGroup(configManager.getDefaultValue(Configs.RPC_SERVER_WORK_POOL),
                    new NamedThreadFactory("netty_work_loop", true));

            bootstrap.group(workGroup)
                    .channel(NettyEventLoopUtil.getServerSocketChannelClass())
                    .option(ChannelOption.TCP_NODELAY, configManager.getDefaultValue(Configs.TCP_NODELAY))
                    .option(ChannelOption.SO_BACKLOG, configManager.getDefaultValue(Configs.TCP_SO_BACKLOG))
                    .option(ChannelOption.SO_KEEPALIVE, configManager.getDefaultValue(Configs.TCP_SO_KEEPALIVE));

            NettyEventLoopUtil.enableTriggeredMode(bootstrap);

            ChannelHandler workHandler = new RpcServerHandler(this);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(1, 1, 1, null))
                            .addLast(new RPCServerChannelIdleHandler())
                            .addLast(new RpcRequestDecoder())
                            .addLast(workHandler)
                            .addLast(new RpcResponseEncoder());
                }
            });

        } catch (EmptyException e) {
            log.error(e.getMessage());
        }

    }


    public void registServer(Class service) {
        Class[] interfaces = service.getInterfaces();
        if (interfaces.length < 1) {
            log.warn("this service %s has not interface here", service.getName());
            return;
        }

        Class serviceInterface = service.getInterfaces()[0];
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

            ServerInfo serverInfo = new ServerInfo(servicName, methodName, method, method.getParameterCount(), method.getParameterTypes());
            serverInfoManager.addServerInfo(servicName, methodName, serverInfo);
            log.info("register service, serviceName={}, methodName={}",
                    serverInfo.getServerName(), serverInfo.getMethodName());
        }

    }

    public ServerInfoManager getServerInfoManager() {
        return serverInfoManager;
    }
}
