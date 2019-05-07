package cn.edu.seu.util;

import java.util.concurrent.atomic.AtomicLong;

public class IDGeneratorUtil {

    private AtomicLong atomicLong = new AtomicLong(1);

    public static final IDGeneratorUtil INSTANCE = new IDGeneratorUtil();

    public Long getID() {
        return atomicLong.getAndIncrement();
    }
}
