package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.CreatureAnimationComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.KnockbackComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.character.PlayerCharacter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bazowa klasa zwierząt i prostych przeciwników.
 * Daje wspólną konfigurację, pasywną walkę jako target i prosty wandering wokół punktu startowego.
 */
public abstract class AbstractCreatureActor extends AbstractActor {
    public static final float DEFAULT_MAX_HEALTH = 50f;

    public static final String PROP_SIZE_W = "sizeW";
    public static final String PROP_SIZE_H = "sizeH";
    public static final String PROP_COLL_HALF_W = "collHalfW";
    public static final String PROP_COLL_HALF_H = "collHalfH";
    public static final String PROP_COLL_OFFSET_X = "collOffsetX";
    public static final String PROP_COLL_OFFSET_Y = "collOffsetY";
    public static final String PROP_SORT_OFFSET_Y = "sortOffsetY";
    public static final String PROP_Z_ORDER = "zOrder";
    public static final String PROP_MAX_HEALTH = "maxHealth";
    public static final String PROP_CURRENT_HEALTH = "currentHealth";

    private static final float MIN_IDLE_DURATION = 2.5f;
    private static final float MAX_IDLE_DURATION = 6.0f;
    private static final float MIN_WANDER_DISTANCE = 0.6f;
    private static final int MAX_TARGET_ATTEMPTS = 8;
    private static final float TARGET_REACHED_DISTANCE = 0.18f;
    private static final float STUCK_VELOCITY_EPSILON = 0.01f;
    private static final float STUCK_TIMEOUT = 0.2f;

    private final Vector2 homePosition = new Vector2();
    private final Vector2 targetPosition = new Vector2();
    private boolean homeInitialized;
    private boolean hasTarget;
    private boolean chasingPlayer;
    private boolean deathHandled;
    private boolean deathStatsHandled;
    private float idleRemaining;
    private float movementAttemptTimer;
    private final Vector2 navigationPosition = new Vector2();
    private final Vector2 playerNavigationPosition = new Vector2();
    private final Vector2 chaseDirection = new Vector2();
    private final Map<Integer, Float> contactDamageTimers = new HashMap<>();
    private final Map<Integer, PlayerCharacter> sensedContactPlayers = new HashMap<>();

    public void configure(TextureAtlas atlas,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder,
                          float maxHealth,
                          float currentHealth) {
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, currentHealth);

        TextureRegion idleFrame = atlas.findRegion(getIdleRegionName());
        if (idleFrame == null) {
            throw new IllegalArgumentException("Nie znaleziono regionu: " + getIdleRegionName());
        }

