# Issue #853 NetCDF Progress Lease Owner

Issue #853 found that `NetCdfImportProgressRepository` used the shared progress
row id as the only write authority. When an `IN_PROGRESS` lease expired and a
second importer reacquired the same `(fileId, variableName)` row, the stale
importer still held the same `progressId` and could renew, complete, or fail the
row owned by the new importer.

## Decision

Use the current `lease_expires_at` value returned by `acquireLease` as the lease
owner token. `renewLease`, `markCompleted`, and `markFailed` now require that
expected token in their `WHERE` clause. A mismatched token produces
`NetCdfException.ImportLeaseLost` and leaves the current owner row unchanged.

No schema migration is needed because the existing lease expiry timestamp is
already updated on every acquisition and renewal.

## Lessons

- A reusable progress row id is not a lease ownership proof. Reacquiring an
  expired row must create a new write token even when the primary key stays the
  same.
- Heartbeat renewal must return the next token. Completion and failure paths
  then prove ownership against the latest lease, not the original acquisition.
- Stale-owner tests should cover every terminal writer, not just heartbeat
  renewal. Otherwise a stale importer can still corrupt the row by marking it
  completed or failed.

## Verification

- RED: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23a - stale lease owner cannot renew after expired lease is reacquired" --no-build-cache` failed with `Expected <99> to be <null>`.
- GREEN targeted: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23a - stale lease owner cannot renew after expired lease is reacquired" --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23b - stale lease owner cannot complete after expired lease is reacquired" --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23c - stale lease owner cannot fail after expired lease is reacquired" --no-build-cache`
- Regression: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest" --no-build-cache`
- Module: `./gradlew :bluetape4k-science:test --no-build-cache` passed with 214 tests.
