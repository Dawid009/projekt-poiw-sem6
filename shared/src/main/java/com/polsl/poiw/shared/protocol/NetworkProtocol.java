package com.polsl.poiw.shared.protocol;

import java.util.Map;

/**
 * Protoków sieciowy:
 * definicje wiadomośći wymienianych między klientem a serwerem
 */
public final class NetworkProtocol {

    // wersja protokołu - musi być taka sama między klientem i serwerem
    public static final int PROTOCOL_VERSION = 8;

    // maksymalna długość wiadomości czatu
    public static final int MAX_CHAT_MESSAGE_LENGTH = 60;

    // domyślny port TCP i UDP
    public static final int DEFAULT_TCP_PORT = 54555;
    public static final int DEFAULT_UDP_PORT = 54777;

    private NetworkProtocol() {}

   /**
    * CONNECTION
    */

    // klient -> serwer: connection request
    public static class ClientConnect {
        public String playerName;
        public int protocolVersion = PROTOCOL_VERSION;
    }

    // serwer -> klient: connection accepted
    public static class ServerAccept {
        public int assignedPlayerId;
        public String mapId;
        public float serverTime;
    }

    // server -> klient: connection rejected
    public static class ServerReject {
        public String reason;
    }

    // klient -> serwer: disconnect
    public static class ClientDisconnect {
        public int playerId;
    }

    /**
     * REPLICATION
     */

    // serwer -> klienci: spawn aktora
    public static class ActorSpawn {
        public int actorId;
        public String actorClass;
        public float x, y;
        public int ownerId;
        public Map<String, Object> initialProperties;
    }

    // serwer -> klienci: destroy actor
    public static class ActorDestroy {
        public int actorId;
    }

    // aktualizacja replikacji pojedynczego aktora
    public static class ReplicationUpdate {
        public int actorId;
        public String componentClass;
        public Map<String, Object> properties;
        public int sequenceNumber;
    }

    // server -> clients: batch update replikacji
    public static class BatchReplicationUpdate {
        public ReplicationUpdate[] updates;
        public float serverTime;
        public int serverTick;
    }

    /**
     * INPUT
     */

    // klient -> serwer: aktualizacja inputu gracza (np. kierunek ruchu)
    public static class ClientInputUpdate {
        public int playerId;
        public int sequenceNumber;
        public float dirX, dirY;
        public boolean sprinting;
        public float timestamp;
    }

    public enum InventoryActionType {
        USE,
        DROP
    }

    public static class ClientInventoryAction {
        public int playerId;
        public int slotIndex = -1;
        public String itemId;
        public InventoryActionType action;
        public boolean wholeStack;
    }

    public static class ClientToolSelection {
        public int playerId;
        public int toolOrdinal;
    }

    public static class ClientAssignedItemUpdate {
        public int playerId;
        public String itemId;
    }

    public enum ChestInventoryTransferDirection {
        PLAYER_TO_CHEST,
        CHEST_TO_PLAYER
    }

    public static class ClientChestInventoryTransfer {
        public int playerId;
        public int chestActorId;
        public int slotIndex = -1;
        public String itemId;
        public boolean wholeStack;
        public ChestInventoryTransferDirection direction;
    }

    // serwer -> klient: korekta pozycji (np. po wykryciu desynchronizacji)
    public static class ServerPositionCorrection {
        public int actorId;
        public float x, y;
        public float velX, velY;
        public int lastProcessedInput;
        public float serverTime;
    }

    /**
     * RPC
     */

    // zdalne wywolanie procedury
    public static class RPCCall {
        public int sourceActorId;
        public int targetActorId;
        public String methodName;
        public Object[] args;
        public RPCTarget target;
    }

    // cel rpc
    public enum RPCTarget {
        SERVER,
        CLIENT,
        MULTICAST
    }

    /**
     * GAME STATE
     */

    // stan pojedynczego gracza
    public static class PlayerStateUpdate {
        public int playerId;
        public String playerName;
        public int score;
        public float health;
        public float maxHealth;
    }

    // server -> clients: aktualizacja stanu gry
    public static class GameStateUpdate {
        public float gameTime;
        public int playerCount;
        public PlayerStateUpdate[] players;
    }

    /**
     * TRAVEL
     */

    // server -> clients: zmiana mapy
    public static class ServerTravel {
        public String levelId;
        public int travelId;
        public boolean preserveControllers;
    }

    // client -> server: potwierdzenie załadowania nowego świata po travel
    public static class ClientTravelAck {
        public int travelId;
    }

    /**
     * MOVEMENT
     */

    // pojedynczy snapshot ruchu aktora — wysyłany UDP
    public static class MovementSnapshot {
        public int actorId;
        public float x, y;
        public float velX, velY;
        public int sequenceNumber;
    }

    // server -> clients: batch snapshotów ruchu — UDP latest-wins
    public static class BatchMovementSnapshot {
        public MovementSnapshot[] snapshots;
        public float serverTime;
        public int serverTick;
    }

    /**
     * HEALTHCHECK
     */

    public static class Ping {
        public long clientTimestamp;
    }

    public static class Pong {
        public long clientTimestamp;
        public long serverTimestamp;
    }

    /**
     * CHAT
     */

    public enum ChatMessageType {
        PLAYER,  // wiadomość od gracza
        SYSTEM   // wiadomość systemowa (join/leave)
    }

    // klient -> serwer (PLAYER) / serwer -> klienci (broadcast)
    public static class ChatMessage {
        public int playerId;
        public String playerName;
        public String message;
        public long timestamp;
        public ChatMessageType type;
    }

    /**
     * PLAYER LIST
     */

    // serwer -> klienci: aktualna lista graczy
    public static class PlayerListUpdate {
        public String[] playerNames;
    }
}
