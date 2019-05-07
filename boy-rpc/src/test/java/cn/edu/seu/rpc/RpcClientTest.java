package cn.edu.seu.rpc;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class RpcClientTest {

    public static void main(String[] args){
        FutureTask<Integer> futureTask = new FutureTask<>(()->{
            System.out.println(Thread.currentThread().getId());
            return 1;
        });

        Executor executor = Executors.newSingleThreadExecutor();
        ((ExecutorService) executor).submit(futureTask);
        ((ExecutorService) executor).shutdown();

        System.out.println("current"+Thread.currentThread().getId());
    }
}
