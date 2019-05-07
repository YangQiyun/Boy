package cn.edu.seu.config;

import exception.EmptyException;

import java.util.Set;

public interface ConfigManager {

    <T> T getDefaultValue(String configType) throws EmptyException;

    Set<String> listAllType();

    <T> void addConfigType(String type,T defaultValue);
}
