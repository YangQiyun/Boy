package cn.edu.seu.exception;

public class FutureTaskNotCompleted extends Exception {

    public FutureTaskNotCompleted(String message) {
        super(message);
    }

    public FutureTaskNotCompleted(Throwable cause) {
        super(cause);
    }
}
