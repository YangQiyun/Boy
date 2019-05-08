package cn.edu.seu.rpc.server;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.protocol.standard.RpcHeader;
import cn.edu.seu.protocol.standard.RpcMessage;
import cn.edu.seu.protocol.standard.StandardProtocol;
import cn.edu.seu.util.NettyEventLoopUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class WorkPool {

    public static final WorkPool INSTANCE = new WorkPool();

    private ExecutorService pool;

    public WorkPool() {
        pool = Executors.newFixedThreadPool(20, new NamedThreadFactory("workPool", true));
    }

    public static class WorkTask implements Runnable {

        private RpcServer rpcServer;

        private RpcMessage<RpcHeader.RequestHeader> request;

        private ChannelHandlerContext ctx;

        public WorkTask(RpcServer rpcServer, RpcMessage request, ChannelHandlerContext ctx) {
            this.rpcServer = rpcServer;
            this.request = request;
            this.ctx = ctx;
        }


        @Override
        public void run() {
            try {
                Object response = StandardProtocol.INSTANCE.processRequest(rpcServer, request);
                ctx.channel().writeAndFlush(response);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    public void executeTask(WorkTask task) {
        pool.submit(task);
    }
}
