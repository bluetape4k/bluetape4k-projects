# Korean Docs And KDoc Localization inventory

Issue: #1093

## 범위 Rules

- In scope: Git-tracked non-README, single-language documentation and Kotlin/KTS KDoc surfaces.
- Korean rewrite target: prose in in-scope docs, public/internal KDoc, and meaningful internal/data-class property contracts.
- Preserve exactly: code identifiers, API names, commands, URLs, exact error text, external product names, issue/PR numbers, and measured values.
- Excluded from rewrite: README files, LLM-facing operating guidance, generated workflow state, CHANGELOG, SECURITY, GitHub metadata, release notes, and pushed commit text.
- Parity-only: `bluetape4k/bluetape4k.github.io`의
  `docs/manual/bluetape4k-projects/en`과 `ko` bilingual pair.

## 현재 inventory

- Central manual source: `bluetape4k/bluetape4k.github.io`
- Central manual root: `docs/manual/bluetape4k-projects`
- Central manual ref: `4e3c00262adb12cd61e4e8a30b6488aa6a287acc`
- Git-tracked files scanned: 7233
- In-scope single-language docs: 879
- Bilingual manual parity-only docs: 516
- Excluded docs: 214
- Kotlin/KTS files for KDoc follow-up: 4613
- KDoc blocks found in Kotlin/KTS files: 34756
- Manual EN files missing KO pair: 0
- Manual KO files missing EN pair: 0
- English-KDoc policy drift findings: 0

아래 bucket 표는 `#1093` 당시 repository-local manual이 존재하던 historical baseline이다.
현재 CI guardrail은 위 central manual ref를 기준으로 parity를 검증한다.

### Document Classification

| Bucket | Count |
|---|---:|
| `in-scope-doc` | 728 |
| `manual-pair-parity-only` | 508 |
| `excluded-readme` | 194 |
| `excluded-operating` | 12 |
| `excluded-public-english` | 2 |

### In-Scope Document Buckets

| Bucket | Count |
|---|---:|
| `docs/lessons` | 265 |
| `docs/superpowers` | 265 |
| `docs/review` | 97 |
| `docs/images` | 24 |
| `docs/benchmarks` | 23 |
| `infra` | 8 |
| `io` | 8 |
| `docs/security-review` | 5 |
| `scripts` | 5 |
| `cache` | 4 |
| `docs/manual` | 3 |
| `utils` | 3 |
| `bluetape4k` | 2 |
| `docs/discussion` | 2 |
| `root` | 1 |
| `docs/cache` | 1 |
| `docs/design` | 1 |
| `docs/evidence` | 1 |
| `docs/followup-issues` | 1 |
| `docs/governance` | 1 |
| `docs/infra-deprecated-inventory.md` | 1 |
| `docs/localization` | 2 |
| `docs/operations` | 1 |
| `docs/process` | 1 |
| `docs/release` | 1 |
| `docs/security` | 1 |
| `docs/windy-dev.md` | 1 |

### Excluded Document Buckets

| Bucket | Count |
|---|---:|
| `excluded-readme` | 194 |
| `excluded-operating` | 12 |
| `excluded-public-english` | 2 |

### Kotlin/KTS Buckets

