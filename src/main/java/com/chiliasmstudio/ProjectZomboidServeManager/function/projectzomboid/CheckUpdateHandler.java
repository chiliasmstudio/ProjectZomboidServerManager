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

package com.chiliasmstudio.ProjectZomboidServeManager.function.projectzomboid;

import com.chiliasmstudio.ProjectZomboidServeManager.ServerConfig;
import com.chiliasmstudio.ProjectZomboidServeManager.lib.Util.Rcon.RconCommandHandler;
import com.chiliasmstudio.ProjectZomboidServeManager.lib.Util.Steam.SteamAPI;
import com.chiliasmstudio.ProjectZomboidServeManager.function.discord.MainBot;
import lombok.Getter;
import lombok.Setter;
import net.kronos.rkon.core.Rcon;
import net.kronos.rkon.core.ex.AuthenticationException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class CheckUpdateHandler extends Thread {
    // Last check time.
    @Getter @Setter
    private long unixTimestamp = 0L;
    // Server config file.
    @Getter
    private final ServerConfig serverConfig = new ServerConfig();
    private final Logger serverLogger;
    private RconCommandHandler rconCommandHandler;
    @Getter
    private String configDir;

    public CheckUpdateHandler(String configDir) throws Exception {
        serverConfig.LoadConfig("config//servers//" + configDir);
        serverLogger = LogManager.getLogger(serverConfig.getServerName());
        this.configDir = configDir;
        serverLogger.debug("Server config dir: " + configDir);
        serverLogger.info("Server config loaded");
    }

    public void run() {
        try {
            // Try rcon connect.
            rconCommandHandler = new RconCommandHandler(serverConfig);
            try {
                rconCommandHandler.connect();
            } catch (IOException e) {
                // Unable to connect to the server
                e.printStackTrace();//TODO Log4j
                return;
            } catch (AuthenticationException e) {
                // Authentication failed
                e.printStackTrace();//TODO Log4j
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            serverLogger.info("Rcon ok.");
            //unixTimestamp = Instant.now().getEpochSecond();
                //Check update
                JSONArray updateList = new JSONArray();
                JSONArray itemList = null;

                try {
                    serverLogger.info(formattedDate(Instant.now().getEpochSecond()) + " (" + Instant.now().getEpochSecond() + ")" + " start check.");
                    itemList = SteamAPI.GetPublishedFileDetails(SteamAPI.GetCollectionDetail(serverConfig.getSteamCollections()));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                // Foreach workshop item.
                boolean needRestart = false;
                int error = 0;
                if (itemList != null) {
                    for (int i = 0; i < itemList.length(); i++) {
                        try {
                            JSONObject item = itemList.getJSONObject(i);
                            if (item.getLong("time_updated") > unixTimestamp) {
                                serverLogger.info("Need update: " + item.get("title"));
                                updateList.put(item);
                                needRestart = true;
                            }
                        } catch (JSONException e) {
                            error++;
                        }
                    }
                } else serverLogger.error("Error on check.");

                serverLogger.info(error + " items error on check.");
                unixTimestamp = Instant.now().getEpochSecond();


                // Restart server.
                if (needRestart) {

                    //TODO WAIL FOR REPLACE
                    MainBot.bot_Main.getTextChannelById(serverConfig.getDiscordChannel()).sendMessage(serverConfig.getServerName() + " need reboot! restart in 10 minute.").queue();

                    MainBot.bot_Main.getTextChannelById(serverConfig.getDiscordChannel()).sendMessage("Mod to update:").queue();
                    for (int i = 0; i < updateList.length(); i++) {
                        JSONObject element = updateList.getJSONObject(i);
                        String message = "https://steamcommunity.com/sharedfiles/filedetails/?id=" + element.get("publishedfileid");
                        MainBot.bot_Main.getTextChannelById(serverConfig.getDiscordChannel()).sendMessage("<" + message + ">").setEmbeds(Collections.emptyList()).queue();
                    }


                    rconCommandHandler.sendMessage("Server need reboot! restart in 10 minute.");
                    serverLogger.info("Server need reboot! restart in 10 minute.");
                    //TODO DISCORD MESSAGE


                    // If debug flag is set, skip the long countdown and reboot after 10 seconds
                    if (serverConfig.isDebugSkipRestartDelay()) {
                        rconCommandHandler.sendMessage("Server need reboot! restart in 10 seconds (debug mode).");
                        serverLogger.info("Server need reboot! restart in 10 seconds (debug mode).");
                        Thread.sleep(10 * 1000L);
                    } else {
                        //Thread.sleep(2 * 1000L);
                        Thread.sleep(300 * 1000L);// 5 minute.
                        rconCommandHandler.sendMessage("Server need reboot! restart in 5 minute.");
                        serverLogger.info("Server need reboot! restart in 5 minute.");

                        //Thread.sleep(2 * 1000L);
                        Thread.sleep(240 * 1000L);// 1 minute.
                        rconCommandHandler.sendMessage("Server need reboot! restart in 1 minute.");
                        serverLogger.info("Server need reboot! restart in 1 minute.");

                        //Thread.sleep(2 * 1000L);
                        Thread.sleep(30 * 1000L);// 30 second.
                        rconCommandHandler.sendMessage("Server need reboot! restart in 30 second.");
                        serverLogger.info("Server need reboot! restart in 30 second.");

                        //Thread.sleep(2 * 1000L);
                        Thread.sleep(20 * 1000L);// 10 second.
                        rconCommandHandler.sendMessage("Server need reboot! restart in 10 second.");
                        serverLogger.info("Server need reboot! restart in 10 second.");
                    }
                    MainBot.bot_Main.getTextChannelById(serverConfig.getDiscordChannel()).sendMessage(serverConfig.getServerName() + " rebooting!").queue();
                    serverLogger.info("Stopping server.");

                    closeServer();
                    Thread.sleep(60 * 1000L);
                    serverLogger.info("Server has stop.");

                    // Try to boot server.
                    //if (!startServer())
                    //    throw new RuntimeException();
                    //SendLog("Boot ok.");

                    // Wait until server is start.
                    //Thread.sleep(serverConfig.getRestartTime() * 1000L);

                    // Try rcon connect.
                    //rconCommandHandler = new RconCommandHandler(serverConfig);
                    //if(!rconCommandHandler.connect())
                    //    throw new RuntimeException();
                    //SendLog("Rcon ok.");
                }
                rconCommandHandler.disConnect();
                serverLogger.debug("Check end, waiting "+serverConfig.getCheckFrequency()+" second");
                Thread.sleep(serverConfig.getCheckFrequency() * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);//TODO Log4j
        }
    }

    /**
     * Start the server and send Discord notification
     *
     * @return true if server is started successfully, false otherwise
     */
    private boolean startServer() {
        try {
            ProcessBuilder server = null;
            if (SystemUtils.IS_OS_WINDOWS) {
                throw new RuntimeException("Error, we not support mac yet");
                //server = new ProcessBuilder("cmd", "/c start " + serverConfig.getServerStartupScrip()).directory(new File(serverConfig.getServerDirectory()));
            } else if (SystemUtils.IS_OS_MAC) {
                throw new RuntimeException("Error, we not support mac yet");
            } else if (SystemUtils.IS_OS_LINUX) {
                //server = new ProcessBuilder("xterm", "-e", "sh your_script.sh").directory(new File(serverConfig.getServerDirectory()));
            }

            Process p = server.start();
            MainBot.bot_Main.getTextChannelById(serverConfig.getDiscordChannel()).sendMessage(serverConfig.getServerName() + " is booting now.").queue();
            return true;
        } catch (IOException e) {
            e.printStackTrace();//TODO Log4j
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Sends a "quit" command to the server via Rcon and disconnects.
     * Returns true if successful, false if there was an error.
     *
     * @return true if the quit command was successfully sent, false otherwise
     */
    private boolean closeServer() {
        try {
            Rcon rcon = new Rcon(serverConfig.getRconIP(), serverConfig.getRconPort(), serverConfig.getRconPassword().getBytes());
            rcon.command("quit");
            rcon.disconnect();
            MainBot.bot_Main.getTextChannelById(serverConfig.getDiscordChannel()).sendMessage(serverConfig.getServerName() + " has stop.").queue();
            return true;
        } catch (IOException e) {
            // Unable to connect to the server
            e.printStackTrace();//TODO Log4j
            return false;
        } catch (AuthenticationException e) {
            // Authentication failed
            e.printStackTrace();//TODO Log4j
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Returns a formatted date string with the timezone set in serverConfig.
     *
     * @param unixTimestamp the Unix timestamp to format
     * @return the formatted date string
     */
    private String formattedDate(Long unixTimestamp) {
        ZoneId zoneId;
        //if (serverConfig.getTimeZone().equalsIgnoreCase("Auto"))
        zoneId = ZoneId.systemDefault();
        //else
        //    zoneId = ZoneId.of(serverConfig.getTimeZone());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(zoneId);
        Instant instant = Instant.ofEpochSecond(unixTimestamp);
        return formatter.format(instant);
    }

    /**
     * Send Message to console.
     *
     * @param obj Message to send.
     */
    private void SendLog(Object obj) {
        System.out.println("[" + serverConfig.getServerName() + "] " + obj);
    }
}
