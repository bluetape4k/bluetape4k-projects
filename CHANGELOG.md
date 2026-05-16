# Changelog

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [1.9.0] — Unreleased

### Added

### Changed

### Fixed

---

## [Unreleased]

### Added

- `bluetape4k-assertions` module was added as the project-local assertion library foundation ([#326](https://github.com/bluetape4k/bluetape4k-projects/pull/326)).
- Collection assertion helper `shouldNotContainAny` was added ([#342](https://github.com/bluetape4k/bluetape4k-projects/pull/342)).
- Case-insensitive string assertions `assertEqualsIgnoringCase` and `assertNotEqualsIgnoringCase` were added ([#343](https://github.com/bluetape4k/bluetape4k-projects/pull/343)).
- `bluetape4k-bom` module README files were added in English and Korean ([#346](https://github.com/bluetape4k/bluetape4k-projects/pull/346)).
- `infra/` deprecated API inventory and follow-up cleanup PR split were documented ([#349](https://github.com/bluetape4k/bluetape4k-projects/pull/349), [#110](https://github.com/bluetape4k/bluetape4k-projects/issues/110)).
- Spring `AnnotatedElementUtils` / `AnnotationUtils` Kotlin reified helper APIs were added ([#365](https://github.com/bluetape4k/bluetape4k-projects/pull/365)).
- Unsigned range assertion coverage and helpers were added to the assertion DSL ([#360](https://github.com/bluetape4k/bluetape4k-projects/pull/360)).
- `bluetape4k-idgenerators` examples were added for Ktor and Spring Boot 4, with dedicated REST endpoints and tests ([#421](https://github.com/bluetape4k/bluetape4k-projects/pull/421), [#422](https://github.com/bluetape4k/bluetape4k-projects/pull/422)).
- Governance and agent guidance docs were expanded for lessons capture, Kover coverage policy, dependency governance, and qmd knowledge retrieval ([#370](https://github.com/bluetape4k/bluetape4k-projects/pull/370), [#373](https://github.com/bluetape4k/bluetape4k-projects/pull/373), [#377](https://github.com/bluetape4k/bluetape4k-projects/pull/377), [#398](https://github.com/bluetape4k/bluetape4k-projects/pull/398)).
- `bluetape4k-bucket4j` `RateLimitDiagnostics`, `RateLimitRejectionReason`, and rejected-result `retryAfter` were added to make retry and rejection reporting independent from Bucket4j probe types ([#434](https://github.com/bluetape4k/bluetape4k-projects/issues/434)).

### Changed

- Cache modules were reorganized under `cache/` while preserving existing Gradle project names, Maven artifact IDs, and Kotlin packages; users do not need to change `io.bluetape4k.cache.*` imports for the folder move ([#350](https://github.com/bluetape4k/bluetape4k-projects/pull/350), [#354](https://github.com/bluetape4k/bluetape4k-projects/issues/354)).
- Spring Boot 4 became the standard Spring Boot line, with migration follow-up fixes and versionless `spring-boot/*` modules ([#348](https://github.com/bluetape4k/bluetape4k-projects/pull/348), [#351](https://github.com/bluetape4k/bluetape4k-projects/pull/351)).
- Gradle catalog의 Spring Boot/Spring Cloud BOM alias를 `spring.boot.dependencies`와 `spring.cloud.dependencies`로 단일화했습니다.
- Nightly and Examples workflows were split and tuned for tiered Testcontainers, Docker shard timeouts, Memgraph images, Kover report behavior, Spring Boot 4-only checks, and graphdb retry policy ([#355](https://github.com/bluetape4k/bluetape4k-projects/pull/355), [#356](https://github.com/bluetape4k/bluetape4k-projects/pull/356), [#357](https://github.com/bluetape4k/bluetape4k-projects/pull/357), [#358](https://github.com/bluetape4k/bluetape4k-projects/pull/358), [#363](https://github.com/bluetape4k/bluetape4k-projects/pull/363), [#366](https://github.com/bluetape4k/bluetape4k-projects/pull/366), [#367](https://github.com/bluetape4k/bluetape4k-projects/pull/367), [#413](https://github.com/bluetape4k/bluetape4k-projects/pull/413), [#414](https://github.com/bluetape4k/bluetape4k-projects/pull/414), [#423](https://github.com/bluetape4k/bluetape4k-projects/pull/423), [#424](https://github.com/bluetape4k/bluetape4k-projects/pull/424)).
- Test code migrated from bluetape4k-assertions to `bluetape4k-assertions`, including `assertFailsWith<T>` / `assertNotFailsWith<T>` migration paths ([#328](https://github.com/bluetape4k/bluetape4k-projects/pull/328), [#330](https://github.com/bluetape4k/bluetape4k-projects/pull/330), [#345](https://github.com/bluetape4k/bluetape4k-projects/pull/345)).
- CI now uses paths-filter so only affected module test jobs run where possible ([#344](https://github.com/bluetape4k/bluetape4k-projects/pull/344)).
- Exposed ORM modules were extracted to the standalone `bluetape4k-exposed` repository ([#334](https://github.com/bluetape4k/bluetape4k-projects/pull/334)).
- AWS, images, texts, leader, and JaVers split cleanup continued by removing moved directories and updating follow-up docs ([#319](https://github.com/bluetape4k/bluetape4k-projects/pull/319), [#320](https://github.com/bluetape4k/bluetape4k-projects/pull/320), [#321](https://github.com/bluetape4k/bluetape4k-projects/pull/321), [#336](https://github.com/bluetape4k/bluetape4k-projects/pull/336), [#338](https://github.com/bluetape4k/bluetape4k-projects/pull/338)).
- Codex/agent guidance now points at Codex resources ([#347](https://github.com/bluetape4k/bluetape4k-projects/pull/347)).
- Dependabot assignment and dependency compatibility guard policies were adjusted, including Redis client major-version guards and compatibility-line dependency restoration ([#379](https://github.com/bluetape4k/bluetape4k-projects/pull/379), [#395](https://github.com/bluetape4k/bluetape4k-projects/pull/395), [#399](https://github.com/bluetape4k/bluetape4k-projects/pull/399), [#411](https://github.com/bluetape4k/bluetape4k-projects/pull/411), [#412](https://github.com/bluetape4k/bluetape4k-projects/pull/412)).
- Dependency baselines were refreshed across build plugins, GitHub Actions, Spring, Pulsar, Hibernate, GeoTools, Protobuf, OpenTelemetry instrumentation, and other library groups ([#378](https://github.com/bluetape4k/bluetape4k-projects/pull/378), [#382](https://github.com/bluetape4k/bluetape4k-projects/pull/382), [#383](https://github.com/bluetape4k/bluetape4k-projects/pull/383), [#384](https://github.com/bluetape4k/bluetape4k-projects/pull/384), [#386](https://github.com/bluetape4k/bluetape4k-projects/pull/386), [#387](https://github.com/bluetape4k/bluetape4k-projects/pull/387), [#388](https://github.com/bluetape4k/bluetape4k-projects/pull/388), [#389](https://github.com/bluetape4k/bluetape4k-projects/pull/389), [#392](https://github.com/bluetape4k/bluetape4k-projects/pull/392), [#393](https://github.com/bluetape4k/bluetape4k-projects/pull/393), [#394](https://github.com/bluetape4k/bluetape4k-projects/pull/394), [#397](https://github.com/bluetape4k/bluetape4k-projects/pull/397), [#401](https://github.com/bluetape4k/bluetape4k-projects/pull/401), [#402](https://github.com/bluetape4k/bluetape4k-projects/pull/402), [#403](https://github.com/bluetape4k/bluetape4k-projects/pull/403), [#404](https://github.com/bluetape4k/bluetape4k-projects/pull/404), [#405](https://github.com/bluetape4k/bluetape4k-projects/pull/405), [#407](https://github.com/bluetape4k/bluetape4k-projects/pull/407), [#408](https://github.com/bluetape4k/bluetape4k-projects/pull/408), [#409](https://github.com/bluetape4k/bluetape4k-projects/pull/409)).
- WIP and README image documentation were refreshed after the idgenerator example lane completed ([#417](https://github.com/bluetape4k/bluetape4k-projects/pull/417), [#425](https://github.com/bluetape4k/bluetape4k-projects/pull/425), [#429](https://github.com/bluetape4k/bluetape4k-projects/pull/429)).
- `bluetape4k-bucket4j` now validates prefixed bucket keys at 512 bytes, documents provider lifecycle/expiration ownership, supports distributed coroutine operation timeouts while retaining the Java one-argument constructor overload, and preserves identified-bandwidth configuration replacement behavior ([#434](https://github.com/bluetape4k/bluetape4k-projects/issues/434)).

### Breaking Changes

- `RateLimitResult(consumedTokens, availableTokens)` was removed. Use `RateLimitResult.consumed(...)` or `RateLimitResult.rejected(...)` and read the new `diagnostics` / `retryAfter` fields for retry guidance ([#434](https://github.com/bluetape4k/bluetape4k-projects/issues/434)).
- `RateLimitResult` primary constructor, `copy(...)`, and `componentN()` shape changed because `diagnostics` was added. Recompile Kotlin callers and update Java callers that construct or destructure the value type directly ([#434](https://github.com/bluetape4k/bluetape4k-projects/issues/434)).
- `AbstractCompressor.compress()` and `AbstractCompressor.decompress()` now propagate compression/decompression failures instead of returning `emptyByteArray` for failed non-empty input ([#317](https://github.com/bluetape4k/bluetape4k-projects/pull/317), [#325](https://github.com/bluetape4k/bluetape4k-projects/issues/325)).

#### Migration

| Previous expectation | Replacement |
| --- | --- |
| `RateLimitResult(consumedTokens, availableTokens)` | `RateLimitResult.consumed(consumedTokens, availableTokens)` or `RateLimitResult.rejected(availableTokens)` |
| Inspect Bucket4j probe types for retry delay | Use `RateLimitResult.retryAfter` or `result.diagnostics.nanosToWaitForRefill` |
| Distributed suspend calls can wait indefinitely on the async store | Use `DistributedSuspendRateLimiter(provider, defaultTimeout = ...)` or `consume(key, tokens, timeout)` |
| Failed `compress()` returns `emptyByteArray` | Use `compressOrNull()` when failures should return `null`, or catch the propagated exception from `compress()`. |
| Failed `decompress()` returns `emptyByteArray` | Use `decompressOrNull()` when corrupt input should return `null`, or catch the propagated exception from `decompress()`. |
| Corrupt compressed data is ignored | Use `decompressOrNull()` for nullable recovery, or catch the propagated exception from `decompress()`. |

### Fixed

- `AbstractCompressor.compress/decompress` no longer swallows exceptions; this behavior change is documented as a breaking change in this release ([#317](https://github.com/bluetape4k/bluetape4k-projects/pull/317)).
- `runSuspendIO` timeout was increased and `BluetapeHttpServer` eager initialization was fixed ([#337](https://github.com/bluetape4k/bluetape4k-projects/pull/337)).
- Kafka Wave 1-3 security, cancellation, DLT, and metrics follow-ups were applied ([#309](https://github.com/bluetape4k/bluetape4k-projects/pull/309), [#310](https://github.com/bluetape4k/bluetape4k-projects/pull/310), [#314](https://github.com/bluetape4k/bluetape4k-projects/pull/314)).
- Code scanning findings for workflow permissions, archive extraction, and secure cookie handling were addressed across multiple follow-up PRs ([#311](https://github.com/bluetape4k/bluetape4k-projects/pull/311), [#312](https://github.com/bluetape4k/bluetape4k-projects/pull/312), [#313](https://github.com/bluetape4k/bluetape4k-projects/pull/313), [#286](https://github.com/bluetape4k/bluetape4k-projects/pull/286), [#287](https://github.com/bluetape4k/bluetape4k-projects/pull/287), [#288](https://github.com/bluetape4k/bluetape4k-projects/pull/288)).
- Core bounded collections, string collection assertions, coroutines/IO helpers, assertions/JUnit boundaries, Lettuce/Redisson/Kafka/Kafka4 async contracts, Okio suspended sources, and Tink keyset rotation received regression hardening and documentation updates ([#352](https://github.com/bluetape4k/bluetape4k-projects/pull/352), [#368](https://github.com/bluetape4k/bluetape4k-projects/pull/368), [#369](https://github.com/bluetape4k/bluetape4k-projects/pull/369), [#371](https://github.com/bluetape4k/bluetape4k-projects/pull/371), [#372](https://github.com/bluetape4k/bluetape4k-projects/pull/372), [#374](https://github.com/bluetape4k/bluetape4k-projects/pull/374), [#375](https://github.com/bluetape4k/bluetape4k-projects/pull/375), [#376](https://github.com/bluetape4k/bluetape4k-projects/pull/376), [#380](https://github.com/bluetape4k/bluetape4k-projects/pull/380), [#381](https://github.com/bluetape4k/bluetape4k-projects/pull/381)).
- JUnit 5 runtime helpers were kept consumable for downstream test modules ([#361](https://github.com/bluetape4k/bluetape4k-projects/pull/361)).
- Daily regression fixes restored Spring Boot, nightly, and compatibility behavior after the Boot 4 and workflow migration waves ([#353](https://github.com/bluetape4k/bluetape4k-projects/pull/353), [#359](https://github.com/bluetape4k/bluetape4k-projects/pull/359), [#362](https://github.com/bluetape4k/bluetape4k-projects/pull/362)).
- Jackson 2/3 streaming EOF contracts were hardened ([#390](https://github.com/bluetape4k/bluetape4k-projects/pull/390), [#396](https://github.com/bluetape4k/bluetape4k-projects/pull/396)).
- Cache core, cache-lettuce, and cache-redisson suspend lifecycle and recovery contracts were hardened ([#400](https://github.com/bluetape4k/bluetape4k-projects/pull/400), [#406](https://github.com/bluetape4k/bluetape4k-projects/pull/406), [#410](https://github.com/bluetape4k/bluetape4k-projects/pull/410)).
- Redisson `RFuture` coroutine adapter dispatcher lookup was restored ([#415](https://github.com/bluetape4k/bluetape4k-projects/pull/415)).
- DAEAD chunk framing was hardened with authenticated v2 frames, and virtual-thread coverage/lifecycle tests were stabilized ([#427](https://github.com/bluetape4k/bluetape4k-projects/pull/427), [#428](https://github.com/bluetape4k/bluetape4k-projects/pull/428)).

---

## [1.8.0] — 2026-05-17

### Added

#### images/** — libvips 고성능 이미지 처리 신규 모듈 그룹 ([#236](https://github.com/bluetape4k/bluetape4k-projects/pull/236), [#136](https://github.com/bluetape4k/bluetape4k-projects/issues/136))

scrimage(Java2D) 기반 `bluetape4k-images` 대비 메모리 1/10, 처리 속도 4~10× 향상. libvips demand-driven pipeline 기반.

**신규 모듈 3종**

- `bluetape4k-images-vips-api` — 바인딩 중립 인터페이스: `VipsImage`, `VipsRuntime`, `VipsEncodeOptions`, 예외 계층 (`VipsDecodeException`/`VipsOperationException`/`VipsEncodeException`)
- `bluetape4k-images-vips-java21` — JVips/JNI 바인딩 구현체 (Java 21+): `JVipsRuntime`, `JVipsImage`, `NativeHandle` Cleaner leak guard
- `bluetape4k-images-vips-java25` — vips-ffm/FFM API 바인딩 구현체 (Java 25+): `FfmVipsRuntime`, `FfmVipsImage`, `Arena.ofShared()` lifecycle

**주요 기능**

- `vipsImageOf(File|Path|ByteArray|InputStream)` / `suspendVipsImageOf(...)` 팩토리 함수
- `resize()` / `thumbnail()` / `crop()` / `toBytes()` / `writeTo()` 완전 구현
- JPEG/PNG/WebP 인코딩 (quality/lossless/effort 옵션)
- 보안: 포맷 허용 목록(JPEG/PNG/WebP magic byte), 50MB 입력 상한, `maxPixels` 픽셀 수 제한
- `VipsRuntime` 4-상태 CAS (`UNINITIALIZED→INITIALIZING→INITIALIZED→SHUTDOWN`), spin-wait 동시성 안전

**의존성**

- `com.criteo:jvips:8.12.2-69bf715` (java21 모듈)
- `app.photofox.vips-ffm:vips-ffm-core:1.9.6` (java25 모듈)

---

#### utils/images — 이미지 배치 플로우 처리 경로 추가 ([#234](https://github.com/bluetape4k/bluetape4k-projects/pull/234))

`bluetape4k-images` 모듈에 Flow 기반 배치 이미지 처리 파이프라인을 추가했습니다.

- `ImageBatchProcessor` — `Flow<ByteArray>` 기반 병렬 이미지 처리 파이프라인
- `ImageProcessingResult` — sealed (`Success`/`Failure`) 처리 결과 타입

---

#### infra/opentelemetry — Coroutines/Flow/WebFlux 트레이싱 API 추가 ([#214](https://github.com/bluetape4k/bluetape4k-projects/pull/214), [#150](https://github.com/bluetape4k/bluetape4k-projects/issues/150))

OpenTelemetry Java SDK를 Kotlin Coroutines / Flow / Spring WebFlux에서 사용하기 위한 래퍼를 추가했습니다.

- `withSpan {}` / `withSpanSuspend {}` — 코루틴 기반 span 생성 DSL
- `Flow<T>.traced()` — Flow 각 요소에 자동 span 부착 확장 함수
- WebFlux 요청 트레이싱 필터 및 Reactor Context 연동

---

#### testing/testcontainers — MiniStack AWS 에뮬레이터 서버 신규 ([#209](https://github.com/bluetape4k/bluetape4k-projects/pull/209))

경량 AWS 에뮬레이터 `ministackorg/ministack:1.3.14` 기반 `MiniStackServer`를 추가했습니다.

- `MiniStackServer` — S3/SQS/SNS/DynamoDB/Lambda 에뮬레이터 (단일 컨테이너)
- `AbstractMiniStackServiceTest` — 공통 테스트 기반 클래스

---

#### testing/testcontainers — FlociServer 기반 AWS 서비스 통합 테스트 추가 ([#207](https://github.com/bluetape4k/bluetape4k-projects/pull/207), [#202](https://github.com/bluetape4k/bluetape4k-projects/issues/202))

`FlociServer`를 활용한 S3/SQS/SNS/DynamoDB AWS 서비스 통합 테스트를 추가했습니다.

---

#### texts/** — 신규 모듈 그룹 (tokenizer/lingua/text-search 승격) ([#170](https://github.com/bluetape4k/bluetape4k-projects/issues/170))

`x-obsoleted/tokenizer` 및 `utils/` 하위 텍스트 처리 모듈을 `texts/**` 그룹으로 승격하였습니다.

**신규 모듈 그룹**

- `bluetape4k-tokenizer-core` — 토크나이저 공통 인터페이스 (`TokenizeRequest/Response`, `BlockwordRequest/Response`, `DictionaryProvider`)
- `bluetape4k-tokenizer-korean` — 한국어 형태소 분석기 (Open Korean Text 기반). **twitter-text 의존성 제거** — `TwitterCompatPatterns.kt` 인라인 정규식으로 대체
- `bluetape4k-tokenizer-japanese` — 일본어 형태소 분석기 (Kuromoji IPAdic 0.9.0)
- `bluetape4k-lingua` — 75+ 언어 감지 (Lingua 라이브러리 래퍼)
- `bluetape4k-text-search` — Aho-Corasick 다중 키워드 검색 (금칙어 필터/하이라이팅/Flow API)

**의존성 변경**

- `bluetape4k-tokenizer-korean`: `com.twitter.twittertext:twitter-text:3.1.0` 제거
- `TwitterCompatPatterns` 내부 구현: `VALID_URL` / `VALID_HASHTAG` / `VALID_MENTION_OR_LIST` / `VALID_CASHTAG` 4개 패턴

---

#### utils/science — NetCDF 지원 완성 (UCAR netCDF-Java 5.9.1) ([#127](https://github.com/bluetape4k/bluetape4k-projects/pull/127), [#107](https://github.com/bluetape4k/bluetape4k-projects/issues/107))

`bluetape4k-science` 모듈에 NetCDF 파일 임포트 파이프라인이 완성되었습니다.

**신규 클래스**

- `NetCdfException` — sealed 7종: `FileOpen` / `FileRecordNotFound` / `VariableNotFound` / `UnsupportedVariable` / `MissingCoordinate` / `UnsupportedProjection` / `ImportAlreadyRunning`
- `NetCdfCatalogService.registerFile()` / `importGridValues()` — UCAR cdm-core 5.9.1 기반 완전 구현
- `NetCdfImportProgressRepository` — heartbeat lease (`INSERT ON CONFLICT DO UPDATE WHERE RETURNING`)
- `service/internal/CoordinateReprojector` — sealed: Geographic 1D / Projected 2D pair
- `service/internal/VariableAxisMap` — AxisType + 이름 fallback 매핑

**스키마 변경**

- `NetCdfGridValueTable.location` → nullable (1D 시계열용)
- `NetCdfGridValueIndexes` — partial expression unique index 2종 (`MD5(ST_AsBinary(location))`)
- `NetCdfImportProgressTable` — `lastSliceIdx` 선형 커서 + `leaseExpiresAt` heartbeat

**CRS 재투영 (proj4j)**

- Geographic 1D: EPSG:4326 / EPSG:4269
- Projected 2D: EPSG:3857, UTM 32601~32760 (북) / 32701~32760 (남), EPSG:3413 / 3031 (Polar)

**Micrometer 5지표**

- `netcdf.register.duration` (Timer, status=success|failure)
- `netcdf.import.variable.records` (Counter, variable=)
- `netcdf.import.slice.duration` (Timer)
- `netcdf.import.nan.skipped` (Counter)
- `netcdf.import.status` (Counter, status=success|failure|resumed)

**의존성 변경**

- 제거: `edu.ucar:netcdfAll:5.6.0` (유령 아티팩트)
- 추가: `edu.ucar:cdm-core:5.9.1` + `edu.ucar:netcdf4:5.9.1` (compileOnly)
- 추가: `micrometer-core` + `guava` (compileOnly)

**CI**

- `ci.yml` `test-utils`에 `:bluetape4k-science:test -PexcludeTags=slow-netcdf` 추가
- `nightly-tests.yml` `test-utils`에 `:bluetape4k-science:test -PincludeTags=slow-netcdf` 추가

---

#### io/io + infra/redisson + infra/lettuce — FastFory Codec 추가 ([#122](https://github.com/bluetape4k/bluetape4k-projects/pull/122), [#113](https://github.com/bluetape4k/bluetape4k-projects/issues/113))

Fury `SCHEMA_CONSISTENT` + `refTracking=false` 모드 기반 고처리량 직렬화 Codec을 추가했습니다.

**io/io (BinarySerializers)**

- `FastFory` / `LZ4FastFory` / `ZstdFory` / `SnappyFory` / `GZipFory` lazy val 5종 추가
- `ForyBinarySerializer.fast()` — SCHEMA_CONSISTENT 모드 (~+70% throughput vs COMPATIBLE)
- `BinarySerializerBenchmark` FastFory 5종 `@Benchmark` 추가

**infra/redisson**

- `FastForyCodec` — Fory COMPATIBLE 바이트 fallback 읽기 지원
- `RedissonCodecs` FastFory 관련 10종 추가 (FastFory/LZ4/Zstd/Snappy/Gzip + Composite 5종)
- `RedissonCodecBenchmark` FastFory 4종 추가: FastFory +26% vs Fory (Apple M4 Pro 기준)

**infra/lettuce**

- `LettuceBinaryCodecs` FastFory 5종 팩토리 추가
- `LettuceCodecBenchmark` FastFory 4종 추가

> **와이어 포맷 경고**: FastFory(SCHEMA_CONSISTENT) ↔ Fory(COMPATIBLE) 비대칭.
> FastForyCodec → 구 Fory 바이트 fallback 읽기 가능, ForyCodec → FastFory 바이트 읽기 불가.
> **휘발성 캐시(TTL 있는 Redis)에서만 사용 권장.**

---

#### io/images — TIFF/SVG/AVIF·HEIC 포맷 확장 ([#199](https://github.com/bluetape4k/bluetape4k-projects/pull/199), [#134](https://github.com/bluetape4k/bluetape4k-projects/issues/134))

`bluetape4k-images` 모듈에 TIFF 다중 페이지, SVG 래스터화, AVIF/HEIC incubating 인터페이스를 추가했습니다.

**TIFF 지원 (TwelveMonkeys ImageIO)**

- `SuspendTiffWriter` — 단일 페이지 TIFF 비동기 저장 (`runInterruptible(Dispatchers.IO)` 적용, 취소 신호 정상 전파)
- `SuspendTiffMultiPageWriter` — 다중 페이지 TIFF 저장 (실패 시 부분 오염 방지: 내부 버퍼에 완성 후 복사)

**SVG 래스터화 (Apache Batik)**

- `BatikSvgRasterizer` / `SuspendSvgRasterizer` — SVG → PNG/JPEG 래스터화
- 보안: `KEY_EXECUTE_ONLOAD=false`, `KEY_ALLOWED_SCRIPT_TYPES=''`, `KEY_CONSTRAIN_SCRIPT_ORIGIN=true` (스크립트 실행 비활성화)
- `withTimeout(options.timeoutMillis)` 적용 (기본 10초), `KEY_MAX_WIDTH/HEIGHT` 항상 적용

**AVIF/HEIC Incubating 인터페이스**

- `AvifWriter` / `HeicReader` — `@IncubatingImageApi` (`@RequiresOptIn`) 어노테이션으로 안정성 경고 부착
- `AvifEncodeOptions.quality` 범위 검증 (`0.0f..1.0f`), `HeicReadOptions.pageIndex ≥ 0` 검증

**ImageFormat enum 확장**

- `TIFF`, `SVG`, `AVIF`, `HEIC` 항목 추가 (테스트 337 passing)

---

#### utils/images — 이미지 분석 기능 (DominantColor/BlurDetector/ExifData) ([#193](https://github.com/bluetape4k/bluetape4k-projects/pull/193), [#133](https://github.com/bluetape4k/bluetape4k-projects/issues/133))

`bluetape4k-images` 모듈에 이미지 분석 기능 3종을 추가했습니다.

- `DominantColor` — k-means 클러스터링 기반 지배 색상 추출 (Top-N 색상 + 비율 반환)
- `BlurDetector` — 라플라시안 분산 알고리즘 기반 흐림 감지 (임계값 조정 가능)
- `ExifData` — Apache Commons Imaging 기반 EXIF 메타데이터 추출 (GPS, Camera, Image 정보)

---

#### utils/images — 이미지 유사도 확장 ([#163](https://github.com/bluetape4k/bluetape4k-projects/pull/163), [#130](https://github.com/bluetape4k/bluetape4k-projects/issues/130))

`bluetape4k-images` 모듈에 4가지 유사도 알고리즘을 추가했습니다.

- `MssimSimilarity` — SSIM/MS-SSIM 구조적 유사도 (휘도·대비·구조 3요소 결합)
- `HashSimilarity` — 퍼셉추얼 해시 (aHash/dHash/pHash/wHash, 64~1024비트 정밀도 조정)
- `HistogramSimilarity` — 히스토그램 비교 (Bhattacharyya/Chi-Square/Correlation/Intersection 4가지 메트릭)
- `KeypointSimilarity` — ORB 특징점 매칭 기반 변환 불변 유사도 (FLANN/BF 매처)

---

#### utils/images — 필터·색보정 DSL ([#166](https://github.com/bluetape4k/bluetape4k-projects/pull/166), [#131](https://github.com/bluetape4k/bluetape4k-projects/issues/131))

`bluetape4k-images` 모듈에 필터·색보정 DSL과 신규 필터 5종을 추가했습니다.

**신규 필터**

- `MedianBlurFilter` — 중앙값 블러 (노이즈 제거)
- `RoundedCornerFilter` — 둥근 모서리 마스킹
- `ColorTemperatureFilter` — 색온도 조정 (웜/쿨 톤)
- `HueAdjustFilter` — 색상(Hue) 회전
- `SaturationAdjustFilter` — 채도 조정

**DSL 체이닝 (`imageFilter {}` 블록)**

- `ImageFilterChain` — 필터 파이프라인 빌더 (`blur {}`, `color {}`, `effect {}`, `style {}`, `transform {}` 섹션)
- `ColorSpaceConverter` — HSV/LAB/YCbCr 색공간 변환 유틸리티

---

#### utils/images — 변환 API ([#172](https://github.com/bluetape4k/bluetape4k-projects/pull/172), [#132](https://github.com/bluetape4k/bluetape4k-projects/issues/132))

`bluetape4k-images` 모듈에 이미지 변환 API 5종을 추가했습니다.

- `AutoCrop` — 여백 자동 감지·제거 (임계값 + 패딩 옵션)
- `SmartCrop` — 관심 영역 기반 지능형 크롭 (엔트로피/에지/안면 가중치)
- `Rotation` — 임의 각도 회전 + 자동 캔버스 확장 옵션
- `PerspectiveTransform` — 4점 호모그래피 기반 원근 보정
- `HistogramEqualization` (CLAHE) — 제한 대비 적응형 히스토그램 평활화
- `ImageFilterChainTransformOps` — 위 변환을 `imageFilter {}` DSL에 `transform {}` 블록으로 통합

---

#### infra/elasticsearch — Kotlin Coroutines 모듈 신규 구현 ([#167](https://github.com/bluetape4k/bluetape4k-projects/pull/167), [#146](https://github.com/bluetape4k/bluetape4k-projects/issues/146))

`bluetape4k-elasticsearch` 신규 모듈로 Elasticsearch Java Client 8.x를 Kotlin Coroutines로 래핑했습니다.

- `ElasticsearchClients` — `elasticsearchClientOf()` / `asyncElasticsearchClientOf()` 팩토리 (TLS/HTTPS/Basic Auth 지원)
- `ElasticsearchClientDsl` — `withElasticsearchClient {}` / `withAsyncElasticsearchClient {}` 스코프 함수
- `ElasticsearchCoroutines` — `indexAsync()` / `searchAsync()` / `deleteAsync()` 등 suspend 확장 함수
- `BulkApiCoroutines` — `bulkAsync()` / `bulkIndexAsync()` bulk 작업 suspend 래퍼
- `BulkIngesterCoroutines` — `BulkIngester` 기반 자동 플러시 bulk 인제스터 (suspend)

---

#### infra/pulsar — bluetape4k-pulsar 신규 모듈 ([#168](https://github.com/bluetape4k/bluetape4k-projects/pull/168), [#147](https://github.com/bluetape4k/bluetape4k-projects/issues/147))

Apache Pulsar 클라이언트를 Kotlin 관용구로 래핑한 `bluetape4k-pulsar` 신규 모듈을 추가했습니다.

- `PulsarClientSupport` — `pulsarClientOf()` / `withPulsarClient {}` 팩토리 + 스코프 함수
- `ProducerSupport` / `ProducerExtensions` — `producerOf()`, `withProducer {}`, `sendSuspend()`, `sendAsyncSuspend()`
- `ConsumerSupport` / `ConsumerExtensions` — `consumerOf()`, `withConsumer {}`, `receiveSuspend()`, `acknowledgeSuspend()`
- `ReaderSupport` / `ReaderExtensions` — `readerOf()`, `withReader {}`, `readNextSuspend()`
- `JacksonSchema` / `Jackson3Schema` — Jackson 기반 Pulsar 스키마 (ObjectMapper 주입)

---

#### testing/testcontainers — FalkorDB 컨테이너 추가 ([#161](https://github.com/bluetape4k/bluetape4k-projects/pull/161), [#160](https://github.com/bluetape4k/bluetape4k-projects/issues/160))

Redis 호환 그래프 데이터베이스 FalkorDB용 Testcontainers 래퍼를 추가했습니다.

- `FalkorDBServer` — `GenericContainer` 기반, `falkordb/falkordb:latest` 이미지, 포트 6379/3000
- `FalkorDBServerExtensions.kt` — `falkorDbClientOf()` / `falkorDbGraphClientOf()` 팩토리 확장 함수
- `FalkorDBServerTest` — 컨테이너 기동·그래프 CRUD 통합 테스트

---

#### testing/testcontainers — AwsEmulatorServer 공통 인터페이스 + FlociServer/ElasticMqServer/MailpitServer 추가 ([#164](https://github.com/bluetape4k/bluetape4k-projects/pull/164), [#155](https://github.com/bluetape4k/bluetape4k-projects/issues/155))

AWS 에뮬레이터 컨테이너를 위한 공통 인터페이스와 신규 서버 3종을 추가했습니다.

**공통 인터페이스**

- `AwsEmulatorServer` — `awsEndpoint`, `awsAccessKey`, `awsSecretKey` 프로퍼티 + `getCredentialProvider()` 확장 함수 계약 통일
- 런타임 에뮬레이터 전환: `-Dbluetape4k.aws.emulator=floci|localstack` JVM 속성

**신규 서버**

- `FlociServer` — AWS 에뮬레이터 (S3/DynamoDB/SQS/SNS/Lambda, 경량 Alpine 기반)
- `ElasticMqServer` — SQS/SNS 에뮬레이터 (elasticmq-native)
- `MailpitServer` — SES SMTP 에뮬레이터 (포트 1025/8025)

**기존 서버 마이그레이션**

- `LocalStackServer`, `MinIOServer` — `@Deprecated(WARNING)` 지정, FlociServer 마이그레이션 안내

---

#### data/hibernate — ORM 7.2.7.Final + Reactive 3.2.0.Final 업그레이드 ([#188](https://github.com/bluetape4k/bluetape4k-projects/pull/188), [#179](https://github.com/bluetape4k/bluetape4k-projects/issues/179))

Hibernate ORM 6.6.x → 7.2.7.Final, Hibernate Reactive 2.4.x → 3.2.0.Final로 업그레이드했습니다.

**핵심 API 변경**

- `SessionFactory.openStatelessSession()` → `SessionFactory.openSession()` (Reactive 3.x API)
- `Mutiny.SessionFactory` / `Stage.SessionFactory` suspend 래퍼 업데이트
- `persistence.xml` `hibernate.dialect` 명시 및 `hibernate.hbm2ddl.auto` 검증 강화

**호환성 처리**

- `@DisabledWithHibernate7AndSpringBoot3` 어노테이션 신설 — Spring Boot 3 + Hibernate 7 조합 미지원 테스트 조건부 비활성화
- `spring-boot3/hibernate-lettuce`: Hibernate 7 호환 `build.gradle.kts` 조정

**의존성 버전**

- `hibernate.version`: `6.6.44.Final` → `7.2.7.Final`
- `hibernate_reactive.version`: `2.4.11.Final` → `3.2.0.Final`

---

### Fixed

#### infra/kafka + infra/kafka4 — JacksonKafkaCodec class-reference 설정 시 allowedTypePackages 미적용 수정 ([#515](https://github.com/bluetape4k/bluetape4k-projects/pull/515))

1.8.0 보안 기능(`allowedTypePackages` 기본값 `emptySet()`)이 Kafka 속성에서 class-reference 방식으로 코덱을 지정할 때 적용되지 않던 문제를 수정했습니다.

- `CustomKafkaExamples` 테스트에서 `*_CLASS_CONFIG = JacksonKafkaCodec::class.java` → 인스턴스 기반 팩토리 설정(`ALLOW_ALL_TYPES_UNSAFE`)으로 교체
- Awaitility 대기 시간 기본 10초 → 30초로 연장 (CI 부하 여유분)
- `infra/kafka` / `infra/kafka4` 양쪽 동일 적용

---

#### bluetape4k/coroutines + bluetape4k/core + infra/lettuce + io/retrofit2 — 1.8.0 hard blocker batch ([#512](https://github.com/bluetape4k/bluetape4k-projects/pull/512))

- **Resumable stale waiter** ([#483](https://github.com/bluetape4k/bluetape4k-projects/issues/483)): `Resumable.await()`에 `invokeOnCancellation` 핸들러 추가 — 취소된 continuation이 슬롯에 잔류하는 문제 수정. `FutureToCompletableFutureWrapper.cancel()`이 하위 `Future`에 취소를 전파하도록 수정.
- **Lettuce write-behind drop** ([#476](https://github.com/bluetape4k/bluetape4k-projects/issues/476)): `LettuceSuspendedLoadedMap` write-behind 재시도 중 엔트리 유실 수정.
- **Retrofit HC5 cancel race** ([#484](https://github.com/bluetape4k/bluetape4k-projects/issues/484)): `Hc5CallFactory` cancel/enqueue race 수정, OkHttp request tag fallback 추가.
- **Retrofit Vert.x cancel+tag** ([#489](https://github.com/bluetape4k/bluetape4k-projects/issues/489)): pre-cancel 시 `promise.cancel(true)` 누락으로 30초 hang 발생하던 문제 수정. Vert.x call의 tag가 `okRequest.tag()`를 fallback으로 조회하도록 수정.

---

#### cache/cache-core — CacheCoroutineLocks per-key Mutex ref-counting 수정 ([#513](https://github.com/bluetape4k/bluetape4k-projects/pull/513), [#499](https://github.com/bluetape4k/bluetape4k-projects/issues/499))

`CacheCoroutineLocks.mutexFor()`와 `releaseMutex()`에 ref-counting을 추가하여 동시 호출 경쟁에서 Mutex 조기 제거로 인한 `NullPointerException` 수정.

---

#### cache/near-cache-core — suspend 이벤트 전파 누락 수정 ([#514](https://github.com/bluetape4k/bluetape4k-projects/pull/514), [#490](https://github.com/bluetape4k/bluetape4k-projects/issues/490))

`AbstractSuspendNearCache`의 `invalidate`/`invalidateAll`/`put` 이벤트가 suspend 경로에서 전파되지 않던 문제를 수정했습니다. Lettuce/Redisson 백엔드에 suspend 전파 계약 명시.

---

#### io/io — org.lz4 → at.yawk.lz4:1.11.0 CVE 보안 마이그레이션 ([#233](https://github.com/bluetape4k/bluetape4k-projects/pull/233), [#203](https://github.com/bluetape4k/bluetape4k-projects/issues/203))

- `org.lz4:lz4-java` → `at.yawk.lz4:lz4-java:1.11.0` 교체 — CVE-2025-12183 / CVE-2025-66566 취약점 수정
- `LZ4Factory` / `LZ4FastDecompressor` API 호환 — 소스 코드 변경 없음

---

#### bluetape4k/core — AssertSupport AssertionError 계약 복구 ([`c6d0ca1da`](https://github.com/bluetape4k/bluetape4k-projects/commit/c6d0ca1da))

- `assertXxx()` 함수가 `IllegalArgumentException` 대신 `AssertionError`를 던지도록 원복 (30+ 파일 테스트 계약 보호)
- 전체 프로덕션 코드의 `assertXxx()` → `requireXxx()` 마이그레이션 완료
- `AssertSupportTest` — 예외 타입 계약을 회귀 테스트로 고정

---

#### 전체 모듈 코드 리뷰 HIGH/CRITICAL 이슈 일괄 수정 (2026-04-28) ([#216](https://github.com/bluetape4k/bluetape4k-projects/pull/216)–[#232](https://github.com/bluetape4k/bluetape4k-projects/pull/232))

전체 모듈 코드 리뷰 결과 발견된 HIGH/CRITICAL 이슈를 모듈 그룹별로 수정했습니다.

| PR | 대상 모듈 | 주요 수정 내용 |
|----|-----------|---------------|
| [#216](https://github.com/bluetape4k/bluetape4k-projects/pull/216) | bluetape4k/core | `assertXxx` 예외 타입, `runCatching` 누수 패턴 |
| [#217](https://github.com/bluetape4k/bluetape4k-projects/pull/217) | infra/lettuce | NearCache 이중 해제, CancellationException 전파 |
| [#218](https://github.com/bluetape4k/bluetape4k-projects/pull/218) | infra/redisson, infra/micrometer | Dispatcher 누락, 타임아웃 처리 |
| [#219](https://github.com/bluetape4k/bluetape4k-projects/pull/219) | bluetape4k/coroutines | Subject CancellationException 전파 검증 |
| [#220](https://github.com/bluetape4k/bluetape4k-projects/pull/220) | data/hibernate, data/mongodb | Session 누수, 예외 래핑 |
| [#221](https://github.com/bluetape4k/bluetape4k-projects/pull/221) | data/r2dbc, data/exposed-postgresql | SQL identifier allowlist |
| [#222](https://github.com/bluetape4k/bluetape4k-projects/pull/222) | testing/junit5 | `@BeforeEach` suspend 패턴 |
| [#223](https://github.com/bluetape4k/bluetape4k-projects/pull/223) | io/vertx | 응답 누수 방지, shutdownNow fallback |
| [#224](https://github.com/bluetape4k/bluetape4k-projects/pull/224) | io/io, io/protobuf | `ObjectInputFilter` 보안 강화, 직렬화 거부 경로 |
| [#225](https://github.com/bluetape4k/bluetape4k-projects/pull/225) | io/feign | 재시도 로직, null 안전성 |
| [#226](https://github.com/bluetape4k/bluetape4k-projects/pull/226) | testing/mock-web-server, mock-webflux-server | `ApiErrorResponse.stackTraces` 제거 |
| [#227](https://github.com/bluetape4k/bluetape4k-projects/pull/227) | infra/kafka | 프로듀서/컨슈머 리소스 누수 |
| [#228](https://github.com/bluetape4k/bluetape4k-projects/pull/228) | spring-boot3/4 | 보안 헤더, 에러 응답 스택트레이스 노출 |
| [#229](https://github.com/bluetape4k/bluetape4k-projects/pull/229) | utils/idgenerators, jwt, geo, rule-engine | 입력 검증 강화 |
| [#230](https://github.com/bluetape4k/bluetape4k-projects/pull/230) | io/csv, texts/tokenizer-core | suspend 블로킹 I/O, InputStream 누수 |
| [#231](https://github.com/bluetape4k/bluetape4k-projects/pull/231) | io/http, io/grpc | 응답 누수 방지, gRPC shutdownNow fallback |
| [#232](https://github.com/bluetape4k/bluetape4k-projects/pull/232) | infra/elasticsearch, io/jackson, io/tink | PIT close 로그, Jackson silent 파싱 에러 |

---

#### CI — gitleaks 히스토리 스캔 false-positive 억제 ([#206](https://github.com/bluetape4k/bluetape4k-projects/pull/206), [#205](https://github.com/bluetape4k/bluetape4k-projects/issues/205))

- `.gitleaksignore` 설정으로 히스토리 내 테스트 픽스처 / 설정 파일 false-positive 5건 억제

---

#### infra/cache + CI — Caffeine `estimatedSize()` 간헐적 실패 및 워크플로우 개선 ([#123](https://github.com/bluetape4k/bluetape4k-projects/pull/123))

- `HazelcastLocalCache` / `LettuceCaffeineLocalCache` / `ResilientLocalJCache.estimatedSize()`: `cleanUp()` 후 호출로 CI 공유 2코어 환경 비동기 지연 문제 수정
- `HazelcastEntryEventListener` self-invalidation race condition 수정 — ADD 이벤트 무시, UPDATE/REMOVE 로컬 멤버 이벤트 무시
- `opentelemetry` 테스트 JVM에 `-Dreactor.netty.native=false` 추가 (io_uring 레이스 컨디션)
- `ci.yml` `paths-ignore` 추가 — `**.md`, `docs/**` 등 문서 변경 시 CI 빌드 스킵
- `security.yml` `gitleaks-action@v2` → CLI 직접 설치로 교체 (유료 라이선스 요구 문제 해결)

#### 코드 리뷰 HIGH 이슈 6건 수정 ([#201](https://github.com/bluetape4k/bluetape4k-projects/pull/201))

- `DisabledWithHibernate7AndSpringBoot3`: 무조건 비활성화 의미를 KDoc/TODO로 명시
- `ExifData.readExif`: `IOException` → warn, 파싱 예외 → debug로 로그 레벨 분리
- `ExifData.toExifData`: `runCatching.getOrNull()` → `runCatchingDebug()` 헬퍼 적용 (15개 필드)
- `DominantColor.dominantColors`: `extractor.extract()` 예외 처리 + `KLogging` 추가
- `MatrixSupport.toRealVector/toRealMatrix`: 최대 차원 상한 적용 (벡터 10M, 행렬 10K) + `StreamCorruptedException`/`EOFException` 컨텍스트 포함 래핑
- `SessionSupportStandaloneTest`: `executed.shouldNotBeNull()` → `executed.shouldBeTrue()`

#### utils/math — MatrixSupport 직렬화 버그 수정 ([#192](https://github.com/bluetape4k/bluetape4k-projects/pull/192), [#187](https://github.com/bluetape4k/bluetape4k-projects/issues/187))

- `MatrixSupport.toRealVector/toRealMatrix`: Java 직렬화 `defaultWriteObject()/defaultReadObject()` 호출 제거 — `Externalizable` 미구현 시 `StreamCorruptedException` 발생하던 버그 수정

#### CI — nightly-tests.yml: test-io-http job에 mock-web-server Docker 빌드 step 추가 ([#171](https://github.com/bluetape4k/bluetape4k-projects/pull/171))

- `nightly-tests.yml` `test-io-http` job에 `jibDockerBuild` 빌드 step 누락으로 발생하던 mock-web-server 컨테이너 기동 실패 수정

#### CI — nightly 테스트 타임아웃 완화 ([#174](https://github.com/bluetape4k/bluetape4k-projects/pull/174))

- `nightly-tests.yml` 일부 job의 `timeout-minutes` 상향 조정 — CI 환경 부하 시 간헐적 타임아웃 방지

---

### Changed

#### images/ 그룹 디렉토리 신설 — utils/images* 이동 ([#237](https://github.com/bluetape4k/bluetape4k-projects/pull/237))

`utils/images*` 4개 모듈을 `images/` 최상위 그룹으로 이동했습니다. 모듈 이름 불변(하위 호환성 유지).

---

#### io/vertx — Vert.x 4.5.26 → 5.0.11 업그레이드 ([#215](https://github.com/bluetape4k/bluetape4k-projects/pull/215), [#197](https://github.com/bluetape4k/bluetape4k-projects/issues/197))

- Vert.x 5.x API 마이그레이션 (`Vertx.factory` 제거, `VertxOptions` 변경 사항 반영)
- `bluetape4k-vertx`: Vert.x 5.0.11 기반 재빌드, 기존 코루틴 확장 함수 호환성 유지

---

#### data/hibernate — Hibernate 7 + Spring Boot 4 마이그레이션 ([#210](https://github.com/bluetape4k/bluetape4k-projects/pull/210))

Hibernate ORM 7.x + Spring Boot 4 조합 호환성 이슈를 수정했습니다.

- `data/hibernate` — `EntityManagerSupport`, JPA QueryDSL 6.x API 호환 수정
- `spring-boot4/jpa-querydsl-demo` — Spring Boot 4 BOM + Hibernate 7 통합 데모 빌드 정상화

---

#### data/exposed-** — Regex 인스턴스 companion object / top-level val 상수화 ([#213](https://github.com/bluetape4k/bluetape4k-projects/pull/213))

- 함수 로컬 `Regex(...)` 호출 → companion object/top-level `val` 상수로 이동 — 반복 컴파일 제거
- 적용 모듈: `exposed-core`, `exposed-dao`, `exposed-jdbc`, `exposed-r2dbc`

---

#### testing/testcontainers — Floci/MiniStack 테스트 기반 클래스 도입 ([#211](https://github.com/bluetape4k/bluetape4k-projects/pull/211))

- `AbstractFlociServiceTest` / `AbstractMiniStackServiceTest` — AWS 에뮬레이터 공통 초기화 추상화

---

#### chore — CodeRabbit 제거, OMC Code Review로 대체 ([`13e759d92`](https://github.com/bluetape4k/bluetape4k-projects/commit/13e759d92))

- `.coderabbit.yaml` 삭제
- PR 템플릿 및 CLAUDE.md 내 `/coderabbit:review` → `/oh-my-claudecode:code-reviewer` 변경

---

### Tests

#### data/exposed-jdbc + data/exposed-r2dbc — 테스트 커버리지 70%+ 달성 ([#124](https://github.com/bluetape4k/bluetape4k-projects/pull/124))

- `ReadableExtensionsTest` 등 누락 테스트 추가로 `exposed-jdbc` / `exposed-r2dbc` 라인 커버리지 70% 초과 달성

#### spring-boot3/hibernate-lettuce + spring-boot4/hibernate-lettuce — 테스트 커버리지 95.4% 달성 ([#125](https://github.com/bluetape4k/bluetape4k-projects/pull/125))

#### data/exposed-r2dbc — 테스트 커버리지 47.60% → 89.11% 달성 ([#180](https://github.com/bluetape4k/bluetape4k-projects/pull/180), [#176](https://github.com/bluetape4k/bluetape4k-projects/issues/176))

- R2DBC 전용 테스트 (`ReadableExtensionsTest`, `UpdatableExtensionsTest`, Repository 통합 테스트 등) 대거 추가로 라인 커버리지 89.11% 달성

#### io/http — 테스트 커버리지 32% → 72% 달성 ([#186](https://github.com/bluetape4k/bluetape4k-projects/pull/186), [#178](https://github.com/bluetape4k/bluetape4k-projects/issues/178))

- `AsyncHttpClient`(HC5) / `MinimalHttpAsyncClient` 테스트 22종 추가 (T1~T22), 독립 `ConnectionManager` 패턴으로 테스트 격리 문제 수정

#### utils/math — 테스트 커버리지 65.4% → 70.7% 달성 ([#183](https://github.com/bluetape4k/bluetape4k-projects/pull/183), [#181](https://github.com/bluetape4k/bluetape4k-projects/issues/181))

- `MatrixSupport` / `VectorSupport` / `StatisticsSupport` 엣지 케이스 테스트 추가

#### infra/nats — 테스트 커버리지 49% → 79.45% 달성 ([#182](https://github.com/bluetape4k/bluetape4k-projects/pull/182), [#177](https://github.com/bluetape4k/bluetape4k-projects/issues/177))

- NATS JetStream 구독/발행 / KV Store / Object Store 통합 테스트 추가, 코드 리뷰 HIGH 3건 수정

#### io/okio + infra/cache-core — 테스트 커버리지 80% 달성 ([#169](https://github.com/bluetape4k/bluetape4k-projects/pull/169))

- `io/okio`: `BufferedSuspendSourceTest` 대규모 보강 (스트림 길이 경계/인코딩/예외 케이스)
- `infra/cache-core`: JCache + Caffeine 통합 테스트 추가로 라인 커버리지 80% 초과

#### data/hibernate — LINE 커버리지 70.4% 달성 ([#194](https://github.com/bluetape4k/bluetape4k-projects/pull/194), [#179](https://github.com/bluetape4k/bluetape4k-projects/issues/179))

- `EntityManagerSupport`, `converters/**`, `criteria/**`, `listeners/**`, `model/**`, `querydsl/**` 유닛·통합 테스트 추가, Hibernate 7 호환성 수정

#### data/hibernate-reactive — 테스트 커버리지 37.8% → 85.7% 달성 ([#195](https://github.com/bluetape4k/bluetape4k-projects/pull/195), [#179](https://github.com/bluetape4k/bluetape4k-projects/issues/179))

- Mutiny API / Stage API 세션 CRUD 통합 테스트 추가 (`MutinySessionSupportTest`, `StageSessionSupportTest`)

#### infra/pulsar — DSL 라이프사이클 테스트 추가 ([#173](https://github.com/bluetape4k/bluetape4k-projects/pull/173))

- `PulsarClientSupportTest`, `ProducerSupportTest`, `ConsumerSupportTest`, `ReaderSupportTest` — Testcontainers Pulsar 기반 통합 테스트

---

### Documentation

#### utils/science — README 전면 재작성 (GIS/Shapefile/JTS/PostGIS/NetCDF 통합 가이드) ([#128](https://github.com/bluetape4k/bluetape4k-projects/issues/128))

`bluetape4k-science` 모듈의 README.md(영문) + README.ko.md(한국어)를 전면 재작성했습니다.

- 5개 도메인 상태 테이블 (GIS / Shapefile / JTS / PostGIS / NetCDF ⚠️ Phase 5)
- Mermaid 통합 아키텍처 다이어그램 3종 (모듈 관계도 + 좌표 변환 흐름 + DB 스키마)
- Module Layout 섹션 신설, Quick Start 5개 섹션, API 가이드, 성능/운영 가이드
- NetCDF 현황 정확히 반영: Table/Repository ✅, `NetCdfCatalogService` ⚠️ Phase 5

#### docs(kdoc) — FastFory 공개 API KDoc 강화 ([`30aaf462e`](https://github.com/bluetape4k/bluetape4k-projects/commit/30aaf462e))

- `ForyBinarySerializer.forHighThroughput()` 팩토리 메서드 추가 및 KDoc 예제 강화

#### docs — README Mermaid UML 다이어그램 / KDoc 커버리지 확대 ([#152](https://github.com/bluetape4k/bluetape4k-projects/issues/152))

10개 모듈 README에 Mermaid UML 다이어그램 추가 및 공개 API KDoc 전수 작성 완료.

**README Mermaid 다이어그램 추가 모듈 (10개)**

| 모듈 | 다이어그램 |
|------|-----------|
| `data/exposed-cache` | `classDiagram`: JdbcCacheRepository / SuspendedJdbc* / R2dbc* 계층, `sequenceDiagram`: Read-Through / Write-Through / Write-Behind |
| `data/exposed-jdbc-lettuce` | `classDiagram`: JdbcLettuceRepository 상속 구조 + NearCache 흐름도 |
| `data/exposed-jdbc-redisson` | `classDiagram`: JdbcRedissonRepository 계층 + 9종 Mermaid 다이어그램 |
| `data/exposed-jdbc-caffeine` | `classDiagram`: AbstractJdbcCaffeineRepository / AbstractSuspendedJdbcCaffeineRepository |
| `data/exposed-r2dbc` | `classDiagram`: R2DBC 파이프라인 + 4종 다이어그램 |
| `infra/cache-core` | `classDiagram`: NearCacheOperations / SuspendNearCacheOperations + `sequenceDiagram` NearCache get/put |
| `infra/cache-lettuce` | `classDiagram`: LettuceNearCache 계층 + `sequenceDiagram`: RESP3 Pub/Sub 무효화 흐름 |
| `infra/cache-redisson` | `classDiagram`: RedissonNearCache / Resp3 변형 + `sequenceDiagram`: RLocalCachedMap 흐름 |
| `infra/cache-hazelcast` | `classDiagram`: HazelcastNearCache + EntryListener 계층 + `sequenceDiagram`: 2-tier 흐름 |
| `utils/batch` | `classDiagram`: BatchJob/BatchStep/Chunk 파이프라인 + `sequenceDiagram`: 실행 흐름 |

**KDoc 전수 작성 완료 (공개 API)**

- `JdbcCacheRepository` / `SuspendedJdbcCacheRepository` / `R2dbcCacheRepository` — `@param`, `@return`, 사용 예제 포함
- `CacheMode` / `CacheWriteMode` / `LocalCacheConfig` — enum 항목별 KDoc + 제약 조건 설명
- `NearCacheOperations` / `SuspendNearCacheOperations` — checkpoint 시맨틱, 사용 순서 명시
- `BatchReader` / `BatchProcessor` / `BatchWriter` / `BatchJob` — 사용 패턴 + 예외 조건 문서화

#### infra/lettuce + infra/redisson + infra/cache-lettuce — JMH 벤치마크 결과 공개 ([#200](https://github.com/bluetape4k/bluetape4k-projects/pull/200), [#184](https://github.com/bluetape4k/bluetape4k-projects/issues/184))

`infra/lettuce`, `infra/redisson`, `infra/cache-lettuce` 모듈의 JMH 실측 벤치마크 결과를 문서화했습니다.

**NearCache JMH 벤치마크 인프라 추가 (infra/cache-lettuce)**

- `src/benchmark` 소스셋 신설 (`kotlinx_benchmark` 플러그인 + `allOpen` 설정)
- `NearCacheBenchmark` — `l1Hit` / `l2Hit` / `l2Miss` / `putSingle` / `putAll` 측정 (`@Param payloadSize: 512/4096/16384`, `batchSize: 100`)
- `NearCacheRemoveBenchmark` — `@Setup(Level.Invocation)` 격리 패턴
- Testcontainers Redis 7+ + RESP3 `RedisClient`, `LettuceNearCacheConfig(recordStats=true)` 사용

**벤치마크 결과 문서 (Benchmark.md + Benchmark.ko.md)**

- `infra/lettuce`: `LettuceCodecBenchmark` — Kryo/Fory/FastFory/LZ4/Zstd/Snappy 실측값 (Apple M4 Pro 기준)
- `infra/redisson`: `RedissonCodecBenchmark` — FastFory +26% vs Fory 수치 포함
- `infra/cache-lettuce`: NearCache L1/L2 히트율별 레이턴시 테이블

---

## [1.7.0] — 2026-04-24

### Added

#### io/csv — univocity-parsers 제거, 자체 RFC 4180 엔진으로 교체 ([`c8ad4f97e`](https://github.com/bluetape4k/bluetape4k-projects/commit/c8ad4f97e))

`bluetape4k-csv` v1.5.0부터 내부 파서/라이터 엔진이 univocity-parsers 에서 자체 구현으로 교체됩니다.

**V1 — 기존 API 유지 (설정 타입만 변경)**

| 변경 전 (univocity) | 변경 후 (자체 엔진) |
|---------------------|-------------------|
| `CsvParserSettings` | `CsvSettings` |
| `TsvParserSettings` | `TsvSettings` |
| `CsvWriterSettings` | `CsvSettings` |
| `TsvWriterSettings` | `TsvSettings` |

- `CsvLexer` / `TsvLexer`: RFC 4180 문자 단위 상태 기계 렉서, BOM 감지·제거, CRLF/LF/CR 지원
- `DelimitedWriter` → `CsvLineWriter` / `TsvLineWriter`: null → 인용 없는 빈 필드, `""` → `""` 인용 출력 (RFC 4180 roundtrip 보장)
- `SuspendCsvRecordReader` / `SuspendTsvRecordReader`: `channelFlow + ensureActive()` 기반 코루틴 취소 협력
- `SuspendCsvRecordWriter` / `SuspendTsvRecordWriter`: `Mutex` 기반 동시 쓰기 보호
- `Record` 공개 인터페이스 (`io.bluetape4k.csv.Record`) — `com.univocity.parsers.common.record.Record` 대체

**V2 — Flow DSL API (신규, `io.bluetape4k.csv.v2`)**

- `CsvRow`: 불변 data class 레코드 (`getString`, `getInt`, `getLong`, `getBoolean`, …)
- `FlowCsvReader` / `FlowCsvReaderImpl`: `csvReader { } / tsvReader { }` DSL, `read(InputStream)` / `readFile(Path)` → `Flow<CsvRow>`
- `FlowCsvWriter` / `FlowCsvWriterImpl`: `csvWriter(writer) { quoteAll = true } / tsvWriter(writer) { }` DSL, `writeAll(Flow<Iterable<*>>)`, `Mutex` 동시성 보호
- `Record.toCsvRow()` (public): V1 Record → V2 CsvRow 변환

**마이그레이션**: [`io/csv/MIGRATION.md`](io/csv/MIGRATION.md) 참조

#### core — `virtualFutureOfNullable` 추가 ([`ce48b684b`](https://github.com/bluetape4k/bluetape4k-projects/commit/ce48b684b))

`virtualFutureOf`가 non-null 타입만 지원하던 한계를 해소합니다.

- `virtualFutureOfNullable<V> { }` — Virtual Thread 위에서 nullable 결과를 반환하는 `CompletableFuture<V?>` 생성

#### CI/CD — GitHub Actions 파이프라인 구성 ([`ce48b684b`](https://github.com/bluetape4k/bluetape4k-projects/commit/ce48b684b))

- `ci.yml`: push/PR on `develop`·`main` 트리거, 8개 job (validate-wrapper, build, test-core, test-io, test-utils, test-exposed-core, test-docker, ci-status)
- `publish-snapshot.yml`: `develop` 브랜치 push 시 Maven Central Snapshots 자동 배포 (`CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY*`)

#### infra/redisson + infra/lettuce — JSON Codec 추가 ([`f3fa8d422`](https://github.com/bluetape4k/bluetape4k-projects/commit/f3fa8d422), [`a27926053`](https://github.com/bluetape4k/bluetape4k-projects/commit/a27926053))

Jackson 3.x / Fastjson2 / Lettuce 전용 JSON 직렬화 Codec을 추가했습니다.

**infra/redisson**
- `Jackson3Codec`: Jackson 3.x 기반 JSON Codec
- `Fastjson2Codec`: `[4-byte classNameLen]+[className UTF-8]+[JSONB bytes]` 포맷, pre-instantiation 보안 검증(`allowedPackagePrefixes`), 실패 시 fallback(`ForyCodec`) 자동 전환
- `RedissonCodecs`에 `jackson3` / `fastjson2` 팩토리 상수 추가
- `RedissonCodecBenchmark` JMH 벤치마크 추가 (JSON vs Binary vs Compressed 비교)

**infra/lettuce**
- `LettuceJsonCodec` + `LettuceJsonCodecs` 신규 구현
- `LettuceJsonCodecs.jackson3<V>()` / `fastjson2<V>()` 팩토리 메서드
- `LettuceCodecBenchmark` JMH 벤치마크 추가

#### utils/lingua — 모듈 승격 ([`b149fa793`](https://github.com/bluetape4k/bluetape4k-projects/commit/b149fa793))

`bluetape4k-lingua` 모듈이 `utils/lingua`로 정식 승격되었습니다.

- Nightly CI 파이프라인(`test-misc`) 에 `lingua` 모듈 추가
- CodeRabbit 코드 리뷰 워크플로 추가 (`.coderabbit.yaml`)

#### testing/testcontainers — Elasticsearch/Pulsar JVM 힙 크기 제한 환경 변수 추가 ([`d1135bc1`](https://github.com/bluetape4k/bluetape4k-projects/commit/d1135bc1))

Docker OOMKilled 방지를 위해 JVM 힙 크기를 환경 변수로 제한합니다.

- `ElasticsearchServer` / `ElasticsearchOssServer`: `ES_JAVA_OPTS=-Xms512m -Xmx512m` 추가
- `PulsarServer`: `PULSAR_MEM=-Xms256m -Xmx256m` 추가

#### testing/mock-webflux-server — Spring Boot 4 WebFlux 기반 Mock 서버 신규 추가 ([`f189a1e5b`](https://github.com/bluetape4k/bluetape4k-projects/commit/f189a1e5b))

Spring Boot 4 + WebFlux + Kotlin Coroutines 기반의 비동기 Mock HTTP 서버.

- `BluetapeWebfluxServer`: `bluetape4k/mock-webflux-server` Docker 이미지를 기반으로 하는 Testcontainers 래퍼
- 기존 `mock-web-server`(MVC/Virtual Thread)와 병행 제공
- `httpbinUrl`, `jsonplaceholderUrl`, `pingUrl` 프로퍼티 제공
- Jib 빌드: `arm64` / `amd64` 호스트 아키텍처 자동 감지

#### testing/mock-web-server — httpbin `/delay/{seconds}` 소수점 지원 ([`acd16ee7`](https://github.com/bluetape4k/bluetape4k-projects/commit/acd16ee7))

- `/delay/{seconds}` 경로가 `0.5`와 같은 소수점 값을 수신하여 밀리초 단위 delay 가능
- `Double` 파싱으로 교체하여 `1000`, `0.5`, `1.5` 모두 지원

### Fixed

#### testing/testcontainers — graphdb 서버 Docker 이미지 TAG 고정 버전으로 수정 ([`f4a3c700e`](https://github.com/bluetape4k/bluetape4k-projects/commit/f4a3c700e))

`latest` 또는 부동(floating) major 버전 태그를 사용하던 Graph DB 서버를 특정 버전으로 고정합니다.

| 서버 | 이전 TAG | 이후 TAG |
|------|----------|----------|
| `Neo4jServer` | `5` (floating major) | `5.26.24` |
| `MemgraphServer` | `3.2.1` | `3.9.0` |
| `PostgreSQLAgeServer` | `latest` | `release_PG17_1.6.0` |

- `PostgreSQLAgeServer` KDoc 불일치 수정: 주석의 `PG17_latest` → `release_PG17_1.6.0`
- `README.md` / `README.ko.md`: Graph DB 서버 Docker 이미지 버전 테이블 추가

#### infra/cache-lettuce — CLIENT TRACKING 경쟁 조건 수정 ([`5b806466`](https://github.com/bluetape4k/bluetape4k-projects/commit/5b806466))

`LettuceNearCache` / `LettuceSuspendNearCache`의 `registerTrackingKey` / `registerTrackingKeys`가 fire-and-forget 방식(await 없음)이어서 외부 SET이 tracking GET보다 먼저 Redis에 도달할 경우 invalidation 메시지가 발송되지 않는 경쟁 조건을 수정했습니다.

- `registerTrackingKey` / `registerTrackingKeys`: fire-and-forget → `await` 방식으로 변경

### Changed

#### 빌드 — Gradle 빌드 출력을 표준 모듈별 `build/` 디렉토리로 변경 ([`fe920ba2`](https://github.com/bluetape4k/bluetape4k-projects/commit/fe920ba2))

- 루트 공유 `build/` 대신 각 모듈 기본 `layout.buildDirectory`(`build/`) 사용
- kosogor 플러그인 제거 후 표준 Gradle 빌드 출력 경로로 대체 ([`c129b11f`](https://github.com/bluetape4k/bluetape4k-projects/commit/c129b11f))

#### CI/CD — Codecov → Coveralls 커버리지 리포터 교체 ([`d6b1d7de`](https://github.com/bluetape4k/bluetape4k-projects/commit/d6b1d7de))

- `codecov/codecov-action` 제거, `coverallsapp/github-action` 으로 전환
- README 뱃지: Kotlin, JVM 21, MIT 라이선스 뱃지 추가

#### CI/CD — Nightly Tests를 ci.yml 테스트 구조 기반으로 재편 ([`3aadab9f`](https://github.com/bluetape4k/bluetape4k-projects/commit/3aadab9f))

- `nightly-tests.yml`: `ci.yml`과 동일한 job 구조로 통일, `build` job 추가
- `test-aws` / `test-testcontainers` job은 Nightly 전용으로 유지
- `test-misc` → `test-misc` + `test-testcontainers` 분리

#### 의존성 — kotlin-stdlib 2.3.21 업그레이드 ([`1c5c8bce`](https://github.com/bluetape4k/bluetape4k-projects/commit/1c5c8bce))

- `kotlin-stdlib` 2.3.20 → 2.3.21

### Performance

#### bluetape4k/coroutines — Flow 처리량 +32.7% ([`549fa341`](https://github.com/bluetape4k/bluetape4k-projects/commit/549fa341))

- `parallelFlowMap`: per-rail Channel + `select` 기반 fan-in 재설계 (+506%)
- `AsyncFlow`: `LazyDeferred` atomic 제거, `start()` → `Deferred<T>` 직접 반환으로 단순화
- JMH 벤치마크: geomean 처리량 +32.7% 개선

#### data/exposed-jdbc — JMH 처리량 +78.9% ([`6df470a9`](https://github.com/bluetape4k/bluetape4k-projects/commit/6df470a9))

- JDBC 배치 insert/update 쿼리 최적화 (25,401 → 45,431 ops/s)
- HikariCP 풀 설정 및 커넥션 재사용 개선

#### io — ForyBinarySerializer.fast() + KryoBinarySerializer.fast() 최적화 (+97%) ([`126ab849`](https://github.com/bluetape4k/bluetape4k-projects/commit/126ab849))

- `ForyBinarySerializer.fast()`: `SCHEMA_CONSISTENT` + 비동기 컴파일 비활성화로 처리량 향상
- `KryoBinarySerializer.fast()`: `KryoProvider.createFastKryo()` + Output 풀 재사용으로 처리량 +97%

#### data — R2DBC 풀 과부하 튜닝 ([`07d6d823`](https://github.com/bluetape4k/bluetape4k-projects/commit/07d6d823))

- R2DBC 연결 풀 `initialSize` / `maxSize` / `maxIdleTime` 설정 최적화 가이드 고정 (#98)

### Removed

#### data/exposed-jasypt — 모듈 전체 삭제 ([`120c1f5a2`](https://github.com/bluetape4k/bluetape4k-projects/commit/120c1f5a2))

jasypt 기반 암호화 컬럼 타입을 `exposed-tink`(Google Tink AEAD/DAEAD)로 완전 대체합니다.

- `JasyptVarCharColumnType`, `JasyptBinaryColumnType`, `JasyptBlobColumnType` 및 테스트 전체 삭제
- `settings.gradle.kts`에서 `bluetape4k-exposed-jasypt` 모듈 제외

#### io/crypto — 모듈 전체 삭제 ([`120c1f5a2`](https://github.com/bluetape4k/bluetape4k-projects/commit/120c1f5a2))

jasypt(`org.jasypt.*`) 기반 `Encryptor`, `Digester` 등 암호화 유틸리티 모듈을 삭제합니다. 암호화는 `io/tink`(`TinkEncryptor`) 사용을 권장합니다.

- `Libs.jasypt` 상수 제거
- 5개 모듈(`okio`, `jackson2`, `jackson3`, `io`, `exposed-jdbc-tests`)에서 `bluetape4k-crypto` compileOnly 의존성 제거
- `data/exposed-core`의 deprecated `EncryptedVarCharColumnType` 등 3개 클래스 삭제

#### core — `@Deprecated` 항목 전수 제거 ([`da4b6dd1f`](https://github.com/bluetape4k/bluetape4k-projects/commit/da4b6dd1f), [`3d8bef82f`](https://github.com/bluetape4k/bluetape4k-projects/commit/3d8bef82f))

`bluetape4k-core` main source에서 `@Deprecated` 항목 26개를 모두 제거했습니다 (-665 lines).

| 파일 | 제거 항목 |
|------|----------|
| `Systemx` | `JAVA_CLASS_VERION`(오타), `processCount`, `javaIoTmpDir`, `isJava6`~`isJava10` |
| `TimeSpec` | `MILLIS_IN_DAY`, `MILLIS_IN_HOUR`, `MILLIS_IN_MINUTE` |
| `DateSupport` | `Date.plus(Date)`, `Timestamp.plus(Timestamp)` |
| `StringSupport` | `ifEmpty`(nullable), `asStringList`, `redact` |
| `NumberSupport` | `coerce` |
| `AutoCloseableSupport` | `using` infix |
| `EnumSupport` | `Class<E>`/`KClass<E>` 기반 함수 8개 |
| `ExecutorSupport` | deprecated `VirtualThreadExecutor` |
| `StructuredTaskScopeSupport` | `structuredTaskScopeFirst` |
| `ProgressionSupport` | `IntProgression.grouped()`, `LongProgression.grouped()` |
| `IterableSupport` / `SequenceSupport` | `tryMap` |
| `QueueSupport` | `linkedBlokcingDequeOf`·`QueueOf` 오타 함수 4개 |
| `AnySupport` | `areEquals` |
| `ArraySupport` | `removeLastValue` |
| `ApacheConstructorUtils` | `getAccessbleConstructor`(오타) |
| `images/ImageInputStream·OutputStream` | `using` deprecated |

---

## [1.6.2] - 2026-04-16

### Removed

#### spring-boot3/4 core — Retrofit2 통합 전면 제거

Jackson 버전 불일치(`spring-boot4`의 `DefaultRetrofitClientConfiguration`이 Jackson 2 API를 사용하는 버그)를 계기로, 두 모듈에서 Retrofit2 관련 코드를 모두 제거했습니다. Feign 사용이 주류이므로 유지 비용 없는 제거가 적절합니다.

**spring-boot3/core** 제거 항목:
- `retrofit2/` main 소스 8개 파일 (`DefaultRetrofitClientConfiguration`, `EnableRetrofitClients`, `Retrofit2Client`, `RetrofitAutoConfiguration`, `RetrofitClientContext`, `RetrofitClientFactoryBean`, `RetrofitClientSpecification`, `RetrofitClientsRegistrar`)
- `retrofit2/` 테스트 전체 (httpbin / jsonplaceholder API 테스트)
- `build.gradle.kts`: `bluetape4k-retrofit2`, `retrofit2_*`, `vertx_*`, `async_http_client*` 의존성 제거
- `application.yml`: retrofit2 서비스 설정 제거

**spring-boot4/core** 제거 항목: spring-boot3와 동일 (Jackson 3 기준으로도 `converter-jackson` 미지원)

---

## [1.6.1] - 2026-04-16

### Added

#### testing/mock-web-server — `bluetape4k-mock-web-server` 신규 모듈 ([
`a340e49b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/a340e49b4))

Spring Boot 4 + Java 25 + Virtual Threads 기반의 자체 내장 Mock HTTP 서버.
기존 외부 의존(`httpbin.org`, `jsonplaceholder.typicode.com`)을 컨테이너화된 로컬 서버로 대체합니다.

- **httpbin 시뮬레이터**: `GET /anything`, `POST /anything`, `/status/{code}`, `/delay/{n}`, `/headers`, `/ip`, `/uuid`, `/gzip`, `/stream/{n}`, `/image/{type}` 등 지원
- **jsonplaceholder 시뮬레이터**: posts / comments / albums / photos / todos / users CRUD API (인메모리 상태)
- **웹 컨텐츠 시뮬레이터**: naver / google / home / login / article HTML 페이지
- `/ping` → `"pong"` 반환 (Testcontainers wait strategy 호환)
- Jib 빌드: `arm64` / `amd64` 호스트 아키텍처 자동 감지
- Jackson 3 (`tools.jackson.*`) 사용 — Spring Boot 4 호환

#### testing/testcontainers — `BluetapeHttpServer` 추가, `HttpbinServer` 대체 ([`a340e49b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/a340e49b4))

- `BluetapeHttpServer`: `bluetape4k/mock-web-server` Docker 이미지를 기반으로 하는 Testcontainers 래퍼
- `httpbinUrl`, `jsonplaceholderUrl`, `pingUrl` 프로퍼티 제공
- 기존 `HttpbinServer`, `HttpbinHttp2Server` 및 관련 테스트 제거

### Changed

#### testing/mock-web-server — 전체 모듈 `BluetapeHttpServer` 마이그레이션 ([
`22986785c`](https://github.com/bluetape4k/bluetape4k-projects/commit/22986785c))

io/feign, io/retrofit2, io/http, infra/micrometer, spring-boot3/4 등 외부 httpbin에 의존하던 테스트를 모두 `BluetapeHttpServer`로 전환하였습니다.

#### spring-boot3/4 cassandra — `ReactiveSession.executeSuspending` SimpleStatement 강제 경유 ([`6376544ae`](https://github.com/bluetape4k/bluetape4k-projects/commit/6376544ae))

Kotlin 오버로드 해석 문제(`execute(String, Map)` → `execute(String, Object...)` vararg로 잘못 dispatch)로 인해 Map 전체가 단일 BIGINT 값으로 직렬화되던 버그를 수정했습니다.

- `executeSuspending(query, vararg args)` → `execute(SimpleStatement.newInstance(query, *args)).awaitSingle()`
- `executeSuspending(query, Map)` → `execute(SimpleStatement.newInstance(query, args)).awaitSingle()`
- spring-boot3 / spring-boot4 cassandra 모듈 동시 적용

#### 빌드 — SB3 BOM 루트 제거, 각 모듈 명시적 선언으로 전환 ([`a340e49b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/a340e49b4))

루트 `build.gradle.kts`에서 Spring Boot 3 / Spring Cloud / Spring Integration BOM을 제거하고 SB3 모듈(29개)에서 `implementation(platform(Libs.spring_boot3_dependencies))`를 직접 선언합니다.
SB4 모듈이 SB3 BOM에 오염되던 문제를 해소합니다.

### Docs

#### 전체 README — Mermaid UML 다이어그램 스타일 가이드 적용 ([`5e5ae1963`](https://github.com/bluetape4k/bluetape4k-projects/commit/5e5ae1963))

모든 모듈 README의 Mermaid 다이어그램(classDiagram / sequenceDiagram / flowchart)에 색상 테마 및 레이아웃 가이드를 일관되게 적용했습니다.

---

## [1.6.0] - 2026-04-14

### Added

#### spring-boot3/batch-exposed — Spring Batch 5.x + Exposed JDBC Partitioned Step 모듈 추가 ([`853c140bc`](https://github.com/bluetape4k/bluetape4k-projects/commit/853c140bc))

- `Partitioned Step + VirtualThread Parallel Query` 전략의 배치 모듈 추가
- `ExposedRangePartitioner`: auto-increment PK를 ID 범위로 분할하는 파티셔너
- `ExposedKeysetItemReader`: keyset 페이징 + 재시작 지원 `ItemStreamReader`
- `ExposedItemWriter` / `ExposedUpdateItemWriter` / `ExposedUpsertItemWriter` 제공
- `virtualThreadPartitionTaskExecutor`, `partitionedBatchJob`, `ExposedBatchAutoConfiguration` 포함
- H2/PostgreSQL/MySQL 기반 테스트 및 파티션 벤치마크 추가

#### spring-boot4/batch-exposed — Spring Boot 4 + Exposed 통합 배치 모듈 추가 ([`a02e9201e`](https://github.com/bluetape4k/bluetape4k-projects/commit/a02e9201e))

- `ExposedKeysetItemReader`, `ExposedRangePartitioner`, `ExposedItemWriter` 계열을 Spring Boot 4 환경에 맞춰 제공
- `ExposedBatchAutoConfiguration` 및 배치 DSL 포함
- README/README.ko와 기본 통합 테스트 세트 함께 추가

#### utils/batch — Kotlin Coroutine 네이티브 경량 배치 프레임워크 모듈 추가 ([`0e9ae3096`](https://github.com/bluetape4k/bluetape4k-projects/commit/0e9ae3096))

- `No Spring`, `No runBlocking` 원칙의 코루틴 배치 실행 모델 제공
- `BatchReader` / `BatchProcessor` / `BatchWriter` chunk 파이프라인과 `BatchJob` / `BatchStep` / `BatchStepRunner` 코어 엔진 제공
- `BatchJobRepository` 기반 체크포인트 재시작, `SkipPolicy` / `RetryPolicy`, `batchJob {}` / `step {}` DSL 포함
- `BatchJob`의 `SuspendWork` 구현으로 `utils/workflow`와 직접 통합
- `ExposedJdbcBatchJobRepository` / `ExposedR2dbcBatchJobRepository` 및 JDBC/R2DBC Reader/Writer 제공

#### utils/rule-engine — Janino/Groovy 스크립트 엔진 추가 ([`833e31a39`](https://github.com/bluetape4k/bluetape4k-projects/commit/833e31a39))

기존 Kotlin Script / MVEL / SpEL 기반 규칙 실행기에 Janino와 Groovy 엔진 선택지를 추가했습니다.

- `io.bluetape4k.rule.engines.janino.*`: `ExpressionEvaluator` / `ScriptEvaluator` 기반 컴파일형 Java 표현식 엔진
- `io.bluetape4k.rule.engines.groovy.*`: `GroovyShell` + `NullSafeBinding` 기반 동적 스크립트 엔진
- 엔진별 `Condition` / `Action` / `Rule` / `Support` 타입 추가
- Janino/Groovy 예제·회귀 테스트 추가 및 README/README.ko 선택 가이드 보강

#### data/exposed-cache — 공통 캐시 인터페이스 모듈 추가 (구 `exposed-redis-api`) ([`d969a78e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/d969a78e0))

`exposed-redis-api`를 `exposed-cache`로 리네이밍하고, DB + 캐시 조합의 공통 인터페이스를 정비한 모듈입니다.

**공통 캐시 인터페이스**
- `JdbcCacheRepository` / `SuspendedJdbcCacheRepository`: JDBC 기반 동기·코루틴 캐시 레포지토리
- `R2dbcCacheRepository`: R2DBC 기반 리액티브 캐시 레포지토리
- `JdbcRedisRepository` / `SuspendJdbcRedisRepository` / `R2dbcRedisRepository`: Redis 백엔드 전용 서브인터페이스 (`invalidateByPattern`)
- `LocalCacheConfig`: Caffeine 로컬 캐시 설정 data class (TTL, 최대 크기)
- `testFixtures`: Read/Write-Through/Behind 시나리오 공유 테스트 인프라

#### data/exposed-jdbc-caffeine — JDBC + Caffeine 로컬 캐시 모듈 추가 ([`d969a78e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/d969a78e0))

- `JdbcCaffeineRepository`: JDBC + Caffeine 동기 로컬 캐시 (Read-Through / Write-Through / Write-Behind)
- `SuspendedJdbcCaffeineRepository`: 위와 동일, suspend 코루틴 버전
- Write-Behind: Channel 기반 비동기 쓰기 큐, `CoroutineScope` 생명주기 관리
- H2 인메모리 DB 기반 테스트 36개 (2 skipped — AutoInc Write-Behind)

#### data/exposed-r2dbc-caffeine — R2DBC + Caffeine AsyncCache 모듈 추가 ([`d969a78e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/d969a78e0))

- `R2dbcCaffeineRepository`: Caffeine `AsyncCache` 기반 suspend 로컬 캐시 (R2DBC, runBlocking 없음)
- Read-Through / Write-Through / Write-Behind 전략 지원
- H2 인메모리 R2DBC 기반 테스트 18개 (1 skipped — AutoInc Write-Behind)

#### data/exposed-r2dbc — R2DBC 커넥션 풀 DSL 추가 ([`db91dd6af`](https://github.com/bluetape4k/bluetape4k-projects/commit/db91dd6af))

`io.bluetape4k.exposed.r2dbc.pool` 패키지에 커넥션 풀 구성을 위한 Kotlin DSL을 제공합니다.

- `R2dbcPoolConfig`: 커넥션 풀 설정 data class (스마트 기본값: CPU×8, 최소 100, TTL·타임아웃 등)
- `R2dbcConnectionConfig`: 커넥션 옵션 DSL 빌더 — 표준 r2dbc-spi 옵션 타입-안전 프로퍼티 + `option()` 드라이버 확장
- `connectionPoolOf(options) { }` / `connectionPoolOf(factory) { }` / `ConnectionFactoryOptions.toConnectionPool { }`
- `connectionFactoryOptionsOf { }` / `connectionFactoryOf { }` — DSL 람다 방식
- `connectionFactoryOptionsOf(url)` / `connectionFactoryOf(url)` — R2DBC URL 파싱 방식
- `r2dbcConnectionPool { connection { } pool { } }` — 연결·풀 설정 통합 DSL
- `r2dbcConnectionPool(url) { }` — URL + 풀 설정 간결 방식
- `R2dbcPoolConfig.toConnectionPoolConfiguration(factory)` 변환 유틸

### Changed

#### io/feign — 기본 구현 클래스와 상수 이름 정리 ([`11ec8881a`](https://github.com/bluetape4k/bluetape4k-projects/commit/11ec8881a))

- Feign 기본 구현 인스턴스 생성을 `Encoder.Default()` / `Decoder.Default()` / `Retryer.Default()`에서 `DefaultEncoder()` / `DefaultDecoder()` / `DefaultRetryer()`로 정리
- 잘못된 상수명 `JAVA_CLASS_VERION` → `JAVA_CLASS_VERSION`, `MILLIS_IN_DAY` → `MillisPerDay` 수정
- 관련 회귀 테스트와 오타성 테스트 코드 함께 정리

#### testing/testcontainers — Pulsar client API 및 의존성 버전 갱신 ([`75a1f84db`](https://github.com/bluetape4k/bluetape4k-projects/commit/75a1f84db))

- `testing/testcontainers`에 `pulsar-client-api` 의존성 추가
- Pulsar 3.3.9, Elasticsearch 9.3.3, CockroachDB 25.4.8로 관련 버전 업데이트
- `PulsarServer`, `ElasticsearchServer`, `CockroachServer` 연관 빌드 설정 동기화

#### spring-boot4/batch-exposed — Spring Batch 6.0 deprecated API 제거 ([`0c0bc8412`](https://github.com/bluetape4k/bluetape4k-projects/commit/0c0bc8412))

- `JobLauncherTestUtils` → `JobOperatorTestUtils`, `launchJob()` → `startJob()`로 테스트 유틸 정리
- `chunk(n, tm)` 호출을 `chunk(n).transactionManager(tm)` 패턴으로 전환
- Boot 3 대비 누락되었던 integration/benchmark 테스트 4종 추가

#### utils/idgenerators — kotlinx-benchmark 성능 측정 추가 ([`cb38152b0`](https://github.com/bluetape4k/bluetape4k-projects/commit/cb38152b0))

- `SingleThreadIdGeneratorBenchmark`, `ConcurrentIdGeneratorBenchmark` 추가
- `Benchmark.md`에 7개 ID 생성기 성능 비교와 컬러 바 차트 문서화

#### data/exposed-cache — 4개 Redis 캐시 모듈 인터페이스 통일 ([`b7311fccb`](https://github.com/bluetape4k/bluetape4k-projects/commit/b7311fccb), [`1385f8c41`](https://github.com/bluetape4k/bluetape4k-projects/commit/1385f8c41))

- `exposed-jdbc-lettuce` / `exposed-r2dbc-lettuce` / `exposed-jdbc-redisson` / `exposed-r2dbc-redisson` 4개 모듈 인터페이스 및 테스트 구조 표준화
- Lettuce fat 인터페이스 → `JdbcRedisRepository` / `R2dbcRedisRepository` 슬림 인터페이스로 분리
- `invalidateByPattern()`: Redis 전용 서브인터페이스로 분리 (공통 캐시 인터페이스에서 제거)
- `RedissonCacheConfig.name` 프로퍼티 추가

#### bluetape4k-core — `HasIdentifier` Deprecate ([`ac61e2864`](https://github.com/bluetape4k/bluetape4k-projects/commit/ac61e2864))

- `HasIdentifier` 인터페이스 `@Deprecated` 처리 — `java.io.Serializable` 직접 구현 권장
- 분산 캐시 직렬화는 `Serializable` + `serialVersionUID` 패턴으로 통일

#### bluetape4k-coroutines — `Deferred` 확장 함수 추가 ([`a52702e75`](https://github.com/bluetape4k/bluetape4k-projects/commit/a52702e75))

- `Deferred.zip(other)`: 두 `Deferred` 결과를 `Pair`로 결합
- `Deferred.zipWith(other, transform)`: 두 결과를 변환 함수로 합성

### Fixed

#### utils/workflow — suspend 워크플로 취소 전파 보강 ([`514b8c4d8`](https://github.com/bluetape4k/bluetape4k-projects/commit/514b8c4d8))

- `SuspendSequentialFlow`, `SuspendParallelFlow`, `SuspendRepeatFlow`, `SuspendRetryFlow`, `SuspendConditionalFlow`에서 `CancellationException` 전파를 일관되게 보장
- 실행 모델 비교 benchmark 및 README 설명 보강

#### utils/states — 종료 상태 조회 일관성 수정 ([`6a66dc0e7`](https://github.com/bluetape4k/bluetape4k-projects/commit/6a66dc0e7))

- 종료 상태에서도 `canTransition()` / `allowedEvents()`가 전이 가능해 보이던 불일치 수정
- 동기/코루틴 상태 머신 회귀 테스트 추가

#### utils/rule-engine — suspend 규칙 엔진 취소 전파 보강 ([`be865e94b`](https://github.com/bluetape4k/bluetape4k-projects/commit/be865e94b))

- `DefaultSuspendRuleEngine`의 `fire()` / `check()` 경로에서 코루틴 취소를 정상 전파하도록 수정
- 회귀 테스트 및 README 설명 갱신

#### bluetape4k-coroutines — `Flow.log()` / `AsyncFlow.log()` 로그 미출력 버그 수정 ([`f56b00a27`](https://github.com/bluetape4k/bluetape4k-projects/commit/f56b00a27), [`8c9a96681`](https://github.com/bluetape4k/bluetape4k-projects/commit/8c9a96681))

- `Flow.log()` 연산자가 실제 로그를 출력하지 않던 문제 수정
- `AsyncFlow.log()` 동일 버그 수정

#### data/exposed-cache — R2DBC Write-Behind 타이밍·UNIQUE 제약 위반 수정 ([`86fa30e83`](https://github.com/bluetape4k/bluetape4k-projects/commit/86fa30e83))

- Write-Behind 비동기 큐 플러시 타이밍 경쟁 조건 수정
- UUID 기반 테스트 테이블 추가로 AutoInc UNIQUE 제약 위반 방지

#### infra/redisson — `RedissonCacheConfig` bluetape4k-patterns 위반 수정 ([`4cffbcc2b`](https://github.com/bluetape4k/bluetape4k-projects/commit/4cffbcc2b), [`5d6da26af`](https://github.com/bluetape4k/bluetape4k-projects/commit/5d6da26af))

- stdlib `require()` → `requirePositiveNumber` / `requireGe` 등 bluetape4k 확장함수로 교체
- `validateUnsupportedMapSettings` 내부 상태 검증은 `check()` 유지 (패턴 원칙에 맞게 복원)

---

## [1.5.0] - 2026-04-05

### Added

#### utils/workflow — Kotlin DSL 워크플로 모듈 추가 ([`685e25a4d`](https://github.com/bluetape4k/bluetape4k-projects/commit/685e25a4d))

j-easy/easy-flows에서 영감을 받아 Kotlin 2.3 + 코루틴 + Virtual Threads로 완전 재작성한 워크플로 엔진입니다.

**API**
- `WorkStatus`: `COMPLETED` / `FAILED` / `PARTIAL` / `CANCELLED` / `ABORTED` (5종)
- `WorkReport` sealed interface: `Success` / `Failure` / `PartialSuccess` / `Cancelled` / `Aborted`
- `WorkContext`: `ConcurrentHashMap` 기반, `compute()` 원자적 read-modify-write 지원
- `RetryPolicy`: 지수 백오프, `maxAttempts`(총 시도 횟수), `maxRetries` 편의 프로퍼티
- `ParallelPolicy`: `ALL`(ShutdownOnFailure) / `ANY`(ShutdownOnSuccess)
- `ErrorStrategy`: `STOP`(return) / `CONTINUE`(continue) — `ABORTED`는 ErrorStrategy 무관 break

**동기 플로우 5종 (Virtual Threads)**
- `SequentialWorkFlow`: 순차 실행, `CONTINUE` 전략 시 `PartialSuccess` 반환
- `ParallelWorkFlow`: `StructuredTaskScopes` 기반, `joinUntil(deadline)` 타임아웃, ALL/ANY 정책
- `ConditionalWorkFlow`: predicate 기반 then/otherwise 분기
- `RepeatWorkFlow`: `repeatWhile` / `until` 조건 + `maxIterations` 상한
- `RetryWorkFlow`: 지수 백오프 재시도

**코루틴 플로우 5종 (suspend)**
- `SuspendSequentialFlow` / `SuspendParallelFlow` / `SuspendConditionalFlow` / `SuspendRepeatFlow` / `SuspendRetryFlow`
- `SuspendParallelFlow` ANY 정책: Channel 기반 첫 성공 즉시 반환
- `workReportFlow()` / `executeAsFlow()`: Flow 스트리밍 지원

**DSL**
- `sequentialFlow {}` / `parallelFlow {}` / `conditionalFlow {}` / `repeatFlow {}` / `retryFlow {}`
- `suspendSequentialFlow {}` 등 코루틴 변형 전체 제공
- `parallelAllFlow {}` / `parallelAnyFlow {}` + nested `parallelAll` / `parallelAny`
- 단일 루트 강제: `require(rootWork == null)`

**테스트**: 173개 전체 통과, 주문처리 실무 예제 2개 (동기/코루틴)

#### utils/states — 코루틴 기반 유한 상태 머신(FSM) 모듈 추가 ([`3e9be7d25`](https://github.com/bluetape4k/bluetape4k-projects/commit/3e9be7d25))

- `BaseStateMachine` / `StateMachine` / `SuspendStateMachineInterface` 3계층 인터페이스 (시그니처 충돌 방지)
- `DefaultStateMachine`: `AtomicReference` CAS 기반 Thread-safe 동기 FSM
- `SuspendStateMachine`: `Mutex` + `MutableStateFlow` 기반 코루틴 FSM + 상태 관찰
- `stateMachine {}` / `suspendStateMachine {}` DSL + `on<E>()` 헬퍼 + Guard 조건 지원
- `TransitionBuilder`: Guard 조건 람다 DSL
- clinic-appointment Map 기반 O(1) 전이 패턴 채택
- 테스트 35개 (Turnstile, Order, Appointment, GuardedTransition)

#### utils/rule-engine — Kotlin DSL 기반 규칙 엔진 모듈 추가 ([`69de83742`](https://github.com/bluetape4k/bluetape4k-projects/commit/69de83742))

- `Rule` / `SuspendRule` / `Facts`(`ConcurrentHashMap`) / `RuleEngine` / `SuspendRuleEngine` 핵심 인터페이스
- `rule {}` / `suspendRule {}` DSL + `@Rule` / `@Condition` / `@Action` 어노테이션 + `RuleProxy` 변환
- `CompositeRule`: `ActivationRuleGroup` / `ConditionalRuleGroup` / `UnitRuleGroup`
- `InferenceRuleEngine`: 전방 추론(Forward Chaining) 지원
- Expression Engine: MVEL2 / SpEL / Kotlin Script (`BasicJvmScriptingHost`, 컴파일 캐시)
- Rule Reader: YAML / JSON / HOCON 파일에서 규칙 로드
- 테스트 72개 통과

#### testing/testcontainers — 신규 서버 8종 추가 ([`3b0e5af8`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b0e5af8))

- `Neo4jServer`: Neo4j 그래프 DB, Bolt/HTTP 포트, `bolt-url` 프로퍼티 export
- `MemgraphServer`: Memgraph 그래프 DB, `bolt-port`/`log-port`/`bolt-url` export
- `PostgreSQLAgeServer`: PostgreSQL + Apache AGE 그래프 확장
- `ToxiproxyServer`: 카오스 테스트용 네트워크 프록시, `control-port`/`control-url` export, latency·bandwidth toxic 주입 테스트 구현
- `TrinoServer`: 분산 SQL 쿼리 엔진
- `WireMockServer`: HTTP stub/mock 서버, stale 커넥션 자동 재시도 (`resetAll`)
- `KeycloakServer`: Keycloak 인증 서버, `auth-url`/`admin-username`/`admin-password` export
- `InfluxDBServer`: InfluxDB 2.x 시계열 DB, `admin-token`/`organization`/`bucket` export

#### testing/testcontainers — `PropertyExportingServer` 계약 강화 ([`cc0d7204`](https://github.com/bluetape4k/bluetape4k-projects/commit/cc0d7204))

- `PropertyExportingServer` 인터페이스: `propertyKeys()` / `properties()` / `registerSystemProperties()` / `writeToSystemProperties()` 통일
- 프로퍼티 키 명명 규칙을 **kebab-case 소문자**로 통일 (`bootstrapServers` → `bootstrap-servers`, `bolt.url` → `bolt-url` 등)
- `withCompatKeys()`: kebab-case 키 추가 시 구 camelCase 키도 병행 등록 (하위 호환)

#### data/exposed-trino — Trino JDBC Dialect 모듈 추가 ([`28dab07f`](https://github.com/bluetape4k/bluetape4k-projects/commit/28dab07f), [`7816a3ca`](https://github.com/bluetape4k/bluetape4k-projects/commit/7816a3ca))

- `TrinoDatabase`: Trino JDBC 연결 팩토리 (`jdbc:trino://`)
- `suspendTransaction` / `queryFlow`: 코루틴 기반 Trino 쿼리 API
- autocommit 전용 (Trino는 트랜잭션 미지원)
- `testcontainers`의 `TrinoServer`와 연동 테스트 포함

### Changed

#### data/exposed-trino, exposed-bigquery, exposed-duckdb — Codex 개선 적용 ([`4ce6750b`](https://github.com/bluetape4k/bluetape4k-projects/commit/4ce6750b), [`c50998bf`](https://github.com/bluetape4k/bluetape4k-projects/commit/c50998bf))

- API 일관성·KDoc·테스트 코드 정리

### Fixed

- `virtualthread/api` — `StructuredTaskScopeAll.joinUntil(Instant)` 메서드 추가, `Jdk21AllScope` 구현: `ParallelWorkFlow` 타임아웃이 실제로 동작하지 않던 문제 수정 ([`685e25a4d`](https://github.com/bluetape4k/bluetape4k-projects/commit/685e25a4d))
- `virtualthread/jdk25` — `Jdk25AllScope.joinUntil(Instant)` 구현: JDK 25 `StructuredTaskScope`에 `joinUntil` 없음 → 스케줄러로 스레드 인터럽트 후 `TimeoutException` 변환
- `WireMockServer.resetAll()`: Apache HttpClient 5 stale 커넥션으로 인한 `NoHttpResponseException` 발생 시 클라이언트 재생성 후 1회 재시도 ([`c4adae7d`](https://github.com/bluetape4k/bluetape4k-projects/commit/c4adae7d))
- `ZooKeeperServer`: Curator 연결 타임아웃 안정화 — `RetryOneTime(1000)` + `blockUntilConnected(10s)` 추가 (IPv6→IPv4 폴백 대응) ([`0d05542d`](https://github.com/bluetape4k/bluetape4k-projects/commit/0d05542d))
- `ToxiproxyServer`: `useDefaultPort` 시 `exposeCustomPorts()` 누락 수정, KDoc 프로퍼티 키 `control.port` → `control-port` ([`a46226b8`](https://github.com/bluetape4k/bluetape4k-projects/commit/a46226b8))

---

## [1.5.0-RC1] - 2026-04-01

### Added

#### utils/science — GIS 공간 데이터 처리 모듈 신규 추가 ([`a32d243b`](https://github.com/bluetape4k/bluetape4k-projects/commit/a32d243b))

debop4k-science를 Kotlin 2.3 + 최신 라이브러리로 완전 재작성한 `bluetape4k-science` 모듈입니다.

**coords 패키지 — 좌표계 및 변환**
- `GeoLocation(latitude, longitude)`: Haversine 거리 계산 (`distanceTo`)
- `BoundingBox`: 경계 박스 + `relationTo(other)` (DISJOINT/INTERSECTS/CONTAINS/WITHIN), JTS `Envelope` 변환
- `DM` / `DMS`: 도분/도분초 data class + `CoordConverters` (Degree ↔ DM ↔ DMS 왕복 변환)
- `UtmZoneSupport`: UTM Zone/Band 결정(`utmZoneOf`), 위도 밴드 `I·O` 제외 로직, `UtmZone.boundingBox()`

**geometry 패키지 — JTS 기반 공간 연산**
- `GeometryOperations`: 두 점 간 각도·거리, 선분 교차점, 위경도 유효성 검증
- `PolygonExtensions`: JTS `Polygon` 넓이(㎡), 무게중심, `BoundingBox` 변환

**projection 패키지 — Proj4J 기반 좌표계 변환**
- `Projections`: `utmToWgs84()`, `wgs84ToUtm()`, `transform(sourceCrs, targetCrs, coord)` — 임의 EPSG CRS 변환
- `CrsRegistry` (internal): EPSG 코드 기반 CRS 캐시 (`ConcurrentHashMap`)

**shapefile 패키지 — GeoTools 31.6 기반 Shapefile 처리**
- `ShapefileReader`: `loadShape(file)` (동기), `loadShapeAsync(file)` (`withContext(Dispatchers.IO)` 래핑)
- `ShapeModels`: `ShapeHeader`, `ShapeAttribute`, `ShapeRecord`, `Shape` — GeoTools 타입을 public API에 노출하지 않음
- `ShapefileExtensions`: `toGeoLocations()`, `filterByBoundingBox()`, `filterByAttribute()`, `computeBoundingBox()`

**exposed 패키지 — PostGIS DB 적재 파이프라인**
- `SpatialLayerTable` / `SpatialFeatureTable`: `AuditableLongIdTable` 상속, `geoGeometry()` + `jacksonb<Map<String,Any?>>()` JSONB
- `PoiTable`: POI 지점 저장 (`geoPoint()` + `jacksonb<Map<String,Any?>>()`)
- `NetCdfFileTable` / `NetCdfGridValueTable`: 테이블 DDL (UCAR 구현은 Phase 4 보류)
- `SpatialLayerRepository` / `SpatialFeatureRepository`: `LongJdbcRepository` 기반 CRUD
- `ShapefileImportService.importShapefile()`: Shapefile → PostGIS 배치 적재 (1,000건/트랜잭션, `ensureActive()`, JTS→WKT→PostGIS 변환)
- `NetCdfFileRepository` / `NetCdfCatalogService`: 구조 정의 (NetCDF 읽기는 Phase 4 후 구현)

> **[!NOTE]**
> GeoTools는 **LGPL** 라이선스로 `compileOnly`로만 선언됩니다. 빌드 스크립트에 OsGeo Maven 저장소 추가 필요:
> ```kotlin
> maven("https://repo.osgeo.org/repository/release/")
> ```

#### data/exposed-postgresql — GeoGeometryColumnType 추가

- `GeoGeometryColumnType`: 모든 PostGIS geometry 타입(Point/Polygon/LineString/MultiPolygon 등)을 수용하는 generic 컬럼 타입 ([`a32d243b`](https://github.com/bluetape4k/bluetape4k-projects/commit/a32d243b))
- `Table.geoGeometry(name)` 확장함수
- `ST_Distance(geography)`, `ST_DWithin(geography)`, `ST_Intersects`, `ST_Contains`, `ST_Within` Expression 클래스 추가

### Changed

#### aws-kotlin — 클라이언트 생성/해제 패턴 통일 ([`af247f65`](https://github.com/bluetape4k/bluetape4k-projects/commit/af247f65))

- 모든 서비스에 `xxxClientOf` + `withXxxClient` 팩토리 함수 쌍을 `*Support.kt`로 분리
  - 신규 파일: `KinesisClientSupport`, `SesClientSupport`, `SesV2ClientSupport`, `SnsClientSupport`, `SqsClientSupport`
- `withXxxClient`를 `xxxClientOf(...).useSafe { }` 패턴으로 통일 — 코루틴 취소·예외 시 자동 `close()` 보장
- `httpClient` 기본값을 `HttpClientEngineProvider.defaultHttpEngine`(CRT)으로 전체 통일
- `*Extensions.kt`에서 팩토리/with 함수 제거 — 확장 함수만 유지

> **[!NOTE]**
> AWS Kotlin SDK 클라이언트는 내부 HTTP 커넥션 풀·스레드를 보유합니다.
> 사용 후 반드시 `close()`를 호출하거나, **`withXxxClient { }` 블록을 사용하면 자동으로 리소스가 해제**됩니다.

### Fixed

- mutiny 테스트 병렬 실행 비활성화 ([`e0082a82`](https://github.com/bluetape4k/bluetape4k-projects/commit/e0082a82))
- JUnit Jupiter 병렬 실행 비활성화 ([`70cd469e`](https://github.com/bluetape4k/bluetape4k-projects/commit/70cd469e))

---

## [1.5.0-Beta3] - 2026-03-31

### Added

#### spring-boot3/4 — Exposed Spring Data JDBC/R2DBC Repository 이관

- **`bluetape4k-spring-boot3-exposed-jdbc`** experimental → projects 이관 ([`08c6711a`](https://github.com/bluetape4k/bluetape4k-projects/commit/08c6711a))
  - `ExposedJdbcRepository<E, ID>`: PartTree 쿼리 자동 생성, QBE(Query By Example), Pageable/Sort 지원
  - `SimpleExposedJdbcRepository`: `findAll`, `findById`, `save`, `saveAll`, `deleteById`, `count`, `exists`
  - `ExposedQueryCreator`: 메서드명 기반 WHERE 절 자동 생성 (`findByName`, `findByPriceLessThan` 등)
  - `@EnableExposedJdbcRepositories`: Auto-Configuration 진입점
- **`bluetape4k-spring-boot4-exposed-jdbc`**: Spring Boot 4 BOM(`platform()`) 적용 동일 기능 제공

- **`bluetape4k-spring-boot3-exposed-r2dbc`** experimental → projects 이관 ([`08c6711a`](https://github.com/bluetape4k/bluetape4k-projects/commit/08c6711a))
  - `ExposedR2dbcRepository<T, ID>`: suspend CRUD (`findAll`, `findByIdOrNull`, `save`, `deleteById`), Flow 지원
  - `SimpleExposedR2dbcRepository`: `toDomain`, `toPersistValues`, `extractId` 오버라이드 패턴
  - `@EnableExposedR2dbcRepositories`: Auto-Configuration 진입점
- **`bluetape4k-spring-boot4-exposed-r2dbc`**: Spring Boot 4 BOM 적용 동일 기능 제공

#### spring-boot3/4 — Exposed Spring Data 데모 앱 추가

- **`bluetape4k-spring-boot3-exposed-jdbc-demo`** / **`bluetape4k-spring-boot4-exposed-jdbc-demo`** ([`3b3b2729`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b3b2729))
  - Exposed DAO + Spring Data JDBC + Spring MVC CRUD 데모 (H2 in-memory, 검색 API 포함)
- **`bluetape4k-spring-boot3-exposed-r2dbc-demo`** / **`bluetape4k-spring-boot4-exposed-r2dbc-demo`** ([`3b3b2729`](https://github.com/bluetape4k/bluetape4k-projects/commit/3b3b2729))
  - Exposed R2DBC + suspend Repository + Spring WebFlux CRUD 데모 (H2 R2DBC in-memory)

#### spring-boot3/4 — Hibernate Lettuce NearCache Auto-Configuration 이관

- **`bluetape4k-spring-boot3-hibernate-lettuce`** experimental → projects 이관 ([`1f0eae55`](https://github.com/bluetape4k/bluetape4k-projects/commit/1f0eae55))
  - `LettuceNearCacheHibernateAutoConfiguration`: YAML 프로퍼티 → `HibernatePropertiesCustomizer` 자동 변환
  - `LettuceNearCacheSpringProperties`: `bluetape4k.cache.lettuce-near.*` 바인딩 — codec, useResp3, local(Caffeine), redisTtl(region별 오버라이드)
  - `LettuceNearCacheMetricsAutoConfiguration`: Micrometer Gauge — `lettuce.nearcache.active.regions`, `lettuce.nearcache.total.local.size`
  - `LettuceNearCacheActuatorAutoConfiguration`: `GET /actuator/nearcache`, `GET /actuator/nearcache/{region}` 엔드포인트
  - 13개 테스트 케이스 전체 통과 (ApplicationContextRunner 단위 + Testcontainers Redis 통합)
- **`bluetape4k-spring-boot4-hibernate-lettuce`**: Spring Boot 4 BOM 적용 (HibernatePropertiesCustomizer 패키지 `org.springframework.boot.hibernate.autoconfigure`로 변경) ([`93d9d10e`](https://github.com/bluetape4k/bluetape4k-projects/commit/93d9d10e))

#### spring-boot3/4 — Hibernate Lettuce NearCache 데모 앱 추가

- **`bluetape4k-spring-boot3-hibernate-lettuce-demo`** / **`bluetape4k-spring-boot4-hibernate-lettuce-demo`** ([`0c85c9e5`](https://github.com/bluetape4k/bluetape4k-projects/commit/0c85c9e5), [`1ebdaa3c`](https://github.com/bluetape4k/bluetape4k-projects/commit/1ebdaa3c))
  - `Product` JPA 엔티티 (`@Cacheable`, `@Cache(NONSTRICT_READ_WRITE)`) + Spring Data JPA
  - `ProductController`: CRUD REST API (`/api/products`)
  - `CacheController`: L1 캐시 통계 조회/evict API (`/api/cache/stats`, `/api/cache/evict`)
  - 6개 통합 테스트 (Testcontainers Redis + H2)

#### data/hibernate-cache-lettuce — Hibernate 2nd Level Cache + Lettuce NearCache 신규 추가

- **`bluetape4k-hibernate-cache-lettuce`** experimental → projects 이관 ([`de7cf96d`](https://github.com/bluetape4k/bluetape4k-projects/commit/de7cf96d))
  - `LettuceNearCacheRegionFactory`: `RegionFactoryTemplate` 상속, Redis 클라이언트/코덱 초기화, region별 `StorageAccess` 생성
  - `LettuceNearCacheStorageAccess`: `DomainDataStorageAccess` 구현, Caffeine(L1) + Redis(L2) 2-tier 캐시 브릿지, 복합키/NaturalId 키 정규화
  - `LettuceNearCacheProperties`: Hibernate properties 파싱, 15가지 코덱 지원(Fory/Kryo × 5가지 압축), region별 TTL 오버라이드
  - 14개 테스트 클래스, 58개 테스트 케이스 전체 통과 (Testcontainers Redis + H2)

#### data/exposed-bigquery — Google BigQuery REST API 통합 신규 추가

- **`bluetape4k-exposed-bigquery`** experimental → projects 이관 ([`d7acd494`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7acd494))
  - `BigQueryContext`: Exposed DSL → H2(PostgreSQL 모드) SQL 생성 후 BigQuery REST API 실행. SELECT/INSERT/UPDATE/DELETE/DDL 지원
  - `BigQueryQueryExecutor`: `toList()`, `toListSuspending()`, `toFlow()`, 페이지네이션 자동 처리
  - `BigQueryResultRow`: Column 참조 기반 타입 안전 행 접근 (Long, BigDecimal, Instant 등)
  - `BigQueryDialect`: `PostgreSQLDialect` 상속 BigQuery 전용 다이얼렉트
  - `BigQueryEmulator`: 로컬 에뮬레이터(포트 9050) 자동 감지, 없으면 Testcontainers 자동 기동

#### data/exposed-duckdb — DuckDB JDBC 통합 신규 추가

- **`bluetape4k-exposed-duckdb`** experimental → projects 이관 ([`d7acd494`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7acd494))
  - `DuckDBDialect`: `PostgreSQLDialect` 상속 DuckDB 전용 다이얼렉트 + `DuckDBDialectMetadata` (FK 제약 캐싱 no-op)
  - `DuckDBDatabase`: 인메모리/파일/읽기전용 연결 팩토리 (`object`)
  - `DuckDBConnectionWrapper`: JDBC 1.1.3 `prepareStatement` 오버로드 호환 래퍼
  - `suspendTransaction`: `Dispatchers.IO` 기반 suspend 트랜잭션 확장 함수
  - `queryFlow`: 대용량 결과셋 `Flow<T>` 스트리밍 확장 함수

#### data/exposed-postgresql — PostgreSQL 전용 Exposed 확장 신규 추가

- **`bluetape4k-exposed-postgresql`** 신규 추가 ([`06c5087f`](https://github.com/bluetape4k/bluetape4k-projects/commit/06c5087f))
  - PostGIS 공간 데이터 컬럼 타입 — `POINT`, `POLYGON` (H2 fallback 지원)
  - pgvector 벡터 검색 컬럼 타입 — `VECTOR(n)`, 유사도 검색 (`<->`, `<#>`, `<=>`)
  - TSTZRANGE 시간 범위 컬럼 타입

#### data/exposed-mysql8 — MySQL 8.0 GIS 전용 Exposed 확장 신규 추가

- **`bluetape4k-exposed-mysql8`** experimental → projects 이관 ([`5a9e32d7`](https://github.com/bluetape4k/bluetape4k-projects/commit/5a9e32d7))
  - GIS 공간 데이터 컬럼 타입 8종: `POINT`, `LINESTRING`, `POLYGON`, `MULTIPOINT`, `MULTILINESTRING`, `MULTIPOLYGON`, `GEOMETRYCOLLECTION`, `GEOMETRY`
  - JTS(Java Topology Suite) 기반 `Geometry` 컬럼 타입
  - 공간 함수: `ST_Contains`, `ST_Distance`, `ST_AsText`, `ST_GeomFromText` 등
  - MySQL Internal Format WKB 자동 변환

#### data/exposed-core — inet/phone 컬럼 타입 통합

- **`exposed-inet`, `exposed-phone`** 모듈을 `exposed-core`로 이관 ([`dd520942`](https://github.com/bluetape4k/bluetape4k-projects/commit/dd520942))
  - `inetAddress`, `cidr` 컬럼 타입 + PostgreSQL `<<` 네트워크 포함 연산자
  - `phoneNumber`, `phoneNumberString` 컬럼 타입 (libphonenumber opt-in)
  - 기존 별도 모듈 의존성 → `bluetape4k-exposed-core`로 통합

#### infra/lettuce — 확률적 자료구조 추가

- **BloomFilter / CuckooFilter**: Redis Lua 스크립트 기반 구현 (RedisBloom 서버 확장 불필요) ([`4a3d0fb9`](https://github.com/bluetape4k/bluetape4k-projects/commit/4a3d0fb9))
  - `BloomFilter<E>`: `add`, `mightContain`, `clear` — 지정 오류율·최대 항목 수 기반 비트배열 크기 자동 계산
  - `CuckooFilter<E>`: `add`, `mightContain`, `delete` 지원 — BloomFilter 대비 삭제 연산 지원
- **HyperLogLog**: PFADD/PFCOUNT/PFMERGE 래핑 ([`4a3d0fb9`](https://github.com/bluetape4k/bluetape4k-projects/commit/4a3d0fb9))
  - `HyperLogLog<E>`: `add`, `count`, `merge` — 대용량 카디널리티 근사 계산 (표준오차 ≈ 0.81%)

#### data/exposed — Auditable 감사 추적 패턴 추가

- **`exposed-core`**: `Auditable` 인터페이스 + `UserContext` (ScopedValue/ThreadLocal 듀얼 전략) + `AuditableIdTable` 베이스 테이블 추가 ([`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca))
- **`exposed-dao`**: `AuditableEntity` (`flush()` 오버라이드로 createdBy/updatedBy 자동 설정) + `AuditableEntityClass` DAO 추가 ([`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca))
- **`exposed-jdbc`**: `AuditableJdbcRepository` (`auditedUpdateById`/`auditedUpdateAll` — updatedAt/updatedBy DB CURRENT_TIMESTAMP 자동 설정) 추가 ([`207bcbca`](https://github.com/bluetape4k/bluetape4k-projects/commit/207bcbca))
- **`exposed-core/dao`**: ULID 커스텀 ID 지원 추가 (`UlidIdTable`, `UlidEntity`) ([`cd345b11`](https://github.com/bluetape4k/bluetape4k-projects/commit/cd345b11))

#### data/hibernate — Hibernate 6.6 NaturalId 확장

- **`Session`/`EntityManager`** 용 `bySimpleNaturalId`, 복합 NaturalId helper 추가 ([`5e7e7f00`](https://github.com/bluetape4k/bluetape4k-projects/commit/5e7e7f00))
- `ConcreteProxy`, embeddable inheritance 매핑 회귀 테스트 추가

#### io/tink — Redis Key Rotation 지원

- Versioned Keyset + Lettuce/Redisson 기반 Redis 키셋 저장소 추가 ([`02fe8621`](https://github.com/bluetape4k/bluetape4k-projects/commit/02fe8621))
- `TinkJsonProtoKeysetFormat` 기반 키셋 직렬화로 deprecated API 제거

#### io/http — MockWebServer 헤더 지연 헬퍼 추가

- **`enqueueBodyWithHeadersDelay`** 확장 함수 추가 — headers delay 기반 취소 테스트 안정화 ([`2a15d515`](https://github.com/bluetape4k/bluetape4k-projects/commit/2a15d515))

#### utils/idgenerators — Uuid 생성기 교체

- 테스트 코드 전반의 `TimebasedUuid` → `Uuid` 클래스 교체 완료 ([`db55831f`](https://github.com/bluetape4k/bluetape4k-projects/commit/db55831f))
  - `Uuid.V7` (EpochTimebased), `Uuid.V1` (DefaultTimebased), `Uuid.V6` (Reordered), `Uuid.V4` (Random), `Uuid.V5` (Namebased)

### Fixed

#### core/coroutines

- **`startCollectOn`**: upstream `launch` 의 `CancellationException` 을 error 로 변환하지 않도록 수정 ([`03c396e6`](https://github.com/bluetape4k/bluetape4k-projects/commit/03c396e6))
- **`DeferredValue.value`** / **`SuspendRingBuffer.iterator`** deprecated 처리 — blocking 계약 명확화 ([`712d7cbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/712d7cbf))
- **`PublishSubject.emitError(null)`** 종료 계약 정리 ([`712d7cbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/712d7cbf))
- **`firstCompleted`**: 첫 완료 기준으로 의미를 정렬하고 `firstSucceeded` 분리 ([`b9c55f5d`](https://github.com/bluetape4k/bluetape4k-projects/commit/b9c55f5d))

#### io/jackson

- **`JsonNode` 변환 로직**: `stringNode` 호출 수정 + `treeToValueOrNull()` `TreeNode` 캐스팅 명시 ([`97a8280e`](https://github.com/bluetape4k/bluetape4k-projects/commit/97a8280e))

#### io/retrofit2

- **Retry Call 재사용 버그**: 매 시도마다 `clone()` 된 Call 사용하도록 수정 ([`cbe6eeca`](https://github.com/bluetape4k/bluetape4k-projects/commit/cbe6eeca))

#### infra/resilience4j

- **Coroutine 예외 계약**: `CompletionStage recover` null cause 및 동기 예외 복구 정리, `CancellationException` 전파 보장 ([`d2b32b60`](https://github.com/bluetape4k/bluetape4k-projects/commit/d2b32b60))

#### infra/bucket4j

- **`RateLimitResult`**: 음수 값 불변식 추가 + error 결과 진단 메시지 보존 강화 ([`62b3d39d`](https://github.com/bluetape4k/bluetape4k-projects/commit/62b3d39d))

#### infra/kafka

- **`SuspendKafkaConsumerTemplate`**: subscribe/assign/commit/seek 관리 기능 추가, 종료 시 `CoroutineScope` 취소 보장 ([`aedc6e44`](https://github.com/bluetape4k/bluetape4k-projects/commit/aedc6e44))

#### infra/cache

- **`NearCacheResilienceConfig`** / **`HazelcastNearCacheConfig`**: 입력 제약 추가 ([`188163b4`](https://github.com/bluetape4k/bluetape4k-projects/commit/188163b4))
- **`RedissonNearCacheConfig`**: TTL/idle 입력 제약 추가 ([`79db098f`](https://github.com/bluetape4k/bluetape4k-projects/commit/79db098f))
- **`LettuceCacheConfig`**: 생성 시점 입력 제약 검증 추가 ([`d4cbd1dd`](https://github.com/bluetape4k/bluetape4k-projects/commit/d4cbd1dd))

#### data/hibernate-cache-lettuce

- **`LettuceNearCacheProperties`**: `Serializable` 처리 + 미지원 codec 즉시 검증 ([`3a061b72`](https://github.com/bluetape4k/bluetape4k-projects/commit/3a061b72))

#### data/exposed

- **`exposed-r2dbc`**: `ON CONFLICT DO NOTHING` PostgreSQL 표준 SQL로 일반화 ([`b9c0b838`](https://github.com/bluetape4k/bluetape4k-projects/commit/b9c0b838))
- **`exposed-r2dbc`**: 페이징 계약 강화 ([`e53024c1`](https://github.com/bluetape4k/bluetape4k-projects/commit/e53024c1))
- **`exposed-jdbc`**: 배치 조회 시 쿼리 조건 보존 수정 ([`b0a73d13`](https://github.com/bluetape4k/bluetape4k-projects/commit/b0a73d13))
- **`exposed lettuce loader/writer`**: `loadAllKeys` PK 오름차순 고정 + `chunkSize` 입력 검증 추가 ([`5fb405e0`](https://github.com/bluetape4k/bluetape4k-projects/commit/5fb405e0))
- **`exposed-postgresql`**: TSTZRANGE fractional seconds 파싱 수정 ([`d7607df2`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7607df2))
- **`exposed-mysql8`**: Geometry literal 경로를 표준 WKB 기반으로 수정 ([`d7607df2`](https://github.com/bluetape4k/bluetape4k-projects/commit/d7607df2))

#### utils/geo

- **`GeoHashCircleQuery`**: 음수 반경 즉시 거부 검증 추가 ([`a4d555cc`](https://github.com/bluetape4k/bluetape4k-projects/commit/a4d555cc))

#### utils/idgenerators

- **UUID 시퀀스 API**: Base62 시퀀스 `size` 검증 일관성 정렬 ([`17f509b8`](https://github.com/bluetape4k/bluetape4k-projects/commit/17f509b8))

### Changed

#### 의존성 버전 업데이트

- `vertx`: 4.5.25 → 4.5.26
- `aws2`: 2.42.15 → 2.42.23
- `aws2_crt`: 0.43.8 → 0.44.0
- `aws_kotlin`: 1.6.18 → 1.6.46
- `aws_smithy_kotlin`: 1.6.2 → 1.6.7
([`42600939`](https://github.com/bluetape4k/bluetape4k-projects/commit/42600939))

#### infra/redisson

- **`RedissonCodecs`** deprecated 객체 제거 ([`52d84d11`](https://github.com/bluetape4k/bluetape4k-projects/commit/52d84d11))

---

## [1.5.0-Beta2] - 2026-03-21

### Added

#### utils/idgenerators — ULID 통합

- **`bluetape4k-ulid` 실험 모듈 → `bluetape4k-idgenerators`에 통합** ([`a40a3392`](https://github.com/bluetape4k/bluetape4k-projects/commit/a40a3392))
  - `ULID` interface + `ULIDFactory` / `ULIDMonotonic` / `ULIDStatefulMonotonic` 구현체 마이그레이션 (`io.bluetape4k.ulid` → `io.bluetape4k.idgenerators.ulid`)
  - `UlidGenerator`: `ULID.StatefulMonotonic` 기반 `IdGenerator<String>` 어댑터 추가
  - `JavaUUIDSupport` / `KotlinUuidSupport` 확장 함수 포함
  - 동시성 테스트 추가: `MultithreadingTester`, `StructuredTaskScopeTester`, `SuspendedJobTester`

#### utils/idgenerators — Ksuid/Snowflake 어댑터

- **`KsuidGenerator`**: `Ksuid.Generator` 전략을 주입받는 `IdGenerator<String>` 어댑터 추가 ([`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))
- **`SnowflakeGenerator`**: `Snowflake` 구현체를 주입받는 `IdGenerator<Long>` 어댑터 추가 ([`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))
- **`Snowflakers.default(machineId)`** / **`Snowflakers.global()`** 팩토리 함수 추가 ([`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))

### Changed

#### utils/idgenerators — ID 생성기 API 통일 (Uuid 패턴)

UUID, KSUID를 `object Uuid { interface Generator; object V1..V7 }` 패턴으로 통일 ([`945b7444`](https://github.com/bluetape4k/bluetape4k-projects/commit/945b7444), [`694a8340`](https://github.com/bluetape4k/bluetape4k-projects/commit/694a8340))

- **`object Uuid`**: `Uuid.Generator` interface + `Uuid.V1`/`V4`/`V5`/`V6`/`V7` nested objects
  - `Uuid.random(random)` — 커스텀 Random V4 생성기
  - `Uuid.epochRandom(random)` — 커스텀 Random V7 생성기
  - `Uuid.namebased(name)` — 결정론적 V5 생성기
  - 인코딩 `Url62.encode()` 로 통일 (`nextBase62()`, `nextBase62s(size)`)
- **`object Ksuid`**: `Ksuid.Generator` interface + `Ksuid.Seconds`(초 기반) / `Ksuid.Millis`(밀리초 기반) nested objects
- **`UuidGenerator(generator: Uuid.Generator = Uuid.V7)`**: `IdGenerator<UUID>` 어댑터
- 기존 `TimebasedUuidGenerator`, `RandomUuidGenerator`, `NamebasedUuidGenerator` → `@Deprecated(WARNING)` + `ReplaceWith` 유지
- 기존 `KsuidMillis` → `@Deprecated(WARNING)` + `Ksuid.Millis` 위임으로 하위 호환 유지

#### 마이그레이션 — deprecated 사용처 신규 API로 교체

`TimebasedUuid.Epoch` → `Uuid.V7`, `KsuidMillis` → `Ksuid.Millis` 교체 완료 ([`7163e35e`](https://github.com/bluetape4k/bluetape4k-projects/commit/7163e35e), [`4d9b712c`](https://github.com/bluetape4k/bluetape4k-projects/commit/4d9b712c))

- `spring-boot3/4/StopWatchSupport`, `utils/jwt/KeyChain`, `examples/coroutines-demo`
- `aws/aws/DynamoDbEntity` (`TimebasedUuidGenerator` → `Uuid.V7.nextBase62()`)
- `data/exposed-core/ColumnExtensions`, `data/hibernate`, `data/exposed-jdbc/r2dbc` 테스트 인프라
- `infra/cache-core` 테스트

---

### Added (Beta1)

#### spring-boot4 — Spring Boot 4.x 전용 모듈 신규 추가

- **`bluetape4k-spring-boot4-core`**: WebFlux/RestClient Coroutines DSL (`suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, `suspendDelete`), Jackson 2 ObjectMapper 커스터마이저, Retrofit2 통합, WebClient/WebTestClient 확장
- **`bluetape4k-spring-boot4-data-redis`**: Spring Data Redis 고성능 직렬화 (`RedisBinarySerializer`, `RedisCompressSerializer`, `redisSerializationContext {}` DSL)
- **`bluetape4k-spring-boot4-r2dbc`**: Spring Data R2DBC 코루틴 확장 (`XyzSuspending` 패턴)
- **`bluetape4k-spring-boot4-mongodb`**: Spring Data MongoDB Reactive 코루틴 확장, Criteria/Query/Update infix DSL
- **`bluetape4k-spring-boot4-cassandra`**: Spring Data Cassandra 코루틴 확장
- **`bluetape4k-spring-boot4-cassandra-demo`**: Cassandra + Spring Data Cassandra 종합 예제

> Spring Boot 4 BOM은 `implementation(platform(...))` 방식으로 적용 (`dependencyManagement { imports }` 방식은 KGP 2.3.x와 충돌)

### Changed

#### spring-boot3, spring-boot4 — Deprecated 함수 제거

- `spring-boot3/r2dbc`, `spring-boot4/r2dbc`: `suspend*` 접두사 deprecated 래퍼 함수 제거 (총 18개)
  - 제거: `suspendFindOneById`, `suspendSelectOne`, `suspendInsert`, `suspendUpdate`, `suspendDelete`, `suspendCount`, `suspendExists` 등
- `spring-boot3/cassandra`, `spring-boot4/cassandra`: `suspend*`/`co*` 접두사 deprecated 래퍼 함수 제거 (총 130개 이상)
  - 제거: `suspendQuery`, `coQuery`, `suspendExecute`, `suspendSelectOne`, `suspendInsert`, `suspendUpdate`, `suspendDelete` 등
  - 사용처 모두 `XyzSuspending` 형식으로 교체 완료

### Fixed

- `gradle.properties`에서 deprecated `kotlin.incremental.useClasspathSnapshot=false` 속성 제거 (KGP 2.3.x에서 불필요)



#### infra/cache-core — JCache 기반 NearCache

- **`NearJCache<K, V>`**: Caffeine front + JCache back 2-Tier 동기 NearCache (`JCache<K,V>` 위임) ([
  `0a09c19d`](https://github.com/bluetape4k/bluetape4k-projects/commit/0a09c19d))
- **`SuspendNearJCache<K, V>`**: Caffeine front + SuspendJCache back 코루틴 NearCache ([
  `0a09c19d`](https://github.com/bluetape4k/bluetape4k-projects/commit/0a09c19d))
- **`NearJCacheConfig<K, V>`** + **`NearJCacheConfigBuilder`** + **`nearJCacheConfig {}`** DSL ([
  `b19b48b9`](https://github.com/bluetape4k/bluetape4k-projects/commit/b19b48b9))
- `AbstractNearCacheOperationsTest` / `AbstractSuspendNearCacheOperationsTest` 동시성 테스트 추가 ([
  `a4c0bf14`](https://github.com/bluetape4k/bluetape4k-projects/commit/a4c0bf14))
- `ResilientNearCacheDecorator` 단위 테스트 추가 ([
  `054be42a`](https://github.com/bluetape4k/bluetape4k-projects/commit/054be42a))

#### infra/cache-lettuce — 팩토리 확장

- **`LettuceCaches.suspendJCache()`**: `LettuceSuspendJCache<V>` 팩토리 ([
  `0b0ebbf6`](https://github.com/bluetape4k/bluetape4k-projects/commit/0b0ebbf6))
- **`LettuceCaches.nearJCache()`**: DSL/Config 오버로드로 `NearJCache<K,V>` 생성 ([
  `0b0ebbf6`](https://github.com/bluetape4k/bluetape4k-projects/commit/0b0ebbf6))
- **`LettuceCaches.suspendNearJCache()`**: DSL/Config 오버로드로 `SuspendNearJCache<K,V>` 생성 ([
  `0b0ebbf6`](https://github.com/bluetape4k/bluetape4k-projects/commit/0b0ebbf6))

#### README 최신화

- 통합 모듈별 README.md 신규 작성: `spring/boot3`, `vertx`, `aws`, `aws-kotlin`, `utils/geo`
- `io/jackson2`, `io/jackson3` — 바이너리(CBOR, Ion, Smile, Avro, Protobuf) 및 텍스트(YAML, CSV, TOML, Properties) 포맷 지원 섹션 추가

### Changed

#### 모듈 리네이밍

- **`bluetape4k-jackson` → `bluetape4k-jackson2`** (디렉토리: `io/jackson` → `io/jackson2`)
  — `bluetape4k-jackson3`과의 버전 대칭을 위한 명시적 리네이밍
- **`bluetape4k-exposed-jackson` → `bluetape4k-exposed-jackson2`** (디렉토리: `data/exposed-jackson` → `data/exposed-jackson2`)

#### 모듈 통합 — io

- **`bluetape4k-jackson2`**: 구 `bluetape4k-jackson-binary`(CBOR, Ion, Smile, Avro, Protobuf) + `bluetape4k-jackson-text`(YAML, CSV, TOML, Properties) 통합 ([`9ca9b975`](https://github.com/bluetape4k/bluetape4k-projects/commit/9ca9b975), [`35d32eb0`](https://github.com/bluetape4k/bluetape4k-projects/commit/35d32eb0))
- **`bluetape4k-jackson3`**: 구 `bluetape4k-jackson3-binary` + `bluetape4k-jackson3-text` 통합 ([`b3415a0f`](https://github.com/bluetape4k/bluetape4k-projects/commit/b3415a0f))

#### 모듈 통합 — utils

- **`bluetape4k-geo`**: 구 `bluetape4k-geocode`(Bing/Google) + `bluetape4k-geohash` + `bluetape4k-geoip2`(MaxMind) 통합 ([`84553efe`](https://github.com/bluetape4k/bluetape4k-projects/commit/84553efe))

#### 모듈 통합 — spring

- **`bluetape4k-spring-boot3`** (`spring/boot3`): 구 `spring/core` + `spring/webflux` + `spring/retrofit2` + `spring/tests` + `spring/jpa` 통합 ([`9f0b5fa2`](https://github.com/bluetape4k/bluetape4k-projects/commit/9f0b5fa2))

#### 모듈 통합 — vertx

- **`bluetape4k-vertx`**: 구 `vertx/core` + `vertx/sqlclient` + `vertx/resilience4j` 통합 ([`a0ba94ad`](https://github.com/bluetape4k/bluetape4k-projects/commit/a0ba94ad))

#### 모듈 통합 — aws

- **`bluetape4k-aws`**: 구 `aws/core`, `aws/dynamodb`, `aws/s3`, `aws/ses`, `aws/sns`, `aws/sqs`, `aws/kms`, `aws/cloudwatch`, `aws/kinesis`, `aws/sts` 통합 (22개 → 2개) ([`f2c36d53`](https://github.com/bluetape4k/bluetape4k-projects/commit/f2c36d53))
- **`bluetape4k-aws-kotlin`**: 구 `aws-kotlin/core`, `aws-kotlin/dynamodb`, `aws-kotlin/s3`, `aws-kotlin/ses`, `aws-kotlin/sesv2`, `aws-kotlin/sns`, `aws-kotlin/sqs`, `aws-kotlin/kms`, `aws-kotlin/cloudwatch`, `aws-kotlin/kinesis`, `aws-kotlin/sts` 통합 ([`f2c36d53`](https://github.com/bluetape4k/bluetape4k-projects/commit/f2c36d53))

#### infra/cache — 일관성 리팩토링

- **`LettuceBinaryCodec` 통일**: 팩토리 파라미터의 `BinarySerializer` → `LettuceBinaryCodec<V>` 교체 ([
  `598c88c0`](https://github.com/bluetape4k/bluetape4k-projects/commit/598c88c0))
- **`LettuceSuspendCacheManager`**: 미사용 파라미터 실제 활용으로 개선 ([
  `495f5330`](https://github.com/bluetape4k/bluetape4k-projects/commit/495f5330))
- **`RedissonCaches`**: 팩토리 네이밍 통일 ([`a10df979`](https://github.com/bluetape4k/bluetape4k-projects/commit/a10df979))
- **`HazelcastCaches.nearJCache/suspendNearJCache`**: 파라미터 2개로 축소 + DSL 지원 ([
  `7f7cdb52`](https://github.com/bluetape4k/bluetape4k-projects/commit/7f7cdb52))
- `JCache NearCache`를 `nearcache.jcache` 서브패키지로 이동 ([
  `f405197b`](https://github.com/bluetape4k/bluetape4k-projects/commit/f405197b))

### Deprecated

#### io/crypto

- **`bluetape4k-crypto`**: Jasypt 기반 암호화 모듈 Deprecated — `bluetape4k-tink` (Google Tink AEAD)로 대체 ([
  `38a05c26`](https://github.com/bluetape4k/bluetape4k-projects/commit/38a05c26))

#### io/okio

- **Cipher/Jasypt Sink/Source**: `io/okio` 모듈에서 cipher 및 jasypt 관련 클래스 제거 — `io/tink`의 `TinkEncryptSink`/
  `TinkDecryptSource` 사용 권장 ([`27edccc5`](https://github.com/bluetape4k/bluetape4k-projects/commit/27edccc5))

#### utils-deprecated

- **`ahocorasick`**: `utils/` → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`lingua`**: `utils/` → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`naivebayes`**: `utils/` → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`mutiny-examples`**: 예제성 모듈 → `utils-deprecated/` 이동, 빌드 제외 ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))

### Removed

- 구 서브모듈 소스 파일 정리 (`jackson-binary/text`, `jackson3-binary/text`, `geocode`, `geohash`, `geoip2`, `vertx/core`, `vertx/sqlclient`, `vertx/resilience4j`, aws 개별 서브모듈) ([`c7fb930c`](https://github.com/bluetape4k/bluetape4k-projects/commit/c7fb930c))
- **`TiDBServer`**: 테스트 인프라에서 TiDB Testcontainers 지원 제거 ([
  `bf617426`](https://github.com/bluetape4k/bluetape4k-projects/commit/bf617426))
- **예제성 모듈 제거**: 사용 빈도 낮은 예제 모듈 빌드에서 제외 (`utils-deprecated/`, `x-obsoleted/` 이동) ([
  `2cdfacf4`](https://github.com/bluetape4k/bluetape4k-projects/commit/2cdfacf4))
- **`bloomfilter`**: 사용 빈도 낮아 `x-obsoleted/bloomfilter`로 이동 ([
  `8b30555c`](https://github.com/bluetape4k/bluetape4k-projects/commit/8b30555c))
- **`captcha`**: 사용 빈도 낮아 `x-obsoleted/captcha`로 이동 ([
  `8b30555c`](https://github.com/bluetape4k/bluetape4k-projects/commit/8b30555c))
- **`logback-kafka`**: 사용 빈도 낮아 `x-obsoleted/logback-kafka`로 이동 ([
  `8b30555c`](https://github.com/bluetape4k/bluetape4k-projects/commit/8b30555c))
- **`nats`**: 사용 빈도 낮아 `x-obsoleted/nats`로 이동
- **`javers`**: 사용 빈도 낮아 `x-obsoleted/javers`로 이동
- **`tokenizer`**: 사용 빈도 낮아 `x-obsoleted/tokenizer`로 이동

### Fixed

#### utils/javatimes

- `MinPeriodTime` / `MaxPeriodTime` import 누락 수정 ([`1962525d`](https://github.com/bluetape4k/bluetape4k-projects/commit/1962525d))

#### infra/kafka

- `StringKafkaCodec` deserializer 인코딩 키 버그 수정 ([`ec7d0d99`](https://github.com/bluetape4k/bluetape4k-projects/commit/ec7d0d99))

#### data/cassandra

- `CqlDuration` nano 파트 변환 버그 수정 ([`e13d043e`](https://github.com/bluetape4k/bluetape4k-projects/commit/e13d043e))

#### utils/geo

- `bluetape4k-geo` 소스 디렉토리 경로 수정 ([`873e64e3`](https://github.com/bluetape4k/bluetape4k-projects/commit/873e64e3))

### Chores

- 전 모듈 코드 리뷰: KDoc 보강, `!!` 남용 패턴 개선, `requireNotNull` 중복 제거 (javatimes, geoip2, geohash, logback-kafka, math, naivebayes, vertx, spring-core, aws-kotlin 등)
- CLAUDE.md 모듈 구조 섹션 업데이트 (모듈 통합 반영) ([`4d111851`](https://github.com/bluetape4k/bluetape4k-projects/commit/4d111851))

---

## [1.4.0] - 2026-03-12

> **Full diff**: [`1.3.0...1.4.0`](https://github.com/bluetape4k/bluetape4k-projects/compare/1.3.0...1.4.0)

### Added

#### infra/lettuce

- **`LettuceIntCodec`**: Int 값을 4바이트 big-endian으로 직렬화하는 `RedisCodec<String, Int>` (Redisson `IntegerCodec`과 바이너리 호환) ([`1277dbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/1277dbfc))
- **`LettuceLongCodec`**: Long 값을 8바이트 big-endian으로 직렬화하는 `RedisCodec<String, Long>` (Redisson `LongCodec`과 바이너리 호환) ([`1277dbf`](https://github.com/bluetape4k/bluetape4k-projects/commit/1277dbfc))
- **`LettuceLeaderElection`** / **`LettuceSuspendLeaderElection`**: 분산 리더 선출 ([`17063567`](https://github.com/bluetape4k/bluetape4k-projects/commit/17063567))
- **`LettuceLeaderGroupElection`** / **`LettuceSuspendLeaderGroupElection`**: 분산 그룹 리더 선출 ([`17063567`](https://github.com/bluetape4k/bluetape4k-projects/commit/17063567))
- **`LeaderElectionOptions`** / **`LeaderGroupElectionOptions`**: `bluetape4k-leaders` 모듈 옵션 클래스 ([`5a026a2`](https://github.com/bluetape4k/bluetape4k-projects/commit/5a026a2a))
- Lettuce cache contracts 강화 및 Redis 8 테스트 추가 ([`19339945`](https://github.com/bluetape4k/bluetape4k-projects/commit/19339945))

#### infra/cache-lettuce

- **`LettuceMemoizer<K, V>`**: `LettuceMap<V>` 기반 동기 메모이제이션 (`Memoizer<K,V>` 인터페이스) ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))
- **`LettuceAsyncMemoizer<K, V>`**: `LettuceMap<V>` 기반 비동기 메모이제이션 (`AsyncMemoizer<K,V>` 인터페이스) ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))
- **`LettuceSuspendMemoizer<K, V>`**: `LettuceMap<V>` 기반 suspend 메모이제이션 (`SuspendMemoizer<K,V>` 인터페이스) ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))

#### infra/redisson

- **`RedissonMemoizer`** / **`AsyncRedissonMemoizer`** / **`RedissonSuspendMemoizer`**: `Memoizer<K,V>` 인터페이스 구현체 추가 ([`a6baef8`](https://github.com/bluetape4k/bluetape4k-projects/commit/a6baef84))

#### cache-core (testFixtures)

- **`AbstractMemoizerTest`** / **`AbstractAsyncMemoizerTest`** / **`AbstractSuspendMemoizerTest`**: Memoizer 공통 테스트 기반 클래스 ([`384311a`](https://github.com/bluetape4k/bluetape4k-projects/commit/384311a1))

#### cache-hazelcast / cache-ignite

- `HazelcastMemoizer`, `IgniteMemoizer` 등 `Memoizer<K,V>` 인터페이스 구현 추가 ([`384311a`](https://github.com/bluetape4k/bluetape4k-projects/commit/384311a1))
- cache 모듈별 **Factory object** 추가 및 JCache SPI 정리 ([`5c7ec82`](https://github.com/bluetape4k/bluetape4k-projects/commit/5c7ec829))

#### io/protobuf (신규 모듈)

- `io/grpc`에서 Protobuf 유틸리티 분리 → `io/protobuf` 독립 모듈 ([`63abe48`](https://github.com/bluetape4k/bluetape4k-projects/commit/63abe486))

#### io/okio (신규 모듈)

- `io/io` 모듈의 okio 패키지 → `io/okio` 독립 모듈로 분리 ([`5b92c2d`](https://github.com/bluetape4k/bluetape4k-projects/commit/5b92c2dd))

### Changed

#### infra/lettuce

- **분산 Primitive 클래스명 변경** (`Redis*` → `Lettuce*`): `RedisMap` → `LettuceMap`, `RedisSuspendMap` → `LettuceSuspendMap`, `RedisAtomicLong` → `LettuceAtomicLong`, `RedisSemaphore` → `LettuceSemaphore`, `RedisLock` → `LettuceLock` 등 ([`92625766`](https://github.com/bluetape4k/bluetape4k-projects/commit/92625766))
- **`LettuceMap<V>`**: `syncCommands` 접근자 `protected` 변경, `putTtl` / `putAllTtl` 메서드 추가 ([`5949f7a`](https://github.com/bluetape4k/bluetape4k-projects/commit/5949f7a4))
- Memoizer 기능: `infra/lettuce` → `infra/cache-lettuce` 이동, `LettuceMemoizer<K:Any, V:Any>` Generic화 ([`6b2b1aa`](https://github.com/bluetape4k/bluetape4k-projects/commit/6b2b1aa6))
- 분산 Primitive 리팩토링 및 동시성 테스트 강화 ([`5949f7a`](https://github.com/bluetape4k/bluetape4k-projects/commit/5949f7a4))

#### infra/redis (모듈 분리)

- `bluetape4k-redis` 단일 모듈 → `lettuce` + `redisson` + `spring-data-redis` 3개 모듈로 분리 ([`953321b`](https://github.com/bluetape4k/bluetape4k-projects/commit/953321bf))
- `awaitSuspending()`: 자체 구현 → `kotlinx.coroutines.future.await`로 대체 ([`221c6d5`](https://github.com/bluetape4k/bluetape4k-projects/commit/221c6d51))

#### utils/leader

- leader local election flows 단순화 (불필요한 중간 state 제거) ([`5ad9477`](https://github.com/bluetape4k/bluetape4k-projects/commit/5ad9477c))
- `LeaderElectionOptions` / `LeaderGroupElectionOptions` 옵션 클래스 lettuce/redisson 모듈에 적용 ([`647ed27`](https://github.com/bluetape4k/bluetape4k-projects/commit/647ed272))

#### bluetape4k-core

- 핵심 유틸리티 코드 단순화 리팩토링 ([`0c5fa93`](https://github.com/bluetape4k/bluetape4k-projects/commit/0c5fa939))

#### infra/cache-core

- **`SuspendNearCache.clear()`**: front/back cache 모두 clear 완료 후 info 로그 추가 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### infra/cache-lettuce

- **`ResilientLettuceSuspendNearCacheTest`**: write-behind 동기화 timeout 3초 → 5초로 증가 (테스트 안정성 개선) ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### infra/cache-redisson

- **`RedisSuspendNearCacheTest`**: Redisson `DEFAULT_EXPIRY_CHECK_PERIOD`(30s)를 고려한 clearAll timeout 30→40초 조정, `untilSuspending`에 `atMost` 명시 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### infra/nats

- **`ServerPoolExample`**: `@TestInstance(PER_CLASS)` + `@BeforeAll`/`@AfterAll` 방식으로 NATS 서버 관리 개선, 순차 기동으로 Docker 레이스 컨디션 방지 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

### Removed

#### infra/cache-ignite2

- **`cache-ignite2` 모듈 완전 제거**: cache umbrella에서 ignite2 의존성 삭제, 모듈 소스 및 테스트 전체 삭제 ([`4972b98`](https://github.com/bluetape4k/bluetape4k-projects/commit/4972b985))
  - Apache Ignite 2.x 지원 종료 (Ignite 3.x `cache-ignite`로 대체)

#### examples

- vertx 관련 예제 제거 (미사용) ([`fa64ee3`](https://github.com/bluetape4k/bluetape4k-projects/commit/fa64ee3c))

### Fixed

#### infra/lettuce

- **`LettuceIntCodec` / `LettuceLongCodec`**: `decodeValue`에서 absolute read → `bytes.duplicate()` 방식으로 수정 (caller position 불변) ([`d59db75`](https://github.com/bluetape4k/bluetape4k-projects/commit/d59db750))
- **`LettuceJCaching`**: 기본값 `localhost:6379` 에 자동 연결되는 문제 수정 ([`7074c27`](https://github.com/bluetape4k/bluetape4k-projects/commit/7074c27c))

#### infra/cache-lettuce

- **`LettuceAsyncMemoizer`**: `thenApply` 내부 blocking sync 호출 → `thenCompose` + `getAsync()`로 교체 ([`d59db75`](https://github.com/bluetape4k/bluetape4k-projects/commit/d59db750))
- **`LettuceAsyncMemoizer.clear()`**: dangling future 방지를 위한 `inFlight.clear()` 제거 ([`d59db75`](https://github.com/bluetape4k/bluetape4k-projects/commit/d59db750))
- **`ResilientLettuceSuspendNearCache.close()`**: 채널을 먼저 닫고 `consumerJob.join()`으로 write-behind 커맨드 소진 대기 후 `scope.cancel()`하도록 종료 순서 개선 (커맨드 유실 방지) ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))
- `LettuceNearCache` / `RedissonNearCache` 기본 코덱을 LZ4 + Fory로 지정 ([`ee5de36`](https://github.com/bluetape4k/bluetape4k-projects/commit/ee5de362))
- Write-behind `DEFAULT_BATCH_SIZE` 변경 (100 → 500) ([`52320804`](https://github.com/bluetape4k/bluetape4k-projects/commit/52320804))

#### infra/cache-hazelcast

- **`HazelcastJCaching`**: 기본 포트(5701)에 무한 재시도하는 문제 수정 ([`ee63b31`](https://github.com/bluetape4k/bluetape4k-projects/commit/ee63b317))

#### infra/cache-ignite

- **`IgniteAsyncMemoizer`**: 비동기 API(`*Async()`)로 개선하여 ARM64 타임아웃 문제 해결 ([`3d96439`](https://github.com/bluetape4k/bluetape4k-projects/commit/3d96439e))
- `untilSuspending` 의 hang 방지 및 timeout 전파 수정 (root cause 보존) ([`12766771`](https://github.com/bluetape4k/bluetape4k-projects/commit/12766771))
- redisson 비동기 near cache 테스트를 `runSuspendIO`로 전환 ([`12766771`](https://github.com/bluetape4k/bluetape4k-projects/commit/12766771))
- ignite 동적 cache readiness 회귀 테스트 추가 ([`12766771`](https://github.com/bluetape4k/bluetape4k-projects/commit/12766771))

#### infra/cache-redisson

- **`RedissonSuspendCache`**: 모든 `*Async()` 호출의 `awaitSuspending()` → `kotlinx.coroutines.future.await()`로 교체 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### aws-kotlin (dynamodb / s3 / sqs / sts)

- `existsTable` 전체 페이지 순회 개선, s3 `exists/putAll/getAll` 보강 ([`aa5aecc`](https://github.com/bluetape4k/bluetape4k-projects/commit/aa5aecc4))
- sqs `receive` 기본 대기시간 결함 수정, sts `durationSeconds` 계약 검증 추가 ([`1b693960`](https://github.com/bluetape4k/bluetape4k-projects/commit/1b693960))

#### testing/testcontainers

- Testcontainers 1.21.4 → 2.0.3 업그레이드 안정화 (2.x 좌표 체계 반영) ([`1aaf713`](https://github.com/bluetape4k/bluetape4k-projects/commit/1aaf7130))
- **`NatsServer`**: NATS 이미지 태그 `2.10` → `2.12` 업그레이드 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))
- **`LocalStackServerTest`**: custom network 테스트 `@Disabled` 처리 (Ryuk 레이스 컨디션, Docker Desktop macOS 이슈) ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### bluetape4k-coroutines

- Coroutines 함수 호출 버그 수정 ([`ec0570c`](https://github.com/bluetape4k/bluetape4k-projects/commit/ec0570c8))

#### testing/junit5

- **`untilSuspending`**: `coroutineScope` → `withContext(Dispatchers.IO)` 변경으로 hang 방지 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))
- Awaitility 4.2+ 필드명 변경(`timeoutConstraint` → `waitConstraint`) 호환성 대응 ([`db4d25b`](https://github.com/bluetape4k/bluetape4k-projects/commit/db4d25b8))

#### 기타

- infra/nats: 관리 API 예외 계약 정리 ([`51ba07c`](https://github.com/bluetape4k/bluetape4k-projects/commit/51ba07c7))
- infra/bucket4j: probe 기반 잔여 토큰 계산 최적화 ([`4af6eba`](https://github.com/bluetape4k/bluetape4k-projects/commit/4af6ebac))
- infra/micrometer: Timer/Retrofit 계측 경로 회귀 보강 ([`740eab5`](https://github.com/bluetape4k/bluetape4k-projects/commit/740eab5e))
- infra/opentelemetry: Span/Coroutine helper 예외 처리 정리 ([`5c033fb`](https://github.com/bluetape4k/bluetape4k-projects/commit/5c033fb4))
- infra/resilience4j: 코드리뷰, KDoc 보강, README 최신화 ([`905554a`](https://github.com/bluetape4k/bluetape4k-projects/commit/905554a0))
- vertx/sqlclient: Transaction CancellationException 전파 보강, TupleMapper 리플렉션 캐시 도입 ([`e6a11a1`](https://github.com/bluetape4k/bluetape4k-projects/commit/e6a11a19))
- utils/tokenizer: Korean/NounTokenizer topN 검증, fallback offset 보정 최적화 ([`56aaa23`](https://github.com/bluetape4k/bluetape4k-projects/commit/56aaa23d))
- utils/javatimes: ISO nullable 포맷 정렬, 캘린더 주 동작 계약 정렬 및 범위 계약 강화 ([`ff943f5`](https://github.com/bluetape4k/bluetape4k-projects/commit/ff943f53))
- testing/junit5: 스트레스 테스터 실행 모델 개선 (대량 rounds 시 메모리 안정화) ([`3c6f1cb`](https://github.com/bluetape4k/bluetape4k-projects/commit/3c6f1cba))

### Chores

- ConsulServer Docker 이미지 `hashicorp/consul 1.20`으로 업데이트 ([`7aa5761`](https://github.com/bluetape4k/bluetape4k-projects/commit/7aa5761f))

---

## [1.3.0] - 2026-03-06

### Added

#### io/tink (신규 모듈)

- **`bluetape4k-tink`**: Google Tink 기반 현대적 암호화 모듈
    - AEAD: AES-GCM, ChaCha20-Poly1305, XChaCha20-Poly1305
    - DAEAD: AES-SIV (결정적 암호화)
    - MAC: HMAC-SHA256/SHA512
    - `TinkEncryptSink` / `TinkDecryptSource` (Okio 스트리밍 암복호화)

#### io/jackson, io/jackson3

- Google Tink 기반 JSON 필드 암호화 지원 (`@EncryptedJsonField`)

#### data/exposed

- **`bluetape4k-exposed-tink`**: Google Tink 기반 Exposed 암호화 컬럼 (AEAD/DAEAD)
- `kotlin.uuid.Uuid` 타입 지원 (exposed-core)

#### cache-core / cache-*

- **Resilient NearCache** (write-behind + retry + graceful degradation):
  `ResilientLettuceNearCache`, `ResilientLettuceSuspendNearCache`,
  `ResilientRedissonNearCache`, `ResilientRedissonSuspendNearCache`

#### io/serialization

- **`ForyBinarySerializer` / `KryoBinarySerializer`**: 보안 모드 (`secureFory` / `secure`) 추가

### Changed

#### cache-core / cache-* (모듈 통합)

- **10개 → 5개(+umbrella)로 통합**:
    - `cache-local` → `cache-core`에 병합
    - `cache-hazelcast-near` → `cache-hazelcast`에 병합
    - `cache-ignite-near` → `cache-ignite`에 병합
    - `cache-redisson-near` → `cache-redisson`에 병합
- `JCaching`의 Redisson/Hazelcast/Ignite 객체를 각 모듈로 분리

#### data/exposed

- Exposed 암호화 컬럼: `bluetape4k-crypto` → `bluetape4k-tink` 전환
- `AbstractValueObject.equalProperties/hashCode` → `abstract`으로 변경

#### build

- Gradle 9.3.1 → 9.4.0 업그레이드
- 전역 싱글톤 `unsafeLazy` → `lazy(SYNCHRONIZED)` 일괄 변경

### Fixed

- 암호화 모듈에서 민감 정보 로깅 제거 (보안)
- io/avro: codec 기본값·Snappy 매핑 정합화, reflect 안정성 개선
- redis memorizer: in-flight dedup 로직 추가로 동시 호출 중복 계산 방지
- CloudWatch Logs: `"logs"` 서비스 지정 누락 수정
- while-delay 구문 → `await-untilSuspending` 전환

---

## [1.2.3] - 2026-03-03

### Added

#### data/mongodb (신규 모듈)

- MongoDB Kotlin Coroutine Driver 기반 확장 모듈
    - `mongoClient {}` DSL, `MongoClientProvider`
    - `findFirst`, `exists`, `upsert`, `findAsFlow` 확장 함수
    - `documentOf {}` DSL, Aggregation Pipeline DSL (`pipeline {}`)

#### spring/mongodb (신규 모듈)

- Spring Data MongoDB Reactive 확장 (코루틴 기반)

#### aws / aws-kotlin

- **CloudWatch Metrics / Logs**: 메트릭 발행/조회, 로그 그룹/스트림/이벤트 (Java SDK + Kotlin SDK)
- **Kinesis**: 스트림 관리, 레코드 전송/조회 (Java SDK + Kotlin SDK)
- **STS**: GetCallerIdentity, AssumeRole, 세션 토큰 발급 (Java SDK + Kotlin SDK)

#### utils/leader

- **`LeaderGroupElection`** / **`SuspendLeaderGroupElection`**: 분산 그룹 리더 선출

#### data/exposed

- **`bluetape4k-exposed-jackson3`**: Jackson 3.x 기반 Exposed JSON 컬럼 지원

#### utils/measured

- Angle, Area, Volume, Temperature, Pressure, Storage, Frequency, Energy/Power, BinarySize, GraphicsLength 단위 추가

#### cache

- **Ignite3 NearCache JCache SPI** 구현 (CachingProvider, CacheManager)

### Changed

- KDoc 표준화 보강 (spring/aws/kafka/opentelemetry/micrometer/bucket4j/cache/redis/nats/resilience4j/jackson/feign/retrofit2 등 전 모듈)

---

## [1.2.2] - 2026-03-01

### Fixed

- Snapshot 배포(publish) 버그 수정

---

## [1.2.1] - 2026-02-28

### Changed

- Maven Central 배포 설정 변경

---

## [1.2.0] - 2026-02-28

### Added

#### aws / aws-kotlin

- **`bluetape4k-aws-kms`**: AWS KMS 암호화 키 관리 (Java SDK v2)
- **`bluetape4k-aws-kotlin-kms`**: AWS KMS Kotlin SDK 확장

#### infra/cache

- **Lettuce `SuspendCache`** 구현
- **cache-redisson-near / cache-hazelcast-near / cache-ignite-near** 전용 near cache 모듈 신설
- **Hazelcast/Ignite2 `SuspendCache`** 서버 연동 구현

#### data/exposed

- **`bluetape4k-exposed-measured`**: Exposed Custom ColumnType 및 DB 방언 매트릭스 테스트
- **`bluetape4k-measured`** 코어 모듈: Units/Measure 기반 단위 조합 연산 (Length/Time/Mass)
- Measured 확장: Angle, Area, Storage, Pressure, Volume, Temperature

### Changed

- cache 모듈을 core/provider 구조로 재편:
    - `bluetape4k-cache-core` (공통 API, NearCache, SuspendCache 추상화)
    - `cache-local/redisson/hazelcast/ignite` provider 분리
- `cache-core` testFixtures 도입 (Abstract 테스트 공통 재사용)
- Virtual Threads 예제 구조화 (part1/part2/part3)

---

## [1.1.0] - 2026-02-22

### Added

#### utils/virtualthread (신규 모듈)

- **Java 21/25 Virtual Threads** 지원 모듈 분리
    - `VirtualThreadExecutor`, `VirtualThreadDispatcher`
    - `StructuredTaskScope` API 추상화 (Jdk21/Jdk25 공용)

#### data/exposed (모듈 분리)

- `bluetape4k-exposed` → `exposed-core` + `exposed-dao` + `exposed-jdbc` 3개 모듈 분리
    - `exposed-core`: JDBC 불필요 핵심 (컬럼 타입, ID 확장, HasIdentifier, ExposedPage)
    - `exposed-dao`: DAO 엔티티 확장, 커스텀 IdTable
    - `exposed-jdbc`: JDBC Repository, SuspendedQuery, VirtualThreadTransaction
    - `exposed`: umbrella (하위 호환)

#### spring

- **WebClient** 설정 개선 및 테스트 보강
- **spring-tests**: HTTP 클라이언트 확장 함수 추가

### Changed

- `CloseableCoroutineScope` 도입 및 Scope 구현체 리팩토링
- Coroutines MDC 컨텍스트 처리 개선 (`logging` 모듈 의존성 변경)
- R2DBC 코루틴 확장 네이밍 정리
- Cassandra 코루틴 함수명 정리
- 리소스 종료(shutdown) 로직 안전성 개선

### Fixed

- `Ksuid`, `KsuidMillis`에서 사용하는 `BytesBase62` 버그 수정
- `javatimes` range/period 로직 및 테스트 보강
- images 스트림 유틸 개선

---

## [1.0.0] - 2026-02-03

### Added

- **Eclipse Collections** 적용 및 컬렉션 처리 전반 최적화
- **spring-tests**: `RestClient` / `WebClient` / `WebTestClient` 확장 함수 추가

### Changed

- Atomic 관련 구조: `kotlinx.atomicfu` 기반으로 통일
- UUID 대신 Base58로 키 생성 방식 변경
- `SuspendRingBuffer` 및 Parallel 코드 개선

---

## [0.1.7] - 2026-01-26

### Changed

- `ExposedRepository.batchUpdate` → `batchUpsert`로 변경 및 기능 개선
- `kotlinx-atomicfu` → Java standard atomics로 교체
- Exposed Entity `toStringBuilder` 명칭 변경

### Fixed

- `TimebasedUuid` Deprecated 메시지 오타 수정

---

## [0.1.6] - 2026-01-23

### Changed

- **`KLogging` → `KLoggingChannel`** 전환 (전 모듈, 코루틴 환경 로깅 개선)
- `TimebasedUuid` 생성 방식 변경 (Reordered 사용)
- Kotlin `Enum.entries` 활용으로 변경
- `CoLeaderElection` 인터페이스 제거 (사용 중단됨)

---

## [0.1.5] - 2026-01-09

의존성 업그레이드 및 내부 안정성 개선.

---

## [0.1.4] - 2026-01-09

### Added

#### io/jackson3 (신규 모듈)

- **`bluetape4k-jackson3`**: Jackson 3.x 기반 JSON 처리 및 확장
- **`bluetape4k-jackson3-binary`**: CBOR, Ion, Smile 포맷 직렬화
- **`bluetape4k-jackson3-text`**: CSV, Properties, YAML, TOML 포맷 지원

#### io/jackson

- **`@JsonMasker`**: JSON 필드 마스킹 직렬화 지원
- **`@JsonEncrypt`**: JSON 필드 암호화/복호화 직렬화 지원

#### data/exposed

- **`bluetape4k-exposed-jackson3`**: Jackson 3.x 기반 Exposed JSON 컬럼 지원

---

## [0.1.3] - 2025-09-27

의존성 업그레이드 및 내부 안정성 개선.

---

## [0.1.2] - 2025-09-26

### Changed

- **Fory Codec** 관련 Serializer 추가
- **Fury Serializer** Deprecated 처리 시작

---

## [0.1.0] - 2025-09-26

### Changed

- **Fury → Fory** 전체 교체 (`io.fury.*` → `org.apache.fory.*`)
- 대규모 의존성 버전 업그레이드 (Kotlin, Spring Boot, Exposed, Hibernate, Vert.x, AWS 등)
- Elasticsearch 8.18.x → 9.1.x 업그레이드
- `suspendedTransactionAsync`를 사용하여 DB 트랜잭션 처리 개선
- Spring DataBuffer 관련 확장 함수 추가

---

## [0.0.10] - 2025-06-11

### Changed

- `runSuspendTest` → `runSuspendIO` 리네이밍
- suspend 함수 리네이밍 전반 (`coXxx` → `xxxSuspending` 패턴 적용)
- 코루틴 기반 테스트 로직 개선
- Bloom Filter suspend 기반 구현 추가
- 코루틴 기반 Writer/City/Country 테스트 보강

---

## [0.0.9] - 2025-05-28

의존성 업그레이드 및 내부 안정성 개선.

---

## [0.0.8] - 2025-05-28

### Added

- **Exposed-R2DBC 확장**: `TableExtensions`, `QueryExtensions`, `ReadableExtensions`
- `BatchInsertOnConflictDoNothing` 지원
- R2DBC 기반 Redisson 캐싱 및 테스트

### Changed

- Exposed v1.0.0-beta-2 업그레이드 및 API 마이그레이션

### Fixed

- `findAll()` 함수 `List<T>` 반환 수정
- `findLastOrNull()` 함수 수정

---

## [0.0.7] - 2025-05-19

의존성 업그레이드.

---

## [0.0.6] - 2025-05-19

### Changed

- **`KLogging` → `KLoggingChannel`** 전환 시작 (코루틴 환경 로깅 개선)
- Fury 0.10.2 업그레이드 (Javers 예외 해결)

---

## [0.0.5] - 2025-03-25

### Added

- **`bluetape4k-fastjson2`**: Fastjson2 기반 직렬화 모듈
- **`bluetape4k-exposed-fastjson2`**: Exposed Fastjson2 JSON 컬럼 지원

### Fixed

- Pulsar, RabbitMQ, ZipKin 버전 다운그레이드 (안정성)
- Entity `equals`에 type 비교 추가
- `idEquals` 에서 backReferencedOn 관계 테이블 참조 수정

---

## [0.0.4] - 2025-03-11

### Added

- **`bluetape4k-exposed-jasypt`**: Jasypt 기반 Exposed 암호화 컬럼 지원
- **`bluetape4k-exposed-tests`**: Exposed 공통 테스트 인프라
- **`ExposedRepository`** / **`ExposedCoroutineRepository`**: 범용 Exposed Repository 패턴 구현
- Exposed Jackson 컬럼 (`jackson`, `jacksonb`) 구현
- `KsuidMillisTable` 추가

### Changed

- Gradle 8.13, Spring Boot 3.4.3, Exposed 0.60.0 업그레이드

---

## [0.0.2] - 2024-11-28

### Added

- **`bluetape4k-spring-r2dbc`**: Spring Data R2DBC 모듈
- **`bluetape4k-spring-tests`**: Spring 테스트 유틸리티 모듈
- **`Measurable` / `MeasurableUnit`** 인터페이스 도입 (단위 추상화 기반)
- Temperature, Angle 단위 추가

### Changed

- Kotlin 2.1.0 업그레이드
- Gradle 8.11.1 업그레이드
- Coroutines 용 Controller 정의

---

## [0.0.1] - 2024-11-22

### Added

초기 릴리즈. 다음 모듈 포함:

- **`bluetape4k-aws-kotlin`**: AWS Kotlin SDK 기반 (core, dynamodb, s3, ses, sns, sqs)
- **`bluetape4k-logback-kafka`**: Logback Kafka Appender
- **`bluetape4k-coroutines`**: Kotlin Coroutines 유틸리티
- Examples: Cassandra, JPA+QueryDSL, Coroutines, MongoDB, Spring Webflux, Vert.x, Redisson, Mutiny, MapStruct