| Bucket | Count |
|---|---:|
| `bluetape4k/core` | 321 |
| `infra/lettuce` | 211 |
| `bluetape4k/coroutines` | 189 |
| `io/http` | 176 |
| `utils/math` | 169 |
| `testing/testcontainers` | 162 |
| `utils/javatimes` | 159 |
| `data/hibernate` | 149 |
| `io/io` | 120 |
| `utils/rule-engine` | 116 |
| `cache/cache-core` | 110 |
| `io/okio` | 104 |
| `spring-boot/cassandra-demo` | 92 |
| `testing/junit5` | 92 |
| `utils/idgenerators` | 88 |
| `io/jackson3` | 76 |
| `io/jackson2` | 75 |
| `infra/nats` | 71 |
| `io/csv` | 69 |
| `spring-boot/cassandra` | 68 |
| `infra/resilience4j` | 67 |
| `infra/kafka4` | 66 |
| `utils/geo` | 64 |
| `data/cassandra` | 63 |
| `spring-boot/core` | 63 |
| `infra/kafka` | 61 |
| `io/vertx` | 58 |
| `data/r2dbc` | 54 |
| `testing/mock-web-server` | 52 |
| `utils/science` | 51 |
| `io/feign` | 50 |
| `infra/redisson` | 49 |
| `io/retrofit2` | 49 |
| `utils/workflow` | 49 |
| `infra/bucket4j` | 47 |
| `testing/mock-webflux-server` | 47 |
| `io/tink` | 43 |
| `cache/cache-lettuce` | 41 |
| `examples/redisson-demo` | 39 |
| `utils/jwt` | 39 |
| `io/grpc` | 38 |
| `examples/coroutines-demo` | 36 |
| `infra/opentelemetry` | 36 |
| `data/jdbc` | 33 |
| `testing/assertions` | 33 |
| `utils/measured` | 32 |
| `infra/micrometer` | 30 |
| `cache/cache-hazelcast` | 29 |
| `io/netty` | 29 |
| `infra/kafka-logback` | 27 |
| `io/protobuf` | 27 |
| `utils/states` | 27 |
| `cache/hibernate-cache-lettuce` | 25 |
| `cache/cache-redisson` | 23 |
| `data/hibernate-reactive` | 23 |
| `infra/pulsar` | 22 |
| `io/avro` | 21 |
| `examples/jpa-querydsl-demo` | 20 |
| `spring-boot/r2dbc` | 20 |
| `infra/elasticsearch` | 18 |
| `benchmark` | 17 |
| `bluetape4k/logging` | 17 |
| `data/mongodb` | 16 |
| `examples/jpa-blazepersistence-demo` | 15 |
| `io/fastjson2` | 15 |
| `examples/virtualthreads-demo` | 14 |
| `spring-boot/mongodb` | 14 |
| `examples/spring-boot` | 13 |
| `utils/money` | 13 |
| `utils/probabilistic` | 13 |
| `io/json` | 12 |
| `ktor/observability` | 12 |
| `spring-boot/hibernate-lettuce` | 11 |
| `spring-boot/redis` | 11 |
| `ktor/core` | 10 |
| `bluetape4k/annotations` | 8 |
| `spring-boot/hibernate-lettuce-demo` | 8 |
| `virtualthread/api` | 8 |
| `ktor/testing` | 7 |
| `utils/mutiny` | 7 |
| `examples/ktor` | 6 |
| `buildSrc` | 5 |
| `ktor/resilience4j` | 5 |
| `virtualthread/jdk21` | 5 |
| `virtualthread/jdk25` | 5 |
| `ktor/openapi` | 3 |
| `bluetape4k/bom` | 1 |
| `build.gradle.kts` | 1 |
| `infra/redis` | 1 |
| `settings.gradle.kts` | 1 |

### Existing KDoc Blocks By Bucket

| Bucket | Count |
|---|---:|
| `bluetape4k/core` | 5378 |
| `testing/testcontainers` | 1707 |
| `utils/math` | 1404 |
| `bluetape4k/coroutines` | 1373 |
| `infra/lettuce` | 1138 |
| `utils/javatimes` | 1059 |
| `io/http` | 989 |
| `io/io` | 936 |
| `io/okio` | 868 |
| `data/hibernate` | 813 |
| `spring-boot/core` | 661 |
| `cache/cache-core` | 634 |
| `testing/mock-web-server` | 623 |
| `utils/science` | 616 |
| `utils/measured` | 600 |
| `io/csv` | 570 |
| `testing/assertions` | 543 |
| `testing/mock-webflux-server` | 526 |
| `testing/junit5` | 514 |
| `infra/resilience4j` | 486 |
| `infra/nats` | 465 |
| `cache/cache-lettuce` | 455 |
| `utils/geo` | 445 |
| `io/jackson3` | 443 |
| `utils/idgenerators` | 440 |
| `spring-boot/cassandra` | 438 |
| `io/jackson2` | 432 |
| `examples/redisson-demo` | 419 |
| `io/vertx` | 414 |
| `infra/kafka4` | 400 |
| `utils/rule-engine` | 393 |
| `data/jdbc` | 372 |
| `data/r2dbc` | 365 |
| `data/hibernate-reactive` | 361 |
| `infra/redisson` | 361 |
| `data/cassandra` | 356 |
| `utils/workflow` | 351 |
| `infra/kafka` | 337 |
| `io/netty` | 332 |
| `io/feign` | 323 |
| `spring-boot/mongodb` | 320 |
| `io/protobuf` | 277 |
| `utils/jwt` | 276 |
| `infra/elasticsearch` | 267 |
| `infra/opentelemetry` | 255 |
| `io/retrofit2` | 238 |
| `examples/coroutines-demo` | 235 |
| `cache/cache-hazelcast` | 194 |
| `infra/micrometer` | 192 |
| `virtualthread/api` | 180 |
| `io/avro` | 174 |
| `cache/cache-redisson` | 173 |
| `infra/bucket4j` | 172 |
| `utils/money` | 149 |
| `io/tink` | 143 |
| `io/grpc` | 127 |
| `data/mongodb` | 102 |
| `examples/jpa-querydsl-demo` | 99 |
| `bluetape4k/logging` | 97 |
| `cache/hibernate-cache-lettuce` | 94 |
| `io/fastjson2` | 91 |
| `utils/mutiny` | 91 |
| `build.gradle.kts` | 90 |
| `utils/states` | 88 |
| `spring-boot/cassandra-demo` | 81 |
| `spring-boot/hibernate-lettuce` | 78 |
| `spring-boot/r2dbc` | 70 |
| `infra/pulsar` | 66 |
| `infra/kafka-logback` | 64 |
| `examples/spring-boot` | 61 |
| `examples/ktor` | 46 |
| `benchmark` | 43 |
| `io/json` | 42 |
| `utils/probabilistic` | 41 |
| `buildSrc` | 37 |
| `ktor/observability` | 33 |
| `examples/virtualthreads-demo` | 29 |
| `virtualthread/jdk25` | 28 |
| `ktor/core` | 25 |
| `spring-boot/hibernate-lettuce-demo` | 24 |
| `spring-boot/redis` | 22 |
| `ktor/testing` | 20 |
| `virtualthread/jdk21` | 20 |
| `settings.gradle.kts` | 18 |
| `examples/jpa-blazepersistence-demo` | 13 |
| `ktor/openapi` | 13 |
| `ktor/resilience4j` | 12 |
| `bluetape4k/annotations` | 11 |
| `infra/redis` | 1 |

