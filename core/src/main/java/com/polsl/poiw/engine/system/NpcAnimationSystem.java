package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.polsl.poiw.engine.component.NpcAnimationComponent;
import com.polsl.poiw.engine.component.SpriteComponent;

public class NpcAnimationSystem extends IteratingSystem {
    public NpcAnimationSystem() {
        super(Family.all(SpriteComponent.class, NpcAnimationComponent.class).get(), 16);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SpriteComponent sprite = SpriteComponent.MAPPER.get(entity);
        NpcAnimationComponent animation = NpcAnimationComponent.MAPPER.get(entity);
        if (sprite == null || animation == null) {
            return;
        }

        animation.update(deltaTime);
        sprite.setRegion(animation.getCurrentFrame());
    }
}
