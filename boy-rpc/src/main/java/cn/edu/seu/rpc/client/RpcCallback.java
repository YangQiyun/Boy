package cn.edu.seu.rpc.client;

public interface RpcCallback<T> {

    void success(T response);

    void fail(Throwable e);
}
