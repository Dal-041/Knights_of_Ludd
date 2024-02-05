package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class SubspaceGlideStats extends BaseShipSystemScript {
    private static final Color PHASE_COLOR = new Color(70, 90, 240, 230);
    private static final float SHIP_ALPHA_MULT = 0.50f;
    public static float SPEED_BONUS = 100f;
    public static float TURN_BONUS = 20f;
    public float lastHardflux = 0f;
    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();
    protected Object STATUSKEY3 = new Object();
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
            stats.getAcceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
            stats.getDeceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, TURN_BONUS * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, TURN_BONUS * 5f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);
        }

        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();

            stats.getFluxDissipation().modifyMult(id, Misc.interpolate(1f, 1.5f, effectLevel));

            // halve hardflux diss, accounting for 1.5x boost to soft flux
            if(ship.getFluxTracker().getHardFlux() >= lastHardflux) {
                lastHardflux = ship.getFluxTracker().getHardFlux();
            }
            else{
                float hardFlux = (ship.getFluxTracker().getHardFlux() + lastHardflux*2)/3;
                lastHardflux = hardFlux;
                if(ship.getFluxTracker().getCurrFlux() < hardFlux) ship.getFluxTracker().setCurrFlux(hardFlux);
                ship.getFluxTracker().setHardFlux(hardFlux);
            }


            ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);
            ship.setApplyExtraAlphaToEngines(true);

            Color colorToUse = new Color(((float) PHASE_COLOR.getRed() / 255f), ((float) PHASE_COLOR.getGreen() / 255f), ((float) PHASE_COLOR.getBlue() / 255f), ((float) PHASE_COLOR.getAlpha() / 255f) * effectLevel);
            Vector2f jitterLocation = MathUtils.getRandomPointInCircle(ship.getLocation(), 2f+(1-effectLevel)*5f);
            SpriteAPI glow1 = Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow1");
            SpriteAPI glow2 = Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow2");
            MagicRender.singleframe(glow1, ship.getLocation(), new Vector2f(glow1.getWidth(), glow1.getHeight()), ship.getFacing() - 90f, colorToUse, true);
            MagicRender.singleframe(glow2, jitterLocation, new Vector2f(glow2.getWidth(), glow2.getHeight()), ship.getFacing() - 90f, colorToUse, true);

            CombatEngineAPI engine = Global.getCombatEngine();
            if(engine.getPlayerShip() == ship){
                engine.maintainStatusForPlayerShip(STATUSKEY1, ship.getSystem().getSpecAPI().getIconSpriteName(), "+" + 50 + "% soft flux dissipation", "-" + 50 + "% hard flux dissipation", true);
                engine.maintainStatusForPlayerShip(STATUSKEY2, ship.getSystem().getSpecAPI().getIconSpriteName(), "Subphase Glide", "+" + (int)SPEED_BONUS + " top speed" , false);
            }

        }

    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getHardFluxDissipationFraction().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        lastHardflux = 0f;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}
