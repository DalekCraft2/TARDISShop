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

import com.google.common.collect.ImmutableList;
import me.eccentric_nz.TARDIS.commands.TARDISCompleter;
import me.eccentric_nz.TARDIS.utility.TARDISStringUtils;
import me.eccentric_nz.tardisshop.database.InsertShopItem;
import me.eccentric_nz.tardisshop.database.ResultSetUpdateShop;
import me.eccentric_nz.tardisshop.database.UpdateShopItem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TARDISShopCommand extends TARDISCompleter implements CommandExecutor, TabCompleter {

    private final TARDISShopPlugin plugin;
    private final ImmutableList<String> rootSubs = ImmutableList.of("add", "remove", "update");
    private final ArrayList<String> itemSubs;

    public TARDISShopCommand(TARDISShopPlugin plugin) {
        this.plugin = plugin;
        itemSubs = new ArrayList<>(this.plugin.getItemsConfig().getKeys(false));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        // must be a player
        if (player == null) {
            sender.sendMessage(plugin.getMessagePrefix() + "Command can only be used by a player!");
            return true;
        }
        // return if no arguments
        if (args.length < 1) {
            player.sendMessage(plugin.getMessagePrefix() + "Too few command arguments!");
            return false;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            plugin.getRemovingItem().add(player.getUniqueId());
            player.sendMessage(plugin.getMessagePrefix() + "Click the " + plugin.getBlockMaterial().toString() + " block to remove the database record.");
            return true;
        } else if (args[0].equalsIgnoreCase("update")) {
            // reload items.yml
            File file = new File(plugin.getDataFolder(), "items.yml");
            try {
                plugin.getItemsConfig().load(file);
            } catch (InvalidConfigurationException | IOException e) {
                plugin.debug("Failed to reload items.yml" + e.getMessage());
            }
            // get shop items
            ResultSetUpdateShop resultSetUpdateShop = new ResultSetUpdateShop(plugin);
            if (resultSetUpdateShop.getAll()) {
                for (TARDISShopItem item : resultSetUpdateShop.getShopItems()) {
                    String lookup = item.getItem().replace(" ", "_").toLowerCase();
                    double cost = plugin.getItemsConfig().getDouble(lookup);
                    if (cost != item.getCost()) {
                        // update database
                        new UpdateShopItem(plugin).updateCost(cost, item.getId());
                        // find armor stand and update display name
                        for (Entity entity : item.getLocation().getWorld().getNearbyEntities(item.getLocation(), 0.5d, 1.0d, 0.5d)) {
                            if (entity instanceof ArmorStand) {
                                entity.setCustomName(ChatColor.RED + "Cost:" + ChatColor.RESET + String.format(" %.2f", cost));
                            }
                        }
                    }
                }
            }
            return true;
        }
        // need at least 2 arguments from here on
        if (args.length < 2) {
            player.sendMessage(plugin.getMessagePrefix() + "Too few command arguments!");
            return true;
        }
        if (args[0].equalsIgnoreCase("add")) {
            String name = args[1].toLowerCase();
            if (!plugin.getItemsConfig().contains(name)) {
                player.sendMessage(plugin.getMessagePrefix() + "Too few command arguments!");
                return true;
            }
            double cost = plugin.getItemsConfig().getDouble(name);
            TARDISShopItem item = new InsertShopItem(plugin).addNamedItem(TARDISStringUtils.capitalise(args[1]), cost);
            plugin.getSettingItem().put(player.getUniqueId(), item);
            player.sendMessage(plugin.getMessagePrefix() + "Click the " + plugin.getBlockMaterial().toString() + " block to update the database record.");
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        // Remember that we can return null to default to online player name matching
        String lastArg = args[args.length - 1];

        if (args.length <= 1) {
            return partial(args[0], rootSubs);
        } else if (args.length == 2) {
            String sub = args[0];
            if (sub.equalsIgnoreCase("add")) {
                return partial(lastArg, itemSubs);
            }
        }
        return ImmutableList.of();
    }
}
