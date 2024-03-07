package org.selkie.kol.plugins

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.setAlpha
import org.magiclib.util.MagicUI
import org.selkie.kol.Utils
import org.selkie.kol.combat.StarficzAIUtils
import java.awt.Color
import java.util.regex.Pattern

class KOL_ArmorPaperdolls : BaseEveryFrameCombatPlugin() {
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return
        val engine = Global.getCombatEngine()
        val ship = engine.playerShip ?: return

        if (!ship.isAlive) return

        //TODO: draw modules on the target preview paperdoll
        /*
        val target = engine.combatUI.mainTargetReticleTarget
        if(target != null){
            engine.addSmoothParticle(target.location, Misc.ZERO, 50f, 50f, 0.1f, Color.red);
            val targetLocation = Vector2f(viewport.convertWorldXtoScreenX(target.location.x), viewport.convertWorldYtoScreenY(target.location.y))
            Vector2f.add(targetLocation, Vector2f(200f,-100f), targetLocation)
            viewport.viewMult
            drawPaperdoll(viewport, target, targetLocation, 0.5f)

        }
        */

        // draw paperdoll for armor
        val center = Vector2f(120f, 120f).scale(Global.getSettings().screenScaleMult) as Vector2f
        MagicUI.openGLForMiscWithinViewport()
        drawPaperdoll(ship, center, Global.getSettings().screenScaleMult)
        MagicUI.closeGLForMiscWithinViewport()
    }

    //TODO: Hardcoded bullshit, no clue how alex scales sprites here.
    private val PAPERDOLL_SCALE: Map<String, Float> = mapOf("kol_lunaria" to 0.455f, "kol_alysse" to 0.615f,
        "kol_larkspur" to 0.66f, "kol_tamarisk" to 0.565f, "kol_lotus" to 0.635f,
        "kol_snowdrop" to 0.75f, "kol_marigold" to 0.64f,"kol_mimosa" to 0.555f, "kol_sundew" to 0.61f)

    private val noHPColor = Color(200, 30, 30, 255)
    private val fullHPColor = Color(120, 230, 0, 255)

    private fun drawPaperdoll(ship: ShipAPI, location: Vector2f, scale: Float){

        if (PAPERDOLL_SCALE.containsKey(ship.hullSpec.baseHullId) ) {
            val shipScale = PAPERDOLL_SCALE[ship.hullSpec.baseHullId]!! * scale;
            val alpha = Math.round(Misc.interpolate(0f,170f, Utils.getUIAlpha(false)))
            val kolPattern = Pattern.compile("kol_.+?_[tml][lr]", Pattern.CASE_INSENSITIVE)
            for (module in ship.childModulesCopy) {
                if (module.hitpoints <= 0f) continue

                var moduleSprite: SpriteAPI
                // use regex matching to figure out the correct paperdoll sprite
                val matcher = kolPattern.matcher(module.hullSpec.spriteName)
                if (matcher.find()) {
                    moduleSprite = Global.getSettings().getSprite("paperdolls", matcher.group())
                } else{
                    // back up use the default ship sprite
                    moduleSprite = Global.getSettings().getSprite(module.hullSpec.spriteName);
                }

                val offset: Vector2f = Vector2f.sub(ship.location, module.location, null).scale(shipScale) as Vector2f
                val paperDollLocation = Vector2f.sub(location, offset, null)

                val armorHullLevel = (StarficzAIUtils.getCurrentArmorRating(module) + module.hitpoints) / (module.armorGrid.armorRating + module.maxHitpoints)
                val paperdollColor = Utils.OKLabInterpolateColor(noHPColor.setAlpha(alpha), fullHPColor.setAlpha(alpha), armorHullLevel)

                moduleSprite.setSize(moduleSprite.width * shipScale, moduleSprite.height * shipScale)
                moduleSprite.color = paperdollColor
                //moduleSprite.setAdditiveBlend()
                moduleSprite.angle = ship.facing - 90f
                moduleSprite.renderAtCenter(paperDollLocation.x, paperDollLocation.y)
            }
        }
    }
}
