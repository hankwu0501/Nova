package xyz.xenondevs.nova.tileentity.impl.world

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.*
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import xyz.xenondevs.particle.ParticleEffect
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.properties.Delegates

private val SCAFFOLDING_STACKS = NovaMaterial.SCAFFOLDING.block!!.let { modelData -> modelData.dataArray.indices.map { modelData.getItem(it) } }
private val FULL_HORIZONTAL = SCAFFOLDING_STACKS[0]
private val FULL_VERTICAL = SCAFFOLDING_STACKS[1]
private val CORNER_DOWN = SCAFFOLDING_STACKS[2]
private val SMALL_HORIZONTAL = SCAFFOLDING_STACKS[3]
private val FULL_SLIM_VERTICAL = SCAFFOLDING_STACKS[4]
private val SLIM_VERTICAL_DOWN = SCAFFOLDING_STACKS[5]
private val DRILL = NovaMaterial.NETHERITE_DRILL.createItemStack()

private val MIN_SIZE = NovaConfig.getInt("quarry.min_size")!!
private val MAX_SIZE = NovaConfig.getInt("quarry.max_size")!!
private val DEFAULT_SIZE_X = NovaConfig.getInt("quarry.default_size_x")!!
private val DEFAULT_SIZE_Z = NovaConfig.getInt("quarry.default_size_z")!!

private val MOVE_SPEED = NovaConfig.getDouble("quarry.move_speed")!!
private val DRILL_SPEED_MULTIPLIER = NovaConfig.getDouble("quarry.drill_speed_multiplier")!!
private val DRILL_SPEED_CLAMP = NovaConfig.getDouble("quarry.drill_speed_clamp")!!

private val MAX_ENERGY = NovaConfig.getInt("quarry.capacity")!!
private val ENERGY_CONSUMPTION_BASE = NovaConfig.getInt("quarry.energy_consumption_base")!!
private val ENERGY_INEFFICIENCY_EXPONENT = NovaConfig.getDouble("quarry.energy_inefficiency_exponent")!!

