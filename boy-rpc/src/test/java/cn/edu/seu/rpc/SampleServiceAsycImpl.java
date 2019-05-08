package cn.edu.seu.rpc;

import cn.edu.seu.rpc.client.RpcCallback;
import cn.edu.seu.rpc.protocol.Sample;

import java.util.concurrent.Future;

public interface SampleServiceAsycImpl extends SampleService {

    Future<Sample.sayHi> hello(Sample.sayHello request, RpcCallback callback);
}
