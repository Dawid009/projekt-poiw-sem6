package com.polsl.poiw.engine.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.level.WorldContext;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.AbstractCreatureActor;
import com.polsl.poiw.gameplay.actor.CropActor;
import com.polsl.poiw.gameplay.actor.CropKind;
import com.polsl.poiw.gameplay.actor.CreatureKind;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.actor.MineableActor;
import com.polsl.poiw.gameplay.actor.MineableKind;
import com.polsl.poiw.gameplay.actor.TiledVisualActor;
import com.polsl.poiw.gameplay.actor.TreeActor;
import com.polsl.poiw.gameplay.actor.TreeKind;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.item.GameplayItems;

import java.util.ArrayList;
import java.util.List;

public class SinglePlayerSaveService {
    public static final int SLOT_COUNT = 3;

    private static final String PREFS_NAME = "poiw-savegames";
    private static final String SLOT_KEY_PREFIX = "slot.";

    private final Json json = new Json();
    private int activeSlotIndex = -1;
    private SaveGameData pendingLoadedSave;
    private float activePlayTimeSeconds;

    public SinglePlayerSaveService() {
        json.setIgnoreUnknownFields(true);
    }

    public List<SaveSlotSummary> getSlotSummaries() {
        List<SaveSlotSummary> summaries = new ArrayList<>(SLOT_COUNT);
        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            SaveGameData data = loadSlot(slotIndex);
            summaries.add(new SaveSlotSummary(
                slotIndex,
                data != null,
                data != null ? Math.max(0f, data.totalPlayTimeSeconds) : 0f,
                data != null ? Math.max(0L, data.lastPlayedEpochMillis) : 0L
            ));
        }
        return summaries;
    }

    public SaveSlotSummary getSlotSummary(int slotIndex) {
        validateSlotIndex(slotIndex);
        SaveGameData data = loadSlot(slotIndex);
        return new SaveSlotSummary(
            slotIndex,
            data != null,
            data != null ? Math.max(0f, data.totalPlayTimeSeconds) : 0f,
            data != null ? Math.max(0L, data.lastPlayedEpochMillis) : 0L
        );
    }

    public void selectSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        activeSlotIndex = slotIndex;
        pendingLoadedSave = loadSlot(slotIndex);
        activePlayTimeSeconds = pendingLoadedSave != null ? Math.max(0f, pendingLoadedSave.totalPlayTimeSeconds) : 0f;
    }

    public void deleteSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        Preferences preferences = getPreferences();
        preferences.remove(buildSlotKey(slotIndex));
        preferences.flush();
        if (activeSlotIndex == slotIndex) {
            endActiveSession();
        }
    }

    public SaveGameData loadActiveSlot() {
        return pendingLoadedSave;
    }

    public Vector2 resolvePlayerSpawn(Vector2 fallbackPosition) {
        if (pendingLoadedSave == null || pendingLoadedSave.player == null) {
            return fallbackPosition != null ? new Vector2(fallbackPosition) : new Vector2();
        }

        return new Vector2(pendingLoadedSave.player.x, pendingLoadedSave.player.y);
    }

    public void setPendingLoadedSave(SaveGameData saveGameData) {
        this.pendingLoadedSave = saveGameData;
    }

    public int getActiveSlotIndex() {
        return activeSlotIndex;
    }

    public boolean hasActiveSlot() {
        return activeSlotIndex >= 0;
    }

    public float getActivePlayTimeSeconds() {
        return Math.max(0f, activePlayTimeSeconds);
    }

    public void setActivePlayTimeSeconds(float activePlayTimeSeconds) {
        this.activePlayTimeSeconds = Math.max(0f, activePlayTimeSeconds);
    }

    public void addPlayTime(float deltaSeconds) {
        if (activeSlotIndex < 0 || deltaSeconds <= 0f) {
            return;
        }

        activePlayTimeSeconds += deltaSeconds;
    }

    public boolean saveCurrentGame(WorldContext context) {
        if (activeSlotIndex < 0 || context == null || context.getGameWorld() == null) {
            return false;
        }

        PlayerCharacter player = resolvePlayer(context);
        if (player == null) {
            return false;
        }

        SaveGameData saveGameData = new SaveGameData();
        saveGameData.levelId = context.getLevelDefinition() != null ? context.getLevelDefinition().getLevelId() : "game";
        saveGameData.lastPlayedEpochMillis = System.currentTimeMillis();
        saveGameData.totalPlayTimeSeconds = getActivePlayTimeSeconds();
        saveGameData.player = player.buildSaveData();

        GameWorld gameWorld = context.getGameWorld();
        for (TreeActor treeActor : gameWorld.getActorsOfClass(TreeActor.class)) {
            saveGameData.trees.add(treeActor.buildSaveData());
        }
        for (MineableActor mineableActor : gameWorld.getActorsOfClass(MineableActor.class)) {
            saveGameData.mineables.add(mineableActor.buildSaveData());
        }
        for (AbstractCreatureActor creatureActor : gameWorld.getActorsOfClass(AbstractCreatureActor.class)) {
            saveGameData.creatures.add(creatureActor.buildSaveData());
        }
        for (CropActor cropActor : gameWorld.getActorsOfClass(CropActor.class)) {
            saveGameData.crops.add(cropActor.buildSaveData());
        }
        for (TiledVisualActor visualActor : gameWorld.getActorsOfClass(TiledVisualActor.class)) {
            saveGameData.visuals.add(visualActor.buildSaveData());
        }
        for (ItemPickupActor pickupActor : gameWorld.getActorsOfClass(ItemPickupActor.class)) {
            SaveGameData.ItemPickupData pickupData = pickupActor.buildSaveData();
            if (pickupData.itemId != null && !pickupData.itemId.isBlank() && pickupData.quantity > 0) {
                saveGameData.itemPickups.add(pickupData);
            }
        }

        writeSlot(activeSlotIndex, saveGameData);
        pendingLoadedSave = saveGameData;
        activePlayTimeSeconds = saveGameData.totalPlayTimeSeconds;
        return true;
    }

    public void applyPendingSave(WorldContext context, PlayerCharacter player, AssetService assetService) {
        if (pendingLoadedSave == null || context == null || player == null || assetService == null) {
            return;
        }

        GameWorld gameWorld = context.getGameWorld();
        TiledMap map = context.getTiledParser() != null ? context.getTiledParser().getCurrentMap() : null;
        if (gameWorld == null || map == null) {
            return;
        }

        clearRestorableActors(gameWorld);

        TextureAtlas creaturesAtlas = assetService.get(AtlasAsset.CREATURES);
        TextureAtlas itemsAtlas = assetService.get(AtlasAsset.ITEMS);

        for (SaveGameData.TreeData treeData : pendingLoadedSave.trees) {
            restoreTree(gameWorld, map, treeData);
        }
        for (SaveGameData.MineableData mineableData : pendingLoadedSave.mineables) {
            restoreMineable(gameWorld, map, mineableData);
        }
        for (SaveGameData.CreatureData creatureData : pendingLoadedSave.creatures) {
            restoreCreature(gameWorld, creaturesAtlas, creatureData);
        }
        for (SaveGameData.CropData cropData : pendingLoadedSave.crops) {
            restoreCrop(gameWorld, map, cropData);
        }
        for (SaveGameData.VisualData visualData : pendingLoadedSave.visuals) {
            restoreVisual(gameWorld, map, visualData);
        }
        for (SaveGameData.ItemPickupData pickupData : pendingLoadedSave.itemPickups) {
            restoreItemPickup(gameWorld, itemsAtlas, pickupData);
        }

        player.applySaveData(pendingLoadedSave.player);
        activePlayTimeSeconds = Math.max(activePlayTimeSeconds, pendingLoadedSave.totalPlayTimeSeconds);
        pendingLoadedSave = null;
    }

    public SaveGameData loadSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        Preferences preferences = getPreferences();
        String rawJson = preferences.getString(buildSlotKey(slotIndex), "");
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            SaveGameData data = json.fromJson(SaveGameData.class, rawJson);
            return data != null ? data : null;
        } catch (RuntimeException exception) {
            Gdx.app.error("SinglePlayerSaveService", "Nie udalo sie odczytac save slotu " + (slotIndex + 1), exception);
            return null;
        }
    }

    public void writeSlot(int slotIndex, SaveGameData saveGameData) {
        validateSlotIndex(slotIndex);
        if (saveGameData == null) {
            return;
        }

        Preferences preferences = getPreferences();
        preferences.putString(buildSlotKey(slotIndex), json.toJson(saveGameData));
        preferences.flush();
    }

    public void endActiveSession() {
        activeSlotIndex = -1;
        pendingLoadedSave = null;
        activePlayTimeSeconds = 0f;
    }

    private Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    private PlayerCharacter resolvePlayer(WorldContext context) {
        if (context.getPlayerController() == null) {
            return null;
        }

        return context.getPlayerController().getPossessedPawn() instanceof PlayerCharacter player ? player : null;
    }

    private void clearRestorableActors(GameWorld gameWorld) {
        destroyActors(gameWorld, gameWorld.getActorsOfClass(TreeActor.class));
        destroyActors(gameWorld, gameWorld.getActorsOfClass(MineableActor.class));
        destroyActors(gameWorld, gameWorld.getActorsOfClass(AbstractCreatureActor.class));
        destroyActors(gameWorld, gameWorld.getActorsOfClass(CropActor.class));
        destroyActors(gameWorld, gameWorld.getActorsOfClass(TiledVisualActor.class));
        destroyActors(gameWorld, gameWorld.getActorsOfClass(ItemPickupActor.class));
        gameWorld.flushDeferredChanges();
    }

    private <T> void destroyActors(GameWorld gameWorld, List<T> actors) {
        if (gameWorld == null || actors == null || actors.isEmpty()) {
            return;
        }

        for (Object actor : new ArrayList<>(actors)) {
            if (actor instanceof com.polsl.poiw.engine.actor.Actor worldActor) {
                gameWorld.destroyActor(worldActor);
            }
        }
    }

    private void restoreTree(GameWorld gameWorld, TiledMap map, SaveGameData.TreeData treeData) {
        if (treeData == null || treeData.tileGid <= 0) {
            return;
        }

        TreeActor treeActor = new TreeActor();
        treeActor.setTreeKind(TreeKind.valueOf(treeData.treeKind));
        treeActor.setStumpTileGid(treeData.stumpTileGid);
        treeActor.setStumpSize(treeData.stumpWidth, treeData.stumpHeight);
        treeActor.setStumpCollision(treeData.stumpCollHalfW, treeData.stumpCollHalfH,
            new Vector2(treeData.stumpCollOffsetX, treeData.stumpCollOffsetY));
        treeActor.configure(
            map,
            treeData.tileGid,
            map.getTileSets().getTile(treeData.tileGid) != null ? map.getTileSets().getTile(treeData.tileGid).getTextureRegion() : null,
            treeData.sizeW,
            treeData.sizeH,
            treeData.collHalfW,
            treeData.collHalfH,
            new Vector2(treeData.collOffsetX, treeData.collOffsetY),
            treeData.sortOffsetY,
            treeData.zOrder,
            treeData.maxHealth,
            treeData.currentHealth
        );
        gameWorld.spawnActor(treeActor, new Vector2(treeData.x, treeData.y));
    }

    private void restoreMineable(GameWorld gameWorld, TiledMap map, SaveGameData.MineableData mineableData) {
        if (mineableData == null || mineableData.tileGid <= 0) {
            return;
        }

        MineableActor mineableActor = new MineableActor();
        mineableActor.setMineableKind(MineableKind.valueOf(mineableData.mineableKind));
        mineableActor.setDropCountRange(mineableData.minDropCount, mineableData.maxDropCount);
        mineableActor.configure(
            map,
            mineableData.tileGid,
            map.getTileSets().getTile(mineableData.tileGid) != null ? map.getTileSets().getTile(mineableData.tileGid).getTextureRegion() : null,
            mineableData.sizeW,
            mineableData.sizeH,
            mineableData.collHalfW,
            mineableData.collHalfH,
            new Vector2(mineableData.collOffsetX, mineableData.collOffsetY),
            mineableData.sortOffsetY,
            mineableData.zOrder,
            mineableData.maxHealth,
            mineableData.currentHealth
        );
        gameWorld.spawnActor(mineableActor, new Vector2(mineableData.x, mineableData.y));
    }

    private void restoreCreature(GameWorld gameWorld, TextureAtlas creaturesAtlas, SaveGameData.CreatureData creatureData) {
        if (creatureData == null || creatureData.creatureKind == null || creatureData.creatureKind.isBlank()) {
            return;
        }

        CreatureKind creatureKind = CreatureKind.valueOf(creatureData.creatureKind);
        AbstractCreatureActor creatureActor = creatureKind.createActor();
        creatureActor.configure(
            creaturesAtlas,
            creatureData.sizeW,
            creatureData.sizeH,
            creatureData.collHalfW,
            creatureData.collHalfH,
            new Vector2(creatureData.collOffsetX, creatureData.collOffsetY),
            creatureData.sortOffsetY,
            creatureData.zOrder,
            creatureData.maxHealth,
            creatureData.currentHealth
        );
        gameWorld.spawnActor(creatureActor, new Vector2(creatureData.x, creatureData.y));
    }

    private void restoreCrop(GameWorld gameWorld, TiledMap map, SaveGameData.CropData cropData) {
        if (cropData == null || cropData.tileGid <= 0 || cropData.cropKind == null || cropData.cropKind.isBlank()) {
            return;
        }

        CropKind cropKind = CropKind.valueOf(cropData.cropKind);
        CropActor cropActor = new CropActor();
        cropActor.configure(
            map,
            cropData.tileGid,
            map.getTileSets().getTile(cropData.tileGid) != null ? map.getTileSets().getTile(cropData.tileGid).getTextureRegion() : null,
            cropKind,
            cropData.growthStage,
            cropKind.toGlobalStageTileIds(cropData.tileGid, cropData.growthStage),
            cropData.sizeW,
            cropData.sizeH,
            cropData.collHalfW,
            cropData.collHalfH,
            new Vector2(cropData.collOffsetX, cropData.collOffsetY),
            cropData.sortOffsetY,
            cropData.zOrder,
            cropData.growthIntervalSeconds,
            cropData.maxHealth
        );
        cropActor.setGrowthTimer(cropData.growthTimer);
        gameWorld.spawnActor(cropActor, new Vector2(cropData.x, cropData.y));
    }

    private void restoreVisual(GameWorld gameWorld, TiledMap map, SaveGameData.VisualData visualData) {
        if (visualData == null || visualData.tileGid <= 0) {
            return;
        }

        TiledVisualActor visualActor = new TiledVisualActor();
        visualActor.configure(
            map,
            visualData.tileGid,
            map.getTileSets().getTile(visualData.tileGid) != null ? map.getTileSets().getTile(visualData.tileGid).getTextureRegion() : null,
            visualData.sizeW,
            visualData.sizeH,
            visualData.sortOffsetY,
            visualData.zOrder,
            visualData.collHalfW,
            visualData.collHalfH,
            new Vector2(visualData.collOffsetX, visualData.collOffsetY)
        );
        gameWorld.spawnActor(visualActor, new Vector2(visualData.x, visualData.y));
    }

    private void restoreItemPickup(GameWorld gameWorld, TextureAtlas itemsAtlas, SaveGameData.ItemPickupData pickupData) {
        if (pickupData == null || pickupData.itemId == null || pickupData.itemId.isBlank() || pickupData.quantity <= 0) {
            return;
        }

        var itemDefinition = GameplayItems.findById(pickupData.itemId);
        if (itemDefinition == null) {
            return;
        }

        ItemPickupActor pickupActor = new ItemPickupActor();
        pickupActor.configure(itemDefinition, pickupData.quantity, itemsAtlas);
        gameWorld.spawnActor(pickupActor, new Vector2(pickupData.x, pickupData.y));
    }

    private String buildSlotKey(int slotIndex) {
        return SLOT_KEY_PREFIX + slotIndex;
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            throw new IllegalArgumentException("Nieprawidlowy indeks slotu zapisu: " + slotIndex);
        }
    }
}