package org.selkie.kol.impl.plugins;

import java.awt.Color;
import java.io.IOException;
import java.lang.String;
import java.util.EnumSet;
import java.util.Random;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.systems.*;
import com.fs.starfarer.renderers.JitterRenderer;
import org.lwjgl.util.vector.Vector2f;

public class kol_phasecloak extends BaseShipSystemScript implements CombatLayeredRenderingPlugin {

    public static Color JITTER_COLOR = new Color(80,160,240,255);
    public static float JITTER_FADE_TIME = 0.5f;

    public static float SHIP_ALPHA_MULT = 0.25f;
    //public static float VULNERABLE_FRACTION = 0.875f;
    public static float VULNERABLE_FRACTION = 0f;
    public static float INCOMING_DAMAGE_MULT = 0.25f;

    public static float MAX_TIME_MULT = 3f;

    public static boolean FLUX_LEVEL_AFFECTS_SPEED = false;
    public static float MIN_SPEED_MULT = 0.5f;
    public static float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 1f;

    protected Boolean addedRenderer = false;
    protected ShipAPI shipEntity;
    protected State sysState;
    protected float sysLevel;

    public SpriteAPI sprHighlight;
    public SpriteAPI sprDiffuse;

    Color colHighlight = new Color(80,160,240);
    Color colDiffuse = new Color(5,120,180);

    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();
    protected Object STATUSKEY3 = new Object();
    protected Object STATUSKEY4 = new Object();

    public static float getMaxTimeMult(MutableShipStatsAPI stats) {
        return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
    }

    protected boolean isDisruptable(ShipSystemAPI cloak) {
        return cloak.getSpecAPI().hasTag(Tags.DISRUPTABLE);
    }

    protected float getDisruptionLevel(ShipAPI ship) {
        //return disruptionLevel;
        //if (true) return 0f;
        if (FLUX_LEVEL_AFFECTS_SPEED) {
            float threshold = ship.getMutableStats().getDynamic().getMod(
                    Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
            if (threshold <= 0) return 1f;
            float level = ship.getHardFluxLevel() / threshold;
            if (level > 1f) level = 1f;
            return level;
        }
        return 0f;
    }

    protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
        float level = effectLevel;
        float f = VULNERABLE_FRACTION;

        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;

        if (level > f) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
        } else {

        }

        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (level > f) {
                if (getDisruptionLevel(playerShip) <= 0f) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                            cloak.getSpecAPI().getIconSpriteName(), "phase coils stable", "top speed at 100%", false);
                } else {
                    //String disruptPercent = "" + (int)Math.round((1f - disruptionLevel) * 100f) + "%";
                    //String speedMultStr = Strings.X + Misc.getRoundedValue(getSpeedMult());
                    String speedPercentStr = (int) Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
                    Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                            cloak.getSpecAPI().getIconSpriteName(),
                            //"phase coils at " + disruptPercent,
                            "phase coil stress",
                            "top speed at " + speedPercentStr, true);
                }
            }
        }
    }

    public float getSpeedMult(ShipAPI ship, float effectLevel) {
        if (getDisruptionLevel(ship) <= 0f) return 1f;
        return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            shipEntity = ship;
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (player) {
            maintainStatus(ship, state, effectLevel);
        }

        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak == null) return;

        if (!addedRenderer) {
            Global.getCombatEngine().addLayeredRenderingPlugin(this);
            addedRenderer = true;
        }
        sysState = state;
        sysLevel = effectLevel;

        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (state == State.ACTIVE || state == State.OUT || state == State.IN) {
                float mult = getSpeedMult(ship, effectLevel);
                if (mult < 1f) {
                    stats.getMaxSpeed().modifyMult(id + "_2", mult);
                } else {
                    stats.getMaxSpeed().unmodifyMult(id + "_2");
                }
                ((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
            }
        }

        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }

        float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
        float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
        stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
        stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
        stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

        float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
        float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
        stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
        stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
        stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

        float level = effectLevel;
        float f = VULNERABLE_FRACTION;



        float jitterLevel = 0f;
        float jitterRangeBonus = 0f;
        float levelForAlpha = level;

