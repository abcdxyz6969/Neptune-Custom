package dev.lrxh.neptune.feature.hotbar.listener;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.Neptune;
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

import java.util.List;

public class ItemListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        Action action = event.getAction();
        Player player = event.getPlayer();
        Profile profile = API.getProfile(player.getUniqueId());

        if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
            return;
        }

        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK
                || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        // cooldown check theo Profile gốc
        if (!profile.hasCooldownEnded("hotbar")) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        event.setCancelled(true);

        // hotbarService lấy item theo itemstack
        Item clickedItem = Neptune.get().getHotbarService().getItem(player, event.getItem());
        if (clickedItem == null) {
            return;
        }

        /*
         * Prevent using items while in match and not in a playing phase (theo enum MatchState gốc)
         */
        if (profile.getState() == ProfileState.IN_GAME) {
            if (profile.getMatch() != null && profile.getMatch().getState() != MatchState.IN_ROUND) {
                return;
            }
        }

        /*
         * ✅ Custom command handler (new)
         */
        if (clickedItem instanceof CustomItem) {
            CustomItem customItem = (CustomItem) clickedItem;
            List<String> commands = customItem.getCommands();

            if (commands == null || commands.isEmpty()) {
                return;
            }

            for (String command : commands) {
                if (command == null) continue;

                command = command.trim();
                if (command.isEmpty() || command.equalsIgnoreCase("none")) continue;

                if (command.startsWith("/")) {
                    command = command.substring(1);
                }

                player.performCommand(command);
            }
        } else {
            // default items vẫn chạy action như cũ
            clickedItem.getAction().execute(player);
        }

        // set cooldown như cũ
        profile.addCooldown("hotbar", 200);
    }
}
