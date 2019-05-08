package cn.edu.seu.connection;

import io.netty.channel.ChannelHandler;

public class DefaultConnectionFactory extends AbstractConnectionFactory {

    public DefaultConnectionFactory(ChannelHandler workHandler) {
        super(workHandler);
    }
}
