package com.polsl.poiw.input;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.component.ControllerComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * Stan gry — przekazuje komendy do wszystkich entity z ControllerComponent.
 *
 * Kiedy gracz wciska WASD, GameControllerState dodaje odpowiedni Command
 * do listy pressedCommands / releasedCommands na ControllerComponent.
 * ControllerSystem potem to odczytuje.
 */
public class GameControllerState implements ControllerState {

    private final ImmutableArray<Entity> controllerEntities;

    public GameControllerState(Engine engine) {
        this.controllerEntities = engine.getEntitiesFor(
            Family.all(ControllerComponent.class).get()
        );
    }

    @Override
    public void keyDown(Command command) {
        for (Entity entity : controllerEntities) {
            if (isRemoteEntity(entity)) continue;
            ControllerComponent.MAPPER.get(entity).getPressedCommands().add(command);
        }
    }

    @Override
    public void keyUp(Command command) {
        for (Entity entity : controllerEntities) {
            if (isRemoteEntity(entity)) continue;
            ControllerComponent.MAPPER.get(entity).getReleasedCommands().add(command);
        }
    }

    // checks if entity is a remote player (SIMULATED_PROXY) - does not receive local inpu
    private boolean isRemoteEntity(Entity entity) {
        TransformComponent tc = TransformComponent.MAPPER.get(entity);
        if (tc == null) return false;
        Actor owner = tc.getOwner();
        return owner != null && owner.getNetRole() == NetRole.SIMULATED_PROXY;
    }
}
