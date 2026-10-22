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

package com.chiliasmstudio.ProjectZomboidServerManager.function.discord;

import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands.HelloWorld;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands.TestCommand;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.LegacyCommands.TestRestartCommand;
import com.chiliasmstudio.ProjectZomboidServerManager.function.discord.SlashCommands.LuCat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class MainBot {
    public static JDABuilder botBuilder = null;
    public static JDA jda = null;

    /**
     * Initialization bot.
     *
     * @param DiscordToken Token of discord bot.
     */
    public static boolean initialize(String DiscordToken) {
        try {
            botBuilder = JDABuilder.createDefault(DiscordToken);
            botBuilder.setActivity(Activity.playing("Hello discord!"));
            botBuilder.enableIntents(GatewayIntent.MESSAGE_CONTENT);

            botBuilder.addEventListeners(new LuCat());

            botBuilder.addEventListeners(new HelloWorld());
            botBuilder.addEventListeners(new TestCommand());
            botBuilder.addEventListeners(new TestRestartCommand());

            jda = botBuilder.build();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
