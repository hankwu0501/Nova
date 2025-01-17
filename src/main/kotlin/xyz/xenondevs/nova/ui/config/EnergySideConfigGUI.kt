package xyz.xenondevs.nova.ui.config

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.BlockSide

class EnergySideConfigGUI(
    val energyStorage: EnergyStorage,
    private val allowedTypes: List<EnergyConnectionType>,
) : SimpleGUI(8, 3) {
    
    private val structure = Structure("" +
        "# # # u # # # #" +
        "# # l f r # # #" +
        "# # # d b # # #")
        .addIngredient('u', SideConfigItem(BlockSide.TOP))
        .addIngredient('l', SideConfigItem(BlockSide.LEFT))
        .addIngredient('f', SideConfigItem(BlockSide.FRONT))
        .addIngredient('r', SideConfigItem(BlockSide.RIGHT))
        .addIngredient('d', SideConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', SideConfigItem(BlockSide.BACK))
    
    init {
        applyStructure(structure)
    }
    
    private fun changeConnectionType(blockFace: BlockFace, forward: Boolean) {
        NetworkManager.handleEndPointRemove(energyStorage, false)
        
        val currentType = energyStorage.energyConfig[blockFace]!!
        var index = allowedTypes.indexOf(currentType)
        if (forward) index++ else index--
        if (index < 0) index = allowedTypes.lastIndex
        else if (index == allowedTypes.size) index = 0
        energyStorage.energyConfig[blockFace] = allowedTypes[index]
        
        NetworkManager.handleEndPointAdd(energyStorage)
    }
    
    private inner class SideConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (energyStorage as TileEntity).getFace(blockSide)
        
        override fun getItemBuilder(): ItemBuilder {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return when (energyStorage.energyConfig[blockFace]!!) {
                EnergyConnectionType.NONE ->
                    NovaMaterial.GRAY_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.none")
                EnergyConnectionType.PROVIDE ->
                    NovaMaterial.ORANGE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.output")
                EnergyConnectionType.CONSUME ->
                    NovaMaterial.BLUE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input")
                EnergyConnectionType.BUFFER ->
                    NovaMaterial.GREEN_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input_output")
            }.setLocalizedName("menu.nova.side_config.${blockSide.lowercase()}")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            changeConnectionType(blockFace, clickType.isLeftClick)
            notifyWindows()
        }
        
    }
    
}