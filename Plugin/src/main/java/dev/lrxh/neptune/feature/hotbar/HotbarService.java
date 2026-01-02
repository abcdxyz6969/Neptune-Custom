package dev.lrxh.neptune.feature.hotbar;

import dev.lrxh.neptune.Neptune;
import dev.lrxh.neptune.feature.hotbar.action.ItemAction;
import dev.lrxh.neptune.feature.hotbar.impl.CustomItem;
import dev.lrxh.neptune.feature.hotbar.impl.Item;
import dev.lrxh.neptune.profile.data.ProfileState;
import dev.lrxh.neptune.util.CC;
import dev.lrxh.neptune.util.ItemBuilder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * HotbarService
 *
 * - Load default items from ITEMS.<STATE>.<ITEMNAME>
 * - Load custom items from CUSTOM_ITEMS
 * - Support COMMAND: string OR list
 * - Support Lore hidden token: THISLINENONE
 *   -> if lore line equals THISLINENONE or empty -> ignored and removed
 */
@Getter
public class HotbarService {

    private static HotbarService instance;

    public static HotbarService get() {
        if (instance == null) instance = new HotbarService();
        return instance;
    }

    private static final String LORE_NONE_TOKEN = "THISLINENONE";

    // Items by state
    private final Map<ProfileState, Map<String, Item>> itemsByState = new EnumMap<>(ProfileState.class);

    private HotbarService() {
        for (ProfileState state : ProfileState.values()) {
            itemsByState.put(state, new HashMap<>());
        }
    }

    /**
     * Call this on enable/reload
     */
    public void load() {
        for (ProfileState state : ProfileState.values()) {
            itemsByState.get(state).clear();
        }

        // Load default items from ITEMS
        loadDefaultItems();

        // Load custom items from CUSTOM_ITEMS
        loadCustomItems();
    }

    /**
     * Load items in config section: ITEMS.<STATE>.<ITEM>
     * Example:
     * ITEMS:
     *   IN_LOBBY:
     *     UNRANKED:
     *       MATERIAL: IRON_SWORD
     *       SLOT: 0
     *       ENABLED: true
     */
    private void loadDefaultItems() {
        ConfigurationSection itemsSection = Neptune.get().getConfigService().getHotbar().getConfigurationSection("ITEMS");
        if (itemsSection == null) return;

        for (String stateKey : itemsSection.getKeys(false)) {
            ProfileState state;
            try {
                state = ProfileState.valueOf(stateKey.toUpperCase());
            } catch (Exception ignored) {
                continue;
            }

            ConfigurationSection stateSection = itemsSection.getConfigurationSection(stateKey);
            if (stateSection == null) continue;

            for (String itemName : stateSection.getKeys(false)) {
                ConfigurationSection itemSection = stateSection.getConfigurationSection(itemName);
                if (itemSection == null) continue;

                if (!itemSection.getBoolean("ENABLED", true)) continue;

                String displayName = CC.translate(itemSection.getString("NAME", itemName));
                String materialName = itemSection.getString("MATERIAL", "STONE");
                Material material = Material.matchMaterial(materialName);

                if (material == null) {
                    Neptune.get().getLogger().warning("[Hotbar] Invalid material '" + materialName + "' for item '" + itemName + "' in state '" + stateKey + "'");
                    continue;
                }

                byte slot = (byte) itemSection.getInt("SLOT", 0);
                int customModelData = itemSection.getInt("CUSTOM_MODEL_DATA", 0);

                List<String> lore = parseLore(itemSection.getStringList("LORE"));

                // ✅ Nếu item có COMMAND -> tạo CustomItem luôn
                List<String> commands = parseCommands(itemSection);
                if (!commands.isEmpty()) {
                    CustomItem customItem = new CustomItem(displayName, material.name(), lore, slot, commands, customModelData);
                    itemsByState.get(state).put(itemName, customItem);
                    continue;
                }

                // ✅ Item mặc định phải có ItemAction
                try {
                    ItemAction action = ItemAction.valueOf(itemName.toUpperCase());
                    Item item = new Item(action, displayName, material.name(), lore, true, slot, customModelData);
                    itemsByState.get(state).put(itemName, item);
                } catch (IllegalArgumentException ignored) {
                    // Không có action + không có COMMAND => bỏ qua
                }
            }
        }
    }

    /**
     * Load custom items from config section: CUSTOM_ITEMS
     * Example:
     * CUSTOM_ITEMS:
     *   TESTCOMMAND:
     *     NAME: '&eSettings'
     *     MATERIAL: REPEATER
     *     SLOT: 3
     *     COMMAND:
     *     - queues
     *     STATE: IN_LOBBY
     *     ENABLED: true
     */
    private void loadCustomItems() {
        ConfigurationSection section = Neptune.get().getConfigService().getHotbar().getConfigurationSection("CUSTOM_ITEMS");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) continue;

