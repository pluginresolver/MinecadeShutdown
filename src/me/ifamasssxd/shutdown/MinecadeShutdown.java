package me.ifamasssxd.shutdown;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MinecadeShutdown extends JavaPlugin implements Listener {
    boolean restarting = false;
    File config = new File(getDataFolder(), "config.yml");
    String SERVER, LEAVE_MESSAGE, SERVER_RESTARTING;

    @Override
    public void onEnable() {
        if (!config.exists()) {
            saveDefaultConfig();
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        SERVER = getConfig().getString("Server.ToKick");
        LEAVE_MESSAGE = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.LeaveMessage"));
        SERVER_RESTARTING = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Restarting"));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName() == this.getName()) {
            disconnectAllPlayers();
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (label.equalsIgnoreCase("leave")) {
                Player p = (Player) sender;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                try {
                    out.writeUTF("Connect");
                    out.writeUTF(SERVER);
                    p.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
                    p.sendMessage(LEAVE_MESSAGE);
                } catch (Exception ex) {
                    getLogger().severe("Failed to send BungeeCord connection details!");
                    ex.printStackTrace();
                }
                bytes.reset();
            }
        } else {
            sender.sendMessage("You cannot perform this command!");
        }
        return true;
    }

    @EventHandler
    public void onPlayerSend(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().contains("stop")) {
            if (restarting)
                return;
            event.setCancelled(true);
            restarting = true;
            disconnectAllPlayers();
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
                }
            }, 20);
        }
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (restarting) {
            event.disallow(Result.KICK_OTHER, SERVER_RESTARTING);
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().contains("stop")) {
            if (restarting)
                return;
            event.setCommand(null);
            restarting = true;
            disconnectAllPlayers();
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
                }
            }, 20);
        }
    }

    public void disconnectAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            try {
                out.writeUTF("Connect");
                out.writeUTF(SERVER);
                p.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
                p.sendMessage(LEAVE_MESSAGE);
            } catch (Exception ex) {
                getLogger().severe("Failed to send BungeeCord connection details!");
            }

            bytes.reset();
        }
    }
}