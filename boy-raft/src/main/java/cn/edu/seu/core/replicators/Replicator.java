package cn.edu.seu.core.replicators;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.core.Peer;
import cn.edu.seu.proto.RaftMessage;
import cn.edu.seu.service.RaftNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Replicator {

    private Peer peer;

    private ExecutorService executor;

    private RaftNode raftNode;


    public Replicator(Peer peer,RaftNode raftNode) {
        executor = Executors.newFixedThreadPool(3, new NamedThreadFactory("boy-replicator-serverID-" + peer.getServerNode().getServerId(), true));
        this.raftNode = raftNode;
        this.peer = peer;
    }

    public void preVote(long term, long lastLogIndex, long lastLogTerm) {
        log.info("this peer {} begins to preVote!", peer);
        executor.submit(() -> {
            RaftMessage.VoteRequest.Builder builder = RaftMessage.VoteRequest.newBuilder();
            builder.setServerId(peer.getServerNode().getServerId())
                    .setTerm(term)
                    .setLastLogIndex(lastLogIndex)
                    .setLastLogTerm(lastLogTerm);
            RaftMessage.VoteRequest request = builder.build();
            peer.getRaftConsensusServiceAsync().preVote(request, raftNode.new PreVoteResponseCallback(this.peer,request));
        });
    }


}
