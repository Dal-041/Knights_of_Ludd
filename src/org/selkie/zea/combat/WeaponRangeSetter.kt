package org.selkie.zea.combat

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier

class WeaponRangeSetter : WeaponRangeModifier {


    data class WeaponRangeModData(
        var BaseRange: Float,
        var TargetRange: Float,
        var ModEnabled: Boolean,
    )

    companion object{
        val MAP_KEY = "WeaponRangeModifierMap"
        fun initMap(ship: ShipAPI){
            if(!ship.customData.contains(MAP_KEY)) {
                val newMap = HashMap<WeaponAPI, WeaponRangeModData>()
                ship.setCustomData(MAP_KEY, newMap)
            }
        }
    }



    override fun getWeaponRangeMultMod(ship: ShipAPI, weapon: WeaponAPI): Float {
        initMap(ship)
        val weaponRangeMap: HashMap<WeaponAPI, WeaponRangeModData> = ship.customData[MAP_KEY] as HashMap<WeaponAPI, WeaponRangeModData>

        if(weapon in weaponRangeMap){
            val weaponRangeModData = weaponRangeMap[weapon]!!
            return if(weaponRangeModData.ModEnabled) weaponRangeModData.TargetRange/weaponRangeModData.BaseRange else 1f
        } else return 1f
    }

    override fun getWeaponRangeFlatMod(ship: ShipAPI, weapon: WeaponAPI): Float {
        return 0f
    }

    override fun getWeaponRangePercentMod(ship: ShipAPI, weapon: WeaponAPI): Float {
        return 0f
    }
}