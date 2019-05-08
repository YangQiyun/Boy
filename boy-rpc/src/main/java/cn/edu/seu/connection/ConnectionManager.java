package cn.edu.seu.connection;

import cn.edu.seu.connection.Connection;
import cn.edu.seu.exception.RemotingException;
import cn.edu.seu.rpc.EndPoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

// 参考自com.alipay.remoting.ConnectionManager
public interface ConnectionManager {
    /**
     * init
     */
    void init();

    /**
     * 加入包含该connection的所有pool
     *
     * @param connection
     */
    void add(Connection connection);

    /**
     * 加入到指定的pool中
     *
     * @param connection
     * @param poolKey
     */
    void add(Connection connection, String poolKey);

    /**
     * 获取指定pool中的某一个connection
     *
     * @param poolKey
     * @return 其他非目标情况均返回null
     */
    Connection get(String poolKey);

    /**
     * 获取指定pool中的所有的链接
     *
     * @param poolKey
     * @return
     */
    List<Connection> getAll(String poolKey);

    /**
     * 获取该manager中的所有链接，key是poolKey，value是conn的list
     *
     * @return
     */
    Map<String, List<Connection>> getAll();


    void remove(Connection connection);

    void remove(Connection connection, String poolKey);


    void remove(String poolKey);

    void removeAll();

    void check(Connection connection) throws RemotingException;

    /**
     * 返回指定的pool链接的个数
     *
     * @param poolKey
     * @return
     */
    int count(String poolKey);

    /**
     * @param endPoint
     * @return
     * @throws InterruptedException
     * @throws RemotingException
     */
    Connection getAndCreateIfAbsent(EndPoint endPoint) throws InterruptedException, RemotingException;

    /**
     * This method can create connection pool with connections initialized and check the number of connections.
     * The connection number of {@link cn.edu.seu.connection.ConnectionPool} is decided by {@link EndPoint#getConnNum()}.
     * Each time call this method, will check the number of connection, if not enough, this will do the healing logic additionally.
     */
    void createConnectionAndHealIfNeed(EndPoint endPoint) throws InterruptedException, RemotingException;

    // ~~~ create operation

    Connection create(EndPoint endPoint) throws RemotingException;

    /**
     * Create a connection using specified {@link String} address.
     *
     * @param address        a {@link String} address, e.g. 127.0.0.1:1111
     * @param connectTimeout an int connect timeout value
     * @return the created {@link Connection}
     * @throws RemotingException if create failed
     */
    Connection create(String address, int connectTimeout) throws RemotingException;

    /**
     * Create a connection using specified ip and port.
     *
     * @param ip             connect ip, e.g. 127.0.0.1
     * @param port           connect port, e.g. 1111
     * @param connectTimeout an int connect timeout value
     * @return the created {@link Connection}
     */
    Connection create(String ip, int port, int connectTimeout) throws RemotingException;

    ThreadPoolExecutor getPoolExecutor();
}
