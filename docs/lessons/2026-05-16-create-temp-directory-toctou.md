# createTempDirectory TOCTOU Race Fix

**Date**: 2026-05-16
**Issue**: #479
**Branch**: `fix/create-temp-directory`

## Root Cause

`createTempDirectory` used a three-step sequence with a time-of-check/time-of-use (TOCTOU) race:

```kotlin
// BEFORE (racy)
val dir = File.createTempFile(prefix, suffix)  // 1. create temp FILE
dir.deleteRecursively()                         // 2. delete it
dir.mkdirs()                                    // 3. recreate as directory
```

Between step 2 and step 3, another process can claim the same path. Additionally, neither
`deleteRecursively()` nor `mkdirs()` return values were checked, so failure was silently
ignored and the function returned as if a valid directory was created.

## Fix

Replace the racy sequence with the JDK atomic `Files.createTempDirectory` API:

```kotlin
// AFTER (atomic)
val dir = Files.createTempDirectory(prefix).toFile()
```

`Files.createTempDirectory` creates the directory in a single atomic OS syscall — there is no
window for another process to claim the path.

The `suffix` parameter is retained in the signature for API compatibility but is now ignored.
The generated directory name no longer ends with the suffix value (e.g., no `.dir` extension);
`Files.createTempDirectory` generates its own unique numeric suffix.

## Test Coverage

Three new tests in `FileSupportTest`:

1. **Atomic creation** — asserts the returned path exists and `isDirectory == true`.
2. **Uniqueness** — two sequential calls produce distinct canonical paths.
3. **Concurrent uniqueness** — 50 concurrent calls on 8 threads all produce unique,
   existing directories, demonstrating the race-free property.

## Key Lessons

**Use JDK atomic filesystem APIs for temporary resources.**
`File.createTempFile` + delete + mkdir is a classic TOCTOU pattern. `Files.createTempDirectory`
(and `Files.createTempFile`) are designed to create resources atomically. Prefer them always.

**Check return values for filesystem operations.**
`deleteRecursively()` and `mkdirs()` return `Boolean` indicating success. In the old code
both were ignored, meaning silent failure was possible. The new code avoids these operations
entirely; the shutdown hook uses `runCatching` to log failures without masking the primary result.

## Verification

```
:bluetape4k-io:test (FileSupportTest)  16 passing (2.1s) — BUILD SUCCESSFUL
```
