# Legacy Data Migration Security Audit Report
**AdvancedHunt Plugin - Data Loss Risk Analysis**  
**Date:** February 3, 2026  
**Scope:** Legacy data migration system (`de.theredend2000.advancedhunt.migration.legacy`)

---

## Executive Summary

**OVERALL RISK LEVEL: HIGH** ⚠️

The legacy migration system has **CRITICAL data loss and consistency risks**. The primary concerns are:

1. **No transaction support** - Partial migrations leave inconsistent state
2. **No rollback mechanism** - Failed migrations cannot be undone
3. **Silent data loss** - Missing treasures and player progress dropped without warning
4. **No validation** - Migrated data never verified against source
5. **Marker file timing** - Written before migration fully completes
6. **No resume capability** - Large migrations must restart from scratch on failure

**Recommendation:** Do NOT use this migration system in production without implementing transaction support and validation.

---

## Critical Issues (Data Loss Imminent)

### 🔴 CRITICAL #1: No Transaction Boundary or Rollback
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L118-L165)  
**Severity:** CRITICAL  
**Lines:** 118-165

**Issue:**
```java
// Multiple independent async saves - no transaction!
CompletableFuture<?>[] saveCollections = newCollections.stream()
    .map(repository::saveCollection)
    .toArray(CompletableFuture[]::new);

return CompletableFuture.allOf(saveCollections)
    .thenCompose(v2 -> repository.saveTreasuresBatch(treasures))
    .thenCompose(v2 -> repository.savePlayerDataBatch(playerDataList))
    // ... continues with more saves
```

If `saveTreasuresBatch()` fails after `saveCollections` succeeds:
- Collections are saved to database
- Treasures are NOT saved
- Player data is NOT saved
- System is in **inconsistent state**

The error handler at line 161-165 only logs the error:
```java
.handle((result, ex) -> {
    if (ex != null) {
        plugin.getLogger().log(Level.SEVERE, "Legacy migration failed", ex);
        return LegacyDataMigrator.<LegacyMigratorResult>failedFuture(ex);
    }
    return CompletableFuture.completedFuture(result);
})
```

**Impact:** 
- Partial migration creates orphaned collections with no treasures
- Player data references non-existent treasures
- Manual database cleanup required
- Data integrity compromised

**Fix:**
- Implement database transaction support
- Wrap all saves in single transaction
- On failure, rollback ALL changes
- Delete marker file if rollback occurs

---

### 🔴 CRITICAL #2: Marker File Written Before Migration Completes
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L147-L148)  
**Severity:** CRITICAL  
**Lines:** 147-148

**Issue:**
```java
.thenApply(v2 -> {
    writeMarker(marker, legacyRoot, newCollections.size(), treasures.size(), 
        playerDataList.size(), rewardPresetCount, placePresetCount, finalTargetBackup);

    cleanupLegacyFiles(legacyRoot);  // Can fail AFTER marker is written

    return new LegacyMigratorResult(...);
})
```

The marker file is written **before** `cleanupLegacyFiles()` executes. If cleanup fails:
- Marker file exists (claims migration succeeded)
- Legacy files still present
- Next startup skips migration due to marker
- Legacy data never cleaned up

**Impact:**
- Migration appears successful but isn't complete
- Legacy data persists indefinitely
- Duplicate data in system
- Confusion about migration state

**Fix:**
- Move `writeMarker()` to separate `.thenCompose()` after cleanup
- Only write marker if cleanup succeeds
- Add cleanup verification before writing marker

---

### 🔴 CRITICAL #3: No Validation of Migrated Data
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L118-L155)  
**Severity:** CRITICAL  
**Lines:** 118-155

**Issue:**
The migration saves data but **never verifies** that saved data matches source:
- No count verification (eggs parsed ≠ treasures created)
- No integrity checks on saved records
- Silent loss of treasures when world not loaded
- No comparison of parsed vs persisted data

Example of silent data loss at lines 111-117:
```java
LegacyBlockSnapshotter.Snapshot snap = snapshots.get(key);
if (snap == null) {
    plugin.getLogger().warning("Skipping legacy egg (no world/block snapshot): " + key);
    continue;  // Treasure lost - no count tracking!
}

org.bukkit.World world = org.bukkit.Bukkit.getWorld(e.egg.world);
if (world == null) {
    plugin.getLogger().warning("Skipping legacy egg (world not loaded): " + key);
    continue;  // Another treasure lost!
}
```

