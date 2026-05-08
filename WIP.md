# bluetape4k TODO

> 현재 버전: 1.7.0-SNAPSHOT | 브랜치: `develop` | 모듈 수: 128개 (examples 제외)
> 최종 업데이트: 2026-05-01 (이슈 반영: #179 #181 #185 #187 #191 #190 #189 #38 #25 #203 #204 #205 #257 #258 #259 #260 #261 #262 #263)

---

## 우선순위 분류

- 🔴 **High** — 릴리스 전 반드시 처리
- 🟡 **Medium** — 다음 마일스톤 대상
- 🟢 **Low** — 장기 개선 과제

---

## 1. 미완성 기능

### 1.1 utils/science — NetCdf 지원 완성 ✅

- Issue: [#107](https://github.com/bluetape4k/bluetape4k-projects/issues/107) — **CLOSED COMPLETED (2026-04-25)**
- [x] `NetCdfCatalogService.kt` — 구현 완료
- [x] `NetCdfTableTest.kt` — 테스트 케이스 완성
- [x] UCAR netcdfAll 의존성 추가 후 전체 파이프라인 검증

#### 참고 자료
- [UCAR NetCDF-Java 공식 문서](https://docs.unidata.ucar.edu/netcdf-java/current/)
- [unidata/netcdf-java GitHub](https://github.com/Unidata/netcdf-java)
- [NetCDF 파일 포맷 명세](https://docs.unidata.ucar.edu/nug/current/)

### 1.2 io/csv — Record.toFieldMap() 복원 🟡

- 배경: 1.7.0에서 Apache Commons CSV → 자체 `CsvLexer` 교체 시 `toFieldMap()` 메서드가 누락됨
- `bluetape4k-graph` 등 외부 소비자가 `record.headers?.zip(record.values)?.toMap()` 로 workaround 중
- [ ] `Record` 인터페이스에 `toFieldMap(): Map<String, String?>` 확장함수 또는 메서드 추가
  ```kotlin
  fun Record.toFieldMap(): Map<String, String?> =
      headers?.zip(values)?.toMap() ?: emptyMap()
  ```
- [ ] `CsvRecordReader` / `SuspendCsvRecordReader` 반환 타입에서 직접 호출 가능하도록 검증
- [ ] `bluetape4k-graph` graph-io/csv 의 workaround 제거 후 재검증

### 1.3 Hibernate 6 → 7 마이그레이션 적용 🔴

- Issue: [#204](https://github.com/bluetape4k/bluetape4k-projects/issues/204)
- [ ] `examples/jpa-querydsl-demo` — Hibernate 6 기반 설정 점검
- [ ] `data/hibernate` 하위 모듈 전체 API 변경사항 반영 확인
- [ ] `@DisabledWithHibernate7AndSpringBoot3` 적용 대상 재검토
- [ ] Hibernate 7 breaking change 체크 (SessionFactory, HQL/Criteria, DDL 변화)

#### 참고 자료
- [Hibernate ORM 7 마이그레이션 가이드](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html)

### 1.4 examples/jpa-querydsl-demo — QueryDSL 쿼리 완성 🟢

- Issue: [#108](https://github.com/bluetape4k/bluetape4k-projects/issues/108)
- [ ] `MemberRepositoryImpl.kt` — `TODO("Not yet implemented")` 3개 구현
  - `findByName()`, `findByAgeGreaterThan()`, `findByNameContaining()` 완성

#### 참고 자료
- [QueryDSL 공식 문서](http://querydsl.com/static/querydsl/latest/reference/html/)
- [querydsl/querydsl GitHub](https://github.com/querydsl/querydsl)
- [QueryDSL + Spring Data JPA 통합 가이드](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html#core.extensions.querydsl)

---

## 2. Deprecated 코드 정리

### 2.1 io 모듈 레거시 정리 ✅

- Issue: [#109](https://github.com/bluetape4k/bluetape4k-projects/issues/109) — **CLOSED COMPLETED (2026-04-24)**
- [x] `io/crypto/` — jasypt 기반 암호화 모듈 전체 삭제, `tink` 모듈로 대체 완료 (2026-04-17)
- [x] `io/http/` — `AHC`(AsyncHttpClient), `OkHttp3`, `HC5` 레거시 HTTP 클라이언트 정리
- [x] `io/jackson2/`, `io/jackson3/` — deprecated 직렬화 API 정리

#### 참고 자료
- [Apache HttpClient 5.x 마이그레이션](https://hc.apache.org/httpcomponents-client-5.2.x/migration-guide/index.html)
- [OkHttp 공식 문서](https://square.github.io/okhttp/)
- [AsyncHttpClient GitHub](https://github.com/AsyncHttpClient/async-http-client)

### 2.2 core 모듈 Deprecated 정리 🟡

- [x] `bluetape4k/core/` — `@Deprecated` 항목 전수 제거 완료 (2026-04-17)
  - Systemx, TimeSpec, DateSupport, StringSupport, NumberSupport, AutoCloseableSupport, EnumSupport, ExecutorSupport, StructuredTaskScopeSupport, ProgressionSupport, IterableSupport, SequenceSupport, QueueSupport, AnySupport, ArraySupport, ApacheConstructorUtils 등 총 26개 항목 제거

### 2.3 infra 모듈 정리 🟡

- Issue: [#110](https://github.com/bluetape4k/bluetape4k-projects/issues/110)
- [ ] `infra/` — 12개 deprecated 파일 검토
  - 레거시 캐시, 큐 연동 API 정리

---

## 3. testing/testcontainers — HazelcastServer 수정 ✅

- Issue: [#111](https://github.com/bluetape4k/bluetape4k-projects/issues/111) — **CLOSED COMPLETED (2026-04-24)**
- [x] `HazelcastServer.kt` — deprecated Hazelcast API 수정 완료 (`storage/` 패키지로 이동)
  - `Config`, `NetworkConfig`, `JoinConfig`, `TcpIpConfig` 최신 API로 교체 완료
  - Hazelcast 5.x 호환성 확보

#### 참고 자료
- [Hazelcast 5.x 공식 문서](https://docs.hazelcast.com/hazelcast/5.5/)
- [Hazelcast 5.x 마이그레이션 가이드](https://docs.hazelcast.com/hazelcast/5.5/migrate/migration-guide-5.0)
- [Hazelcast Kotlin 클라이언트](https://github.com/hazelcast/hazelcast-kotlin-client)

---

## 4. x-obsoleted 처리 계획 🟡

- Epic: [#114](https://github.com/bluetape4k/bluetape4k-projects/issues/114)
14개 레거시 모듈 전수 조사 완료 (2026-04-20). 실무 가치 기준으로 재분류.

### 4.1 🔴 승격 강력 추천 — 구현 충실도 높고 실무 수요 큼

- [ ] **javers → data/javers-eventsourcing** (74 kt 파일, 3 서브모듈, **전략적 최우선**)
  - Event Sourcing / CQRS / DDD Aggregate 변경 추적의 정석 라이브러리
  - Hibernate Envers 대비 장점: 비정규화 snapshot, 명시적 commit, Unit of Work 불필요
  - **이미 5종 백엔드 구현**: Caffeine, Cache2K, Lettuce, Redisson, Kafka
  - JPA/Hibernate 없이도 동작 — **Exposed와 결합 시 DDD 친화적**
  - Kafka 백엔드 = Event Sourcing 기반 CQRS에 그대로 활용 가능
  - 상세 계획은 하단 §12 참조

- [x] **nats → infra/nats** (70 kt 파일) — **승격 완료** (`infra/nats/` 존재, x-obsoleted/nats 정리 필요)
  - NATS JetStream + Kotlin Coroutines 통합 완료
  - `x-obsoleted/nats/` 잔존 → Phase 3 삭제 대상

- [x] **lingua → texts/lingua** (3 kt 파일, 높은 ROI) — PR #170 승격 완료
  - 75+ 언어 자동 감지 — 콘텐츠 분류, 다국어 라우팅, 검색 인덱싱 필수
  - 코드는 작지만 가치 높음 (Lingua 라이브러리 얇은 래퍼)

- [x] **ahocorasick → texts/text-search** (11 kt 파일) — 이슈 [#140](https://github.com/bluetape4k/bluetape4k-projects/issues/140) / PR [#165](https://github.com/bluetape4k/bluetape4k-projects/pull/165) 승격 완료
  - 다중 키워드 검색 O(n) — 금칙어 필터, 태그 추출, 치환, 검색 하이라이팅
  - Trie DSL + case-insensitive/whole-word/overlapping 옵션 지원
  - 수요 꾸준함 (커뮤니티/메신저 서비스 필수)

#### 참고 자료 (§4.1 승격 후보)
- [JaVers 공식 문서](https://javers.org/documentation/)
- [JaVers GitHub](https://github.com/javers/javers) — Apache 2.0 라이선스
- [JaVers Event Sourcing 블로그](https://javers.org/blog/2016/01/event-sourcing-using-javers.html)
- [NATS JetStream 공식 문서](https://docs.nats.io/nats-concepts/jetstream)
- [nats-io/nats.java GitHub](https://github.com/nats-io/nats.java)
- [pemistahl/lingua GitHub](https://github.com/pemistahl/lingua) — 75+ 언어 감지
- [robert-bor/aho-corasick GitHub](https://github.com/robert-bor/aho-corasick) — Java Aho-Corasick 구현
- [Aho–Corasick 알고리즘 위키](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)

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

#### 참고 자료 (§4.2 조건부 승격 후보)
- [Guava BloomFilter](https://guava.dev/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html)
- [bloomfilter 알고리즘 위키](https://en.wikipedia.org/wiki/Bloom_filter)
- [Cuckoo filter 논문 (Fan et al., 2014)](https://www.cs.cmu.edu/~dga/papers/cuckoo-conext2014.pdf)
- [danielwegener/logback-kafka-appender GitHub](https://github.com/danielwegener/logback-kafka-appender)
- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)

### 4.3 🟢 삭제 — 구현 없음 또는 사용처 없음

- [ ] **mapstruct** (1 kt) — 예제만, Kotlin data class copy로 충분, 삭제 (이슈 [#144](https://github.com/bluetape4k/bluetape4k-projects/issues/144))
- [ ] **mutiny-examples** (0 kt) — `utils/mutiny`로 통합 완료, 삭제 (이슈 [#144](https://github.com/bluetape4k/bluetape4k-projects/issues/144))
- [x] **tokenizer** — PR #170으로 `texts/tokenizer-core`, `texts/tokenizer-korean`, `texts/tokenizer-japanese` 로 승격 완료
- [ ] **vertx-coroutines / vertx-sqlclient / vertx-webclient** (0~2 kt) — `infra/vertx` umbrella 이미 존재, 삭제 (이슈 [#144](https://github.com/bluetape4k/bluetape4k-projects/issues/144))
- [ ] **naivebayes** (2 kt) — Naive Bayes classifier, LLM/transformer가 대체, 수요 낮음, 삭제 (이슈 [#144](https://github.com/bluetape4k/bluetape4k-projects/issues/144))
- [ ] **x-obsoleted/nats/** 디렉토리 삭제 — `infra/nats` 승격 완료 (이슈 [#139](https://github.com/bluetape4k/bluetape4k-projects/issues/139) closed)
- [ ] **x-obsoleted/lingua/** 디렉토리 삭제 — `texts/lingua` 승격 완료 (이슈 [#170](https://github.com/bluetape4k/bluetape4k-projects/issues/170) closed)

### 4.4 실행 계획

- [x] Phase 1: 🔴 5개 모듈 승격 (tokenizer-core/korean/japanese, lingua, text-search) — PR #170 완료
- [ ] Phase 2: 🟡 조건부 3개 처리 — bloomfilter 부분 흡수, captcha/logback-kafka 승격
- [ ] Phase 3: 🟢 7개 모듈 완전 제거 — `settings.gradle.kts` 정리
- [ ] Phase 4: `x-obsoleted/` 디렉토리 최종 삭제

---

## 5. Spring Boot 3 제거 / Spring Boot 4 단독 유지 🔴

- Issue (동기화): [#112](https://github.com/bluetape4k/bluetape4k-projects/issues/112)
- Issue (제거): [#263](https://github.com/bluetape4k/bluetape4k-projects/issues/263)

> **결정 (2026-05-01)**: Spring Boot 3.5 EOL = 2026-06-30 (2개월 후). LTS 없음.
> Spring Boot 4 단독 유지로 전환. spring-boot3 그룹 13개 모듈 전체 제거 예정.

### 제거 작업 (#263)

- [ ] `infra/kafka/build.gradle.kts` — `bluetape4k-spring-boot3-core` → `bluetape4k-spring-boot4-core` 교체
- [ ] `spring-boot3/**` 디렉토리 13개 모듈 전체 삭제
- [ ] `settings.gradle.kts` — `includeModules("spring-boot3", ...)` 제거
- [ ] `buildSrc/Libs.kt` — `spring_boot3_*`, `resilience4j_spring_boot3` 등 상수 정리
- [ ] CI yml spring-boot3 관련 설정 제거
- [ ] 전체 빌드 통과 확인 (128 → 115개)

### 외부 workshop 프로젝트 별도 대응

- [ ] `exposed-r2dbc-workshop` — spring-boot3 참조 15개 파일 → Spring Boot 4 마이그레이션
- [ ] `exposed-workshop` — spring-boot3 참조 5개 파일 → Spring Boot 4 마이그레이션

---

## 6. 모듈 신규 추가 검토 🟢

### 6.1 data 계층

> **참고**: Exposed는 Oracle, SQL Server, MariaDB를 공식 지원 (`OracleDialect`, `SQLServerDialect`, `MariaDBDialect` 내장).
> bluetape4k에서 필요한 것은 **ClickHouse 전용 dialect** 구현임.

- [x] **exposed-clickhouse** — ClickHouse OLAP 분석 DB Exposed Dialect 구현 완료
  - Issue: [#145](https://github.com/bluetape4k/bluetape4k-projects/issues/145) — **CLOSED COMPLETED (2026-04-25)**
  - `data/exposed-clickhouse/` 및 `examples/exposed-clickhouse-oltp-olap/` 구현 완료

#### 참고 자료
- [JetBrains Exposed GitHub](https://github.com/JetBrains/Exposed)
- [Exposed 공식 지원 DB 목록 (Oracle/SQLServer/MariaDB 포함)](https://github.com/JetBrains/Exposed/tree/main/exposed-core/src/main/kotlin/org/jetbrains/exposed/sql/vendors)
- [ClickHouse 공식 문서](https://clickhouse.com/docs/)
- [ClickHouse JDBC 드라이버 (clickhouse-java)](https://github.com/ClickHouse/clickhouse-java)
- [ClickHouse MergeTree 엔진](https://clickhouse.com/docs/engines/table-engines/mergetree-family/mergetree)
- [ClickHouse 성능 벤치마크](https://clickhouse.com/docs/concepts/why-clickhouse-is-so-fast)
- [Testcontainers ClickHouse 모듈](https://java.testcontainers.org/modules/clickhouse/)

### 6.2 infra 계층

- [x] **infra/nats** — NATS JetStream + Kotlin Coroutines 통합 완료 (`infra/nats/` 존재)
- [x] **infra/elasticsearch** — Elasticsearch Kotlin Coroutines 클라이언트 완료 (`infra/elasticsearch/` 존재)
- [x] **infra/pulsar** — Apache Pulsar 통합 완료 (`infra/pulsar/` 존재)

#### 참고 자료
- [NATS.io 공식 문서](https://docs.nats.io/)
- [NATS JetStream 개념](https://docs.nats.io/nats-concepts/jetstream)
- [nats-io/nats.java GitHub](https://github.com/nats-io/nats.java)
- [Elasticsearch Java API Client (8.x)](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [elastic/elasticsearch-java GitHub](https://github.com/elastic/elasticsearch-java)
- [Apache Pulsar 공식 문서](https://pulsar.apache.org/)
- [Apache Pulsar Java 클라이언트](https://pulsar.apache.org/docs/client-libraries-java/)

### 6.3 utils 계층

- [ ] **utils/ai** — LLM 통합 유틸리티 (Anthropic/OpenAI SDK 래퍼)
- [ ] **utils/vector** — 벡터 임베딩, 유사도 계산 유틸리티
- [ ] **utils/tracing** — OpenTelemetry + Coroutines 통합 강화

#### 참고 자료
- [Anthropic Java SDK GitHub](https://github.com/anthropics/anthropic-sdk-java)
- [OpenAI Java SDK GitHub](https://github.com/openai/openai-java)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j) — Kotlin 친화적 LLM 래퍼
- [OpenTelemetry Java 공식 문서](https://opentelemetry.io/docs/languages/java/)
- [OpenTelemetry Kotlin Coroutines 계측](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/kotlinx-coroutines)
- [Qdrant 벡터 DB](https://qdrant.tech/documentation/)
- [Weaviate 벡터 DB](https://weaviate.io/developers/weaviate)
- [Milvus 벡터 DB](https://milvus.io/docs)

### 6.5 utils/images — 이미지 처리 확장 ✅ **이관됨**

> `images/` 디렉토리가 [bluetape4k-image](https://github.com/bluetape4k/bluetape4k-image) 독립 레포지토리로 분리됨.
> 상세 TODO는 [bluetape4k-image/WIP.md](https://github.com/bluetape4k/bluetape4k-image/blob/develop/WIP.md) 참조.
> 관련 이슈: [#1](https://github.com/bluetape4k/bluetape4k-image/issues/1) [#2](https://github.com/bluetape4k/bluetape4k-image/issues/2) [#3](https://github.com/bluetape4k/bluetape4k-image/issues/3) [#4](https://github.com/bluetape4k/bluetape4k-image/issues/4) [#5](https://github.com/bluetape4k/bluetape4k-image/issues/5)

### 6.4 testing 계층

- [ ] **testing/testcontainers/llm** — Ollama, LocalAI 컨테이너 지원 완성
- [ ] **testing/testcontainers/vector-db** — Qdrant, Weaviate, Milvus 지원

#### 참고 자료
- [Ollama 공식 사이트](https://ollama.com/)
- [LocalAI GitHub](https://github.com/mudler/LocalAI)
- [Testcontainers Ollama 모듈](https://github.com/testcontainers/testcontainers-java/tree/main/modules/ollama)
- [Qdrant 공식 문서](https://qdrant.tech/documentation/)
- [Weaviate 공식 문서](https://weaviate.io/developers/weaviate)
- [Milvus 공식 문서](https://milvus.io/docs)

---

## 7. 문서화 개선 🟡

- [ ] 각 모듈 README.md + README.ko.md Mermaid UML 다이어그램 추가
  - 미완성 모듈: `data/exposed-*` (일부), `infra/cache-*`, `utils/batch`
- [ ] KDoc 커버리지 확대
  - 현재 public API 중 KDoc 미작성 항목 파악 (Dokka 보고서 활용)
- [ ] CHANGELOG.md 1.7.0 항목 지속 업데이트
- [ ] `docs/` 디렉토리 아키텍처 문서 갱신

---

## 8. Monorepo 재편 — 도메인별 독립 Repository 전환 🟡

- Epic: [#257](https://github.com/bluetape4k/bluetape4k-projects/issues/257)

> 현재 128개 모듈 단일 monorepo → 도메인 경계 기준으로 독립 repo 분리.
> `infra/**`, `io/**`, `spring-boot4` 기본 기능은 현 repo 유지.
> 분리 후 core 의존성은 발행 아티팩트(`io.bluetape4k:...`) 참조로 전환.

### Phase 0 — spring-boot3 제거 (→ §5 참조)

- Issue: [#263](https://github.com/bluetape4k/bluetape4k-projects/issues/263)
- 128개 → **115개** 예상

### Phase 1 — 독립 repo 3개 분리 (병렬 진행 가능)

| Phase | Repo | 대상 | 모듈 수 | Issue |
|-------|------|------|---------|-------|
| 1-A | `bluetape4k-aws` | `aws/**` | 2 | [#258](https://github.com/bluetape4k/bluetape4k-projects/issues/258) |
| 1-B | `bluetape4k-images` | `images/**` | 5 | [#259](https://github.com/bluetape4k/bluetape4k-projects/issues/259) |
| 1-C | `bluetape4k-texts` | `texts/**` | 5 | [#260](https://github.com/bluetape4k/bluetape4k-projects/issues/260) |

모두 역방향 참조 0개, Spring 의존 없음 — `bluetape4k-leader` 구조 복사 후 project ref → artifact ref 전환.
- 115개 → **103개** 예상

공통 작업 (1-A/B/C 동일):
- [ ] GitHub repo 생성
- [ ] `buildSrc`, `settings.gradle.kts`, `build.gradle.kts` 구성 (bluetape4k-leader 기반)
- [ ] 모듈 이동 + project 참조 → `io.bluetape4k:...:${Versions.bluetape4k}` 전환
- [ ] GitHub Actions CI 구성 (build + test + publish)
- [ ] 현 repo에서 해당 디렉토리 제거 + `settings.gradle.kts` 업데이트

### Phase 2 — bluetape4k-exposed 분리

- Issue: [#261](https://github.com/bluetape4k/bluetape4k-projects/issues/261)
- 대상: `data/exposed-**` 25개 + `spring-boot4/exposed-jdbc`, `spring-boot4/exposed-r2dbc` 2개 = **27개**
- Phase 1 publish pipeline 안정화 후 진행
- 103개 → **76개** 예상

핵심 결정 사항:
- [ ] spring-boot4 exposed 모듈(2개)을 bluetape4k-exposed로 함께 이동 여부 확정
- [ ] `exposed-jdbc-lettuce`, `exposed-r2dbc-lettuce` → `infra/lettuce` 발행 아티팩트 안정성 확인

### Phase 3 — bluetape4k-data 분리 (보류)

- Issue: [#262](https://github.com/bluetape4k/bluetape4k-projects/issues/262)
- 대상: `data/hibernate`, `jdbc`, `r2dbc`, `mongodb`, `cassandra` (~7개)
- Phase 2 완료 및 spring-boot4 의존 방향 확정 후 재평가

### 목표 모듈 수

```
현재: 128개
Phase 0 완료: 115개  (spring-boot3 제거)
Phase 1 완료: 103개  (aws + images + texts 분리)
Phase 2 완료:  76개  (exposed 분리)
Phase 3 완료:  69개  (data 분리, 선택)
```

---

## 9. 빌드 / CI 개선 🟡

- [ ] **설정 캐시** `warn` → `on` 으로 전환 (현재 경고 해결 후)
- [ ] **의존성 검증** `lenient` → `strict` 전환 검토
- [x] **Gradle 9.x 호환성** — deprecated API 제거 완료 (이슈 [#153](https://github.com/bluetape4k/bluetape4k-projects/issues/153), 2026-04-26)
- [ ] **Kotlin 2.3 컴파일러** 최신 기능 활용 검토
  - `-Xcontext-parameters` 전면 도입 검토
- [x] **kapt → KSP** 마이그레이션 완료 (이슈 [#153](https://github.com/bluetape4k/bluetape4k-projects/issues/153), 2026-04-26)
- [x] GitHub Actions CI 파이프라인 구성 완료 (2026-04-17)

#### 참고 자료
- [Gradle 9.x 릴리스 노트](https://docs.gradle.org/9.0/release-notes.html)
- [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-overview.html)
- [kapt → KSP 마이그레이션 가이드](https://kotlinlang.org/docs/ksp-migration-guide.html)
- [Kotlin 2.x 새 기능](https://kotlinlang.org/docs/whatsnew-eap.html)
- [Kotlin Context Parameters 제안](https://github.com/Kotlin/KEEP/blob/master/proposals/context-parameters.md)
  - `ci.yml`: validate-wrapper, build, test-core, test-io, test-utils, test-exposed-core, test-docker, ci-status
  - `publish-snapshot.yml`: develop 브랜치 push 시 Maven Central Snapshots 자동 배포

---

## 10. 보안 🔴

- [x] `io/crypto/` deprecated 암호화 → `tink` 완전 대체 완료 (2026-04-17)
- [ ] **lz4java 보안 패치 업그레이드** — Issue: [#203](https://github.com/bluetape4k/bluetape4k-projects/issues/203)
  - `buildSrc/Libs.kt` 버전 확인 → 최신 릴리스 업그레이드
  - infra/lettuce, cache-core 등 lz4 사용 모듈 빌드·테스트 검증
- [ ] **보안 스캔 workflow 항상 실패 — 점검 및 수정** — Issue: [#205](https://github.com/bluetape4k/bluetape4k-projects/issues/205)
  - 실패 workflow 파일 특정 (gitleaks / dependencyCheck / trivy)
  - 오탐 vs 실제 설정 오류 구분 후 수정
- [ ] `gitleaks detect` — 시크릿 스캔 CI 연동
- [ ] 의존성 취약점 스캔 — `./gradlew dependencyCheckAnalyze` 주기 실행

#### 참고 자료
- [Gitleaks GitHub](https://github.com/gitleaks/gitleaks)
- [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Google Tink 암호화 라이브러리](https://developers.google.com/tink)

---

## 11. 성능 / 품질

### 11.1 벤치마크 결과 공개 🟢

- Issue: [#184](https://github.com/bluetape4k/bluetape4k-projects/issues/184)
- [ ] `utils/benchmark` 모듈 JMH 벤치마크 결과 문서화 (현재 결과 미공개)
- [ ] `infra/lettuce` NearCache 성능 벤치마크 결과 공개 (L1/L2 hit/miss 수치 포함)

#### 참고 자료
- [JMH (Java Microbenchmark Harness)](https://github.com/openjdk/jmh)
- [Gradle JMH Plugin](https://github.com/melix/jmh-gradle-plugin)

### 11.2 Coroutines 품질 개선 ✅

- Issue: [#185](https://github.com/bluetape4k/bluetape4k-projects/issues/185) — **CLOSED COMPLETED (2026-04-27)**
- [x] Coroutines structured concurrency 감사 — `GlobalScope` 사용처 전수 제거 완료
- [x] `StateFlow` / `SharedFlow` 사용 일관성 검토 완료

#### 참고 자료
- [Kotlin Coroutines 구조적 동시성 공식 문서](https://kotlinlang.org/docs/coroutines-and-channels.html)
- [StateFlow vs SharedFlow 비교](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/)

---

## 12. Javers + Exposed = Event Sourcing / CQRS / DDD 🔴

- Epic: [#115](https://github.com/bluetape4k/bluetape4k-projects/issues/115)
> x-obsoleted `javers/` 3개 서브모듈(74 kt 파일, Caffeine/Cache2K/Lettuce/Redisson/Kafka 백엔드)을 `data/` 트리로 승격하고, Exposed 생태계와 통합해 **JPA 대체 가능한 Event Sourcing 기반 DDD 스택**을 구축.

### 12.1 전략적 가치

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

### 12.2 모듈 구조 제안

- [ ] **data/javers-core** — 공통 추상화 (`AbstractCdoSnapshotRepository`, JQL DSL, Snowflake CommitId)
- [ ] **data/javers-exposed** — Exposed JDBC 기반 `ExposedCdoSnapshotRepository` 신규 구현 (snapshot 테이블 직접 관리)
- [ ] **data/javers-exposed-r2dbc** — Exposed R2DBC 코루틴 버전
- [ ] **data/javers-caffeine / javers-cache2k** — 로컬 캐시 snapshot (읽기 성능)
- [ ] **data/javers-lettuce / javers-redisson** — Redis 분산 snapshot (다중 인스턴스 공유)
- [ ] **data/javers-kafka** — Kafka commit 이벤트 스트림 (Event Sourcing)

### 12.3 Phase 1 — 기반 이관 (🔴 최우선)

- [ ] `x-obsoleted/javers/*` → `data/javers-*` 이동, 패키지 네이밍 유지
- [ ] `settings.gradle.kts` 등록
- [ ] Javers 최신 버전 (현재 7.x) 대응 — API breaking change 검토
- [ ] 기존 74개 파일 컴파일 복구 + 테스트 재통과
- [ ] Kotlin 2.3 / JVM 21 대응

### 12.4 Phase 2 — Exposed 통합 (신규 구현)

- [ ] **ExposedCdoSnapshotRepository** — snapshot/commit을 Exposed Table로 관리
  - `CdoSnapshotTable` — global_id, commit_id, version, type, state(JSON), changed_properties(JSON)
  - `CommitTable` — commit_id, author, commit_date, properties(JSON)
- [ ] JSON 컬럼은 기존 `exposed-jackson`/`exposed-fastjson2` 활용
- [ ] 트랜잭션 통합 — 비즈니스 INSERT/UPDATE + Javers commit을 한 트랜잭션 내 커밋
- [ ] Aggregate root 자동 감지 — Exposed Entity `@TypeName` / `@Id` 어노테이션 매핑

### 12.5 Phase 3 — DDD 패턴 헬퍼

- [ ] **AggregateRoot<ID>** — DDD Aggregate root 마커 interface
- [ ] **DomainEvent** sealed class 패턴 + Javers commit properties 매핑
- [ ] **Repository<T: AggregateRoot<ID>, ID>** — save/load 시 자동 commit
- [ ] **EventPublisher** — commit 성공 시 Kafka/NATS 발행 (outbox 패턴 대체)
- [ ] **Projection** 빌더 — Javers JQL 결과 → read model DTO

### 12.6 Phase 4 — CQRS / Event Sourcing 데모

- [ ] **examples/javers-exposed-ddd** — 주문/재고 도메인 샘플
  - Command side: Exposed write + Javers commit
  - Query side: Kafka commit consumer → Redis projection
- [ ] **Spring Boot 3/4 자동 구성** — `JaversExposedAutoConfiguration`
  - `JaversBuilder` bean, `CdoSnapshotRepository` bean 자동 선택 (exposed/redis/kafka)
- [ ] 성능 벤치마크 — JPA Envers vs Javers+Exposed (INSERT/UPDATE/audit query)

### 12.7 리스크 / 고려사항

- Javers는 GPL이 아닌 Apache-2.0 — OK
- Javers gson 의존성 — Jackson/FastJson2 코덱 래퍼 추가 필요 (`codecs/JaversCodec`)
- 비정규화 JSON 스토리지 → 복잡한 집계 쿼리는 JQL 한계 존재, 별도 projection 필요
- 마이그레이션 가이드 필요 — 기존 JPA Envers 사용자가 이관할 수 있도록 문서화

#### 참고 자료
- [JaVers 공식 문서](https://javers.org/documentation/)
- [JaVers GitHub (Apache 2.0)](https://github.com/javers/javers)
- [JaVers JQL (Javers Query Language)](https://javers.org/documentation/jql-examples/)
- [JaVers Event Sourcing 블로그](https://javers.org/blog/2016/01/event-sourcing-using-javers.html)
- [DDD — Aggregate 패턴 (Martin Fowler)](https://martinfowler.com/bliki/DDD_Aggregate.html)
- [Event Sourcing 패턴 (Martin Fowler)](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS 패턴 (Martin Fowler)](https://martinfowler.com/bliki/CQRS.html)
- [Outbox 패턴](https://microservices.io/patterns/data/transactional-outbox.html)
- [Hibernate Envers 공식 문서](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#envers)

---

## 13. Redis Codec — ForyFast 지원 추가 ✅

- Issue: [#113](https://github.com/bluetape4k/bluetape4k-projects/issues/113) — **CLOSED COMPLETED (2026-04-24)**

ForyBinarySerializer.fast() (SCHEMA_CONSISTENT, refTracking=false) 활용 고성능 Redis Codec.

### 구현 완료

- [x] `io/io` `BinarySerializers.kt` — `ForyFast`, `LZ4ForyFast`, `ZstdForyFast`, `SnappyForyFast` 추가
- [x] `infra/redisson` `ForyFastCodec.kt` — Redisson BaseCodec 구현 완료
- [x] `infra/redisson` `RedissonCodecs.kt` — `ForyFast`, `LZ4ForyFast`, `ZstdForyFast` 추가
- [x] `infra/lettuce` `LettuceBinaryCodecs.kt` — `foryFast()`, `lz4ForyFast()`, `zstdForyFast()` 추가
- [x] 각 Codec 테스트 완료
- [x] 기존 Codec 벤치마크에 ForyFast 비교군 추가

### 제약사항 (반드시 숙지)

- ForyFast(SCHEMA_CONSISTENT)와 Fory(COMPATIBLE) 포맷 상호 비호환 → fallback = Kryo5
- 순환 참조 객체 불가, 스키마 진화 불가
- **휘발성 캐시 전용** — DB/파일 영속 데이터에 사용 금지

#### 참고 자료
- [Apache Fory 공식 사이트](https://fory.apache.org/)
- [apache/fory GitHub](https://github.com/apache/fory)
- [Fory Java 사용 가이드](https://fory.apache.org/docs/start/install/)
- [Redisson Codec 커스터마이징](https://github.com/redisson/redisson/wiki/4.-data-serialization)
- [Lettuce 커스텀 Codec](https://lettuce.io/core/release/reference/#codecs)
- [LZ4 Java](https://github.com/lz4/lz4-java)
- [Zstd Java](https://github.com/luben/zstd-jni)

---

## 14. AWS 서비스 에뮬레이터 전환 — LocalStack → floci ✅

- Issue: [#155](https://github.com/bluetape4k/bluetape4k-projects/issues/155) — **CLOSED COMPLETED (2026-04-26)**

> **배경 (2026-04-25 기준)**:
> - LocalStack GitHub(`localstack/localstack`) **2026-03-23 아카이브**. BSL 라이선스, KMS/CloudFront/EBS 유료 이동.
> - MinIO GitHub(`minio/minio`) **2026-04-25 아카이브** → AIStor로 리브랜딩.
> - **floci** (MIT, GraalVM Native, 24ms 시작, 13MiB 메모리, 33 서비스, 공식 Testcontainers 모듈) — **1순위 대안**.
> - MiniStack/fakecloud는 floci 미지원 서비스 필요 시 fallback 검토.

### 14.1 올인원 AWS 에뮬레이터 비교

| 에뮬레이터 | 지원 서비스 | 라이선스 | 이미지 크기 | 메모리 | 시작 시간 | TC 공식 모듈 | 상태 |
|-----------|------------|---------|------------|-------|---------|------------|------|
| **floci** | 33개 (S3/SQS/SNS/DynamoDB/Lambda/KMS/RDS-실제/ElastiCache-실제/MSK/Step Functions/...) | **MIT** | **90MB** | **13MiB** | **24ms** | ✅ 공식 | ✅ 매우 활성 (2026-02~) |
| **MiniStack** | 40+ (동일 + Athena/Transfer Family/...) | **MIT** | ~270MB | ~40MB | <1초 | 커뮤니티 | ✅ 활성 (2.2k ⭐) |
| **fakecloud** | 23개 (1,680 operations, Smithy 기반) | AGPL-3.0 | 19MB | 10MiB | 500ms | Java SDK 포함 | ✅ 활성 (2026-04~, Rust) |
| LocalStack (Hobby) | 30+ | BSL | ~1GB | ~500MB | ~20초 | ✅ 공식 | ⚠️ 아카이브 |
| LocalStack (Ultimate) | 110+ | 독점 | ~1GB | ~500MB | ~20초 | ✅ 공식 | 💰 $89/월 |
| Moto (서버 모드) | 100+ | Apache 2.0 | Python | 낮음 | 빠름 | ❌ | ✅ 활성 (Python 전용) |

**floci 핵심 차별점 (2026년 기준 최우선 추천):**
- GraalVM Native Image → 24ms 시작, 메모리 13MiB, Docker 이미지 90MB
- **공식 Testcontainers 모듈** — `io.floci:floci-testcontainers`
- RDS: 실제 PostgreSQL/MySQL 컨테이너, ElastiCache: 실제 Redis/Valkey
- MSK (Kafka), ECS, API Gateway v2, Cognito, Step Functions 포함

**MiniStack 추가 차별점:**
- Athena (실제 DuckDB), Transfer Family, CloudFront, WAF v2 지원
- `EDGE_PORT` 등 LocalStack 호환 환경변수로 드롭인 교체 가능

### 14.2 서비스 전용 에뮬레이터

| 에뮬레이터 | 서비스 | 언어 | 라이선스 | TC 통합 | Stars | 상태 |
|-----------|-------|------|---------|---------|-------|------|
| **ElasticMQ** | SQS 전용 | Scala/Pekko | Apache 2.0 | JVM 임베드 가능 | 2.8k | ✅ 활성 (JVM 네이티브 최적) |
| **DynamoDB Local** | DynamoDB 전용 | Java (AWS) | AWS 약관 | GenericContainer | - | ✅ AWS 공식 |
| **AIStor Free** (구 MinIO) | S3 전용 | Go | AGPLv3 | ✅ 공식 모듈 | 60.8k (아카이브) | ⚠️ 아카이브→AIStor |
| **GoAWS** | SQS + SNS | Go | MIT | GenericContainer | 836 | ✅ 활성 |
| **AWS SAM Local** | Lambda + API GW | Python | Apache 2.0 | 간접 가능 | 6.7k | ✅ AWS 공식 |
| **Mailpit** | SMTP (SES 대체) | Go | MIT | 커뮤니티 모듈 | 9.1k | ✅ 매우 활성 |
| **WireMock** | HTTP mock (범용) | Java | Apache 2.0 | ✅ 공식 모듈 | 6k+ | ✅ 매우 활성 |
| **Scality CloudServer** | S3 멀티백엔드 | JS | Apache 2.0 | GenericContainer | 2k | 보통 |
| ~~Step Functions Local~~ | Step Functions | Java | AWS | GenericContainer | - | ❌ 아카이브 (2년 방치) |
| ~~s3rver~~ | S3 | JS | MIT | - | 601 | ❌ 2025-09 아카이브 |
| ~~MailHog~~ | SMTP | Go | MIT | - | 14.6k | ❌ 2020년 중단 |
| ~~dynalite~~ | DynamoDB | JS | Apache 2.0 | - | 1k | ❌ 2020년 이후 중단 |

### 14.3 서비스별 최적 에뮬레이터 선택 가이드

| AWS 서비스 | 권장 (올인원) | 권장 (전용) | 비고 |
|-----------|-------------|------------|------|
| S3 | **floci** | AIStor Free (TC 공식) | MinIO 아카이브됨 |
| SQS | **floci** | ElasticMQ (JVM 임베드) | SQS 단독 시 ElasticMQ 최적 |
| SQS + SNS | **floci** | GoAWS | GoAWS: MIT, Docker, 경량 |
| DynamoDB | **floci** | DynamoDB Local | AWS 공식 = API 완전 보장 |
| SES/SES v2 | **floci** | Mailpit (SMTP 캡처) | MailHog 대체 → Mailpit |
| Secrets Manager | **floci** | - | 전용 에뮬레이터 없음 |
| KMS | **floci** | - | LocalStack Hobby 유료 이동 |
| Kinesis | **floci** | - | Kinesalite 유지보수 중단 |
| Lambda + API GW | **floci** | AWS SAM Local | SAM = 실제 Lambda 런타임 |
| Step Functions | **floci** | - | AWS 공식 Docker 아카이브됨 |
| RDS | **floci** | TC PostgreSQL | 실제 컨테이너 권장 |
| ElastiCache | **floci** | TC Redis | 실제 컨테이너 권장 |
| MSK (Kafka) | **floci** | TC Kafka | floci = 실제 Kafka 컨테이너 |
| HTTP API mock | - | **WireMock** (TC 공식) | AWS API 형태만 흉내낼 때 |
| 이메일 캡처 | - | **Mailpit** | REST API + Web UI 제공 |

### 14.4 전환 작업 항목

**Phase 1 — floci 도입 완료**

- [x] `testing/testcontainers/` 에 `FlociServer.kt` 추가 완료
- [x] `testing/testcontainers/` 에 `ElasticMqEmbeddedServer.kt` 추가
- [x] `testing/testcontainers/` 에 `MailpitServer.kt` 추가
- [x] `aws/`, `aws-kotlin/` 모듈 테스트: floci 전환 완료

**Phase 2 — MiniStack 병행 검토**

- [ ] floci vs MiniStack API 호환성 비교 테스트 (필요 시 진행)

**Phase 3 — 레거시 정리 완료**

- [x] `LocalStackServer.kt` — `@Deprecated` 마킹 완료
- [x] `LocalStackServer` 사용처 floci로 교체
- [x] CI/CD nightly-tests.yml Docker 이미지 전환 완료

### 14.5 floci Testcontainers 통합 예시

```kotlin
// floci 공식 Testcontainers 모듈 활용
@Testcontainers
class AwsIntegrationTest {
    companion object {
        @Container
        val floci = FlociContainer()  // io.floci:floci-testcontainers
            .withServices(FlociService.S3, FlociService.SQS, FlociService.DYNAMODB)
    }

    fun s3Client() = S3Client.builder()
        .endpointOverride(floci.endpointOverride())
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")
        ))
        .region(Region.US_EAST_1)
        .build()
}

// ElasticMQ 임베디드 (SQS 전용, Docker 불필요)
class ElasticMqEmbeddedServer(port: Int = 9324) : AutoCloseable {
    private val server = SQSRestServerBuilder
        .withPort(port)
        .withInterface("localhost")
        .start()

    fun sqsClient() = SqsClient.builder()
        .endpointOverride(URI.create("http://localhost:$port"))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("x", "x")
        ))
        .region(Region.US_EAST_1)
        .build()

    override fun close() = server.stopAndWait()
}

// Mailpit (SES SMTP 캡처)
class MailpitServer : GenericContainer<MailpitServer>("axllent/mailpit:latest") {
    init {
        withExposedPorts(1025, 8025)  // SMTP, Web UI
    }
    fun smtpPort() = getMappedPort(1025)
    fun webUiUrl() = "http://localhost:${getMappedPort(8025)}"
}
```

### 14.6 리스크 / 고려사항

- **floci**: 2026-02 출시 (약 3개월, 생태계 미성숙). 33 서비스 중 엣지 케이스 API 호환성 검증 필요
- **MiniStack**: 드롭인 교체 가능하나 공식 TC 모듈 없음 (커뮤니티)
- **fakecloud**: AGPL-3.0 라이선스 — 라이브러리로 배포 시 주의
- **ElasticMQ**: Scala 의존성 (JAR 크기 증가 ~15MB)
- **LocalStack 마이그레이션**: 기존 `LocalStackServer` 사용처가 많을 경우 점진적 교체 필요

#### 참고 자료
- [floci 공식 GitHub](https://github.com/floci-io/floci) — MIT, Java/GraalVM Native
- [floci Testcontainers 모듈](https://github.com/floci-io/floci/tree/main/testcontainers)
- [MiniStack 공식 사이트](https://ministack.org)
- [ministackorg/ministack GitHub](https://github.com/ministackorg/ministack)
- [fakecloud GitHub](https://github.com/faiscadev/fakecloud) — Rust, AGPL-3.0
- [LocalStack 아카이브 공지](https://github.com/localstack/localstack)
- [ElasticMQ GitHub (softwaremill)](https://github.com/softwaremill/elasticmq)
- [DynamoDB Local Docker Hub](https://hub.docker.com/r/amazon/dynamodb-local)
- [AIStor (구 MinIO)](https://min.io)
- [Moto GitHub](https://github.com/getmoto/moto)
- [GoAWS GitHub](https://github.com/Admiral-Piett/goaws)
- [AWS SAM CLI GitHub](https://github.com/aws/aws-sam-cli)
- [WireMock Testcontainers 모듈](https://wiremock.org/docs/solutions/testcontainers/)
- [Mailpit GitHub](https://github.com/axllent/mailpit)
- [Scality CloudServer GitHub](https://github.com/scality/cloudserver)
- [Testcontainers LocalStack 모듈](https://java.testcontainers.org/modules/localstack/)
- [Testcontainers MinIO 모듈](https://java.testcontainers.org/modules/minio/)

---

## 15. data/hibernate — Hibernate 7.x 업그레이드 ✅

- Issue: [#179](https://github.com/bluetape4k/bluetape4k-projects/issues/179) — **CLOSED COMPLETED (2026-04-27)**
- [x] Hibernate ORM 6.6.44 → 7.2.7.Final 업그레이드 완료
- [x] Hibernate Reactive 2.4.11 → 3.2.0.Final 업그레이드 완료
- [x] `data/hibernate` 테스트 커버리지 70.4% 달성
- [x] HC5 비동기 클라이언트 CM 격리 문제 해결 (`minimalHttpAsyncClient` 독립 CM 전환)

#### 참고 자료
- [Hibernate ORM 7.x 마이그레이션 가이드](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html)
- [Hibernate Reactive 3.x 릴리스 노트](https://github.com/hibernate/hibernate-reactive/releases)

---

## 16. 테스트 커버리지 70%+ 달성 ✅ (2026-04-27)

최근 완료된 커버리지 향상 작업:

| 모듈 | 이전 | 달성 | 이슈 |
|------|------|------|------|
| `exposed-r2dbc` | 47.60% | 89.11% | [#176](https://github.com/bluetape4k/bluetape4k-projects/issues/176) |
| `infra/nats` | 49% | 79.45% | [#177](https://github.com/bluetape4k/bluetape4k-projects/issues/177) |
| `io/http` | 32.40% | 72% | [#178](https://github.com/bluetape4k/bluetape4k-projects/issues/178) |
| `data/hibernate` | - | 70.4% | [#179](https://github.com/bluetape4k/bluetape4k-projects/issues/179) |
| `utils/math` | 65.4% | 70.7% | [#181](https://github.com/bluetape4k/bluetape4k-projects/issues/181) |

### 커버리지 70% 미달 모듈 추적 🟡

- Issue: [#152](https://github.com/bluetape4k/bluetape4k-projects/issues/152) — README Mermaid UML / KDoc 커버리지 확대
- [ ] Dokka 보고서 기반 KDoc 미작성 public API 전수 파악
- [ ] 커버리지 미달 모듈 목록 업데이트 (Kover 리포트 활용)

---

## 17. 장기 오픈 이슈 처리 🟢

### 17.1 bucket4j + Exposed Rate Limiting ✅ **이관됨**

- Issue: [#38](https://github.com/bluetape4k/bluetape4k-projects/issues/38) — **CLOSED (2026-05-08, 이관)**
- 추적: bluetape4k-exposed [#4](https://github.com/bluetape4k/bluetape4k-exposed/issues/4)

### 17.2 Spring Modulith + Exposed ✅ **이관됨**

- Issue: [#25](https://github.com/bluetape4k/bluetape4k-projects/issues/25) — **CLOSED (2026-05-08, 이관)**
- 추적: bluetape4k-exposed [#5](https://github.com/bluetape4k/bluetape4k-exposed/issues/5)

---

## 18. Vert.x 4.x → 5.x 업그레이드 🟢

- Issue: [#197](https://github.com/bluetape4k/bluetape4k-projects/issues/197)
- 현재 버전: `4.5.26` (`buildSrc/src/main/kotlin/Libs.kt`)
- 목표 버전: `5.x` 최신 안정 릴리스
- 참고: [Vert.x 5 Migration Guide](https://vertx.io/docs/guides/vertx-5-migration-guide/)

### 영향 모듈

| 모듈 | 경로 | 비고 |
|------|------|------|
| `bluetape4k-vertx` | `io/vertx/` | 핵심 모듈, 전면 API 변경 |
| `bluetape4k-feign` | `io/feign/` | Vert.x HTTP client 사용 |
| `bluetape4k-retrofit2` | `io/retrofit2/` | Vert.x client 사용 |
| `bluetape4k-http` | `io/http/` | Vert.x HTTP 지원 |
| `bluetape4k-micrometer` | `infra/micrometer/` | Vert.x Micrometer metrics |
| `bluetape4k-hibernate-reactive` | `data/hibernate-reactive/` | Hibernate Reactive (Vert.x 기반) |

### x-obsoleted 삭제 (5.x 마이그레이션 후 정리)

- [ ] `x-obsoleted/vertx-coroutines/` 삭제 — `io/vertx/`로 통합됨
- [ ] `x-obsoleted/vertx-sqlclient/` 삭제 — `io/vertx/`로 통합됨
- [ ] `x-obsoleted/vertx-webclient/` 삭제 — `io/vertx/`로 통합됨

### Phase T0: 버전 업 + 컴파일 픽스

- [ ] `buildSrc/Libs.kt`: `vertx = "4.5.26"` → `5.x` 최신 버전으로 변경
- [ ] `resilience4j_vertx` 호환 버전 확인 및 업그레이드
- [ ] `data/hibernate-reactive/` Hibernate Reactive 3.x + Vert.x 5.x 호환 확인
- [ ] 전체 컴파일 오류 목록 수집: `./gradlew compileKotlin --continue 2>&1 | rg "error:"`

### Phase T1: API 마이그레이션 (주요 Breaking Changes)

#### Kotlin coAwait 변경

- [ ] `xxxAwait(port, host)` → `xxx(host, port).coAwait()` 전환
  - 예: `server.listenAwait(port)` → `server.listen(port).coAwait()`
  - 대상: `io/vertx/`, `io/http/` 전체 `*Await` 호출부 검색
  - `rg "Await\(" io/vertx/ io/http/ --type kotlin`

#### Vertx 빌더 패턴 변경

- [ ] `Vertx.vertx(VertxOptions().setMetricsOptions(...))` → `Vertx.builder().withMetrics(factory).build()`
  - 대상: `infra/micrometer/` Vert.x Micrometer 설정
  - `MicrometerMetricsOptions.setMicrometerRegistry()` → `VertxBuilder.withMetrics(MicrometerMetricsFactory(registry))`

#### SQL Client 빌더 패턴 변경

- [ ] `PgPool.pool(vertx, opts, poolOpts)` → `PgBuilder.pool().with(poolOpts).connectingTo(opts).using(vertx).build()`
- [ ] `MySQLPool.pool(...)` → `MySQLBuilder.pool()...build()` 동일 패턴
- [ ] 대상: `io/vertx/` SQL 클라이언트 래퍼 전체

#### WebSocket 클라이언트 변경

- [ ] `httpClient.webSocket(...)` → `vertx.createWebSocketClient().connect(...)`
- [ ] `HttpClientOptions.setMaxPoolSize()` → `PoolOptions.setHttp1MaxSize()`

#### executeBlocking 변경

- [ ] `executeBlocking { promise -> promise.complete(x) }` → `executeBlocking { x }` (Callable 스타일)
- [ ] `rg "executeBlocking" io/vertx/ --type kotlin`

#### CompositeFuture 변경

- [ ] `CompositeFuture.all((list as List<Future<Any>>))` → `Future.all(list)`
- [ ] `CompositeFuture.any(...)` → `Future.any(...)`
- [ ] `CompositeFuture.join(...)` → `Future.join(...)`

#### Worker 스레딩 모델 변경

- [ ] `DeploymentOptions().setWorker(true)` → `DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)`

#### Vert.x JUnit5 변경

- [ ] `testContext.succeeding { ... }` → `testContext.succeedingThenComplete { ... }`
- [ ] 대상: `io/vertx/src/test/` 전체 테스트

#### 제거된 모듈 처리 (Sunsetting)

- [ ] `vertx-jdbc-client` 의존성 제거 — 5.x에서 삭제됨 (SQL Client로 대체)
- [ ] RxJava 2 Vert.x 연동 코드 제거 (존재 시)
- [ ] `NoStackTraceThrowable` catch → `VertxException` 또는 `Exception`으로 변경

### Phase T2: 테스트 및 검증

- [ ] `io/vertx/` 단위/통합 테스트 전수 통과
- [ ] `io/http/` HTTP 클라이언트 통합 테스트
- [ ] `infra/micrometer/` Vert.x 메트릭 수집 검증
- [ ] `data/hibernate-reactive/` R2DBC/Reactive 통합 테스트
- [ ] 각 모듈 README.md + README.ko.md 업데이트

### 참고 자료

- [Vert.x 5 Migration Guide](https://vertx.io/docs/guides/vertx-5-migration-guide/)
- [Vert.x 5.x Release Notes](https://github.com/eclipse-vertx/vert.x/releases)
- [Hibernate Reactive 3.x + Vert.x 5.x](https://hibernate.org/reactive/)

---

## 완료 기준

각 항목은 다음 조건을 모두 만족해야 완료:

- [ ] 코드 변경 완료
- [ ] 단위/통합 테스트 통과
- [ ] README.md + README.ko.md 업데이트
- [ ] testlog 기록 (불필요 — 생략)
