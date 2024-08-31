package de.theredend2000.advancedegghunt.util.messages;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.configurations.MenuMessageConfig;
import de.theredend2000.advancedegghunt.util.HexColor;
import org.bukkit.ChatColor;

public class MenuMessageManager {

    private final Main plugin;
    private MenuMessageConfig menuMessageConfig;

    public MenuMessageManager() {
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

    public String getMenuMessage(MenuMessageKey key) {
        String menuMessage = menuMessageConfig.getMenuMessage(key.getPath());

        return HexColor.color(ChatColor.translateAlternateColorCodes('&', menuMessage));
    }

    public String getMenuMessage(MenuMessageKey key, String... replacements) {
        String menuMessage = this.getMenuMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            menuMessage = menuMessage.replace(replacements[i], replacements[i + 1]);
        }

        return HexColor.color(ChatColor.translateAlternateColorCodes('&', menuMessage));
    }
}
