package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.selkie.kol.impl.fx.FakeSmokePlugin;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class PhasespaceSkip extends BaseShipSystemScript {
  private static final Color PHASE_COLOR = new Color(0.45F, 0.05F, 0.45F, 0.2F);
  
  private static final Color AFTERIMAGE_COLOR = new Color(0.25F, 0.05F, 0.4F, 0.3F);
  
  private static final float PHANTOM_DELAY = 0.07F;
  
  private static final float PHANTOM_ANGLE_DIFFERENCE = 5.0F;
  
  private static final float PHANTOM_DISTANCE_DIFFERENCE = 55.0F;
  
  private static final float PHANTOM_FLICKER_DIFFERENCE = 11.0F;
  
  private static final int PHANTOM_FLICKER_CLONES = 4;
  
  private static final float SHIP_ALPHA_MULT = 0.0F;
  
  private static final float SPEED_BONUS_MULT = 3.0F;
  
  private static final float TURN_BONUS_MULT = 2.0F;
  
  private static final float MOBILITY_BONUS_MULT = 50.0F;
  
  private int lastMessage = 0;
  
  private float phantomDelayCounter = 0.0F;
  
  private boolean runOnce = true;
  
  public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
    ShipAPI ship;
    boolean player = false;
    if (stats.getEntity() instanceof ShipAPI) {
      ship = (ShipAPI)stats.getEntity();
      player = (ship == Global.getCombatEngine().getPlayerShip());
      id = id + "_" + ship.getId();
    } else {
      return;
    } 
    float amount = Global.getCombatEngine().getElapsedInLastFrame();
    if (Global.getCombatEngine().isPaused())
      amount = 0.0F; 
    if (state == ShipSystemStatsScript.State.COOLDOWN || state == ShipSystemStatsScript.State.IDLE) {
      unapply(stats, id);
      return;
    } 
    if (state == ShipSystemStatsScript.State.OUT && this.runOnce) {
      this.runOnce = false;
      stats.getMaxSpeed().unmodify(id);
      stats.getMaxTurnRate().unmodify(id);
      if (ship.getAngularVelocity() > stats.getMaxTurnRate().getModifiedValue())
        ship.setAngularVelocity(ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity()) * stats.getMaxTurnRate().getModifiedValue()); 
      if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue())
        ship.getVelocity().set((ship.getVelocity()).x / ship.getVelocity().length() * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity()).y / ship.getVelocity().length() * stats.getMaxSpeed().getModifiedValue()); 
      return;
    } 
    if (state == ShipSystemStatsScript.State.IN || state == ShipSystemStatsScript.State.ACTIVE || state == ShipSystemStatsScript.State.OUT) {
      ship.setPhased(true);
      float speedBonus = 1.0F + 6.0F * effectLevel;
      float mobilityBonus = 1.0F + 49.0F * effectLevel;
      stats.getMaxSpeed().modifyMult(id, speedBonus);
      stats.getAcceleration().modifyMult(id, mobilityBonus);
      stats.getDeceleration().modifyMult(id, mobilityBonus);
      stats.getMaxTurnRate().modifyMult(id, 5.0F);
      stats.getTurnAcceleration().modifyMult(id, mobilityBonus);
    } 
    ship.setExtraAlphaMult(1.0F - 1.0F * effectLevel);
    ship.setApplyExtraAlphaToEngines(true);
    Vector2f phantomPos = MathUtils.getRandomPointInCircle(null, 55.0F);
    phantomPos.x += (ship.getLocation()).x;
    phantomPos.y += (ship.getLocation()).y;
    if (!Global.getCombatEngine().getViewport().isNearViewport(phantomPos, ship.getCollisionRadius() * 1.5F))
      return; 
    this.phantomDelayCounter += amount;
    if (this.phantomDelayCounter > 0.07F) {
      float angleDifference = MathUtils.getRandomNumberInRange(-5.0F, 5.0F) - 90.0F;
      for (int j = 0; j < 4; j++) {
        Vector2f modifiedPhantomPos = new Vector2f(MathUtils.getRandomNumberInRange(-11.0F, 11.0F), MathUtils.getRandomNumberInRange(-11.0F, 11.0F));
        modifiedPhantomPos.x += phantomPos.x;
        modifiedPhantomPos.y += phantomPos.y;
        MagicRender.battlespace(Global.getSettings().getSprite("kol_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom"), modifiedPhantomPos, new Vector2f(0.0F, 0.0F), new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()), new Vector2f(0.0F, 0.0F), ship.getFacing() + angleDifference, 0.0F, AFTERIMAGE_COLOR, true, 0.1F, 0.0F, 0.3F);
      } 
      Color colorToUse = new Color(PHASE_COLOR.getRed() / 255.0F, PHASE_COLOR.getGreen() / 255.0F, PHASE_COLOR.getBlue() / 255.0F, PHASE_COLOR.getAlpha() / 255.0F * effectLevel);
      MagicRender.battlespace(Global.getSettings().getSprite("kol_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom"), new Vector2f((ship.getLocation()).x, (ship.getLocation()).y), new Vector2f(0.0F, 0.0F), new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()), new Vector2f(0.0F, 0.0F), ship.getFacing() - 90.0F, 0.0F, colorToUse, true, 0.0F, 0.1F, 0.2F);
      this.phantomDelayCounter -= 0.07F;
    } 
    for (int i = 0; i < 900.0F * amount; i++) {
      Vector2f pointToSpawnAt = MathUtils.getRandomPointInCircle(phantomPos, ship.getCollisionRadius());
      int emergencyCounter = 0;
      while (!CollisionUtils.isPointWithinBounds(pointToSpawnAt, (CombatEntityAPI)ship) && emergencyCounter < 1000) {
        pointToSpawnAt = MathUtils.getRandomPointInCircle(phantomPos, ship.getCollisionRadius());
        emergencyCounter++;
      } 
      pointToSpawnAt = MathUtils.getRandomPointInCircle(pointToSpawnAt, 50.0F);
      FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.32F, 0.68F), MathUtils.getRandomNumberInRange(55.0F, 75.0F), pointToSpawnAt, MathUtils.getRandomPointInCircle(null, 10.0F), MathUtils.getRandomNumberInRange(-15.0F, 15.0F), 0.85F, new Color(0.0F, 0.0F, 0.0F));
    } 
  }
  
  public void unapply(MutableShipStatsAPI stats, String id) {
    ShipAPI ship;
    if (stats.getEntity() instanceof ShipAPI) {
      ship = (ShipAPI)stats.getEntity();
    } else {
      return;
    } 
    this.runOnce = true;
    stats.getMaxSpeed().unmodify(id);
    stats.getAcceleration().unmodify(id);
    stats.getDeceleration().unmodify(id);
    stats.getMaxTurnRate().unmodify(id);
    stats.getTurnAcceleration().unmodify(id);
    if (Math.abs(ship.getAngularVelocity()) > stats.getMaxTurnRate().getModifiedValue())
      ship.setAngularVelocity(ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity()) * stats.getMaxTurnRate().getModifiedValue()); 
    if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue())
      ship.getVelocity().set((ship.getVelocity()).x / ship.getVelocity().length() * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity()).y / ship.getVelocity().length() * stats.getMaxSpeed().getModifiedValue()); 
    ship.setPhased(false);
    ship.setExtraAlphaMult(1.0F);
  }
  
  public ShipSystemStatsScript.StatusData getStatusData(int index, ShipSystemStatsScript.State state, float effectLevel) {
    if (state == ShipSystemStatsScript.State.ACTIVE) {
      if (index == 0 && (Math.random() < 0.012987012974917889D || this.lastMessage == 1 || this.lastMessage == 4)) {
        if (this.lastMessage == 0) {
          this.lastMessage = 1;
        } else if (this.lastMessage == 1) {
          this.lastMessage = 4;
        } else {
          this.lastMessage = 0;
        } 
        return new ShipSystemStatsScript.StatusData("it screams", false);
      } 
      if (index == 0 && (Math.random() < 0.01315789483487606D || this.lastMessage == 2 || this.lastMessage == 5)) {
        if (this.lastMessage == 0) {
          this.lastMessage = 2;
        } else if (this.lastMessage == 2) {
          this.lastMessage = 5;
        } else {
          this.lastMessage = 0;
        } 
        return new ShipSystemStatsScript.StatusData("it wails", false);
      } 
      if (index == 0 && (Math.random() < 0.013333333656191826D || this.lastMessage == 3 || this.lastMessage == 6)) {
        if (this.lastMessage == 0) {
          this.lastMessage = 3;
        } else if (this.lastMessage == 3) {
          this.lastMessage = 6;
        } else {
          this.lastMessage = 0;
        } 
        return new ShipSystemStatsScript.StatusData("it yells", false);
      } 
      if (index == 0) {
        this.lastMessage = 0;
        return new ShipSystemStatsScript.StatusData("breaching phasespace", false);
      } 
    } 
    return null;
  }
}


/* Location:              C:\Program Files (x86)\Fractal Softworks\Starsector old\Starsector 095\mods\sylphon\jars\SylphonRnD.jar!\data\scripts\shipsystems\SRD_NullspaceSkip.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       1.1.3
 */