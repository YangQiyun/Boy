package cn.edu.seu.core;

/**
 * 状态的描述
 * 一般情况下正常的状态全都是用NORMAL
 */
public enum Status {
    NORMAL(0,"normal"),
    STORAGE_LOG_ERROR(1000,"STORAGE_LOG_ERROR");

    Status(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;

    private String message;

    public boolean isOk() {
        return this.code == 0;
    }

    @Override
    public String toString() {
        return "{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }}
