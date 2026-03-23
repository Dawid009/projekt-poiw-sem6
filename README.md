# ProjektPOIW

Prosta gra 2D z scieciowym trybem gry.

## Platformy

- `core`: Główny moduł zawierający logikę aplikacji współdzieloną przez wszystkie platformy.
- `lwjgl3`: Główna platforma desktopowa wykorzystująca LWJGL3.
- `server`: Oddzielna aplikacja serwera

## Gradle

Projekt korzysta z [Gradle](https://gradle.org/) do zarządzania zależnościami.  
Wrapper Gradle jest już dołączony, więc można uruchamiać zadania za pomocą poleceń `gradlew.bat` lub `./gradlew`.

### Przydatne zadania i flagi Gradle:

- `--continue`: błędy nie przerywają wykonywania zadań.
- `--daemon`: używa demona Gradle do uruchamiania zadań.
- `--offline`: korzysta z zapisanych lokalnie zależności.
- `--refresh-dependencies`: wymusza ponowną weryfikację wszystkich zależności.
- `build`: buduje źródła i archiwa wszystkich projektów.
- `cleanEclipse`: usuwa dane projektu Eclipse.
- `cleanIdea`: usuwa dane projektu IntelliJ.
- `clean`: usuwa foldery `build` z wynikami kompilacji.
- `eclipse`: generuje pliki projektu dla Eclipse.
- `idea`: generuje pliki projektu dla IntelliJ.
- `lwjgl3:jar`: tworzy wykonywalny plik JAR aplikacji.
- `lwjgl3:run`: uruchamia aplikację.
- `server:run`: uruchamia aplikację serwera.
- `test`: uruchamia testy jednostkowe (jeśli istnieją).

Większość zadań, które nie są przypisane do konkretnego projektu, można uruchamiać z prefiksem `name:` — gdzie `name` to identyfikator projektu.

Na przykład: `core:clean` usuwa folder `build` tylko z projektu `core`.
