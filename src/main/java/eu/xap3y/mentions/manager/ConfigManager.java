package eu.xap3y.mentions.manager;

import eu.xap3y.mentions.Mentions;

public class ConfigManager {

    public static void reloadConfig() {
        if (!Mentions.getInstance().getDataFolder().exists()) {
            Mentions.getInstance().getDataFolder().mkdir();
        }

        Mentions.getInstance().saveDefaultConfig();
        Mentions.getInstance().reloadConfig();
    }

}
