package org.selkie.zea.shipsystems;

/** MiniJump without the post-jump phase-through, used by the Seiryu. */
public class MiniJumpNoPhase extends MiniJump {
	@Override
	protected float getPhaseDuration() {
		return 0f;
	}
}
