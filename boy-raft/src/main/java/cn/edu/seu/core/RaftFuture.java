package cn.edu.seu.core;

/**
 * 客户端请求进行raftgroup复制后的异步结果状态响应
 */
public interface RaftFuture {

    /**
     * 根据不同的raft处理后的状态结果进行处理
     * @param status
     */
    void run(Status status);
}
