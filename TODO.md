# bluetape4k TODO

> 현재 버전: 1.7.0-SNAPSHOT | 브랜치: `develop` | 모듈 수: 132개
> 최종 업데이트: 2026-04-17

---

## 우선순위 분류

- 🔴 **High** — 릴리스 전 반드시 처리
- 🟡 **Medium** — 다음 마일스톤 대상
- 🟢 **Low** — 장기 개선 과제

---

## 1. 미완성 기능

### 1.1 utils/science — NetCdf 지원 완성 🟡

- [ ] `NetCdfCatalogService.kt` — `TODO("Phase 4 UCAR netcdfAll 완료 후 구현 예정")` 구현
  - `listLayers()`, `getLayer()`, `createLayer()` 등 미구현 메서드 완성
- [ ] `NetCdfTableTest.kt` — 테스트 케이스 완성
- [ ] UCAR netcdfAll 의존성 추가 후 전체 파이프라인 검증

### 1.2 examples/jpa-querydsl-demo — QueryDSL 쿼리 완성 🟢

- [ ] `MemberRepositoryImpl.kt` — `TODO("Not yet implemented")` 3개 구현
  - `findByName()`, `findByAgeGreaterThan()`, `findByNameContaining()` 완성

---

## 2. Deprecated 코드 정리

### 2.1 io 모듈 레거시 정리 🔴

- [x] `io/crypto/` — jasypt 기반 암호화 모듈 전체 삭제, `tink` 모듈로 대체 완료 (2026-04-17)
- [ ] `io/http/` — `AHC`(AsyncHttpClient), `OkHttp3`, `HC5` 레거시 HTTP 클라이언트 정리
  - Retrofit2도 SB3/4 core에서 이미 제거됨 — io 모듈도 정리 대상 검토
- [ ] `io/jackson2/`, `io/jackson3/` — deprecated 직렬화 API 정리

### 2.2 core 모듈 Deprecated 정리 🟡

- [x] `bluetape4k/core/` — `@Deprecated` 항목 전수 제거 완료 (2026-04-17)
  - Systemx, TimeSpec, DateSupport, StringSupport, NumberSupport, AutoCloseableSupport, EnumSupport, ExecutorSupport, StructuredTaskScopeSupport, ProgressionSupport, IterableSupport, SequenceSupport, QueueSupport, AnySupport, ArraySupport, ApacheConstructorUtils 등 총 26개 항목 제거

### 2.3 infra 모듈 정리 🟡

- [ ] `infra/` — 12개 deprecated 파일 검토
  - 레거시 캐시, 큐 연동 API 정리

---

## 3. testing/testcontainers — HazelcastServer 수정 🔴

- [ ] `HazelcastServer.kt` — deprecated Hazelcast API 4개 수정
  - `Config`, `NetworkConfig`, `JoinConfig`, `TcpIpConfig` 최신 API로 교체
  - Hazelcast 5.x 호환성 확보

---

## 4. x-obsoleted 처리 계획 🟡

14개 레거시 모듈 전수 조사 완료 (2026-04-20). 실무 가치 기준으로 재분류.

### 4.1 🔴 승격 강력 추천 — 구현 충실도 높고 실무 수요 큼

- [ ] **javers → data/javers-eventsourcing** (74 kt 파일, 3 서브모듈, **전략적 최우선**)
  - Event Sourcing / CQRS / DDD Aggregate 변경 추적의 정석 라이브러리
  - Hibernate Envers 대비 장점: 비정규화 snapshot, 명시적 commit, Unit of Work 불필요
  - **이미 5종 백엔드 구현**: Caffeine, Cache2K, Lettuce, Redisson, Kafka
  - JPA/Hibernate 없이도 동작 — **Exposed와 결합 시 DDD 친화적**
  - Kafka 백엔드 = Event Sourcing 기반 CQRS에 그대로 활용 가능
  - 상세 계획은 하단 §11 참조

- [ ] **nats → infra/nats** (30 kt 파일, **최우선**)
  - NATS JetStream + Kotlin Coroutines 통합 — Cloud Native 메시지 큐 수요 재부상
  - Kafka 대안으로 경량 (50MB, JVM 없이 실행), 마이크로서비스/IoT에 적합
  - 이미 구현 성숙도 높음 (client/stream/consumer/kv/objectstore)
  - TODO 6.2 infra/nats 항목과 통합 검토

