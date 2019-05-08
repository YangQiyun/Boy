package cn.edu.seu.rpc;

import cn.edu.seu.rpc.protocol.Sample;

public interface SampleService {

    Sample.sayHi hello(Sample.sayHello request);
}
