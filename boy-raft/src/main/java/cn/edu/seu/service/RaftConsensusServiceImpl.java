package cn.edu.seu.service;

import cn.edu.seu.proto.RaftMessage;

public class RaftConsensusServiceImpl implements RaftConsensusService{

    private RaftNode raftNode;

    public RaftConsensusServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public RaftMessage.VoteResponse preVote(RaftMessage.VoteRequest request) {
        return raftNode.handlePreVoteRequest(request);
    }

    @Override
    public RaftMessage.VoteResponse requestVote(RaftMessage.VoteRequest request) {
        return raftNode.handleRequestVoteRequest(request);
    }

    @Override
    public RaftMessage.AppendEntriesResponse appendEntries(RaftMessage.AppendEntriesRequest request) {
        return raftNode.handleAppendEntries(request);
    }
}
