package cn.edu.seu.rpc;

import cn.edu.seu.rpc.client.RpcClient;
import cn.edu.seu.rpc.protocol.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by wenweihu86 on 2017/5/1.
 */
public class BenchmarkTest {

    private static volatile int totalRequestNum = 0;

    public static void main(String[] args) {
        List<EndPoint> serverList = new ArrayList<>();
        serverList.add(new EndPoint("127.0.0.1", 8787, 5));
        RpcClient rpcClient = new RpcClient(serverList);
        int threadNum = 200;
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(new ThreadTask(rpcClient));
            threads[i].start();
        }
        while (true) {
            int lastRequestNum = totalRequestNum;
            try {
                Thread.sleep(1000);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            System.out.println("qps=" + (totalRequestNum - lastRequestNum));
        }
    }

    public static class ThreadTask implements Runnable {

        private RpcClient rpcClient;
        private SampleService sampleService;

        public ThreadTask(RpcClient rpcClient) {
            this.rpcClient = rpcClient;
            this.sampleService = RpcProxy.getProxy(rpcClient, SampleService.class);
        }

        public void run() {
            int currentRequestNum = 0;
            int maxRequestNum = 10000;
            while (true) {
                long beginTime = System.currentTimeMillis();
                // build request
                Sample.sayHello request = Sample.sayHello.newBuilder()
                        .setId(new Random().nextInt(100))
                        .setMessage("hello").build();
                // sync call
                Sample.sayHi response = sampleService.hello(request);
                long endTime = System.currentTimeMillis();
                if (response != null) {
                    currentRequestNum++;
                    totalRequestNum++;
                    if (currentRequestNum == maxRequestNum) {
                        float averageTime = ((float) (endTime - beginTime)) % maxRequestNum;
                        System.out.println("average elpaseMs=" + averageTime);
                        currentRequestNum = 0;
                    }
                } else {
                    System.out.println("server error");
                }
            }
        }

    }
}
