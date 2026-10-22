package com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestartEmbedTest {

    @Test
    void 模組過多時截斷且不超過欄位上限() {
        List<RestartEmbed.Mod> mods = new ArrayList<>();
        for (int i = 0; i < 200; i++)
            mods.add(new RestartEmbed.Mod(String.valueOf(1000000000L + i), "Some Long Workshop Mod Title " + i));

        String value = RestartEmbed.formatMods(mods);
        assertTrue(value.length() <= MessageEmbed.VALUE_MAX_LENGTH);
        assertTrue(value.contains("…及其他"));
    }

    @Test
    void 沒有標題時以ID代替且跳脫中括號() {
        String value = RestartEmbed.formatMods(List.of(
                new RestartEmbed.Mod("1", ""),
                new RestartEmbed.Mod("2", "[B42] Mod")));
        assertTrue(value.contains("• [1]("));
        assertTrue(value.contains("• [(B42) Mod]("));
    }
}
