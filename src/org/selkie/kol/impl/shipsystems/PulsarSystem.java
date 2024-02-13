package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.TerrainSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.combat.activators.IFFOverrideActivator;
import org.selkie.kol.impl.combat.madness.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PulsarSystem extends BaseShipSystemScript implements CombatPulsarRenderer.CombatPulsarRendererDelegate, CombatAuroraRenderer.CombatAuroraRendererDelegate, CombatFlareManager.CombatFlareManagerDelegate {
    //apply, getStatusData, unapply
    public boolean inited = false;
    public ShipAPI ship = null;
    protected State state = null;

    //BaseTerrain
    public static final float EXTRA_SOUND_RADIUS = 100f;

    protected CombatEntityAPI entity;
    protected String terrainId = "Pulsar Wave";
    protected String name = "Pulsar Wave";

    //Terrain plugin
    public float PULSAR_ARC = 90f; //1 / ((float) Math.PI * 2f) * 360f;
    public float PULSAR_LENGTH = 3000f; //1 / ((float) Math.PI * 2f) * 360f;
    public float fxMult = 1f;
    public boolean single = false;
    public String nameTooltip = "Pulsar Wave";

    public String spriteCat = "terrain";
    public String spriteKey = "pulsar";

    protected SpriteAPI flareTexture = Global.getSettings().getSprite(spriteCat, spriteKey);
    protected SpriteAPI auroraTexture = null;
    protected BaseCombatLayeredRenderingPlugin CombatLayer;
    Color color = null;

    protected CombatPulsarRenderer flare1, flare2;
    protected CombatAuroraRenderer renderer;
    protected CombatFlareManager flareManager;
    protected CombatPulsarCorona.CombatCoronaParams params;
    protected CombatRangeBlockerUtil blocker = null; //new CombatRangeBlockerUtil(1400, PULSAR_LENGTH);

    protected float pulsarAngle = 60f;
    protected float pulsarRotation = -1f * (20f);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        this.entity = stats.getEntity();
        this.state = state;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
            init();
        } else {
            return;
        }
        if (!ship.isAlive() || ship.isHulk()) return;

        advance(amount);
        fxMult = 0.2f;
        if (state == State.ACTIVE && !ship.isPhased()) {
            fxMult = 0.65f;
            applyEffect(ship, amount);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        super.unapply(stats, id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return super.getStatusData(index, state, effectLevel);
    }

    public void init() {
        if (!inited) {
            this.params = new CombatPulsarCorona.CombatCoronaParams(PULSAR_LENGTH*2, ship.getCollisionRadius()+50f, ship, 500f, 1f, 0f);
            this.single = false;
            if (blocker == null) {
                blocker = new CombatRangeBlockerUtil(2000, PULSAR_LENGTH*2f);
            }
            name = "The Blizzard";
            params.name = "The Blizzard";
            nameTooltip = "The Blizzard";
            spriteCat = "terrain";
            spriteKey = "pulsar";
            flareTexture = Global.getSettings().getSprite(spriteCat, spriteKey);
            //flareTexture.setAlphaMult(0.1f); //after any sprite changes
            flare1 = new CombatPulsarRenderer(this);
            flare2 = new CombatPulsarRenderer(this);
            Global.getCombatEngine().addLayeredRenderingPlugin(flare1);
            Global.getCombatEngine().addLayeredRenderingPlugin(flare2);
            inited = true;
        }
    }

    public void multiplyArc(float mult) {
        PULSAR_ARC = mult / ((float) Math.PI * 2f) * 360f;
        //if (mult >= 2f) flareTexture = Global.getSettings().getSprite("terrain", "wavefront");
    }

    public String getNameForTooltip() {
        return nameTooltip;
    }

    TerrainSpecAPI getSpec() {
        return null; //TODO replace uses
    }

    protected boolean shouldPlayLoopOne() {
        return getSpec().getLoopOne() != null; //&& containsEntity(Global.getCombatEngine().getPlayerShip());
    }

    protected float getLoopOneVolume() {
        if (Global.getCombatEngine().getPlayerShip() == null) return 0f;
        float intensity = getIntensityAtPoint(Global.getCombatEngine().getPlayerShip().getLocation());
        intensity *= fxMult;
        return intensity;
    }

    protected float getExtraSoundRadius() {
        return 0f;
//		float base = super.getExtraSoundRadius();
//
//		//float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), Global.getSector().getPlayerFleet().getLocation());
//		float extra = 0f;
//
//		return base + extra;
    }


    public float getFXMult() {
        return fxMult;
    }

    public void advance(float amount) {

        pulsarAngle += pulsarRotation * amount * 1f;
        pulsarAngle = Misc.normalizeAngle(pulsarAngle);

        flare1.advance(amount);
        if (!single) flare2.advance(amount);

        flare1.setCurrAngle(pulsarAngle);
        if (!single) flare2.setCurrAngle(pulsarAngle + 180f);

        //pulsarAngle += pulsarRotation * days * 10.25f;
        //pulsarAngle += pulsarRotation * days * 5f;

        if (amount > 0 && blocker != null) {
            blocker.updateLimits(entity, params.relatedEntity, 0.5f);
            //blocker.sync();
            blocker.advance(amount, 100f, 0.5f);
        }

        render(CombatEngineLayers.BELOW_SHIPS_LAYER, Global.getCombatEngine().getViewport());
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (blocker != null && !blocker.wasEverUpdated()) {
            blocker.updateAndSync(entity, params.relatedEntity, 1f);
        }

        if (isNearViewport(pulsarAngle, viewport)) {
            flare1.render(layer, viewport); //viewport.getAlphaMult()
        }
//		else {
//			System.out.println("SKIP1");
//		}

        if (!single && isNearViewport(pulsarAngle + 180f, viewport)) {
            flare2.render(layer, viewport);
        }
//		else {
//			System.out.println("SKIP2");
//		}
    }


    protected boolean isNearViewport(float angle, ViewportAPI viewport) {
        float wClose = getPulsarInnerWidth();
        float wFar = getPulsarOuterWidth();
        float distClose = getPulsarInnerRadius();
        float distFar = getPulsarOuterRadius();

        float length = distFar - distClose;
        float incr = (float) Math.ceil((distFar - distClose) / 2000f);

        for (float dist = wClose; dist < distFar; dist += incr) {
            Vector2f test = Misc.getUnitVectorAtDegreeAngle(angle);
            test.scale(dist);
            Vector2f.add(test, entity.getLocation(), test);

            float testDist = wClose + (wFar - wClose) * (dist - distClose) / length;
            testDist *= 0.5f;
            if (viewport.isNearViewport(test, testDist + 500f)) {
                return true;
            }
        }
        return false;
    }

    protected float getMinRadiusForContains() {
        return params.middleRadius - params.bandWidthInEngine / 2f;
    }

    protected float getMaxRadiusForContains() {
        return params.middleRadius + params.bandWidthInEngine / 2f;
    }

    public float getRenderRange() {
        return params.middleRadius + params.bandWidthInEngine / 2f + 100000f;
        //return getPulsarOuterRadius() + 1000f;
    }

    public boolean containsPoint(Vector2f point, float radius) {
        if (blocker != null && blocker.isAnythingShortened()) {
            float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), point);
            float dist = Misc.getDistance(this.entity.getLocation(), point);
            float max = blocker.getCurrMaxAt(angle);
            if (dist > max) return false;
        }

        if (!Misc.isInArc(pulsarAngle, PULSAR_ARC, entity.getLocation(), point) &&
                (single || !Misc.isInArc(pulsarAngle + 180f, PULSAR_ARC, entity.getLocation(), point))) {
            return false;
        }

        float dist = Misc.getDistance(this.entity.getLocation(), point);
        if (dist < getPulsarInnerRadius()) return false;

        if (dist > getMaxRadiusForContains() + radius) return false;
        if (dist < getMinRadiusForContains() - radius) return false;
        return true;
    }

    public void applyEffect(CombatEntityAPI entity, float amount) {
        if (entity instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) entity;
            //if (ship.getFleetMember() == null || ship.getFleetMember().getFleetData() == null || ship.getFleetMember().getFleetData().getFleet() == null) return;
            //if (ship.getFleetMember().getFleetData().getFleet().hasTag("zea_rulesfortheebutnotforme")) return;

            String buffId = getModId();
            float buffDur = 0.1f;

            for (CombatEntityAPI enemy : AIUtils.getNearbyEnemies(ship, params.bandWidthInEngine/2)) {
                if (enemy instanceof ShipAPI) {
                    ShipAPI tgtShip = (ShipAPI) enemy;
                    if (tgtShip.isPhased()) continue;
                    float intensity = getIntensityAtPoint(tgtShip.getLocation());
                    if (intensity <= 0) return;
                    if (tgtShip.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) intensity = intensity / 2f;
                    tgtShip.getMutableStats().getCRLossPerSecondPercent().modifyMult(getModId(), 3f, "Blizzard");
                    tgtShip.getFluxTracker().increaseFlux(amount*200f*intensity, false);
                    tgtShip.getFluxTracker().increaseFlux(amount, true);
                    // "wind" effect - adjust velocity
                    float maxSpeed = tgtShip.getMaxSpeed();
                    float currSpeed = tgtShip.getVelocity().length();

                    float maxWindBurn = params.windBurnLevel;


                    float currWindBurn = intensity * maxWindBurn;
                    float maxFleetBurnIntoWind = maxSpeed - Math.abs(currWindBurn);

                    float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), tgtShip.getLocation());
                    Vector2f windDir = Misc.getUnitVectorAtDegreeAngle(angle);
                    if (currWindBurn < 0) {
                        windDir.negate();
                    }

                    Vector2f velDir = Misc.normalise(new Vector2f(tgtShip.getVelocity()));
                    velDir.scale(currSpeed);

                    float fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir);

                    float accelMult = 0.5f;
                    if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
                        accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind);
                    }

                    float seconds = amount;

                    Vector2f vel = tgtShip.getVelocity();
                    windDir.scale(seconds * tgtShip.getAcceleration() * accelMult);
                    tgtShip.getVelocity().set(vel.x + windDir.x, vel.y + windDir.y);

                    Color glowColor = getPulsarColorForAngle(angle);
                    int alpha = glowColor.getAlpha();
                    if (alpha < 75) {
                        glowColor = Misc.setAlpha(glowColor, 75);
                    }
                    // visual effects - glow, tail

                    float durIn = 1f;
                    float durOut = 3f;
                    Misc.normalise(windDir);
                    float sizeNormal = 10f + 25f * intensity;
                }
            }

            for (CombatEntityAPI ally : AIUtils.getNearbyAllies(ship, params.bandWidthInEngine/2)) {
                if (ally instanceof ShipAPI) {
                    ShipAPI tgtShip = (ShipAPI) ally;
                    if (tgtShip.isPhased()) continue;
                    float intensity = getIntensityAtPoint(tgtShip.getLocation());
                    if (intensity <= 0) return;
                    if (tgtShip.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
                        intensity = intensity / 2f;
                    }
                    tgtShip.getMutableStats().getCRLossPerSecondPercent().modifyMult(getModId(), 2f, "Blizzard");
                    tgtShip.getFluxTracker().increaseFlux(amount*150f*intensity, false);
                    tgtShip.getFluxTracker().increaseFlux(amount, true);
                    // "wind" effect - adjust velocity
                    float maxSpeed = tgtShip.getMaxSpeed();
                    float currSpeed = tgtShip.getVelocity().length();

                    float maxWindBurn = params.windBurnLevel;


                    float currWindBurn = intensity * maxWindBurn;
                    float maxFleetBurnIntoWind = maxSpeed - Math.abs(currWindBurn);

                    float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), tgtShip.getLocation());
                    Vector2f windDir = Misc.getUnitVectorAtDegreeAngle(angle);
                    if (currWindBurn < 0) {
                        windDir.negate();
                    }

                    Vector2f velDir = Misc.normalise(new Vector2f(tgtShip.getVelocity()));
                    velDir.scale(currSpeed);

                    float fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir);

                    float accelMult = 0.5f;
                    if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
                        accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind);
                    }

                    float seconds = amount;

                    Vector2f vel = tgtShip.getVelocity();
                    windDir.scale(seconds * tgtShip.getAcceleration() * accelMult);
                    tgtShip.getVelocity().set(vel.x + windDir.x, vel.y + windDir.y);

                    Color glowColor = getPulsarColorForAngle(angle);
                    int alpha = glowColor.getAlpha();
                    if (alpha < 75) {
                        glowColor = Misc.setAlpha(glowColor, 75);
                    }
                    // visual effects - glow, tail

                    float durIn = 1f;
                    float durOut = 3f;
                    Misc.normalise(windDir);
                    float sizeNormal = 10f + 25f * intensity;
                }
            }

            for (CombatEntityAPI missile : AIUtils.getNearbyEnemyMissiles(ship, params.bandWidthInEngine/2)) {
                if (missile instanceof MissileAPI) {
                    MissileAPI tgt = (MissileAPI) missile;
                    float intensity = getIntensityAtPoint(tgt.getLocation()) / 2f;
                    if (intensity <= 0) return;

                    if (tgt.getOwner() != ship.getOwner()) {
                        if (tgt.getMissileAI() instanceof GuidedMissileAI) {
                            ((GuidedMissileAI) tgt.getMissileAI()).setTarget(tgt.getSource());
                        } else if (tgt.isGuided()) {
                            tgt.setMissileAI(new IFFOverrideActivator.DummyMissileAI(tgt, tgt.getSource()));
                        } //Salamander sourced missiles

                        tgt.setOwner(ship.getOwner());
                        tgt.setSource(ship);
                    }

                    // "wind" effect - adjust velocity
                    float maxSpeed = tgt.getMaxSpeed();
                    float currSpeed = tgt.getVelocity().length();

                    float maxWindBurn = params.windBurnLevel;

                    float currWindBurn = intensity * maxWindBurn;
                    float maxFleetBurnIntoWind = maxSpeed - Math.abs(currWindBurn);

                    float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), tgt.getLocation());
                    Vector2f windDir = Misc.getUnitVectorAtDegreeAngle(angle);
                    if (currWindBurn < 0) {
                        windDir.negate();
                    }

                    Vector2f velDir = Misc.normalise(new Vector2f(tgt.getVelocity()));
                    velDir.scale(currSpeed);

                    float fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir);

                    float accelMult = 0.5f;
                    if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
                        accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind);
                    }

                    float seconds = amount;

                    Vector2f vel = tgt.getVelocity();
                    windDir.scale(seconds * tgt.getAcceleration() * accelMult);
                    tgt.getVelocity().set(vel.x + windDir.x, vel.y + windDir.y);

                    Color glowColor = getPulsarColorForAngle(angle);
                    int alpha = glowColor.getAlpha();
                    if (alpha < 75) {
                        glowColor = Misc.setAlpha(glowColor, 75);
                    }
                }
            }
        }
    }

    private String getModId() {
        return "PulsarWeapon";
    }

    public float getIntensityAtPoint(Vector2f point) {
        float maxDist = params.bandWidthInEngine;
        float minDist = params.relatedEntity.getCollisionRadius();
        float dist = Misc.getDistance(point, params.relatedEntity.getLocation());

        if (dist > maxDist) return 0f;

        float intensity = 1f;
        if (minDist < maxDist) {
            intensity = 1f - (dist - minDist) / (maxDist - minDist);
            //intensity = 0.5f + intensity * 0.5f;
            if (intensity < 0) intensity = 0;
            if (intensity > 1) intensity = 1;
        }

        float angle = Misc.getAngleInDegreesStrict(params.relatedEntity.getLocation(), point);
        float diff = Misc.getAngleDiff(angle, pulsarAngle);
        if (!single) diff = Math.min(diff, Misc.getAngleDiff(angle, pulsarAngle + 180f));
        float maxDiff = PULSAR_ARC / 2f;
        if (diff > maxDiff) diff = maxDiff;

        if (diff > maxDiff * 0.5f) {
            intensity *= 0.25f + 0.75f * (1f - (diff - maxDiff * 0.5f) / (maxDiff * 0.5f));
        }

        return intensity;
    }

    public Color getNameColor() {
        Color bad = Misc.getNegativeHighlightColor();
        Color base = Color.gray;
        //bad = Color.red;
        return Misc.interpolateColor(base, bad, Global.getSector().getCampaignUI().getSharedFader().getBrightness() * 1f);
    }

    public boolean hasTooltip() {
        return true;
    }

    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        float small = 5f;
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();
        Color fuel = Global.getSettings().getColor("progressBarFuelColor");
        Color bad = Misc.getNegativeHighlightColor();

        tooltip.addTitle(getNameForTooltip());
        tooltip.addPara(Global.getSettings().getDescription(getTerrainName(), Description.Type.TERRAIN).getText1(), pad);

        float nextPad = pad;
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad);
            nextPad = small;
        }
        tooltip.addPara("Reduces the combat readiness of " +
                "all ships caught in the pulsar beam at a rapid pace, and blows the fleet off-course.", nextPad);
        tooltip.addPara("The magnitude of the effect drops off rapidly with distance from the source.", pad);

        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad);
            tooltip.addPara("Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.", small);
        }

        //tooltip.addPara("Does not stack with other similar terrain effects.", pad);
    }

    public boolean isTooltipExpandable() {
        return true;
    }

    public float getTooltipWidth() {
        return 350f;
    }

    public String getTerrainName() {
        return "Pulsar Weapon";
    }

    public float getMaxEffectRadius(Vector2f locFrom) {
        //float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), locFrom);
        float maxDist = params.bandWidthInEngine;
        return maxDist;
    }
    public float getMinEffectRadius(Vector2f locFrom) {
        return 0f;
    }

    public float getOptimalEffectRadius(Vector2f locFrom) {
        return params.relatedEntity.getCollisionRadius();
    }

    public boolean canPlayerHoldStationIn() {
        return false;
    }


    public CombatRangeBlockerUtil getPulsarBlocker() {
        return blocker;
    }

    public Vector2f getPulsarCenterLoc() {
        return params.relatedEntity.getLocation();
    }

    public Color getPulsarColorForAngle(float angle) {
        if (color == null) {
            Color c = Color.white;
            if (params.relatedEntity instanceof PlanetAPI) {
                c = ((PlanetAPI)params.relatedEntity).getSpec().getCoronaColor();
            } else {
                c = Color.white;
            }
            float alpha = 1f;
            color = Misc.setAlpha(c, (int) (200 * alpha));
            return color;
        } else {
            return color;
        }
    }


    public float getPulsarInnerRadius() {
        return params.relatedEntity.getCollisionRadius();
    }


    public float getPulsarOuterRadius() {
        return params.middleRadius + params.bandWidthInEngine * 0.5f;
    }

    public float getPulsarInnerWidth() {
        //PULSAR_ARC = 1f / ((float) Math.PI * 2f) * 360f;
        return PULSAR_ARC / 360f * 2f * (float) Math.PI * getPulsarInnerRadius();
        //return PULSAR_ARC / 360f * 2f * (float) Math.PI * getPulsarInnerRadius();
    }

    public float getPulsarOuterWidth() {
        float r1 = getPulsarInnerRadius();
        float r2 = getPulsarOuterRadius();
        return getPulsarInnerWidth() * r2 / r1;
    }

    public float getPulsarScrollSpeed() {
        if (state == State.ACTIVE) return 150f;
        return 25f;
    }

    public SpriteAPI getPulsarTexture() {
        return flareTexture;
    }

    public float getAuroraAlphaMultForAngle(float angle) {
        return 1f;
    }

    public float getAuroraBandWidthInTexture() {
        return 256f;
        //return 512f;
    }

    public float getAuroraTexPerSegmentMult() {
        return 1f;
        //return 2f;
    }

    public Vector2f getAuroraCenterLoc() {
        return params.relatedEntity.getLocation();
    }

    public Color getAuroraColorForAngle(float angle) {
        if (color == null) {
            if (params.relatedEntity instanceof PlanetAPI) {
                color = ((PlanetAPI)params.relatedEntity).getSpec().getCoronaColor();
                //color = Misc.interpolateColor(color, Color.white, 0.5f);
            } else {
                color = Color.white;
            }
            color = Misc.setAlpha(color, 25);
        }
        if (flareManager.isInActiveFlareArc(angle)) {
            return flareManager.getColorForAngle(color, angle);
        }
        return color;
    }

    public float getAuroraInnerRadius() {
        return params.relatedEntity.getCollisionRadius() + 5f;
    }

    public float getAuroraOuterRadius() {
        return params.middleRadius + params.bandWidthInEngine * 0.5f;
    }

    public float getAuroraShortenMult(float angle) {
        return 0.85f + flareManager.getShortenMod(angle);
    }

    public float getAuroraInnerOffsetMult(float angle) {
        return flareManager.getInnerOffsetMult(angle);
    }

    public SpriteAPI getAuroraTexture() {
        return auroraTexture;
    }

    public CombatRangeBlockerUtil getAuroraBlocker() {
        return blocker;
    }

    public float getAuroraThicknessFlat(float angle) {
//	float shorten = blocker.getShortenAmountAt(angle);
//	if (shorten > 0) return -shorten;
//	if (true) return -4000f;

        if (flareManager.isInActiveFlareArc(angle)) {
            return flareManager.getExtraLengthFlat(angle);
        }
        return 0;
    }

    public float getAuroraThicknessMult(float angle) {
        if (flareManager.isInActiveFlareArc(angle)) {
            return flareManager.getExtraLengthMult(angle);
        }
        return 1f;
    }

    public java.util.List<Color> getFlareColorRange() {
        List<Color> result = new ArrayList<Color>();

        result.add(Color.RED);
        result.add(Color.YELLOW);
        result.add(Color.GREEN);
        result.add(Color.BLUE);
        result.add(Color.MAGENTA);

        /*
        if (params.relatedEntity instanceof ShipAPI) {
            Color color = ((ShipAPI)params.relatedEntity).getHullSpec().getShieldSpec().getRingColor();
            int alpha = (int) (255 * fxMult);
            result.add(Misc.setAlpha(color, alpha));
        } else {
            result.add(Color.white);
        }
        */
        //result.add(Misc.setAlpha(getAuroraColorForAngle(0), 127));
        return result;
    }

    public float getFlareArcMax() {
        return 60;
    }

    public float getFlareArcMin() {
        return 30;
    }

    public float getFlareExtraLengthFlatMax() {
        return 500;
    }

    public float getFlareExtraLengthFlatMin() {
        return 200;
    }

    public float getFlareExtraLengthMultMax() {
        return 1.25f;
    }

    public float getFlareExtraLengthMultMin() {
        return 1.1f;
    }

    public float getFlareFadeInMax() {
        return 10f;
    }

    public float getFlareFadeInMin() {
        return 3f;
    }

    public float getFlareFadeOutMax() {
        return 10f;
    }

    public float getFlareFadeOutMin() {
        return 3f;
    }

    public float getFlareOccurrenceAngle() {
        return 0;
    }

    public float getFlareOccurrenceArc() {
        return 360f;
    }

    public float getFlareProbability() {
        return params.flareProbability;
    }

    public float getFlareSmallArcMax() {
        return 20;
    }

    public float getFlareSmallArcMin() {
        return 10;
    }

    public float getFlareSmallExtraLengthFlatMax() {
        return 100;
    }

    public float getFlareSmallExtraLengthFlatMin() {
        return 50;
    }

    public float getFlareSmallExtraLengthMultMax() {
        return 1.05f;
    }

    public float getFlareSmallExtraLengthMultMin() {
        return 1;
    }

    public float getFlareSmallFadeInMax() {
        return 2f;
    }

    public float getFlareSmallFadeInMin() {
        return 1f;
    }

    public float getFlareSmallFadeOutMax() {
        return 2f;
    }

    public float getFlareSmallFadeOutMin() {
        return 1f;
    }

    public float getFlareShortenFlatModMax() {
        return 0.05f;
    }

    public float getFlareShortenFlatModMin() {
        return 0.05f;
    }

    public float getFlareSmallShortenFlatModMax() {
        return 0.05f;
    }

    public float getFlareSmallShortenFlatModMin() {
        return 0.05f;
    }

    public int getFlareMaxSmallCount() {
        return 0;
    }

    public int getFlareMinSmallCount() {
        return 0;
    }

    public float getFlareSkipLargeProbability() {
        return 0f;
    }

    public CombatEntityAPI getFlareCenterEntity() {
        return this.entity;
    }
}
