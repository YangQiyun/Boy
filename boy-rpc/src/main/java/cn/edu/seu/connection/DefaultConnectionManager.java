package cn.edu.seu.connection;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.config.Configs;
import cn.edu.seu.config.GlobalSwitch;
import cn.edu.seu.config.RpcConfigManager;
import cn.edu.seu.exception.RemotingException;
import cn.edu.seu.rpc.EndPoint;
import exception.EmptyException;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class DefaultConnectionManager implements ConnectionManager, ConnectionSelectStrategy {

    private ConcurrentHashMap<String, ConnectionFutureTask> connTasks = new ConcurrentHashMap<>();

    private RpcConfigManager configManager = RpcConfigManager.INSTANCE;

    private ThreadPoolExecutor poolExecutor;

    private ScheduledExecutorService checkExecutor;

    private GlobalSwitch configSwitch;

    private ConnectionFactory connectionFactory;

    private ChannelHandler workHandler;

    private ConcurrentHashMap<String, ReentrantLock> createLosks = new ConcurrentHashMap<>();

    public DefaultConnectionManager(ChannelHandler workHandler) {
        this.workHandler = workHandler;
    }

    public void init() {

        connectionFactory = new DefaultConnectionFactory(workHandler);
        connectionFactory.init();

        try {
            poolExecutor = new ThreadPoolExecutor(configManager.getDefaultValue(Configs.CONN_CREATE_TP_MIN_SIZE),
                    configManager.getDefaultValue(Configs.CONN_CREATE_TP_MAX_SIZE),
                    configManager.getDefaultValue(Configs.CONN_CREATE_TP_KEEPALIVE_TIME),
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(configManager.getDefaultValue(Configs.CONN_CREATE_TP_QUEUE_SIZE)),
                    new NamedThreadFactory("connectionPool", true));

            checkExecutor = Executors.newScheduledThreadPool(2,
                    new NamedThreadFactory("checkConnection", true));
        } catch (EmptyException e) {
            poolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            log.error("connection thread pool parameters are not init");
        }

        configSwitch = new GlobalSwitch();
        check();

    }

    public Connection select(List<Connection> conns) {
        return conns.get(new Random().nextInt(conns.size()));
    }

    public Connection select(String poolKey) {
        if (null != getAll(poolKey)) {
            return select(getAll(poolKey));
        }
        return null;
    }

    public void add(Connection connection) {

        for (String poolkey : connection.getPoolKeys()) {
            add(connection, poolkey);
        }
    }

    public void add(Connection connection, String poolKey) {

        if (null == connTasks.get(poolKey)) {
            EndPoint tmpPonit = new EndPoint(connection.getIp(), connection.getPort(), -1);
            try {
                doCreate(tmpPonit, new ConnectionPoolCallable(true, poolKey), false);
            } catch (Exception e) {
                log.error("create empty connection pool is err!", e.getCause());
            }
        }

        if (null == connTasks.get(poolKey).getSaftly(log)) {
            log.error("add connection fail");
        } else {
            connection.increRefer();
            connTasks.get(poolKey).getSaftly(log).addConnection(connection);
        }
    }

    public Connection get(String poolKey) {

        if (null == connTasks.get(poolKey)) {
            return null;
        }

        ConnectionPool pool = connTasks.get(poolKey).getSaftly(log);
        return null == pool ? null : pool.getOne();
    }

    public List<Connection> getAll(String poolKey) {

        if (null == connTasks.get(poolKey)) {
            return null;
        }

        ConnectionPool pool = connTasks.get(poolKey).getSaftly(log);
        return null == pool ? null : pool.getAll();
    }

    public Map<String, List<Connection>> getAll() {
        return null;
    }

    public void remove(Connection connection) {
        for (String poolKey : connection.getPoolKeys()) {
            remove(connection, poolKey);
        }
    }

    public void remove(Connection connection, String poolKey) {
        try {
            if (connTasks.get(poolKey).getSaftly(log).getAll().remove(connection)) {
                connection.decreRefer();
            }
        } catch (Exception e) {
            log.error("remove connection error", e);
        }

    }

    public void remove(String poolKey) {

        ConnectionFutureTask task = connTasks.remove(poolKey);
        if (task != null) {
            ConnectionPool pool = task.getSaftly(log);
            task.cancel(true);
            if (pool != null) {
                for (Connection connection : pool.getAll()) {
                    connection.decreRefer();
                }
            }
        }
    }

    public void removeAll() {
        for (String poolKey : connTasks.keySet()) {
            remove(poolKey);
        }
    }

    private void check() {
        checkExecutor.schedule(() -> {
            for (String poolKey : connTasks.keySet()) {
                if (null != connTasks.get(poolKey) && null != connTasks.get(poolKey).getSaftly(log)) {
                    ConnectionPool pool = connTasks.get(poolKey).getSaftly(log);
                    for (Connection connection : pool.getAll()) {
                        if (connection.isIDle()) {
                            connection.close();
                        }
                    }
                }
            }
        }, 20, TimeUnit.SECONDS);
    }

    public void check(Connection connection) throws RemotingException {
        throw new RemotingException("this method is not complete! please do not use this!");
    }

    public int count(String poolKey) {
        ConnectionFutureTask task = connTasks.get(poolKey);
        if (null == task || task.getSaftly(log) != null) {
            return 0;
        } else {
            return task.getSaftly(log).getAll().size();
        }
    }

    public Connection getAndCreateIfAbsent(EndPoint endPoint) throws InterruptedException, RemotingException {
        return doCreate(endPoint,
                new ConnectionPoolCallable(connectionFactory,
                        endPoint,
                        this,
                        configSwitch.isOn(GlobalSwitch.CONNECT_WARM_SWITCH)), true);
    }

    private Connection doCreate(EndPoint endPoint, ConnectionPoolCallable callable, boolean openDegradedMode) throws InterruptedException, RemotingException {

        createLosks.putIfAbsent(endPoint.getUniqueKey(), new ReentrantLock());
        ReentrantLock lock = createLosks.get(endPoint.getUniqueKey());
        try {
            lock.lock();

            String poolKey = endPoint.getUniqueKey();
            if (null == connTasks.get(poolKey)) {
                ConnectionFutureTask initTask = new ConnectionFutureTask(callable);
                initTask = connTasks.putIfAbsent(poolKey, initTask);

                if (null == initTask) {
                    initTask = connTasks.get(poolKey);
                    initTask.run();
                }

                ConnectionPool pool = null;
                int timesOfInterrupt = 0;
                int retry = 5;

                for (int i = 0; i < retry && null == pool; ++i) {
                    try {
                        pool = initTask.get();

                        // the last retry is fail. change to degraded mode. and reset the retry times.
                        if (i == retry) {
                            initTask.cancel(true);
                            // Degraded mode 保证pool优先创建出一个connection即可
                            if (openDegradedMode) {
                                connTasks.remove(poolKey);
                                openDegradedMode = false;
                                i = 0;
                                initTask = new ConnectionFutureTask(
                                        new ConnectionPoolCallable(connectionFactory,
                                                endPoint,
                                                this,
                                                true));
                                connTasks.put(poolKey, initTask);
                                continue;
                            } else {
                                throw new RemotingException(String.format("create connection pool of key %s is time out!", poolKey));

                            }
                        }

                        if (null == pool) {
                            continue;
                        } else {
                            return pool.getOne();
                        }


                    } catch (InterruptedException e) {
                        if (i + 1 < retry) {
                            continue;// retry if interrupted
                        }
                        this.connTasks.remove(poolKey);
                        log.warn(
                                "Future task of poolKey {} interrupted {} times. InterruptedException thrown and stop retry.",
                                poolKey, (timesOfInterrupt + 1), e);
                        throw e;
                    } catch (ExecutionException e) {
                        // DO NOT retry if ExecutionException occurred
                        this.connTasks.remove(poolKey);

                        Throwable cause = e.getCause();
                        if (cause instanceof RemotingException) {
                            throw (RemotingException) cause;
                        } else {
                            log.error("uncheck excepiton in create connection pool", cause);
                        }
                    }
                }

                // can not be there
                throw new InterruptedException("can not be there when create connection pool");
            } else {
                return get(poolKey);
            }
        } finally {
            lock.unlock();
        }
    }

    public void createConnectionAndHealIfNeed(EndPoint endPoint) throws InterruptedException, RemotingException {
        throw new RemotingException("this method is not complete! please do not use this!");
    }

    public Connection create(EndPoint endPoint) throws RemotingException {
        if (null == connTasks.get(endPoint.getUniqueKey())) {
            try {
                return getAndCreateIfAbsent(endPoint);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
            return null;
        } else {
            return get(endPoint.getUniqueKey());
        }
    }

    public Connection create(String address, int connectTimeout) throws RemotingException {
        throw new RemotingException("this method is not complete! please do not use this!");
    }

    public Connection create(String ip, int port, int connectTimeout) throws RemotingException {
        throw new RemotingException("this method is not complete! please do not use this!");
    }

    public ThreadPoolExecutor getPoolExecutor() {
        return poolExecutor;
    }
}
