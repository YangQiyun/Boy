package cn.edu.seu;

import cn.edu.seu.conf.NodeOptions;
import cn.edu.seu.rpc.server.RpcServer;
import cn.edu.seu.service.RaftConsensusServiceImpl;
import cn.edu.seu.service.RaftNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RaftGroupService {

    /**
     * raft group中的唯一标识
     */
    private String groupId;

    /**
     * node 启动选项
     */
    private NodeOptions nodeOptions;

    /**
     * 初始化raft组时，初始化leader的serverNode Id
     */
    private int serverId;

    /**
     * rpc server 模块
     */
    private RpcServer rpcServer;

    /**
     * 当前的raft结点
     */
    private RaftNode node;

    private volatile boolean started = false;

    public RaftGroupService(String groupId, NodeOptions nodeOptions, int serverId, RpcServer rpcServer) {
        this.groupId = groupId;
        this.nodeOptions = nodeOptions;
        this.serverId = serverId;
        this.rpcServer = rpcServer;
    }

    public boolean start() {


        synchronized (this) {
            if (started) {
                return true;
            }
            started = true;
        }

        try {
            if (nodeOptions == null || nodeOptions.getConfiguration() == null || nodeOptions.getConfiguration().isEmpty()) {
                throw new IllegalArgumentException("init raftGroupService params is illegal");
            }

            //todo is need a nodeManager?

            this.node = RaftGroupServiceFactory.createAndInitRaftNode(groupId, serverId, nodeOptions);
            registRpcServer();
            rpcServer.init();
            rpcServer.start();
        } catch (Exception e) {
            log.error("init raftGroupService fail ,err is {}", e.getMessage());
            started = false;
        }


        return true;
    }

    private void registRpcServer() {
        //todo
        rpcServer.registServer(new RaftConsensusServiceImpl(this.node));
    }
}
