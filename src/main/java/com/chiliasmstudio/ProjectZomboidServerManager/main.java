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

package com.chiliasmstudio.ProjectZomboidServerManager;

import com.chiliasmstudio.ProjectZomboidServerManager.function.projectzomboid.CheckUpdateHandler;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


import static com.chiliasmstudio.ProjectZomboidServerManager.function.discord.MainBot.initialize;

public class main {
    private static final Logger mainLogger = LogManager.getLogger("main");
    public static CheckUpdateHandler[] checkUpdates = new CheckUpdateHandler[30];

    public static void main(String[] args) throws Exception {

        for (String arg : args) {
            // Set the logging level based on the flags passed
            if ("-debug".equalsIgnoreCase(arg)) {
                Configurator.setRootLevel(Level.DEBUG);
                break; // Stop checking after setting DEBUG level
            } else if ("-trace".equalsIgnoreCase(arg)) {
                Configurator.setRootLevel(Level.TRACE);
                break; // Stop checking after setting TRACE level
            }
        }


        //setUpLog4j();
        mainLogger.trace("This is the trace message, Hello world!");
        mainLogger.debug("This is a debug message, Hello world!");
        mainLogger.info("This is an info message, Hello world!");
        //mainLogger.error("This is an error message.");

        Config.loadConfig(Path.of("config", "config.properties"));
        initialize(Config.discordToken);
        Thread.sleep(5000L);

        loadServerConfig();
    }

    public static void loadServerConfig() throws Exception {
        // Load the main configuration file


        File folder = new File(".//config//servers");
        File[] listOfFiles = folder.listFiles();
        boolean hasServerConfig = false; // Flag to check if any other config files are found

        // Iterate through the files and check for .properties files other than server_example.properties
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".properties") && !listOfFiles[i].getName().equals("server_example.properties")) {
                hasServerConfig = true; // Set flag to true if a valid config is found
                mainLogger.info("find server config: " + listOfFiles[i].getName());
                CheckUpdateHandler updater = new CheckUpdateHandler(listOfFiles[i].getName());
                checkUpdates[i] = updater;
            }
        }

        // If no server config files are found other than server_example.properties, throw an exception
        if (!hasServerConfig) {
            throw new Exception("No server config files found other than server_example.properties!");
        }

        // Start update checks for all the valid server configurations
        for (CheckUpdateHandler servers : checkUpdates) {
            if (servers != null) {
                servers.start();
                Thread.sleep(10*1000L);
            }
        }

        // Keep all threads running, remove finished ones and recreate them
        while (true) {
            boolean anyThreadRemovedOrRestarted = false;

            // Check each thread if it has finished (is not alive). Restart only those that finished.
            for (int i = 0; i < checkUpdates.length; i++) {
                CheckUpdateHandler server = checkUpdates[i];
                if (server != null && !server.isAlive()) {
                    // Thread is not alive (ended), remove it and restart it
                    mainLogger.debug("Thread for server " + server.getServerConfig().getServerName() + " has ended. Removing and restarting...");
                    // Recreate the thread for the server
                    checkUpdates[i] = new CheckUpdateHandler(server.getConfigDir());
                    checkUpdates[i].start();  // Start the new thread
                    anyThreadRemovedOrRestarted = true;  // Indicate that at least one thread was restarted
                    Thread.sleep(10 * 1000L);  // Wait before checking the next thread
                }
            }

            // If no threads were removed or restarted, wait before checking again
            if (!anyThreadRemovedOrRestarted) {
                mainLogger.trace("No threads need to be restarted at this moment");
                Thread.sleep(10 * 1000L);  // Sleep for a bit before checking again
            }
        }
    }

    //Not use yet
    public static void setUpLog4j() throws IOException {
        File configFile = new File("config/log4j2.xml");
        if (!configFile.exists()) {
            // If it does not exist, copy the resource file from the JAR
            try (InputStream resourceStream = Config.class.getClassLoader().getResourceAsStream("log4j2.xml")) {
                if (resourceStream == null) {
                    throw new FileNotFoundException("Resource log4j2.xml not found in JAR!");
                }

                // Create directories if they do not exist
                File configDir = new File(configFile.getParent());
                if (!configDir.exists()) {
                    configDir.mkdir();
                }

                // Copy the file from the JAR to the current directory
                Files.copy(resourceStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Config file copied from JAR resources.");
            } catch (IOException ex) {
                throw new IOException("Failed to copy log4j2.xml from JAR resources!", ex);
            }
        }

        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        File file = new File("Config/log4j2.xml");
        context.setConfigLocation(file.toURI());
    }
}
