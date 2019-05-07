package cn.edu.seu.rpc;

public interface RpcCallback<T> {

    void success(T response);

    void fail(Throwable e);
}
