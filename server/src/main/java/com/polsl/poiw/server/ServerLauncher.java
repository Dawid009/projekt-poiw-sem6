package com.polsl.poiw.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

/** Uruchamia dedykowany serwer gry w trybie headless. */
public class ServerLauncher {
    /**
     * Punkt wejścia aplikacji serwerowej.
     *
     * @param args opcjonalne argumenty CLI, np. porty i limit graczy
     */
    public static void main(String[] args) {
        int tcpPort = NetworkProtocol.DEFAULT_TCP_PORT;
        int udpPort = NetworkProtocol.DEFAULT_UDP_PORT;
        int maxPlayers = 4;

        // parse cli args
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--tcp-port" -> { if (i + 1 < args.length) tcpPort = Integer.parseInt(args[++i]); }
                case "--udp-port" -> { if (i + 1 < args.length) udpPort = Integer.parseInt(args[++i]); }
                case "--max-players" -> { if (i + 1 < args.length) maxPlayers = Integer.parseInt(args[++i]); }
            }
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60; // 60 Hz server tick rate

        new HeadlessApplication(new GameServer(tcpPort, udpPort, maxPlayers), config);
    }
}
