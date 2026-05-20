package com.polsl.poiw.gameplay.crop;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.CropActor;
import com.polsl.poiw.gameplay.actor.CropKind;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.item.GameplayItems;

public final class CropPlantingService {
    private static final int MAX_TILE_SCAN_GID = 4096;
    private static final float DEFAULT_CROP_SIZE = 1f;
    private static final float CROP_COLLISION_HALF_WIDTH_RATIO = 0.22f;
    private static final float CROP_COLLISION_HALF_HEIGHT_RATIO = 0.16f;
    private static final float CROP_COLLISION_OFFSET_Y_RATIO = -0.18f;
    private static final int DEFAULT_CROP_Z_ORDER = 1;
    private static final float TILE_SAMPLE_EPSILON = 0.0001f;

    private CropPlantingService() {
    }

    public static boolean tryPlant(PlayerCharacter player,
                                   String itemId,
                                   GameWorld gameWorld,
                                   TiledMap map,
                                   boolean replicatedCrop) {
        CropKind cropKind = resolvePlantableCrop(itemId);
        if (player == null || cropKind == null || gameWorld == null || map == null) {
            return false;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory == null || inventory.getStack(itemId) == null) {
            return false;
        }

        PlantingTile plantingTile = findPlantingTile(player, gameWorld, map);
        if (plantingTile == null) {
            return false;
        }

        int stageZeroTileGid = resolveStageZeroTileGid(map, cropKind);
        if (stageZeroTileGid <= 0) {
            return false;
        }

        if (inventory.removeItem(itemId, 1) <= 0) {
            return false;
        }

        TiledMapTile stageZeroTile = map.getTileSets().getTile(stageZeroTileGid);
        TextureRegion region = stageZeroTile != null ? stageZeroTile.getTextureRegion() : null;
        boolean hasRenderableRegion = isRenderableRegion(region);
        float sizeW = hasRenderableRegion ? region.getRegionWidth() * Main.UNIT_SCALE : DEFAULT_CROP_SIZE;
        float sizeH = hasRenderableRegion ? region.getRegionHeight() * Main.UNIT_SCALE : DEFAULT_CROP_SIZE;
        float collHalfW = sizeW * CROP_COLLISION_HALF_WIDTH_RATIO;
        float collHalfH = sizeH * CROP_COLLISION_HALF_HEIGHT_RATIO;
        float collOffsetY = sizeH * CROP_COLLISION_OFFSET_Y_RATIO;
        float sortOffsetY = sizeH * 0.5f + collOffsetY - collHalfH;

        CropActor crop = new CropActor();
        if (hasRenderableRegion) {
            crop.configure(
                map,
                stageZeroTileGid,
                region,
                cropKind,
                0,
                cropKind.toGlobalStageTileIds(stageZeroTileGid, 0),
                sizeW,
                sizeH,
                collHalfW,
                collHalfH,
                new Vector2(0f, collOffsetY),
                sortOffsetY,
                DEFAULT_CROP_Z_ORDER,
                cropKind.getGrowthIntervalSeconds(),
                cropKind.getMaxHealth()
            );
        } else {
            crop.configureServer(
                stageZeroTileGid,
                cropKind,
                0,
                cropKind.toGlobalStageTileIds(stageZeroTileGid, 0),
                sizeW,
                sizeH,
                collHalfW,
                collHalfH,
                new Vector2(0f, collOffsetY),
                sortOffsetY,
                DEFAULT_CROP_Z_ORDER,
                cropKind.getGrowthIntervalSeconds(),
                cropKind.getMaxHealth()
            );
        }
        crop.setReplicated(replicatedCrop);
        gameWorld.spawnActor(crop, new Vector2(plantingTile.tileX(), plantingTile.tileY()));
        return true;
    }

    public static CropKind resolvePlantableCrop(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        if (GameplayItems.SEEDS_CARROT.getItemId().equals(itemId) || GameplayItems.ITEM_CARROT.getItemId().equals(itemId)) {
            return CropKind.CARROT;
        }
        if (GameplayItems.SEEDS_WHEAT.getItemId().equals(itemId)) {
            return CropKind.WHEAT;
        }
        return null;
    }

    private static PlantingTile findPlantingTile(PlayerCharacter player, GameWorld gameWorld, TiledMap map) {
        TransformComponent transform = player.getComponent(TransformComponent.class);
        BoxCollisionComponent collision = player.getComponent(BoxCollisionComponent.class);
        Vector2 position = player.getPosition();
        float width = transform != null ? transform.getSize().x : 1f;
        float height = transform != null ? transform.getSize().y : 1f;
        float centerX = position.x + width * 0.5f;
        float centerY = position.y + height * 0.5f;
        float halfWidth = collision != null ? collision.getHalfWidth() : width * 0.2f;
        float halfHeight = collision != null ? collision.getHalfHeight() : height * 0.1f;
        Vector2 collisionOffset = collision != null ? collision.getOffset() : Vector2.Zero;
        centerX += collisionOffset.x;
        centerY += collisionOffset.y;

        float left = centerX - halfWidth;
        float right = centerX + halfWidth;
        float bottom = centerY - halfHeight;
        float top = centerY + halfHeight;

        PlantingTile overlappedTile = findBestOverlappedPlantingTile(
            map,
            gameWorld,
            left,
            right,
            bottom,
            top,
            centerX,
            bottom
        );
        if (overlappedTile != null) {
            return overlappedTile;
        }

        int preferredTileX = MathUtils.floor(centerX);
        int tileY = MathUtils.floor(centerY - halfHeight - 0.01f);
        int minTileX = MathUtils.floor(centerX - halfWidth + 0.01f);
        int maxTileX = MathUtils.floor(centerX + halfWidth - 0.01f);

        if (preferredTileX < 0 || tileY < 0) {
            return null;
        }

        PlantingTile preferredTile = resolvePlantingTile(map, gameWorld, preferredTileX, tileY);
        if (preferredTile != null) {
            return preferredTile;
        }

        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            if (tileX == preferredTileX) {
                continue;
            }

            PlantingTile adjacentTile = resolvePlantingTile(map, gameWorld, tileX, tileY);
            if (adjacentTile != null) {
                return adjacentTile;
            }
        }

