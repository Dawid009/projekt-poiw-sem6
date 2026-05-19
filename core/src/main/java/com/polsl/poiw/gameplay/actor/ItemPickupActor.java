package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.collision.CollisionResult;
import com.polsl.poiw.engine.collision.OverlapListener;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.PickupCollectAnimationComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.item.GameplayItems;

import java.util.HashMap;
import java.util.Map;

public class ItemPickupActor extends AbstractActor implements OverlapListener {

    private static final String TAG = "ItemPickupActor";
    private static final float ITEM_SIZE = 0.5f;
    private static final float BOB_AMPLITUDE = 0.06f;
    private static final float BOB_SPEED = 2.2f;
    private static final float POP_DURATION_BASE = 0.22f;
    private static final float POP_HEIGHT_BASE = 0.18f;
    private static final float COLLECT_DURATION = 0.16f;
    private static final float COLLECT_END_SCALE = 0.35f;
    public static final String PROP_ITEM_ID = "itemId";
    public static final String PROP_QUANTITY = "quantity";

    private static TextureRegion fallbackWhiteRegion;

    private ItemDefinition itemDefinition;
    private int quantity;
    private int ignoredActorId = -1;
    private float pickupGraceRemaining = 0f;
    private boolean bobbingInitialized;
    private float bobTime;
    private float popTime;
    private float popDuration;
    private float popHeight;
    private boolean collectAnimating;
    private float collectTime;
    private final Vector2 collectStart = new Vector2();
    private final Vector2 collectTarget = new Vector2();

    public void configure(ItemDefinition itemDefinition, int quantity) {
        configure(itemDefinition, quantity, (TextureAtlas) null);
    }

    public void configure(ItemDefinition itemDefinition, int quantity, TextureAtlas itemsAtlas) {
        configureInternal(itemDefinition, quantity, itemsAtlas, null, true);
    }

    public void configure(ItemDefinition itemDefinition, int quantity, TextureAtlas itemsAtlas, Skin skin) {
        configureInternal(itemDefinition, quantity, itemsAtlas, skin, true);
    }

    public void configureServer(ItemDefinition itemDefinition, int quantity) {
        configureInternal(itemDefinition, quantity, null, null, false);
    }

