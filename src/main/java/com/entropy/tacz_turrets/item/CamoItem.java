package com.entropy.tacz_turrets.item;

import com.entropy.tacz_turrets.TACZTurrets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class CamoItem extends Item {
    private final Identifier pattern;

    public CamoItem(String name) {
        super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, TACZTurrets.id(name + "_camo"))));
        pattern = TACZTurrets.id("textures/entity/camo/" + name + ".png");
    }

    public Identifier pattern() {
        return pattern;
    }
}
