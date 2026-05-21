package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.item.GameplayItems;

public class TreeActor extends AbstractTiledTargetActor {
	private static final int SMALL_STUMP_TILE_GID = 17;
	private static final int NORMAL_STUMP_TILE_GID = 77;
	private static final float SMALL_STUMP_SIZE = 32f / 16f;
	private static final float NORMAL_STUMP_SIZE = 41f / 16f;

	private static final float HIT_SWAY_DURATION = 0.18f;
	private static final float HIT_SWAY_ROTATION = 4.5f;
	private static final float FALL_DURATION = 0.42f;
	private static final float FALL_ROTATION = 72f;

	private TreeKind treeKind = TreeKind.OAK;
	private int stumpTileGid = 17;
	private float stumpWidth = 2f;
	private float stumpHeight = 2f;
	private float stumpCollHalfW;
	private float stumpCollHalfH;
	private final Vector2 stumpCollOffset = new Vector2();
	private final Vector2 restPosition = new Vector2();
	private boolean restPositionInitialized;
	private boolean pivotConfigured;
	private boolean falling;
	private boolean destroyQueued;
	private boolean destructionStatsHandled;
	private float swayTimer;
	private float swayDirection = 1f;
	private float fallTimer;
	private float fallDirection = 1f;

	public void setTreeKind(TreeKind treeKind) {
		if (treeKind != null) {
			this.treeKind = treeKind;
			applyDefaultStumpData(treeKind);
		}
	}

	public void setStumpTileGid(int stumpTileGid) {
		if (stumpTileGid > 0) {
			this.stumpTileGid = stumpTileGid;
		}
	}

	public void setStumpSize(float stumpWidth, float stumpHeight) {
		if (stumpWidth > 0f) {
			this.stumpWidth = stumpWidth;
		}
		if (stumpHeight > 0f) {
			this.stumpHeight = stumpHeight;
		}
	}

	public void setStumpCollision(float collHalfW, float collHalfH, Vector2 collOffset) {
		this.stumpCollHalfW = Math.max(0f, collHalfW);
		this.stumpCollHalfH = Math.max(0f, collHalfH);
		this.stumpCollOffset.set(collOffset != null ? collOffset : Vector2.Zero);
	}

	@Override
	public void tick(float delta) {
		TransformComponent transform = getComponent(TransformComponent.class);
		HealthComponent health = getComponent(HealthComponent.class);
		if (transform == null || health == null) {
			super.tick(delta);
			return;
		}

		ensureRestPosition(transform);

		if (!health.isAlive()) {
			updateFallSequence(delta, transform);
			return;
		}

		super.tick(delta);
		updateHitSway(delta, transform);
	}

	@Override
	protected void onBeforeDestroy() {
		float stumpSortOffsetY = stumpCollHalfH > 0f
			? stumpHeight * 0.5f + stumpCollOffset.y - stumpCollHalfH
			: stumpHeight * 0.72f;
		spawnVisualDecoration(stumpTileGid, stumpWidth, stumpHeight,
			stumpSortOffsetY, -1, stumpCollHalfW, stumpCollHalfH, stumpCollOffset);
		spawnItemDrops(GameplayItems.WOOD_LOG, treeKind.getMinLogs(), treeKind.getMaxLogs());
	}

	private void applyDefaultStumpData(TreeKind treeKind) {
		if (treeKind == TreeKind.SMALL) {
			this.stumpTileGid = SMALL_STUMP_TILE_GID;
			this.stumpWidth = SMALL_STUMP_SIZE;
			this.stumpHeight = SMALL_STUMP_SIZE;
		} else {
			this.stumpTileGid = NORMAL_STUMP_TILE_GID;
			this.stumpWidth = NORMAL_STUMP_SIZE;
			this.stumpHeight = NORMAL_STUMP_SIZE;
		}
	}

	private void ensureRestPosition(TransformComponent transform) {
		if (!restPositionInitialized) {
			restPosition.set(transform.getPosition());
			restPositionInitialized = true;
			swayDirection = ((getActorId() + stumpTileGid) & 1) == 0 ? -1f : 1f;
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

	private void updateFallSequence(float delta, TransformComponent transform) {
		if (!falling) {
			falling = true;
			swayTimer = 0f;
			fallTimer = 0f;
			fallDirection = computeFallDirection();
			if (!destructionStatsHandled) {
				destructionStatsHandled = true;
				GameplayStatsBridge.recordTreeCut(this);
			}
		}

		fallTimer = Math.min(FALL_DURATION, fallTimer + delta);
		float progress = Math.min(1f, fallTimer / FALL_DURATION);
		float eased = Interpolation.pow2Out.apply(progress);
		applyPose(transform, fallDirection * eased * FALL_ROTATION);

		if (hasAuthority() && !destroyQueued && progress >= 1f) {
			destroyQueued = true;
			resetPose(transform);
			onBeforeDestroy();
			if (getWorld() != null) {
				getWorld().destroyActor(this);
			}
		}
	}

	private float computeFallDirection() {
		int seed = getActorId() * 31 + getTileGid() * 17 + Math.round(stumpWidth * 10f);
		return (seed & 1) == 0 ? -1f : 1f;
	}

	private void configureRotationPivot(TransformComponent transform) {
		float pivotYNormalized = 0.1f;
		BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
		if (collision != null && transform.getSize().y > 0f) {
			float pivotLocalY = transform.getSize().y * 0.5f
				+ collision.getOffset().y
				- collision.getHalfHeight();
			pivotYNormalized = MathUtils.clamp(pivotLocalY / transform.getSize().y, 0.04f, 0.24f);
		}
		transform.setRotationOriginNormalized(0.5f, pivotYNormalized);
	}

	private void applyPose(TransformComponent transform, float rotationDeg) {
		transform.getPosition().set(restPosition);
		transform.setRotationDeg(rotationDeg);
	}

	private void resetPose(TransformComponent transform) {
		transform.getPosition().set(restPosition);
		transform.setRotationDeg(0f);
	}

	public SaveGameData.TreeData buildSaveData() {
		SaveGameData.TreeData data = new SaveGameData.TreeData();
		TransformComponent transform = getComponent(TransformComponent.class);
		BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
		HealthComponent health = getComponent(HealthComponent.class);

		data.treeKind = treeKind.name();
		data.tileGid = getTileGid();
		if (transform != null) {
			data.x = transform.getPosition().x;
			data.y = transform.getPosition().y;
			data.sizeW = transform.getSize().x;
			data.sizeH = transform.getSize().y;
			data.sortOffsetY = transform.getSortOffsetY();
			data.zOrder = transform.getZOrder();
		}
		if (collision != null) {
			data.collHalfW = collision.getHalfWidth();
			data.collHalfH = collision.getHalfHeight();
			data.collOffsetX = collision.getOffset().x;
			data.collOffsetY = collision.getOffset().y;
		}
		if (health != null) {
			data.maxHealth = health.getMaxHealth();
			data.currentHealth = health.getCurrentHealth();
		}
		data.stumpTileGid = stumpTileGid;
		data.stumpWidth = stumpWidth;
		data.stumpHeight = stumpHeight;
		data.stumpCollHalfW = stumpCollHalfW;
		data.stumpCollHalfH = stumpCollHalfH;
		data.stumpCollOffsetX = stumpCollOffset.x;
		data.stumpCollOffsetY = stumpCollOffset.y;
		return data;
	}
}