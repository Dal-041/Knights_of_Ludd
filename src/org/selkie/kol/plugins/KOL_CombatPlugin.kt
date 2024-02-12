package org.selkie.kol.plugins

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.Utils
import org.selkie.kol.combat.ParticleController
import org.selkie.kol.combat.StarficzAIUtils
import java.awt.Color
import java.util.regex.Pattern

class KOL_CombatPlugin : BaseEveryFrameCombatPlugin() {
    private val MAX_RANGE = 250f;

    private val PAPERDOLL_SCALE: Map<String, Float> = mapOf("kol_lunaria" to 0.455f, "kol_alysse" to 0.615f,
        "kol_larkspur" to 0.66f, "kol_tamarisk" to 0.565f, "kol_lotus" to 0.635f,
        "kol_snowdrop" to 0.75f, "kol_marigold" to 0.64f,"kol_mimosa" to 0.555f, "kol_sundew" to 0.61f)

    override fun init(engine: CombatEngineAPI) {
        engine.addLayeredRenderingPlugin(ParticleController())
    }

    override fun renderInUICoords(viewport: ViewportAPI?) {
        super.renderInUICoords(viewport)
        if(viewport == null) return
        if(Global.getCurrentState() != GameState.COMBAT) return
        val engine = Global.getCombatEngine()
        val ship = engine.playerShip ?: return

        if (!ship.isAlive) return

        //TODO, also draw modlues on the target paperdoll
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
        val center = Vector2f(120f, 120f) //TODO: make sure this doesnt change across ui scales
        drawPaperdoll(viewport, ship, center, 1f)
    }

    override fun advance(amount: Float, events: List<InputEventAPI?>?) {

    }

    private fun drawPaperdoll(viewport: ViewportAPI, ship: ShipAPI, location: Vector2f, scale: Float){
        val kolPattern = Pattern.compile("kol_.+?_[tml][lr]", Pattern.CASE_INSENSITIVE)
        if (PAPERDOLL_SCALE.containsKey(ship.hullSpec.hullId) ) {
            val shipScale = PAPERDOLL_SCALE[ship.hullSpec.hullId]!! * scale;
            val alpha = Math.round(Misc.interpolate(0f,255f, Utils.getUIAlpha(true)))
            for (module in ship.childModulesCopy) {
                if (!module.isAlive) continue

                var moduleSprite = Global.getSettings().getSprite(module.hullSpec.spriteName)
                val matcher = kolPattern.matcher(module.hullSpec.spriteName)
                if (matcher.find()) {
                    moduleSprite = Global.getSettings().getSprite("paperdolls", matcher.group())
                }

                val offset = Vector2f()
                Vector2f.sub(ship.location, module.location, offset)
                offset.scale(shipScale)
                val paperDollLocation = Vector2f()
                Vector2f.sub(location, offset, paperDollLocation)

                val armorHullLevel = (StarficzAIUtils.getWeakestTotalArmor(module) + module.hitpoints) / (module.armorGrid.armorRating + module.maxHitpoints)
                val paperdollColor = Utils.OKLabInterpolateColor(Color(255, 30, 30, alpha), Color(150, 200, 255, alpha), armorHullLevel)
                /*
                MagicRender.screenspace(
                    moduleSprite, MagicRender.positioning.LOW_LEFT, paperDollLocation, Misc.ZERO, Vector2f(moduleSprite.width * shipScale, moduleSprite.height * shipScale),
                    Misc.ZERO, ship.facing - 90f, 0f, paperdollColor, false, -1f, -1f, -1f
                )
                */
                moduleSprite.setSize(moduleSprite.width * shipScale, moduleSprite.height * shipScale)
                moduleSprite.color = paperdollColor;
                moduleSprite.setAdditiveBlend();
                //moduleSprite.setBlendFunc(GL11.GL_ONE, GL11.GL_ZERO)
                moduleSprite.angle = ship.facing - 90f
                moduleSprite.renderAtCenter(paperDollLocation.x, paperDollLocation.y)
            }
        }
    }
}
