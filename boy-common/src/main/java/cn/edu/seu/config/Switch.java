package cn.edu.seu.config;

/**
 * 开关接口
 */
public interface Switch {

    void tureOn(int index);

    void turnOff(int index);

    boolean isOn(int index);
}
