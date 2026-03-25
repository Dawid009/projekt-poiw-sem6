package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;

/**
 * Kamera śledzi Actora z tym komponentem.
 * Powinien być dokładnie JEDEN entity z CameraFollowComponent w danym momencie.
 */
public class CameraFollowComponent extends AbstractActorComponent {
    public static final ComponentMapper<CameraFollowComponent> MAPPER =
        ComponentMapper.getFor(CameraFollowComponent.class);
}
