package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.combat.entities.Ship;
import org.selkie.kol.ReflectionUtils;
import org.selkie.kol.helpers.KolStaticStrings;

import java.util.List;
import java.util.Objects;


public class AlysseAIFixer implements EveryFrameWeaponEffectPlugin {
    boolean inited = false;

    public static class AlysseAIFixerWeaponSetter implements AdvanceableListener{
        boolean isLeft = false;
        WeaponAPI weapon;
        CombatEngineAPI engine;

        public AlysseAIFixerWeaponSetter(WeaponAPI weapon){

            this.weapon = weapon;
            this.engine = Global.getCombatEngine();
            if (weapon.getShip().getId().contains("LEFT")){
                isLeft = true;
            }
        }

        @Override
        public void advance(float amount) {
            weapon.setForceNoFireOneFrame(true);
            if((!engine.isUIAutopilotOn() || engine.getPlayerShip() != weapon.getShip()) && weapon.getShip().getOriginalOwner() != -1){
                setSlots(weapon, WeaponAPI.WeaponType.BUILT_IN);
                addWeaponToGroups(weapon);
            } else{
                setSlots(weapon, WeaponAPI.WeaponType.DECORATIVE);
                weapon.getShip().removeWeaponFromGroups(weapon);
            }

            if(isLeft){
                boolean leftDead = true;
                for(ShipAPI module : weapon.getShip().getChildModulesCopy()){
                    if(module.getHullSpec().getBaseHullId().contains("tl") && !module.hasTag(KolStaticStrings.KOL_MODULE_DEAD)){
                        leftDead = false;
                    }
                }

                if(leftDead){
                    weapon.disable();
                } else{
                    weapon.repair();
                }
            } else{
                boolean rightDead = true;
                for(ShipAPI module : weapon.getShip().getChildModulesCopy()){
                    if(module.getHullSpec().getBaseHullId().contains("tr") && !module.hasTag(KolStaticStrings.KOL_MODULE_DEAD)){
                        rightDead = false;
                    }
                }

                if(rightDead){
                    weapon.disable();
                } else{
                    weapon.repair();
                }
            }
        }

        public static void setSlots(WeaponAPI weapon, WeaponAPI.WeaponType type){
            if(weapon.getSlot().getWeaponType() != type) {
                List<String> weaponTypeField = ReflectionUtils.INSTANCE.getFieldsOfType(weapon.getSlot(), WeaponAPI.WeaponType.class);
                ReflectionUtils.set(weaponTypeField.get(0), weapon.getSlot(), type);
            }
        }

        private void addWeaponToGroups(WeaponAPI weapon){
            Ship actualShip = (Ship) weapon.getShip();
            List<?> weaponGroups = actualShip.getGroups();
            if(weaponGroups.isEmpty()) return;
            if(weapon.getShip().getWeaponGroupFor(weapon) == null){
                WeaponGroupAPI group = getGroup(weapon.getShip());
                if(group != null){
                    group.addWeaponAPI(weapon);
                }
                else{
                    try {
                        Object weaponGroup = weaponGroups.get(0).getClass().newInstance();
                        ((WeaponGroupAPI) weaponGroup).setType(WeaponGroupType.LINKED);
                        ((WeaponGroupAPI) weaponGroup).addWeaponAPI(weapon);
                        ReflectionUtils.INSTANCE.invoke("addGroup", actualShip, new Object[]{weaponGroup},false);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public WeaponGroupAPI getGroup(ShipAPI ship){

            if(ship.getWeaponGroupsCopy().size() == 7){
                return ship.getWeaponGroupsCopy().get(6);
            }
            for(WeaponGroupAPI group : ship.getWeaponGroupsCopy()){
                for (WeaponAPI weapon : group.getWeaponsCopy()){
                    if(Objects.equals(weapon.getSpec().getWeaponId(), "kol_alysse_ai_fixer")){
                        return group;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(weapon.getShip().getOriginalOwner() == -1){
            AlysseAIFixerWeaponSetter.setSlots(weapon, WeaponAPI.WeaponType.DECORATIVE);
            weapon.getShip().removeWeaponFromGroups(weapon);
        } else{
            weapon.setForceNoFireOneFrame(true);
            if(inited) return;
            inited = true;
            weapon.getShip().addListener(new AlysseAIFixerWeaponSetter(weapon));
        }
    }
}
