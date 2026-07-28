# Code 검토 - Issue #880 Testcontainers Floci and MinIO policy

Date: 2026-06-23
Issue: #880
Module: `:bluetape4k-testcontainers`

## 요약

`MinIOServer` remains available for explicit MinIO compatibility tests without
class-level deprecation noise. `FlociServer.TAG` is pinned to the verified
`1.5.27` release tag, and README/KDoc guidance keeps new AWS/S3 emulator tests
on `FlociServer` or `MiniStackServer`.

## 발견 사항

- P0: 0
- P1: 0
- P2: 0
- P3: 0

## Independent 검토 Notes

- Code reviewer: PASS, no findings.
- Verifier: pre-commit readiness FAIL because the worktree was intentionally
  dirty before commit and the verifier did not independently replay the RED
  state before interruption. Implementation evidence had no P0 behavior blocker.
  The dirty-worktree concern is resolved by committing this branch before PR
  creation.

## RED Evidence

Baseline compile checks:

```text
./gradlew :bluetape4k-testcontainers:compileKotlin --warning-mode all --rerun-tasks
No MinIOServer deprecation warning reproduced; only Gradle deprecation warnings were filtered.

./gradlew :bluetape4k-testcontainers:compileTestKotlin --warning-mode all --rerun-tasks
No MinIOServer deprecation warning reproduced; LocalStack/Gradle deprecation warnings remained.
```

Contract RED:

```text
./gradlew :bluetape4k-testcontainers:test \
  --tests 'io.bluetape4k.testcontainers.aws.FlociServerTest.Floci server uses the current stable image tag' \
  --tests 'io.bluetape4k.testcontainers.storage.MinIOServerTest.MinIOServer remains available for explicit MinIO compatibility tests' \
  --no-build-cache

GRADLE_STATUS=1
FlociServerTest: Expected "1.5.17" to equal to "1.5.27"
MinIOServerTest: Expected @Deprecated(...) to be null
```

## GREEN Evidence

```text
./gradlew :bluetape4k-testcontainers:test \
  --tests 'io.bluetape4k.testcontainers.aws.FlociServerTest.Floci server uses the current stable image tag' \
  --tests 'io.bluetape4k.testcontainers.storage.MinIOServerTest.MinIOServer remains available for explicit MinIO compatibility tests' \
  --no-build-cache

2 passing, 0 failures
```

```text
./gradlew :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-testcontainers:compileTestKotlin \
  :bluetape4k-testcontainers:cleanTest \
  :bluetape4k-testcontainers:test \
  --tests '*FlociServerTest' \
  --tests '*MinIOServerTest' \
  --no-build-cache --warning-mode all

10 passing, 0 failures
```

```text
./gradlew :bluetape4k-testcontainers:compileKotlin --warning-mode all --rerun-tasks
BUILD SUCCESSFUL in 7s
MinIOServer warning filter: no MinIOServer, MinIO core, or old replacement warning matches
```

Static checks:

```text
git diff --check
clean
```

## Scope Notes

- Docker Hub `floci/floci` release tags were checked on 2026-06-23; latest plain `1.5.x` release tag was `1.5.27`.
- Gradle 10 deprecation cleanup remains out of scope.
- Full repository build was not run.
- Merge is intentionally out of scope for this PR per user instruction.
