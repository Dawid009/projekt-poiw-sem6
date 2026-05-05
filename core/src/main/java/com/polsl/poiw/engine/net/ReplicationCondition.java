package com.polsl.poiw.engine.net;

// replication conditions - when and to who the field is replicated

public enum ReplicationCondition {
    // to all clients, every update
    ALWAYS,
    // to clients only when the actor is spawned
    INITIAL_ONLY,
    // only to owning client
    OWNER_ONLY,
    // to everyone except owning client
    SKIP_OWNER,
    // checked programmatically
    CUSTOM
}
