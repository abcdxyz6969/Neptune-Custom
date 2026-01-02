package dev.lrxh.neptune.commands;

import dev.lrxh.neptune.feature.divisions.menu.DivisionsMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DivisionsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // ✅ Open divisions menu
        new DivisionsMenu().open(player);

        return true;
    }
}
