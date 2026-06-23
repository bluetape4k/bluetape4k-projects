# Issue #845 Flake Component Byte Layout

Issue #845 found that `Flake.asComponentString()` parsed generated Flake IDs in
a different byte order than `Flake.nextId()` writes.

## Decision

Parse component strings in the written layout: timestamp `Long`, six node bytes,
then sequence `Short`. Also validate the Flake ID length consistently with
`asBase62String()`.

## Lessons

- Component helpers should be tested against the exact binary layout, not only
  logged. A fixed clock and fixed node id make the expected string stable.
- The first generated ID for a fixed-clock `Flake` currently has sequence `1`
  because the generator initializes `lastTime` from the same clock value and
  then increments within the same millisecond.
- Byte layout tests should use distinct timestamp and node values so shifted
  reads cannot accidentally produce plausible output.

## Verification

- RED: `./gradlew :bluetape4k-idgenerators:test --tests "io.bluetape4k.idgenerators.flake.FlakeTest.component string uses timestamp node and sequence byte layout" --no-build-cache` failed with shifted components: `7528612310232073478-25939941-1`.
- GREEN targeted: the same Flake component layout test passed.
- Module: `./gradlew :bluetape4k-idgenerators:test --no-build-cache` passed with 1149 tests.
- Build: `./gradlew :bluetape4k-idgenerators:build --no-build-cache` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
