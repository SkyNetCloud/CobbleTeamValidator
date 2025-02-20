package ca.skynetcloud.cobbleteamvalidator.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FormatConfig {

    private static final String NAME = "CobbleTeamValidator";
    private static final String CONFIG_PATH = "config/cobbleteamvalidator/formats.json";
    private static final Gson GSON = new Gson();
    private FormatConfigData config;

    public void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        configFile.getParentFile().mkdirs();

        if (configFile.exists()) {
            System.out.println(NAME + ": Config file exists. Loading it.");

            try (FileReader fileReader = new FileReader(configFile)) {
                config = GSON.fromJson(fileReader, FormatConfigData.class);

                if (config == null) {
                    System.err.println("Loaded config is null. Resetting to default config.");
                    config = new FormatConfigData();  // Fallback to default config
                }
            } catch (JsonSyntaxException e) {
                System.err.println("Error reading config file. Using default config.");
                e.printStackTrace();
                config = new FormatConfigData();  // Fallback to default config
            } catch (IOException e) {
                System.err.println("Error reading the config file.");
                e.printStackTrace();
                config = new FormatConfigData();  // Fallback to default config
            }
        } else {
            System.out.println(NAME + ": Config file not found. Creating a new one with default settings.");
            config = new FormatConfigData();  // Create new default config
        }

        saveConfig();  // Save the config if it was fresh or modified
    }

    private void saveConfig() {
        try (FileWriter fileWriter = new FileWriter(CONFIG_PATH)) {
            GSON.toJson(config, fileWriter);
            fileWriter.flush();
            System.out.println(NAME + ": Config saved successfully.");
        } catch (IOException e) {
            System.err.println("Failed to save the config!");
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        System.out.println("Reloading config");
        loadConfig();
    }

    // Placeholder for your FormatConfigData class
    public static class FormatConfigData {

    }
}
