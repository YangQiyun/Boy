package cn.edu.seu.rpc.client;

import cn.edu.seu.common.NamedThreadFactory;
import cn.edu.seu.config.AbstractConfigurableInstance;
import cn.edu.seu.config.Configs;
import cn.edu.seu.connection.Connection;
import cn.edu.seu.connection.DefaultConnectionManager;
import cn.edu.seu.exception.RemotingException;
import cn.edu.seu.rpc.EndPoint;
import exception.EmptyException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcClient extends AbstractConfigurableInstance {

    private DefaultConnectionManager connectionManager;
    private ConcurrentMap<Long, RpcFuture> pendingRPC;
    private List<EndPoint> endPoints;
    private ScheduledExecutorService timeoutTimer;
    private ScheduledExecutorService checkTimer;
    private Random random = new Random();

    public RpcClient(EndPoint endPoint){
        this();
        ArrayList<EndPoint> endPoints = new ArrayList<>(1);
        endPoints.add(endPoint);
        this.endPoints = endPoints;
        init();
    }

    public RpcClient(List<EndPoint> endPoints) {
        this();
        this.endPoints = endPoints;
        init();
    }

    private RpcClient() {
        super();
        setConfig(Configs.RPC_CLIENT, true);
        setConfig(Configs.RPC_SERVER, Configs.RPC_SERVER_DEFAULT);
        setConfig(Configs.READ_TIMEOUT_MILLIS, Configs.READ_TIMEOUT_MILLIS_DEFAULT);
        setConfig(Configs.RETRY_TIME, Configs.RETRY_TIME_DEFAULT);
    }

    private void init() {
        connectionManager = new DefaultConnectionManager(new RpcClientHandler(this));
        connectionManager.init();
        pendingRPC = new ConcurrentHashMap<>();

        for (EndPoint endPoint : endPoints) {
            try {
                connectionManager.create(endPoint);
            } catch (RemotingException e) {
                log.error(e.getMessage());
            }
        }

        timeoutTimer = Executors.newScheduledThreadPool(1, new NamedThreadFactory("sendRequestTimer", true));
        checkTimer = Executors.newScheduledThreadPool(1,new NamedThreadFactory("checkConnection",true));
        checkConnection();
    }

    public RpcFuture sendRequest(long requestId, Object fullRequest, Class<?> responseClass, RpcCallback callback) {
        // random one endpoint
        Connection connection = connectionManager.get(endPoints.get(random.nextInt(endPoints.size())).getUniqueKey());
        if (null != connection && null != connection.getChannel()) {
            try {
                ScheduledFuture<?> scheduledFuture = timeoutTimer.schedule(() -> {
                    RpcFuture rpcFuture = removeFuture(requestId);
                    if (rpcFuture != null) {
                        log.debug("request timeout, requestId={}", requestId);
                    }
                }, getConfig(Configs.READ_TIMEOUT_MILLIS), TimeUnit.MILLISECONDS);

                RpcFuture future = new RpcFuture(requestId, responseClass, callback, scheduledFuture, fullRequest);
                addFuture(requestId, future);
                connection.getChannel().writeAndFlush(fullRequest);
                return future;
            } catch (EmptyException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }

    private void checkConnection(){
        checkTimer.schedule(new Runnable() {
            @Override
            public void run() {
                for(EndPoint endPoint:endPoints){
                    try {
                        if (connectionManager.create(endPoint) != null){
                            continue;
                        }
                    } catch (RemotingException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        },1,TimeUnit.SECONDS);
    }

    public void addFuture(long requestId, RpcFuture future) {
        pendingRPC.put(requestId, future);
    }

    public RpcFuture getFuture(long requestId) {
        return pendingRPC.get(requestId);
    }

    public RpcFuture removeFuture(long requestId) {
        return pendingRPC.remove(requestId);
    }
}