- [x] **lingua → utils/lingua** (3 kt 파일, 높은 ROI)
  - 75+ 언어 자동 감지 — 콘텐츠 분류, 다국어 라우팅, 검색 인덱싱 필수
  - 코드는 작지만 가치 높음 (Lingua 라이브러리 얇은 래퍼)
  - `utils/ai` 신설 시 전처리 도구로 자연스럽게 포함 가능

- [ ] **ahocorasick → utils/text-search** (11 kt 파일)
  - 다중 키워드 검색 O(n) — 금칙어 필터, 태그 추출, 치환, 검색 하이라이팅
  - Trie DSL + case-insensitive/whole-word/overlapping 옵션 지원
  - 수요 꾸준함 (커뮤니티/메신저 서비스 필수)

### 4.2 🟡 조건부 승격 — 기존 모듈과 통합/부분 이관

- [ ] **bloomfilter** — 부분 승격 또는 흡수
  - 이미 `infra/lettuce`에 Redis Lua BloomFilter/CuckooFilter 존재 (중복)
  - `InMemoryBloomFilter` / `InMemoryMutableBloomFilter` / `InMemorySuspendBloomFilter`만 **utils/probabilistic** 로 승격 검토
  - Redis 기반은 `infra/lettuce`로 흡수 완료된 상태 — 원본은 폐기

- [ ] **captcha → utils/images-captcha** (10 kt 파일)
  - CAPTCHA 이미지 생성 — hCaptcha/reCAPTCHA 대체 못 하지만 내부 툴/어드민용 수요 있음
  - `utils/images` 작업과 자연스럽게 연계됨 (Java2D 기반 이미지 생성)
  - utils/images의 **서브모듈** 또는 **별도 utils 모듈**로 승격 검토

- [ ] **logback-kafka → infra/logging-kafka** (14 kt 파일)
  - Kafka appender — `infra/kafka`와 네임스페이스 통합
  - Logback + Kafka는 관측성 스택에 유용 (ELK 대안, 경량 로그 파이프라인)

### 4.3 🟢 삭제 — 구현 없음 또는 사용처 없음

- [ ] **mapstruct** (1 kt) — 예제만, Kotlin data class copy로 충분, 삭제
- [ ] **mutiny-examples** (0 kt) — `utils/mutiny`로 통합 완료, 삭제
- [ ] **tokenizer** (0 kt) — 구현 없음, 삭제
- [ ] **vertx-coroutines / vertx-sqlclient / vertx-webclient** (0~2 kt) — `infra/vertx` umbrella 이미 존재, 삭제
- [ ] **naivebayes** (2 kt) — Naive Bayes classifier, LLM/transformer가 대체, 수요 낮음, 삭제 (또는 `utils/ml` 신설 시 포함)

### 4.4 실행 계획

- [ ] Phase 1: 🔴 3개 모듈 승격 (nats, lingua, ahocorasick) — 독립 PR
- [ ] Phase 2: 🟡 조건부 3개 처리 — bloomfilter 부분 흡수, captcha/logback-kafka 승격
- [ ] Phase 3: 🟢 7개 모듈 완전 제거 — `settings.gradle.kts` 정리
- [ ] Phase 4: `x-obsoleted/` 디렉토리 최종 삭제

---

## 5. Spring Boot 3 / 4 동기화 유지 🔴

현재 13개 모듈 완벽 대칭 — 신규 모듈 추가 시 반드시 양쪽에 동시 구현:

- [ ] 신규 모듈 추가 체크리스트 확립 (PR 템플릿에 반영)
- [ ] Spring Boot 4 BOM 업데이트 추적 (Spring Framework 7.x 대응)
- [ ] spring-boot4 모듈 독립 테스트 CI 구성 확인

---

## 6. 모듈 신규 추가 검토 🟢

### 6.1 data 계층

- [ ] **exposed-oracle** — Oracle JDBC dialect 지원 (기업 수요)
- [ ] **exposed-sqlserver** — SQL Server 지원
- [ ] **exposed-clickhouse** — ClickHouse 분석 DB 지원
- [ ] **exposed-mariadb** — MariaDB 전용 dialect (MySQL8과 분리)

### 6.2 infra 계층

