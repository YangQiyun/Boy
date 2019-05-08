package cn.edu.seu.connection;

import cn.edu.seu.exception.FutureTaskNotCompleted;
import cn.edu.seu.exception.FutureTaskNotRunYetException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConnectionFutureTask extends FutureTask<ConnectionPool> {

    private AtomicBoolean ran = new AtomicBoolean(false);

    public ConnectionFutureTask(Callable<ConnectionPool> callable) {
        super(callable);
    }

    @Override
    public void run() {
        if (!ran.getAndSet(true)) {
            super.run();
        }
    }

    @Override
    public ConnectionPool get() throws InterruptedException, ExecutionException {
        return super.get();
    }

    private void checkStatus() throws FutureTaskNotRunYetException, FutureTaskNotCompleted {
        if (!ran.get()) {
            throw new FutureTaskNotRunYetException("no run");
        }
        if (!isDone()) {
            throw new FutureTaskNotCompleted("not completed");
        }
    }

    /**
     * 苞笼所有的exception，返回connection或者为null
     *
     * @param logger
     * @return
     */
    public ConnectionPool getSaftly(Logger logger) {
        ConnectionPool connectionPool = null;
        try {
            checkStatus();
            connectionPool = super.get();
        } catch (InterruptedException e) {
            logger.error("Future task interrupted!", e);
        } catch (ExecutionException e) {
            logger.error("Future task execute failed!", e);
        } catch (FutureTaskNotRunYetException e) {
            logger.error("Future task has not run yet!", e);
        } catch (FutureTaskNotCompleted e) {
            logger.error("Future task has not completed!", e);
        }
        return connectionPool;
    }

}
