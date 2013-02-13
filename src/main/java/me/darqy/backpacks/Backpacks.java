package me.darqy.backpacks;

import me.darqy.backpacks.util.SnooperApi;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.darqy.backpacks.command.*;

public class Backpacks extends JavaPlugin {
    
    private GroupConfig groupConfig;
    private BackpacksConfig config;
    private File groupsFolder;
    
    private Map<String, BackpackManager> managers = new HashMap();

    @Override
    public void onEnable() {        
        try {
            reloadConfiguration();
            registerCommands();
            scheduleBackpackSaver();
            SnooperApi.setPlugin(this);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error enabling myself :(: ", e);
        }
    }
    
    @Override
    public void onDisable() {
        closeAllBackpacks();
        saveAllBackpacks();
    }
    
    public void saveAllBackpacks() {
        for (BackpackManager mngr : managers.values()) {
            try {
                mngr.saveBackpacks();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error while saving backpack!", e);
            }
        }
    }
    
    public void closeAllBackpacks() {
        for (BackpackManager mngr : managers.values()) {
            mngr.closeBackpacks();
        }
    }
    
    public BackpackManager getManager(World world) {
        return getManager(world.getName());
    }
    
    public BackpackManager getManager(String world) {
        if (config.getConfiguredWorldsOnly() && !groupConfig.configured(world)) {
            return null;
        }
        String group = groupConfig.getGroup(world);
        
        if (!managers.containsKey(group)) {
            BackpackManager mngr = new BackpackManager(new File(groupsFolder, group));
            managers.put(group, mngr);
            return mngr;
        } else {
            return managers.get(group);
        }
    }
    
    public static Player matchPlayer(CommandSender sender, String target) {
        Player player = Bukkit.getPlayer(target);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + target + " not found online");
        }
        return player;
    }
    
    
    private void reloadConfiguration() throws IOException {
        File groupsFile = new File(getDataFolder(), "groups.yml");
        
        if (!groupsFile.exists()) {
            saveResource("groups.yml", false);
        }

        groupConfig = new GroupConfig(YamlConfiguration.loadConfiguration(groupsFile));
        groupsFolder = new File(getDataFolder(), "groups/");
        
        File configFile = new File(getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        
        config = new BackpacksConfig(YamlConfiguration.loadConfiguration(configFile));
    }
    
    private void registerCommands() {
        getCommand("createpack").setExecutor(new CmdCreateBackpack(this));
        getCommand("backpack").setExecutor(new CmdBackpack(this));
        getCommand("inspectpack").setExecutor(new CmdInspectBackpack(this));
        getCommand("listpacks").setExecutor(new CmdListBackpacks(this));
        getCommand("packtools").setExecutor(new CmdBackpackTools(this));
    }
    
    private void scheduleBackpackSaver() {
        getServer().getScheduler().scheduleSyncRepeatingTask(
                this, new BackpackSaver(this), 0L, config.getSaveInterval());
    }
    
    private static class BackpackSaver implements Runnable {

        private Backpacks plugin;
        
        public BackpackSaver(Backpacks instance) {
            this.plugin = instance;
        }
        
        @Override
        public void run() {
            plugin.saveAllBackpacks();
        }
        
    }
    
}