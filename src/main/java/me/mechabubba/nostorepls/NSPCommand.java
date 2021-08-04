package me.mechabubba.nostorepls;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

public class NSPCommand implements CommandExecutor {
    NoStorePls plugin;

    public NSPCommand(NoStorePls plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1) {
            PluginDescriptionFile desc = plugin.getDescription();
            sender.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + ChatColor.UNDERLINE + desc.getName());
            sender.sendMessage("" + ChatColor.YELLOW + ChatColor.ITALIC + "\"" + desc.getDescription() + "\"");
            sender.sendMessage("" + ChatColor.YELLOW + "Author: " + ChatColor.WHITE + desc.getAuthors());
            sender.sendMessage("" + ChatColor.YELLOW + "Version: " + ChatColor.WHITE + desc.getVersion());
            sender.sendMessage("" + ChatColor.YELLOW + "Reload the config by preforming \"/" + label + " reload\".");
        } else {
            switch(args[0]) {
                case "reload":
                    plugin.reloadConfig();
                    plugin.loadBlocklists();
                    sender.sendMessage("" + ChatColor.GREEN + "Reloaded NSP config!");
                    break;
                default:
                    sender.sendMessage("" + ChatColor.RED + "Unknown subcommand. Do /" + label + " for more information.");
                    break;
            }
        }
        return true;
    }
}