**Impact:**
- **Silent data loss** - treasures skipped without reporting total loss
- Admin thinks migration succeeded when data was lost
- No way to verify data integrity
- Cannot detect corruption or save failures

**Fix:**
- Add validation phase after all saves complete
- Query database to verify record counts match source
- Compare parsed totals vs saved totals
- FAIL migration if counts don't match
- Log detailed discrepancy report

---

### 🔴 CRITICAL #4: Silent Loss of Player Progress Data
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L136-L142)  
**Severity:** CRITICAL  
**Lines:** 136-142

**Issue:**
```java
for (LegacyPlayerDataParser.LegacyFoundEgg fe : lp.found) {
    if (!legacyNameToCollectionId.containsKey(fe.collectionName)) {
        continue;  // SILENTLY SKIP - no log, no count
    }

    LegacyLocationKey key = new LegacyLocationKey(fe.collectionName, fe.world, fe.x, fe.y, fe.z);
    UUID treasureId = legacyToTreasureId.get(key);
    if (treasureId == null) {
        missingLinks++;  // Counted but NEVER reported to user
        continue;
    }
    pd.addFoundTreasure(treasureId);
    foundLinks++;
}
```

The `missingLinks` counter is tracked but:
- Never written to marker file
- Never logged for admin review
- Never included in migration result summary
- Players lose progress with no notification

**Impact:**
- **Player progress data lost** without admin knowledge
- Players lose credit for found treasures
- No way to identify affected players
- Cannot restore lost progress

**Fix:**
- Log each missing link with player UUID and location
- Add `missingLinks` to marker file summary
- FAIL migration if missing links > threshold (e.g., 5%)
- Provide report file of all lost progress for manual review

---

## High Severity Issues (Data Loss Likely)

### 🟠 HIGH #5: No Resume Capability on Failure
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L74-L156)  
**Severity:** HIGH  
**Lines:** 74-156

**Issue:**
Migration is all-or-nothing with no checkpointing:
- Long migrations (10,000+ eggs) can take minutes
- If failure occurs at 95% completion, must restart from 0%
- No way to resume from last successful checkpoint

**Impact:**
- Large datasets may never successfully migrate
- Server crashes during migration force full restart
- Wasted time and resources
- Increased risk of partial migration

**Fix:**
- Implement checkpoint system (save every 1000 records)
- Store checkpoint progress in temporary marker file
- On restart, check for checkpoint and resume
- Only delete checkpoint marker after full success

---

### 🟠 HIGH #6: Backup Not Verified Before Migration
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L78-L83)  
**Severity:** HIGH  
**Lines:** 78-83

**Issue:**
```java
if (config.createBackups()) {
    try {
        targetBackup = ZipBackupUtil.createZipBackup(plugin.getDataFolder(), backupsDir, "legacy-migration-target");
        plugin.getLogger().info("Created migration backup: " + targetBackup.getAbsolutePath());
    } catch (IOException e) {
        return failedFuture(e);  // Good - fails if backup fails
    }
}
```

While it fails if backup creation fails, it doesn't verify:
- Backup file is not corrupt
- Backup contains expected files
- Backup is extractable/restorable
- Backup has sufficient data

**Impact:**
- Corrupt backup discovered only when restoration needed
- Cannot restore after failed migration
- False sense of security

**Fix:**
- Verify backup ZIP integrity after creation
- Check that backup contains key files (data folders)
- Log backup file size and entry count
- Optionally test-extract one file to verify readability

---

### 🟠 HIGH #7: Cleanup Errors Swallowed
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L198-L201)  
**Severity:** HIGH  
**Lines:** 198-201

**Issue:**
```java
try {
    int deletedDirs = 0;
    int deletedFiles = 0;
    // ... cleanup logic ...
} catch (Exception e) {
    plugin.getLogger().log(Level.WARNING, "Legacy cleanup failed", e);
    // Error swallowed - no re-throw, no marker update
}
```

If cleanup fails:
- Exception is logged but ignored
- Legacy files remain on disk
- Marker file indicates success
- Next run skips cleanup (see lines 60-65)

**Impact:**
- Legacy data never deleted
- Disk space wasted
- Duplicate data potential
- Admin thinks cleanup succeeded

**Fix:**
- Propagate cleanup failure to calling code
- Do NOT write marker if cleanup fails
- Add cleanup retry logic on next startup
- Force cleanup before writing final marker

---

### 🟠 HIGH #8: World Validation Happens Too Late
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L110-L117)  
**Severity:** HIGH  
**Lines:** 110-117

