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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

/**
 * 建構模組更新與重啟通知的 Embed 卡片。
 */
public class RestartEmbed {
    private static final String WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    /**
     * 待更新的工作坊模組。
     *
     * @param id    工作坊 ID。
     * @param title 模組名稱。
     */
    public record Mod(String id, String title) {
    }

    /**
     * @param serverName 伺服器名稱。
     * @param countdown  預計重啟倒數的文字。
     * @param debugMode  是否為除錯快速重啟。
     * @param mods       待更新的模組。
     * @param note       額外說明 (可為 null)。
     */
    public static MessageEmbed build(String serverName, String countdown, boolean debugMode, List<Mod> mods, String note) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔄 Project Zomboid 伺服器模組更新與重啟通知");
        embed.setDescription("偵測到 Steam 工作坊模組有新版本發布，伺服器即將重新啟動以套用更新。");
        embed.setColor(new Color(0xE67E22));

        embed.addField("伺服器名稱", serverName, true);
        embed.addField("預計重啟倒數", countdown, true);
        embed.addField("除錯快速重啟", debugMode ? "已啟用" : "已停用 (正常排程)", true);
        embed.addField("📦 待更新工作坊模組 (" + mods.size() + ")", formatMods(mods), false);
        if (note != null)
            embed.addField("ℹ️ 說明", note, false);

        embed.setFooter("Project Zomboid Server Manager", null);
        embed.setTimestamp(Instant.now());
        return embed.build();
    }

    /**
     * 將模組列成 Markdown 超連結，超過欄位長度上限時以「其他 N 個」收尾。
     */
    static String formatMods(List<Mod> mods) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            Mod mod = mods.get(i);
            String title = mod.title() == null || mod.title().isBlank() ? mod.id() : mod.title().replace("]", ")").replace("[", "(");
            String line = "• [" + title + "](" + WORKSHOP_URL + mod.id() + ")\n";
            String rest = "…及其他 " + (mods.size() - i) + " 個";
            if (result.length() + line.length() + rest.length() > MessageEmbed.VALUE_MAX_LENGTH) {
                result.append(rest);
                return result.toString();
            }
            result.append(line);
        }
        return result.isEmpty() ? "-" : result.toString().stripTrailing();
    }
}
