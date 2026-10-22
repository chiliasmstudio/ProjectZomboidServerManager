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

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Properties;

import static com.chiliasmstudio.ProjectZomboidServerManager.Config.*;

@Getter
public class ServerConfig {
    /**
     * 從伺服器設定檔載入設定值。
     *
     * @param configFile 伺服器設定檔路徑。
     * @throws IOException            讀取失敗時。
     * @throws Config.ConfigException 必要設定值缺漏或格式錯誤時。
     */
    public void loadConfig(Path configFile) throws IOException {
        Properties properties = readProperties(configFile);

        //-------- Basic -------
        serverName = requireString(properties, "ServerName");

        steamCollections = requireLong(properties, "SteamCollections");
        if (steamCollections <= 0)
            throw new ConfigException("SteamCollections must be a positive workshop id!");

        checkFrequency = requireLong(properties, "CheckFrequency");
        if (checkFrequency <= 0)
            throw new ConfigException("CheckFrequency must be greater than 0!");

        String timeZoneValue = properties.getProperty("TimeZone", "auto").trim();
        try {
            timeZone = timeZoneValue.isEmpty() || timeZoneValue.equalsIgnoreCase("auto")
                    ? ZoneId.systemDefault()
                    : ZoneId.of(timeZoneValue);
        } catch (DateTimeException e) {
            throw new ConfigException("TimeZone \"" + timeZoneValue + "\" is not a valid TZ database name!");
        }

        //-------- Rcon --------
        rconIP = requireString(properties, "RconIP");

        long port = requireLong(properties, "RconPort");
        if (port < 1 || port > 65535)
            throw new ConfigException("RconPort must be between 1 and 65535!");
        rconPort = (int) port;

        rconPassword = requireString(properties, "RconPassword");

        //-------- Discord --------
        discordChannel = optionalLong(properties, "DiscordChannel", 0L);

        //-------- Debug --------
        debugSkipRestartDelay = Boolean.parseBoolean(
                properties.getProperty("debugSkipRestartDelay", "false").trim());
    }


    //-------- Basic --------
    /**
     * 伺服器名稱。
     */
    private String serverName = "";

    /**
     * Steam 工作坊合集 ID。
     */
    private long steamCollections = -1L;

    /**
     * 檢查更新的間隔 (秒)。
     */
    private long checkFrequency = -1L;

    /**
     * 主控台與 Discord 顯示時間所使用的時區。
     */
    private ZoneId timeZone = ZoneId.systemDefault();

    //-------- Rcon --------
    /**
     * 伺服器 Rcon IP。
     */
    private String rconIP = "";

    /**
     * 伺服器 Rcon 連接埠。
     */
    private int rconPort = -1;

    /**
     * 伺服器 Rcon 密碼。
     */
    private String rconPassword = "";

    //-------- Discord --------
    /**
     * 發送通知的 Discord 頻道 (選填，0 表示不發送)。
     */
    private long discordChannel = 0L;

    //-------- Debug --------
    /**
     * 除錯用：略過 5 分鐘、1 分鐘、30 秒、10 秒的重啟倒數，
     * 直接在 10 秒後重啟伺服器。
     */
    private boolean debugSkipRestartDelay = false;
}
