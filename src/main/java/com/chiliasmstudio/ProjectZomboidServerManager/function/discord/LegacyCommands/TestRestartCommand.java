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

package com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands;

import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.SlashCommandProvider;
import com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Discord.RestartEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

/**
 * 測試用指令：以模擬資料發送重啟通知卡片，提供文字指令 !testrestart 與斜線指令 /testrestart。
 */
public class TestRestartCommand extends ListenerAdapter implements SlashCommandProvider {

    /**
     * 建立指令監聽器。
     */
    public TestRestartCommand() {
    }

    /**
     * 以模擬資料建構測試重啟卡片，版面與實際更新通知共用 RestartEmbed。
     *
     * @return 測試用的重啟通知卡片。
     */
    private MessageEmbed buildRestartEmbed() {
        List<RestartEmbed.Mod> mods = List.of(
                new RestartEmbed.Mod("2778991696", "Hydrocraft for v41"),
                new RestartEmbed.Mod("2200148440", "Brita's Weapon Pack"),
                new RestartEmbed.Mod("2335368829", "Authentic Z - Current"));
        return RestartEmbed.build("Noble TestNet 1", "10 分鐘", false, mods,
                "此訊息為測試指令觸發產生的模擬重啟卡片。");
    }

    /**
     * 處理傳統文字指令 !testrestart。
     *
     * @param event 收到訊息的事件。
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromGuild() && !event.getAuthor().isBot()) {
            String content = event.getMessage().getContentRaw().trim();
            if (content.equalsIgnoreCase("!testrestart")) {
                event.getChannel().sendMessageEmbeds(buildRestartEmbed()).queue();
            }
        }
    }

    /**
     * 處理斜線指令 /testrestart。
     *
     * @param event 斜線指令事件。
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("testrestart")) {
            event.replyEmbeds(buildRestartEmbed()).queue();
        }
    }

    /**
     * 取得 /testrestart 的斜線指令定義。
     *
     * @return 斜線指令定義。
     */
    @Override
    public SlashCommandData getSlashCommand() {
        return Commands.slash("testrestart", "發送測試重啟通知 Embed 卡片");
    }
}
