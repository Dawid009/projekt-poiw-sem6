package com.polsl.poiw.gameplay.actor;

public enum CreatureKind {
    COW("cow") {
        @Override
        public AbstractCreatureActor createActor() {
            return new CowActor();
        }
    },
    PIG("pig") {
        @Override
        public AbstractCreatureActor createActor() {
            return new PigActor();
        }
    },
    SHEEP("sheep") {
        @Override
        public AbstractCreatureActor createActor() {
            return new SheepActor();
        }
    },
    CHICKEN("chicken") {
        @Override
        public AbstractCreatureActor createActor() {
            return new ChickenActor();
        }
    },
    SKELETON("skeleton") {
        @Override
        public AbstractCreatureActor createActor() {
            return new SkeletonActor();
        }
    },
    SLIME("slime") {
        @Override
        public AbstractCreatureActor createActor() {
            return new SlimeActor();
        }
    };

    private final String metadataValue;

    CreatureKind(String metadataValue) {
        this.metadataValue = metadataValue;
    }

    public abstract AbstractCreatureActor createActor();

    public static CreatureKind fromMetadata(String metadataValue) {
        if (metadataValue == null || metadataValue.isBlank()) {
            return null;
        }

        for (CreatureKind kind : values()) {
            if (kind.metadataValue.equalsIgnoreCase(metadataValue)) {
                return kind;
            }
        }
        return null;
    }
}