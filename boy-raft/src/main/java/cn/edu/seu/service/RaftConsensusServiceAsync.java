package cn.edu.seu.service;

import cn.edu.seu.proto.RaftMessage;
import cn.edu.seu.rpc.client.RpcCallback;

import java.util.concurrent.Future;

public interface RaftConsensusServiceAsync extends RaftConsensusService{
    Future<RaftMessage.VoteResponse> preVote(
            RaftMessage.VoteRequest request,
            RpcCallback<RaftMessage.VoteResponse> callback);

    Future<RaftMessage.VoteResponse> requestVote(
            RaftMessage.VoteRequest request,
            RpcCallback<RaftMessage.VoteResponse> callback);

    Future<RaftMessage.AppendEntriesResponse> appendEntries(
            RaftMessage.AppendEntriesRequest request,
            RpcCallback<RaftMessage.AppendEntriesResponse> callback);
}
