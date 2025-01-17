package xyz.xenondevs.nova.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.RootCommandNode
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import org.bukkit.Location
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer
import xyz.xenondevs.nova.command.COMMAND_DISPATCHER
import xyz.xenondevs.nova.util.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.ReflectionUtils.getField
import xyz.xenondevs.nova.util.ReflectionUtils.getMethod
import xyz.xenondevs.nova.util.ReflectionUtils.getVersion

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object ReflectionRegistry {
    
    // Path and version
    val CB_PACKAGE_PATH = getCB()
    val VERSION = getVersion()
    
    // CB classes
    val CB_CRAFT_SERVER_CLASS = getCBClass("CraftServer")
    val CB_CRAFT_ENTITY_CLASS = getCBClass("entity.CraftEntity")
    val CB_CRAFT_WORLD_CLASS = getCBClass("CraftWorld")
    val CB_CRAFT_ITEM_STACK_CLASS = getCBClass("inventory.CraftItemStack")
    val CB_CRAFT_COMMAND_MAP_CLASS = getCBClass("command.CraftCommandMap")
    val CB_CRAFT_META_ITEM_CLASS = getCBClass("inventory.CraftMetaItem")
    
    // CB methods
    val CB_CRAFT_SERVER_SYNC_COMMANDS_METHOD = getMethod(CB_CRAFT_SERVER_CLASS, true, "syncCommands")
    val CB_CRAFT_ENTITY_GET_HANDLE_METHOD = getMethod(CB_CRAFT_ENTITY_CLASS, false, "getHandle")
    val CB_CRAFT_WORLD_GET_HANDLE_METHOD = getMethod(CB_CRAFT_WORLD_CLASS, false, "getHandle")
    val CB_CRAFT_WORLD_CREATE_ENTITY_METHOD = getMethod(CB_CRAFT_WORLD_CLASS, false, "createEntity", Location::class.java, Class::class.java)
    val CB_CRAFT_WORLD_ADD_ENTITY_METHOD = getMethod(CB_CRAFT_WORLD_CLASS, false, "addEntity", Entity::class.java, SpawnReason::class.java, Consumer::class.java)
    val CB_CRAFT_ITEM_STACK_AS_NMS_COPY_METHOD = getMethod(CB_CRAFT_ITEM_STACK_CLASS, false, "asNMSCopy", ItemStack::class.java)
    val CB_CRAFT_SERVER_GET_COMMAND_MAP_METHOD = getMethod(CB_CRAFT_SERVER_CLASS, false, "getCommandMap")
    val CB_CRAFT_COMMAND_MAP_GET_COMMAND_METHOD = getMethod(CB_CRAFT_COMMAND_MAP_CLASS, false, "getCommand", String::class.java)
    val CB_CRAFT_META_APPLY_TO_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "applyToItem", CompoundTag::class.java)
    
    // NMS fields
    val ARMOR_STAND_ARMOR_ITEMS_FIELD = getField(ArmorStand::class.java, true, "cd")
    
    // CB fields
    val CB_CRAFT_META_ITEM_INTERNAL_TAG_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "internalTag")
    val CB_CRAFT_META_ITEM_DISPLAY_NAME_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "displayName")
    val CB_CRAFT_META_ITEM_LORE_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "lore")
    
    // other fields
    val COMMAND_DISPATCHER_ROOT_FIELD = getField(CommandDispatcher::class.java, true, "root")
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class.java, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class.java, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class.java, true, "arguments")
    
    // objects
    val COMMAND_DISPATCHER_ROOT_NODE = COMMAND_DISPATCHER_ROOT_FIELD.get(COMMAND_DISPATCHER)!! as RootCommandNode<Any>
    
}
