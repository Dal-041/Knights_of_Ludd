package org.selkie.kol.impl.combat.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.magiclib.subsystems.MagicSubsystem;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.kol.impl.shipsystems.PulsarSystem;

public class BlizzardSubsystem extends MagicSubsystem {
    public static final ShipSystemSpecAPI entry = Global.getSettings().getShipSystemSpec(ZeaStaticStrings.systemIDBlizzard);
    public static final float tActive = entry.getActive();
    public final float tCD = entry.getCooldown(stats);
    public final float tRegen = entry.getRegen(stats);
    public final PulsarSystem pulsar = new PulsarSystem();
    public final float beamRange = pulsar.PULSAR_LENGTH;

    final IntervalUtil intervalAI = new IntervalUtil(0.5f, 1f);
    IntervalUtil intervalActive = new IntervalUtil(0.5f, 1f);

    public BlizzardSubsystem(ShipAPI ship) {
        super(ship);
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        return tRegen;
    }

    @Override
    public float getBaseActiveDuration() {
        return tActive;
    }

    @Override
    public float getBaseCooldownDuration() {
        return tCD;
    }

    @Override
    public float getBaseInDuration() {
        return entry.getIn();
    }

    @Override
    public float getBaseOutDuration() {
        return entry.getOut();
    }

    @Override
    public boolean canActivate() {
        return canActivateInternal();
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        intervalAI.advance(amount);
        return intervalAI.intervalElapsed() && canActivate() && ship.getFluxTracker().getFluxLevel() > 0.75f && !AIUtils.getNearbyEnemies(ship, beamRange).isEmpty();
    }

    @Override
    public String getDisplayText() {
        return entry.getName();
    }

    private static ShipSystemStatsScript.State translateState(State CAstate) {
        if (CAstate == State.ACTIVE) return ShipSystemStatsScript.State.ACTIVE;
        if (CAstate == State.IN) return ShipSystemStatsScript.State.IN;
        if (CAstate == State.OUT) return ShipSystemStatsScript.State.OUT;
        if (CAstate == State.COOLDOWN) return ShipSystemStatsScript.State.COOLDOWN;
        if (CAstate == State.READY) return ShipSystemStatsScript.State.IDLE;
        return null;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        pulsar.apply(ship.getMutableStats(), entry.getName(), translateState(getState()), 1f);
    }
}
