package cn.edu.seu.core.replicators;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.core.Peer;
import cn.edu.seu.proto.RaftMessage;
import cn.edu.seu.service.RaftNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Replicator {

    private Peer peer;

    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;

    private RaftNode raftNode;


    public Replicator(Peer peer,RaftNode raftNode) {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("boy-replicator-serverID-" + peer.getServerNode().getServerId(), true);
        executor = Executors.newFixedThreadPool(3,namedThreadFactory);
        this.raftNode = raftNode;
        this.peer = peer;
        scheduledExecutorService = Executors.newScheduledThreadPool(1, namedThreadFactory);
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                appendEntitiesStart();
            }
        },raftNode.getRaftOptions().getAppendLogPeriodMilliseconds(), TimeUnit.MILLISECONDS);
    }

    public void preVote(long term, long lastLogIndex, long lastLogTerm) {
        log.info("ball ball preVote from peer {} !", peer);
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

    public void reqeustVote(long term, long lastLogIndex, long lastLogTerm) {
        log.info("ball ball request vote from peer {}!", peer);
        executor.submit(() -> {
            RaftMessage.VoteRequest.Builder builder = RaftMessage.VoteRequest.newBuilder();
            builder.setServerId(peer.getServerNode().getServerId())
                    .setTerm(term)
                    .setLastLogIndex(lastLogIndex)
                    .setLastLogTerm(lastLogTerm);
            RaftMessage.VoteRequest request = builder.build();
            peer.getRaftConsensusServiceAsync().requestVote(request, raftNode.new requestVoteCallback(this.peer,request));
        });
    }

    private void appendEntitiesStart(){
        long nextToSend = peer.getNextIndex();
        if(raftNode.getLogManager().getLastLogIndex() >= nextToSend){
            // 重复发送处理？
            RaftMessage.LogEntry logEntry = raftNode.getLogManager().getLogEntry(nextToSend);
            if(logEntry==null){
                log.error("send the logEntry in replicator of peer {} but fail find the index of log {}",peer,nextToSend);
            }
            raftNode.appendEntity(peer,logEntry);
        }
    }

    public Peer getPeer() {
        return peer;
    }
}