### In-Scope Document Sample

- `WIP.md`
- `bluetape4k/core/src/test/resources/files/Utf8Samples.txt`
- `bluetape4k/coroutines/2026-04-21-self-improve.md`
- `cache/cache-lettuce/Benchmark.ko.md`
- `cache/cache-lettuce/Benchmark.md`
- `cache/cache-lettuce/src/Archicture.md`
- `cache/cache-redisson/Archicture.md`
- `docs/benchmarks/2026-04-25-fory-fast-codec-benchmark.md`
- `docs/benchmarks/2026-05-21-io-http-client-benchmark.md`
- `docs/benchmarks/2026-05-29-web-framework-benchmark.md`
- `docs/benchmarks/2026-06-11-idgenerators-self-improve-benchmark.md`
- `docs/benchmarks/2026-06-11-io-same-condition-compressor-benchmark.md`
- `docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md`
- `docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md`
- `docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md`
- `docs/benchmarks/2026-07-22-issue-756-lettuce-buffer-codec-allocation.md`
- `docs/benchmarks/2026-07-23-issue-756-fory-codec-followup.md`
- `docs/benchmarks/raw/issue-1039/run-20260718T030512Z/environment.txt`
- `docs/benchmarks/raw/issue-1039/run-20260718T031704Z/environment.txt`
- `docs/benchmarks/raw/issue-738/baseline-environment.txt`
- `docs/benchmarks/raw/issue-738/baseline-focused.txt`
- `docs/benchmarks/raw/issue-738/baseline-snowflake-profile.txt`
- `docs/benchmarks/raw/issue-738/candidate-1-focused.txt`
- `docs/benchmarks/raw/issue-738/candidate-2-focused.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-focused.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-ksuidMillisDefault-repeat.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-snowflake-profile.txt`
- `docs/benchmarks/raw/issue-738/focused-comparison-candidate-3.md`
- `docs/benchmarks/raw/issue-758/run-20260718T204256Z/environment.txt`
- `docs/benchmarks/raw/issue-758/run-20260718T204443Z/environment.txt`
- `docs/cache/near-cache-capability-matrix.md`
- `docs/design/2026-05-24-hc5-first-http-client-recommendation.md`
- `docs/discussion/2026-04-19-token-saving-tools-evaluation.md`
- `docs/discussion/2026-04-19-token-tools-evaluation.md`
- `docs/evidence/issue-754/json/fastjson2-2.0.62-capability.md`
- `docs/followup-issues/utils-science-readme-rewrite.md`
- `docs/governance/kover-coverage-policy.md`
- `docs/images/readme-charts/benchmark-before-after-chart-sample-01-summary.txt`
- `docs/images/readme-charts/benchmark-comparison-chart-sample-01-summary.txt`
- `docs/images/readme-charts/benchmark-protobuf-codec-throughput-chart-01-summary.txt`
- ... 686 more

### Manual Pair Missing KO

No missing Korean manual pairs were found.

### Manual Pair Missing EN

No missing English manual pairs were found.

### English-KDoc 정책 Drift

No English-KDoc policy drift was found in the tracked documentation scope.

## Follow-Up Partition

- #1094 owns repeatable guardrails based on this inventory.
- #1095-#1100 own documentation buckets and manual parity verification.
- #1101-#1108 own Kotlin KDoc buckets by module group.
- #1109 owns the final repository-wide audit after child PRs land.

## Reproduction

```bash
export BLUETAPE4K_MANUAL_ROOT="/path/to/bluetape4k.github.io/docs/manual/bluetape4k-projects"
export BLUETAPE4K_MANUAL_REF="$(git -C /path/to/bluetape4k.github.io rev-parse HEAD)"
python3 scripts/docs-localization-inventory.py --check
```
