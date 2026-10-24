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

package com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Discord;

import net.dv8tion.jda.api.entities.Message;

import java.util.concurrent.TimeUnit;

/**
 * Discord 訊息相關的工具方法。
 */
public class Utils {

    /**
     * 工具類別，不需要建立實例。
     */
    private Utils() {
    }

    /**
     * 在指定秒數後刪除訊息。
     *
     * @param message 要刪除的訊息。
     * @param delay   刪除前等待的秒數。
     */
    public static void deleteAfter(Message message, int delay) {
        message.delete().queueAfter(delay, TimeUnit.SECONDS);
    }
}
