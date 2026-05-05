# DEBUG — Temporary Debug Features

This file documents all temporary debug features added for networking diagnosis.
Remove these when the networking layer is stable.

---

## Server Debug Features (`GameServer.java`)

### 1. LOG_DEBUG level enabled
- **Where:** `GameServer.create()` — `Gdx.app.setLogLevel(LOG_DEBUG)`
- **Purpose:** Shows debug-level log messages (heartbeat, handleClientConnect details)
- **Remove:** Delete the `setLogLevel` line

### 2. Server Heartbeat
- **Where:** `GameServer.render()` — logs frame count + player count every 30 seconds
- **Fields:** `frameCount`, `heartbeatTimer`, `HEARTBEAT_INTERVAL`
- **Output:** `[GameServer] [HEARTBEAT] frame=1800 players=0`
- **Purpose:** Confirms render loop is running. If heartbeat stops, the main loop is blocked.
- **Remove:** Delete fields and the heartbeat block in render()

### 3. Frame counter on connection events
- **Where:** `onClientConnected()`, `onClientDisconnected()`, `handleClientConnect()`
- **Output:** `[GameServer] Nowe połączenie: 2 [frame=1234 t=1714826400000]`
- **Purpose:** If connect and disconnect show the SAME frame number, it means processMessages() didn't run between them (main loop was blocked). The `t=` timestamp (millis since epoch) shows wall-clock timing.
- **Remove:** Strip `[frame=... t=...]` from log strings, remove `frameCount` refs

### 4. Delta capping
- **Where:** `GameServer.render()` — caps delta at 0.25s
- **Output:** `[GameServer] Delta capped: X → 0.25` (only when triggered)
- **Purpose:** Prevents physics accumulator running hundreds of steps on large deltas (e.g. first frame after create())
- **Keep/Remove:** Consider keeping as safety measure; remove the debug log line

---

## Client Debug Features (`GameInstance.java`)

### 5. Connect phase message logging
- **Where:** `handleConnectPhaseMessage()` — logs class name of every message received during CONNECTING
- **Output:** `[GameInstance] Connect phase message received: ServerAccept`
- **Purpose:** Confirms whether ServerAccept is received, or if no messages arrive at all
- **Remove:** Delete the `Gdx.app.debug(TAG, "Connect phase message...")` line

### 6. ClientConnect sent confirmation
- **Where:** `connectToServer()` — logs after sending ClientConnect
- **Output:** `[GameInstance] ClientConnect sent, waiting for ServerAccept...`
- **Purpose:** Confirms ClientConnect was dispatched
- **Remove:** Delete the `Gdx.app.debug(TAG, "ClientConnect sent...")` line

---

## Physics Fix (`GameWorld.java`)

### 7. Physics step cap (max 8 per frame)
- **Where:** `GameWorld.update()` — `while (... && maxSteps-- > 0)` + accumulator drain
- **Purpose:** Prevents Box2D running 600+ steps on a single large delta. Without this, if any frame takes too long, the next frame's delta is large → more steps → larger delta → spiral.
- **Keep:** This is a safety fix, not debug. **Do NOT remove.**

---

## How to Diagnose

### Server not responding to client:
1. Start server: `./gradlew server:run`
2. Wait for heartbeat: `[HEARTBEAT] frame=1800 players=0`
3. Connect client
4. Check server output for:
   - `Nowe połączenie: X [frame=N t=T1]` — connection detected
   - `handleClientConnect from conn=X ...` — message processed
   - `Gracz zaakceptowany: ... [frame=N]` — accept sent
   - If `Nowe połączenie` and `Rozłączenie` have the **same frame number** → main loop was blocked

### Client timeout:
1. Start client, open console/log
2. Click connect
3. Look for:
   - `ClientConnect sent, waiting for ServerAccept...`
   - `Connect phase message received: ServerAccept` — if this appears, the fix worked
   - If timeout fires without any "Connect phase message" log → server never responded

### Key: frame number comparison
If server shows:
```
Nowe połączenie: 2 [frame=5000 t=1714826400000]
Rozłączenie: 2     [frame=5000 t=1714826400000]
```
→ Both events processed in the same frame = server was stuck for 10s

If server shows:
```
Nowe połączenie: 2 [frame=5000 t=1714826400000]
Rozłączenie: 2     [frame=5600 t=1714826410000]
```
→ 600 frames between events = server was running fine, but ServerAccept was lost in transit

---

## Removal Checklist

When networking is stable, remove in this order:
1. `Gdx.app.setLogLevel(LOG_DEBUG)` in `GameServer.create()`
2. `frameCount`, `heartbeatTimer`, `HEARTBEAT_INTERVAL` fields in `GameServer`
3. Heartbeat block in `GameServer.render()` (keep delta cap)
4. `[frame=... t=...]` suffixes in connection log messages
5. `Gdx.app.debug` call in `handleClientConnect()`
6. `Gdx.app.debug` calls in `GameInstance.handleConnectPhaseMessage()` and `connectToServer()`
7. This file (`DEBUG.md`)
