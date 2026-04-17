# bluetape4k TODO

> 현재 버전: 1.7.0-SNAPSHOT | 브랜치: `develop` | 모듈 수: 134개
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

- [ ] `io/crypto/` — `@Deprecated` 암호화 API 제거 → `tink` 모듈로 대체 유도
  - digest, encrypt 관련 45개 파일 점진적 제거
- [ ] `io/http/` — `AHC`(AsyncHttpClient), `OkHttp3`, `HC5` 레거시 HTTP 클라이언트 정리
  - Retrofit2도 SB3/4 core에서 이미 제거됨 — io 모듈도 정리 대상 검토
- [ ] `io/jackson2/`, `io/jackson3/` — deprecated 직렬화 API 정리

### 2.2 core 모듈 Deprecated 정리 🟡

- [ ] `bluetape4k/core/` — 21개 파일 `@Deprecated` 항목 점검
  - 대체 API가 있는 경우 마이그레이션 가이드 제공 후 제거

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

14개 레거시 모듈에 대한 명확한 정책 결정 필요:

| 모듈 | 권장 처리 |
|------|---------|
| `vertx-coroutines`, `vertx-sqlclient`, `vertx-webclient` | 완전 제거 (Vert.x 통합 → 별도 프로젝트) |
| `mutiny-examples` | 완전 제거 (Mutiny → utils/mutiny 통합 완료) |
| `nats` | 재검토 (NATS 수요 있으면 infra로 승격) |
| `bloomfilter`, `naivebayes` | 재검토 (ML/검색 수요 있으면 utils로 승격) |
| `tokenizer`, `ahocorasick`, `lingua` | 재검토 (NLP 수요 있으면 승격) |
| `javers`, `mapstruct`, `captcha` | 완전 제거 |
| `logback-kafka` | infra/kafka 로 통합 검토 |

- [ ] 각 모듈별 처리 결정 후 `settings.gradle.kts`에서 제외 또는 이동
- [ ] `x-obsoleted/` 디렉토리 최종 삭제

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
- [ ] GitHub Actions CI 파이프라인 구성 (현재 없음)

---

## 9. 보안 🔴

- [ ] `io/crypto/` deprecated 암호화 → `tink` 완전 대체
- [ ] `gitleaks detect` — 시크릿 스캔 CI 연동
- [ ] 의존성 취약점 스캔 — `./gradlew dependencyCheckAnalyze` 주기 실행
- [ ] `exposed-jasypt` — Jasypt 최신 버전 호환성 확인

---

## 10. 성능 / 품질 🟢

- [ ] `utils/benchmark` 모듈 결과 문서화 (현재 결과 미공개)
- [ ] `infra/lettuce` NearCache 성능 벤치마크 공개
- [ ] Coroutines structured concurrency 감사 — `GlobalScope` 사용처 제거
- [ ] `StateFlow` / `SharedFlow` 사용 일관성 검토

---

## 완료 기준

각 항목은 다음 조건을 모두 만족해야 완료:

- [ ] 코드 변경 완료
- [ ] 단위/통합 테스트 통과
- [ ] README.md + README.ko.md 업데이트
- [ ] testlog 기록 (`wiki/testlogs/YYYY-MM.md`)
