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

import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.SlashCommandProvider;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;


public class HelloWorld extends ListenerAdapter implements SlashCommandProvider {

    /**
     * Say Hello world! when !Hello world is received.
     */
    public void onMessageReceived(MessageReceivedEvent event) {
        // 私訊時 getMember() 為 null，改用 getAuthor() 判斷是否為 Bot
        if (event.getMessage().getContentStripped().equalsIgnoreCase("!Hello world") && !event.getAuthor().isBot()) {
            event.getChannel().sendMessage("Hello world!").queue();
            Emoji customEmoji = Emoji.fromCustom("test",1315566752192790558L,true);
            event.getChannel().sendMessage(customEmoji.getFormatted()).queue();

        }
    }

    /**
     * Reply Hello world! when helloworld command send.
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("helloworld")) {
            event.reply("Hello world!").queue(); // reply immediately
        }
    }

    @Override
    public SlashCommandData getSlashCommand() {
        return Commands.slash("helloworld", "Say hello world!");
    }
}
