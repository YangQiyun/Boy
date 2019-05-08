package cn.edu.seu.config;

import exception.EmptyException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RpcConfigManager implements ConfigManager {

    private Map<String, Object> defaultConfigMap = new HashMap<String, Object>();

    public static RpcConfigManager INSTANCE = new RpcConfigManager();

    public RpcConfigManager() {
        defaultConfigMap.putIfAbsent(Configs.NETTY_IO_RATIO, Configs.NETTY_IO_RATIO_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.NETTY_BUFFER_POOLED, Configs.NETTY_BUFFER_POOLED_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.NETTY_BUFFER_HIGH_WATERMARK, Configs.NETTY_BUFFER_HIGH_WATERMARK_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.NETTY_BUFFER_LOW_WATERMARK, Configs.NETTY_BUFFER_LOW_WATERMARK_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.NETTY_EPOLL_LT, Configs.NETTY_EPOLL_LT_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.NETTY_EPOLL_SWITCH, Configs.NETTY_EPOLL_SWITCH_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.NETTY_CONNECT_TIMEOUT, Configs.NETTY_CONNECT_TIMEOUT_DEFAULT);

        defaultConfigMap.putIfAbsent(Configs.TCP_NODELAY, Configs.TCP_NODELAY_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.TCP_SO_BACKLOG, Configs.TCP_SO_BACKLOG_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.TCP_SO_KEEPALIVE, Configs.TCP_SO_KEEPALIVE_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.TCP_SO_REUSEADDR, Configs.TCP_SO_REUSEADDR_DEFAULT);

        defaultConfigMap.putIfAbsent(Configs.CONN_CREATE_TP_KEEPALIVE_TIME, Configs.CONN_CREATE_TP_KEEPALIVE_TIME_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.CONN_CREATE_TP_MAX_SIZE, Configs.CONN_CREATE_TP_MAX_SIZE_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.CONN_CREATE_TP_MIN_SIZE, Configs.CONN_CREATE_TP_MIN_SIZE_DEFAULT);
        defaultConfigMap.putIfAbsent(Configs.CONN_CREATE_TP_QUEUE_SIZE, Configs.CONN_CREATE_TP_QUEUE_SIZE_DEFAULT);

        defaultConfigMap.putIfAbsent(Configs.RPC_SERVER_WORK_POOL, Configs.RPC_SERVER_WORK_POOL_DEFAULT);
    }

    public <T> T getDefaultValue(String configType) throws EmptyException {
        if (null == defaultConfigMap.get(configType)) {
            throw new EmptyException("the config type of " + configType + " is not init");
        }
        return (T) defaultConfigMap.get(configType);
    }

    public Set<String> listAllType() {
        return defaultConfigMap.keySet();
    }

    public <T> void addConfigType(String type, T defaultValue) {
        defaultConfigMap.put(type, defaultValue);
    }


}
