package cn.edu.seu.core.replicators;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.core.Peer;
import cn.edu.seu.core.RaftNodeState;
import cn.edu.seu.proto.RaftMessage;
import cn.edu.seu.service.RaftNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Replicator {

    private Peer peer;

    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture appendFuture;
    private Thread appendThread;
    private BlockingDeque<RaftMessage.LogEntry> blockingDeque;

    private RaftNode raftNode;
    private long nextToSend = 1;
    private int tryTimes = 5;


    public Replicator(Peer peer, RaftNode raftNode) {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("boy-replicator-serverID-" + peer.getServerNode().getServerId(), true);
        executor = Executors.newFixedThreadPool(3, namedThreadFactory);
        this.raftNode = raftNode;
        this.peer = peer;
        this.blockingDeque = new LinkedBlockingDeque<>();
        appendThread = namedThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        RaftMessage.LogEntry logEntry = blockingDeque.take();
                        if (!raftNode.appendEntity(peer, logEntry)) {
                            log.error("peer {}第一次发送失败", peer);
                            int currentTry = tryTimes;
                            while (currentTry != 0) {
                                currentTry--;
                                if (raftNode.appendEntity(peer, logEntry)) {
                                    break;
                                }
                                log.error("peer {}第 {} 次发送失败", tryTimes - currentTry, peer);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("replicator of peer {} appendThread happen error {}", peer, e.getMessage());
                }
            }
        });
        appendThread.start();
        scheduledExecutorService = Executors.newScheduledThreadPool(1, namedThreadFactory);
        resetAppend();
    }

    private void resetAppend() {
        if (appendFuture != null && !appendFuture.isDone()) {
            appendFuture.cancel(true);
        }
        appendFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    appendEntitiesStart();
                } catch (Exception e) {
                    log.error("boy-replicator-appendEntitiesStart" + e.getMessage());
                }
            }
        }, raftNode.getRaftOptions().getAppendLogPeriodMilliseconds(), TimeUnit.MILLISECONDS);
    }

    public void preVote(long term, long lastLogIndex, long lastLogTerm) {
        log.info("ball ball preVote from peer {} !", peer);
        executor.submit(() -> {
            RaftMessage.VoteRequest.Builder builder = RaftMessage.VoteRequest.newBuilder();
            builder.setServerId(raftNode.serverId)
                    .setTerm(term)
                    .setLastLogIndex(lastLogIndex)
                    .setLastLogTerm(lastLogTerm);
            RaftMessage.VoteRequest request = builder.build();
            peer.getRaftConsensusServiceAsync().preVote(request, raftNode.new PreVoteResponseCallback(this.peer, request));
        });
    }

    public void reqeustVote(long term, long lastLogIndex, long lastLogTerm) {
        log.info("ball ball request vote from peer {}!", peer);
        executor.submit(() -> {
            RaftMessage.VoteRequest.Builder builder = RaftMessage.VoteRequest.newBuilder();
            builder.setServerId(raftNode.serverId)
                    .setTerm(term)
                    .setLastLogIndex(lastLogIndex)
                    .setLastLogTerm(lastLogTerm);
            RaftMessage.VoteRequest request = builder.build();
            peer.getRaftConsensusServiceAsync().requestVote(request, raftNode.new requestVoteCallback(this.peer, request));
        });
    }

    private void appendEntitiesStart() {
        if (raftNode.getRaftNodeState() == RaftNodeState.STATE_LEADER) {
            if (raftNode.getLogManager() != null && raftNode.getLogManager().getLastLogIndex() >= nextToSend) {
                // 重复发送处理？
                RaftMessage.LogEntry logEntry = raftNode.getLogManager().getLogEntry(nextToSend);
                if (logEntry == null) {
                    log.error("send the logEntry in replicator of peer {} but fail find the index of log {}", peer, nextToSend);
                } else {
                    nextToSend++;
                    log.info("replicator 开始发送LogEntry {} 给 peer {}", logEntry, peer);
                }
                blockingDeque.add(logEntry);
            }
        }
        resetAppend();
    }

    public Peer getPeer() {
        return peer;
    }
}
