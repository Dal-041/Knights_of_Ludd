package org.selkie.kol.campaign.movement;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

/**
 * Generic point-to-point chase driver for a {@link KolEntityMover}: makes the moved
 * entity follow a target token (the player fleet) the way a fleet would -
 *
 * <ul>
 *   <li>tracking the target inside a shared system,</li>
 *   <li>heading to an exit jump point and jumping out into hyperspace at the exit's
 *       hyperspace-side partner (chasing the target across hyperspace while in transit),</li>
 *   <li>approaching the destination system's nearest entry (the hyperspace partner of one
 *       of its in-system exit jump points / gravity wells) and entering the system through
 *       it, then resuming tracking.</li>
 * </ul>
 *
 * <p>Jump topology: jump points are paired entities - one in the system, linked to one in
 * hyperspace; the in-system token's destination <em>is</em> its hyperspace partner. The
 * system-to-system link is stored on the hyperspace side, so routing here is purely
 * geometric: exit through a jump point of the current system, enter through the
 * destination system's entry closest to the entity. If no usable exit/entry exists, the
 * entity holds (logged once).
 *
 * <p>Nascent gravity wells are excluded: transverse jumps (entering a system through a
 * nascent well) are a player-only ability.
 *
 * <p>The driver is location-type agnostic ({@link SectorEntityToken}s and
 * {@link LocationAPI}s); wire it up with {@link KolEntityMover#setTargetProvider} and
 * {@link KolEntityMover#setFrameHook}.
 */
public class KolChaseDriver implements KolEntityMover.TargetProvider, KolEntityMover.FrameHook {

    /** Close enough to a jump point (in-system, or to its hyperspace partner) to "jump". */
    private static final float JUMP_EPSILON = 50f;

    private final KolEntityMover mover;
    private final SectorEntityToken target;

    /** In-system exit jump point the entity is working toward (null when not applicable). */
    private JumpPointAPI exitJumpPoint;
    /** In-system entry (exit jump point of the destination system) the entity is entering through. */
    private JumpPointAPI entryJumpPoint;
    private final Vector2f lastTargetInSystemPosition = new Vector2f();
    private boolean noRouteLogged;

    public KolChaseDriver(KolEntityMover mover, SectorEntityToken target) {
        this.mover = mover;
        this.target = target;
    }

    @Override
    public ReadableVector2f getTarget() {
        SectorEntityToken entity = mover.getEntity();
        LocationAPI entityLocation = entity.getContainingLocation();
        LocationAPI targetLocation = target.getContainingLocation();

        if (entityLocation == targetLocation) {
            return target.getLocation(); // tracking in the same location
        }
        if (entityLocation instanceof StarSystemAPI) {
            // In a system, target elsewhere: head to the exit jump point.
            return (exitJumpPoint != null) ? exitJumpPoint.getLocation() : null; // null = no exit, hold
        }
        // Entity in hyperspace
        if (targetLocation instanceof StarSystemAPI) {
            // Target has arrived: work to the destination system's nearest entry.
            SectorEntityToken entry = (entryJumpPoint != null) ? hyperPartner(entryJumpPoint) : null;
            return (entry != null) ? entry.getLocation() : null;
        }
        // Target still in transit: chase it across hyperspace.
        return target.getLocation();
    }

