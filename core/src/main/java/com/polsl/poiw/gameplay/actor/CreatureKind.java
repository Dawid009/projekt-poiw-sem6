package com.polsl.poiw.gameplay.actor;

public enum CreatureKind {
    COW(22) {
        @Override
        public AbstractCreatureActor createActor() {
            return new CowActor();
        }
    },
    PIG(24) {
        @Override
        public AbstractCreatureActor createActor() {
            return new PigActor();
        }
    },
    SHEEP(26) {
        @Override
        public AbstractCreatureActor createActor() {
            return new SheepActor();
        }
    },
    CHICKEN(28) {
        @Override
        public AbstractCreatureActor createActor() {
            return new ChickenActor();
        }
    };

    private final int globalTileId;

    CreatureKind(int globalTileId) {
        this.globalTileId = globalTileId;
    }

    public abstract AbstractCreatureActor createActor();

    public static CreatureKind fromGlobalTileId(int globalTileId) {
        for (CreatureKind kind : values()) {
            if (kind.globalTileId == globalTileId) {
                return kind;
            }
        }
        return null;
    }
}