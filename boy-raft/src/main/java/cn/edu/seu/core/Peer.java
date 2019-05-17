package cn.edu.seu.core;

import cn.edu.seu.conf.ServerNode;
import cn.edu.seu.rpc.RpcProxy;
import cn.edu.seu.rpc.client.RpcClient;
import cn.edu.seu.service.RaftConsensusService;
import cn.edu.seu.service.RaftConsensusServiceAsync;
import lombok.Data;

@Data
public class Peer {

    private ServerNode serverNode;
    private RpcClient rpcClient;
    private RaftConsensusService raftConsensusService;
    private RaftConsensusServiceAsync raftConsensusServiceAsync;

    public Peer(ServerNode serverNode) {
        this.serverNode = serverNode;
        this.init();
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    private void init() {
        rpcClient = new RpcClient(serverNode.getEndPoint());
        raftConsensusService = RpcProxy.getProxy(rpcClient, RaftConsensusService.class);
        raftConsensusServiceAsync = RpcProxy.getProxy(rpcClient, RaftConsensusServiceAsync.class);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "ip=" + serverNode.getEndPoint().getIp() +
                "port=" + serverNode.getEndPoint().getPort() +
                '}';
    }
}
