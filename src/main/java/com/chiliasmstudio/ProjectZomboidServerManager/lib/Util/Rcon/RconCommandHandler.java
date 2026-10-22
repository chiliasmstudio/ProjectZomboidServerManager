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

package com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Rcon;

import com.chiliasmstudio.ProjectZomboidServerManager.ServerConfig;
import net.kronos.rkon.core.Rcon;

public class RconCommandHandler {

    /**
     * Sends a message to the target server via Rcon.
     * Returns true if successful, false if there was an error.
     *
     * @param message The message to be sent
     * @param serverConfig The configuration of the target server
     * @return true if the message was successfully sent, false otherwise
     */

    private ServerConfig serverConfig;
    private Rcon rcon;

    public RconCommandHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void connect() throws Exception{
        rcon = new Rcon(serverConfig.getRconIP(), serverConfig.getRconPort(), serverConfig.getRconPassword().getBytes());
    }

    public boolean disConnect() {
        try {
            rcon.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean sendMessage(String message) {
        try {
            rcon.command("servermsg \"" + message + "\"");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean send(String command) {
        try {
            rcon.command(command);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
