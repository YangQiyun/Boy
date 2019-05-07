package cn.edu.seu.config;

import exception.EmptyException;

import java.util.HashMap;

public class AbstractConfigurableInstance implements Configure {
    private HashMap<String, Object> configStorage = new HashMap<>();
    private GlobalSwitch globalSwitch = new GlobalSwitch();
    private ConfigManager configManager;

    public AbstractConfigurableInstance() {
        setConfigManager(new DefaultConfigManager());
    }

    public AbstractConfigurableInstance(ConfigManager configManager) {
        setConfigManager(configManager);
        for (String configType : configManager.listAllType()) {
            try {
                configStorage.putIfAbsent(configType, configManager.getDefaultValue(configType));
            } catch (EmptyException e) {
                e.printStackTrace();
            }
        }
    }

    public GlobalSwitch getSwitch() {
        return this.globalSwitch;
    }

    public <T> T getConfig(String configType) throws Exception {
        if (null == configStorage.get(configType)) {
            throw new Exception("the config of " + configType + " is not init!");
        }
        return (T) configStorage.get(configType);
    }

    public void setConfig(String configType, Object value) {
        configStorage.put(configType, value);
    }

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }
}
