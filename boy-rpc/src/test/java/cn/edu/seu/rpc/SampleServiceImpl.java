package cn.edu.seu.rpc;

import cn.edu.seu.rpc.protocol.Sample;

public class SampleServiceImpl implements SampleService {

    @Override
    public Sample.sayHi hello(Sample.sayHello request) {
        System.out.println(String.format("receive request id is %d and the message is %s", request.getId(), request.getMessage()));
        return Sample.sayHi.newBuilder()
                .setId(request.getId())
                .setMessage("haode").build();
    }

}
