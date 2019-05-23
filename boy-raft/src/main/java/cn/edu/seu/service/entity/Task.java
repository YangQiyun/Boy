package cn.edu.seu.service.entity;

import cn.edu.seu.core.RaftFuture;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class Task {

    private ByteBuffer data;

    private RaftFuture done;

    public Task(ByteBuffer data, RaftFuture done) {
        this.data = data;
        this.done = done;
    }
}
