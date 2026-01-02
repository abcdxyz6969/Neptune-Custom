package dev.lrxh.neptune.commands;

import com.jonahseguin.drink.annotation.Command;
import dev.lrxh.neptune.feature.divisions.menu.DivisionsMenu;
import org.bukkit.entity.Player;

public class DivisionsCommand {

    @Command(name = "", desc = "Open divisions menu")
    public void divisions(Player player) {
        new DivisionsMenu().open(player);
    }
}
