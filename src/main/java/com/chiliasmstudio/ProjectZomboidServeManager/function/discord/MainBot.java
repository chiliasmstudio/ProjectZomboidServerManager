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

package com.chiliasmstudio.ProjectZomboidServeManager.function.discord;

import com.chiliasmstudio.ProjectZomboidServeManager.function.discord.LegacyCommands.HelloWorld;
import com.chiliasmstudio.ProjectZomboidServeManager.function.discord.LegacyCommands.TestCommand;
import com.chiliasmstudio.ProjectZomboidServeManager.function.discord.LegacyCommands.TestRestartCommand;
import com.chiliasmstudio.ProjectZomboidServeManager.function.discord.SlashCommands.LuCat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class MainBot {
    public static JDABuilder botBuilder_Main = null;
    public static JDA bot_Main = null;

    /**
     * Initialization bot.
     *
     * @param DiscordToken Token of discord bot.
     */
    public static boolean initialization_Main(String DiscordToken) {
        try {
            botBuilder_Main = JDABuilder.createDefault(DiscordToken);
            botBuilder_Main.setActivity(Activity.playing("Hello discord!"));
            botBuilder_Main.enableIntents(GatewayIntent.MESSAGE_CONTENT);

            botBuilder_Main.addEventListeners(new LuCat());

            botBuilder_Main.addEventListeners(new HelloWorld());
            botBuilder_Main.addEventListeners(new TestCommand());
            botBuilder_Main.addEventListeners(new TestRestartCommand());

            bot_Main = botBuilder_Main.build();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