class Quarry(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    override val gui by lazy { QuarryGUI() }
    private val inventory = getInventory("quarryInventory", 9, true) {}
    
    private val entityId = uuid.hashCode()
    
    private var sizeX = retrieveData("sizeX") { DEFAULT_SIZE_X }
    private var sizeZ = retrieveData("sizeZ") { DEFAULT_SIZE_Z }
    
    private var energyPerTick by Delegates.notNull<Int>()
    
    private val solidScaffolding = getMultiModel("solidScaffolding")
    private val armX = getMultiModel("armX")
    private val armZ = getMultiModel("armZ")
    private val armY = getMultiModel("armY")
    private val drill = getMultiModel("drill")
    
    private val y: Int
    private var minX = 0
    private var minZ = 0
    private var maxX = 0
    private var maxZ = 0
    
    private var lastPointerLocation: Location
    private var pointerLocation: Location
    private var pointerDestination: Location? = retrieveOrNull("pointerDestination")
    
    private var drillProgress = retrieveData("drillProgress") { 0.0 }
    private var drilling = retrieveData("drilling") { false }
    private var done = retrieveData("done") { false }
    
    private val energySufficiency: Double
        get() = min(1.0, energy.toDouble() / energyPerTick.toDouble())
    
    private val currentMoveSpeed: Double
        get() = MOVE_SPEED * energySufficiency
    
    private val currentDrillSpeedMultiplier: Double
        get() = DRILL_SPEED_MULTIPLIER * energySufficiency
    
    init {
        setDefaultInventory(inventory)
        y = location.blockY
        updateBounds()
        
        pointerLocation = retrieveOrNull("pointerLocation") ?: Location(world, minX + 1.5, y - 2.0, minZ + 1.5)
        lastPointerLocation = retrieveOrNull("lastPointerLocation") ?: Location(world, 0.0, 0.0, 0.0)
    }
    
    private fun updateBounds(): Boolean {
        val back = getFace(BlockSide.BACK)
        val right = getFace(BlockSide.RIGHT)
        val modX = back.modX.takeUnless { it == 0 } ?: right.modX
        val modZ = back.modZ.takeUnless { it == 0 } ?: right.modZ
        
        val distanceX = modX * (sizeX + 1)
        val distanceZ = modZ * (sizeZ + 1)
        
        minX = min(location.blockX, location.blockX + distanceX)
        minZ = min(location.blockZ, location.blockZ + distanceZ)
        maxX = max(location.blockX, location.blockX + distanceX)
        maxZ = max(location.blockZ, location.blockZ + distanceZ)
        
        updateEnergyPerTick()
        
        if (isRegionProtected()) {
            if (sizeX == MIN_SIZE && sizeZ == MIN_SIZE) {
                runTaskLater(3) { TileEntityManager.destroyAndDropTileEntity(this, true) }
                return false
            } else resize(MIN_SIZE, MIN_SIZE)
        }
        
        return true
    }
    
    private fun resize(sizeX: Int, sizeZ: Int) {
        this.sizeX = sizeX
        this.sizeZ = sizeZ
        
        if (updateBounds()) {
            drilling = false
            drillProgress = 0.0
            done = false
            pointerDestination = null
            pointerLocation = Location(world, minX + 1.5, y - 2.0, minZ + 1.5)
            
            solidScaffolding.removeAllModels()
            armX.removeAllModels()
            armY.removeAllModels()
            armZ.removeAllModels()
            drill.removeAllModels()
            
            createScaffolding()
        }
    }
    
    private fun isRegionProtected(): Boolean {
        val minLoc = Location(world, minX.toDouble(), y.toDouble(), minZ.toDouble())
        val maxLoc = Location(world, maxX.toDouble(), y.toDouble(), maxZ.toDouble())
        var protected = false
        
        minLoc.fullCuboidTo(maxLoc) {
            protected = !ProtectionUtils.canBreak(ownerUUID, it)
            return@fullCuboidTo !protected
        }
        
        return protected
    }
    
    private fun updateEnergyPerTick() {
        energyPerTick = (ENERGY_CONSUMPTION_BASE + (sizeX * sizeZ).toDouble().pow(ENERGY_INEFFICIENCY_EXPONENT)).toInt()
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        if (first) createScaffolding()
    }
    
    override fun saveData() {
        super.saveData()
        storeData("sizeX", sizeX)
        storeData("sizeZ", sizeZ)
        storeData("pointerLocation", pointerLocation)
        storeData("lastPointerLocation", lastPointerLocation)
        storeData("pointerDestination", pointerDestination)
        storeData("drillProgress", drillProgress)
        storeData("drilling", drilling)
        storeData("done", done)
    }
    
    override fun handleTick() {
        if (energy == 0) return
        
        if (!done) {
            if (!drilling) {
                val pointerDestination = pointerDestination ?: selectNextDestination()
                if (pointerDestination != null) {
                    if (pointerLocation.distance(pointerDestination) > 0.2) {
                        moveToPointer(pointerDestination)
                    } else {
                        pointerLocation = pointerDestination.clone()
                        pointerDestination.y -= 1
                        drilling = true
                    }
                } else done = true
            } else drill()
            
            energy = max(0, energy - energyPerTick)
        }
        
        if (hasEnergyChanged) gui.energyBar.update()
    }
    
    private fun moveToPointer(pointerDestination: Location) {
        val deltaX = pointerDestination.x - pointerLocation.x
        val deltaY = pointerDestination.y - pointerLocation.y
        val deltaZ = pointerDestination.z - pointerLocation.z
        
        var moveX = 0.0
        var moveY = 0.0
        var moveZ = 0.0
        
        val moveSpeed = currentMoveSpeed
        
        if (deltaY > 0) {
            moveY = deltaY.coerceIn(-moveSpeed, moveSpeed)
        } else {
            var distance = 0.0
            moveX = deltaX.coerceIn(-moveSpeed, moveSpeed)
            distance += moveX
            moveZ = deltaZ.coerceIn(-(moveSpeed - distance), moveSpeed - distance)
            distance += moveZ
            if (distance == 0.0) moveY = deltaY.coerceIn(-moveSpeed, moveSpeed)
        }
        
        pointerLocation.add(moveX, moveY, moveZ)
        
        updatePointer()
    }
    
    private fun drill() {
        val block = pointerDestination!!.block
        spawnDrillParticles(block)
        
        val drillSpeed = min(DRILL_SPEED_CLAMP, block.type.breakSpeed * currentDrillSpeedMultiplier)
        drillProgress += drillSpeed
        pointerLocation.y -= drillSpeed - max(0.0, drillProgress - 1)
        
        block.setBreakState(entityId, (drillProgress * 9).roundToInt())
        
        if (drillProgress >= 1f) { // is done drilling
            val drops = block.breakAndTakeDrops()
            drops.forEach { drop ->
                val leftover = inventory.addItem(null, drop)
                if (leftover != 0) {
                    drop.amount = leftover
                    world.dropItemNaturally(block.location, drop)
                }
            }
            
            pointerDestination = null
            drillProgress = 0.0
            drilling = false
        }
        
        updatePointer()
    }
    
    private fun updatePointer(force: Boolean = false) {
        if (force || lastPointerLocation.z != pointerLocation.z)
            armX.useArmorStands { it.teleport { z = pointerLocation.z } }
        if (force || lastPointerLocation.x != pointerLocation.x)
            armZ.useArmorStands { it.teleport { x = pointerLocation.x } }
        if (force || lastPointerLocation.x != pointerLocation.x || lastPointerLocation.z != pointerLocation.z)
            armY.useArmorStands { it.teleport { x = pointerLocation.x; z = pointerLocation.z } }
        if (force || lastPointerLocation.y != pointerLocation.y) updateVerticalArmModels()
        
        drill.useArmorStands {
            val location = pointerLocation.clone()
            location.yaw = it.location.yaw.mod(360f)
            if (drilling) location.yaw += 25f * (2 - drillProgress.toFloat())
            else location.yaw += 10f
            it.teleport(location)
        }
        
        lastPointerLocation = pointerLocation.clone()
    }
    
    private fun updateVerticalArmModels() {
        for (y in y - 1 downTo pointerLocation.blockY + 1) {
            val location = pointerLocation.clone()
            location.y = y.toDouble()
            if (!armY.hasModelLocation(location)) armY.addModels(Model(FULL_SLIM_VERTICAL, location))
        }
        armY.removeIf { armorStand, _ -> armorStand.location.blockY - 1 < pointerLocation.blockY }
    }
    
    private fun selectNextDestination(): Location? {
        val destination = LocationUtils.getTopBlocksBetween(
            world,
            minX + 1, 0, minZ + 1,
            maxX - 1, y - 2, maxZ - 1
        )
            .filter { ProtectionUtils.canBreak(ownerUUID, it) && (it.block.type.isBreakable() || TileEntityManager.getTileEntityAt(it) != null) }
            .sortedBy { it.distance(pointerLocation) }
            .maxByOrNull { it.y }
            ?.center()
            ?.apply { y += 1 }
        
        pointerDestination = destination
        return destination
    }
    
    private fun spawnDrillParticles(block: Block) {
        // block cracks
        particleBuilder(ParticleEffect.BLOCK_CRACK, block.location.center().apply { y += 1 }) {
            texture(block.type)
            offsetX(0.2f)
            offsetZ(0.2f)
            speed(0.5f)
        }.display()
        
        // smoke
        particleBuilder(ParticleEffect.SMOKE_NORMAL, pointerLocation.clone().apply { y -= 0.1 }) {
            amount(10)
            speed(0.02f)
        }.display()
    }
    
    private fun createScaffolding() {
        createScaffoldingOutlines()
        createScaffoldingCorners()
        createScaffoldingPillars()
        createScaffoldingArms()
        
        drill.addModels(Model(DRILL, pointerLocation))
        runTaskLater(1) { updatePointer(true) }
    }
    
    private fun createScaffoldingOutlines() {
        val min = Location(location.world, minX.toDouble(), location.y, minZ.toDouble())
        val max = Location(location.world, maxX.toDouble(), location.y, maxZ.toDouble())
        
        min.getRectangle(max, true).forEach { (axis, locations) ->
            locations.forEach { createHorizontalScaffolding(solidScaffolding, it, axis) }
        }
    }
    
    private fun createScaffoldingArms() {
        val baseLocation = pointerLocation.clone().also { it.y = y.toDouble() }
        
        val armXLocations = LocationUtils.getStraightLine(baseLocation, Axis.X, minX..maxX)
        armXLocations.withIndex().forEach { (index, location) ->
            location.x += 0.5
            if (index == 0 || index == armXLocations.size - 1) {
                createSmallHorizontalScaffolding(
                    armX,
                    location.apply { yaw = if (index == 0) 180f else 0f },
                    Axis.X,
                    center = false
                )
            } else {
                createHorizontalScaffolding(armX, location, Axis.X, false)
            }
        }
        
        val armZLocations = LocationUtils.getStraightLine(baseLocation, Axis.Z, minZ..maxZ)
        armZLocations.withIndex().forEach { (index, location) ->
            location.z += 0.5
            if (index == 0 || index == armZLocations.size - 1) {
                createSmallHorizontalScaffolding(armZ,
                    location.apply { yaw = if (index == 0) 0f else 180f },
                    Axis.Z,
                    center = false
                )
            } else {
                createHorizontalScaffolding(armZ, location, Axis.Z, false)
            }
        }
        
        armY.addModels(Model(SLIM_VERTICAL_DOWN, baseLocation.clone()))
    }
    
    private fun createScaffoldingPillars() {
        for (corner in getCornerLocations(location.y)) {
            corner.y -= 1
            
            val blockBelow = corner.getNextBlockBelow(true)
            if (blockBelow != null && blockBelow.positionEquals(corner)) continue
            
            corner
                .getStraightLine(Axis.Y, blockBelow?.blockY?.plus(1) ?: 0)
                .forEach { createVerticalScaffolding(solidScaffolding, it) }
        }
    }
    
    
    private fun createScaffoldingCorners() {
        val y = location.y
        
        val corners = getCornerLocations(y)
            .filterNot { it.blockLocation == location }
            .map { it.center() }
        
        solidScaffolding.addModels(corners.map { Model(CORNER_DOWN, it) })
    }
    
    private fun getCornerLocations(y: Double) =
        listOf(
            Location(world, minX.toDouble(), y, minZ.toDouble()),
            Location(world, maxX.toDouble(), y, minZ.toDouble(), 90f, 0f),
            Location(world, maxX.toDouble(), y, maxZ.toDouble(), 180f, 0f),
            Location(world, minX.toDouble(), y, maxZ.toDouble(), 270f, 0f)
        )
    
    private fun createSmallHorizontalScaffolding(model: MultiModel, location: Location, axis: Axis, center: Boolean = true) {
        location.yaw += if (axis == Axis.Z) 0f else 90f
        model.addModels(Model(SMALL_HORIZONTAL, if (center) location.center() else location))
    }
    
    private fun createHorizontalScaffolding(model: MultiModel, location: Location, axis: Axis, center: Boolean = true) {
        location.yaw = if (axis == Axis.Z) 0f else 90f
        model.addModels(Model(FULL_HORIZONTAL, if (center) location.center() else location))
    }
    
    private fun createVerticalScaffolding(model: MultiModel, location: Location) {
        model.addModels(Model(FULL_VERTICAL, location.center()))
    }
    
    inner class QuarryGUI : TileEntityGUI("menu.nova.quarry") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Quarry,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES))
        ) { openWindow(it) }
        
        private val sizeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # # # # . |" +
                "| # # # . . . . |" +
                "| m n p . . . . |" +
                "| # # # . . . . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('n', NumberDisplayItem { sizeX }.also(sizeItems::add))
            .addIngredient('p', AddNumberItem(MIN_SIZE..MAX_SIZE, { sizeX }, ::setSize).also(sizeItems::add))
            .addIngredient('m', RemoveNumberItem(MIN_SIZE..MAX_SIZE, { sizeX }, ::setSize).also(sizeItems::add))
            .addIngredient('u', UpgradesTeaserItem)
            .build()
            .also { it.fillRectangle(4, 2, 3, inventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4) { Triple(energy, MAX_ENERGY, if (!done) -energyPerTick else 0) }
        
        private fun setSize(size: Int) {
            resize(size, size)
            sizeItems.forEach(Item::notifyWindows)
        }
        
        private inner class NumberDisplayItem(private val getNumber: () -> Int) : BaseItem() {
            
            override fun getItemBuilder(): ItemBuilder {
                val number = getNumber()
                return NovaMaterial.NUMBER.item.getItemBuilder(getNumber())
                    .setLocalizedName(TranslatableComponent("menu.nova.quarry.size", number, number))
                    .addLocalizedLoreLines(localized(ChatColor.GRAY, "menu.nova.quarry.size_tip"))
            }
            
            override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
            
        }
        
    }
    
}