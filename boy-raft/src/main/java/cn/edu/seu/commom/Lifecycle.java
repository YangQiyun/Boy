package cn.edu.seu.commom;

/**
 * 生命周期管理接口，必须拥有init和shutdown的实现
 * @param <T>
 */
public interface Lifecycle<T> {

    boolean init(T opts);

    void shutdown();
}
