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

package com.chiliasmstudio.ProjectZomboidServerManager.function.projectzomboid;

import com.chiliasmstudio.ProjectZomboidServerManager.ServerConfig;
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Discord.DiscordNotifier;
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Rcon.RconCommandHandler;
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Steam.SteamAPI;
import lombok.Getter;
import net.kronos.rkon.core.Rcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CheckUpdateHandler extends Thread {
    // 伺服器設定。
    @Getter
    private final ServerConfig serverConfig = new ServerConfig();
    private final Logger serverLogger;
    private final UpdateState updateState;
    private RconCommandHandler rconCommandHandler;
    @Getter
    private final String configDir;

    public CheckUpdateHandler(String configDir) throws IOException {
        serverConfig.loadConfig(Path.of("config", "servers", configDir));
        serverLogger = LogManager.getLogger(serverConfig.getServerName());
        this.configDir = configDir;

        String stateName = configDir.replaceFirst("\\.properties$", "") + ".json";
        updateState = new UpdateState(Path.of("data", stateName));
        updateState.load();

        serverLogger.debug("Server config dir: " + configDir);
        serverLogger.info("Server config loaded");
    }

    public void run() {
        try {
            checkOnce();
            serverLogger.debug("Check end, waiting " + serverConfig.getCheckFrequency() + " second");
            Thread.sleep(serverConfig.getCheckFrequency() * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            serverLogger.error("Unexpected error on check.", e);
        }
    }

    /**
     * 執行一次更新檢查，若有模組更新則倒數並關閉伺服器。
     * 只有在查詢成功、且重啟流程完成後才會更新並保存記錄，
     * 因此途中失敗時下一輪會重新偵測到同一批更新。
     */
    private void checkOnce() throws InterruptedException, IOException {
        long now = Instant.now().getEpochSecond();
        serverLogger.info(formattedDate(now) + " (" + now + ") start check.");

        ArrayList<Long> collectionItems = SteamAPI.getCollectionDetail(serverConfig.getSteamCollections());
        JSONArray itemList = collectionItems == null ? null : SteamAPI.getPublishedFileDetails(collectionItems);
        if (itemList == null) {
            serverLogger.error("Error on check, will retry next time.");
            return;
        }

        int invalid = UpdateState.countInvalidItems(itemList);
        if (invalid > 0)
            serverLogger.warn(invalid + " items have no update time (removed or private?).");

        if (updateState.isEmpty()) {
            // 首次執行只建立基準
            updateState.record(itemList);
            updateState.save();
            serverLogger.info("Baseline recorded for " + itemList.length() + " items.");
            return;
        }

        List<JSONObject> updateList = updateState.findUpdatedItems(itemList);
        if (updateList.isEmpty()) {
            updateState.record(itemList);
            updateState.save();
            return;
        }

        for (JSONObject item : updateList)
            serverLogger.info("Need update: " + item.optString("title"));

        restartServer(updateList);

        updateState.record(itemList);
        updateState.save();
    }

    /**
     * 透過 Rcon 廣播倒數後關閉伺服器。
     */
    private void restartServer(List<JSONObject> updateList) throws InterruptedException {
        long channel = serverConfig.getDiscordChannel();
        DiscordNotifier.send(channel, serverConfig.getServerName() + " need reboot! restart in 10 minute.");
        StringBuilder mods = new StringBuilder("Mod to update:");
        for (JSONObject item : updateList)
            mods.append("\n<https://steamcommunity.com/sharedfiles/filedetails/?id=")
                    .append(item.optString("publishedfileid")).append(">");
        DiscordNotifier.send(channel, mods.toString());

        rconCommandHandler = new RconCommandHandler(serverConfig);
        try {
            rconCommandHandler.connect();
        } catch (Exception e) {
            serverLogger.error("Unable to connect rcon, countdown message will not be shown in game.", e);
        }

        broadcast("Server need reboot! restart in 10 minute.");

        // 除錯模式下略過長倒數，10 秒後直接重啟
        if (serverConfig.isDebugSkipRestartDelay()) {
            broadcast("Server need reboot! restart in 10 seconds (debug mode).");
            Thread.sleep(10 * 1000L);
        } else {
            Thread.sleep(300 * 1000L);
            broadcast("Server need reboot! restart in 5 minute.");
            Thread.sleep(240 * 1000L);
            broadcast("Server need reboot! restart in 1 minute.");
            Thread.sleep(30 * 1000L);
            broadcast("Server need reboot! restart in 30 second.");
            Thread.sleep(20 * 1000L);
            broadcast("Server need reboot! restart in 10 second.");
            Thread.sleep(10 * 1000L);
        }
        rconCommandHandler.disConnect();

        DiscordNotifier.send(channel, serverConfig.getServerName() + " rebooting!");
        serverLogger.info("Stopping server.");
        closeServer();
        Thread.sleep(60 * 1000L);
        serverLogger.info("Server has stop.");
    }

    /**
     * 同時寫入 log 並透過 Rcon 在遊戲內廣播。
     */
    private void broadcast(String message) {
        serverLogger.info(message);
        if (!rconCommandHandler.sendMessage(message))
            serverLogger.warn("Failed to broadcast via rcon: " + message);
    }

    /**
     * 透過新的 Rcon 連線送出 quit 指令關閉伺服器。
     *
     * @return 是否成功送出。
     */
    private boolean closeServer() {
        try {
            Rcon rcon = new Rcon(serverConfig.getRconIP(), serverConfig.getRconPort(), serverConfig.getRconPassword().getBytes());
            rcon.command("quit");
            rcon.disconnect();
            DiscordNotifier.send(serverConfig.getDiscordChannel(), serverConfig.getServerName() + " has stop.");
            return true;
        } catch (Exception e) {
            serverLogger.error("Failed to send quit via rcon.", e);
            return false;
        }
    }

    /**
     * 以伺服器設定的時區格式化時間。
     *
     * @param unixTimestamp Unix 時間戳記。
     * @return 格式化後的時間字串。
     */
    private String formattedDate(long unixTimestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(serverConfig.getTimeZone());
        return formatter.format(Instant.ofEpochSecond(unixTimestamp));
    }
}
