package com.polsl.poiw.shared.protocol;

import com.esotericsoftware.kryo.Kryo;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * rejestracja klas dla serializera
 * musi byc wywolane identycznie na serwerze i kliencie
 */

public final class NetworkSerializer {

    private NetworkSerializer() {}

    /**
     * registers all protocol classes and helper collections in kryo
     * the order must be identical on server and client
     */
    public static void registerAll(Kryo kryo) {
        //collections and tables
        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
        kryo.register(float[].class);
        kryo.register(int[].class);
        kryo.register(String[].class);
        kryo.register(Object[].class);

        // connection
        kryo.register(NetworkProtocol.ClientConnect.class);
        kryo.register(NetworkProtocol.ServerAccept.class);
        kryo.register(NetworkProtocol.ServerReject.class);
        kryo.register(NetworkProtocol.ClientDisconnect.class);

        // replication
        kryo.register(NetworkProtocol.ActorSpawn.class);
        kryo.register(NetworkProtocol.ActorDestroy.class);
        kryo.register(NetworkProtocol.ReplicationUpdate.class);
        kryo.register(NetworkProtocol.ReplicationUpdate[].class);
        kryo.register(NetworkProtocol.BatchReplicationUpdate.class);

        // input
        kryo.register(NetworkProtocol.ClientInputUpdate.class);
        kryo.register(NetworkProtocol.ClientAttackRequest.class);
        kryo.register(NetworkProtocol.ClientRespawnRequest.class);
        kryo.register(NetworkProtocol.InventoryActionType.class);
        kryo.register(NetworkProtocol.ClientInventoryAction.class);
        kryo.register(NetworkProtocol.ClientToolSelection.class);
        kryo.register(NetworkProtocol.ClientAssignedItemUpdate.class);
        kryo.register(NetworkProtocol.ChestInventoryTransferDirection.class);
        kryo.register(NetworkProtocol.ClientChestInventoryTransfer.class);
        kryo.register(NetworkProtocol.TradeTransferDirection.class);
        kryo.register(NetworkProtocol.ClientTradeTransfer.class);
        kryo.register(NetworkProtocol.ClientTradePurchase.class);
        kryo.register(NetworkProtocol.ClientTradeSell.class);
        kryo.register(NetworkProtocol.ServerPositionCorrection.class);

        // RPC
        kryo.register(NetworkProtocol.RPCCall.class);
        kryo.register(NetworkProtocol.RPCTarget.class);

        // game state
        kryo.register(NetworkProtocol.PlayerStateUpdate.class);
        kryo.register(NetworkProtocol.PlayerStateUpdate[].class);
        kryo.register(NetworkProtocol.GameStateUpdate.class);

        // travel
        kryo.register(NetworkProtocol.ServerTravel.class);
        kryo.register(NetworkProtocol.ClientTravelAck.class);

        // movement
        kryo.register(NetworkProtocol.MovementSnapshot.class);
        kryo.register(NetworkProtocol.MovementSnapshot[].class);
        kryo.register(NetworkProtocol.BatchMovementSnapshot.class);

        // healthcheck
        kryo.register(NetworkProtocol.Ping.class);
        kryo.register(NetworkProtocol.Pong.class);

        // chat
        kryo.register(NetworkProtocol.ChatMessageType.class);
        kryo.register(NetworkProtocol.ChatMessage.class);

        // player list
        kryo.register(NetworkProtocol.PlayerListUpdate.class);
    }
}
