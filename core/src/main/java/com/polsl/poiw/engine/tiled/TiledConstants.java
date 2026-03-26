package com.polsl.poiw.engine.tiled;

/**
 * Stałe używane przy parsowaniu map Tiled.
 */
public final class TiledConstants {
    /** Piksele na metr (1 tile 16x16 = 1 metr w Box2D) */
    public static final float PPM = 16f;

    /** Nazwy warstw w pliku .tmx (muszą się zgadzać z Tiled) */
    public static final String LAYER_COLLISION = "collision";
    public static final String LAYER_WATER = "water";
    public static final String LAYER_SPAWNS = "spawns";
    public static final String LAYER_OBJECTS = "objects";
    public static final String LAYER_TRIGGERS = "trigger";
    public static final String LAYER_PLAYER_START = "player";

    /** Custom properties na obiektach w Tiled */
    public static final String PROP_TYPE = "type";
    public static final String PROP_ENEMY_ID = "enemyId";
    public static final String PROP_RESPAWN_TIME = "respawnTime";
    public static final String PROP_MAX_CONCURRENT = "maxConcurrent";
    public static final String PROP_LOOT_TABLE_ID = "lootTableId";
    public static final String PROP_DAMAGE = "damage";
    public static final String PROP_DAMAGE_TYPE = "damageType";
    public static final String PROP_COOLDOWN = "cooldown";

    private TiledConstants() {}
}
