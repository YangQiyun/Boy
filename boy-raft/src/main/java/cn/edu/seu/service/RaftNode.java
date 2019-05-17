package cn.edu.seu.service;

import cn.edu.seu.commom.Lifecycle;
import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.conf.Configuration;
import cn.edu.seu.conf.NodeOptions;
import cn.edu.seu.conf.RaftOptions;
import cn.edu.seu.conf.ServerNode;
import cn.edu.seu.core.Ballot;
import cn.edu.seu.core.Peer;
import cn.edu.seu.core.RaftFuture;
import cn.edu.seu.core.RaftNodeState;
import cn.edu.seu.core.replicators.ReplicatorGroup;
import cn.edu.seu.proto.RaftMessage;
import cn.edu.seu.rpc.client.RpcCallback;
import cn.edu.seu.storage.LogManager;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class RaftNode implements Lifecycle<NodeOptions> {


    /**
     * 心跳和选举的线程调度池，由于不需要并发支持，所以初始化为两个线程，并且两者之间不会相互干扰
     */
    private ScheduledExecutorService scheduledExecutorService;
    /**
     * raft 对端机器
     */
    private ConcurrentMap<Integer, Peer> peerMap = new ConcurrentHashMap<>();

    private volatile RaftNodeState raftNodeState;
    private LogManager logManager;
    private ReplicatorGroup replicatorGroup;

    private Ballot preVoteBallot;

    private NodeOptions nodeOptions;
    private RaftOptions raftOptions;
    private Configuration configuration;
    public int serverId;

    /**
     * 客户端请求的任务处理
     */
    private Disruptor<LogEntryAndFutrure> applyDisruptor;
    private RingBuffer<LogEntryAndFutrure> applyQueue;

    private long currentTerm;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();


    public RaftNode(String groupId, ServerNode serverNode) {

    }

    @Override
    public boolean init(NodeOptions opts) {
        this.nodeOptions = opts;
        this.raftOptions = this.nodeOptions.getRaftOptions();
        this.configuration = this.nodeOptions.getConfiguration();
        this.serverId = this.nodeOptions.getServerId();
        this.raftNodeState = RaftNodeState.STATE_FOLLOWER;
        this.replicatorGroup = new ReplicatorGroup(this);

        for (ServerNode serverNode : this.nodeOptions.getConfiguration().getServerNodes()) {
            Peer peer = new Peer(serverNode);
            peerMap.put(serverNode.getServerId(), peer);
            // 初始化所有节点连接
            replicatorGroup.addReplicator(peer);

        }
        scheduledExecutorService = Executors.newScheduledThreadPool(2,
                new NamedThreadFactory("boy-raft-scheduled", true));


        //todo if node manager exists,this time should check the node;

        applyDisruptor = new Disruptor<LogEntryAndFutrure>(LogEntryAndFutrure::new,
                this.raftOptions.getDisruptorBufferSize(),
                new NamedThreadFactory("boy-RaftNode-Disruptor-", true));

        preVoteBallot = new Ballot(peerMap);
        // 开始默认的选举操作

        return false;
    }

    @Override
    public void shutdown() {
        if (!scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdownNow();
        }
    }

    /**
     * logEntry日志分发对象
     */
    private static class LogEntryAndFutrure {
        RaftMessage.LogEntry entry;
        RaftFuture done;
        long expectedTerm;

        public void reset() {
            this.entry = null;
            this.done = null;
            this.expectedTerm = 0;
        }
    }

    private class LogEntryAndFutureHandler implements EventHandler<LogEntryAndFutrure> {

        @Override
        public void onEvent(LogEntryAndFutrure logEntryAndFutrure, long l, boolean b) throws Exception {
            //todo batch here
            executeLogEntryAndFuture(logEntryAndFutrure);
        }
    }

    private void executeLogEntryAndFuture(LogEntryAndFutrure logEntryAndFutrure) {

    }

    private void stepDown(long newTerm) {

    }


    /**
     * 2pc-防止断网过久对正常的raft组
     */
    public boolean startPreVote() {
        try {
            writeLock.lock();
            if (!configuration.isExistServerNodeId(serverId)) {
                log.warn("current serverId {} does not exist in the configuration.", serverId);
                return false;
            }

            if (raftNodeState != RaftNodeState.STATE_FOLLOWER) {
                log.warn("current raftNodeState is {} can not startPreVote!", raftNodeState);
                return false;
            }

            // todo if todo release the leaderNode in stateMachine

            preVoteBallot.reset(peerMap);
            // preVote
            long lastLogIndex = logManager.getLastLogIndex();
            replicatorGroup.sendPreVote(currentTerm, lastLogIndex, logManager.getTerm(lastLogIndex));

            // 虽然最终自己会处理，但是预先投票，投票器会过滤重复票
            preVoteBallot.grant(peerMap.get(serverId));
            if (preVoteBallot.isGrant()) {
                startVote();
            }
        } finally {
            writeLock.unlock();
        }

        return true;
    }

    public boolean startVote() {
        return true;
    }


    @Slf4j
    public class PreVoteResponseCallback implements RpcCallback<RaftMessage.VoteResponse> {

        private Peer peer;
        private RaftMessage.VoteRequest request;

        public PreVoteResponseCallback(Peer peer, RaftMessage.VoteRequest request) {
            this.peer = peer;
            this.request = request;
        }

        @Override
        public void success(RaftMessage.VoteResponse response) {
            try {
                writeLock.lock();
                if (raftNodeState != RaftNodeState.STATE_FOLLOWER) {
                    log.error("the peer {} the state is {} ,so the preVote is invalid", peerMap.get(serverId), raftNodeState);
                    return;
                }
                if (request.getTerm() != currentTerm) {
                    log.error("current term has been changed before is {} now is {}", request.getTerm(), currentTerm);
                    return;
                }
                if (response.getTerm() > currentTerm) {
                    log.info("receive term is {} now is {},have to stepDown", response.getTerm(), currentTerm);
                    stepDown(response.getTerm());
                    return;
                }
                if (response.getGranted()) {
                    preVoteBallot.grant(peer);
                    if (preVoteBallot.isGrant()) {
                        startVote();
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public void fail(Throwable e) {
            log.warn("the peer {} send preVote to target peer {} is fail", peerMap.get(serverId), this.peer);
        }
    }

    /**
     * 预投票请求处理
     * 除了任期要大于等于本节点
     * 最后的日志和日志所在的任期也要大于本节点，保证不回退有效日志
     */
    public RaftMessage.VoteResponse handlePreVoteRequest(RaftMessage.VoteRequest request) {

        RaftMessage.VoteResponse.Builder responseBuilder = RaftMessage.VoteResponse.newBuilder();
        try {
            writeLock.lock();
            responseBuilder.setGranted(false)
                    .setTerm(this.currentTerm);
            // preVote 对于大的term不进行处理
            if (!this.raftNodeState.isActive()) {
                log.error("peer {} receives preVote request but current state is't active", peerMap.get(serverId));
                return responseBuilder.build();
            }
            if (request.getTerm() < currentTerm) {
                log.error("peer {} receives remote peer {} preVote request but request term {} is smaller than currentTerm ",
                        peerMap.get(serverId), peerMap.get(request.getServerId()), request.getTerm(), currentTerm);
                return responseBuilder.build();
            }
            long lastLogIndex = logManager.getLastLogIndex();
            long lastLogTerm = logManager.getTerm(lastLogIndex);
            if (request.getLastLogIndex() >= lastLogIndex && request.getLastLogTerm() >= lastLogTerm) {
                responseBuilder.setGranted(true);
                return responseBuilder.build();
            }

            return responseBuilder.build();
        } finally {
            log.info("preVote request from peer {} " +
                            "in term {} (my term is {}), granted={}",
                    peerMap.get(request.getServerId()), request.getTerm(),
                    responseBuilder.getTerm(), responseBuilder.getGranted());
            writeLock.unlock();
        }
    }
}
