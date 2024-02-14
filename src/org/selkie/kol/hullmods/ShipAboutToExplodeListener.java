package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;

public class ShipAboutToExplodeListener implements AdvanceableListener, HullDamageAboutToBeTakenListener {
    private static boolean active = false;
    private static final String id = "ExploRes";
    private static HashMap<ShipAPI, Integer> modifiedShips = new HashMap();

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
        float hull = ship.getHitpoints();

        if (damageAmount >= hull) { //That's our cue!
            for (ShipAPI luckygit : Global.getCombatEngine().getShips()) {
                if (luckygit.isFighter() || luckygit.isPhased() || !luckygit.getHullSpec().getBaseHullId().startsWith("kol_")) continue;
                if (MathUtils.getDistance(ship.getLocation(), luckygit.getLocation()) < 400) {
                    luckygit.getMutableStats().getArmorDamageTakenMult().modifyMult(id,0.1f);
                    luckygit.getMutableStats().getHullDamageTakenMult().modifyMult(id,0.1f);
                    modifiedShips.put(luckygit, 20);
                    active = true;
                }
            }
        }
        return false;
    }

    @Override
    public void advance(float amount) {
        if (!active) return;
        ArrayList<ShipAPI> toRemove = new ArrayList<>();

        for (ShipAPI ship : modifiedShips.keySet()) {
            int temp = modifiedShips.get(ship)-1;
            if (temp == 0) {
                ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                toRemove.add(ship);
            } else {
                modifiedShips.put(ship, temp);
            }
        }
        for (ShipAPI ship : toRemove) {
            modifiedShips.remove(ship);
        }
        if (modifiedShips.isEmpty()) active = false;
    }
}
