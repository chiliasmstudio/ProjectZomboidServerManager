/*
 * < ProjectZomboidServerMannger - Project Zomboid server manage software >
 *     Copyright (C) 2022-2026 chiliasmstudio
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.chiliasmstudio.ProjectZomboidServerManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    /**
     * 主設定檔載入時使用的 Properties，保留下來供查詢指令開關。
     */
    private static Properties properties = new Properties();

    /**
     * 從 config.properties 載入主設定。
     * 若檔案不存在，會先從 JAR 內複製範本。
     *
     * @param configFile 主設定檔路徑。
     * @throws IOException     讀取或複製設定檔失敗時。
     * @throws ConfigException 必要設定值缺漏或格式錯誤時。
     */
    public static void loadConfig(Path configFile) throws IOException {
        copyResourceIfMissing("config/config-example.properties", configFile);

        properties = readProperties(configFile);
        discordToken = requireString(properties, "DiscordToken");
        steamKey = requireString(properties, "SteamKey");
    }

    /**
     * 查詢某個 Discord 指令是否啟用，鍵名即為指令類別名稱 (例如 LuCat = true)。
     * 未設定時預設為停用。
     *
     * @param commandName 指令類別名稱。
     * @return 是否啟用。
     */
    public static boolean isCommandEnabled(String commandName) {
        return Boolean.parseBoolean(properties.getProperty(commandName, "false").trim());
    }

    /**
     * 若目標檔案不存在，從 JAR 資源複製一份。
     *
     * @param resourcePath JAR 內資源路徑。
     * @param target       目標檔案路徑。
     */
    public static void copyResourceIfMissing(String resourcePath, Path target) throws IOException {
        if (Files.exists(target))
            return;
        try (InputStream resourceStream = Config.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resourceStream == null)
                throw new FileNotFoundException("Resource " + resourcePath + " not found in JAR!");
            if (target.getParent() != null)
                Files.createDirectories(target.getParent());
            Files.copy(resourceStream, target);
        }
    }

    /**
     * 以 UTF-8 讀取 properties 檔，避免中文值變成亂碼。
     */
    public static Properties readProperties(Path file) throws IOException {
        Properties result = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            result.load(reader);
        }
        return result;
    }

    /**
     * 讀取必要的字串設定，缺漏時拋出包含鍵名的例外。
     */
    public static String requireString(Properties props, String key) {
        String value = props.getProperty(key, "").trim();
        if (value.isEmpty())
            throw new ConfigException("Required config \"" + key + "\" is missing or empty!");
        return value;
    }

    /**
     * 讀取必要的整數設定，缺漏或格式錯誤時拋出包含鍵名的例外。
     */
    public static long requireLong(Properties props, String key) {
        String value = requireString(props, key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ConfigException("Config \"" + key + "\" must be a number, but got \"" + value + "\"!");
        }
    }

    /**
     * 讀取選填的整數設定，未設定時回傳預設值。
     */
    public static long optionalLong(Properties props, String key, long defaultValue) {
        if (props.getProperty(key, "").trim().isEmpty())
            return defaultValue;
        return requireLong(props, key);
    }

    /**
     * 設定檔內容錯誤。
     */
    public static class ConfigException extends RuntimeException {
        public ConfigException(String message) {
            super(message);
        }
    }

    /**
     * Discord Bot 的 Token。
     */
    public static String discordToken = "";

    /**
     * Steam Web API 金鑰。
     */
    public static String steamKey = "";
}
