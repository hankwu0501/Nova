package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.item.EnergyProgressItem
import xyz.xenondevs.nova.util.*
import java.util.*
import kotlin.math.min

private const val MAX_ENERGY = 10_000
private const val ENERGY_PER_TICK = 5

class CoalGenerator(
    material: NovaMaterial,
    armorStand: ArmorStand
) : TileEntity(material, armorStand) {
    
    private var energy: Int = retrieveData(0, "energy")
    private var burnTime: Int = retrieveData(0, "burnTime")
    private var totalBurnTime: Int = retrieveData(0, "totalBurnTime")
    
    private val inventory = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("inventory"), 1)
        .also { it.setItemUpdateHandler(this::handleInventoryUpdate) }
    private val gui by lazy { CoalGeneratorGUI() }
    
    override fun handleTick() {
        if (burnTime != 0) {
            burnTime--
            energy = min(MAX_ENERGY, energy + ENERGY_PER_TICK)
            
            gui.progressItem.percentage = burnTime.toDouble() / totalBurnTime.toDouble()
            gui.energyBar.percentage = energy.toDouble() / MAX_ENERGY.toDouble()
        } else burnItem()
    }
    
    private fun burnItem() {
        val fuelStack = inventory.getItemStack(0)
        if (energy < MAX_ENERGY && fuelStack != null) {
            val fuel = fuelStack.type.fuel
            if (fuel != null) {
                burnTime += fuel.burnTime
                totalBurnTime = burnTime
                if (fuel.remains == null) {
                    inventory.removeOne(null, 0)
                } else {
                    inventory.setItemStack(null, 0, fuel.remains.toItemStack())
                }
            }
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.player != null) { // done by a player
            if (event.newItemStack != null && event.newItemStack.type.fuel == null) {
                // illegal item
                event.isCancelled = true
            }
        }
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        runAsyncTaskLater(1) {
            when (event.hand) {
                EquipmentSlot.HAND -> event.player.swingMainHand()
                EquipmentSlot.OFF_HAND -> event.player.swingOffHand()
                else -> Unit
            }
        }
        
        gui.openWindow(event.player)
    }
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val drops = super.destroy(dropItems)
        
        // add items from inventory if there are any
        if (inventory.hasItemStack(0)) {
            drops += inventory.getItemStack(0)
        }
        
        // delete the inventory since the items will be dropped
        VirtualInventoryManager.getInstance().remove(inventory)
        
        return drops
    }
    
    override fun createItem(): ItemStack =
        material.createItemBuilder()
            .addLoreLines("§7Energy: $energy/$MAX_ENERGY")
            .build()
    
    override fun saveData() {
        storeData("energy", energy)
        storeData("burnTime", burnTime)
        storeData("totalBurnTime", totalBurnTime)
    }
    
    private fun getEnergyValues() = energy to MAX_ENERGY
    
    inner class CoalGeneratorGUI {
        
        val progressItem = EnergyProgressItem()
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| # # # # # # # |" +
                "| # # # i # # # |" +
                "| # # # ! # # # |" +
                "| # # # # # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inventory, 0))
            .addIngredient('!', progressItem)
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4, ::getEnergyValues)
            .apply { percentage = energy.toDouble() / MAX_ENERGY.toDouble() }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Coal Generator", gui).show()
        }
        
    }
    
}
