/*
 * < ProjectZomboidServerMannger - Project Zomboid server manage software >
 *     Copyright (C) 2022-2024 chiliasmstudio
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

package com.chiliasmstudio.ProjectZomboidServeManager;

import lombok.Getter;
import lombok.Setter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig {
    /**
     * Load config value form config.properties.
     *
     * @param configFileDir Directory of config.
     * @throws IOException When fail to load config.
     * @throws Exception   When required argument is invalid.
     */
    public void LoadConfig(String configFileDir) throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream(configFileDir));

        //-------- Basic -------
        ServerName = properties.getProperty("ServerName", "");
        if (ServerName == null || ServerName.isEmpty())
            throw new Exception("ServerName not found!");

        SteamCollections = Long.valueOf(properties.getProperty("SteamCollections", ""));
        if (SteamCollections < 0)
            throw new Exception("SteamCollections not found!");

        CheckFrequency = Long.valueOf(properties.getProperty("CheckFrequency", ""));
        if (CheckFrequency <= 0)
            throw new Exception("CheckFrequency not found!");
        //else if (CheckFrequency < 60)

        //-------- Rcon --------
        RconIP = properties.getProperty("RconIP", "");
        if (RconIP == null || RconIP.isEmpty())
            throw new Exception("ServerIP not found!");

        RconPort = Integer.parseInt(properties.getProperty("RconPort", ""));
        if (RconPort < 0 || RconPort > 65535)
            throw new Exception("RconPort not found!");

        RconPassword = properties.getProperty("RconPassword", "");
        if (RconPassword == null || RconPassword.isEmpty())
            throw new Exception("RconPassword not found!");

        //-------- Discord --------
        DiscordChannel = Long.valueOf(properties.getProperty("DiscordChannel", ""));

        //-------- Debug --------
        debugSkipRestartDelay = Boolean.parseBoolean(
                properties.getProperty("debugSkipRestartDelay", "false"));
    }


    //-------- Basic --------
    /**
     * Server name.
     */
    @Getter
    private String ServerName = "";

    /**
     * Steam collections id.
     */
    @Getter
    private Long SteamCollections = -1L;

    /**
     * CheckFrequency.
     */
    @Getter
    private Long CheckFrequency = -1L;

    //-------- Rcon --------
    /**
     * Server rcon ip.
     */
    @Getter
    private String RconIP = "";

    /**
     * Server rcon port.
     */
    @Getter
    private int RconPort = -1;

    /**
     * Server rcon password.
     */
    @Getter
    private String RconPassword = "";

    //-------- Discord --------
    /**
     * Discord Channel that send notify. (optional).
     */
    @Getter
    private Long DiscordChannel = 0L;

    //-------- Debug --------
    /**
     * Debug flag to skip long restart countdown delay (5m, 1m, 30s, 10s)
     * and restart the server directly after 10 seconds.
     */
    @Getter
    @Setter
    private boolean debugSkipRestartDelay = false;
}