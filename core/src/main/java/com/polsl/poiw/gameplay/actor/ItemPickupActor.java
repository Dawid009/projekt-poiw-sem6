package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.collision.CollisionResult;
import com.polsl.poiw.engine.collision.OverlapListener;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.character.PlayerCharacter;

public class ItemPickupActor extends AbstractActor implements OverlapListener {

    private static final String TAG = "ItemPickupActor";
    private static final float ITEM_SIZE = 0.5f;
    private static final float BOB_AMPLITUDE = 0.06f;
    private static final float BOB_SPEED = 2.2f;

    private ItemDefinition itemDefinition;
    private int quantity;
    private int ignoredActorId = -1;
    private float pickupGraceRemaining = 0f;
    private final Vector2 baseRenderPosition = new Vector2();
    private boolean bobbingInitialized;
    private float bobTime;

    public void configure(ItemDefinition itemDefinition, int quantity) {
        configure(itemDefinition, quantity, (TextureAtlas) null);
    }

    public void configure(ItemDefinition itemDefinition, int quantity, TextureAtlas itemsAtlas) {
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

        if (itemRegion != null) {
            addComponent(new SpriteComponent(itemRegion, Color.WHITE.cpy()));
        }
    }

    public void configure(ItemDefinition itemDefinition, int quantity, Skin skin) {
        configure(itemDefinition, quantity, (TextureAtlas) null);
        TextureRegion whiteRegion = skin.getRegion("white");
        if (whiteRegion != null) {
            addComponent(new SpriteComponent(whiteRegion, itemDefinition.getDisplayColor()));
        }
    }

    public void setPickupGrace(int actorIdToIgnore, float durationSeconds) {
        // Krotki grace period blokuje natychmiastowe podniesienie itemu po dropie.
        this.ignoredActorId = actorIdToIgnore;
        this.pickupGraceRemaining = Math.max(0f, durationSeconds);
    }

    @Override
    public void beginPlay() {
        super.beginPlay();

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            baseRenderPosition.set(transform.getPosition());
            bobbingInitialized = true;
            bobTime = (getActorId() & 7) * 0.35f;
        }
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
            baseRenderPosition.set(transform.getPosition());
            bobbingInitialized = true;
        }

        bobTime += delta * BOB_SPEED;
        transform.getPosition().set(baseRenderPosition.x,
            baseRenderPosition.y + (float) Math.sin(bobTime) * BOB_AMPLITUDE);
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
            getWorld().destroyActor(this);
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
        if (itemsAtlas == null || definition == null) {
            return null;
        }

        String regionName = definition.getTextureRegionName();
        if (regionName == null || regionName.isBlank()) {
            return null;
        }

        return itemsAtlas.findRegion(regionName);
    }
}