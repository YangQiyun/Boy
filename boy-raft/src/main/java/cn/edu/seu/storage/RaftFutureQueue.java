package cn.edu.seu.storage;

import cn.edu.seu.core.RaftFuture;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RaftFutureQueue {

    private final Lock lock;
    private long firstIndex;
    private ArrayList<RaftFuture> queue;

    public RaftFutureQueue() {
        lock = new ReentrantLock();
        firstIndex = 0;
        queue = new ArrayList<>();
    }

    public void resetFirstIndex(long firstIndex){
        try{
            lock.lock();
            this.firstIndex = firstIndex;
        }finally {
            lock.unlock();
        }
    }

    public void appendFuture(RaftFuture raftFuture){
        try{
            lock.lock();
            this.queue.add(raftFuture);
        }finally {
            lock.unlock();
        }
    }
}
