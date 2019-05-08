package cn.edu.seu.rpc.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Data
@Slf4j
public class RpcFuture<T> implements Future<Object> {

    private Long requestId;

    private Class<T> responseClass;

    private RpcCallback rpcCallback;

    private ScheduledFuture scheduledFuture;

    private Object fullRequest;

    private CountDownLatch latch;

    private Object response;

    private Throwable throwable;

    private boolean isCancel = false;
    private boolean isDone = false;
    private boolean isCanceled = false;

    public RpcFuture(Long requestId, Class<T> responseClass, RpcCallback rpcCallback, ScheduledFuture scheduledFuture, Object fullRequest) {
        this.requestId = requestId;
        this.responseClass = responseClass;
        this.rpcCallback = rpcCallback;
        this.scheduledFuture = scheduledFuture;
        this.fullRequest = fullRequest;

        latch = new CountDownLatch(1);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return isCancel;
    }

    @Override
    public boolean isCancelled() {
        return isCanceled;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        if (this.throwable != null) {
            log.error(this.throwable.getMessage());
            return null;
        }
        return (T) response;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            if (latch.await(timeout, unit)) {
                if (throwable != null) {
                    log.error("error occurrs due to {}", throwable.getCause().getMessage());
                    return null;
                }
            } else {
                log.warn("sync call time out");
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("sync call is interrupted, {}", e);
            return null;
        }
        return (T) response;
    }

    public void timeout() {
        this.response = null;
        latch.countDown();
        if (rpcCallback != null) {
            rpcCallback.fail(new RuntimeException("timeout"));
        }
        isDone = true;
    }

    public void success(Object response) {
        this.response = response;
        scheduledFuture.cancel(true);
        if (rpcCallback != null) {
            rpcCallback.success((T) response);
        }
        latch.countDown();
        isDone = true;
    }

    public void fail(Throwable throwable) {
        this.throwable = throwable;
        scheduledFuture.cancel(true);
        if (rpcCallback != null) {
            rpcCallback.fail(throwable);
        }
        latch.countDown();
        isDone = true;
    }
}
