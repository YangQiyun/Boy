package cn.edu.seu.core.replicators;

import cn.edu.seu.core.Peer;
import cn.edu.seu.service.RaftNode;

import java.util.concurrent.ConcurrentHashMap;

public class ReplicatorGroup {

    private ConcurrentHashMap<Integer, Replicator> replicators = new ConcurrentHashMap<>();
    private RaftNode raftNode;

    public ReplicatorGroup(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    public boolean addReplicator(Peer peer) {
        return replicators.put(peer.getServerNode().getServerId(), new Replicator(peer, raftNode)) == null;
    }

    public void sendPreVote(long term, long lastLogIndex, long lastLogTerm) {
        replicators.forEachValue(1L, replicator -> {
            if(replicator.getPeer().getServerNode().getServerId() != raftNode.serverId){
                replicator.preVote(term, lastLogIndex, lastLogTerm);

            }
        });
    }

    public void sendRequestVote(long term, long lastLogIndex, long lastLogTerm) {
        replicators.forEachValue(1L, replicator -> {
            if(replicator.getPeer().getServerNode().getServerId() != raftNode.serverId){
                replicator.reqeustVote(term, lastLogIndex, lastLogTerm);

            }
        });
    }

}
