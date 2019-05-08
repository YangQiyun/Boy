package cn.edu.seu.rpc;

import cn.edu.seu.rpc.client.RpcCallback;
import cn.edu.seu.rpc.client.RpcClient;
import cn.edu.seu.rpc.protocol.Sample;

import java.util.ArrayList;
import java.util.List;

public class RpcClientTest {

    public static void main(String[] args) {
        List<EndPoint> serverList = new ArrayList<>();
        serverList.add(new EndPoint("127.0.0.1", 8989, 1));
        RpcClient rpcClient = new RpcClient(serverList);

        SampleService sampleService = RpcProxy.getProxy(rpcClient, SampleService.class);
        Sample.sayHi response = sampleService.hello(Sample.sayHello.newBuilder()
                .setId(1)
                .setMessage("hello").build());


        if (response != null) {
            System.out.println(String.format("receive is requestid is %d and the message is %s", response.getId(),
                    response.getMessage()));
        }

        RpcCallback<Sample.sayHi> rpcCallback = new RpcCallback<Sample.sayHi>() {
            @Override
            public void success(Sample.sayHi response) {
                System.out.println(String.format("receive is requestid is %d and the message is %s", response.getId(),
                        response.getMessage()));
            }

            @Override
            public void fail(Throwable e) {
                System.out.println(String.format("error is %s", e.getMessage()));
            }
        };

        SampleServiceAsycImpl serviceAsyc = RpcProxy.getProxy(rpcClient, SampleServiceAsycImpl.class);
        serviceAsyc.hello(Sample.sayHello.newBuilder()
                .setId(2)
                .setMessage("asyc").build(), rpcCallback);

    }
}
