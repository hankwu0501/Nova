package xyz.xenondevs.nova.util.protection.plugin

import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.util.protection.FakeOnlinePlayer
import xyz.xenondevs.nova.util.protection.ProtectionPlugin

object GriefPrevention : ProtectionPlugin {
    
    private val GRIEF_PREVENTION = if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) GriefPrevention.instance else null
    
    override fun canBreak(player: OfflinePlayer, location: Location) =
        GRIEF_PREVENTION?.allowBreak(FakeOnlinePlayer(player, location.world!!), location.block, location) == null
    
    override fun canPlace(player: OfflinePlayer, location: Location) =
        GRIEF_PREVENTION?.allowBuild(FakeOnlinePlayer(player, location.world!!), location) == null
    
    override fun canUse(player: OfflinePlayer, location: Location) = canBreak(player, location)
}