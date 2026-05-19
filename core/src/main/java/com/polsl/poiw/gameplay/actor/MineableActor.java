package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.TransformComponent;

public class MineableActor extends AbstractTiledTargetActor {
	private static final float HIT_SWAY_DURATION = 0.16f;
	private static final float HIT_SWAY_ROTATION = 5.5f;

	private MineableKind mineableKind = MineableKind.IRON;
	private int minDropCount = 1;
	private int maxDropCount = 1;
	private final Vector2 restPosition = new Vector2();
	private boolean restPositionInitialized;
	private boolean pivotConfigured;
	private float swayTimer;
	private float swayDirection = 1f;

	public void setMineableKind(MineableKind mineableKind) {
		if (mineableKind != null) {
			this.mineableKind = mineableKind;
		}
	}

	public void setDropCountRange(int minDropCount, int maxDropCount) {
		int clampedMin = Math.max(0, minDropCount);
		int clampedMax = Math.max(clampedMin, maxDropCount);
		this.minDropCount = clampedMin;
		this.maxDropCount = clampedMax;
	}

	@Override
	public void tick(float delta) {
		TransformComponent transform = getComponent(TransformComponent.class);
		HealthComponent health = getComponent(HealthComponent.class);
		if (transform == null || health == null) {
			super.tick(delta);
			return;
		}

		ensurePoseSetup(transform);

		if (!health.isAlive()) {
			applyPose(transform, 0f);
			super.tick(delta);
			return;
		}

		super.tick(delta);
		updateHitSway(delta, transform);
	}

	@Override
	protected void onBeforeDestroy() {
		spawnItemDrops(mineableKind.getDropItem(), minDropCount, maxDropCount);
	}

	private void ensurePoseSetup(TransformComponent transform) {
		if (!restPositionInitialized) {
			restPosition.set(transform.getPosition());
			restPositionInitialized = true;
			swayDirection = ((getActorId() + getTileGid()) & 1) == 0 ? -1f : 1f;
		}
		if (!pivotConfigured) {
			configureRotationPivot(transform);
			pivotConfigured = true;
		}
	}

	private void updateHitSway(float delta, TransformComponent transform) {
		DamageReactionComponent damageReaction = getComponent(DamageReactionComponent.class);
		if (damageReaction != null && damageReaction.consumeReactionTrigger()) {
			swayTimer = HIT_SWAY_DURATION;
			swayDirection = -swayDirection;
		}

		if (swayTimer <= 0f) {
			applyPose(transform, 0f);
			return;
		}

		swayTimer = Math.max(0f, swayTimer - delta);
		float progress = 1f - swayTimer / HIT_SWAY_DURATION;
		float envelope = 1f - progress;
		float sway = MathUtils.sin(progress * MathUtils.PI2 * 1.5f + MathUtils.HALF_PI) * envelope;
		applyPose(transform, swayDirection * sway * HIT_SWAY_ROTATION);
	}

	private void configureRotationPivot(TransformComponent transform) {
		float pivotYNormalized = 0.38f;
		BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
		if (collision != null && transform.getSize().y > 0f) {
			float pivotLocalY = transform.getSize().y * 0.5f
				+ collision.getOffset().y
				- collision.getHalfHeight();
			pivotYNormalized = MathUtils.clamp(pivotLocalY / transform.getSize().y, 0.18f, 0.48f);
		}
		transform.setRotationOriginNormalized(0.5f, pivotYNormalized);
	}

	private void applyPose(TransformComponent transform, float rotationDeg) {
		transform.getPosition().set(restPosition);
		transform.setRotationDeg(rotationDeg);
	}
}