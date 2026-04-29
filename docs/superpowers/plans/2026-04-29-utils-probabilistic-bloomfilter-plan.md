# utils/probabilistic Bloom Filter 구현 계획

Spec: `docs/superpowers/specs/2026-04-29-utils-probabilistic-bloomfilter-design.md`
Issue: #142

## T1. 신규 모듈 골격 추가

- complexity: medium
- expected files:
  - `utils/probabilistic/build.gradle.kts`
  - `utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/...`
  - `utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/...`
- work:
  - `:probabilistic` 자동 include 구조에 맞춰 `utils/probabilistic` 생성
  - core, coroutines compileOnly/test 의존성 추가
  - Guava/Eclipse Collections 의존성은 추가하지 않음
- verification:
  - `./bin/repo-test-summary -- ./gradlew :probabilistic:compileKotlin`
- docs impact:
  - 신규 모듈 README 필요
  - 신규 durable convention은 기존 repo 규칙으로 충분해 AGENTS.md 추가는 불필요

## T2. Bloom Filter 공개 계약과 생성 DSL 구현

- complexity: high
- expected files:
  - `BloomFilter.kt`
  - `MutableBloomFilter.kt`
  - `SuspendBloomFilter.kt`
  - `BloomFilterConfig.kt`
  - `BloomFilters.kt`
  - `BloomHasher.kt`
- work:
  - `expectedInsertions`, `falsePositiveProbability`, `bitSize`, `hashFunctionCount`, `mightContain`, `put`, `approximateElementCount`, `expectedFpp`, `clear` 계약 정의
  - `bloomFilter`, `mutableBloomFilter`, `suspendBloomFilter` DSL 구현
  - `expectedInsertions > 0`, `fpp in (0, 1)` 검증
  - 기본 hash 전략 작성
- verification:
  - `./bin/repo-test-summary -- ./gradlew :probabilistic:compileKotlin`
- docs impact:
  - 공개 API KDoc 한국어 작성

## T3. JDK/Kotlin 기반 인메모리 구현 작성

- complexity: high
- expected files:
  - `bloomfilter/InMemoryBloomFilter.kt`
  - `bloomfilter/InMemoryMutableBloomFilter.kt`
  - `bloomfilter/InMemorySuspendBloomFilter.kt`
  - `bloomfilter/BloomFilterMath.kt`
- work:
  - `LongArray` bitset 구현
  - SHA-256 double hashing offset 생성
  - `putAll` 호환성 검증 후 bitset 병합
  - suspend wrapper는 dispatcher 전환 없이 메모리 연산만 위임
- verification:
  - `./bin/repo-test-summary -- ./gradlew :probabilistic:compileKotlin :probabilistic:compileTestKotlin`
- docs impact:
  - README에 `put` 반환값, FPP, `putAll` compatibility, thread-safe 비보장, suspend 제약 기록

## T4. 테스트 작성

- complexity: medium
- expected files:
  - `BloomFilterConfigTest.kt`
  - `InMemoryBloomFilterTest.kt`
  - `InMemorySuspendBloomFilterTest.kt`
- work:
  - 입력 검증 회귀 테스트
  - bit size/hash count 계산 검증
  - 삽입 원소 false negative 없음 검증
  - 관측 FPP가 설정값을 과도하게 넘지 않는지 검증
  - `putAll` 성공/실패 계약 검증
  - `runTest` suspend API 검증
- verification:
  - `./bin/repo-test-summary -- ./gradlew :probabilistic:test`
- docs impact:
  - 테스트명은 한글/계약 중심으로 작성

## T5. README 작성

- complexity: low
- expected files:
  - `utils/probabilistic/README.md`
  - `utils/probabilistic/README.ko.md`
- work:
  - 설치/의존성, DSL 예시, suspend 예시, FPP/제한 사항 문서화
  - Redis 구현은 `infra/lettuce`를 사용하라고 안내
  - Guava/Eclipse Collections 미사용을 명시
- verification:
  - Markdown 링크/코드 블록 육안 검토
- docs impact:
  - README.md / README.ko.md 신규 작성

## T6. 검증, 리뷰, 커밋/PR

- complexity: medium
- expected files:
  - spec/plan
  - implementation/docs/test files
- work:
  - targeted compile/test 실행
  - Step 4-S/4-P 조건 판단 및 필요 시 cleanup/perf scan
  - Step 5 verifier 체크
  - Step 6-R 6개 리뷰 티어 수행
  - Lore trailer 포함 커밋, push, PR 생성
- verification:
  - `./bin/repo-test-summary -- ./gradlew :probabilistic:test`
  - `./bin/repo-test-summary -- ./gradlew :probabilistic:compileKotlin :probabilistic:compileTestKotlin`
- docs impact:
  - `docs/superpowers/index`는 현 repo에 없음. superpowers index 업데이트는 N/A로 기록

## Pre-Implementation Risk Mitigations

- Guava/Eclipse Collections 의존성을 추가하지 않는다.
- `put` 반환값을 "신규 확정"으로 문서화하지 않는다.
- `putAll`은 직접 구현의 config/hash compatibility 체크 후 수행한다.
- FPP 테스트는 큰 1M 샘플 대신 deterministic 10k/20k 규모로 구성해 CI 시간을 제한한다.
- SHA-256 `MessageDigest`는 ThreadLocal로 재사용해 hot-path 객체 생성을 줄인다.
- suspend API는 메모리 연산임을 KDoc/README에 반복 기록한다.
