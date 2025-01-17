package xyz.xenondevs.nova.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object MobDuplicatorAdvancement : Advancement(NamespacedKey(NOVA, "mob_duplicator")) {
    
    init {
        setParent(MobKillerAdvancement.key)
        addObtainCriteria(NovaMaterial.MOB_DUPLICATOR)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.MOB_DUPLICATOR.toIcon())
        }
    }
    
}