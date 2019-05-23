package cn.edu.seu.core;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 投票器，用于投票和获取结果
 */
@Slf4j
public class Ballot {

    private Map<Integer, Boolean> voteMap;
    private int quorum;

    public Ballot(Map<Integer, Peer> peerMap) {
        voteMap = new HashMap<>(peerMap.size());
        peerMap.forEach((key, value) -> {
            voteMap.put(key, new Boolean(false));
        });
        init();
    }

    private void init() {
        quorum = voteMap.size() / 2 + 1;
    }

    public synchronized void grant(Peer peer) {
        log.info("peer {}尝试投票",peer);
        if (!voteMap.put(peer.getServerNode().getServerId(), true)) {
            log.info("peer {}投票成功",peer);
            quorum--;
        }
        log.info("peer {}投票结束",peer);
    }

    public synchronized boolean isGrant() {
        if (quorum <= 0) {
            log.info("投票器有效值是{}", voteMap.size() / 2 + 1);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void reset(Map<Integer, Peer> peerMap) {
        voteMap = new HashMap<>(peerMap.size());
        peerMap.forEach((key, value) -> {
            voteMap.put(key, new Boolean(false));
        });
        init();
    }
}
