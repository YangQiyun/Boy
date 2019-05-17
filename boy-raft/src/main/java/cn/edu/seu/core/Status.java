package cn.edu.seu.core;

/**
 * 状态的描述
 */
public enum Status {
    ;

    Status(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;

    private String message;
}
