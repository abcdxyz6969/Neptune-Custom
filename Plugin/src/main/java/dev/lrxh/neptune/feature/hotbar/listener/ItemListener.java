package dev.lrxh.neptune.feature.hotbar.listener;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.feature.hotbar.HotbarService;
import dev.lrxh.neptune.feature.hotbar.impl.CustomItem;
import dev.lrxh.neptune.feature.hotbar.impl.Hotbar;
import dev.lrxh.neptune.feature.hotbar.impl.Item;
import dev.lrxh.neptune.game.match.impl.MatchState;
import dev.lrxh.neptune.profile.data.ProfileState;
import dev.lrxh.neptune.profile.impl.Profile;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class ItemListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        Profile profile = API.getProfile(player.getUniqueId());

        if (event.getItem() == null || event.getItem().getType() == Material.AIR) return;
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!profile.hasCooldownEnded("hotbar")) return;

        if (profile.getState() == ProfileState.IN_GAME) {
            if (profile.getMatch() != null && profile.getMatch().getState() == MatchState.IN_ROUND) {
                return;
            }
        }

        event.setCancelled(true);

        Hotbar hotbar = HotbarService.get().getItems().get(profile.getState());
        if (hotbar == null) return;

        int slot = player.getInventory().getHeldItemSlot();
        Item clickedItem = HotbarService.get().getItemForSlot(hotbar, slot);
        if (clickedItem == null) return;

        if (clickedItem instanceof CustomItem) {
            CustomItem customItem = (CustomItem) clickedItem;
            List<String> commands = customItem.getCommands();

            if (commands == null || commands.isEmpty()) return;

            for (String command : commands) {
                if (command == null) continue;
                command = command.trim();
                if (command.isEmpty() || command.equalsIgnoreCase("none")) continue;
                if (command.startsWith("/")) command = command.substring(1);
                player.performCommand(command);
            }
        } else {
            clickedItem.getAction().execute(player);
        }

        profile.addCooldown("hotbar", 200);
    }
}
