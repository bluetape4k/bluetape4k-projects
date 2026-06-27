# Issue 791 Review: Jackson Tink durable-search guidance

## Scope

- Jackson2 `@JsonTinkEncrypt` README and KDoc
- Jackson3 `@JsonTinkEncrypt` README and KDoc
- `TinkEncryptors` README and KDoc wording for singleton keysets
- Durable storage guidance toward `TinkDaeads.versioned(store)` and `VersionedKeysetStore`

## Findings

No P0/P1 findings.

## Checks

- Jackson2 and Jackson3 no longer describe `@JsonTinkEncrypt(DETERMINISTIC_AES256_SIV)` as DB-search-capable durable storage.
- `TinkEncryptAlgorithm` KDoc now explains that enum values resolve to process-local singleton encryptors.
- `JsonTinkEncrypt` KDoc warns against using the annotation for durable encrypted database columns or searchable indexes.
- Tink README guidance distinguishes durable searchable DB columns from process-local singleton deterministic equality.
- `rg` over Jackson2/Jackson3 found no remaining `DB search`, `DB 검색`, or `searchable in DB` claims.

## Verification Evidence

- Red evidence before implementation: `rg` found Jackson2/Jackson3 README and KDoc claims that deterministic field encryption was suitable for DB search.
- `:bluetape4k-jackson2:compileTestKotlin :bluetape4k-jackson3:compileTestKotlin :bluetape4k-tink:compileTestKotlin --warning-mode all --rerun-tasks`: passed; only existing root Gradle Kotlin DSL deprecation warnings were reported.
- Targeted field-encryption tests passed: Jackson2 `JsonTinkEncryptTest` 14 passing, Jackson3 `JsonTinkEncryptTest` 14 passing, Tink `TinkEncryptorTest` 22 passing.
- `git diff --check`: passed.
- CodeGraph `detect_changes`: 11 changed files, 0 changed functions/classes, 0 affected flows, 0 test gaps, risk score 0.00.

## Residual Risk

This patch does not add keyset/provider-backed Jackson field encryption. That keeps the behavioral surface stable and satisfies the issue's documentation-safe acceptance path, but callers that need durable searchable JSON-field encryption still need a dedicated persisted-keyset integration rather than `@JsonTinkEncrypt`.

## Concurrency Helper Gate

No new `MultithreadingTester`, `SuspendedJobTester`, or `StructuredTaskScopeTester` coverage was added because this patch changes documentation and KDoc only. Existing Jackson2/Jackson3 `JsonTinkEncryptTest` coverage already exercises multithreaded, coroutine suspend job, and virtual-thread paths and passed unchanged.
