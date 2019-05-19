package cn.edu.seu.core;

import cn.edu.seu.service.RaftNode;
import cn.edu.seu.storage.RaftFutureQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

@Slf4j
public class BallotBox {


    private RaftFutureQueue futureQueue;
    private RaftNode raftNode;
    private final StampedLock stampedLock = new StampedLock();
    private long lastCommittedIndex = 0;
    private long pendingIndex;
    private final LinkedList<Ballot> pendingMetaQueue = new LinkedList<>();

    public BallotBox(RaftFutureQueue futureQueue, RaftNode raftNode) {
        this.futureQueue = futureQueue;
        this.raftNode = raftNode;
    }

    public long getLastCommittedIndex() {
        long optimisticRead = stampedLock.tryOptimisticRead();
        long theValue = this.lastCommittedIndex;
        if (stampedLock.validate(optimisticRead)) {
            return theValue;
        }
        long readLockStamp = 0;
        try {
            readLockStamp = stampedLock.readLock();
            return this.lastCommittedIndex;
        } finally {
            stampedLock.unlockRead(readLockStamp);
        }
    }

    public void appendTask(RaftFuture future, Map<Integer, Peer> peerMap) {
        long stamped = 0;
        try {
            Ballot ballot = new Ballot(peerMap);
            pendingMetaQueue.add(ballot);
            futureQueue.appendFuture(future);
            stamped = stampedLock.writeLock();

        } finally {
            stampedLock.unlockWrite(stamped);
        }
    }

    // only for leader
    // 当前日志管理加入内容的位置
    public boolean resetPendingIndex(long newPendingIndex) {
        long stamped = 0;
        try {
            stamped = stampedLock.writeLock();
            this.pendingIndex = newPendingIndex;
            futureQueue.resetFirstIndex(newPendingIndex);
        } finally {
            stampedLock.unlockWrite(stamped);
        }
        return true;
    }

    // only for leader
    public boolean commitAt(long firstIndex, long endIndex, Peer peer) {
        long stamped = 0;
        try {
            // lastCommittedIndex >= pendingIndex，因为pending存开始标志，lastCommittedIndex用来定位的
            long lastCommittedIndex = 0;
            stamped = stampedLock.writeLock();
            if (endIndex < pendingIndex) {
                log.info("peer {} commit last log has been committed!", peer);
                return true;
            }
            if (endIndex > pendingIndex + pendingMetaQueue.size()) {
                log.error("peer {} commit last log is larger than leader can control!", peer);
                return false;
            }

            // 能够跳过那些比较落后的半数法定节点
            long startAt = firstIndex > pendingIndex ? firstIndex : pendingIndex;
            for (long i = startAt; i <= endIndex; i++) {
                pendingMetaQueue.get((int) (startAt - pendingIndex)).grant(peer);
                if (pendingMetaQueue.get((int) (endIndex - pendingIndex)).isGrant()) {
                    lastCommittedIndex = i;
                }
            }

            if (0 == lastCommittedIndex) {
                return true;
            }

            long gap = lastCommittedIndex - pendingIndex + 1;
            pendingIndex += gap;
            while (gap != 0) {
                pendingMetaQueue.pollFirst();
                gap--;
            }
        } finally {
            stampedLock.unlockWrite(stamped);
        }
        raftNode.updateCommit(getLastCommittedIndex());
        return true;
    }

    public boolean followCommit(long lastCommittedIndex){
        boolean doUnlock = true;
        final long stamp = stampedLock.writeLock();
        try {
            if (pendingIndex != 0 || !pendingMetaQueue.isEmpty()) {
                if(lastCommittedIndex < this.pendingIndex) {
                    log.error("Node changes to leader, pendingIndex=%d, param lastCommittedIndex=%d", pendingIndex,
                            lastCommittedIndex);
                }
                return false;
            }
            if (lastCommittedIndex < this.lastCommittedIndex) {
                return false;
            }
            if (lastCommittedIndex > this.lastCommittedIndex) {
                this.lastCommittedIndex = lastCommittedIndex;
                stampedLock.unlockWrite(stamp);
                doUnlock = false;
                raftNode.updateCommit(lastCommittedIndex);
            }
        } finally {
            if (doUnlock) {
                stampedLock.unlockWrite(stamp);
            }
        }
        return true;
    }

}
