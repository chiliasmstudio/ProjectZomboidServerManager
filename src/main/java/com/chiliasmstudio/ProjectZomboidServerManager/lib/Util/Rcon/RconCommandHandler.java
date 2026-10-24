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

package com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Rcon;

import com.chiliasmstudio.ProjectZomboidServerManager.ServerConfig;
import net.kronos.rkon.core.Rcon;

/**
 * 包裝單一伺服器的 Rcon 連線。
 * 送出失敗時會嘗試重新連線一次，避免長時間倒數期間連線逾時。
 */
public class RconCommandHandler {
    /**
     * 提供 Rcon 連線資訊的伺服器設定。
     */
    private final ServerConfig serverConfig;

    /**
     * 目前的 Rcon 連線，尚未連線或已斷線時為 null。
     */
    private Rcon rcon;

    /**
     * 建立 Rcon 連線包裝，此時尚未連線。
     *
     * @param serverConfig 提供 Rcon 連線資訊的伺服器設定。
     */
    public RconCommandHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 建立 Rcon 連線並完成驗證。
     *
     * @throws Exception 無法連線或驗證失敗時。
     */
    public void connect() throws Exception {
        rcon = new Rcon(serverConfig.getRconIP(), serverConfig.getRconPort(), serverConfig.getRconPassword().getBytes());
    }

    /**
     * 中斷 Rcon 連線。
     *
     * @return 是否正常中斷 (尚未連線時也回傳 true)。
     */
    public boolean disConnect() {
        if (rcon == null)
            return true;
        try {
            rcon.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            rcon = null;
        }
    }

    /**
     * 在遊戲內廣播訊息。訊息中的雙引號會被替換，避免截斷指令。
     *
     * @param message 要廣播的訊息。
     * @return 是否成功送出。
     */
    public boolean sendMessage(String message) {
        return send("servermsg \"" + message.replace("\"", "'") + "\"");
    }

    /**
     * 送出 Rcon 指令，失敗時重新連線後再試一次。
     *
     * @param command 要送出的指令。
     * @return 是否成功送出。
     */
    public boolean send(String command) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                if (rcon == null)
                    connect();
                rcon.command(command);
                return true;
            } catch (Exception e) {
                disConnect();
            }
        }
        return false;
    }
}
