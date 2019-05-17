package cn.edu.seu.core;

import java.util.ArrayList;
import java.util.concurrent.locks.StampedLock;

public class BallotBox {

    private StateMachine stateMachine;

    private final StampedLock stampedLock = new StampedLock();
    private long lastCommittedIndex = 0;
    private long pendingIndex;
    private final ArrayList<Ballot> pendingMetaQueue = new ArrayList<>();

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

}
