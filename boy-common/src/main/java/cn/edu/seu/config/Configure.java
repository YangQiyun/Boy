package cn.edu.seu.config;

import exception.EmptyException;

public interface Configure {

    /**
     * 获取开关设置
     *
     * @return
     */
    GlobalSwitch getSwitch();

    /**
     * 获取配置信息
     *
     * @param configType
     * @param <T>
     * @return
     */
    <T> T getConfig(String configType) throws EmptyException;

    /**
     * 设置配置的值
     *
     * @param configType
     * @param value
     */
    void setConfig(String configType, Object value);
}
