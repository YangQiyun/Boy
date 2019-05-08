package cn.edu.seu.exception;

public class FutureTaskNotRunYetException extends Exception {

    public FutureTaskNotRunYetException(String message) {
        super(message);
    }

    public FutureTaskNotRunYetException(Throwable cause) {
        super(cause);
    }
}