    public void configureFromReplication(Map<String, Object> initialProperties, TextureAtlas itemsAtlas, Skin skin) {
        Object itemId = initialProperties != null ? initialProperties.get(PROP_ITEM_ID) : null;
        Object quantityValue = initialProperties != null ? initialProperties.get(PROP_QUANTITY) : null;
        ItemDefinition definition = itemId instanceof String value ? GameplayItems.findById(value) : null;
        int replicatedQuantity = quantityValue instanceof Number number ? number.intValue() : 1;
        configureInternal(definition, replicatedQuantity, itemsAtlas, skin, true);
    }

    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        if (itemDefinition != null) {
            properties.put(PROP_ITEM_ID, itemDefinition.getItemId());
        }
        properties.put(PROP_QUANTITY, quantity);
        return properties;
    }

    private void configureInternal(ItemDefinition itemDefinition,
                                   int quantity,
                                   TextureAtlas itemsAtlas,
                                   Skin skin,
                                   boolean createSprite) {
        this.itemDefinition = itemDefinition;
        this.quantity = Math.max(1, quantity);

        TextureRegion itemRegion = findItemRegion(itemsAtlas, itemDefinition);
        float sizeW = itemRegion != null ? itemRegion.getRegionWidth() * Main.UNIT_SCALE : ITEM_SIZE;
        float sizeH = itemRegion != null ? itemRegion.getRegionHeight() * Main.UNIT_SCALE : ITEM_SIZE;

        addComponent(new TransformComponent(
            new Vector2(),
            0,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            0f
        ));

        BoxCollisionComponent collision = new BoxCollisionComponent(
            CollisionProfile.ITEM,
            sizeW * 0.5f,
            sizeH * 0.5f
        );
        collision.addOverlapListener(this);
        addComponent(collision);
        addComponent(new PickupCollectAnimationComponent());

        if (!createSprite) {
            return;
        }

        if (itemRegion != null) {
            addComponent(new SpriteComponent(itemRegion, Color.WHITE.cpy()));
            return;
        }

        TextureRegion whiteRegion = skin != null ? skin.getRegion("white") : getFallbackWhiteRegion();
        if (whiteRegion != null && itemDefinition != null) {
            addComponent(new SpriteComponent(whiteRegion, itemDefinition.getDisplayColor()));
        }
    }

    public void configure(ItemDefinition itemDefinition, int quantity, Skin skin) {
        configureInternal(itemDefinition, quantity, null, skin, true);
    }

    public void setPickupGrace(int actorIdToIgnore, float durationSeconds) {
        // Krotki grace period blokuje natychmiastowe podniesienie itemu po dropie.
        this.ignoredActorId = actorIdToIgnore;
        this.pickupGraceRemaining = Math.max(0f, durationSeconds);
    }

    @Override
    public void beginPlay() {
        super.beginPlay();

        initializeVisualMotion();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
        if (pickupGraceRemaining > 0f) {
            pickupGraceRemaining = Math.max(0f, pickupGraceRemaining - delta);
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }

        if (!bobbingInitialized) {
            initializeVisualMotion();
        }

        PickupCollectAnimationComponent collectAnimation = getComponent(PickupCollectAnimationComponent.class);
        if (collectAnimation != null && (collectAnimation.consumeStartTrigger()
            || (collectAnimation.isCollecting() && !collectAnimating))) {
            beginCollectionAnimation(transform, collectAnimation);
        }

        if (collectAnimating && collectAnimation != null && collectAnimation.isCollecting()) {
            updateCollectionAnimation(delta, transform, collectAnimation);
            return;
        }

        float popOffsetY = 0f;
        if (popTime < popDuration) {
            popTime = Math.min(popDuration, popTime + delta);
            float progress = popDuration > 0f ? popTime / popDuration : 1f;
            popOffsetY = 4f * popHeight * progress * (1f - progress);
        } else {
            bobTime += delta * BOB_SPEED;
        }

        transform.getScaling().set(1f, 1f);
        float bobOffsetY = popTime >= popDuration ? (float) Math.sin(bobTime) * BOB_AMPLITUDE : 0f;
        transform.setRenderOffset(0f, popOffsetY + bobOffsetY);
    }

    @Override
    public void onBeginOverlap(Actor self, Actor other, CollisionResult result) {
        if (!hasAuthority()) {
            return;
        }
        if (!(other instanceof PlayerCharacter player)) {
            return;
        }
        if (pickupGraceRemaining > 0f && other.getActorId() == ignoredActorId) {
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory == null || itemDefinition == null || quantity <= 0) {
            return;
        }

        int added = inventory.addItem(itemDefinition, quantity);
        if (added <= 0) {
            return;
        }

        quantity -= added;
        Gdx.app.debug(TAG, "Actor #" + other.getActorId() + " picked up "
            + itemDefinition.getDisplayName() + " x" + added);

        if (quantity <= 0 && getWorld() != null) {
            PickupCollectAnimationComponent collectAnimation = getComponent(PickupCollectAnimationComponent.class);
            TransformComponent itemTransform = getComponent(TransformComponent.class);
            TransformComponent playerTransform = player.getComponent(TransformComponent.class);
            if (collectAnimation != null && itemTransform != null && playerTransform != null) {
                float targetX = playerTransform.getPosition().x + playerTransform.getSize().x * 0.5f
                    - itemTransform.getSize().x * 0.5f;
                float targetY = playerTransform.getPosition().y + playerTransform.getSize().y * 0.5f
                    - itemTransform.getSize().y * 0.5f;
                collectAnimation.startCollection(targetX, targetY, COLLECT_DURATION);
            } else {
                getWorld().destroyActor(this);
            }
        }
    }

    @Override
    public void onEndOverlap(Actor self, Actor other) {
    }

    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    public int getQuantity() {
        return quantity;
    }

    private TextureRegion findItemRegion(TextureAtlas itemsAtlas, ItemDefinition definition) {
        if (definition == null) {
            return null;
        }

        TextureAtlas atlas = itemsAtlas != null ? itemsAtlas : resolveItemsAtlas();
        if (atlas == null) {
            return null;
        }

        String regionName = definition.getTextureRegionName();
        if (regionName == null || regionName.isBlank()) {
            return null;
        }

        return atlas.findRegion(regionName);
    }

    private TextureAtlas resolveItemsAtlas() {
        if (Gdx.app == null) {
            return null;
        }
        if (!(Gdx.app.getApplicationListener() instanceof Main main)) {
            return null;
        }

        try {
            return main.getAssetService().get(AtlasAsset.ITEMS);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void initializeVisualMotion() {
        bobbingInitialized = true;
        bobTime = (getActorId() & 7) * 0.35f;
        popDuration = POP_DURATION_BASE + ((getActorId() >> 1) & 3) * 0.02f;
        popHeight = POP_HEIGHT_BASE + ((getActorId() >> 3) & 3) * 0.03f;
        popTime = 0f;
        collectAnimating = false;
        collectTime = 0f;
    }

    private void beginCollectionAnimation(TransformComponent transform,
                                          PickupCollectAnimationComponent collectAnimation) {
        collectAnimating = true;
        collectTime = 0f;
        collectStart.set(transform.getPosition());
        collectTarget.set(collectAnimation.getTargetX(), collectAnimation.getTargetY());
        transform.setRenderOffset(0f, 0f);

        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        if (collision != null) {
            collision.setEnabled(false);
            collision.setSensorOverride(true);
            if (collision.getBody() != null) {
                collision.getBody().setLinearVelocity(0f, 0f);
            }
        }
    }

    private void updateCollectionAnimation(float delta,
                                           TransformComponent transform,
                                           PickupCollectAnimationComponent collectAnimation) {
        float duration = Math.max(0.01f, collectAnimation.getDurationSeconds());
        collectTime = Math.min(duration, collectTime + delta);

        float progress = collectTime / duration;
        float posX = MathUtils.lerp(collectStart.x, collectTarget.x, progress);
        float posY = MathUtils.lerp(collectStart.y, collectTarget.y, progress);
        float scale = MathUtils.lerp(1f, COLLECT_END_SCALE, progress);

        transform.getPosition().set(posX, posY);
        transform.getScaling().set(scale, scale);
        transform.setRenderOffset(0f, 0f);

        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        if (collision != null && collision.getBody() != null) {
            collision.getBody().setTransform(
                posX + transform.getSize().x * 0.5f,
                posY + transform.getSize().y * 0.5f,
                0f
            );
            collision.getBody().setLinearVelocity(0f, 0f);
        }

        if (progress >= 1f && hasAuthority() && getWorld() != null) {
            getWorld().destroyActor(this);
        }
    }

    private TextureRegion getFallbackWhiteRegion() {
        if (fallbackWhiteRegion != null) {
            return fallbackWhiteRegion;
        }

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        fallbackWhiteRegion = new TextureRegion(texture);
        return fallbackWhiteRegion;
    }
}