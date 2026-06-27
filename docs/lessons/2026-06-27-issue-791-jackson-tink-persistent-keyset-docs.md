# Issue 791: Jackson Tink durable-search guidance

## Context

Jackson2 and Jackson3 field-encryption docs described `DETERMINISTIC_AES256_SIV` as suitable for DB search, but `@JsonTinkEncrypt` resolves algorithms through `TinkEncryptors` singleton instances. Those singleton keysets are generated in memory for the current JVM process, so persisted ciphertext can become unreadable or search-incompatible after restart, rollout, or multi-instance access.

## Decision

Use the documentation-safe fix path for this milestone issue: remove durable DB-search claims from Jackson field-encryption README/KDoc, keep the API behavior unchanged, and point durable searchable storage guidance to `bluetape4k-tink` versioned keyset APIs.

## Outcome

- Jackson2 and Jackson3 no longer promote `@JsonTinkEncrypt(DETERMINISTIC_AES256_SIV)` for durable DB search.
- `JsonTinkEncrypt`, `TinkEncryptAlgorithm`, and `TinkEncryptors` KDoc now describe the process-local singleton-keyset boundary.
- Tink README guidance now recommends `TinkDaeads.versioned(store)` with persisted AES-SIV keysets for durable searchable DB columns.

## Verification

- Red evidence: pre-change `rg` showed Jackson README/KDoc DB-search claims.
- `rg -n "DB search|DB 검색|searchable in DB|DB 검색 가능" io/jackson2 io/jackson3 -g '*.md' -g '*.kt'` produced no matches after the fix.
- `./gradlew :bluetape4k-jackson2:compileTestKotlin :bluetape4k-jackson3:compileTestKotlin :bluetape4k-tink:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.crypto.JsonTinkEncryptTest" :bluetape4k-jackson3:test --tests "io.bluetape4k.jackson3.crypto.JsonTinkEncryptTest" :bluetape4k-tink:test --tests "io.bluetape4k.tink.encrypt.TinkEncryptorTest" --no-daemon --no-configuration-cache`
- `git diff --check`

## Future Guard

Do not document process-local singleton encryptors as durable database storage helpers. If Jackson field encryption needs durable searchable storage later, add an explicit keyset/provider-backed API and tests that prove ciphertext survives keyset reload and remains searchable for deterministic fields.

## Concurrency Helper Gate

No new concurrency helper was needed because the change is documentation/KDoc only. Existing Jackson field-encryption tests already cover multithreading, coroutine suspend job, and virtual-thread execution and passed during this work.
