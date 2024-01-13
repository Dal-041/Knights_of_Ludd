package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.magiclib.plugins.MagicTrailPlugin;
import org.selkie.kol.impl.fx.FakeSmokePlugin;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SlipDrive extends BaseShipSystemScript {
  public static Color AFTERIMAGE_COLOR = new Color(0.25F, 0.05F, 0.4F, 0.3F);
  
  public static float SHADOW_DELAY = 0.05F;
  
  public static float SHADOW_ANGLE_DIFFERENCE = 8.0F;
  
  public static float SHADOW_DISTANCE_DIFFERENCE = 45.0F;
  
  public static float SHADOW_FLICKER_DIFFERENCE = 8.0F;
  
  public static int SHADOW_FLICKER_CLONES = 4;
  
  private Vector2f shadowPos = new Vector2f(0.0F, 0.0F);
  
  private Vector2f startPos = new Vector2f(0.0F, 0.0F);
  
  private float shadowDelayCounter = 0.0F;
  
  private boolean runOnce = true;
  
  private float globalCounter = 0.0F;
  
  public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
    ShipAPI ship;
    if (Global.getCombatEngine().isPaused())
      return; 
    if (stats.getEntity() instanceof ShipAPI) {
      ship = (ShipAPI)stats.getEntity();
      boolean player = (ship == Global.getCombatEngine().getPlayerShip());
      id = id + "_" + ship.getId();
    } else {
      return;
    } 
    if (effectLevel > 0.5F && this.runOnce) {
      this.shadowPos.x = ship.getLocation().getX();
      this.shadowPos.y = ship.getLocation().getY();
      this.startPos.x = ship.getLocation().getX();
      this.startPos.y = ship.getLocation().getY();
      this.runOnce = false;
    } 
    if (state == ShipSystemStatsScript.State.IN) {
      stats.getMaxSpeed().modifyFlat(id, 100f);
      stats.getMaxTurnRate().modifyFlat(id, 100f);
      stats.getTurnAcceleration().modifyFlat(id, 100f);
      stats.getAcceleration().modifyFlat(id, 500.0F);
      stats.getDeceleration().modifyFlat(id, 500.0F);
      return;
    } 
    if (effectLevel >= 1.0F)
      this.globalCounter += Global.getCombatEngine().getElapsedInLastFrame(); 
    float shadowProgress = this.globalCounter / ship.getSystem().getChargeActiveDur();
    MagicTrailPlugin.cutTrailsOnEntity((CombatEntityAPI)ship);
    ship.setPhased(true);
    ship.setExtraAlphaMult(0.0F);
    ship.setApplyExtraAlphaToEngines(true);
    Vector2f tempMovementVector = Vector2f.sub(ship.getLocation(), this.startPos, new Vector2f(0.0F, 0.0F));
    tempMovementVector.x = tempMovementVector.x * shadowProgress + MathUtils.getRandomNumberInRange(-SHADOW_DISTANCE_DIFFERENCE, SHADOW_DISTANCE_DIFFERENCE);
    tempMovementVector.y = tempMovementVector.y * shadowProgress + MathUtils.getRandomNumberInRange(-SHADOW_DISTANCE_DIFFERENCE, SHADOW_DISTANCE_DIFFERENCE);
    this.shadowPos = Vector2f.add(this.startPos, tempMovementVector, new Vector2f(0.0F, 0.0F));
    this.shadowDelayCounter += Global.getCombatEngine().getElapsedInLastFrame();
    if (this.shadowDelayCounter > SHADOW_DELAY) {
      float angleDifference = MathUtils.getRandomNumberInRange(-SHADOW_ANGLE_DIFFERENCE, SHADOW_ANGLE_DIFFERENCE) - 90.0F;
      for (int j = 0; j < SHADOW_FLICKER_CLONES; j++) {
        Vector2f modifiedShadowPos = new Vector2f(MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE));
        modifiedShadowPos.x += this.shadowPos.x;
        modifiedShadowPos.y += this.shadowPos.y;
        MagicRender.battlespace(Global.getSettings().getSprite("kol_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom"), modifiedShadowPos, new Vector2f(0.0F, 0.0F), new Vector2f(168.0F, 229.0F), new Vector2f(0.0F, 0.0F), ship.getFacing() + angleDifference, 0.0F, AFTERIMAGE_COLOR, true, 0.1F, 0.0F, 0.3F);
      } 
      this.shadowDelayCounter -= SHADOW_DELAY;
    } 
    MagicTrailPlugin.cutTrailsOnEntity((CombatEntityAPI)ship);
    for (int i = 0; i < 500.0F * Global.getCombatEngine().getElapsedInLastFrame(); i++)
      FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.38F, 0.67F), MathUtils.getRandomNumberInRange(70.0F, 90.0F), MathUtils.getRandomPointInCircle(this.shadowPos, SHADOW_DISTANCE_DIFFERENCE), MathUtils.getRandomPointInCircle(null, 10.0F), MathUtils.getRandomNumberInRange(-12.0F, 12.0F), 0.7F, new Color(0.0F, 0.0F, 0.0F));
  }
  
  public void unapply(MutableShipStatsAPI stats, String id) {
    ShipAPI ship;
    boolean player = false;
    if (stats.getEntity() instanceof ShipAPI) {
      ship = (ShipAPI)stats.getEntity();
      player = (ship == Global.getCombatEngine().getPlayerShip());
      id = id + "_" + ship.getId();
    } else {
      return;
    } 
    stats.getDeceleration().unmodify(id);
    stats.getAcceleration().unmodify(id);
    stats.getMaxSpeed().unmodify(id);
    stats.getMaxTurnRate().unmodify(id);
    stats.getTurnAcceleration().unmodify(id);
    if (ship.getSystem().getEffectLevel() <= 0.0F) {
      this.startPos = new Vector2f(0.0F, 0.0F);
      this.shadowPos = new Vector2f(0.0F, 0.0F);
      this.shadowDelayCounter = 0.0F;
      this.globalCounter = 0.0F;
      ship.setPhased(false);
      ship.setExtraAlphaMult(1.0F);
      this.runOnce = true;
      for (int i = 0; i < 100; i++)
        FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.1F, 0.78F), MathUtils.getRandomNumberInRange(50.0F, 90.0F), MathUtils.getRandomPointInCircle(ship.getLocation(), SHADOW_DISTANCE_DIFFERENCE * 2.8F), MathUtils.getRandomPointInCircle(null, 10.0F), MathUtils.getRandomNumberInRange(-12.0F, 12.0F), 0.7F, new Color(0.0F, 0.0F, 0.0F));
    } 
  }
  
  public ShipSystemStatsScript.StatusData getStatusData(int index, ShipSystemStatsScript.State state, float effectLevel) {
    if (index == 0)
      return new ShipSystemStatsScript.StatusData("slipping into phasespace...", false);
    return null;
  }
}