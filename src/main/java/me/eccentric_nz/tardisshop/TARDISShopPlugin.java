/*
 * Copyright (C) 2021 eccentric_nz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.eccentric_nz.tardisshop;

import me.eccentric_nz.TARDIS.TARDIS;
import me.eccentric_nz.TARDIS.api.TardisAPI;
import me.eccentric_nz.TARDIS.files.TARDISFileCopier;
import me.eccentric_nz.tardisshop.database.TARDISShopDatabase;
import me.eccentric_nz.tardisshop.listener.TARDISShopItemBreak;
import me.eccentric_nz.tardisshop.listener.TARDISShopItemDespawn;
import me.eccentric_nz.tardisshop.listener.TARDISShopItemExplode;
import me.eccentric_nz.tardisshop.listener.TARDISShopItemInteract;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class TARDISShopPlugin extends JavaPlugin {

    public static TARDISShopPlugin plugin;
    private static boolean twaEnabled = false;
    private static TardisAPI tardisApi;
    private final TARDISShopDatabase service = TARDISShopDatabase.getInstance();
    private final HashMap<UUID, TARDISShopItem> settingItem = new HashMap<>();
    private final Set<UUID> removingItem = new HashSet<>();
    private String messagePrefix;
    private Economy economy;
    private TARDIS tardis;
    private NamespacedKey itemKey;
    private Material blockMaterial;
    private FileConfiguration itemsConfig;

    public static boolean isTwaEnabled() {
        return twaEnabled;
    }

    public static TardisAPI getTardisApi() {
        return tardisApi;
    }

    @Override
    public void onDisable() {
        try {
            if (service.connection != null) {
                service.connection.close();
            }
        } catch (SQLException e) {
            debug("Could not close database connection: " + e);
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        PluginManager pluginManager = getServer().getPluginManager();
        String enable = "";
        /* Get TARDIS */
        Plugin plugin = pluginManager.getPlugin("TARDIS");
        if (plugin == null || !plugin.isEnabled()) {
            enable = "TARDIS";
        }
        copy("items.yml");
        tardis = (TARDIS) plugin;
        tardisApi = tardis.getTardisAPI();
        /* Get Vault */
        Plugin vault = pluginManager.getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            enable = "Vault";
        }
        if (pluginManager.isPluginEnabled("TARDISWeepingAngels")) {
            twaEnabled = true;
        }
        if (enable.isEmpty()) {
            setupEconomy();
            messagePrefix = ChatColor.GOLD + "[" + getDescription().getName() + "]" + ChatColor.RESET + " ";
            pluginManager.registerEvents(new TARDISShopItemInteract(this), this);
            pluginManager.registerEvents(new TARDISShopItemDespawn(this), this);
            pluginManager.registerEvents(new TARDISShopItemBreak(this), this);
            pluginManager.registerEvents(new TARDISShopItemExplode(this), this);
            itemKey = new NamespacedKey(this, "tardis_shop_item");
            blockMaterial = Material.valueOf(getConfig().getString("block"));
            itemsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "items.yml"));
            TARDISShopCommand command = new TARDISShopCommand(this);
            getCommand("tardisshop").setExecutor(command);
            getCommand("tardisshop").setTabCompleter(command);
            try {
                String path = getDataFolder() + File.separator + "TARDISShop.db";
                service.setConnection(path);
                service.createTables();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Connection and Tables Error: " + e.getMessage());
            }
        } else {
            getLogger().log(Level.WARNING, "This plugin requires " + enable + " to function, disabling...");
            pluginManager.disablePlugin(this);
        }
    }

    //Loading economy API from Vault
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> registeredServiceProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (registeredServiceProvider == null) {
            return;
        }
        economy = registeredServiceProvider.getProvider();
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public Economy getEconomy() {
        return economy;
    }

    public NamespacedKey getItemKey() {
        return itemKey;
    }

    public HashMap<UUID, TARDISShopItem> getSettingItem() {
        return settingItem;
    }

    public Set<UUID> getRemovingItem() {
        return removingItem;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }

    public void debug(Object object) {
        if (getConfig().getBoolean("debug")) {
            getLogger().log(Level.CONFIG, (String) object);
        }
    }

    /**
     * Copies a file to the TARDISShop plugin directory if it is not present.
     *
     * @param fileName the name of the file to copy
     * @return a File
     */
    private File copy(String fileName) {
        String filepath = getDataFolder() + File.separator + fileName;
        InputStream in = getResource(fileName);
        return TARDISFileCopier.copy(filepath, in, false);
    }
}