//		ShipSystemAPI cloak = ship.getPhaseCloak();
//		if (cloak == null) cloak = ship.getSystem();


        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = level;

        } else if (state == State.OUT) {
            if (level > 0.5f) {
                ship.setPhased(true);
            } else {
                ship.setPhased(false);
            }
            levelForAlpha = level;
//			if (level >= f) {
//				ship.setPhased(true);
//				if (f >= 1) {
//					levelForAlpha = level;
//				} else {
//					levelForAlpha = (level - f) / (1f - f);
//				}
//				float time = cloak.getChargeDownDur();
//				float fadeLevel = JITTER_FADE_TIME / time;
//				if (level >= f + fadeLevel) {
//					jitterLevel = 0f;
//				} else {
//					jitterLevel = (fadeLevel - (level - f)) / fadeLevel;
//				}
//			} else {
//				ship.setPhased(false);
//				levelForAlpha = 0f;
//
//				float time = cloak.getChargeDownDur();
//				float fadeLevel = JITTER_FADE_TIME / time;
//				if (level < fadeLevel) {
//					jitterLevel = level / fadeLevel;
//				} else {
//					jitterLevel = 1f;
//				}
//				//jitterLevel = level / f;
//				//jitterLevel = (float) Math.sqrt(level / f);
//			}
        }

//		ship.setJitter(JITTER_COLOR, jitterLevel, 1, 0, 0 + jitterRangeBonus);
//		ship.setJitterUnder(JITTER_COLOR, jitterLevel, 11, 0f, 7f + jitterRangeBonus);
        //ship.getEngineController().fadeToOtherColor(this, spec.getEffectColor1(), new Color(0,0,0,0), jitterLevel, 1f);
        //ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);

        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        ship.setApplyExtraAlphaToEngines(true);


        //float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * levelForAlpha;
        float extra = 0f;
