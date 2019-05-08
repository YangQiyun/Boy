package cn.edu.seu.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultConfigManager implements ConfigManager {

    private Map<String, Object> defaultConfigMap = new HashMap<>();

    public DefaultConfigManager() {
        //todo default config
    }

    @Override
    public <T> T getDefaultValue(String configType) {
        return (T) defaultConfigMap.get(configType);
    }

    @Override
    public Set<String> listAllType() {
        return defaultConfigMap.keySet();
    }

    @Override
    public <T> void addConfigType(String type, T defaultValue) {
        defaultConfigMap.put(type, defaultValue);
    }
}
