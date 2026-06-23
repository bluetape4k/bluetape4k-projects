# Issue #853 NetCDF Progress Lease Owner Review

## Scope

- `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt`
- `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/repository/NetCdfImportProgressRepository.kt`
- `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/NetCdfTables.kt`
- `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Correctness | Reacquired expired leases keep the same row id, so row id alone cannot authorize writes. | P1 | `lease_expires_at` is now an owner token checked by all progress writers. |
| Stale writer safety | Stale renew, complete, and fail calls previously updated the current owner's row. | P1 | All three calls now throw `ImportLeaseLost` on token mismatch and leave the row unchanged. |
| Service behavior | Renewing a lease changes the owner token needed by later completion or failure handling. | P1 | `NetCdfCatalogService` carries a mutable lease token through import context and catch paths. |
| API clarity | Callers need a typed stop signal when lease ownership is lost. | P2 | Added `NetCdfException.ImportLeaseLost`. |
| Schema impact | Adding a separate owner column would require migration for a narrow bugfix. | P3 | Reused existing timestamp token, avoiding schema churn. |
| Test evidence | RED captured stale renewal mutating `lastSliceIdx`; GREEN covers renew, complete, fail, and class regressions. | P0/P1 | Evidence recorded below and in PR DoD. |

## Verification

- RED: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23a - stale lease owner cannot renew after expired lease is reacquired" --no-build-cache` failed with `Expected <99> to be <null>`.
- GREEN targeted: same module task with tests `23a`, `23b`, and `23c` passed with 3 tests.
- Regression: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest" --no-build-cache` passed with 33 tests.
- Module: `./gradlew :bluetape4k-science:test --no-build-cache` passed with 214 tests.
- Whitespace: `git diff --check` passed.
