package cn.edu.seu.rpc.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RPCServerChannelIdleHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) event;
            if (e.state() == IdleState.ALL_IDLE) {
                // if no read and write for period time, close current channel
                log.debug("channel={} ip={} is idle for period time, close now.",
                        ctx.channel(), ctx.channel().remoteAddress());
                ctx.close();
            } else {
                log.debug("idle on channel[{}]:{}", e.state(), ctx.channel());
            }
        }
    }
}
