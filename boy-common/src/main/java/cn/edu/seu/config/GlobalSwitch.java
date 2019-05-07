package cn.edu.seu.config;

import java.util.BitSet;

/**
 * 全局开关设置
 */
public class GlobalSwitch implements Switch{

    public static final int CONNECT_WARM_SWITCH = 0;

    private BitSet setting = new BitSet();

    // todo 通过configManager获取常规的设置,通过mysql进行刷新
    public GlobalSwitch(){
        setting.set(CONNECT_WARM_SWITCH,true);
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
