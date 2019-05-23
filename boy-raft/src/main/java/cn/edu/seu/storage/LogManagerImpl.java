package cn.edu.seu.storage;

import cn.edu.seu.commom.Lifecycle;
import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.core.Peer;
import cn.edu.seu.core.Status;
import cn.edu.seu.dao.LogDao;
import cn.edu.seu.dao.LogStorageUtils;
import cn.edu.seu.proto.RaftMessage;
import com.google.protobuf.ByteString;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import exception.EmptyException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class LogManagerImpl implements LogManager, Lifecycle<LogOptions> {

    private LogStorageUtils storageUtils = LogStorageUtils.INSTANCE;

    private Peer peer;
    private String tableName;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private Condition preLogCommit = writeLock.newCondition();
    private volatile long lastLogIndex = 0;
    private long pendIndex = 0;
    private volatile long term = 0;

    private Disruptor<LogEntryWithClosure> disruptor;
    private RingBuffer<LogEntryWithClosure> ringBuffer;

    public LogManagerImpl(Peer peer) {
        this.peer = peer;
        init(new LogOptions());
    }

    @Override
    public boolean init(LogOptions logOptions) {
        tableName = "server" + peer.getServerNode().getEndPoint().getPort();
        disruptor = new Disruptor<>(LogEntryWithClosure::new,
                logOptions.getDisruptorBufferSize(),
                new NamedThreadFactory("boy-raft-logManager-serverID" + peer.getServerNode().getServerId(), true));
        disruptor.handleEventsWith(new LogEntryWithClosureHandler());
        disruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(this.getClass().getSimpleName()));
        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();

        return true;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public long getTerm(long index) {
        RaftMessage.LogEntry logEntry = findLogEntryByIndex(index);
        if (logEntry != null) {
            return logEntry.getIndex();
        } else {
            return -1;
        }
    }

    @Override
    public long getLastLogIndex() {
        try {
            readLock.lock();
            return lastLogIndex;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 当前版本每次只是append一条log
     *
     * @param entry
     * @param closure
     */
    @Override
    public void appendEntries(RaftMessage.LogEntry entry, LogClosure closure) {
        closure.firstLogIndex = entry.getIndex();
        disruptor.publishEvent((entryWithClosure, sequence) -> {
            log.debug("logManager publish event {}", entry);
            entryWithClosure.setClosure(closure);
            entryWithClosure.setLogEntry(entry);
        });
    }

    @Override
    public RaftMessage.LogEntry getLogEntry(long index) {
        return findLogEntryByIndex(index);
    }

    /**
     * 该节点发生变动时触发，和node同步,但是咩用
     *
     * @param term
     */
    public void setTerm(long term) {
        try {
            writeLock.lock();
            this.term = term;
        } finally {
            writeLock.unlock();
        }
    }

    @Data
    private static class LogEntryWithClosure {

        RaftMessage.LogEntry logEntry;
        LogClosure closure;

    }

    private class LogEntryWithClosureHandler implements EventHandler<LogEntryWithClosure> {
        /**
         * 存在问题，必须按照顺序执行，前面log没完成会造成后面log锁住
         *
         * @param logEntryWithClosure
         * @param l
         * @param b
         * @throws Exception
         */
        @Override
        public void onEvent(LogEntryWithClosure logEntryWithClosure, long l, boolean b) throws Exception {
            Status status = Status.STORAGE_LOG_ERROR;
            if (appendToStorage(logEntryWithClosure.logEntry)) {
                status = Status.NORMAL;
            }
            log.debug("logManager 的插入结果是{}", status);
            try {
                writeLock.lock();
                long waitForIndex = logEntryWithClosure.logEntry.getIndex();
                while (lastLogIndex != waitForIndex - 1) {
                    preLogCommit.await();
                }
                if (lastLogIndex == waitForIndex - 1) {
                    lastLogIndex = logEntryWithClosure.logEntry.getIndex();
                    term = logEntryWithClosure.logEntry.getTerm();
                    log.debug("更新lastLogIndex {} 和 term {}", lastLogIndex, term);
                    preLogCommit.signalAll();
                } else {
                    log.error("LogEntryWithClosureHandler could not happen, now lastLogIndex is" +
                            "{} and waitForIndex is {}", lastLogIndex, waitForIndex);
                }
            } finally {
                writeLock.unlock();
            }
            logEntryWithClosure.closure.run(status);
        }
    }

    private String getTableName() {
        if (tableName == null) {
            log.error("the tableName of logManager from peer {} is not init!", peer);
        }
        return tableName;
    }

    private boolean appendToStorage(RaftMessage.LogEntry entry) {
        return storageUtils.insertLog(getTableName(), entry.getIndex(), entry.getTerm(), entry.getData().toByteArray());
    }

    private RaftMessage.LogEntry findLogEntryByIndex(long index) {
        LogDao logDao = null;
        try {
            logDao = storageUtils.findByLogIndex(getTableName(), index);
        } catch (EmptyException e) {
            log.error("can not find the logEntry by index {}", index);
            return null;
        }
        RaftMessage.LogEntry logEntry = RaftMessage.LogEntry.newBuilder()
                .setType(RaftMessage.EntryType.ENTRY_TYPE_DATA)
                .setTerm(logDao.getTerm())
                .setData(ByteString.copyFrom(logDao.getData()))
                .setIndex(logDao.getLogindex())
                .build();
        return logEntry;
    }

    public long addOneAndGet() {
        try {
            writeLock.lock();
            return ++pendIndex;
        } finally {
            writeLock.unlock();
        }
    }

}