            if (!itemSection.getBoolean("ENABLED", true)) continue;

            String displayName = CC.translate(itemSection.getString("NAME", key));
            String materialName = itemSection.getString("MATERIAL", "STONE");
            Material material = Material.matchMaterial(materialName);

            if (material == null) {
                Neptune.get().getLogger().warning("[Hotbar] Invalid material '" + materialName + "' for custom item '" + key + "'");
                continue;
            }

            byte slot = (byte) itemSection.getInt("SLOT", 0);
            int customModelData = itemSection.getInt("CUSTOM_MODEL_DATA", 0);

            List<String> lore = parseLore(itemSection.getStringList("LORE"));
            List<String> commands = parseCommands(itemSection);

            String stateKey = itemSection.getString("STATE", "IN_LOBBY");
            ProfileState state;

            try {
                state = ProfileState.valueOf(stateKey.toUpperCase());
            } catch (Exception ignored) {
                state = ProfileState.IN_LOBBY;
            }

            if (commands.isEmpty()) {
                // Nếu không có command thì item này vô nghĩa -> skip
                continue;
            }

            CustomItem customItem = new CustomItem(displayName, material.name(), lore, slot, commands, customModelData);
            itemsByState.get(state).put(key, customItem);
        }
    }

    /**
     * Give items of a state to player
     */
    public void give(Player player, ProfileState state) {
        player.getInventory().clear();

        Map<String, Item> items = itemsByState.getOrDefault(state, Collections.emptyMap());
        for (Item item : items.values()) {
            if (item == null) continue;

            ItemBuilder builder = new ItemBuilder(item.getMaterial())
                    .name(item.getDisplayName())
                    .customModelData(item.getCustomModelData());

            // ✅ Nếu lore rỗng thì không set lore (tránh tạo khoảng trống)
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                builder.lore(item.getLore());
            }

            ItemStack stack = builder.build();
            player.getInventory().setItem(item.getSlot(), stack);
        }

        player.updateInventory();
    }

    /**
     * Get item object based on clicked ItemStack
     * This method checks all states (lobby/editor/game...) for a match.
     */
    public Item getItem(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return null;
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return null;

        String clickedName = clicked.getItemMeta().getDisplayName();
        Material clickedType = clicked.getType();

        Integer clickedCMD = null;
        if (clicked.getItemMeta().hasCustomModelData()) {
            clickedCMD = clicked.getItemMeta().getCustomModelData();
        }

        // search all states
        for (Map<String, Item> map : itemsByState.values()) {
            for (Item item : map.values()) {
                if (item == null) continue;

                Material type = Material.matchMaterial(item.getMaterial());
                if (type == null) continue;

                if (type != clickedType) continue;
                if (!CC.translate(item.getDisplayName()).equals(clickedName)) continue;

                // Compare CustomModelData if exists
                if (item.getCustomModelData() > 0 || clickedCMD != null) {
                    if (clickedCMD == null) continue;
                    if (item.getCustomModelData() != clickedCMD) continue;
                }

                return item;
            }
        }

        return null;
    }

    /**
     * Parse lore:
     * - remove empty lines
     * - remove THISLINENONE token lines
     * - translate colors
     */
    private List<String> parseLore(List<String> lore) {
        if (lore == null) return new ArrayList<>();

        List<String> out = new ArrayList<>();
        for (String line : lore) {
            if (line == null) continue;

            line = line.trim();

            // ✅ Ignore hidden token line
            if (line.equalsIgnoreCase(LORE_NONE_TOKEN)) continue;

            // ✅ Ignore empty lines to avoid spacing
            if (line.isEmpty()) continue;

            out.add(CC.translate(line));
        }

        return out;
    }

    /**
     * Parse commands:
     * - support COMMAND: "cmd"
     * - support COMMAND: [ "cmd1", "cmd2" ]
     */
    private List<String> parseCommands(ConfigurationSection section) {
        List<String> commands = new ArrayList<>();

        if (section == null) return commands;

        if (section.isString("COMMAND")) {
            String cmd = section.getString("COMMAND");
            if (cmd != null && !cmd.trim().isEmpty()) {
                commands.add(cmd.trim());
            }
        } else if (section.isList("COMMAND")) {
            List<String> list = section.getStringList("COMMAND");
            if (list != null) {
                for (String cmd : list) {
                    if (cmd == null) continue;
                    cmd = cmd.trim();
                    if (cmd.isEmpty()) continue;
                    commands.add(cmd);
                }
            }
        }

        return commands;
    }
}
