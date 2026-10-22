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

package com.chiliasmstudio.ProjectZomboidServerManager.function.discord;

import com.chiliasmstudio.ProjectZomboidServerManager.Config;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands.HelloWorld;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands.TestCommand;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands.TestRestartCommand;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.SlashCommands.LuCat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MainBot {
    private static final Logger botLogger = LogManager.getLogger("MainBot");
    public static JDABuilder botBuilder = null;
    public static JDA jda = null;

    /**
     * 所有可用的 Discord 指令，需在 config.properties 以類別名稱開啟 (例如 LuCat = true)。
     */
    private static final List<ListenerAdapter> AVAILABLE_COMMANDS = List.of(
            new LuCat(),
            new HelloWorld(),
            new TestCommand(),
            new TestRestartCommand()
    );

    /**
     * 會讀取訊息內容的文字指令，啟用任一個時才需要 MESSAGE_CONTENT 特權 intent。
     */
    private static final List<Class<?>> TEXT_COMMANDS = List.of(
            HelloWorld.class, TestCommand.class, TestRestartCommand.class);

    /**
     * 初始化 Bot。
     *
     * @param discordToken Discord Bot 的 Token。
     */
    public static boolean initialize(String discordToken) {
        try {
            botBuilder = JDABuilder.createDefault(discordToken);
            botBuilder.setActivity(Activity.playing("Hello discord!"));

            boolean needMessageContent = false;
            List<SlashCommandData> slashCommands = new ArrayList<>();
            for (ListenerAdapter command : AVAILABLE_COMMANDS) {
                String name = command.getClass().getSimpleName();
                if (!Config.isCommandEnabled(name))
                    continue;
                botBuilder.addEventListeners(command);
                botLogger.info("Discord command enabled: " + name);
                if (TEXT_COMMANDS.contains(command.getClass()))
                    needMessageContent = true;
                if (command instanceof SlashCommandProvider provider)
                    slashCommands.add(provider.getSlashCommand());
            }
            if (needMessageContent)
                botBuilder.enableIntents(GatewayIntent.MESSAGE_CONTENT);

            // updateCommands() 會整批取代伺服器的指令，因此統一在此一次註冊所有已啟用的斜線指令
            botBuilder.addEventListeners(new ListenerAdapter() {
                @Override
                public void onGuildReady(GuildReadyEvent event) {
                    event.getGuild().updateCommands().addCommands(slashCommands).queue();
                }
            });

            jda = botBuilder.build();
        } catch (Exception e) {
            botLogger.error("Failed to initialize discord bot.", e);
            return false;
        }
        return true;
    }
}
