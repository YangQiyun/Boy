package cn.edu.seu.protocol;

import cn.edu.seu.protocol.standard.RpcHeader;
import cn.edu.seu.protocol.standard.RpcMessage;
import cn.edu.seu.protocol.standard.StandardProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class RpcResponseEncoder extends MessageToMessageEncoder<RpcMessage<RpcHeader.ResponseHeader>> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage<RpcHeader.ResponseHeader> msg, List<Object> out) throws Exception {
        StandardProtocol.INSTANCE.encodeResponse(ctx, msg, out);
    }
}
