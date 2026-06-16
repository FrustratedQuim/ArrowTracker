# ArrowTracker

- Small tracker plugin for Paper 1.21.4
- What u get:
  - `/tracker <x> <z>` - start personal arrow
  - `/tracker clear` - stop it
  - Same coords again = toggle off
- How its done:
  - Packet text display
  - Mounted on player
  - Points by player `x/z` vs target `x/z`
  - 1 active tracker per player
- Req:
  - Paper 1.21.4
  - PacketEvents
  - resourcepack:
    - [ArrowTracker.zip](https://github.com/user-attachments/files/29009154/ArrowTracker.zip)
- API:
  - public contract = `com.ratger.arrowtracker.api.*`
  - published artifact is thin api jar, not plugin runtime jar
  - get it from `Bukkit.getServicesManager()`
  - use `depend: [ArrowTracker]` if its required
  - use `softdepend: [ArrowTracker]` if integration is optional

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.FrustratedQuim:ArrowTracker:1.0.0")
}
```

```kotlin
val api = Bukkit.getServicesManager().load(ArrowTrackerApi::class.java) ?: return

api.showTracker(player, TrackerTarget(128.5, -64.0))
api.hideTracker(player)

when (api.toggleTracker(player, TrackerTarget(128.5, -64.0))) {
    TrackerState.ACTIVE -> player.sendMessage("tracker on")
    TrackerState.INACTIVE -> player.sendMessage("tracker off")
}
```

- API methods:
  - `showTracker(player, target)` = enable/update, returns `false` only if same target already active
  - `hideTracker(player)` = disable, returns `false` if tracker already off
  - `toggleTracker(player, target)` = returns final state
  - `isTracking(playerId)` = current active state
  - `getTarget(playerId)` = current target or `null`
  - `getActiveTrackers()` = snapshot of active trackers
- Note:
  - call ArrowTracker API only from main server thread
  - default JitPack coordinate = `com.github.FrustratedQuim:ArrowTracker:<Tag>`
  - `publicationGroupId` and `publicationArtifactId` can be overridden via Gradle properties before publish