- [ ] **infra/nats** — NATS JetStream + Kotlin Coroutines 통합 (x-obsoleted 승격)
- [ ] **infra/elasticsearch** — Elasticsearch Kotlin Coroutines 클라이언트
- [ ] **infra/pulsar** — Apache Pulsar 통합

### 6.3 utils 계층

- [ ] **utils/ai** — LLM 통합 유틸리티 (Anthropic/OpenAI SDK 래퍼)
- [ ] **utils/vector** — 벡터 임베딩, 유사도 계산 유틸리티
- [ ] **utils/tracing** — OpenTelemetry + Coroutines 통합 강화

### 6.5 utils/images — 이미지 처리 확장 🟢

유사도 지표 확장 (2026-04-20 `ImageSimilarity` 기반 후속):

- [ ] **MSSIM** — 11×11 sliding window 기반 정밀 SSIM (현재는 global luminance SSIM)
- [ ] **aHash / dHash / wHash** — Average/Difference/Wavelet Hash (pHash 보완)
- [ ] **pHash 크기 옵션** — 64bit 고정 → 256bit/1024bit 선택 가능
- [ ] **색 히스토그램 유사도** — Chi-square, Bhattacharyya, Earth Mover's Distance
- [ ] **키포인트 매칭** — SIFT/ORB/AKAZE (OpenCV 또는 BoofCV 통합 검토)
- [ ] **CLIP/DINOv2 임베딩** — `utils/ai` 의존 시 neural similarity

필터 / 색 보정:

- [ ] **Brightness / Contrast / Saturation / Gamma** Filter 래퍼 DSL
- [ ] **GaussianBlur / Sharpen / Sepia / Grayscale / Invert** scrimage 래퍼 통일
- [ ] **Vignette / Border / Rounded-corner** 장식 필터
- [ ] **Pixelate / Mosaic / Median filter** 노이즈 제거·모자이크
- [ ] **Color space 변환** — RGB ↔ HSV/HSL/LAB/YCbCr

변환 / 조작:

- [ ] **AutoCrop** — 여백 자동 제거 (whitespace trim, 임계값 기반)
- [ ] **Smart crop** — 얼굴/관심 영역 중심 크롭 (saliency 기반)
- [ ] **Rotation/Flip/Mirror** 확장 API 일관화
- [ ] **Perspective transform** — 4점 호모그래피
- [ ] **Histogram equalization** — 콘트라스트 자동 보정

분석:

- [ ] **Dominant color extraction** — ColorThief 통합 (이미 scrimage에 포함됨 — 래퍼 필요)
- [ ] **Blur/defocus detection** — Laplacian variance 기반 품질 판정
- [ ] **EXIF 메타데이터** 읽기/쓰기 API (drew-noakes/metadata-extractor)
- [ ] **OCR** — Tesseract (tess4j) 또는 PaddleOCR 통합 인터페이스
- [ ] **얼굴/객체 탐지** — MediaPipe 또는 ONNX Runtime 연동 인터페이스

포맷 지원:

- [ ] **AVIF** — 읽기/쓰기 (libavif 바인딩)
- [ ] **HEIC/HEIF** — iPhone 기본 포맷 지원
- [ ] **TIFF multi-page** — 문서 스캔용 다중 페이지 지원
- [ ] **Raw 카메라 포맷** — dcraw 연동
- [ ] **SVG 래스터화** — Apache Batik 래퍼

성능 / 동시성:

- [ ] **배치 처리 Flow DSL** — `Flow<File>.processImages { ... }`
- [ ] **썸네일 자동 생성 파이프라인** — 다중 사이즈 일괄 생성
- [ ] **Tile-based 대용량 이미지 처리** — 메모리 초과 없이 기가픽셀 처리

#### 🟡 utils/images-vips — libvips 고성능 백엔드 (Medium)

Scrimage(Java2D) 대비 4~10× 처리 속도·1/10 메모리. AVIF/HEIC/DZI/OpenSlide
등 Java2D가 못 다루는 포맷도 단일 API로 처리. Instagram/Cloudflare Images/
Shopify 프로덕션 사용 검증됨.

**Phase 1 — 모듈 골격 / 네이티브 의존성 격리**

