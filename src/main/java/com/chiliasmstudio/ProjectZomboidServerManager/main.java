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

import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.MainBot;
import com.chiliasmstudio.ProjectZomboidServerManager.function.projectzomboid.CheckUpdateHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 程式進入點：載入設定、啟動 Discord Bot，並為每台伺服器排程更新檢查。
 */
public class main {
    /**
     * 程式進入點類別，不需要建立實例。
     */
    private main() {
    }

    /**
     * 主程式的 logger。
     */
    private static final Logger mainLogger = LogManager.getLogger("main");

    /**
     * 設定檔目錄。
     */
    private static final Path CONFIG_DIR = Path.of("config");

    /**
     * 伺服器設定檔目錄。
     */
    private static final Path SERVERS_DIR = CONFIG_DIR.resolve("servers");

    /**
     * 伺服器設定範本檔名，載入時會被略過。
     */
    private static final String SERVER_EXAMPLE = "server_example.properties";

    /**
     * 程式進入點。
     *
     * @param args 啟動參數，支援 -debug 與 -trace 調整 log 等級。
     * @throws Exception 設定載入失敗或等待 Bot 就緒時被中斷。
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            // 依啟動參數設定 log 等級
            if ("-debug".equalsIgnoreCase(arg)) {
                Configurator.setRootLevel(Level.DEBUG);
                break;
            } else if ("-trace".equalsIgnoreCase(arg)) {
                Configurator.setRootLevel(Level.TRACE);
                break;
            }
        }

        Config.loadConfig(CONFIG_DIR.resolve("config.properties"));
        if (!MainBot.initialize(Config.discordToken)) {
            mainLogger.fatal("Failed to start discord bot, exiting.");
            System.exit(1);
        }
        MainBot.jda.awaitReady();

        startUpdateCheckers();
    }

    /**
     * 讀取 config/servers 下的伺服器設定，為每台伺服器排程更新檢查。
     *
     * @throws IOException           讀取設定目錄或設定檔失敗時。
     * @throws IllegalStateException 找不到範本以外的伺服器設定檔時。
     */
    public static void startUpdateCheckers() throws IOException {
        Config.copyResourceIfMissing("config/servers/" + SERVER_EXAMPLE, SERVERS_DIR.resolve(SERVER_EXAMPLE));

        List<String> configFiles;
        try (Stream<Path> files = Files.list(SERVERS_DIR)) {
            configFiles = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".properties") && !name.equals(SERVER_EXAMPLE))
                    .sorted()
                    .toList();
        }
        if (configFiles.isEmpty())
            throw new IllegalStateException("No server config files found other than " + SERVER_EXAMPLE + "!");

        List<CheckUpdateHandler> handlers = new ArrayList<>();
        for (String configFile : configFiles) {
            mainLogger.info("find server config: " + configFile);
            handlers.add(new CheckUpdateHandler(configFile));
        }

        // 每台伺服器一條執行緒，上一輪 (含重啟倒數) 結束後才開始計算下一輪間隔
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(handlers.size());
        for (int i = 0; i < handlers.size(); i++) {
            CheckUpdateHandler handler = handlers.get(i);
            // 錯開各伺服器的首次檢查，避免同時呼叫 Steam API
            scheduler.scheduleWithFixedDelay(handler, i * 10L,
                    handler.getServerConfig().getCheckFrequency(), TimeUnit.SECONDS);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mainLogger.info("Shutting down.");
            scheduler.shutdownNow();
            if (MainBot.jda != null)
                MainBot.jda.shutdown();
        }));
    }
}