//		if (isDisruptable(cloak)) {
//			extra = disruptionLevel;
//		}
        float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha * (1f - extra);
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship = null;
        //boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            //player = ship == Global.getCombatEngine().getPlayerShip();
            //id = id + "_" + ship.getId();
        } else {
            return;
        }

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxSpeed().unmodifyMult(id + "_2");
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);

        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);

        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak != null) {
            ((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(0f);
        }

//		stats.getMaxSpeed().unmodify(id);
//		stats.getMaxTurnRate().unmodify(id);
//		stats.getTurnAcceleration().unmodify(id);
//		stats.getAcceleration().unmodify(id);
//		stats.getDeceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
//		if (index == 0) {
//			return new StatusData("time flow altered", false);
//		}
        return null;
    }

    @Override
    public void init(CombatEntityAPI entity) {
        if (entity instanceof ShipAPI) {
            shipEntity = (ShipAPI) entity;
        } else if (shipEntity == null) {
            return;
        }
        String pathOne = shipEntity.getHullSpec().getSpriteName().replace(".png", "_glow1.png");
        String pathTwo = shipEntity.getHullSpec().getSpriteName().replace(".png", "_glow2.png");
        //colHighlight = shipEntity.getSystem().getSpecAPI().getEffectColor1();
        //colDiffuse = shipEntity.getSystem().getSpecAPI().getEffectColor2();
        try {
            Global.getSettings().loadTexture(pathOne);
            Global.getSettings().loadTexture(pathTwo);
        } catch (IOException e) {

        }

        sprHighlight = Global.getSettings().getSprite(pathOne);
        sprDiffuse = Global.getSettings().getSprite(pathTwo);
    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        //Bootleg obf reimplementation

        //Haha... ha... ha... it was just stuck the whole time
        /*
        if (sprHighlight == null || sprDiffuse == null) return;

        Vector2f location = shipEntity.getLocation();

        float cdTotal; //var6
        float cdPerc; //var8
        float jit; //var9
        //ShipSystemAPI sys = shipEntity.getSystem();
        if (layer == CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER) {
            //ShipSystemAPI.SystemState state = shipEntity.getSystem().getState(); //var4
            float sqrtCDperc = 0.0F;
            float cdRemain; //var7
            if (sysState == State.COOLDOWN || sysState == State.IDLE) {
                cdTotal = shipEntity.getSystem().getCooldown();
                cdRemain = shipEntity.getSystem().getCooldownRemaining();
                if (cdTotal <= 0.0F) {
                    return;
                }

                cdPerc = cdRemain / cdTotal; //var8
                cdPerc = (float)Math.sqrt((double)cdPerc);
                sqrtCDperc = cdPerc; //var5
            } else if (sysState == State.OUT) {
                cdTotal = sysLevel;
                cdRemain = 1.0F;
                if (cdTotal > cdRemain) {
                    sqrtCDperc = 0.0F;
                } else {
                    sqrtCDperc = 1.0F - cdTotal / cdRemain;
                }
            }

            cdTotal = Math.max(0, 1.0F - sqrtCDperc);
            cdRemain = Math.max(sqrtCDperc * 0.33F, cdTotal * 0.67F); // this.null.Object * 0.67F ????
            if (cdTotal > 0.0F) {
                cdPerc = shipEntity.getAlphaMult() * (1f/13f); // * The mysterious float param var3, probably frame interval
                sprHighlight.setAdditiveBlend();
                sprDiffuse.setAdditiveBlend();
                sprDiffuse.setAlphaMult(cdPerc * cdRemain * shipEntity.getExtraAlphaMult2());
                sprHighlight.setAlphaMult(cdPerc * cdRemain * shipEntity.getExtraAlphaMult2());
                sprDiffuse.setAngle(shipEntity.getFacing() - 90.0F);
                sprHighlight.setAngle(shipEntity.getFacing() - 90.0F);
                sprHighlight.setColor(colHighlight);
                sprDiffuse.setColor(colDiffuse);
                jit = 10.0F * sysLevel * cdTotal; //default 20F which is SO MUCH JITTER
                if (sysState == State.OUT) {
                    jit = 0.0F;
                }

                jitter(sprDiffuse, location.x, location.y, 0.0f, jit, 15); //JitterRenderer
                jitter(sprHighlight, location.x, location.y, 0.0f, jit, 5);
            }
        }
        */

        /* Backup code
        Vector2f location = shipEntity.getLocation();

        sprHighlight.setColor(colHighlight);
        sprHighlight.setColor(colDiffuse);

        sprHighlight.setAdditiveBlend();
        sprDiffuse.setAdditiveBlend();

        sprHighlight.setAngle(shipEntity.getFacing() + 270);
        sprHighlight.setAlphaMult(0.8f);
        sprHighlight.renderAtCenter(location.x, location.y);

        sprDiffuse.setAngle(shipEntity.getFacing() + 270);
        sprDiffuse.setAlphaMult(0.8f);
        sprDiffuse.renderAtCenter(location.x, location.y);
        */
    }

    public void jitter(SpriteAPI sprite, float x, float y, float range, float intensity, int copies) {
        Random random = new Random();

        for(int var7 = 0; var7 < copies; ++var7) {
            Vector2f var8 = new Vector2f();
            if (false) {
                float var9 = range + (intensity - range) * random.nextFloat();
                var8 = Misc.getPointAtRadius(var8, var9, random);
            } else if (range <= 0.0F) {
                var8.x = random.nextFloat() * intensity - intensity / 2.0F;
                var8.y = random.nextFloat() * intensity - intensity / 2.0F;
            } else {
                var8.x = random.nextFloat() * (intensity - range) + range;
                var8.y = random.nextFloat() * (intensity - range) + range;
                if (var8.x < range) {
                    var8.x = range;
                }

                if (var8.y < range) {
                    var8.y = range;
                }

                var8.x *= Math.signum(random.nextFloat() - 0.5F);
                var8.y *= Math.signum(random.nextFloat() - 0.5F);
            }

            sprite.renderAtCenter(x + var8.x, y + var8.y);
        }
    }
}
