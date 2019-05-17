package cn.edu.seu.conf;

import cn.edu.seu.rpc.EndPoint;
import lombok.Data;

/**
 * 标定raft group中的某一个服务节点
 */
@Data
public class ServerNode {

    /**
     * 具体的ip和host信息
     */
    private EndPoint endPoint;

    /**
     * 服务节点的标号，在一个raftGroup中同一个endPoint只能有一个serverId与之对应
     */
    private int serverId;
}
