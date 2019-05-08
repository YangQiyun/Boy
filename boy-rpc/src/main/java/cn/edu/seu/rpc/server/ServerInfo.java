package cn.edu.seu.rpc.server;

import com.google.protobuf.MessageLite;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
@Data
public class ServerInfo {

    private String serverName;

    private String methodName;

    private Method method;

    private Method parseMethod;

    private int parameterCount;

    private Class<?>[] parameterTypes;

    private Object service;

    public ServerInfo(Object service, String serverName, String methodName, Method method, int parameterCount, Class<?>[] parameterTypes) {
        this.service = service;
        this.serverName = serverName;
        this.methodName = methodName;
        this.method = method;
        this.parameterCount = parameterCount;
        this.parameterTypes = parameterTypes;
    }

    public Method getParseMethod() {
        if (null == parseMethod) {
            if (MessageLite.class.isAssignableFrom(parameterTypes[0])) {
                try {
                    Method parseFrom = parameterTypes[0].getMethod("parseFrom", byte[].class);
                    this.parseMethod = parseFrom;
                } catch (NoSuchMethodException e) {
                    log.error(e.getMessage());
                }
            } else {
                return null;
            }
        }

        return parseMethod;
    }
}
