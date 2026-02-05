# AdvancedHunt — Comprehensive Code Audit Report

**Generated:** February 5, 2026  
**Scope:** Full codebase — plugin core, platform API, all version modules, migration layer  
**Categories:** Performance, Memory, Concurrency, Consistency, Best Practice, Bug, Security

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Critical Issues](#critical-issues)
3. [High-Severity Issues](#high-severity-issues)
4. [Medium-Severity Issues](#medium-severity-issues)
5. [Low-Severity Issues](#low-severity-issues)
6. [Cross-Version Consistency Matrix](#cross-version-consistency-matrix)
7. [Aggregate Statistics](#aggregate-statistics)

---

## Executive Summary

This audit identified **~130 issues** across the entire AdvancedHunt codebase. The most impactful problem areas are:

1. **Placeholder PlayerData returns** — `PlayerManager.getPlayerData()` returns empty placeholder data when the real data hasn't loaded yet, causing downstream bugs in treasure collection deduplication, completion detection, and progress tracking.
2. **Concurrency & thread-safety** — Multiple managers use non-volatile fields, non-atomic operations, and fire-and-forget async chains that silently swallow failures.
3. **NBT double-`getState()` bug** — At least two locations call `block.getState()` twice, causing NBT modifications to be silently lost.
4. **SQL data integrity** — `REPLACE INTO` silently drops columns, orphan rows accumulate on collection deletion, and table name mismatches exist.
5. **Resource leaks** — HTTP connections, SQL statements, and scheduled tasks are not always properly closed or cancelled.

### Priority Action Items

| Priority | Issue | Impact |
|----------|-------|--------|
| P0 | Fix `PlayerData` placeholder pattern (#MGR-1, #MGR-18, #INT-2, #INT-3) | Players can bypass find-deduplication and completion never triggers |
| P0 | Fix NBT double-`getState()` (#MGR-10, #DEL-1) | Head textures/NBT silently vanish after block restore |
| P0 | Fix `REPLACE INTO` column drop (#SQL-3) | ACT schedule data silently wiped on every collection save |
| P1 | Fix async Bukkit API calls (#CMD-1, #CMD-2, #MGR-3) | Undefined behavior / potential crashes |
| P1 | Fix `TreasureInteractionHandler` race conditions (#INT-1, #INT-2) | Duplicate treasure claims in single-player-find mode |
| P1 | Fix firework UUID memory leak (#LIS-8, #UTIL-6) | Unbounded memory growth over time |
| P1 | Fix hex color breakage in rewards (#UTIL-1) | All hex colors in reward messages are broken |

---

## Critical Issues

### CRIT-1: XXE Vulnerability in BukkitSource Update Checker
- **File:** `plugin/.../util/updater/source/BukkitSource.java` ~L37, ~L82
- **Category:** Security
- **Description:** `DocumentBuilderFactory.newInstance()` is used without disabling external entities or DTDs. A malicious RSS feed could exploit XML External Entity injection to read local files or perform SSRF.
- **Fix:** Set `factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)` and `factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)`.

### CRIT-2: Non-Thread-Safe `cachedCollections` in CollectionManager
- **File:** `plugin/.../managers/CollectionManager.java` ~L56
- **Category:** Concurrency
- **Description:** `cachedCollections` is a plain `ArrayList` assigned from an async callback and read from any thread. No `volatile`, no synchronization, no CopyOnWrite. Other threads may see a stale or partially-constructed list.
- **Fix:** Use `CopyOnWriteArrayList`, or declare as `volatile` and swap atomically, or wrap in `synchronized`.

### CRIT-3: Non-Atomic Clear+Reload in TreasureManager
- **File:** `plugin/.../managers/TreasureManager.java` ~L72
- **Category:** Concurrency
- **Description:** `loadTreasures()` clears all four cache maps then repopulates them asynchronously. During the window between `clear()` and repopulation completing, all spatial lookups return empty — players can't interact with any treasures.
- **Fix:** Build new maps, then swap references atomically.

### CRIT-4: Non-Thread-Safe Singleton — TreasureInteractionHandler
- **File:** `plugin/.../managers/TreasureInteractionHandler.java` ~L37
- **Category:** Concurrency
- **Description:** `getInstance()` is a lazy singleton with no `volatile` and no synchronization. Two threads can create two instances; the `reset()` method also lacks visibility guarantees.
- **Fix:** Make `instance` `volatile` + double-checked locking, or use eager init / enum singleton.

### CRIT-5: BungeeCord `Text` Class Missing on 1.8–1.15 Servers
- **File:** `modules/spigot-1_8/.../Spigot18PlatformAdapter.java` ~L195, `modules/spigot-1_9plus/.../Spigot19PlatformAdapter.java` ~L191
- **Category:** Bug (Minecraft-specific)
- **Description:** Both adapters use `net.md_5.bungee.api.chat.hover.content.Text`, which didn't exist in BungeeCord chat API until ~1.16. On genuine 1.8–1.12 server JARs, `sendClickableCopyText()` will throw `NoClassDefFoundError`.
- **Fix:** Use the legacy `HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(text).create())` constructor for pre-1.16 adapters.

### CRIT-6: SQL Table Name Mismatch
- **File:** `plugin/.../data/SqlRepository.java` migration 8 vs ~L242
- **Category:** Bug
- **Description:** Migration 8 creates table `ah_place_items_groups`, but all CRUD methods and `createTables()` reference `ah_place_preset_groups`. The migration creates a dead/orphaned table.
- **Fix:** Change migration 8's table name to `ah_place_preset_groups`.

---

## High-Severity Issues

### CMD-1: Async Thread `sendMessage()` in `createCollection`/`renameCollection`
- **File:** `plugin/.../commands/AdvancedHuntCommand.java` ~L373-380
- **Category:** Concurrency / Bug
- **Description:** `.thenAccept(success -> sender.sendMessage(...))` runs on the completing thread (async I/O). `sender.sendMessage()` is a Bukkit API call that should only be invoked from the main thread. Other handlers (e.g., `deleteCollection`) correctly wrap this in `Bukkit.getScheduler().runTask()`.
- **Fix:** Wrap `sendMessage` calls inside `Bukkit.getScheduler().runTask(plugin, ...)`.

### CMD-2: Async Thread Messages in All Reset Commands
- **File:** `plugin/.../commands/AdvancedHuntCommand.java` ~L408-448
- **Category:** Concurrency / Bug
- **Description:** All four reset handlers (`resetAll`, `resetCollection`, `resetPlayer`, `resetPlayerCollection`) use `.thenAccept()` without scheduling back to the main thread.
- **Fix:** Wrap each `.thenAccept` body in `Bukkit.getScheduler().runTask()`.

### CMD-3: `Bukkit.getOfflinePlayers()` on Every Tab Complete
- **File:** `plugin/.../commands/AdvancedHuntCommand.java` ~L96-100
- **Category:** Performance
- **Description:** `Bukkit.getOfflinePlayers()` returns ALL players who ever joined. On mature servers this is tens of thousands of entries, causing heavy I/O on every tab-completion.
- **Fix:** Cache with a short TTL, or only suggest online players via `Bukkit.getOnlinePlayers()`.

### SQL-1: Race Condition — Lock Released Between Load and Save (YamlRepository)
- **File:** `plugin/.../data/YamlRepository.java` ~L457-477
- **Category:** Concurrency
- **Description:** Write lock is acquired to load existing data, then released. `saveConfigAtomicWithLock()` reacquires the lock. Between unlock and re-lock, another thread can write and those changes are silently overwritten.
- **Fix:** Hold the write lock for the entire load-modify-save sequence.

### SQL-2: Same Race in `savePlayerDataBatch`
- **File:** `plugin/.../data/YamlRepository.java` ~L495-521
- **Category:** Concurrency
- **Description:** Same lock-release-relock pattern, applied per player in a batch loop.
- **Fix:** Hold write lock for entire read-modify-write cycle per file.

### SQL-3: `REPLACE INTO` Drops Unlisted Columns
- **File:** `plugin/.../data/SqlRepository.java` ~L564-572
- **Category:** Bug
- **Description:** `REPLACE INTO ah_collections` only lists 8 columns. The table has additional columns (`reset_cron`, `active_start`, `active_end`). `REPLACE` does DELETE+INSERT, silently setting unlisted columns to NULL on every save.
- **Fix:** Include all columns, or switch to `INSERT ... ON CONFLICT DO UPDATE`.

### SQL-4: `deleteCollection` Orphans `ah_player_found` Rows
- **File:** `plugin/.../data/SqlRepository.java` ~L670-693
- **Category:** Bug / Memory
- **Description:** Deletes from `ah_collections` and `ah_treasures` but never cleans up `ah_player_found` entries. Orphan rows accumulate forever, bloating the database.
- **Fix:** Add `DELETE FROM ah_player_found WHERE treasure_id IN (SELECT id FROM ah_treasures WHERE collection_id = ?)` before deleting from `ah_treasures`.

### SQL-5: MySQL Uses `ForkJoinPool.commonPool()` for Blocking I/O
- **File:** `plugin/.../data/SqlRepository.java` ~L137-140
- **Category:** Performance
- **Description:** When `useSqlite` is false, `asyncExecutor` is null, so all `supplyAsync`/`runAsync` calls use the common `ForkJoinPool`. Blocking JDBC calls can starve other async tasks JVM-wide.
- **Fix:** Create a dedicated executor for MySQL I/O.

### SQL-6: `shutdown()` Doesn't Await Running SQLite Tasks
- **File:** `plugin/.../data/SqlRepository.java` ~L263-270
- **Category:** Concurrency
- **Description:** `sqliteExecutor.shutdownNow()` + immediate `dataSource.close()` can cause in-flight SQL operations to get `SQLException`.
- **Fix:** `shutdown()` → `awaitTermination(5s)` → `shutdownNow()` → close data source.

### SQL-7: Statement Leak in `createPerformanceIndexes`
- **File:** `plugin/.../data/SqlRepository.java` ~L295-307
- **Category:** Memory
- **Description:** `conn.createStatement().execute(sql)` creates Statements without closing them.
- **Fix:** Wrap in try-with-resources.

### MGR-1: `getPlayerData()` Returns Empty Placeholder Before Data Loads
- **File:** `plugin/.../managers/PlayerManager.java` ~L51
- **Category:** Bug (Root Cause of Multiple Downstream Issues)
- **Description:** Returns a PlayerData with an empty found-set while real data loads async. Any caller checking `hasFound()` or counting progress will get wrong results. This is the root cause of issues INT-2, INT-3, and MGR-3.
- **Fix:** Return `CompletableFuture<PlayerData>`, or gate callers behind a loaded flag, or use a blocking `getPlayerDataWithTimeout()` only from async contexts.

### MGR-2: Caffeine Cache Rejects Null from `loadPlayerData`
- **File:** `plugin/.../managers/PlayerManager.java` ~L107
- **Category:** Bug
- **Description:** `repository.loadPlayerData(...).thenAccept(data -> playerDataCache.put(..., data))` — if `data` is null (new player), Caffeine throws NPE.
- **Fix:** `playerDataCache.put(..., data != null ? data : new PlayerData(uuid))`.

### MGR-3: `checkCompletion` Calls Bukkit API from Async Thread
- **File:** `plugin/.../managers/CollectionManager.java` ~L108
- **Category:** Concurrency / Bug
- **Description:** `playTreasureFound(player)` is called from `CompletableFuture`'s ForkJoinPool thread. Sound/packet calls from async threads are unsafe.
- **Fix:** Wrap in `Bukkit.getScheduler().runTask()`.

### MGR-4: `CopyOnWriteArrayList` Used for Frequent Modifications
- **File:** `plugin/.../managers/TreasureManager.java` ~L252
- **Category:** Performance
- **Description:** Per-chunk and per-collection lists use `CopyOnWriteArrayList`. Every add/remove copies the entire array. For servers with thousands of treasures, loading/deleting is O(n²).
- **Fix:** Use `Collections.synchronizedList(new ArrayList<>())`.

### MGR-5: `removeCollection` Causes N Array Copies
- **File:** `plugin/.../managers/TreasureManager.java` ~L128
- **Category:** Performance
- **Description:** Iterates all cores calling `chunkList.removeIf(...)` on CopyOnWriteArrayList — full array copy per removal per chunk.
- **Fix:** Collect IDs first, then do single `removeIf` per chunk list or rebuild lists.

### INT-1: `playersCollecting` Guard Released Before Async Claim Completes
- **File:** `plugin/.../managers/TreasureInteractionHandler.java` ~L82
- **Category:** Bug
- **Description:** UUID removed from `playersCollecting` in `finally` block immediately, but the actual claim happens asynchronously. Window exists for duplicate collection.
- **Fix:** Remove UUID in the async callback instead of `finally`.

### INT-2: TOCTOU Race in Single-Player-Find
- **File:** `plugin/.../managers/TreasureInteractionHandler.java` ~L113
- **Category:** Bug
- **Description:** `getPlayersWhoFound()` check and `claimTreasure` are not atomic. Two players can both pass the empty-check and both claim.
- **Fix:** Implement atomic claim (DB unique constraint or synchronized gate).

### INT-3: Player Data Check Uses Potentially Empty Placeholder
- **File:** `plugin/.../managers/TreasureInteractionHandler.java` ~L102
- **Category:** Bug
- **Description:** `playerManager.getPlayerData()` may return placeholder with empty found-set, bypassing duplicate-collection check.
- **Fix:** Ensure data is loaded before allowing interaction.

### MGR-10: NBT Lost Due to Double `getState()` in TreasureVisibilityManager
- **File:** `plugin/.../managers/TreasureVisibilityManager.java` ~L355
- **Category:** Bug
- **Description:** `NBT.modify(block.getState(), ...)` modifies one snapshot, then `block.getState().update(true, false)` creates a fresh snapshot. NBT changes are silently discarded.
- **Fix:** `BlockState state = block.getState(); NBT.modify(state, ...); state.update(true, false);`

### DEL-1: Same Double-`getState()` Bug in CollectionDeletionCleanupManager
- **File:** `plugin/.../managers/CollectionDeletionCleanupManager.java` ~L200
- **Category:** Bug
- **Description:** Identical to MGR-10 — NBT modifications lost on block restore during collection deletion cleanup.
- **Fix:** Same as MGR-10.

### LIS-1: Chat Input Race Between Timeout and Handler
- **File:** `plugin/.../listeners/ChatInputListener.java` ~L51
- **Category:** Concurrency
- **Description:** The timeout task and chat handler can both `remove()` from the map simultaneously. Both paths execute their logic — player may see both timeout message and callback result.
- **Fix:** Use a shared `AtomicBoolean consumed` to prevent double-processing.

### LIS-8: Firework UUID Memory Leak
- **File:** `plugin/.../listeners/PlayerProtectionListener.java` ~L30-36
- **Category:** Memory
- **Description:** Firework UUID removed from set only when it damages an entity. Fireworks that detonate without hitting anything leak their UUID forever.
- **Fix:** Use `FireworkExplodeEvent` for cleanup, or use an expiring cache.

### UTIL-1: Hex Colors Broken in Reward Messages
- **File:** `plugin/.../managers/RewardManager.java` ~L196
- **Category:** Bug
- **Description:** `replacePlaceholders()` does `.replaceAll("&", "§")` converting ALL `&` to `§`. Then `HexColor.color(message, '&')` looks for `&` as the color prefix — but `&` no longer exists. Hex codes like `&#FF0000` are completely broken.
- **Fix:** Remove the blanket `& → §` replacement or apply `HexColor.color()` before it.

### UTIL-2: No HTTP Connection Timeouts in Update Sources
- **Files:** `plugin/.../util/updater/source/{ModrinthSource,SpigotSource,BukkitSource}.java`
- **Category:** Performance / Bug
- **Description:** No `setConnectTimeout()` or `setReadTimeout()` on any `HttpURLConnection`. A hanging remote server blocks the async thread indefinitely.
- **Fix:** Set 10s connect + 15s read timeouts on all connections.

### UTIL-3: HTTP Connection/Stream Resource Leaks
- **Files:** Same update source files
- **Category:** Memory
- **Description:** `HttpURLConnection`, `InputStreamReader`, and `InputStream` are never closed in try-with-resources in `getLatestUpdate()`.
- **Fix:** Wrap all connection I/O in try-with-resources.

### UTIL-4: Main.java — HintManager.stop() Never Called on Disable
- **File:** `plugin/.../Main.java` ~L254-256
- **Category:** Memory / Bug
- **Description:** `onDisable()` calls `cancelAllVisualHints()` but never `stop()`. The async cleanup task keeps running after disable.
- **Fix:** Call `hintManager.stop()`.

### UTIL-5: Main.java — LeaderboardManager.stop() Never Called on Disable
- **File:** `plugin/.../Main.java` ~L254-278
- **Category:** Memory / Bug
- **Description:** Periodic leaderboard update task keeps running after plugin disable.
- **Fix:** Add `leaderboardManager.stop()` to `onDisable()`.

### UTIL-6: FireworkManager — fireworkUUIDs Grows Unboundedly
- **File:** `plugin/.../managers/FireworkManager.java` ~L24
- **Category:** Memory
- **Description:** UUIDs added but never removed from `fireworkUUIDs` set. Grows indefinitely.
- **Fix:** Remove UUIDs on firework detonation or use expiring cache.

### UTIL-7: FireworkManager — NPE When Config Section Missing
- **File:** `plugin/.../managers/FireworkManager.java` ~L36
- **Category:** Bug
- **Description:** `getConfigurationSection("fireworks.effects")` result used without null check. Missing section causes NPE.
- **Fix:** Null-check and return default `FireworkEffect`.

### UTIL-8: ItemBuilder — NPE in hideTooltip() When Meta Is Null
- **File:** `plugin/.../util/ItemBuilder.java` ~L101-105
- **Category:** Bug
- **Description:** `hideTooltip()` calls `ensureMeta()` then directly calls `meta.setDisplayName()` without null check.
- **Fix:** `if (meta == null) return this;`

### VER-1: Post-Snapshot Migration Work on Main Thread
- **File:** `plugin/.../migration/legacy/LegacyDataMigrator.java` ~L99-170
- **Category:** Performance
- **Description:** `LegacyBlockSnapshotter` completes its future on the main thread. The entire `.thenCompose()` chain (building treasures, saving data, file cleanup) runs on main thread, blocking ticks.
- **Fix:** Use `.thenComposeAsync()` with a plugin executor.

### VER-2: `WrapperPlayServerSpawnLivingEntity` Used on 1.20.5+
- **File:** `modules/spigot-1_20_5plus/.../Spigot1205PlusPlatformAdapter.java` ~L96-113
- **Category:** Bug
- **Description:** The "Spawn Living Entity" packet was removed in 1.19+. The 1.21+ adapter correctly uses `WrapperPlayServerSpawnEntity` but 1.20.5+ doesn't.
- **Fix:** Use `WrapperPlayServerSpawnEntity` + `WrapperPlayServerEntityMetadata`.

### VER-3: Broken Fallback in `spawnGlowingBlockMarkerForPlayer` on 1.20.5+
- **File:** `modules/spigot-1_20_5plus/.../Spigot1205PlusPlatformAdapter.java` ~L124-127
- **Category:** Bug
- **Description:** Fallback calls `super.spawnGlowingBlockMarkerForPlayer()` which uses `SpawnLivingEntity` — doesn't exist in 1.20.5+ protocol. Silently fails.
- **Fix:** Override fallback to use `WrapperPlayServerSpawnEntity` or log+return false.

### VER-4: `SkullMeta.setOwner(String)` Blocks on Mojang API
- **Files:** `modules/spigot-1_8/.../Spigot18PlatformAdapter.java` ~L85, `modules/spigot-1_9plus/.../Spigot19PlatformAdapter.java` ~L66
- **Category:** Performance
- **Description:** `setOwner(ownerName)` triggers a blocking Mojang API lookup if not cached. Freezes server tick if called from main thread.
- **Fix:** Use UUID-based skull owner or perform async.

### MENU-1: `getMaxItemsPerPage()` Override Breaks Pagination
- **Files:** `plugin/.../menu/collection/LeaderboardMenu.java` ~L183, `plugin/.../menu/treasure/WhoFoundMenu.java` ~L103
- **Category:** Bug
- **Description:** Both menus override `getMaxItemsPerPage()` to return `entries.size()` (total data size) instead of per-page limit. `getTotalPages()` always returns 1, disabling pagination.
- **Fix:** Remove the `getMaxItemsPerPage()` overrides.

### MENU-2: `hasNextPage` Never Set in CollectionEditorMenu
- **File:** `plugin/.../menu/collection/CollectionEditorMenu.java` ~L38-78
- **Category:** Bug
- **Description:** Unlike other `PagedMenu` subclasses, never calculates `hasNextPage`. Defaults to `false`, so next page always shows "last page" message.
- **Fix:** Add pagination calculation like other subclasses.

### MENU-3: Repeating Timer Task Leak in MemoryMinigameMenu
- **File:** `plugin/.../menu/minigame/MemoryMinigameMenu.java` ~L53-72
- **Category:** Bug / Memory
- **Description:** Each `startRound()` creates a new repeating task without cancelling the previous round's timer. After 5 rounds, 5 concurrent tasks are running.
- **Fix:** Store and cancel the previous task reference before creating a new one.

---

## Medium-Severity Issues

### MED-1: `checkResets()` Iterates Stale `cachedCollections`
- **File:** `plugin/.../managers/CollectionManager.java` ~L194
- **Category:** Concurrency
- **Description:** Called from async timer, iterates `cachedCollections` which can be swapped by another async callback mid-iteration.
- **Fix:** Take a local snapshot of the list reference.

### MED-2: Failed `resetCollectionProgress` Silently Ignored
- **File:** `plugin/.../managers/CollectionManager.java` ~L207
- **Category:** Bug
- **Description:** `repository.resetCollectionProgress()` future result never checked. Failed resets are silent.
- **Fix:** Chain `.exceptionally()` to log failures.

### MED-3: `locationsEqual` NPE When World Is Null
- **File:** `plugin/.../managers/TreasureManager.java` ~L265
- **Category:** Bug
- **Description:** `l1.getWorld().getName()` without null-checking `getWorld()` on unloaded worlds.
- **Fix:** Add null checks.

### MED-4: `updateTreasure` Stale Core When Location Changes
- **File:** `plugin/.../managers/TreasureManager.java` ~L147
- **Category:** Bug
- **Description:** If old and new treasure share same ID but different location, old chunk key retains stale core.
- **Fix:** Always remove old core before adding new.

### MED-5: Blocking `getFullTreasure()` on Main Thread
- **File:** `plugin/.../managers/TreasureManager.java` ~L237
- **Category:** Performance
- **Description:** `.join()` blocking call can freeze the server.
- **Fix:** Migrate callers to async variant.

### MED-6: `onChunkLoad` Spawns Runnables Per Chunk
- **File:** `plugin/.../managers/TreasureVisibilityManager.java` ~L481
- **Category:** Performance
- **Description:** New `BukkitRunnable` timer per chunk load event, even when no treasures need action.
- **Fix:** Aggregate pending chunks into a single periodic task.

### MED-7: Non-Atomic Entity ID Allocation
- **File:** `plugin/.../managers/TreasureVisibilityManager.java` ~L440
- **Category:** Concurrency
- **Description:** `counter.getAndIncrement()` + overflow `counter.set(min)` is not atomic. Two threads can get the same ID.
- **Fix:** Use `updateAndGet` for atomic overflow.

### MED-8: Excessive Location Garbage in Packet Handler
- **File:** `plugin/.../managers/TreasureVisibilityManager.java` ~L370
- **Category:** Performance
- **Description:** `new Location(...)` for every block change packet on busy servers generates significant garbage.
- **Fix:** Check chunk map with raw coords first, only construct Location when needed.

### MED-9: `savePlayerData` Only Inserts, Never Removes Stale Entries (SQL)
- **File:** `plugin/.../data/SqlRepository.java` ~L306-327
- **Category:** Bug
- **Description:** `INSERT OR IGNORE` never removes rows deleted from in-memory PlayerData. Stale rows persist.
- **Fix:** Delete existing rows first, then re-insert.

### MED-10: `renameCollection` TOCTOU Race (SQL)
- **File:** `plugin/.../data/SqlRepository.java` ~L699-715
- **Category:** Concurrency
- **Description:** Uniqueness check and rename are separate statements, not transactional.
- **Fix:** Wrap in transaction or rely on UNIQUE constraint.

### MED-11: Statement Leak in `getAllPlayerUUIDs`
- **File:** `plugin/.../data/SqlRepository.java` ~L722-730
- **Category:** Memory
- **Description:** Statement not managed by try-with-resources.
- **Fix:** Use try-with-resources.

### MED-12: Read-Write Lock Gap in `saveCollection` (Yaml)
- **File:** `plugin/.../data/YamlRepository.java` ~L785-810
- **Category:** Concurrency
- **Description:** Read lock released, then write lock acquired. Changes can be lost between.
- **Fix:** Use write lock for entire read-modify-write.

### MED-13: `checkExistingDataAndMigrate` Loads Data Twice
- **File:** `plugin/.../commands/AdvancedHuntCommand.java` ~L594-614
- **Category:** Performance
- **Description:** `targetRepo.loadCollections()` called in `allOf` then again in `thenCompose`.
- **Fix:** Store and reuse initial futures.

### MED-14: `allocateClientSideEntityId` Iterates All World Entities
- **File:** `plugin/.../commands/AdvancedHuntCommand.java` ~L291-308
- **Category:** Performance
- **Description:** Builds HashSet of all entity IDs just to avoid collisions in 1–2 billion range.
- **Fix:** Remove entity scan; collision is astronomically unlikely.

### MED-15: Migration Doesn't Cover PlaceItems/PlaceItemGroups
- **File:** `plugin/.../managers/MigrationService.java`
- **Category:** Bug
- **Description:** PlaceItems and PlaceItemGroups are not migrated between backends. Data silently lost.
- **Fix:** Add migration steps for these data types.

### MED-16: Individual Load Failure Aborts Entire Migration Chunk
- **File:** `plugin/.../managers/MigrationService.java` ~L157-180
- **Category:** Bug
- **Description:** Single bad entry in `CompletableFuture.allOf()` terminates the whole batch.
- **Fix:** Use `.exceptionally(ex -> null)` per individual future.

### MED-17: `List.removeAll(List)` Is O(n*m)
- **Files:** `plugin/.../data/YamlRepository.java` ~L1725, ~L1765
- **Category:** Performance
- **Description:** `found.removeAll(collectionTreasureIds)` where both are Lists. O(n×m) per player.
- **Fix:** Convert `collectionTreasureIds` to HashSet first.

### MED-18: `Bukkit.getWorld()` Called on Async Threads
- **Files:** `plugin/.../data/SqlRepository.java` ~L342, `plugin/.../data/YamlRepository.java` ~L543
- **Category:** Concurrency
- **Description:** World lookups on async threads can return null for unloaded worlds. Null world in Location causes silent NPEs downstream.
- **Fix:** Store world name as String, defer `getWorld()` to main thread.

### MED-19: `getPlayerDataWithTimeout` Blocks Main Thread
- **File:** `plugin/.../managers/PlayerManager.java` ~L76
- **Category:** Performance
- **Description:** `CompletableFuture.get(timeout, MILLISECONDS)` blocks calling thread up to 5 seconds.
- **Fix:** Ensure only called from async contexts; add main-thread check.

### MED-20: `savePlayerData` Fire-and-Forget
- **File:** `plugin/.../managers/PlayerManager.java` ~L100
- **Category:** Best Practice
- **Description:** Save failures are silently lost.
- **Fix:** Chain `.exceptionally()` to log failures.

### MED-21: PlaceModeManager Leaks on Player Quit
- **File:** `plugin/.../managers/PlaceModeManager.java`
- **Category:** Memory
- **Description:** Player UUIDs remain in `placeModePlayers` forever if player disconnects while in place mode.
- **Fix:** Listen for `PlayerQuitEvent` and call `removePlaceMode`.

### MED-22: Static `clickCooldowns` Map Never Cleaned
- **File:** `plugin/.../menu/Button.java` ~L17
- **Category:** Memory
- **Description:** `ConcurrentHashMap<UUID, Long>` grows unboundedly. Never cleaned up.
- **Fix:** Purge stale entries periodically, or clear on player quit.

### MED-23: `processing` Flag Set from Wrong Thread
- **File:** `plugin/.../menu/act/ActRulesMenu.java` ~L89-98
- **Category:** Concurrency
- **Description:** `.exceptionally()` sets `processing = false` on ForkJoinPool thread, read from main thread. Data race.
- **Fix:** Wrap in `runTask()` or make `processing` volatile.

### MED-24: Async Thread Accessing Non-Thread-Safe Managers
- **Files:** `plugin/.../menu/collection/LeaderboardMenu.java` ~L110, `plugin/.../menu/collection/ProgressMenu.java` ~L109
- **Category:** Concurrency
- **Description:** `runTaskAsynchronously` calls manager methods that may access non-thread-safe data structures.
- **Fix:** Verify thread safety or move calls to main thread.

### MED-25: CompletableFuture Chains Lack Error Handlers
- **Files:** Multiple menu files (CollectionListMenu, CronEditorMenu, CronFieldMenu, CronPresetMenu, ActRuleEditorMenu)
- **Category:** Bug
- **Description:** `.thenRun()` without `.exceptionally()` — save failures silently swallowed.
- **Fix:** Add error handlers.

### MED-26: Concurrent `Collection` Object Mutation by Multiple Admins
- **Files:** CollectionSettingsMenu, RewardPresetActionsMenu, ActRulesMenu
- **Category:** Concurrency
- **Description:** Multiple menus mutate the same Collection object. Last save wins, silently overwriting other admin's changes.
- **Fix:** Optimistic locking or re-load before save.

### MED-27: Event Not Cancelled for Non-Sneak Treasure Interact
- **File:** `plugin/.../listeners/PlayerInteractListener.java` ~L48
- **Category:** Bug
- **Description:** When player is not sneaking, event isn't cancelled. Default block interaction (opening chest, etc.) proceeds alongside treasure collection.
- **Fix:** Cancel event before `handleTreasureCollect`.

### MED-28: Missing Protection for BlockFade/LeavesDecay/BlockFromTo Events
- **File:** `plugin/.../listeners/TreasureProtectListener.java`
- **Category:** Bug
- **Description:** No protection against ice melting, snow melting, coral dying, leaves decaying, or water/lava flow destroying treasure blocks.
- **Fix:** Add handlers for `BlockFadeEvent`, `LeavesDecayEvent`, `BlockFromToEvent`.

### MED-29: `deprecated event.getItemInHand()` in PlaceModeListener
- **File:** `plugin/.../listeners/PlaceModeListener.java` ~L42-48
- **Category:** Bug
- **Description:** Returns wrong hand's item on dual-wield servers (1.9+).
- **Fix:** Use `getInventory().getItemInMainHand()` and check `event.getHand()`.

### MED-30: CoordinateRevealType.Y Silently Does Nothing
- **File:** `plugin/.../managers/HintManager.java` ~L206-221
- **Category:** Bug
- **Description:** Switch statement handles X and Z but falls to `default: return;` for Y. Y-coordinate hints silently do nothing.
- **Fix:** Add `case Y:` handler or exclude Y from random selection.

### MED-31: `clearItemCache` Log Always Shows 0
- **File:** `plugin/.../managers/RewardManager.java` ~L218-220
- **Category:** Bug
- **Description:** Logs `itemCache.size()` after calling `clear()` — always 0.
- **Fix:** Capture size before clearing.

### MED-32: LeaderboardManager Cache Keyed by Name, Not UUID
- **File:** `plugin/.../managers/LeaderboardManager.java` ~L53
- **Category:** Bug
- **Description:** If two collections share the same name, second overwrites first's leaderboard.
- **Fix:** Key by collection UUID.

### MED-33: ScanManager — Bukkit API Calls from Async Thread
- **File:** `plugin/.../managers/ScanManager.java` ~L55-60
- **Category:** Concurrency
- **Description:** `proximityManager.processTick()` invoked from `runTaskAsynchronously`, calls `getWorld()` which is Bukkit API.
- **Fix:** Pre-resolve to primitives, or run on main thread.

### MED-34: Inconsistent Color Processing in MessageManager
- **File:** `plugin/.../managers/MessageManager.java` ~L65-100
- **Category:** Bug / Consistency
- **Description:** `getMessage(key)` uses `HexColor.color()` but `getMessage(key, applyPrefix)` uses `ChatColor.translateAlternateColorCodes()` — no hex support.
- **Fix:** Use `HexColor.color()` consistently.

### MED-35: ATOMIC_MOVE May Not Be Supported in ConfigUpdater
- **File:** `plugin/.../util/ConfigUpdater.java` ~L78
- **Category:** Bug
- **Description:** `Files.move()` with `ATOMIC_MOVE` throws `AtomicMoveNotSupportedException` on some filesystems.
- **Fix:** Catch and fall back to non-atomic move.

### MED-36: ConfigUpdater Doesn't Handle YAML Multiline Values
- **File:** `plugin/.../util/ConfigUpdater.java` ~L90-130
- **Category:** Bug
- **Description:** Line-by-line parser misinterprets continuation lines of `|` / `>` blocks as new keys.
- **Fix:** Detect multiline indicators and skip continuation lines.

### MED-37: Static `PRESETS` Date Ranges Become Stale Across Year Boundaries
- **File:** `plugin/.../menu/act/ActPresetMenu.java` ~L30-42
- **Category:** Bug
- **Description:** `getChristmasDateRange()` computed at class-load time. Plugin staying loaded across Dec 31 → Jan 1 uses old year.
- **Fix:** Compute dynamically when menu opens.

### MED-38: JSON Text Component Escaping Is Incomplete (1.13+ adapter)
- **File:** `modules/spigot-1_13plus/.../Spigot113PlatformAdapter.java` ~L30-39
- **Category:** Bug
- **Description:** `toJsonTextComponent()` only escapes `\ " \n \r \t`. Unicode/emoji can malform JSON, causing invisible hologram names.
- **Fix:** Use Gson's `JsonPrimitive` for proper string escaping.

### MED-39: `forceCleanup()` Uses Common ForkJoinPool for File I/O
- **File:** `plugin/.../migration/legacy/LegacyDataMigrator.java` ~L37-45
- **Category:** Concurrency
- **Description:** Recursive directory deletion on shared ForkJoinPool can starve other tasks.
- **Fix:** Provide a dedicated executor.

### MED-40: Async `scheduleEdits` Adds Unnecessary Tick Delay
- **File:** `plugin/.../managers/CollectionDeletionCleanupManager.java` ~L133
- **Category:** Performance
- **Description:** `runTask()` called when already on primary thread, adding unnecessary 1-tick delay.
- **Fix:** Call `applyEdits()` directly when on primary thread.

### MED-41: `queuedEdits` Counter Can Go Negative
- **File:** `plugin/.../managers/CollectionDeletionCleanupManager.java` ~L150
- **Category:** Bug
- **Description:** `stop()` sets counter to 0, but pending BukkitRunnables still decrement it.
- **Fix:** Guard with `Math.max(0, ...)`.

### MED-42: Orphan ItemsAdder Core When `getFullTreasure` Returns Null
- **File:** `plugin/.../listeners/ItemsAdderIntegrationListener.java` ~L99-111
- **Category:** Bug
- **Description:** Furniture break when core exists but `getFullTreasure()` returns null — orphan core with no cleanup.
- **Fix:** Log warning and clean up orphan core.

### MED-43: ItemsAdder Custom Block Break May Not Fire BlockBreakEvent
- **File:** `plugin/.../listeners/ItemsAdderIntegrationListener.java` ~L150-157
- **Category:** Bug
- **Description:** Place mode defers to `PlaceModeListener.onBlockBreak`, but `BlockBreakEvent` may not be fired by ItemsAdder for custom blocks.
- **Fix:** Verify or handle deletion directly.

### MED-44: `MONITOR` Priority Used for State-Modifying `onOpen`
- **File:** `plugin/.../listeners/MenuListener.java` ~L96-99
- **Category:** Best Practice
- **Description:** `MONITOR` should be read-only. If `onOpen` modifies state, this violates the contract.
- **Fix:** Use `HIGH` or `NORMAL` if `onOpen` modifies state.

### MED-45: Priority-Based Block Break Ordering Is Fragile
- **Files:** `plugin/.../listeners/PlaceModeListener.java` + `plugin/.../listeners/TreasureProtectListener.java`
- **Category:** Consistency
- **Description:** Correctness depends on PlaceMode (NORMAL) running before TreasureProtect (HIGH). If priorities change, behavior breaks.
- **Fix:** Consolidate or explicitly document ordering.

### MED-46: Treasure Block Drops on Delete in Place Mode
- **File:** `plugin/.../listeners/PlaceModeListener.java` ~L110-118
- **Category:** Consistency
- **Description:** When treasure deleted, block breaks normally with drops (e.g., diamond ore drops diamonds).
- **Fix:** Consider `event.setDropItems(false)`.

---

## Low-Severity Issues

### LOW-1: `AsyncPlayerChatEvent` Deprecated in Modern Paper
- **File:** `plugin/.../listeners/ChatInputListener.java` ~L51
- **Fix:** Support Paper's `AsyncChatEvent` alongside legacy.

### LOW-2: `onMove` Fires Extremely Frequently
- **File:** `plugin/.../listeners/ChatInputListener.java` ~L88-91
- **Fix:** Consider dynamic listener registration only while input requests are active.

### LOW-3: Missing Null/Empty Check in `onFurniturePlaceSuccess`
- **File:** `plugin/.../listeners/ItemsAdderIntegrationListener.java` ~L53-60
- **Fix:** Add same validation as `onCustomBlockPlace`.

### LOW-4: `XMaterial.AIR.get()` Called on Every Inventory Close
- **File:** `plugin/.../listeners/MenuListener.java` ~L85-92
- **Fix:** Cache the Material result.

### LOW-5: Firework UUID Set Exposed as Raw Mutable Collection
- **File:** `plugin/.../listeners/PlayerProtectionListener.java` ~L33-35
- **Fix:** Expose `isTrackedFirework()` and `removeFirework()` methods instead.

### LOW-6: Players in Place Mode Invulnerable to Void Damage
- **File:** `plugin/.../listeners/PlayerProtectionListener.java` ~L39-47
- **Fix:** Exclude `DamageCause.VOID` and `SUICIDE`.

### LOW-7: Message Spam on Rapid Treasure Clicks
- **File:** `plugin/.../listeners/TreasureProtectListener.java` ~L30-43
- **Fix:** Add per-player cooldown.

### LOW-8: Cached ItemsAdder Plugin Lookup Per Event
- **File:** `plugin/.../listeners/PlayerInteractListener.java` ~L55-65
- **Fix:** Cache plugin presence at construction.

### LOW-9: Broad `catch (NoClassDefFoundError | Exception)`
- **File:** `plugin/.../listeners/PlayerInteractListener.java` ~L62
- **Fix:** Narrow to classloading-related errors only.

### LOW-10: Missing `ignoreCancelled = true` on Interact Listener
- **File:** `plugin/.../listeners/PlayerInteractListener.java` ~L30
- **Fix:** Add `ignoreCancelled = true`.

### LOW-11: `new Random()` Instead of `ThreadLocalRandom`
- **Files:** `plugin/.../commands/AdvancedHuntCommand.java` ~L469, `plugin/.../Main.java` ~L91
- **Fix:** Use `ThreadLocalRandom.current()`.

### LOW-12: `rebuildLeaderboardCache` Not Tracked
- **File:** `plugin/.../data/YamlRepository.java` ~L2065
- **Fix:** Track via `trackFuture()`.

### LOW-13: Unused `active_start`/`active_end`/`reset_cron` Columns
- **File:** `plugin/.../data/SqlRepository.java` ~L220-225
- **Fix:** Remove or implement read/write support.

### LOW-14: `placePresetGroupsFile` Fallback Uses Wrong Filename
- **File:** `plugin/.../data/YamlRepository.java` ~L1074
- **Fix:** Use consistent filename.

### LOW-15: `fileLocks` Map Grows Unboundedly
- **File:** `plugin/.../data/YamlRepository.java` ~L55
- **Fix:** Ensure `startPeriodicFlush` called, or use bounded cache.

### LOW-16: `ResultSet` Not in Try-With-Resources
- **File:** `plugin/.../data/SqlRepository.java` ~L274-290
- **Fix:** Wrap in try-with-resources.

### LOW-17: Broad `catch (Throwable ignored)` Blocks
- **Files:** Multiple command/adapter files
- **Fix:** Log at FINE level; avoid catching `Throwable`.

### LOW-18: `getCollectionByName` Compiles UUID Regex Per Call
- **File:** `plugin/.../managers/CollectionManager.java` ~L77
- **Fix:** Pre-compile as `static final Pattern`.

### LOW-19: `reloadCollections()` Called in Constructor, Never Awaited
- **File:** `plugin/.../managers/CollectionManager.java` ~L37
- **Fix:** Document async contract or add ready-state flag.

### LOW-20: `overrideTreasureRewardsInCollection` Lacks Error Handling
- **File:** `plugin/.../managers/TreasureManager.java` ~L205
- **Fix:** Add `.exceptionally()` per batch.

### LOW-21: Deprecated `World.refreshChunk()` Used
- **File:** `plugin/.../managers/TreasureVisibilityManager.java` ~L522
- **Fix:** Use packet-based chunk resending.

### LOW-22: `PlaceModeManager.processTick` NPE Risk
- **File:** `plugin/.../managers/PlaceModeManager.java` ~L42
- **Fix:** Null-check player before use.

### LOW-23: `PlayerSnapshot` Holds Live Player Reference
- **File:** `plugin/.../util/PlayerSnapshot.java` ~L8
- **Fix:** Store UUID + snapshot data; resolve Player only when needed.

### LOW-24: Unused `reusableLocation` Field
- **File:** `plugin/.../managers/ParticleManager.java` ~L67
- **Fix:** Remove.

### LOW-25: Static Mutable `staticSoundConfig`
- **File:** `plugin/.../managers/SoundManager.java` ~L12
- **Fix:** Remove static field; use instance methods.

### LOW-26: Version Parsed on Every `ItemBuilder.build()`
- **File:** `plugin/.../util/ItemBuilder.java` ~L160-164
- **Fix:** Cache in static field.

### LOW-27: Concurrent Downloads for Same Plugin in Updater
- **File:** `plugin/.../util/updater/PluginUpdater.java` ~L47-59
- **Fix:** Track in-progress downloads per plugin.

### LOW-28: Duplicate API Calls for Download in SpigotSource/ModrinthSource
- **Files:** `plugin/.../util/updater/source/SpigotSource.java`, `ModrinthSource.java`
- **Fix:** Store download URL/version ID from `getLatestUpdate()`.

### LOW-29: Duplicate `ThreadLocal<CronParser>` Instances
- **Files:** `plugin/.../util/CronUtils.java`, `ValidationUtil.java`, `ActRuleEvaluator.java`
- **Fix:** Consolidate to single shared parser.

### LOW-30: Skull Owner UUID+Name Both Applied
- **File:** `plugin/.../util/ItemBuilder.java` ~L148-153
- **Fix:** Prioritize UUID over name.

### LOW-31: PAPI `UUID.fromString()` Exception on Every Non-UUID Request
- **File:** `plugin/.../placeholder/AdvancedHuntExpansion.java` ~L69-75
- **Fix:** Pre-check format before `UUID.fromString()`.

### LOW-32: `ItemsAdderAdapter` Bridge Never Invalidated
- **File:** `plugin/.../util/ItemsAdderAdapter.java` ~L15
- **Fix:** Add `reset()` method.

### LOW-33: `RewardPreset.getRewards()` Creates Defensive Copy Every Call
- **File:** `plugin/.../model/RewardPreset.java` ~L31
- **Fix:** Return `Collections.unmodifiableList()` instead.

### LOW-34: `ThreadLocal<CronParser>` Never Removed
- **File:** `plugin/.../util/CronUtils.java` ~L20-22
- **Fix:** Call `remove()` on disable (classloader leak on hot reload).

### LOW-35: `ConfigUpdater.MARK_BUFFER_SIZE` May Be Too Small
- **File:** `plugin/.../util/ConfigUpdater.java` ~L128
- **Fix:** Increase to 64KB.

### LOW-36: Duplicate Teleport Logic Across Menus
- **Files:** `plugin/.../menu/collection/CollectionListMenu.java`, `plugin/.../menu/treasure/TreasureActionMenu.java`
- **Fix:** Extract into `TeleportHelper` utility.

### LOW-37: `Treasure.toString()` NPE When World Null
- **File:** `plugin/.../model/Treasure.java` ~L57
- **Fix:** Null-check `getWorld()`.

### LOW-38: `replaceAll("&", "§")` Uses Regex Unnecessarily
- **File:** `plugin/.../managers/RewardManager.java` ~L196
- **Fix:** Use `.replace()` (literal).

### LOW-39: `ConcurrentHashMap` for Never-Modified Static Maps
- **File:** `plugin/.../util/ConfigMigrationHandler.java` ~L15-16
- **Fix:** Use `Collections.unmodifiableMap()`.

### LOW-40: Hardcoded Color Codes Instead of Message System
- **Files:** CollectionEditorMenu, ActRulesMenu, ActDurationMenu, CollectionSettingsMenu, RewardActionMenu
- **Fix:** Route through MessageManager.

### LOW-41: Hardcoded Year Presets (2025 Already Past)
- **File:** `plugin/.../menu/cron/CronFieldMenu.java` ~L222
- **Fix:** Generate dynamically.

### LOW-42: `PagedMenu.index` Used Inconsistently Across Subclasses
- **File:** `plugin/.../menu/PagedMenu.java` ~L16
- **Fix:** Document contract or make local.

### LOW-43: Dead Code `promptAddCommandReward()` in RewardsMenu
- **File:** `plugin/.../menu/reward/RewardsMenu.java` ~L232-252
- **Fix:** Remove.

### LOW-44: No Input Length Validation on Chat Callbacks
- **Files:** Multiple menu files
- **Fix:** Add reasonable length limits.

### LOW-45: Duplicate Preset Entry in ActPresetMenu
- **File:** `plugin/.../menu/act/ActPresetMenu.java` ~L31-32
- **Fix:** Remove duplicate.

### LOW-46: Double Feedback Message on Cron Preset Apply
- **File:** `plugin/.../menu/cron/CronEditorMenu.java` ~L126-131
- **Fix:** Consolidate to single message.

### LOW-47: Silent `catch (Throwable ignored)` in All Platform Adapters
- **Files:** All adapter files across modules
- **Fix:** Log at FINE level minimum.

### LOW-48: `LegacyParticleSpawner` EFFECTS Map Not Unmodifiable
- **File:** `modules/spigot-1_8/.../LegacyParticleSpawner.java` ~L12-30
- **Fix:** Wrap with `Collections.unmodifiableMap()`.

### LOW-49: `Spigot18PlatformAdapter.ensurePlayerHeadItem` Redundant Null Check
- **Files:** Spigot18, Spigot19, Spigot113 adapters
- **Fix:** Remove null check on `item.getType()` (never null).

### LOW-50: `sendSkullUpdatePacket` Code Duplicated Between 1.8 and 1.9+
- **Files:** Spigot18, Spigot19 adapters
- **Fix:** Extract to shared utility.

### LOW-51: `LegacyMigrationUtil.migrateLegacyPlaceholders` Case-Sensitive
- **File:** `plugin/.../migration/legacy/LegacyMigrationUtil.java` ~L18-24
- **Fix:** Use case-insensitive replacement.

### LOW-52: No Input Validation on Chat Input Callbacks
- **Files:** Multiple menu files
- **Fix:** Sanitize and limit length.

---

## Cross-Version Consistency Matrix

| Feature | 1.8 | 1.9+ | 1.13+ | 1.14+ | 1.15+ | 1.20.5+ | 1.21+ |
|---------|-----|------|-------|-------|-------|---------|-------|
| `isAir` | `"AIR"` only | `"AIR"` only | AIR/CAVE_AIR/VOID_AIR | inherited | `material.isAir()` | inherited | inherited |
| Particle spawn | Effect API | Bukkit Particle | inherited | inherited | inherited | inherited | inherited |
| Skull owner (UUID) | no-op | no-op | `setOwningPlayer` | inherited | inherited | inherited | inherited |
| Skull owner (String) | **`setOwner` (BLOCKING)** | **`setOwner` (BLOCKING)** | inherited | inherited | inherited | inherited | inherited |
| Unbreakable | no-op | no-op | no-op | `setUnbreakable` | inherited | inherited | inherited |
| Custom model data | no-op | no-op | no-op | `setCustomModelData` | inherited | inherited | inherited |
| Hide tooltip | no-op | no-op | no-op | no-op | no-op | `setHideTooltip` | inherited |
| Player head material | SKULL_ITEM | same | PLAYER_HEAD | inherited | inherited | inherited | inherited |
| Main hand check | always true | `EquipmentSlot.HAND` | inherited | inherited | inherited | inherited | inherited |
| Block state string | `block.getData()` | inherited | `BlockData.getAsString()` | inherited | inherited | inherited | inherited |
| Firework silent | no-op | `setSilent()` | inherited | inherited | inherited | inherited | inherited |
| Copy-to-clipboard | SUGGEST_COMMAND | inherited | inherited | inherited | COPY_TO_CLIPBOARD | inherited | inherited |
| Hologram spawn | SpawnLiving | SpawnLiving | SpawnLiving | SpawnLiving | inherited | **SpawnLiving** ⚠️ | SpawnEntity ✓ |
| Glow marker | returns false | Shulker | inherited | inherited | inherited | BlockDisplay (**fallback broken** ⚠️) | inherited |
| Skull packet | SkullOwner | SkullOwner (dup) | inherited | inherited | inherited | profile format | inherited |
| Armor stand flags | idx 10 | idx 11 | idx 11 | idx 14 | inherited | idx 15 | inherited |
| **Hover event class** | **`Text` — may crash** ⚠️ | **`Text` — may crash** ⚠️ | inherited | inherited | inherited | inherited | inherited |

**⚠️ = Known issue; see corresponding entry above**

---

## Aggregate Statistics

| Severity | Count |
|----------|-------|
| **CRITICAL** | 6 |
| **HIGH** | 24 |
| **MEDIUM** | 46 |
| **LOW** | 52 |
| **Total** | **128** |

### By Category

| Category | Count |
|----------|-------|
| Bug | 48 |
| Concurrency | 28 |
| Performance | 20 |
| Memory | 14 |
| Best Practice | 22 |
| Consistency | 12 |
| Security | 1 |

> **Note:** Some issues are counted in multiple categories.

---

*This document is intended to be used as a work tracking reference. Issues can be addressed individually or grouped by affected subsystem.*
