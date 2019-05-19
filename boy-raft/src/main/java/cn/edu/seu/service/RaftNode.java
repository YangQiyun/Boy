package cn.edu.seu.service;

import cn.edu.seu.commom.Lifecycle;
import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.conf.Configuration;
import cn.edu.seu.conf.NodeOptions;
import cn.edu.seu.conf.RaftOptions;
import cn.edu.seu.conf.ServerNode;
import cn.edu.seu.core.Ballot;
import cn.edu.seu.core.BallotBox;
import cn.edu.seu.core.Peer;
import cn.edu.seu.core.RaftFuture;
import cn.edu.seu.core.RaftNodeState;
import cn.edu.seu.core.StateMachine;
import cn.edu.seu.core.Status;
import cn.edu.seu.core.replicators.ReplicatorGroup;
import cn.edu.seu.proto.RaftMessage;
import cn.edu.seu.rpc.client.RpcCallback;
import cn.edu.seu.service.entity.Task;
import cn.edu.seu.storage.LogExceptionHandler;
import cn.edu.seu.storage.LogManager;
import cn.edu.seu.storage.LogManagerImpl;
import cn.edu.seu.storage.RaftFutureQueue;
import com.google.protobuf.ByteString;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class RaftNode implements Lifecycle<NodeOptions> {

    /**
     * 心跳和选举的线程调度池，由于不需要并发支持，所以初始化为两个线程，并且两者之间不会相互干扰
     */
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture electionScheduledFuture;
    private ScheduledFuture heartbeatScheduledFuture;
    private ConcurrentMap<Integer, Peer> peerMap = new ConcurrentHashMap<>();

    private volatile RaftNodeState raftNodeState;
    private LogManager logManager;
    private ReplicatorGroup replicatorGroup;
    private RaftFutureQueue futureQueue;
    private StateMachine stateMachine;

    private BallotBox ballotBox;
    private Ballot preVoteBallot;
    private Ballot formalVoteBallot;

    private NodeOptions nodeOptions;
    private RaftOptions raftOptions;
    private Configuration configuration;


    /**
     * 客户端请求的任务处理
     */
    private Disruptor<LogEntryAndFutrure> applyDisruptor;
    private RingBuffer<LogEntryAndFutrure> applyQueue;

    private String groupId;
    public int serverId;
    private long currentTerm;
    private int leaderId;
    private int voteForId;
    // 已知的最大的已经被提交的日志条目的索引值
    private long commitIndex;
    // 已知当前节点提交到stateMachine的索引值
    private long appliedIndex;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();

    public RaftNode(String groupId, ServerNode serverNode) {
        this.groupId = groupId;
        this.serverId = serverNode.getServerId();
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
        this.logManager = new LogManagerImpl(peerMap.get(serverId));
        scheduledExecutorService = Executors.newScheduledThreadPool(2,
                new NamedThreadFactory("boy-raft-scheduled", true));


        //todo if node manager exists,this time should check the node;

        applyDisruptor = new Disruptor<>(LogEntryAndFutrure::new,
                this.raftOptions.getDisruptorBufferSize(),
                new NamedThreadFactory("boy-RaftNode-Disruptor-", true));
        applyDisruptor.handleEventsWith(new LogEntryAndFutureHandler());
        applyDisruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(this.getClass().getSimpleName()));
        applyDisruptor.start();
        applyQueue = this.applyDisruptor.getRingBuffer();

        futureQueue = new RaftFutureQueue();
        ballotBox = new BallotBox(futureQueue, this);

        preVoteBallot = new Ballot(peerMap);
        formalVoteBallot = new Ballot(peerMap);
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                resetElectionTimer();
            }
        }, 15, TimeUnit.SECONDS);
        // 开始默认的选举操作
        //resetElectionTimer();
        return true;
    }

    @Override
    public void shutdown() {
        if (!scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdownNow();
        }
    }

    // 客户端入口
    public void applyTask(final Task task) {

        RaftMessage.LogEntry.Builder logEntryBuilder = RaftMessage.LogEntry.newBuilder().setData(ByteString.copyFrom(task.getData()));
        logEntryBuilder.setTerm(this.currentTerm);
        logEntryBuilder.setType(RaftMessage.EntryType.ENTRY_TYPE_DATA);
        applyQueue.publishEvent((event, sequence) -> {
            event.done = task.getDone();
            event.entry = logEntryBuilder.build();
        });

    }

    /**
     * logEntry日志分发对象
     */
    private static class LogEntryAndFutrure {
        RaftMessage.LogEntry entry;
        RaftFuture done;

        public void reset() {
            this.entry = null;
            this.done = null;
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
        try {
            this.writeLock.lock();
            if (raftNodeState != RaftNodeState.STATE_LEADER) {
                log.debug("current node is not the leader, peer is {} and the leaderId is {}", peerMap.get(serverId), leaderId);
                return;
            }

            // 将本次日志添加到投票箱中，每一次都要发起投票
            this.ballotBox.appendTask(logEntryAndFutrure.done, peerMap);
            // 分发到其他节点append,由replicator自己处理
            // 本地append
            this.logManager.appendEntries(logEntryAndFutrure.entry, new LeaderAppendLogClosure(logEntryAndFutrure.entry));
        } finally {
            this.writeLock.unlock();
        }
    }

    class LeaderAppendLogClosure extends LogManager.LogClosure {

        public LeaderAppendLogClosure(RaftMessage.LogEntry entry) {
            super(entry);
        }

        @Override
        public void run(final Status status) {
            if (status != null && status.isOk()) {
                RaftNode.this.ballotBox.commitAt(this.firstLogIndex, this.entry.getIndex(), peerMap.get(serverId));
            } else {
                log.error("leader append normal log err the status is {}", status);
            }

        }
    }

    // in lock
    class FollowerAppendLogClosure extends LogManager.LogClosure {

        public volatile boolean done = false;
        private RaftMessage.AppendEntriesResponse.Builder response;

        public FollowerAppendLogClosure(RaftMessage.LogEntry entry, RaftMessage.AppendEntriesResponse.Builder response) {
            super(entry);
            this.response = response;
        }

        @Override
        public void run(Status status) {
            try {
                if (status != null && status.isOk()) {
                    try {
                        readLock.lock();
                        if (currentTerm != response.getTerm()) {
                            response.setResCode(RaftMessage.ResCode.RES_CODE_FAIL).setTerm(currentTerm);
                            return;
                        }
                    } finally {
                        readLock.unlock();
                    }
                    response.setResCode(RaftMessage.ResCode.RES_CODE_SUCCESS).setTerm(currentTerm);
                    ballotBox.followCommit(entry.getIndex());
                } else {
                    log.error("leader append normal log err the status is {}", status);
                }
            } finally {
                done = true;
            }
        }
    }


    private void stepDown(long newTerm) {
        if (currentTerm > newTerm) {
            log.error("can't be happened");
            return;
        }
        if (currentTerm < newTerm) {
            ((LogManagerImpl) logManager).setTerm(currentTerm);
            currentTerm = newTerm;
            leaderId = 0;
            voteForId = 0;
            //raftLog.updateMetaData(currentTerm, votedFor, null);
        }
        raftNodeState = RaftNodeState.STATE_FOLLOWER;
        // stop heartbeat
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
        resetElectionTimer();
    }

    /**
     * 超时选举器
     */
    private void resetElectionTimer() {
        if (electionScheduledFuture != null && !electionScheduledFuture.isDone()) {
            electionScheduledFuture.cancel(true);
        }
        electionScheduledFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    startPreVote();
                } catch (Exception e) {
                    log.error("startVote error is {} ", e.getMessage());
                }

            }
        }, raftOptions.getElectionTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * 心跳器
     */
    // in lock, 开始心跳，对leader有效
    private void startNewHeartbeat() {
        log.debug("start new heartbeat");
        for (final Peer peer : peerMap.values()) {
            if(peer.getServerNode().getServerId() == serverId){
                continue;
            }
            scheduledExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    appendEntity(peer,null);
                }
            });
        }
        resetHeartbeatTimer();
    }

    private void resetHeartbeatTimer() {
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
        heartbeatScheduledFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                startNewHeartbeat();
            }
        }, raftOptions.getHeartbeatPeriodMilliseconds(), TimeUnit.MILLISECONDS);
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
        resetElectionTimer();
        return true;
    }


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

    public boolean startVote() {
        try {
            writeLock.lock();
            if (!configuration.isExistServerNodeId(serverId)) {
                log.warn("current serverId {} does not exist in the configuration.", serverId);
                return false;
            }

            if (raftNodeState != RaftNodeState.STATE_FOLLOWER) {
                log.warn("current raftNodeState is {} can not startVote!", raftNodeState);
                return false;
            }
            this.leaderId = 0;
            this.raftNodeState = RaftNodeState.STATE_CANDIDATE;
            this.currentTerm++;
            log.info("Running for election in term {}", currentTerm);
            this.voteForId = serverId;

            formalVoteBallot.reset(peerMap);
            // requestVote
            long lastLogIndex = logManager.getLastLogIndex();
            replicatorGroup.sendRequestVote(currentTerm, lastLogIndex, logManager.getTerm(lastLogIndex));

            // 虽然最终自己会处理，但是预先投票，投票器会过滤重复票
            formalVoteBallot.grant(peerMap.get(serverId));
            if (formalVoteBallot.isGrant()) {
                becomeLeader();
            }
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    public class requestVoteCallback implements RpcCallback<RaftMessage.VoteResponse> {

        private Peer peer;
        private RaftMessage.VoteRequest request;

        public requestVoteCallback(Peer peer, RaftMessage.VoteRequest request) {
            this.peer = peer;
            this.request = request;
        }

        @Override
        public void success(RaftMessage.VoteResponse response) {
            try {
                writeLock.lock();
                if (raftNodeState != RaftNodeState.STATE_CANDIDATE) {
                    log.error("the peer {} the state is {} ,so the requestVote is invalid", peerMap.get(serverId), raftNodeState);
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
                    formalVoteBallot.grant(peer);
                    if (formalVoteBallot.isGrant()) {
                        becomeLeader();
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public void fail(Throwable e) {
            log.warn("the peer {} send requestVote to target peer {} is fail", peerMap.get(serverId), this.peer);
        }
    }

    public RaftMessage.VoteResponse handleRequestVoteRequest(RaftMessage.VoteRequest request) {

        RaftMessage.VoteResponse.Builder responseBuilder = RaftMessage.VoteResponse.newBuilder();
        try {
            writeLock.lock();
            responseBuilder.setGranted(false);
            if (!this.raftNodeState.isActive()) {
                log.error("peer {} receives preVote request but current state is't active", peerMap.get(serverId));
                return responseBuilder.build();
            }
            if (request.getTerm() < currentTerm) {
                log.error("peer {} receives remote peer {} preVote request but request term {} is smaller than currentTerm ",
                        peerMap.get(serverId), peerMap.get(request.getServerId()), request.getTerm(), currentTerm);
                return responseBuilder.build();
            }
            if (request.getTerm() > currentTerm) {
                log.info("receive term is {} now is {},have to stepDown", request.getTerm(), currentTerm);
                stepDown(request.getTerm());
                // 更新到follow状态
            }
            long lastLogIndex = logManager.getLastLogIndex();
            long lastLogTerm = logManager.getTerm(lastLogIndex);
            if (request.getLastLogTerm() >= lastLogTerm && request.getLastLogIndex() >= lastLogIndex) {
                if (0 == voteForId) {
                    // 当投票过后自然成为follow防止自己成为主动去选举
                    stepDown(request.getTerm());
                    voteForId = request.getServerId();
                    responseBuilder.setGranted(true);
                    responseBuilder.setTerm(currentTerm);
                    return responseBuilder.build();
                }
            }

            responseBuilder.setTerm(currentTerm);
            return responseBuilder.build();
        } finally {
            log.info("requestVote request from peer {} " +
                            "in term {} (my term is {}), granted={}",
                    peerMap.get(request.getServerId()), request.getTerm(),
                    responseBuilder.getTerm(), responseBuilder.getGranted());
            writeLock.unlock();
        }
    }

    // in lock
    private void becomeLeader() {
        raftNodeState = RaftNodeState.STATE_LEADER;
        leaderId = serverId;

        // stop vote timer
        if (electionScheduledFuture != null && !electionScheduledFuture.isDone()) {
            electionScheduledFuture.cancel(true);
        }
        // reset the log pending index
        this.ballotBox.resetPendingIndex(this.logManager.getLastLogIndex() + 1);
        // start heartbeat timer
        startNewHeartbeat();
        log.info("this peer {} become the leader!", peerMap.get(serverId));
    }

    /**
     * 根新commit信息
     */
    public void updateCommit(long commitIndex) {

        if (raftNodeState != RaftNodeState.STATE_LEADER) {
            log.error("current raftNode state is not leader but still upateCommit");
            return;
        }
        //  todo commit state

        this.commitIndex = commitIndex;
    }

    public RaftMessage.AppendEntriesResponse handleAppendEntries(RaftMessage.AppendEntriesRequest request) {
        try {
            writeLock.lock();
            long nowLastIndexOfLog = logManager.getLastLogIndex();
            long nowLastTermOfLog = logManager.getTerm(nowLastIndexOfLog);

            RaftMessage.AppendEntriesResponse.Builder responseBuilder
                    = RaftMessage.AppendEntriesResponse.newBuilder();
            responseBuilder.setTerm(currentTerm);
            responseBuilder.setResCode(RaftMessage.ResCode.RES_CODE_FAIL);
            responseBuilder.setLastLogIndex(nowLastIndexOfLog);
            if (request.getTerm() < currentTerm) {
                return responseBuilder.build();
            }
            stepDown(request.getTerm());

            // 当前节点已经进入startVote阶段
            if (0 == leaderId) {
                leaderId = request.getServerId();
                log.info("this peer {} is been the follower of the leader peer {}", peerMap.get(serverId), peerMap.get(request.getServerId()));
            }

            // 当前已经发生了脑裂，两个leader都进行stepDown重新触发选举
            if (request.getServerId() != leaderId) {
                log.error("Another peer {} declares that it is the leader at term {} which was occupied by leader {}.",
                        serverId, currentTerm, this.leaderId);
                stepDown(request.getTerm() + 1);
                return responseBuilder.setTerm(request.getTerm() + 1)
                        .setResCode(RaftMessage.ResCode.RES_CODE_FAIL).build();
            }

            // 日志不能顺序append。拒绝
            if (request.getPrevLogIndex() != nowLastIndexOfLog) {
                log.info("Rejecting AppendEntries RPC would leave gap, " +
                                "request prevLogIndex={}, my lastLogIndex={}",
                        request.getPrevLogIndex(), nowLastIndexOfLog);
                return responseBuilder.build();
            }

            if (request.getPrevLogTerm() != nowLastTermOfLog) {
                log.info("Rejecting AppendEntries RPC: terms don't agree, " +
                                "request prevLogTerm={} in prevLogIndex={}, my is {}",
                        request.getPrevLogTerm(), request.getPrevLogIndex(),
                        nowLastTermOfLog);
                Validate.isTrue(request.getPrevLogIndex() > 0);
                // 方便进行回退
                responseBuilder.setLastLogIndex(request.getPrevLogIndex() - 1);
                return responseBuilder.build();
            }

            responseBuilder.setResCode(RaftMessage.ResCode.RES_CODE_SUCCESS);

            if(request.getEntries(0)!=null) {
                FollowerAppendLogClosure followerAppendLogClosure = new FollowerAppendLogClosure(request.getEntries(0), responseBuilder);
                logManager.appendEntries(request.getEntries(0), followerAppendLogClosure);
                // 保证log顺序append，log设计需改进
                while (!followerAppendLogClosure.done) {
                    Thread.yield();
                }
            }
            log.info("AppendEntries request from server {} " +
                            "in term {} (my term is {}), entryCount={} resCode={}",
                    request.getServerId(), request.getTerm(), currentTerm,
                    request.getEntriesCount(), responseBuilder.getResCode());

            if(commitIndex < request.getCommitIndex()){
                this.commitIndex = request.getCommitIndex();
            }

            //  followCommit
            if(commitIndex > appliedIndex){
                long endIndex = Math.min(commitIndex,logManager.getLastLogIndex());
                updateCommit(endIndex);
            }
            return responseBuilder.build();
        } finally {
            writeLock.unlock();
        }
    }

    public void appendEntity(Peer peer, RaftMessage.LogEntry logEntry) {
        RaftMessage.AppendEntriesRequest.Builder requestBuilder = RaftMessage.AppendEntriesRequest.newBuilder();

        long prevLogIndex = peer.getNextIndex() - 1;
        long prevLogTerm = logManager.getTerm(prevLogIndex);

        // 判断心跳包
        int numEnty = logEntry == null ? 0 : 1;

        writeLock.lock();
        try {
            requestBuilder.setServerId(serverId);
            requestBuilder.setTerm(currentTerm);
            requestBuilder.setPrevLogTerm(prevLogTerm);
            requestBuilder.setPrevLogIndex(prevLogIndex);
            requestBuilder.addEntries(logEntry);
            // 半数落后的法定节点，可以提交commit，因为leader已经commit了
            requestBuilder.setCommitIndex(Math.min(commitIndex, prevLogIndex + numEnty));
        } finally {
            writeLock.unlock();
        }

        RaftMessage.AppendEntriesRequest request = requestBuilder.build();
        RaftMessage.AppendEntriesResponse response = peer.getRaftConsensusService().appendEntries(request);

        writeLock.lock();
        try {
            if (response == null) {
                log.warn("appendEntries with peer {} failed", peer);
                return;
            }
            log.info("AppendEntries response[{}] from server {} " +
                            "in term {} (my term is {})",
                    response.getResCode(), peer.getServerNode().getServerId(),
                    response.getTerm(), currentTerm);

            if (response.getTerm() > currentTerm) {
                stepDown(response.getTerm());
            } else {
                // 成功append
                if (response.getResCode() == RaftMessage.ResCode.RES_CODE_SUCCESS) {
                    peer.setMatchIndex(prevLogIndex + numEnty);
                    peer.setNextIndex(peer.getMatchIndex() + numEnty);
                    // commit过了也无所谓，因为会过滤
                    // 但是不能快，通过replicator保证
                    if(0 != numEnty) {
                        this.ballotBox.commitAt(logEntry.getIndex(), logEntry.getIndex(), peer);
                    }
                } else {
                    peer.setNextIndex(response.getLastLogIndex() + 1);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public LogManager getLogManager(){
        return this.logManager;
    }

    public RaftOptions getRaftOptions(){
        return this.raftOptions;
    }

}
