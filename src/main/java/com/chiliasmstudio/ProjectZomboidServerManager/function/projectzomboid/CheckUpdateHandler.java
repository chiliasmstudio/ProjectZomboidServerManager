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
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Discord.RestartEmbed;
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Rcon.RconCommandHandler;
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Steam.SteamAPI;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckUpdateHandler implements Runnable {
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
        updateState = new UpdateState(Instant.now().getEpochSecond());

        serverLogger.debug("Server config dir: " + configDir);
        serverLogger.info("Server config loaded");
    }

    /**
     * 由排程器依 CheckFrequency 週期呼叫。
     * 必須攔截所有例外，否則 ScheduledExecutorService 會停止後續排程。
     */
    @Override
    public void run() {
        try {
            checkOnce();
            serverLogger.debug("Check end, next check in " + serverConfig.getCheckFrequency() + " second");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            serverLogger.error("Unexpected error on check.", e);
        }
    }

    /**
     * 執行一次更新檢查，若有模組更新則倒數並關閉伺服器。
     * 只有在重啟流程完成後才會更新記錄，因此途中失敗時下一輪會重新偵測到同一批更新。
     * Steam 請求失敗時會每 30 秒重試，共重試 3 次，仍失敗則略過本輪等待下次檢查。
     * 個別模組查詢失敗 (result != 1) 時只略過該模組。
     */
    private void checkOnce() throws InterruptedException {
        long now = Instant.now().getEpochSecond();
        serverLogger.info(formattedDate(now) + " (" + now + ") start check.");

        ArrayList<Long> collectionItems = SteamAPI.getCollectionDetail(serverConfig.getSteamCollections());
        if (collectionItems == null) {
            serverLogger.error("Failed to get collection list, wait for next check.");
            return;
        }

        JSONArray itemList = SteamAPI.getPublishedFileDetails(collectionItems);
        if (itemList == null) {
            serverLogger.error("Failed to get workshop item details, wait for next check.");
            return;
        }
        logSkippedItems(collectionItems, itemList);

        List<JSONObject> updateList = updateState.findUpdatedItems(itemList);
        if (updateList.isEmpty()) {
            updateState.record(itemList);
            return;
        }

        for (JSONObject item : updateList)
            serverLogger.info("Need update: " + item.optString("title"));

        restartServer(updateList);

        updateState.record(itemList);
    }

    /**
     * 記錄本輪被略過的模組 (查詢失敗、已下架或設為私人)。
     */
    private void logSkippedItems(List<Long> collectionItems, JSONArray itemList) {
        Set<Long> validIds = new HashSet<>();
        for (int i = 0; i < itemList.length(); i++) {
            JSONObject item = itemList.optJSONObject(i);
            if (UpdateState.isValid(item))
                validIds.add(item.optLong("publishedfileid"));
        }
        List<Long> skipped = collectionItems.stream().filter(id -> !validIds.contains(id)).toList();
        if (!skipped.isEmpty())
            serverLogger.warn(skipped.size() + "/" + collectionItems.size() + " items skipped this check: " + skipped);
    }

    /**
     * 透過 Rcon 廣播倒數後關閉伺服器。
     */
    private void restartServer(List<JSONObject> updateList) throws InterruptedException {
        long channel = serverConfig.getDiscordChannel();
        boolean debugMode = serverConfig.isDebugSkipRestartDelay();
        List<RestartEmbed.Mod> mods = updateList.stream()
                .map(item -> new RestartEmbed.Mod(item.optString("publishedfileid"), item.optString("title")))
                .toList();
        DiscordNotifier.sendEmbed(channel, RestartEmbed.build(
                serverConfig.getServerName(), debugMode ? "10 秒" : "10 分鐘", debugMode, mods, null));

        rconCommandHandler = new RconCommandHandler(serverConfig);
        broadcast("Server need reboot! restart in 10 minute.");

        // 除錯模式下略過長倒數，10 秒後直接重啟
        if (debugMode) {
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

        DiscordNotifier.send(channel, serverConfig.getServerName() + " rebooting!");
        serverLogger.info("Stopping server.");
        if (rconCommandHandler.send("quit")) {
            DiscordNotifier.send(channel, serverConfig.getServerName() + " has stop.");
            serverLogger.info("Server has stop.");
        } else {
            serverLogger.error("Failed to send quit via rcon.");
        }
        rconCommandHandler.disConnect();
        // 等待伺服器完全關閉，再進行下一次檢查
        Thread.sleep(60 * 1000L);
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
