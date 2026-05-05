package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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

    private ItemDefinition itemDefinition;
    private int quantity;
    private int ignoredActorId = -1;
    private float pickupGraceRemaining = 0f;

    public void configure(ItemDefinition itemDefinition, int quantity) {
        this.itemDefinition = itemDefinition;
        this.quantity = Math.max(1, quantity);

        addComponent(new TransformComponent(
            new Vector2(),
            0,
            new Vector2(ITEM_SIZE, ITEM_SIZE),
            new Vector2(1f, 1f),
            0f,
            0f
        ));

        BoxCollisionComponent collision = new BoxCollisionComponent(
            CollisionProfile.ITEM,
            ITEM_SIZE * 0.5f,
            ITEM_SIZE * 0.5f
        );
        collision.addOverlapListener(this);
        addComponent(collision);
    }

    public void configure(ItemDefinition itemDefinition, int quantity, Skin skin) {
        configure(itemDefinition, quantity);
        // Do czasu dostarczenia artu item lezy jako prosty kolorowy kwadrat.
        TextureRegion whiteRegion = skin.getRegion("white");
        addComponent(new SpriteComponent(whiteRegion, itemDefinition.getDisplayColor()));
    }

    public void setPickupGrace(int actorIdToIgnore, float durationSeconds) {
        // Krotki grace period blokuje natychmiastowe podniesienie itemu po dropie.
        this.ignoredActorId = actorIdToIgnore;
        this.pickupGraceRemaining = Math.max(0f, durationSeconds);
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
        if (pickupGraceRemaining > 0f) {
            pickupGraceRemaining = Math.max(0f, pickupGraceRemaining - delta);
        }
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
}