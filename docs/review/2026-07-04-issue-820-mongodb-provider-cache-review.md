# Issue #820 MongoDB Provider Cache 검토

## Scope

- Issue: #820 `P1: Make MongoClientProvider custom-settings cache semantics safe`
- Module: `:bluetape4k-mongodb`
- Files reviewed:
  - `data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt`
  - `data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt`
  - `data/mongodb/README.md`
  - `data/mongodb/README.ko.md`

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---|---|
| API contract | PASS | `MongoClientProvider` now caches all overloads by final immutable `MongoClientSettings`; provider-managed ownership is explicit through `close(...)` and `closeAll()`. |
| Correctness | PASS | Same URL plus equal settings returns the same client; same URL plus different settings returns different clients. |
| Resource lifecycle | PASS | Provider-managed clients are removed before `closeSafe()`; `closeAll()` removes by `(key, value)` to avoid clearing entries created concurrently after iteration starts. |
| Kotlin style | PASS | Uses package-level helper style already present in the module, `requireNotBlank`, `val`, and bluetape4k assertion extensions in touched tests. |
| Test quality | PASS | Regression tests cover equal settings, different settings with the same URL, and explicit provider close semantics. MockK operations remain field-level with `clearMocks` in `@BeforeEach`. |
| Documentation | PASS | English and Korean READMEs describe settings-based cache keys and provider-managed shared-client ownership. Public KDoc is English. |
| Compatibility | PASS | `getOrCreate(connectionString)` and builder overloads remain source-compatible; only cache semantics become settings-aware. |

## Verification

- `./gradlew :bluetape4k-mongodb:compileKotlin :bluetape4k-mongodb:compileTestKotlin :bluetape4k-mongodb:test :bluetape4k-mongodb:koverXmlReport --no-build-cache --no-configuration-cache`
  - Result: PASS
  - Tests: 52 passing
  - Coverage XML: `data/mongodb/build/reports/kover/report.xml`
- `git diff --check`
  - Result: PASS

## Residual Risk

- Shutdown hooks may still see already-closed clients when a provider entry is closed explicitly before JVM shutdown. This is acceptable because provider close paths use `closeSafe()` and MongoDB clients tolerate repeated close calls.
- `closeAll()` is intended for lifecycle and test boundaries, not as a hot-path cache eviction policy.