**Issue:**
World existence is checked **during** treasure creation, not **before** migration starts:
```java
org.bukkit.World world = org.bukkit.Bukkit.getWorld(e.egg.world);
if (world == null) {
    plugin.getLogger().warning("Skipping legacy egg (world not loaded): " + key);
    continue;  // Migration continues with partial data
}
```

**Impact:**
- Migration starts without validating all worlds exist
- Partial migration completes with missing data
- Admin must manually re-run after loading missing worlds
- Data loss if worlds are never loaded

**Fix:**
- Pre-validate all referenced worlds before migration starts
- FAIL fast if any world is missing
- Log list of required worlds vs loaded worlds
- Give admin chance to load missing worlds before migration

---

## Medium Severity Issues (Data Quality Concerns)

### 🟡 MEDIUM #9: Exception in Async Block Snapshotter
**File:** [LegacyBlockSnapshotter.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyBlockSnapshotter.java#L50-L62)  
**Severity:** MEDIUM  
**Lines:** 50-62

**Issue:**
```java
@Override
public void run() {
    try {
        int end = Math.min(idx + batch, locations.size());
        for (; idx < end; idx++) {
            // ... snapshot logic ...
        }
        // ...
    } catch (Throwable t) {
        cancel();
        plugin.getLogger().log(Level.SEVERE, "Legacy block snapshot failed", t);
        future.completeExceptionally(t);
    }
}
```

If exception occurs:
- Task cancelled but no cleanup of partial results
- `out` map contains incomplete data
- Calling code receives exception but partial data still in map
- No indication which snapshots succeeded vs failed

**Impact:**
- Inconsistent snapshot data
- Hard to debug which blocks failed
- Partial data could cause downstream issues

**Fix:**
- Clear `out` map before completing exceptionally
- Log which location index failed
- Return empty map on error (fail-safe)

---

### 🟡 MEDIUM #10: Silent Skip of Invalid Eggs
**File:** [LegacyEggsParser.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyEggsParser.java#L144-L146)  
**Severity:** MEDIUM  
**Lines:** 144-146

**Issue:**
```java
if (world == null || world.trim().isEmpty()) {
    continue;  // No logging!
}
```

Eggs with invalid world names are silently dropped.

**Impact:**
- Admin doesn't know data was excluded
- Cannot fix source data before migration
- Lost treasures not reported

**Fix:**
- Log warning for each skipped egg with collection name and coordinates
- Add skipped egg count to migration result
- Include skipped eggs in marker file summary

---

### 🟡 MEDIUM #11: Player Files with Invalid UUID Silently Skipped
**File:** [LegacyPlayerDataParser.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyPlayerDataParser.java#L47-L50)  
**Severity:** MEDIUM  
**Lines:** 47-50

**Issue:**
```java
UUID uuid;
try {
    uuid = UUID.fromString(base);
} catch (Exception ignored) {
    continue;  // No logging
}
```

Files with invalid UUID names are silently ignored.

**Impact:**
- Corrupted player data files ignored
- Admin unaware of data quality issues
- Potential data loss

**Fix:**
- Log warning for each skipped file with filename
- Add count of skipped files to result
- Could indicate data corruption that needs investigation

---

### 🟡 MEDIUM #12: No ACT Rule Translation Verification
**File:** [LegacyActRuleTranslator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyActRuleTranslator.java#L30-L90)  
**Severity:** MEDIUM  
**Lines:** 30-90

**Issue:**
Complex translation logic with no verification output:
- No logging of what was converted
- No comparison of input requirements vs output rules
- Admin cannot verify correct translation
- Silent failures in cron expression generation

**Impact:**
- Translation errors undetected
- Collection availability incorrect
- Admin must manually verify all ACT rules

**Fix:**
- Log input requirements and output rules for each collection
- Validate generated cron expressions
- Compare expected vs actual behavior
- Provide migration report of all translations

---

### 🟡 MEDIUM #13: Memory Risk with Large Datasets
**File:** [LegacyBlockSnapshotter.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyBlockSnapshotter.java#L32-L67)  
**Severity:** MEDIUM  
**Lines:** 32-67

**Issue:**
All snapshots stored in single HashMap in memory:
```java
Map<LegacyLocationKey, Snapshot> out = new HashMap<>();
```

For 10,000+ eggs:
- All snapshot data held in memory simultaneously
- NBT data can be large (complex block states)
- Potential OutOfMemoryError

**Impact:**
- Migration fails on large datasets
- Server crashes possible
- Performance degradation

**Fix:**
- Stream results to disk instead of memory
- Use batch processing with intermediate saves
- Add memory monitoring and warning

---

### 🟡 MEDIUM #14: Case Normalization May Break Matching
**File:** [LegacyLocationKey.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyLocationKey.java#L24-L26)  
**Severity:** MEDIUM  
**Lines:** 24-26

**Issue:**
```java
private static String normalize(String s) {
    if (s == null) return "";
    return s.trim().toLowerCase(Locale.ROOT);
}
```

Collection and world names converted to lowercase for matching.

**Impact:**
- If legacy data has "MyWorld" and new data has "myworld", match fails
- Player progress linking breaks
- Treasures orphaned

**Fix:**
- Document case normalization behavior
- Warn if mixed-case names detected in source data
- Optionally preserve original case with case-insensitive matching

---

### 🟡 MEDIUM #15: Unsupported Reset Patterns Return Null
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L224-L243)  
**Severity:** MEDIUM  
**Lines:** 224-243

**Issue:**
```java
private static String translateProgressResetCron(LegacyEggsParser.LegacyReset reset) {
    // ... simple interval checks ...
    return null;  // Most patterns not supported!
}
```

Complex reset schedules return `null` and are lost.

**Impact:**
- Players lose progress reset schedules
- Collection behavior changes unexpectedly
- Manual reconfiguration required

**Fix:**
- Log warning when reset pattern cannot be migrated
- Provide report of all unsupported resets
- Document limitations
- Add support for more patterns

---

### 🟡 MEDIUM #16: Place Item Serialization Failure Silent
**File:** [LegacyPlacePresetParser.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyPlacePresetParser.java#L73-L76)  
**Severity:** MEDIUM  
**Lines:** 73-76

**Issue:**
```java
String serialized = ItemSerializer.serialize(item);
if (serialized == null) {
    return null;  // No logging
}
```

Serialization failures silently return null.

**Impact:**
- Place presets lost without notification
- Admin unaware of missing presets
- Cannot debug serialization issues

**Fix:**
- Log warning when serialization fails with preset name
- Add count of failed presets to result
- Include error details for debugging

---

### 🟡 MEDIUM #17: No Duplicate Egg Detection
**File:** [LegacyDataMigrator.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyDataMigrator.java#L101-L108)  
**Severity:** MEDIUM  
**Lines:** 101-108

**Issue:**
No deduplication of eggs at same location:
```java
for (LegacyEggsParser.LegacyPlacedEgg egg : lc.placedEggs) {
    eggsToCreate.add(new PlacedEggToCreate(lc.name, newCollectionId, egg));
    // No check for duplicates!
}
```

**Impact:**
- Duplicate treasures created if legacy data has duplicates
- Collection count incorrect
- Player progress tracking confused

**Fix:**
- Check for duplicate location keys before adding
- Log warning if duplicates detected
- Only keep first occurrence or merge rewards

---

## Low Severity Issues (Minor Concerns)

### 🟢 LOW #18: Reward Order Not Preserved
**File:** [LegacyEggsParser.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyEggsParser.java#L159-L161)  
**Severity:** LOW  
**Lines:** 159-161

**Issue:**
```java
// Preserve numeric ordering if possible
out.removeIf(Objects::isNull);
return out;
```

Comment indicates ordering should be preserved, but no implementation.

**Impact:**
- Reward execution order may change
- Minor behavior difference from legacy

**Fix:**
- Sort rewards by original key if numeric
- Parse key as integer for ordering

---

### 🟢 LOW #19: No Validation of Reward Commands
**File:** [LegacyRewardPresetParser.java](plugin/src/main/java/de/theredend2000/advancedhunt/migration/legacy/LegacyRewardPresetParser.java#L71-L76)  
**Severity:** LOW  
**Lines:** 71-76

**Issue:**
```java
String command = r.getString("command");
if (command == null || command.trim().isEmpty()) {
    continue;
}
command = LegacyMigrationUtil.migrateLegacyPlaceholders(command);
rewards.add(new Reward(RewardType.COMMAND, chance, null, null, command));
```

No validation that command is well-formed or will execute.

**Impact:**
- Malformed commands migrated
- Runtime errors when rewards execute
- Admin must manually review

**Fix:**
- Add basic command syntax validation
- Warn about potentially invalid commands
- Log all migrated commands for review

---

## Missing Features (Risk Amplifiers)

### ❌ No Pre-Migration Dry-Run Mode
- Cannot test migration without modifying data
- Cannot preview what will be migrated
- Cannot detect issues before committing

**Recommendation:** Add `--dry-run` flag that parses and reports without saving.

---

### ❌ No Migration Report File
- Only logs to console (loses history)
- No persistent record of what was migrated
- Cannot audit migrations months later

**Recommendation:** Generate detailed migration report file with:
- All parsed counts
- All saved counts
- Discrepancies
- Warnings
- Timing information

---

### ❌ No Data Integrity Checks
- No validation that UUIDs are unique
- No check for circular references
- No foreign key validation

**Recommendation:** Add validation phase that checks data relationships.

---

### ❌ No Migration Version Tracking
- No way to know which migration logic was used
- Cannot track migration schema evolution
- Cannot revert to previous migration

**Recommendation:** Store migration version in marker file.

---

## Recommended Fix Priority

### Immediate (Before Any Production Use)
1. **Add transaction support and rollback** (CRITICAL #1)
2. **Fix marker timing** (CRITICAL #2)  
3. **Add data validation** (CRITICAL #3)
4. **Report missing player progress** (CRITICAL #4)
5. **Validate worlds upfront** (HIGH #8)

### Short Term (Before Large Migrations)
6. **Add resume capability** (HIGH #5)
7. **Verify backups** (HIGH #6)
8. **Fix cleanup error handling** (HIGH #7)
9. **Add dry-run mode**
10. **Generate migration report file**

### Medium Term (Quality Improvements)
11. **Add logging for skipped data** (MEDIUM #9-11)
12. **Memory optimization** (MEDIUM #13)
13. **ACT translation verification** (MEDIUM #12)
14. **Duplicate detection** (MEDIUM #17)

---

## Testing Recommendations

### Unit Tests Needed
- [ ] Parser tests with corrupted YAML files
- [ ] Parser tests with missing required fields
- [ ] Parser tests with invalid data types
- [ ] Location key collision tests
- [ ] ACT rule translation tests
- [ ] Placeholder migration tests

### Integration Tests Needed
- [ ] Full migration with small dataset
- [ ] Migration with missing worlds
- [ ] Migration with invalid player UUIDs
- [ ] Migration failure and rollback
- [ ] Backup creation and restoration
- [ ] Duplicate egg handling
- [ ] Memory stress test with 10,000+ eggs

### Manual Test Cases
- [ ] Migration with empty legacy data
- [ ] Migration with partially corrupted files
- [ ] Migration interruption (server crash)
- [ ] Migration with unloaded worlds
- [ ] Verify migrated data manually
- [ ] Test backup restoration

---

## Code Quality Issues

### Poor Error Messages
- Generic exceptions with no context
- Stack traces without actionable information
- Missing location/file details in errors

### Missing Logging
- No progress indicators during long operations
- No timing information
- No detailed success metrics

### No Metrics
- No tracking of migration performance
- No statistics on data quality
- No failure rate tracking

---

## Documentation Gaps

### Missing Documentation
- No migration troubleshooting guide
- No rollback procedure documentation
- No data loss scenarios documented
- No manual intervention procedures

### Needed Documentation
1. **Migration Prerequisites**
   - Required worlds to be loaded
   - Required disk space
   - Required memory
   - Estimated time for dataset size

2. **Failure Recovery**
   - How to rollback failed migration
   - How to restore from backup
   - How to clean up partial migration
   - How to retry after fixing issues

3. **Data Loss Scenarios**
   - When treasures are skipped
   - When player progress is lost
   - When presets fail to migrate
   - How to detect data loss

---

## Summary Statistics

| Category | Count |
|----------|-------|
| **Critical Issues** | 4 |
| **High Severity** | 4 |
| **Medium Severity** | 9 |
| **Low Severity** | 2 |
| **Missing Features** | 4 |
| **Total Issues** | 23 |

---

## Final Recommendation

**DO NOT USE IN PRODUCTION** until at minimum these are fixed:
1. Transaction support with rollback
2. Data validation and verification
3. Proper marker timing (after all operations complete)
4. Missing player progress reporting
5. Upfront world validation

The migration system shows good architectural design (async operations, backup creation, batch processing) but **lacks critical data safety features**. The risk of silent data loss is **unacceptably high** for production use.

**Estimated effort to fix critical issues:** 2-3 days of development + 1-2 days of testing.
