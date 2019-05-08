package cn.edu.seu.rpc.client;

import cn.edu.seu.protocol.standard.RpcHeader;
import cn.edu.seu.protocol.standard.RpcMessage;
import cn.edu.seu.protocol.standard.StandardProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClientHandler extends ChannelInboundHandlerAdapter {

    private RpcClient rpcClient;

    public RpcClientHandler(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        StandardProtocol.INSTANCE.processResponse(rpcClient, (RpcMessage<RpcHeader.ResponseHeader>) msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("now is dfsf" + cause.getMessage());
        log.error(cause.getMessage(), cause);
        ctx.close();
    }
}
