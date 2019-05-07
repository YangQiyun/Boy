package cn.edu.seu.protocol.standard;

import com.google.protobuf.MessageLite;
import lombok.Data;

@Data
public class RpcMessage<T extends MessageLite>{

    private T header;
    private byte[] body;

    public RpcMessage<T> copyFrom(RpcMessage<T> rhs) {
        this.header = rhs.getHeader();
        this.body = rhs.getBody();
        return this;
    }
}
