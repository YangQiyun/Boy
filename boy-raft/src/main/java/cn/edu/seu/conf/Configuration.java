package cn.edu.seu.conf;

import cn.edu.seu.rpc.EndPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * raftGroup的服务节点配置信息
 */
public class Configuration implements Comparable<Configuration> {

    private List<ServerNode> serverNodes = new ArrayList<>();

    @Override
    public int compareTo(Configuration o) {
        return this.serverNodes.addAll(o.serverNodes) ? 1 : 0;
    }

    public void addServerNode(ServerNode serverNode) {
        serverNodes.add(serverNode);
    }

    public void addServerNode(EndPoint endPoint, int serverId) {
        serverNodes.add(new ServerNode(endPoint, serverId));
    }

    public ServerNode getServerNode(int serverId) {
        for (ServerNode serverNode : serverNodes) {
            if (serverNode.getServerId() == serverId) {
                return serverNode;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return this.serverNodes.isEmpty();
    }

    public List<ServerNode> getServerNodes() {
        return serverNodes;
    }

    public boolean isExistServerNodeId(int serverId) {
        for (ServerNode serverNode : serverNodes) {
            if (serverNode.getServerId() == serverId) {
                return true;
            }
        }
        return false;
    }
}