    @Override
    public void tick(float dt) {
        SectorEntityToken entity = mover.getEntity();
        LocationAPI entityLocation = entity.getContainingLocation();
        LocationAPI targetLocation = target.getContainingLocation();

        if (targetLocation instanceof StarSystemAPI) {
            lastTargetInSystemPosition.set(target.getLocation());
        }

        if (entityLocation == targetLocation) {
            exitJumpPoint = null;
            entryJumpPoint = null;
            noRouteLogged = false;
            return;
        }

        if (entityLocation instanceof StarSystemAPI) {
            // In a system, target elsewhere (another system, or in transit): work to an exit.
            StarSystemAPI system = (StarSystemAPI) entityLocation;
            exitJumpPoint = pickExitJumpPoint(system);
            if (exitJumpPoint == null) {
                logNoRoute(system.getId(), "no exit jump point");
                return;
            }
            noRouteLogged = false;
            SectorEntityToken partner = hyperPartner(exitJumpPoint);
            if (partner == null) {
                logNoRoute(system.getId(), "exit jump point " + exitJumpPoint.getId() + " has no hyperspace partner");
                return;
            }
            if (Misc.getDistance(entity.getLocation(), exitJumpPoint.getLocation()) <= JUMP_EPSILON) {
                mover.relocate(Global.getSector().getHyperspace(), partner.getLocation());
                System.out.println("[KolChaseDriver] " + entity.getId() + " jumped out of " + system.getId()
                        + " via " + exitJumpPoint.getId());
                exitJumpPoint = null;
            }
        } else if (targetLocation instanceof StarSystemAPI) {
            // In hyperspace, target has arrived in a system: enter through the system's
            // entry closest to where the entity is.
            StarSystemAPI destination = (StarSystemAPI) targetLocation;
            entryJumpPoint = pickEntryJumpPoint(destination);
            if (entryJumpPoint == null) {
                logNoRoute(destination.getId(), "no entry jump point");
                return;
            }
            noRouteLogged = false;
            SectorEntityToken partner = hyperPartner(entryJumpPoint);
            if (partner == null) {
                logNoRoute(destination.getId(), "entry jump point " + entryJumpPoint.getId() + " has no hyperspace partner");
                return;
            }
            if (Misc.getDistance(entity.getLocation(), partner.getLocation()) <= JUMP_EPSILON) {
                mover.relocate(destination, entryJumpPoint.getLocation());
                System.out.println("[KolChaseDriver] " + entity.getId() + " entered " + destination.getId()
                        + " via " + entryJumpPoint.getId());
                entryJumpPoint = null;
            }
        }
        // Entity and target both in hyperspace: nothing to do here; getTarget() chases the target.
    }

    private void logNoRoute(String systemId, String reason) {
        if (!noRouteLogged) {
            noRouteLogged = true;
            System.out.println("[KolChaseDriver] " + mover.getEntity().getId() + ": " + reason
                    + " in " + systemId + "; holding position.");
        }
    }

    /**
     * The hyperspace-side partner of an in-system jump point: the destination of the
     * jump point that lives in hyperspace (auto-generated entrance, well). Nascent
     * gravity wells are excluded - entering through them is a player-only ability.
     */
    private static SectorEntityToken hyperPartner(JumpPointAPI jumpPoint) {
        for (JumpPointAPI.JumpDestination destination : jumpPoint.getDestinations()) {
            SectorEntityToken entity = destination.getDestination();
            if (entity != null && !(entity instanceof NascentGravityWellAPI) && entity.isInHyperspace()) {
                return entity;
            }
        }
        return null;
    }

    /**
     * An exit jump point of {@code system}: nearest to the target's last in-system position
     * (the target can only jump from a jump point, and we tracked it there).
     */
    private JumpPointAPI pickExitJumpPoint(StarSystemAPI system) {
        JumpPointAPI best = null;
        float bestDistance = Float.MAX_VALUE;
        for (SectorEntityToken token : system.getJumpPoints()) {
            if (!(token instanceof JumpPointAPI) || hyperPartner((JumpPointAPI) token) == null) {
                continue;
            }
            JumpPointAPI jumpPoint = (JumpPointAPI) token;
            float distance = Misc.getDistance(lastTargetInSystemPosition, jumpPoint.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = jumpPoint;
            }
        }
        return best;
    }

    /**
     * An entry of {@code system}: the in-system exit jump point whose hyperspace partner
     * is closest to the entity's current position.
     */
    private JumpPointAPI pickEntryJumpPoint(StarSystemAPI system) {
        SectorEntityToken entity = mover.getEntity();
        JumpPointAPI best = null;
        float bestDistance = Float.MAX_VALUE;
        for (SectorEntityToken token : system.getJumpPoints()) {
            if (!(token instanceof JumpPointAPI)) {
                continue;
            }
            JumpPointAPI jumpPoint = (JumpPointAPI) token;
            SectorEntityToken partner = hyperPartner(jumpPoint);
            if (partner == null) {
                continue;
            }
            float distance = Misc.getDistance(entity.getLocation(), partner.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = jumpPoint;
            }
        }
        return best;
    }
}
