# Issue #845 Flake Component Layout 검토

## Scope

- `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/flake/Flake.kt`
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/flake/FlakeTest.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Binary layout correctness | `asComponentString()` read node bytes before timestamp bytes. | P1 | It now reads timestamp, node, sequence in the same order `nextId()` writes them. |
| Public helper contract | KDoc promises `{timestamp}-{nodeId}-{sequence}`. | P1 | Added deterministic assertion for exact component output. |
| Input validation | `asComponentString()` accepted arbitrary byte array sizes while `asBase62String()` requires 16 bytes. | P2 | Added the same 16-byte size guard. |
| Regression stability | Existing tests only logged component strings. | P2 | Fixed clock and fixed node id make the expected output deterministic. |

## Verification

- RED: component layout regression failed with shifted timestamp and node values.
- GREEN targeted: component layout regression passed.
- Module: `./gradlew :bluetape4k-idgenerators:test --no-build-cache` passed with 1149 tests.
- Build: `./gradlew :bluetape4k-idgenerators:build --no-build-cache` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
