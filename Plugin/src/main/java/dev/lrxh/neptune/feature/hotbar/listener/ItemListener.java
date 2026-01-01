package dev.lrxh.neptune.feature.hotbar.listener;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.feature.hotbar.impl.CustomItem;
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

public class ItemListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        Action action = event.getAction();
        Player player = event.getPlayer();
        Profile profile = API.getInstance().getProfileService().get(player.getUniqueId());

        if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
            return;
        }

        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK
                || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        if (profile.isOnCooldown("hotbar")) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        event.setCancelled(true);

        Item clickedItem = API.getInstance().getHotbarService().getItem(player, event.getItem());

        if (clickedItem == null) {
            return;
        }

        /*
         * Prevent using items while in match and not in playing state
         */
        if (profile.getState() == ProfileState.FIGHTING) {
            if (profile.getMatch() != null && profile.getMatch().getState() != MatchState.PLAYING) {
                return;
            }
        }

        /*
         * Custom command handler
         */
        if (clickedItem instanceof CustomItem) {
            CustomItem customItem = (CustomItem) clickedItem;

            if (customItem.getCommands() == null || customItem.getCommands().isEmpty()) {
                return;
            }

            for (String command : customItem.getCommands()) {
                if (command == null) continue;

                command = command.trim();
                if (command.isEmpty() || command.equalsIgnoreCase("none")) continue;

                if (command.startsWith("/")) {
                    command = command.substring(1);
                }

                player.performCommand(command);
            }
        } else {
            clickedItem.getAction().execute(player);
        }

        profile.addCooldown("hotbar", 200);
    }
}
