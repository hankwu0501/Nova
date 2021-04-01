package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.inventory.ItemStack

fun Location.dropItems(items: Iterable<ItemStack>) {
    val world = world!!
    items.forEach { world.dropItemNaturally(this, it) }
}

fun Location.removeOrientation() {
    yaw = 0f
    pitch = 0f
}