package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.polsl.poiw.engine.component.ControllerComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.input.Command;

/**
 * System sterowania — odczytuje komendy z ControllerComponent i tłumaczy na ruch.
 *
 * Wciśnięcie UP dodaje +1 do direction.y, zwolnienie UP odejmuje -1.
 * Dzięki temu jednoczesne wciśnięcie UP+RIGHT daje normalny wektor po normalizacji.
 */
public class ControllerSystem extends IteratingSystem {

    public ControllerSystem() {
        super(Family.all(ControllerComponent.class).get(), 5);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ControllerComponent controller = ControllerComponent.MAPPER.get(entity);
        if (controller.getPressedCommands().isEmpty() && controller.getReleasedCommands().isEmpty()) {
            return;
        }

        for (Command command : controller.getPressedCommands()) {
            switch (command) {
                case UP -> moveEntity(entity, 0f, 1f);
                case DOWN -> moveEntity(entity, 0f, -1f);
                case LEFT -> moveEntity(entity, -1f, 0f);
                case RIGHT -> moveEntity(entity, 1f, 0f);
            }
        }
        controller.getPressedCommands().clear();

        for (Command command : controller.getReleasedCommands()) {
            switch (command) {
                case UP -> moveEntity(entity, 0f, -1f);
                case DOWN -> moveEntity(entity, 0f, 1f);
                case LEFT -> moveEntity(entity, 1f, 0f);
                case RIGHT -> moveEntity(entity, -1f, 0f);
            }
        }
        controller.getReleasedCommands().clear();
    }

    private void moveEntity(Entity entity, float dx, float dy) {
        MovementComponent move = MovementComponent.MAPPER.get(entity);
        if (move != null) {
            move.getDirection().x += dx;
            move.getDirection().y += dy;
        }
    }
}
