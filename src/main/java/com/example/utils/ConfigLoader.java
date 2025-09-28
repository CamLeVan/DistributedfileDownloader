package com.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final String CONFIG_FILE = "application.properties";
    private static Properties properties;

    static {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Warning: Could not find " + CONFIG_FILE + ", using default values");
                properties.setProperty("servers", "localhost:12347,localhost:12348");
                properties.setProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
                properties.setProperty("file_path", "test.txt");
                properties.setProperty("master_server", "localhost:12347");
                properties.setProperty("db_path", "files.db");
            }
        } catch (IOException e) {
            System.err.println("Failed to load config file: " + e.getMessage());
            properties.setProperty("servers", "localhost:12347,localhost:12348");
            properties.setProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
            properties.setProperty("file_path", "test.txt");
            properties.setProperty("master_server", "localhost:12347");
            properties.setProperty("db_path", "files.db");
        }
    }

    public static String[] getServers() {
        String serversStr = properties.getProperty("servers", "").trim();
        if (serversStr.isEmpty()) {
            System.err.println("Warning: No servers configured, using default");
            return new String[]{"localhost:12347", "localhost:12348"};
        }
        String[] servers = serversStr.split(",");
        for (int i = 0; i < servers.length; i++) {
            servers[i] = servers[i].trim();
        }
        return servers;
    }

    public static String getExpectedHash() {
        return properties.getProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
    }

    public static String getFilePath() {
        return properties.getProperty("file_path", "test.txt");
    }

    // FIXED: Default an toàn nếu servers rỗng
    public static String getMasterServer() {
        String[] servers = getServers();
        String defaultMaster = servers.length > 0 ? servers[0] : "localhost:12347";
        return properties.getProperty("master_server", defaultMaster);
    }

    public static String getDbPath() {
        return properties.getProperty("db_path", "files.db");
    }
}