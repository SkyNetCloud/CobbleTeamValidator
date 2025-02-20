package ca.skynetcloud.cobbleteamvalidator.config;

import ca.skynetcloud.cobbleteamvalidator.CobbleTeamValidator;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FormatConfig {

    private static final String SHOWDOWN_URL = "https://play.pokemonshowdown.com/data/formats.js";
    private static final File FORMATS_FILE = new File("config/cobblevalidator/formats.json");
    public static JsonObject formatsData;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void fetchFormats() {
        try {
            String rawData = downloadFromUrl();
            if (rawData == null) {
                LogUtils.getLogger().error("Failed to download format data.");
                return;
            }

            String jsonData = rawData.replaceFirst("exports\\.Formats =", "").trim();
            if (jsonData.endsWith(";")) {
                jsonData = jsonData.substring(0, jsonData.length() - 1);
            }

            JsonArray formatsArray = JsonParser.parseString(jsonData).getAsJsonArray();
            JsonObject formattedData = formatToJsonObject(formatsArray);

            saveConfig(formattedData);
            formatsData = formattedData;

            LogUtils.getLogger().info("Pokémon Showdown format rules updated!");

        } catch (Exception e) {
            LogUtils.getLogger().error("Error fetching formats: " + e.getMessage());
        }
    }

    private static @NotNull JsonObject formatToJsonObject(JsonArray formatsArray) {
        JsonObject formattedData = new JsonObject();

        for (JsonElement element : formatsArray) {
            JsonObject formatObj = element.getAsJsonObject();
            String formatName = getFormatName(formatObj);
            String originalName = formatObj.has("name") ? formatObj.get("name").getAsString() : "unknown_format";
            JsonObject formatData = extractFormatData(formatObj);

            formatData.addProperty("original", originalName);
            formatData.addProperty("generation", formatObj.has("mod") ? formatObj.get("mod").getAsString() : "unknown");

            formattedData.add(formatName, formatData);
        }
        return formattedData;
    }

    private static String getFormatName(JsonObject formatObj) {
        if (formatObj.has("name") && !formatObj.get("name").isJsonNull()) {
            return normalizeFormatName(formatObj.get("name").getAsString());
        }
        return "unknown_format";
    }

    private static JsonObject extractFormatData(JsonObject formatObj) {
        JsonObject formatData = new JsonObject();
        JsonArray bannedPokemons = extractBannedPokemons(formatObj);
        JsonArray rules = extractRules(formatObj);

        formatData.add("banned_pokemon", bannedPokemons);
        formatData.add("rules", rules);
        return formatData;
    }

    private static JsonArray extractBannedPokemons(JsonObject formatObj) {
        JsonArray bannedList = formatObj.has("banlist") ? formatObj.getAsJsonArray("banlist") : new JsonArray();
        JsonArray bannedPokemons = new JsonArray();

        for (JsonElement bannedPokemon : bannedList) {
            bannedPokemons.add(bannedPokemon.getAsString().toLowerCase());
        }

        return bannedPokemons;
    }

    private static JsonArray extractRules(JsonObject formatObj) {
        return formatObj.has("ruleset") ? formatObj.getAsJsonArray("ruleset") : new JsonArray();
    }

    private static String normalizeFormatName(String formatName) {
        return formatName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private static String downloadFromUrl() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(FormatConfig.SHOWDOWN_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }

    private static void saveConfig(JsonObject data) {
        try {
            File directory = FORMATS_FILE.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (FileWriter fileWriter = new FileWriter(FORMATS_FILE)) {
                GSON.toJson(data, fileWriter);
                fileWriter.flush();
                LogUtils.getLogger().info(CobbleTeamValidator.NAME + ": Config saved successfully.");
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to save the config!", e);
        }
    }

    public static void loadFormats() {
        if (FORMATS_FILE.exists()) {
            try (FileReader reader = new FileReader(FORMATS_FILE)) {
                formatsData = JsonParser.parseReader(reader).getAsJsonObject();
                LogUtils.getLogger().info("Formats loaded from file successfully!");
            } catch (IOException e) {
                LogUtils.getLogger().error("Failed to load formats from file: " + e.getMessage());
            }
        } else {
            LogUtils.getLogger().error("No format file found, fetching formats...");
            fetchFormats();
        }
    }
}