        addComponent(new SpriteComponent(idleFrame, Color.WHITE.cpy()));
        addComponent(new CreatureAnimationComponent(
            atlas,
            getIdleRegionName(),
            getWalkRegionName(),
            usesRightFacingSourceFrames()
        ));
    }

    public void configureServer(float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder,
                                float maxHealth,
                                float currentHealth) {
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, currentHealth);
    }

    public void configureFromReplication(TextureAtlas atlas, Map<String, Object> initialProperties) {
        float sizeW = getFloat(initialProperties, PROP_SIZE_W, 2f);
        float sizeH = getFloat(initialProperties, PROP_SIZE_H, 2f);
        float collHalfW = getFloat(initialProperties, PROP_COLL_HALF_W, 0.4f);
        float collHalfH = getFloat(initialProperties, PROP_COLL_HALF_H, 0.12f);
        float collOffsetX = getFloat(initialProperties, PROP_COLL_OFFSET_X, 0f);
        float collOffsetY = getFloat(initialProperties, PROP_COLL_OFFSET_Y, 0f);
        float sortOffsetY = getFloat(initialProperties, PROP_SORT_OFFSET_Y, 0f);
        int zOrder = getInt(initialProperties, PROP_Z_ORDER, 1);
        float maxHealth = getFloat(initialProperties, PROP_MAX_HEALTH, DEFAULT_MAX_HEALTH);
        float currentHealth = getFloat(initialProperties, PROP_CURRENT_HEALTH, maxHealth);

        configure(atlas, sizeW, sizeH, collHalfW, collHalfH,
            new Vector2(collOffsetX, collOffsetY), sortOffsetY, zOrder, maxHealth, currentHealth);
    }

    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        HealthComponent health = getComponent(HealthComponent.class);

        if (transform != null) {
            properties.put(PROP_SIZE_W, transform.getSize().x);
            properties.put(PROP_SIZE_H, transform.getSize().y);
            properties.put(PROP_SORT_OFFSET_Y, transform.getSortOffsetY());
            properties.put(PROP_Z_ORDER, transform.getZOrder());
        }
        if (collision != null) {
            properties.put(PROP_COLL_HALF_W, collision.getHalfWidth());
            properties.put(PROP_COLL_HALF_H, collision.getHalfHeight());
            properties.put(PROP_COLL_OFFSET_X, collision.getOffset().x);
            properties.put(PROP_COLL_OFFSET_Y, collision.getOffset().y);
        }
        if (health != null) {
            properties.put(PROP_MAX_HEALTH, health.getMaxHealth());
            properties.put(PROP_CURRENT_HEALTH, health.getCurrentHealth());
        }

        return properties;
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null && !health.isAlive()) {
            if (!deathStatsHandled) {
                deathStatsHandled = true;
                onDeathObserved();
            }
            if (hasAuthority() && !deathHandled) {
                deathHandled = true;
                onBeforeDestroy();
            }
            if (getWorld() != null) {
                getWorld().destroyActor(this);
            }
            return;
        }

        if (!hasAuthority()) {
            return;
        }

        MovementComponent movement = getComponent(MovementComponent.class);
        if (movement == null) {
            return;
        }

        if (isAggressiveToPlayers()) {
            PlayerCharacter chaseTarget = sensePlayers();
            updateContactDamage(delta, sensedContactPlayers);

            if (chaseTarget != null) {
                chasingPlayer = true;
                updateChase(delta, chaseTarget, movement);
                return;
            }

            if (chasingPlayer) {
                chasingPlayer = false;
                enterIdle(movement);
            }
        } else {
            contactDamageTimers.clear();
            sensedContactPlayers.clear();
        }

        updateWander(delta);
    }

    protected float getMoveSpeed() {
        return 1.1f;
    }

    protected float getWanderRadius() {
        return 3f;
    }

    protected boolean isAggressiveToPlayers() {
        return false;
    }

    protected float getChaseRadius() {
        return 0f;
    }

    protected float getContactDamageAmount() {
        return 0f;
    }

    protected float getContactDamageInterval() {
        return 1f;
    }

    protected float getContactPadding() {
        return 0.08f;
    }

    protected abstract String getIdleRegionName();

    protected abstract String getWalkRegionName();

    protected boolean usesRightFacingSourceFrames() {
        return false;
    }

    protected void onBeforeDestroy() {
    }

    protected void onDeathObserved() {
    }

    /** Zbiera stan stworzenia potrzebny do zapisu i późniejszego odtworzenia. */
    public SaveGameData.CreatureData buildSaveData() {
        SaveGameData.CreatureData data = new SaveGameData.CreatureData();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        HealthComponent health = getComponent(HealthComponent.class);
        CreatureKind creatureKind = resolveCreatureKind();

        data.creatureKind = creatureKind != null ? creatureKind.name() : "";
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
        return data;
    }

    protected final void spawnItemDrops(ItemDefinition itemDefinition, int minCount, int maxCount) {
        if (getWorld() == null || itemDefinition == null || maxCount <= 0) {
            return;
        }

        int dropCount = MathUtils.random(Math.max(0, minCount), Math.max(minCount, maxCount));
        if (dropCount <= 0) {
            return;
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        Vector2 position = transform != null ? transform.getPosition() : getPosition();
        Vector2 size = transform != null ? transform.getSize() : new Vector2(1f, 1f);
        float centerX = position.x + size.x * 0.5f;
        float centerY = position.y + Math.min(size.y * 0.3f, 0.8f);

        for (int index = 0; index < dropCount; index++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float radius = MathUtils.random(0.2f, 0.55f);
            Vector2 dropPosition = new Vector2(
                centerX + MathUtils.cos(angle) * radius - 0.25f,
                centerY + MathUtils.sin(angle) * radius - 0.25f
            );

            ItemPickupActor pickupActor = new ItemPickupActor();
            if (isReplicated()) {
                pickupActor.configureServer(itemDefinition, 1);
                pickupActor.setReplicated(true);
            } else {
                pickupActor.configure(itemDefinition, 1);
            }
            getWorld().spawnActor(pickupActor, dropPosition);
        }
    }

    private void addSharedComponents(float sizeW,
                                     float sizeH,
                                     float collHalfW,
                                     float collHalfH,
                                     Vector2 collOffset,
                                     float sortOffsetY,
                                     int zOrder,
                                     float maxHealth,
                                     float currentHealth) {
        addComponent(new TransformComponent(
            new Vector2(),
            zOrder,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            sortOffsetY
        ));
        addComponent(new MovementComponent(getMoveSpeed()));
        addComponent(new KnockbackComponent());
        addComponent(new HealthComponent(maxHealth, currentHealth));
        addComponent(new DamageReactionComponent());
        addComponent(CombatComponent.createPassiveTarget());
        addComponent(new BoxCollisionComponent(CollisionProfile.ENEMY, collHalfW, collHalfH, collOffset));
    }

    /**
     * Proste AI spaceru.
     * Stworzenie stoi chwilę w miejscu, losuje punkt w pobliżu domu i próbuje do niego dojść.
     */
    private void updateWander(float delta) {
        MovementComponent movement = getComponent(MovementComponent.class);
        if (movement == null) {
            return;
        }

        KnockbackComponent knockback = getComponent(KnockbackComponent.class);
        if (knockback != null && knockback.isActive()) {
            movement.getDirection().setZero();
            movementAttemptTimer = 0f;
            return;
        }

        Vector2 currentPosition = getNavigationPosition(this, navigationPosition);
        if (!homeInitialized) {
            homePosition.set(currentPosition);
            homeInitialized = true;
            enterIdle(movement);
            return;
        }

        if (!hasTarget) {
            movement.getDirection().setZero();
            idleRemaining = Math.max(0f, idleRemaining - delta);
            if (idleRemaining > 0f) {
                return;
            }

            if (!chooseNewTarget(currentPosition)) {
                enterIdle(movement);
                return;
            }
        }

        float targetReachedDistance2 = TARGET_REACHED_DISTANCE * TARGET_REACHED_DISTANCE;
        if (currentPosition.dst2(targetPosition) <= targetReachedDistance2) {
            enterIdle(movement);
            return;
        }

        movement.getDirection().set(targetPosition).sub(currentPosition);
        if (movement.getDirection().isZero(0.001f)) {
            enterIdle(movement);
            return;
        }
        movement.getDirection().nor();
        movementAttemptTimer += delta;

        CollisionComponent collision = getComponentByType(CollisionComponent.class);
        Body body = collision != null ? collision.getBody() : null;
        if (body == null) {
            return;
        }

        if (movementAttemptTimer >= STUCK_TIMEOUT
            && body.getLinearVelocity().len2() <= STUCK_VELOCITY_EPSILON
            && currentPosition.dst2(targetPosition) > targetReachedDistance2) {
            enterIdle(movement);
        }
    }

    /** Wraca do stanu bez celu i losuje nową długość postoju. */
    private void enterIdle(MovementComponent movement) {
        hasTarget = false;
        movementAttemptTimer = 0f;
        idleRemaining = MathUtils.random(MIN_IDLE_DURATION, MAX_IDLE_DURATION);
        movement.getDirection().setZero();
    }

    /** Szuka nowego celu spaceru w promieniu od pozycji domowej. */
    private boolean chooseNewTarget(Vector2 currentPosition) {
        movementAttemptTimer = 0f;
        idleRemaining = 0f;

        float minDistance2 = MIN_WANDER_DISTANCE * MIN_WANDER_DISTANCE;
        for (int attempt = 0; attempt < MAX_TARGET_ATTEMPTS; attempt++) {
            float radius = MathUtils.random(MIN_WANDER_DISTANCE, getWanderRadius());
            float angle = MathUtils.random(0f, MathUtils.PI2);
            targetPosition.set(homePosition.x + MathUtils.cos(angle) * radius,
                homePosition.y + MathUtils.sin(angle) * radius);

            if (currentPosition.dst2(targetPosition) >= minDistance2) {
                hasTarget = true;
                return true;
            }
        }

        hasTarget = false;
        return false;
    }

    private void updateChase(float delta, PlayerCharacter player, MovementComponent movement) {
        KnockbackComponent knockback = getComponent(KnockbackComponent.class);
        if (knockback != null && knockback.isActive()) {
            movement.getDirection().setZero();
            movementAttemptTimer = 0f;
            return;
        }

        Vector2 currentPosition = getNavigationPosition(this, navigationPosition);
        if (!homeInitialized) {
            homePosition.set(currentPosition);
            homeInitialized = true;
        }

        Vector2 playerPosition = getNavigationPosition(player, playerNavigationPosition);
        chaseDirection.set(playerPosition).sub(currentPosition);
        if (chaseDirection.isZero(0.001f)) {
            movement.getDirection().setZero();
            movementAttemptTimer = 0f;
            return;
        }

        hasTarget = false;
        idleRemaining = 0f;
        movementAttemptTimer = 0f;
        movement.getDirection().set(chaseDirection).nor();
    }

    private PlayerCharacter sensePlayers() {
        sensedContactPlayers.clear();

        if (getWorld() == null) {
            return null;
        }

        float chaseRadius = getChaseRadius();
        if (chaseRadius <= 0f) {
            return null;
        }

        Vector2 currentPosition = getNavigationPosition(this, navigationPosition);
        float chaseRadius2 = chaseRadius * chaseRadius;
        float nearestDistance2 = chaseRadius2;
        PlayerCharacter nearestPlayer = null;

        for (PlayerCharacter player : getWorld().getActorsOfClass(PlayerCharacter.class)) {
            if (player == null || !player.isAlive()) {
                continue;
            }

            Vector2 playerPosition = getNavigationPosition(player, playerNavigationPosition);
            float distance2 = currentPosition.dst2(playerPosition);
            if (distance2 <= nearestDistance2) {
                nearestDistance2 = distance2;
                nearestPlayer = player;
            }

            if (isPlayerInContactRange(currentPosition, playerPosition, player)) {
                sensedContactPlayers.put(player.getActorId(), player);
            }
        }

        return nearestPlayer;
    }

    private void updateContactDamage(float delta, Map<Integer, PlayerCharacter> currentContacts) {
        if (currentContacts.isEmpty()) {
            contactDamageTimers.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Float>> iterator = contactDamageTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Float> entry = iterator.next();
            PlayerCharacter player = currentContacts.get(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            float remaining = entry.getValue() - delta;
            if (remaining <= 0f) {
                applyContactDamage(player);
                remaining = getContactDamageInterval();
            }
            entry.setValue(remaining);
        }

        for (Map.Entry<Integer, PlayerCharacter> entry : currentContacts.entrySet()) {
            if (contactDamageTimers.containsKey(entry.getKey())) {
                continue;
            }

            PlayerCharacter player = entry.getValue();
            if (player == null || !player.isAlive()) {
                continue;
            }

            applyContactDamage(player);
            contactDamageTimers.put(entry.getKey(), getContactDamageInterval());
        }
    }

    private void applyContactDamage(PlayerCharacter player) {
        if (player == null || !player.isAlive()) {
            return;
        }

        float damage = getContactDamageAmount();
        if (damage <= 0f) {
            return;
        }

        HealthComponent playerHealth = player.getComponent(HealthComponent.class);
        if (playerHealth != null) {
            playerHealth.applyDamage(damage, getOwnerId());
        }
    }

    private boolean isPlayerInContactRange(Vector2 currentPosition, Vector2 playerPosition, PlayerCharacter player) {
        float halfWidthSum = getCollisionHalfWidth(this) + getCollisionHalfWidth(player) + getContactPadding();
        float halfHeightSum = getCollisionHalfHeight(this) + getCollisionHalfHeight(player) + getContactPadding();

        return Math.abs(currentPosition.x - playerPosition.x) <= halfWidthSum
            && Math.abs(currentPosition.y - playerPosition.y) <= halfHeightSum;
    }

    /**
     * Zwraca pozycję używaną do AI.
     * Gdy istnieje ciało Box2D, bierze je zamiast surowego transformu.
     */
    private Vector2 getNavigationPosition(AbstractActor actor, Vector2 out) {
        CollisionComponent collision = actor != null ? actor.getComponentByType(CollisionComponent.class) : null;
        if (collision != null && collision.getBody() != null) {
            return out.set(collision.getBody().getPosition());
        }

        TransformComponent transform = actor != null ? actor.getComponent(TransformComponent.class) : null;
        if (transform == null) {
            return actor != null ? out.set(actor.getPosition()) : out.setZero();
        }

        return out.set(transform.getPosition()).add(transform.getSize().x * 0.5f, transform.getSize().y * 0.5f);
    }

    private float getCollisionHalfWidth(AbstractActor actor) {
        BoxCollisionComponent collision = actor != null ? actor.getComponent(BoxCollisionComponent.class) : null;
        if (collision != null) {
            return collision.getHalfWidth();
        }

        TransformComponent transform = actor != null ? actor.getComponent(TransformComponent.class) : null;
        return transform != null ? transform.getSize().x * 0.5f : 0.5f;
    }

    private float getCollisionHalfHeight(AbstractActor actor) {
        BoxCollisionComponent collision = actor != null ? actor.getComponent(BoxCollisionComponent.class) : null;
        if (collision != null) {
            return collision.getHalfHeight();
        }

        TransformComponent transform = actor != null ? actor.getComponent(TransformComponent.class) : null;
        return transform != null ? transform.getSize().y * 0.5f : 0.5f;
    }

    private CreatureKind resolveCreatureKind() {
        if (this instanceof CowActor) {
            return CreatureKind.COW;
        }
        if (this instanceof PigActor) {
            return CreatureKind.PIG;
        }
        if (this instanceof SheepActor) {
            return CreatureKind.SHEEP;
        }
        if (this instanceof ChickenActor) {
            return CreatureKind.CHICKEN;
        }
        if (this instanceof SkeletonActor) {
            return CreatureKind.SKELETON;
        }
        if (this instanceof SlimeActor) {
            return CreatureKind.SLIME;
        }
        return null;
    }

    private float getFloat(Map<String, Object> properties, String key, float defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
