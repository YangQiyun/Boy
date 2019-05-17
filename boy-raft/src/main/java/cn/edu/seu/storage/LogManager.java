package cn.edu.seu.storage;

public interface LogManager {

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
}
