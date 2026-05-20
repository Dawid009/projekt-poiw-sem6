package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.ControllerComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.TransformComponent;
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
        // SIMULATED_PROXY doesnt process local input
        TransformComponent tc = TransformComponent.MAPPER.get(entity);
        if (tc != null && tc.getOwner() != null && tc.getOwner().getNetRole() == NetRole.SIMULATED_PROXY) {
            return;
        }

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
                case SELECT -> requestAttack(entity);
                case TOOL_PREVIOUS -> cycleTool(entity, false);
                case TOOL_NEXT -> cycleTool(entity, true);
                case CANCEL -> {
                }
            }
        }
        controller.getPressedCommands().clear();

        for (Command command : controller.getReleasedCommands()) {
            switch (command) {
                case UP -> moveEntity(entity, 0f, -1f);
                case DOWN -> moveEntity(entity, 0f, 1f);
                case LEFT -> moveEntity(entity, 1f, 0f);
                case RIGHT -> moveEntity(entity, -1f, 0f);
                case SELECT, CANCEL, TOOL_PREVIOUS, TOOL_NEXT -> {
                }
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

    private void requestAttack(Entity entity) {
        ControllerComponent controller = ControllerComponent.MAPPER.get(entity);
        if (controller != null) {
            controller.triggerAttackInput();
        }

        CombatComponent combat = CombatComponent.MAPPER.get(entity);
        if (combat != null) {
            combat.requestAttack();
        }
    }

    private void cycleTool(Entity entity, boolean next) {
        PlayerToolComponent toolComponent = PlayerToolComponent.MAPPER.get(entity);
        if (toolComponent == null) {
            return;
        }

        if (next) {
            toolComponent.cycleNext();
        } else {
            toolComponent.cyclePrevious();
        }
    }
}
