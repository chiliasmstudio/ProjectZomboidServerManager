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

package com.chiliasmstudio.ProjectZomboidServerManager.function.discord.SlashCommands;

import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.SlashCommandProvider;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;


/**
 * 斜線指令 /lucat：回覆 meow。
 */
public class LuCat extends ListenerAdapter implements SlashCommandProvider {

    /**
     * 建立指令監聽器。
     */
    public LuCat() {
    }

    /**
     * 收到 /lucat 時回覆 meow。
     *
     * @param event 斜線指令事件。
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("lucat")) {
            event.reply("meow").queue(); // reply immediately
        }
    }

    /**
     * 取得 /lucat 的斜線指令定義。
     *
     * @return 斜線指令定義。
     */
    @Override
    public SlashCommandData getSlashCommand() {
        return Commands.slash("lucat", "meow");
    }
}
