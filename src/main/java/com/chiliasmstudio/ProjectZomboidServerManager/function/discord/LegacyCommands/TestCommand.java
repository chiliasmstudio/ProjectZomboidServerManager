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

package com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.time.Instant;

public class TestCommand extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 確保這是來自伺服器的訊息，而非私訊
        if (event.isFromGuild() && !event.getAuthor().isBot()) {
            // 當用戶輸入 "!embed" 時觸發
            if (event.getMessage().getContentRaw().equals("!embed")) {
                // 創建 EmbedBuilder
                EmbedBuilder embed = new EmbedBuilder();

                // 設定標題、描述、顏色
                embed.setTitle("這是一個嵌入範例");
                embed.setDescription("這是嵌入消息的描述部分。");
                embed.setColor(Color.BLUE);

                // 添加欄位
                embed.addField("欄位 1", "這是欄位 1 的內容", false);
                embed.addField("欄位 2", "這是欄位 2 的內容", false);

                // 設定 footer 和時間戳
                embed.setFooter("這是 Footer");
                embed.setTimestamp(Instant.now());

                // 發送嵌入消息
                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            }
        }
    }
}
