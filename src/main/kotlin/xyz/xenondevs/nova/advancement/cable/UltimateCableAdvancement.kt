package xyz.xenondevs.nova.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object UltimateCableAdvancement : Advancement(NamespacedKey(NOVA, "ultimate_cable")) {
    
    init {
        setParent(EliteCableAdvancement.key)
        addObtainCriteria(NovaMaterial.ULTIMATE_CABLE)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.ULTIMATE_CABLE.toIcon())
        }
    }
    
}