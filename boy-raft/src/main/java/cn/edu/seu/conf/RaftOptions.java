package cn.edu.seu.conf;

import lombok.Data;

/**
 * raft 参数
 */
@Data
public class RaftOptions {

    /**
     * 选举延迟时间，当follower在该时间内未收到leader的appendEntities的时候，
     * 默认为leader结点已经宕机，需要进行重新的选举
     */
    private int electionTimeoutMs = 1000;

    /**
     * 内部disruptor的任务数量池
     */
    private int disruptorBufferSize = 16384;
}
