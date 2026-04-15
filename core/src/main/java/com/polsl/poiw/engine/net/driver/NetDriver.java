package com.polsl.poiw.engine.net.driver;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.polsl.poiw.shared.protocol.NetworkSerializer;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NetDriver - wrapper around Kryonet providing thread-safety
 * kryonet listener runs on a networking thread
 * all messages are enqueued to a ConcurrentLinkedQueue and processed on the main thread via processMessages()
 * DO NOT modify GameWorld/ECS from the networking thread
 */
public class NetDriver {

    private static final String TAG = "NetDriver";

    private final boolean isServer;
    private Server kryoServer;
    private Client kryoClient;
    private final ConnectionManager connectionManager;

    // incoming messages queue (networking thread -> main thread)
    private final ConcurrentLinkedQueue<ReceivedMessage> incomingMessages = new ConcurrentLinkedQueue<>();

    // connection events queue (networking thread -> main thread)
    private final ConcurrentLinkedQueue<ConnectionEvent> connectionEvents = new ConcurrentLinkedQueue<>();

    /**
     * all called on main thread
     * ------------------
     */
    // message handler (connectionId, message)
    private BiConsumer<Integer, Object> messageHandler;

    // new connection handler (connectionId)
    private Consumer<Integer> connectHandler;

    // disconnection handler (connectionId)
    private Consumer<Integer> disconnectHandler;

    /**
     * -------------------
     */

    public NetDriver(boolean isServer) {
        this.isServer = isServer;
        this.connectionManager = new ConnectionManager();
    }

    /**
     * SERVER API
     */

    // starts the server on specified ports
    public void startServer(int tcpPort, int udpPort) throws IOException {
        if (!isServer) throw new IllegalStateException("NetDriver nie jest w trybie serwera");

        kryoServer = new Server(16384, 8192);
        NetworkSerializer.registerAll(kryoServer.getKryo());

        kryoServer.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                incomingMessages.add(new ReceivedMessage(connection.getID(), object));
            }

            @Override
            public void connected(Connection connection) {
                connectionEvents.add(new ConnectionEvent(connection.getID(), true));
            }

            @Override
            public void disconnected(Connection connection) {
                connectionEvents.add(new ConnectionEvent(connection.getID(), false));
            }
        });

        kryoServer.bind(tcpPort, udpPort);
        kryoServer.start();
        Gdx.app.log(TAG, "Serwer uruchomiony na TCP:" + tcpPort + " UDP:" + udpPort);
    }

    public void sendToClient(int connectionId, Object msg, boolean reliable) {
        if (kryoServer == null) return;
        if (reliable) {
            kryoServer.sendToTCP(connectionId, msg);
        } else {
            kryoServer.sendToUDP(connectionId, msg);
        }
    }

    public void sendToAllClients(Object msg, boolean reliable) {
        if (kryoServer == null) return;
        if (reliable) {
            kryoServer.sendToAllTCP(msg);
        } else {
            kryoServer.sendToAllUDP(msg);
        }
    }

    public void sendToAllExcept(int excludeConnectionId, Object msg, boolean reliable) {
        if (kryoServer == null) return;
        if (reliable) {
            kryoServer.sendToAllExceptTCP(excludeConnectionId, msg);
        } else {
            kryoServer.sendToAllExceptUDP(excludeConnectionId, msg);
        }
    }

    /**
     * CLIENT API
     */

    // connects a client to the server
    // returns true if success
    public boolean connectToServer(String host, int tcpPort, int udpPort) {
        if (isServer) throw new IllegalStateException("NetDriver nie jest w trybie klienta");

        kryoClient = new Client(16384, 8192);
        NetworkSerializer.registerAll(kryoClient.getKryo());

        kryoClient.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                incomingMessages.add(new ReceivedMessage(connection.getID(), object));
            }

            @Override
            public void connected(Connection connection) {
                connectionEvents.add(new ConnectionEvent(connection.getID(), true));
            }

            @Override
            public void disconnected(Connection connection) {
                connectionEvents.add(new ConnectionEvent(connection.getID(), false));
            }
        });

        kryoClient.start();
        try {
            kryoClient.connect(5000, host, tcpPort, udpPort);
            Gdx.app.log(TAG, "Połączono z serwerem: " + host + ":" + tcpPort);
            return true;
        } catch (IOException e) {
            Gdx.app.error(TAG, "Nie można połączyć z serwerem: " + host + ":" + tcpPort, e);
            return false;
        }
    }

    public void sendToServer(Object msg, boolean reliable) {
        if (kryoClient == null || !kryoClient.isConnected()) return;
        if (reliable) {
            kryoClient.sendTCP(msg);
        } else {
            kryoClient.sendUDP(msg);
        }
    }

    /**
     * common api
     */

    // parse and handle all incoming messages and connection events (to be called on the main thread)
    // must be called every frame from the main game loop
    public void processMessages() {
        // parse connection events
        ConnectionEvent event;
        while ((event = connectionEvents.poll()) != null) {
            if (event.connected) {
                if (connectHandler != null) connectHandler.accept(event.connectionId);
            } else {
                if (disconnectHandler != null) disconnectHandler.accept(event.connectionId);
            }
        }

        // paerse incoming messages
        ReceivedMessage msg;
        while ((msg = incomingMessages.poll()) != null) {
            if (messageHandler != null) {
                messageHandler.accept(msg.connectionId, msg.message);
            }
        }
    }

    public void disconnect() {
        if (kryoClient != null) {
            kryoClient.close();
        }
    }

    // clears all connections and frees resources
    public void dispose() {
        if (kryoServer != null) {
            kryoServer.stop();
            kryoServer.close();
        }
        if (kryoClient != null) {
            kryoClient.stop();
            kryoClient.close();
        }
    }

    /**
     * handlers
     */

    public void setMessageHandler(BiConsumer<Integer, Object> handler) {
        this.messageHandler = handler;
    }

    public void setConnectHandler(Consumer<Integer> handler) {
        this.connectHandler = handler;
    }

    public void setDisconnectHandler(Consumer<Integer> handler) {
        this.disconnectHandler = handler;
    }

    /**
     * getters
     */

    public boolean isServer() { return isServer; }
    public ConnectionManager getConnectionManager() { return connectionManager; }
    public boolean isConnected() {
        if (isServer) return kryoServer != null;
        return kryoClient != null && kryoClient.isConnected();
    }

    /**
     * internal
     */

    private record ReceivedMessage(int connectionId, Object message) {}
    private record ConnectionEvent(int connectionId, boolean connected) {}
}