        return null;
    }

    private static PlantingTile findBestOverlappedPlantingTile(TiledMap map,
                                                               GameWorld gameWorld,
                                                               float left,
                                                               float right,
                                                               float bottom,
                                                               float top,
                                                               float referenceX,
                                                               float referenceY) {
        int minTileX = MathUtils.floor(left);
        int maxTileX = MathUtils.floor(right - TILE_SAMPLE_EPSILON);
        int minTileY = MathUtils.floor(bottom);
        int maxTileY = MathUtils.floor(top - TILE_SAMPLE_EPSILON);

        PlantingTile bestTile = null;
        float bestOverlap = 0f;
        float bestDistanceSq = Float.POSITIVE_INFINITY;

        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                PlantingTile plantingTile = resolvePlantingTile(map, gameWorld, tileX, tileY);
                if (plantingTile == null) {
                    continue;
                }

                float overlapWidth = Math.min(right, tileX + 1f) - Math.max(left, tileX);
                float overlapHeight = Math.min(top, tileY + 1f) - Math.max(bottom, tileY);
                if (overlapWidth <= 0f || overlapHeight <= 0f) {
                    continue;
                }

                float overlapArea = overlapWidth * overlapHeight;
                float dx = tileX + 0.5f - referenceX;
                float dy = tileY + 0.5f - referenceY;
                float distanceSq = dx * dx + dy * dy;
                if (overlapArea > bestOverlap + TILE_SAMPLE_EPSILON
                    || (Math.abs(overlapArea - bestOverlap) <= TILE_SAMPLE_EPSILON && distanceSq < bestDistanceSq)) {
                    bestTile = plantingTile;
                    bestOverlap = overlapArea;
                    bestDistanceSq = distanceSq;
                }
            }
        }

        return bestTile;
    }

    private static PlantingTile resolvePlantingTile(TiledMap map, GameWorld gameWorld, int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || isOccupiedByCrop(gameWorld, tileX, tileY)) {
            return null;
        }

        for (MapLayer layer : map.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }

            TiledMapTileLayer.Cell cell = tileLayer.getCell(tileX, tileY);
            if (cell == null || cell.getTile() == null) {
                continue;
            }

            TiledMapTile tile = cell.getTile();
            String type = tile.getProperties().get("type", String.class);
            if (type == null || type.isBlank()) {
                type = tile.getProperties().get("class", String.class);
            }
            Boolean occupied = tile.getProperties().get("occupied", Boolean.class);
            boolean farmland = "Farmland".equalsIgnoreCase(type)
                || (tile.getProperties().containsKey("occupied") && tile.getProperties().containsKey("watered"));
            if (farmland && !Boolean.TRUE.equals(occupied)) {
                return new PlantingTile(tileX, tileY);
            }
        }

        return null;
    }

    private static boolean isOccupiedByCrop(GameWorld gameWorld, int tileX, int tileY) {
        for (CropActor crop : gameWorld.getActorsOfClass(CropActor.class)) {
            Vector2 cropPosition = crop.getPosition();
            if (MathUtils.floor(cropPosition.x + 0.01f) == tileX && MathUtils.floor(cropPosition.y + 0.01f) == tileY) {
                return true;
            }
        }
        return false;
    }

    private static int resolveStageZeroTileGid(TiledMap map, CropKind cropKind) {
        if (map == null || cropKind == null) {
            return -1;
        }

        for (int gid = 1; gid <= MAX_TILE_SCAN_GID; gid++) {
            TiledMapTile tile = map.getTileSets().getTile(gid);
            if (tile == null) {
                continue;
            }

            String cropType = tile.getProperties().get("crop_type", String.class);
            if (cropType == null || CropKind.fromMetadata(cropType) != cropKind) {
                continue;
            }

            int growthStage = tile.getProperties().get("growth_stage", 0, Integer.class);
            return cropKind.toGlobalStageTileIds(gid, growthStage)[0];
        }

        return -1;
    }

    private static boolean isRenderableRegion(TextureRegion region) {
        return region != null && region.getRegionWidth() > 0 && region.getRegionHeight() > 0;
    }

    private record PlantingTile(int tileX, int tileY) {
    }
}