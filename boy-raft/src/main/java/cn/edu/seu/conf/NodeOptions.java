package cn.edu.seu.conf;

import lombok.Data;

/**
 * 节点配置信息
 */
@Data
public class NodeOptions {

    /**
     * raftGroup配置信息，需要进行初始化设置
     */
    private Configuration configuration = new Configuration();

    /**
     * raft Node的参数配置信息
     */
    private RaftOptions raftOptions = new RaftOptions();

    /**
     * 当前节点的代号
     */
    private int serverId;
}
