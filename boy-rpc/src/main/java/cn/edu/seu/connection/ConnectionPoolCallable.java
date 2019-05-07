package cn.edu.seu.connection;

import cn.edu.seu.exception.RemotingException;
import cn.edu.seu.rpc.ConnectionManager;
import cn.edu.seu.rpc.EndPoint;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class ConnectionPoolCallable implements Callable<ConnectionPool> {

    private ConnectionFactory connectionFactory;

    private EndPoint endPoint;

    private ConnectionManager connectionManager;

    // 如果不是进行预热启动，那么进行异步建立连接池
    private boolean warmUp = false;

    private boolean initEmpty = false;
    private String emptyKey;

    public ConnectionPoolCallable(ConnectionFactory connectionFactory, EndPoint endPoint, ConnectionManager connectionManager, boolean warmUp) {
        this.connectionFactory = connectionFactory;
        this.endPoint = endPoint;
        this.connectionManager = connectionManager;
        this.warmUp = warmUp;
    }

    public ConnectionPoolCallable(boolean empty, String poolKey) {
        if (empty == false) {
            throw new IllegalArgumentException("this constructor must create with empty pool!");
        }
        this.emptyKey = poolKey;
        initEmpty = true;
    }

    @Override
    public ConnectionPool call() throws Exception {

        if (initEmpty) {
            return new ConnectionPool(emptyKey);
        }

        ConnectionPool pool = new ConnectionPool(endPoint.getUniqueKey());
        pool.addConnection(connectionFactory.createConnection(endPoint));
        if (endPoint.getConnNum() > 1) {
            if (warmUp) {
                for (int i = 0; i < endPoint.getConnNum() - 1; i++) {
                    connectionManager.getPoolExecutor().submit(() -> {
                        try {
                            pool.addConnection(connectionFactory.createConnection(endPoint));
                        } catch (RemotingException e) {
                            log.error(e.getMessage());
                        }
                    });
                }
            } else {
                for (int i = 0; i < endPoint.getConnNum() - 1; i++) {
                    try {
                        pool.addConnection(connectionFactory.createConnection(endPoint));
                    } catch (RemotingException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        }
        return pool;
    }
}
