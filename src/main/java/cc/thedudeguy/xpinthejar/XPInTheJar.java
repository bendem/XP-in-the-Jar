/**
 * Copyright (C) 2011  Chris Churchwell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package cc.thedudeguy.xpinthejar;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import cc.thedudeguy.xpinthejar.databases.Bank;
import cc.thedudeguy.xpinthejar.listeners.BankListener;
import cc.thedudeguy.xpinthejar.listeners.BottleListener;

public class XPInTheJar extends JavaPlugin {

    public static XPInTheJar instance;
    public static Logger logger;
    public static String xpBottleName = ChatColor.AQUA + "XP Bottle";

    public boolean spoutEnabled = false;
    public boolean protocolLibEnabled = false;

    /**
     * Checks if an item is an EXP Bottle.
     * Can probably do this better lulx
     * @param item
     * @return
     */
    public static boolean isItemXPBottle(ItemStack item) {
        // not a potion not a bottle
        if (!item.getType().equals(Material.POTION)) {
            return false;
        }
        // not 0 -> not water -> not xp bottle
        if (item.getDurability() > 0) {
            return false;
        }
        // no display name = not xp bottle
        if (!item.getItemMeta().hasDisplayName()) {
            return false;
        }
        // display name doesnt match = not xp bottle
        if (!item.getItemMeta().getDisplayName().equals(XPInTheJar.xpBottleName)) {
            return false;
        }

        //only 1 in stack (failsafe?)
        return item.getAmount() == 1;

    }

    public static int getXpStored(ItemStack item) {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        List<String> lore;
        if(meta.hasLore()) {
            lore = meta.getLore();
            if(lore.isEmpty()) {
                return 0;
            }
            String l = lore.get(0);
            int xp;
            try {
                xp = Integer.parseInt(l.split(" ")[0]);
            } catch(NumberFormatException e) {
                return 0;
            }
            return xp;
        }
        return 0;
    }

    public static void setXpStored(ItemStack item, int xp) {
        PotionMeta meta = (PotionMeta)item.getItemMeta();
        if(xp < 1) {
            meta.setLore(null);
            item.setItemMeta(meta);
            return;
        }
        List<String> lore = new ArrayList<>();
        lore.add(xp + " xp collected");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Actions to enable this plugin
     */
    public void onEnable() {
        instance = this;
        logger = getLogger();

        //copy the config and load missing entries
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        //ResourceManager.preLoginCache();
        setupDatabase();

        //register events
        if(getConfig().getBoolean("enableExpBank")) {
            getServer().getPluginManager().registerEvents(new BankListener(), this);
        }
        if(getConfig().getBoolean("enableXPBottles")) {
            getServer().getPluginManager().registerEvents(new BottleListener(), this);
        }

        // Register CommandHandler
        getCommand("xpinthejar").setExecutor(new CommandHandler());

        //check for spout
        if(Bukkit.getPluginManager().isPluginEnabled("Spout")) {
            if(getConfig().getBoolean("enableSpout")) {
                XPInTheJar.logger.log(Level.INFO, "Spout present. Enabling Spout features.");
                spoutEnabled = true;
            } else {
                XPInTheJar.logger.log(Level.INFO, "Disabling Spout features even though Spout is present.");
            }
        }

        //check for ProtocolLib
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            if(getConfig().getBoolean("enableProtocolLib")) {
                XPInTheJar.logger.log(Level.INFO, "ProtocolLib present. Enabling ProtocolLib features.");
                protocolLibEnabled = true;
            } else {
                XPInTheJar.logger.log(Level.INFO, "Disabling ProtocolLib features even though ProtocolLib is present.");
            }

        }

        getLogger().log(Level.INFO, "Enabled.");
    }

    public void onDisable() {
        getLogger().log(Level.INFO, "Disabled.");
    }

    private void setupDatabase() {
        try {
            getDatabase().find(Bank.class).findRowCount();
        } catch (PersistenceException ex) {
            getLogger().log(Level.INFO, "Installing database for " + getDescription().getName() + " due to first time usage");
            installDDL();
        }
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<>();
        list.add(Bank.class);
        return list;
    }

}
