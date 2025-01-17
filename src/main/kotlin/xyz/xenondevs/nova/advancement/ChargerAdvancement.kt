package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object ChargerAdvancement : Advancement(NamespacedKey(NOVA, "charger")) {
    
    init { 
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(NovaMaterial.CHARGER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.CHARGER.toIcon())
        }
    }
    
}