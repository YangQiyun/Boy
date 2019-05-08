package cn.edu.seu.rpc;

import cn.edu.seu.config.Configs;
import cn.edu.seu.protocol.standard.StandardProtocol;
import cn.edu.seu.rpc.client.RpcCallback;
import cn.edu.seu.rpc.client.RpcClient;
import cn.edu.seu.util.IDGeneratorUtil;
import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcProxy implements MethodInterceptor {

    private RpcClient rpcClient;

    public RpcProxy(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public static <T> T getProxy(RpcClient rpcClient, Class<?> superClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(superClass);
        enhancer.setCallback(new RpcProxy(rpcClient));
        return (T) enhancer.create();
    }

    public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        // 通过校验首个参数判断是否是proto类型来进行是否代理的操作
        if (method.getParameterTypes().length < 1 || MessageLite.class.isAssignableFrom(method.getParameterTypes()[0])) {
            log.info("%s no need to proxy", method.getName());
            return methodProxy.invokeSuper(target, args);
        }

        Long requestId = IDGeneratorUtil.INSTANCE.getID();
        StandardProtocol protocol = StandardProtocol.INSTANCE;
        RpcCallback callback;
        Class<?> responseClass;
        Object fullRequest;

        // 校验真正的函数
        int parameterLen = method.getParameterTypes().length;
        if (parameterLen > 1) {
            // interface is (request,callback)
            if (!RpcCallback.class.isAssignableFrom(method.getParameterTypes()[1])) {
                throw new IllegalArgumentException("must be (request,[callback])");
            }

            callback = (RpcCallback) args[1];
            Method syncMethod = method.getDeclaringClass().getMethod(
                    method.getName(), method.getParameterTypes()[0]);
            responseClass = syncMethod.getReturnType();
            fullRequest = protocol.newRequest(requestId, syncMethod, args[0]);

        } else {
            callback = null;
            responseClass = method.getReturnType();
            fullRequest = protocol.newRequest(requestId, method, args[0]);
        }

        int currentTryTimes = 0;
        Object response = null;
        while (currentTryTimes++ < (int) rpcClient.getConfig(Configs.RETRY_TIME)) {
            Future future = rpcClient.sendRequest(requestId, fullRequest, responseClass, callback);
            if (future == null) {
                continue;
            }
            if (callback != null) {
                return future;
            } else {
                response = future.get(
                        rpcClient.getConfig(Configs.READ_TIMEOUT_MILLIS),
                        TimeUnit.MILLISECONDS);
                if (response != null) {
                    break;
                }
            }
        }
        return response;
    }


}
