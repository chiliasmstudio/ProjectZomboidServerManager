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

package com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Discord;

import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.MainBot;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 發送 Discord 通知。頻道未設定 (0) 或找不到時只記錄 log，不會拋出例外中斷重啟流程。
 */
public class DiscordNotifier {
    /**
     * 工具類別，不需要建立實例。
     */
    private DiscordNotifier() {
    }

    /**
     * 通知相關的 logger。
     */
    private static final Logger notifierLogger = LogManager.getLogger("DiscordNotifier");

    /**
     * 發送文字訊息。
     *
     * @param channelId 頻道 ID，0 表示不發送。
     * @param message   訊息內容。
     */
    public static void send(long channelId, String message) {
        TextChannel channel = findChannel(channelId);
        if (channel != null)
            channel.sendMessage(message).queue(null,
                    e -> notifierLogger.warn("Failed to send discord message to " + channelId, e));
    }

    /**
     * 發送 Embed 卡片。
     *
     * @param channelId 頻道 ID，0 表示不發送。
     * @param embed     要發送的卡片。
     */
    public static void sendEmbed(long channelId, MessageEmbed embed) {
        TextChannel channel = findChannel(channelId);
        if (channel != null)
            channel.sendMessageEmbeds(embed).queue(null,
                    e -> notifierLogger.warn("Failed to send discord embed to " + channelId, e));
    }

    /**
     * 取得文字頻道。
     *
     * @param channelId 頻道 ID。
     * @return 文字頻道；未設定、Bot 尚未啟動或找不到時回傳 null。
     */
    private static TextChannel findChannel(long channelId) {
        if (channelId == 0L || MainBot.jda == null)
            return null;
        TextChannel channel = MainBot.jda.getTextChannelById(channelId);
        if (channel == null)
            notifierLogger.warn("Discord channel " + channelId + " not found, notification skipped.");
        return channel;
    }
}
