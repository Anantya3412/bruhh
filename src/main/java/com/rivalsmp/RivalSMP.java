package com.rivalsmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RivalSMP extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("rivalsmp") == null || getCommand("announce") == null) {
            getLogger().severe("A command is missing from plugin.yml. Disabling RivalSMP.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("rivalsmp").setExecutor(this);
        getCommand("announce").setExecutor(this);
        getLogger().info("RivalSMP enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RivalSMP disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();

        if (config.getBoolean("settings.join-message-enabled", true)) {
            String message = config.getString(
                    "settings.join-message", "&b%player% &7joined the server!");
            event.setJoinMessage(color(message.replace("%player%", player.getName())));
        }

        if (config.getBoolean("settings.welcome-message-enabled", true)) {
            String message = config.getString(
                    "settings.welcome-message", "&7Welcome to &bRivalSMP&7, &f%player%&7!");
            player.sendMessage(prefix() + color(message.replace("%player%", player.getName())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (getConfig().getBoolean("settings.quit-message-enabled", true)) {
            String message = getConfig().getString(
                    "settings.quit-message", "&b%player% &7left the server!");
            event.setQuitMessage(color(message.replace("%player%", event.getPlayer().getName())));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("announce")) {
            if (!sender.hasPermission("rivalsmp.admin")) {
                sender.sendMessage(prefix() + color("&cYou don't have permission."));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(prefix() + color("&cUsage: /announce <message>"));
                return true;
            }
            String announcementPrefix = getConfig().getString(
                    "settings.announcement-prefix", "&8[&6Announcement&8] &f");
            Bukkit.broadcastMessage(color(announcementPrefix + String.join(" ", args)));
            return true;
        }

        if (!sender.hasPermission("rivalsmp.use")) {
            sender.sendMessage(prefix() + color("&cYou don't have permission."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(color("&8&m---------&r &bRivalSMP &8&m---------"));
            sender.sendMessage(color("&b/rivalsmp info &7- Show plugin information"));
            sender.sendMessage(color("&b/rivalsmp reload &7- Reload configuration"));
            sender.sendMessage(color("&b/announce <message> &7- Broadcast an announcement"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                sender.sendMessage(prefix() + color("&7Version: &b" + getPluginMeta().getVersion()));
                sender.sendMessage(prefix() + color("&7Server: &b" + Bukkit.getName()));
                sender.sendMessage(prefix() + color("&7Online players: &b"
                        + Bukkit.getOnlinePlayers().size()));
            }
            case "reload" -> {
                if (!sender.hasPermission("rivalsmp.admin")) {
                    sender.sendMessage(prefix() + color("&cYou don't have permission."));
                    return true;
                }
                reloadConfig();
                sender.sendMessage(prefix() + color("&aConfiguration reloaded."));
            }
            default -> sender.sendMessage(prefix()
                    + color("&cUnknown subcommand. Use /rivalsmp help."));
        }
        return true;
    }

    private String prefix() {
        return color(getConfig().getString("settings.prefix", "&8[&bRivalSMP&8] &7"));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
