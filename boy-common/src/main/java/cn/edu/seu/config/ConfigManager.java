package cn.edu.seu.config;

import java.util.Set;

public interface ConfigManager {

    <T> T getDefaultValue(String configType);

    Set<String> listAllType();

    <T> void addConfigType(String type,T defaultValue);
}
