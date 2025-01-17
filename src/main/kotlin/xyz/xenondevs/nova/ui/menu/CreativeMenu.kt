package xyz.xenondevs.nova.ui.menu

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial

object CreativeMenu {
    
    fun getWindow(player: Player): SimpleWindow {
        val items = NovaMaterial.values().filter { it.item.data < 9000 }.map(::ObtainItem)
        val gui = GUIBuilder(GUIType.SCROLL, 9, 6).setStructure("" +
            "x x x x x x x x #" +
            "x x x x x x x x u" +
            "x x x x x x x x #" +
            "x x x x x x x x #" +
            "x x x x x x x x d" +
            "x x x x x x x x #")
            .setItems(items)
            .build()
        
        return SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.creative")), gui)
    }
}

class ObtainItem(val material: NovaMaterial) : SimpleItem(material.createItemBuilder()) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.inventory.addItem(material.createItemStack())
    }
}