package cn.edu.seu.connection;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class ConnectionPool {

    private String poolKey;

    public ConnectionPool(String poolKey) {
        this.poolKey = poolKey;
    }

    private CopyOnWriteArrayList<Connection> conns = new CopyOnWriteArrayList<Connection>();

    private ConnectionSelectStrategy strategy;

    public Connection getOne() {
        return null;
    }

    public List<Connection> getAll() {
        return getConns();
    }

    public void addConnection(Connection connection) {
        if (null == connection) {
            return;
        }
        conns.add(connection);
    }

    public int getPoolNum() {
        return conns.size();
    }

}
