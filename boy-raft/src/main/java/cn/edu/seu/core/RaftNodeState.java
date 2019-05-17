package cn.edu.seu.core;

/**
 * raft节点的状态
 */
public enum RaftNodeState {
    STATE_LEADER, // It's a leader
    STATE_TRANSFERRING, // It's transferring leadership
    STATE_CANDIDATE, //  It's a candidate
    STATE_FOLLOWER, // It's a follower
    STATE_ERROR, // It's in error
    STATE_UNINITIALIZED, // It's uninitialized
    STATE_SHUTTING, // It's shutting down
    STATE_SHUTDOWN, // It's shutdown already
    STATE_END; // State end

    public boolean isActive() {
        return this.ordinal() < STATE_ERROR.ordinal();
    }
}
