package de.theredend2000.advancedhunt.util.messages;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.MenuMessageConfig;
import de.theredend2000.advancedhunt.util.HexColor;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuManager {

    private final Main plugin;
    private MenuMessageConfig menuMessageConfig;

    public MenuManager() {
        this.plugin = Main.getInstance();
        reloadMessages();
    }

    public void reloadMessages() {
        String lang = plugin.getPluginConfig().getLanguage();
        if (lang == null)
            lang = "en";

        menuMessageConfig = new MenuMessageConfig(plugin, lang);
        menuMessageConfig.reloadConfig();
    }

    public String getMenuItemName(MenuMessageKey key, String... replacements) {
        String displayname = menuMessageConfig.getMenuMessage(key.getPath() + ".displayname");
        if(displayname == null) return "";

        for (int i = 0; i < replacements.length; i += 2) {
            displayname = displayname.replace(replacements[i], replacements[i + 1]);
        }

        return HexColor.color(displayname.replaceAll("%PLUGIN_NAME_S%",plugin.getPluginConfig().getPluginNameSingular()).replaceAll("%PLUGIN_NAME_P%",plugin.getPluginConfig().getPluginNamePlural()));
    }

    public List<String> getMenuItemLore(MenuMessageKey key, String... replacements) {
        List<String> lore = menuMessageConfig.getMenuMessageList(key.getPath() + ".lore");
        if(lore == null) return Collections.singletonList("");

        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }
            processedLore.add(HexColor.color(line.replaceAll("%PLUGIN_NAME_S%",plugin.getPluginConfig().getPluginNameSingular()).replaceAll("%PLUGIN_NAME_P%",plugin.getPluginConfig().getPluginNamePlural())));
        }

        return processedLore;
    }
}
