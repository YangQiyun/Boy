package cn.edu.seu.config;

import java.util.HashMap;

public class AbstractConfigurableInstance implements Configure {
    private HashMap<String, Object> configStorage = new HashMap<>();
    private GlobalSwitch globalSwitch = new GlobalSwitch();
    private ConfigManager configManager;

    public AbstractConfigurableInstance(){
        setConfigManager(new DefaultConfigManager());
    }

    public AbstractConfigurableInstance(ConfigManager configManager) {
        setConfigManager(configManager);
        for (String configType : configManager.listAllType()) {
            configStorage.putIfAbsent(configType, configManager.getDefaultValue(configType));
        }
    }

    public GlobalSwitch getSwitch() {
        return this.globalSwitch;
    }

    public <T> T get(String configType) {
        return (T) configStorage.get(configType);
    }

    public void set(String configType, Object value) {
        configStorage.put(configType, value);
    }

    public void setConfigManager(ConfigManager configManager){
        this.configManager = configManager;
    }
}
