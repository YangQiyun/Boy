package cn.edu.seu;

import cn.edu.seu.conf.NodeOptions;
import cn.edu.seu.rpc.EndPoint;
import cn.edu.seu.rpc.server.RpcServer;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestElect {

    public static void main(String[] args){
        NodeOptions nodeOptions = new NodeOptions();
        EndPoint endPoint1 = new EndPoint("127.0.0.1",8901,2);
        EndPoint endPoint2 = new EndPoint("127.0.0.1",8902,2);
        EndPoint endPoint3 = new EndPoint("127.0.0.1",8903,2);
        nodeOptions.getConfiguration().addServerNode(endPoint1,1);
        nodeOptions.getConfiguration().addServerNode(endPoint2,2);
        nodeOptions.getConfiguration().addServerNode(endPoint3,3);
        nodeOptions.setServerId(1);

        RpcServer rpcServer = new RpcServer(endPoint1);

        RaftGroupService raftGroupService = new RaftGroupService("raft", nodeOptions, 1, rpcServer);
        raftGroupService.start();

        synchronized (TestElect.class) {
            try {
                TestElect.class.wait();
            } catch (Throwable e) {

            }
        }
    }
}
