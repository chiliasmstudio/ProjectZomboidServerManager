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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.time.Instant;

public class TestRestartCommand extends ListenerAdapter implements SlashCommandProvider {

    /**
     * 建構測試重啟的 MessageEmbed 卡片
     */
    private MessageEmbed buildRestartEmbed() {
        EmbedBuilder embed = new EmbedBuilder();

        // 標題與側邊顏色（警告橙紅色）
        embed.setTitle("🔄 [測試] Project Zomboid 伺服器模組更新與重啟通知");
        embed.setDescription("偵測到 Steam 工作坊模組有新版本發布，伺服器即將重新啟動以套用更新。");
        embed.setColor(new Color(0xE67E22));

        // 伺服器資訊欄位
        embed.addField("伺服器名稱", "Noble TestNet 1", true);
        embed.addField("預計重啟倒數", "10 分鐘", true);
        embed.addField("除錯快速重啟", "已停用 (正常排程)", true);

        // 模擬更新的模組列表 (Markdown 超連結格式)
        String modUpdates = "• [Hydrocraft for v41](https://steamcommunity.com/sharedfiles/filedetails/?id=2778991696)\n" +
                "• [Brita's Weapon Pack](https://steamcommunity.com/sharedfiles/filedetails/?id=2200148440)\n" +
                "• [Authentic Z - Current](https://steamcommunity.com/sharedfiles/filedetails/?id=2335368829)";
        embed.addField("📦 待更新工作坊模組 (3)", modUpdates, false);

        embed.addField("ℹ️ 說明", "此訊息為測試指令觸發產生的模擬重啟卡片。", false);

        // 頁尾資訊與時間戳記
        embed.setFooter("Project Zomboid Server Manager • 測試通知", null);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    /**
     * 處理傳統文字指令 !testrestart
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
     * 處理斜線指令 /testrestart
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("testrestart")) {
            event.replyEmbeds(buildRestartEmbed()).queue();
        }
    }

    @Override
    public SlashCommandData getSlashCommand() {
        return Commands.slash("testrestart", "發送測試重啟通知 Embed 卡片");
    }
}