- [ ] `utils/images-vips` 신규 모듈 생성 (네이티브 의존성 격리, `utils/images`는 순수 JVM 유지)
- [ ] JNI 바인딩 선택 평가 — [jvips](https://github.com/criteo/JVips) vs [libvips-java](https://github.com/libvips/libvips-java) vs FFI (Java 22 `java.lang.foreign`)
- [ ] CI에서 libvips 설치 전략 — GitHub Actions `apt-get install libvips-dev` + macOS `brew install vips`
- [ ] Testcontainers 기반 CI fallback — libvips 미설치 환경에서 테스트 스킵 전략

**Phase 2 — 핵심 API (scrimage 패턴 복제)**

- [ ] `VipsImage` 래퍼 — scrimage `ImmutableImage`와 동등한 추상화
- [ ] `vipsImageOf(File/ByteArray/InputStream/Path)` 팩토리
- [ ] `SuspendVipsJpegWriter` / `SuspendVipsPngWriter` / `SuspendVipsWebpWriter` / `SuspendVipsAvifWriter`
- [ ] `suspendBytes(writer)` / `suspendWrite(writer, path)` — 기존 `bluetape4k-images` API 일관성 유지
- [ ] 리소스 해제 — `Closeable` + `use {}` 패턴 필수 (네이티브 메모리)

**Phase 3 — 고성능 작업**

- [ ] **스트리밍 썸네일** — `thumbnail(width, height)` — 원본 전체 디코드 없이 단계적 축소
- [ ] **타일 기반 리사이즈** — 기가픽셀 이미지 처리
- [ ] **포맷 변환 파이프라인** — JPEG→AVIF/WebP 배치 변환 DSL
- [ ] **Deep Zoom / DZI 생성** — Seadragon/OpenSeadragon 호환 타일 출력

**Phase 4 — ImageProcessor 추상화**

- [ ] `ImageProcessor` 공통 인터페이스 — scrimage/vips 중 자동 선택
- [ ] 선택 정책 — 파일 크기 > 10MB 또는 지원 포맷(AVIF/HEIC/DZI)이면 vips, 나머지는 scrimage
- [ ] `AutoImageProcessor` 편의 API — 내부 구현 숨김

**Phase 5 — 검증**

- [ ] JMH 벤치마크 — resize/encode/thumbnail (scrimage vs vips, 10개 대표 이미지)
- [ ] 메모리 프로파일링 — 기가픽셀 이미지 처리 시 heap/native 사용량
- [ ] Spring Boot 3/4 자동 구성 — `VipsImageAutoConfiguration`

**리스크**

- 네이티브 의존성 — 배포 환경별 libvips 설치 필요 (Alpine/Ubuntu/macOS 가이드)
- JVM↔네이티브 메모리 해제 누락 시 OOM 위험 — `use {}` 강제 + leak detector 테스트
- 크로스 플랫폼 CI 비용 — Linux/macOS 모두 테스트 필요

통합:

- [ ] **S3 업로드/다운로드** — AWS SDK 통합 (`bluetape4k-aws` 활용)
- [ ] **CDN URL 서명** — CloudFront/S3 pre-signed URL 유틸
- [ ] **Spring Boot 3/4 자동 구성** — `ImageProcessingProperties` + 헬퍼 Bean

품질 / 테스트:

- [ ] **JMH 벤치마크** — 주요 연산(resize/encode/similarity) 측정 결과 공개
- [ ] **크로스 플랫폼 골든 이미지** — WatermarkFilterTest 패턴을 모든 필터 테스트에 확대 적용
- [ ] **Property-based test** — 랜덤 이미지 생성 → invariant 검증 (`pHash(scale(x)) ≈ pHash(x)` 등)

### 6.4 testing 계층

- [ ] **testing/testcontainers/llm** — Ollama, LocalAI 컨테이너 지원 완성
- [ ] **testing/testcontainers/vector-db** — Qdrant, Weaviate, Milvus 지원

---

## 7. 문서화 개선 🟡

- [ ] 각 모듈 README.md + README.ko.md Mermaid UML 다이어그램 추가
  - 미완성 모듈: `data/exposed-*` (일부), `infra/cache-*`, `utils/batch`
- [ ] KDoc 커버리지 확대
  - 현재 public API 중 KDoc 미작성 항목 파악 (Dokka 보고서 활용)
- [ ] CHANGELOG.md 1.7.0 항목 지속 업데이트
- [ ] `docs/` 디렉토리 아키텍처 문서 갱신

---

## 8. 빌드 / CI 개선 🟡

- [ ] **설정 캐시** `warn` → `on` 으로 전환 (현재 경고 해결 후)
- [ ] **의존성 검증** `lenient` → `strict` 전환 검토
- [ ] **Gradle 9.x 호환성** — deprecated API 사용 제거
  - `settings.gradle.kts` `includeModules` 함수 Gradle 9 호환 확인
- [ ] **Kotlin 2.3 컴파일러** 최신 기능 활용 검토
  - `-Xcontext-parameters` 전면 도입 검토
- [ ] **kapt → KSP** 마이그레이션 검토 (kapt 사용 모듈 파악 필요)
- [x] GitHub Actions CI 파이프라인 구성 완료 (2026-04-17)
  - `ci.yml`: validate-wrapper, build, test-core, test-io, test-utils, test-exposed-core, test-docker, ci-status
  - `publish-snapshot.yml`: develop 브랜치 push 시 Maven Central Snapshots 자동 배포

---

## 9. 보안 🔴

- [x] `io/crypto/` deprecated 암호화 → `tink` 완전 대체 완료 (2026-04-17)
- [ ] `gitleaks detect` — 시크릿 스캔 CI 연동
- [ ] 의존성 취약점 스캔 — `./gradlew dependencyCheckAnalyze` 주기 실행

---

## 10. 성능 / 품질 🟢

- [ ] `utils/benchmark` 모듈 결과 문서화 (현재 결과 미공개)
- [ ] `infra/lettuce` NearCache 성능 벤치마크 공개
- [ ] Coroutines structured concurrency 감사 — `GlobalScope` 사용처 제거
- [ ] `StateFlow` / `SharedFlow` 사용 일관성 검토

---

## 11. Javers + Exposed = Event Sourcing / CQRS / DDD 🔴

> x-obsoleted `javers/` 3개 서브모듈(74 kt 파일, Caffeine/Cache2K/Lettuce/Redisson/Kafka 백엔드)을 `data/` 트리로 승격하고, Exposed 생태계와 통합해 **JPA 대체 가능한 Event Sourcing 기반 DDD 스택**을 구축.

### 11.1 전략적 가치

| 측면 | JPA + Hibernate Envers | **Javers + Exposed** |
|------|--------------------------|------------------------|
| 변경 추적 | 엔티티별 `*_AUD` 테이블 (정규화) | Commit/Snapshot JSON (비정규화) |
| 스키마 변경 | Envers 마이그레이션 복잡 | snapshot 스토어 단일 테이블 |
| Unit of Work | Hibernate session 강결합 | 명시적 `javers.commit(author, object)` |
| Lazy loading | proxy/flush 타이밍 이슈 | 없음 (DSL로 명시적 쿼리) |
| CQRS | 별도 구현 필요 | snapshot=read, commit=event (자연) |
| Event Sourcing | 지원 안 함 | **Kafka 백엔드로 즉시 가능** |
| 비동기/코루틴 | 제한적 (EntityManager blocking) | Exposed R2DBC + 코루틴 |
| 성능 | 프록시 오버헤드 | 예측 가능한 SQL |

### 11.2 모듈 구조 제안

- [ ] **data/javers-core** — 공통 추상화 (`AbstractCdoSnapshotRepository`, JQL DSL, Snowflake CommitId)
- [ ] **data/javers-exposed** — Exposed JDBC 기반 `ExposedCdoSnapshotRepository` 신규 구현 (snapshot 테이블 직접 관리)
- [ ] **data/javers-exposed-r2dbc** — Exposed R2DBC 코루틴 버전
- [ ] **data/javers-caffeine / javers-cache2k** — 로컬 캐시 snapshot (읽기 성능)
- [ ] **data/javers-lettuce / javers-redisson** — Redis 분산 snapshot (다중 인스턴스 공유)
- [ ] **data/javers-kafka** — Kafka commit 이벤트 스트림 (Event Sourcing)

### 11.3 Phase 1 — 기반 이관 (🔴 최우선)

- [ ] `x-obsoleted/javers/*` → `data/javers-*` 이동, 패키지 네이밍 유지
- [ ] `settings.gradle.kts` 등록
- [ ] Javers 최신 버전 (현재 7.x) 대응 — API breaking change 검토
- [ ] 기존 74개 파일 컴파일 복구 + 테스트 재통과
- [ ] Kotlin 2.3 / JVM 21 대응

### 11.4 Phase 2 — Exposed 통합 (신규 구현)

- [ ] **ExposedCdoSnapshotRepository** — snapshot/commit을 Exposed Table로 관리
  - `CdoSnapshotTable` — global_id, commit_id, version, type, state(JSON), changed_properties(JSON)
  - `CommitTable` — commit_id, author, commit_date, properties(JSON)
- [ ] JSON 컬럼은 기존 `exposed-jackson`/`exposed-fastjson2` 활용
- [ ] 트랜잭션 통합 — 비즈니스 INSERT/UPDATE + Javers commit을 한 트랜잭션 내 커밋
- [ ] Aggregate root 자동 감지 — Exposed Entity `@TypeName` / `@Id` 어노테이션 매핑

### 11.5 Phase 3 — DDD 패턴 헬퍼

- [ ] **AggregateRoot<ID>** — DDD Aggregate root 마커 interface
- [ ] **DomainEvent** sealed class 패턴 + Javers commit properties 매핑
- [ ] **Repository<T: AggregateRoot<ID>, ID>** — save/load 시 자동 commit
- [ ] **EventPublisher** — commit 성공 시 Kafka/NATS 발행 (outbox 패턴 대체)
- [ ] **Projection** 빌더 — Javers JQL 결과 → read model DTO

### 11.6 Phase 4 — CQRS / Event Sourcing 데모

- [ ] **examples/javers-exposed-ddd** — 주문/재고 도메인 샘플
  - Command side: Exposed write + Javers commit
  - Query side: Kafka commit consumer → Redis projection
- [ ] **Spring Boot 3/4 자동 구성** — `JaversExposedAutoConfiguration`
  - `JaversBuilder` bean, `CdoSnapshotRepository` bean 자동 선택 (exposed/redis/kafka)
- [ ] 성능 벤치마크 — JPA Envers vs Javers+Exposed (INSERT/UPDATE/audit query)

### 11.7 리스크 / 고려사항

- Javers는 GPL이 아닌 Apache-2.0 — OK
- Javers gson 의존성 — Jackson/FastJson2 코덱 래퍼 추가 필요 (`codecs/JaversCodec`)
- 비정규화 JSON 스토리지 → 복잡한 집계 쿼리는 JQL 한계 존재, 별도 projection 필요
- 마이그레이션 가이드 필요 — 기존 JPA Envers 사용자가 이관할 수 있도록 문서화

---

## 12. Redis Codec — ForyFast 지원 추가 🟡

> `2026-04-23-redis-json-codec-design.md` 스펙에서 범위 분리. JSON Codec(Jackson3/Fastjson2) 완료 후 후속 PR로 진행.

ForyBinarySerializer.fast() (SCHEMA_CONSISTENT, refTracking=false) 를 활용한 고성능 Redis Codec.

### 구현 대상

- [ ] `io/io` `BinarySerializers.kt` — `ForyFast`, `LZ4ForyFast`, `ZstdForyFast`, `SnappyForyFast` lazy 프로퍼티 추가
- [ ] `infra/redisson` `ForyFastCodec.kt` — Redisson BaseCodec 구현, fallbackCodec = Kryo5
- [ ] `infra/redisson` `RedissonCodecs.kt` — `ForyFast`, `LZ4ForyFast`, `ZstdForyFast` val 추가
- [ ] `infra/lettuce` `LettuceBinaryCodecs.kt` — `foryFast()`, `lz4ForyFast()`, `zstdForyFast()` factory 추가
- [ ] 각 Codec 테스트 (roundtrip + ForyCodec 비호환 검증)
- [ ] 기존 JSON/Binary Codec 벤치마크에 ForyFast 비교군 추가

### 제약사항 (반드시 숙지)

- ForyFast(SCHEMA_CONSISTENT)와 Fory(COMPATIBLE) 포맷 상호 비호환 → fallback = Kryo5
- 순환 참조 객체 불가, 스키마 진화 불가
- **휘발성 캐시 전용** — DB/파일 영속 데이터에 사용 금지

---

## 완료 기준

각 항목은 다음 조건을 모두 만족해야 완료:

- [ ] 코드 변경 완료
- [ ] 단위/통합 테스트 통과
- [ ] README.md + README.ko.md 업데이트
- [ ] testlog 기록 (`wiki/testlogs/YYYY-MM.md`)
