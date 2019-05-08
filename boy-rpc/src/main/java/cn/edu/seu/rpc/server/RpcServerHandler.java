package cn.edu.seu.rpc.server;

import cn.edu.seu.protocol.standard.RpcHeader;
import cn.edu.seu.protocol.standard.RpcMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@ChannelHandler.Sharable
@Slf4j
public class RpcServerHandler extends ChannelInboundHandlerAdapter {

    private RpcServer rpcServer;

    public RpcServerHandler(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMessage<RpcHeader.RequestHeader> rpcMessage = (RpcMessage<RpcHeader.RequestHeader>) msg;
        WorkPool.INSTANCE.executeTask(new WorkPool.WorkTask(rpcServer, rpcMessage, ctx));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage(), cause);
        ctx.close();
    }
}
