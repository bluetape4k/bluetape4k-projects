# Issue 807 - Elasticsearch PIT Cleanup Review

## Scope

- `infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/SearchApiCoroutines.kt`
- `infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/SearchApiCoroutinesUnitTest.kt`
- `infra/elasticsearch/README.md`
- `infra/elasticsearch/README.ko.md`

## 7-Tier Review

| Tier | Result | Evidence |
|------|--------|----------|
| Correctness | PASS | `searchAsFlow` now closes PIT through `closePointInTimeBestEffort` from `finally`. |
| Cancellation safety | PASS | PIT close runs inside `withContext(NonCancellable)` so collector cancellation does not skip suspend cleanup. |
| Failure propagation | PASS | Cleanup failures are logged and swallowed so the original collector cancellation or upstream error can continue. |
| Regression coverage | PASS | Unit test cancels a running coroutine and proves the suspend cleanup block still completes. |
| Integration coverage | PASS | Full `bluetape4k-elasticsearch` Testcontainers suite passes with 33 tests. |
| Documentation | PASS | README and README.ko document non-cancellable PIT cleanup behavior. |
| Kotlin style | PASS | Validation uses `requireNotBlank`; tests use `runTest` and bluetape4k boolean assertion. |
| Impact radius | PASS | CodeGraph review context reports low risk, 0 impacted files, and 0 test gaps for changed Kotlin files. |

## Findings

- P0: none.
- P1: none.

## Verification

- PASS: `./gradlew :bluetape4k-elasticsearch:compileKotlin :bluetape4k-elasticsearch:compileTestKotlin :bluetape4k-elasticsearch:test --tests 'io.bluetape4k.elasticsearch.coroutines.SearchApiCoroutinesUnitTest' --no-build-cache --no-configuration-cache`
- PASS: `./gradlew :bluetape4k-elasticsearch:test --tests 'io.bluetape4k.elasticsearch.coroutines.SearchApiCoroutinesTest' --no-build-cache --no-configuration-cache`
- PASS: `./gradlew :bluetape4k-elasticsearch:compileKotlin :bluetape4k-elasticsearch:compileTestKotlin :bluetape4k-elasticsearch:test :bluetape4k-elasticsearch:koverXmlReport --no-build-cache --no-configuration-cache`
- PASS: `infra/elasticsearch` test result XML summary: 7 suites, 33 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `git diff --check`
- PASS: CodeGraph `get_review_context` for changed Kotlin files: low risk, 0 impacted files, 0 test gaps.
