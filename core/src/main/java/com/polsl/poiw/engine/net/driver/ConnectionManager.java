package com.polsl.poiw.engine.net.driver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * connected players management on the server
 * maps kryonet connectionId to game playerId
 * thread-safe (ConcurrentHashMap) - accessed from networking thread and main thread
 */
public class ConnectionManager {

    // connectionId -> PlayerConnection
    private final Map<Integer, PlayerConnection> byConnectionId = new ConcurrentHashMap<>();

    // playerId -> PlayerConnection
    private final Map<Integer, PlayerConnection> byPlayerId = new ConcurrentHashMap<>();

    public void addConnection(int connectionId, int playerId, String playerName, int spawnIndex) {
        PlayerConnection conn = new PlayerConnection(connectionId, playerId, playerName, spawnIndex);
        byConnectionId.put(connectionId, conn);
        byPlayerId.put(playerId, conn);
    }

    public void removeConnection(int connectionId) {
        PlayerConnection conn = byConnectionId.remove(connectionId);
        if (conn != null) {
            byPlayerId.remove(conn.getPlayerId());
        }
    }

    public PlayerConnection getByConnectionId(int connectionId) {
        return byConnectionId.get(connectionId);
    }

    public PlayerConnection getByPlayerId(int playerId) {
        return byPlayerId.get(playerId);
    }

    public Collection<PlayerConnection> getAllConnections() {
        return Collections.unmodifiableCollection(byConnectionId.values());
    }

    public int getPlayerCount() {
        return byConnectionId.size();
    }
}
