# Spectator 1.6.4

Install the jar on both the Minecraft 1.6.4 Forge client and server. With cheats or operator permission, use `/spectator on` and `/spectator off`. The player returns to their previous game mode on exit.

While spectating, move in flight and scroll to change flying speed. Double-tap forward to sprint with a flight boost. Left click to open the player selection bar; press 1–7 to teleport to a listed player, and 8 or 9 to change pages. The list contains players currently loaded in your dimension. The regular hotbar and status bars are hidden. Right-click vanilla inventory blocks to view a read-only snapshot without opening the actual container. Snapshots are not live, show at most 54 slots, and do not support custom mod inventories. Inventory and container slots are read-only while spectating. Vertical flight also follows the selected speed. Other spectators appear as translucent heads.

The mod bypasses the server's collision checks for spectator movement, prevents spectators from triggering pressure plates and tripwire, and suppresses the client block overlay. The implementation uses a Forge core plugin and may conflict with other mods that modify the same methods. Player menu behavior and rendering have not yet been verified in an interactive Minecraft client.

Install [mise](https://mise.jdx.dev/), then run `mise install`, `mise run setup`, and `mise run build` in the project directory. The resulting jar is in `build/libs`. The project pins Java 8 and Gradle 3 and stores the Gradle cache in the project. Recent IntelliJ versions do not support importing Gradle this old; use the terminal build.


