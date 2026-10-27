package com.techminions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Предмет, которым можно щёлкнуть (ПКМ) по своему миньону. Вызывается на сервере из MinionEntity.mobInteract. */
public interface MinionInteractable {
    void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion);
}
