package cn.edu.seu.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 投票器，用于投票和获取结果
 */
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
        if (!voteMap.put(peer.getServerNode().getServerId(), true)) {
            quorum--;
        }
    }

    public synchronized boolean isGrant() {
        if (quorum <= 0) {
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
