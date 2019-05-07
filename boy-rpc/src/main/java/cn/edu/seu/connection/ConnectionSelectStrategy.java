package cn.edu.seu.connection;

import java.util.List;

public interface ConnectionSelectStrategy {

    Connection select(List<Connection> conns);
}
