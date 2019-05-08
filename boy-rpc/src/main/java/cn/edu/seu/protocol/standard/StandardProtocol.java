package cn.edu.seu.protocol.standard;

import cn.edu.seu.rpc.client.RpcClient;
import cn.edu.seu.rpc.client.RpcFuture;
import cn.edu.seu.rpc.RpcMeta;
import cn.edu.seu.rpc.server.RpcServer;
import cn.edu.seu.rpc.server.ServerInfo;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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

    public RpcMessage<RpcHeader.ResponseHeader> processRequest(RpcServer rpcServer, RpcMessage<RpcHeader.RequestHeader> request) throws Exception {
        long startTime = System.currentTimeMillis();

        String serviceName = request.getHeader().getServiceName();
        String methodName = request.getHeader().getMethodName();

        RpcMessage<RpcHeader.ResponseHeader> response = new RpcMessage<>();
        RpcHeader.ResponseHeader.Builder responseBuilder = RpcHeader.ResponseHeader.newBuilder()
                .setRequestId(request.getHeader().getRequestId());

        ServerInfo serverInfo = rpcServer.getServerInfoManager().getServerInfo(serviceName, methodName);
        if (null == serverInfo) {
            log.error(String.format("no such serverName %s or methodName %s", serviceName, methodName));
            RpcHeader.ResponseHeader responseHeader = responseBuilder
                    .setResCode(RpcHeader.RespCode.RESP_FAIL)
                    .setResMsg("no such service")
                    .build();
            response.setHeader(responseHeader);
            return response;
        } else {
            MessageLite param = (MessageLite) serverInfo.getParseMethod().invoke(null, request.getBody());
            if (serverInfo.getParameterCount() != 1 ||
                    serverInfo.getParameterTypes().length != 1 ||
                    !serverInfo.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
                log.error(String.format("no match the methodName %s parameterCount %s parameterType[0] %s",
                        serverInfo.getMethodName(),
                        serverInfo.getParameterCount(),
                        serverInfo.getParameterTypes()[0].getName()));

                RpcHeader.ResponseHeader responseHeader = responseBuilder.setResCode(RpcHeader.RespCode.RESP_FAIL)
                        .setResMsg("no such service")
                        .build();
                response.setHeader(responseHeader);
                return response;
            }

            MessageLite result = (MessageLite) serverInfo.getMethod().invoke(null, param);
            RpcHeader.ResponseHeader responseHeader = responseBuilder.setResCode(RpcHeader.RespCode.RESP_SUCCESS)
                    .setResMsg("")
                    .build();
            response.setHeader(responseHeader);
            response.setBody(result.toByteArray());

            long endTime = System.currentTimeMillis();
            log.debug("elapseMS={} service={} method={} callId={}",
                    endTime - startTime, request.getHeader().getServiceName(),
                    request.getHeader().getMethodName(), request.getHeader().getRequestId());
            return response;
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
