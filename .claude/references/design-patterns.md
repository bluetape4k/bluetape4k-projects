# Key Design Patterns Reference

## Assert vs Require (CRITICAL — do NOT change exception types)

Two distinct validation utilities with different exception contracts:

- `assertXxx()` (`AssertSupport.kt`) → throws **`AssertionError`** — internal invariants only, `@Deprecated(WARNING)`
- `requireXxx()` (`RequireSupport.kt`) → throws **`IllegalArgumentException`** — caller contract / parameter validation

**Production code MUST use `requireXxx()`.**
Changing `assertXxx()` to throw `IllegalArgumentException` breaks 30+ cross-module tests.
`AssertSupportTest` explicitly guards this contract.

## Coroutines-First

All async work uses Coroutines. Wrap blocking APIs with `withContext(Dispatchers.IO)`.

## Record / Model Data Class

Must implement `Serializable` + `companion object : KLogging()` + `serialVersionUID = 1L`. Place in
`exposed.model` package.

## Repository Generic

`<ID: Any, E: Any>` — no table type generic.
`SoftDeleted*` repos retain `T` for `table.isDeleted`.

## NearCache

- `NearCacheOperations<V>` (blocking), `SuspendNearCacheOperations<V>` (suspend)
- Use `lettuceNearCacheOf<V>()` + `.withResilience {}`

## Auditable Pattern (3 layers)

- `exposed-core` → `AuditableIdTable` + `UserContext`
- `exposed-dao` → `AuditableEntity` auto-sets createdBy/updatedBy
- `exposed-jdbc` → `auditedUpdateById()` / `auditedUpdateAll()` auto-sets updatedAt/updatedBy

**Always use `auditedUpdate*` for UPDATE operations.**

## NetCDF Pipeline (`utils/science`)

Three Exposed tables:

- `NetCdfFileTable` (`AuditableLongIdTable`) — file metadata
- `NetCdfGridValueTable` (`LongIdTable`) — grid cells with nullable PostGIS
  `location`, partial expression unique index via `MD5(ST_AsBinary(location))`
- `NetCdfImportProgressTable` (`LongIdTable`) — system-only state, `lastSliceIdx` cursor + `leaseExpiresAt` heartbeat

Slice insert uses raw `INSERT ... ON CONFLICT DO NOTHING` — Exposed `upsert` cannot match expression unique indexes.

## AwsEmulatorServer (`testing/testcontainers`)

- Common interface for local AWS emulators
- Properties use `aws` prefix (`awsEndpoint`/`awsAccessKey`/`awsSecretKey`) to avoid JVM getter collision with
  `LocalStackContainer.getEndpoint()`
- `getCredentialProvider()` lives in `AwsEmulatorServerExtensions.kt` (requires `aws2-auth` on classpath)
- Switch emulators at test runtime: `-Dbluetape4k.aws.emulator=floci|localstack`
- `LocalStackServer` and `MinIOServer` are `@Deprecated(WARNING)`

## High-Performance Stack

LZ4/Zstd compression · Kryo/Fory serialization · Custom Redis codecs

## Virtual Threads

- Never use `@Synchronized` / `synchronized {}` — use `reentrantLock()` instead (monitor locks pin carrier threads)
- `atomicfu`: class property level only — never method-local (use `java.util.concurrent.atomic.*` locally)

## Test Resource Files (new modules)

Every new module MUST include:

- `src/test/resources/junit-platform.properties`
- `src/test/resources/logback-test.xml`
