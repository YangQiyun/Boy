package cn.edu.seu.protocol.standard;

import cn.edu.seu.rpc.RpcCallback;
import cn.edu.seu.rpc.RpcClient;
import cn.edu.seu.rpc.RpcFuture;
import cn.edu.seu.rpc.RpcMeta;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class StandardProtocol {

    public static final StandardProtocol INSTANCE = new StandardProtocol();

    // 协议参数
    // 最小的长度,八个字节是两个int长度，标定了header和body的长度
    private int lessLen = 8;

    public void decodeRequest(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decode(ctx, in, out, true);
    }

    public void decodeResponse(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decode(ctx, in, out, false);
    }

    public void encodeRequest(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        encode(ctx, msg, out, true);
    }

    public void encodeResponse(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        encode(ctx, msg, out, false);
    }

    public RpcMessage<RpcHeader.RequestHeader> newRequest(Long requestId, Method method, Object request) {
        RpcMessage<RpcHeader.RequestHeader> rpcMessage = new RpcMessage<>();
        RpcHeader.RequestHeader.Builder builder = RpcHeader.RequestHeader.newBuilder();
        builder.setRequestId(requestId);

        RpcMeta rpcMeta = method.getAnnotation(RpcMeta.class);
        if (rpcMeta != null && StringUtils.isNoneBlank(rpcMeta.serviceName())) {
            builder.setServiceName(rpcMeta.serviceName());
        } else {
            builder.setServiceName(method.getDeclaringClass().getName());
        }

        if (rpcMeta != null && StringUtils.isNoneBlank(rpcMeta.methodName())) {
            builder.setMethodName(rpcMeta.methodName());
        } else {
            builder.setMethodName(method.getName());
        }

        if (!MessageLite.class.isAssignableFrom(request.getClass())) {
            throw new IllegalArgumentException("request should be protobuf type");
        }

        try {
            Method encodeMethod = request.getClass().getMethod("toByteArray");
            rpcMessage.setBody((byte[]) encodeMethod.invoke(request));
        } catch (Exception e) {
            log.error("request object has no method toByteArray");
        }

        return rpcMessage;
    }

    public void processResponse(RpcClient rpcClient, RpcMessage<RpcHeader.ResponseHeader> rpcMessage) throws Exception {
        RpcFuture future = rpcClient.getFuture(rpcMessage.getHeader().getRequestId());
        if (future == null) {
            return;
        }
        rpcClient.removeFuture(rpcMessage.getHeader().getRequestId());

        if (rpcMessage.getHeader().getResCode() != RpcHeader.RespCode.RESP_SUCCESS) {
            future.fail(new RuntimeException(rpcMessage.getHeader().getResMsg()));
        } else {
            Method decodeMethod = future.getResponseClass().getMethod("parseFrom", byte[].class);
            MessageLite responseBody = (MessageLite) decodeMethod.invoke(
                    null, rpcMessage.getBody());
            future.success(responseBody);
        }
    }

    private void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, boolean isRequest) throws Exception {
        if (in.readableBytes() > lessLen) {
            in.markReaderIndex();
            int headerLength = in.readInt();
            int bodyLength = in.readInt();
            if (in.readableBytes() < headerLength + bodyLength) {
                in.resetReaderIndex();
                return;
            }

            byte[] headerBytes = new byte[headerLength];
            in.readBytes(headerBytes, 0, headerLength);
            byte[] bodyBytes = new byte[bodyLength];
            in.readBytes(bodyBytes, 0, bodyLength);

            if (isRequest) {
                RpcMessage<RpcHeader.RequestHeader> rpcMessage = new RpcMessage<>();
                RpcHeader.RequestHeader requestHeader = RpcHeader.RequestHeader.parseFrom(headerBytes);
                rpcMessage.setHeader(requestHeader);
                rpcMessage.setBody(bodyBytes);
                out.add(rpcMessage);
            } else {
                RpcMessage<RpcHeader.ResponseHeader> rpcMessage = new RpcMessage<>();
                RpcHeader.ResponseHeader responseHeader = RpcHeader.ResponseHeader.parseFrom(headerBytes);
                rpcMessage.setHeader(responseHeader);
                rpcMessage.setBody(bodyBytes);
                out.add(rpcMessage);
            }

        }
    }

    private void encode(ChannelHandlerContext ctx, Object msg, List<Object> out, boolean isRequest) throws Exception {
        RpcMessage rpcMessage;
        if (isRequest) {
            rpcMessage = (RpcMessage<RpcHeader.RequestHeader>) msg;
        } else {
            rpcMessage = (RpcMessage<RpcHeader.ResponseHeader>) msg;
        }

        byte[] headerBytes = rpcMessage.getHeader().toByteArray();

        int headerLength = headerBytes.length;
        int bodyLength = rpcMessage.getBody().length;
        ByteBuf byteBuf = Unpooled.buffer(lessLen);
        byteBuf.writeInt(headerLength);
        byteBuf.writeInt(bodyLength);

        out.add(Unpooled.wrappedBuffer(byteBuf.array(), headerBytes, rpcMessage.getBody()));

    }
}
