package org.selkie.zea.combat.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.loading.WeaponGroupSpec
import com.fs.starfarer.api.loading.WeaponGroupType

fun makeNewRedirectionDrone(ship: ShipAPI, weapon: WeaponAPI) : ShipAPI {
    return makeNewRedirectionDrone(ship, weapon.spec.weaponId)
}

fun makeNewRedirectionDrone(ship: ShipAPI, weaponID: String): ShipAPI {
    val spec = Global.getSettings().getHullSpec("beam_redirection_drone")
    val v = Global.getSettings().createEmptyVariant("beam_redirection_drone", spec)
    v.addWeapon("WS 000", weaponID)
    val g = WeaponGroupSpec(WeaponGroupType.LINKED)
    g.addSlot("WS 000")
    v.addWeaponGroup(g)

    val redirectionDrone = Global.getCombatEngine().createFXDrone(v)
    val stats = redirectionDrone.mutableStats
    redirectionDrone.layer = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER
    redirectionDrone.owner = ship.originalOwner

    redirectionDrone.isDrone = true
    redirectionDrone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP, 100000.0f, ship)
    redirectionDrone.collisionClass = CollisionClass.NONE
    redirectionDrone.giveCommand(ShipCommand.SELECT_GROUP, null as Any?, 0)

    stats.hullDamageTakenMult.modifyMult("dem", 0.0f)
    stats.energyWeaponDamageMult.applyMods(ship.mutableStats.energyWeaponDamageMult)
    stats.missileWeaponDamageMult.applyMods(ship.mutableStats.missileWeaponDamageMult)
    stats.ballisticWeaponDamageMult.applyMods(ship.mutableStats.ballisticWeaponDamageMult)
    stats.beamWeaponDamageMult.applyMods(ship.mutableStats.beamWeaponDamageMult)
    stats.beamWeaponRangeBonus.applyMods(ship.mutableStats.beamWeaponRangeBonus)
    stats.energyWeaponRangeBonus.applyMods(ship.mutableStats.energyWeaponRangeBonus)
    stats.ballisticWeaponRangeBonus.applyMods(ship.mutableStats.ballisticWeaponRangeBonus)

    Global.getCombatEngine().addEntity(redirectionDrone)

    return redirectionDrone
}
