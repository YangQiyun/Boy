package cn.edu.seu.config;

public class Configs {
    // ~~~ configs and default values for bootstrap

    /**
     * TCP_NODELAY option
     */
    public static final String TCP_NODELAY = "boy.tcp.nodelay";
    public static final boolean TCP_NODELAY_DEFAULT = true;

    /**
     * TCP SO_REUSEADDR option
     */
    public static final String TCP_SO_REUSEADDR = "boy.tcp.so.reuseaddr";
    public static final boolean TCP_SO_REUSEADDR_DEFAULT = true;

    /**
     * TCP SO_BACKLOG option
     */
    public static final String TCP_SO_BACKLOG = "boy.tcp.so.backlog";
    public static final int TCP_SO_BACKLOG_DEFAULT = 1024;

    /**
     * TCP SO_KEEPALIVE option
     */
    public static final String TCP_SO_KEEPALIVE = "boy.tcp.so.keepalive";
    public static final boolean TCP_SO_KEEPALIVE_DEFAULT = true;

    /**
     * Netty ioRatio option
     */
    public static final String NETTY_IO_RATIO = "boy.netty.io.ratio";
    public static final int NETTY_IO_RATIO_DEFAULT = 70;

    /**
     * Netty buffer allocator, enabled as default
     */
    public static final String NETTY_BUFFER_POOLED = "boy.netty.buffer.pooled";
    public static final boolean NETTY_BUFFER_POOLED_DEFAULT = true;

    /**
     * Netty buffer high watermark
     */
    public static final String NETTY_BUFFER_HIGH_WATERMARK = "boy.netty.buffer.high.watermark";
    public static final String NETTY_BUFFER_HIGH_WATERMARK_DEFAULT = Integer.toString(64 * 1024);

    /**
     * Netty buffer low watermark
     */
    public static final String NETTY_BUFFER_LOW_WATERMARK = "boy.netty.buffer.low.watermark";
    public static final String NETTY_BUFFER_LOW_WATERMARK_DEFAULT = Integer.toString(32 * 1024);

    /**
     * Netty epoll switch
     */
    public static final String NETTY_EPOLL_SWITCH = "boy.netty.epoll.switch";
    public static final boolean NETTY_EPOLL_SWITCH_DEFAULT = false;

    /**
     * Netty epoll level trigger enabled
     */
    public static final String NETTY_EPOLL_LT = "boy.netty.epoll.lt";
    public static final boolean NETTY_EPOLL_LT_DEFAULT = true;


    /**
     * Default connect timeout value, time unit: ms
     */
    public static final String NETTY_CONNECT_TIMEOUT = "boy.netty.connection.timeout";
    public static final int NETTY_CONNECT_TIMEOUT_DEFAULT = 1000;

    /**
     * default connection number per url
     */
    public static final int DEFAULT_CONN_NUM_PER_URL = 1;

    /**
     * max connection number of each url
     */
    public static final int MAX_CONN_NUM_PER_URL = 100 * 10000;

    // ~~~ configs for processor manager

    public static final String HEARTBEAT_SWITCH = "boy.heartbeat.switch";
    public static final boolean HEARTBEAT_SWITCH_DEFAULT = true;

    // ~~~ configs for rpc type

    public static final String RPC_CLIENT = "rpc_client";
    public static final boolean RPC_CLIENT_DEFAULT = false;
    public static final String RPC_SERVER = "rpc_server";
    public static final boolean RPC_SERVER_DEFAULT = false;

    // ~~~ configs for rpc
    public static final String RETRY_TIME = "rpc_retry_time";
    public static int RETRY_TIME_DEFAULT = 1;

    public static final String READ_TIMEOUT_MILLIS = "readTimeoutMillis";
    public static long READ_TIMEOUT_MILLIS_DEFAULT = 1000;

    // ~~~ configs and default values for connection manager

    /**
     * Thread pool min size for the connection manager executor
     */
    public static final String CONN_CREATE_TP_MIN_SIZE = "boy.conn.create.tp.min";
    public static final int CONN_CREATE_TP_MIN_SIZE_DEFAULT = 3;

    /**
     * Thread pool max size for the connection manager executor
     */
    public static final String CONN_CREATE_TP_MAX_SIZE = "boy.conn.create.tp.max";
    public static final int CONN_CREATE_TP_MAX_SIZE_DEFAULT = 8;

    /**
     * Thread pool queue size for the connection manager executor
     */
    public static final String CONN_CREATE_TP_QUEUE_SIZE = "boy.conn.create.tp.queue";
    public static final int CONN_CREATE_TP_QUEUE_SIZE_DEFAULT = 50;

    /**
     * Thread pool keep alive time for the connection manager executor
     */
    public static final String CONN_CREATE_TP_KEEPALIVE_TIME = "boy.conn.create.tp.keepalive";
    public static final long CONN_CREATE_TP_KEEPALIVE_TIME_DEFAULT = 60L;

    // ~~~ configs for server
    public static final String RPC_SERVER_WORK_POOL = "boy.server.workpool";
    public static final int RPC_SERVER_WORK_POOL_DEFAULT = 20;
}
