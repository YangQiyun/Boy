package cn.edu.seu.service;

import cn.edu.seu.proto.RaftMessage;

public interface RaftConsensusService {

    RaftMessage.VoteResponse preVote(RaftMessage.VoteRequest request);

    RaftMessage.VoteResponse requestVote(RaftMessage.VoteRequest request);

    RaftMessage.AppendEntriesResponse appendEntries(RaftMessage.AppendEntriesRequest request);

}
