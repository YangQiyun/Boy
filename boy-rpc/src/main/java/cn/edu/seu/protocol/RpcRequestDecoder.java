package cn.edu.seu.protocol;

import cn.edu.seu.protocol.standard.StandardProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RpcRequestDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        StandardProtocol.INSTANCE.decodeRequest(ctx, in, out);
    }
}
