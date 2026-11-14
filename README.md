# Spectator 1.6.4

Install the jar on both the Minecraft 1.6.4 Forge client and server. With cheats or operator permission, use `/gamemode spectator` or `/gamemode 3` to enter spectator, and `/gamemode` with another mode to leave it. Press F3+N to toggle between spectator and the previous game mode. The standard `/gamemode` player argument also works.

While spectating, move in flight and scroll to change flying speed. Double-tap forward to sprint with a flight boost. Left-click an entity to follow its view; use F5 to change perspective and press the sneak key to return to free flight. Press a number key to open the player selection bar, then press 1–9 to teleport to a listed player. The bar shows the heads of up to nine players currently loaded in your dimension. The regular hotbar and status bars are hidden. Right-click vanilla inventory blocks to view a read-only snapshot without opening the actual container. Chests, furnaces, dispensers, droppers, hoppers, brewing stands and beacons use their usual layouts. Snapshots are not live, show at most 54 slots, and do not support custom mod inventories. Inventory and container slots are read-only while spectating. Vertical flight also follows the selected speed. Spectators appear as translucent heads, including in the inventory preview.

The mod bypasses the server's collision checks for spectator movement, prevents spectators from triggering pressure plates and tripwire, and suppresses player-generated vanilla particles and the client block overlay. The implementation uses a Forge core plugin and may conflict with other mods that modify the same methods. Camera behavior and rendering have not yet been verified in an interactive Minecraft client. Later Minecraft versions' entity-specific spectator shaders are not available in 1.6.4.

Install [mise](https://mise.jdx.dev/), then run `mise install`, `mise run setup`, and `mise run build` in the project directory. The resulting jar is in `build/libs`. The project pins Java 8 and Gradle 3 and stores the Gradle cache in the project. Recent IntelliJ versions do not support importing Gradle this old; use the terminal build.


