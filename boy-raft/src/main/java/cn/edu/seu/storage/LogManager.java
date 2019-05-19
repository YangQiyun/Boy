package cn.edu.seu.storage;

import cn.edu.seu.core.Status;
import cn.edu.seu.proto.RaftMessage;

public interface LogManager {


    abstract class LogClosure {
        protected long firstLogIndex = 0;
        protected RaftMessage.LogEntry entry;

        public LogClosure(RaftMessage.LogEntry entry) {
            this.entry = entry;
        }

        public abstract void run(Status status);
    }


    /**
     * Get the log term at index.
     *
     * @param index the index of log entry
     * @return the term of log entry
     */
    long getTerm(long index);

    /**
     * Get the last log index of log
     */
    long getLastLogIndex();

    void appendEntries(RaftMessage.LogEntry entry, LogClosure closure);

    RaftMessage.LogEntry getLogEntry(long index);

}
