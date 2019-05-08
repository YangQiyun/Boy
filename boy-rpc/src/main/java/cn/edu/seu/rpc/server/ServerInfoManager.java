package cn.edu.seu.rpc.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerInfoManager {

    private Map<String, ServerInfo> storage = new ConcurrentHashMap<>();

    public static final ServerInfoManager INSTANCE = new ServerInfoManager();

    public void addServerInfo(String serverName, String methodName, ServerInfo serverInfo) {
        storage.put(getUniqueKey(serverName, methodName), serverInfo);
    }

    public ServerInfo getServerInfo(String serverName, String methodName) {
        return storage.get(getUniqueKey(serverName, methodName));
    }

    public String getUniqueKey(String serverName, String methodName) {
        return new StringBuilder().append(serverName)
                .append("-")
                .append(methodName).toString();
    }
}
