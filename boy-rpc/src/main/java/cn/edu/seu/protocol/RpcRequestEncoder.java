package cn.edu.seu.protocol;

import cn.edu.seu.protocol.standard.RpcHeader;
import cn.edu.seu.protocol.standard.RpcMessage;
import cn.edu.seu.protocol.standard.StandardProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class RpcRequestEncoder extends MessageToMessageEncoder<RpcMessage<RpcHeader.RequestHeader>> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage<RpcHeader.RequestHeader> msg, List<Object> out) throws Exception {
        StandardProtocol.INSTANCE.encodeRequest(ctx, msg, out);
    }
}
