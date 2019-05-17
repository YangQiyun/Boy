package cn.edu.seu.rpc;

import cn.edu.seu.rpc.server.RpcServer;

public class RpcServerTest {

    public static void main(String[] args) {
        RpcServer server = new RpcServer(new EndPoint("127.0.0.1", 8787, 1));
        server.registServer(new SampleServiceImpl());
        server.start();

        // make server keep running
        synchronized (RpcServerTest.class) {
            try {
                RpcServerTest.class.wait();
            } catch (Throwable e) {

            }
        }
    }
}
