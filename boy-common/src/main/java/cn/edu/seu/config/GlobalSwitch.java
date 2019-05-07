package cn.edu.seu.config;

import java.util.BitSet;

/**
 * 全局开关设置
 */
public class GlobalSwitch implements Switch{

    private static final int CONN_MONITOR_SWITCH = 0;

    private BitSet setting = new BitSet();

    // todo 通过configManager获取常规的设置
    public GlobalSwitch(){

    }

    public void tureOn(int index) {
        setting.set(index);
    }

    public void turnOff(int index) {
        setting.clear(index);
    }

    public boolean isOn(int index) {
        return setting.get(index);
    }
}
