package dev.lrxh.neptune.feature.hotbar.impl;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomItem extends Item {

    private final List<String> commands;
    private final List<String> consoleCommands;

    public CustomItem(String displayName, String material, List<String> lore, byte slot,
                      List<String> commands, List<String> consoleCommands, int customModelData) {
        super(null, displayName, material, lore, true, slot, customModelData);
        this.commands = commands;
        this.consoleCommands = consoleCommands;
    }
}
