package cn.edu.seu;

import cn.edu.seu.conf.NodeOptions;
import cn.edu.seu.conf.ServerNode;
import cn.edu.seu.service.RaftNode;

public class RaftGroupServiceFactory {

    /**
     * 创建一个raft结点
     */
    public static RaftNode createRaftNode(String groupId, ServerNode serverNode) {
        return new RaftNode(groupId, serverNode) {
        };
    }

    /**
     * 创建raft结点并且进行初始化
     * @param groupId
     * @param serverId
     * @param opts nodeOpts
     * @return 新创建的raft结点
     */
    public static RaftNode createAndInitRaftNode(String groupId, int serverId, NodeOptions opts) {

        //opts and it's configuration should not empty
        ServerNode serverNode = opts.getConfiguration().getServerNode(serverId);
        if (null == serverNode) {
            throw new IllegalArgumentException(String.format("the serverId {} could not find the configuration information in opts", serverId));
        }

        final RaftNode ret = createRaftNode(groupId, serverNode);
        if (!ret.init(opts)) {
            throw new IllegalStateException("Fail to init node, please see the logs to find the reason.");
        }
        return ret;
    }
}
