package cn.edu.seu.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Data
@Slf4j
public class Connection {

    private Channel channel;

    private List<String> poolKeys = new LinkedList<>();

    private String ip;

    private int port;

    private AtomicInteger reference;

    public Connection(Channel channel, String ip, int port, String poolKey) {
        this.channel = channel;
        this.ip = ip;
        this.port = port;
        this.poolKeys.add(poolKey);

        reference = new AtomicInteger(1);
    }

    public void increRefer() {
        reference.getAndIncrement();
    }

    public void decreRefer() {
        reference.getAndDecrement();
    }

    public boolean isIDle() {
        return 0 == reference.get() ? true : false;
    }

    public void close() {
        if (channel == null) {
            return;
        }

        try {


            channel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("the connection which ip is %s and port is %s is closed", ip, port);
                }
            });
        } catch (Exception e) {
            log.warn("Exception caught when closing connection {}", ip, port, e);
        }
    }
}
