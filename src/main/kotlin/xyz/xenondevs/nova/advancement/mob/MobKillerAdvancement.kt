package xyz.xenondevs.nova.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object MobKillerAdvancement : Advancement(NamespacedKey(NOVA, "mob_killer")) {
    
    init {
        setParent(BreederAdvancement.key)
        addObtainCriteria(NovaMaterial.MOB_KILLER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.MOB_KILLER.toIcon())
        }
    }
    
}