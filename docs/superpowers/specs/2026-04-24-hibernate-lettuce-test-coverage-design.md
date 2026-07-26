# hibernate-lettuce 테스트 커버리지 70%+ 설계 스펙

- 작성일: 2026-04-24
- 대상 모듈: `spring-boot3/hibernate-lettuce`, `spring-boot4/hibernate-lettuce`
- 목표: JaCoCo LINE coverage 70% 이상 (class/branch coverage도 함께 개선)
- 작성자: general-purpose agent (Opus 4.7)

---

## 1. 문제 재정의

### 1.1 현재 상태

두 모듈은 동일한 6개 소스 클래스로 구성된다:

1. `LettuceNearCacheHibernateAutoConfiguration` — `HibernatePropertiesCustomizer` 등록
2. `LettuceNearCacheSpringProperties` — `@ConfigurationProperties` 바인딩 + 중첩 `LocalProperties` / `RedisTtlProperties` /
   `MetricsProperties`
3. `LettuceNearCacheActuatorAutoConfiguration` — `LettuceNearCacheActuatorEndpoint` 빈 등록
4. `LettuceNearCacheActuatorEndpoint` — `@Endpoint(id="nearcache")` REST 엔드포인트 (`getAllRegionStats`,
   `getRegionStats(name)`)
5. `LettuceNearCacheMetricsAutoConfiguration` — `LettuceNearCacheMetricsBinder` 빈 등록
6. `LettuceNearCacheMetricsBinder` — `SmartInitializingSingleton`로 Micrometer Gauge 2개 등록

### 1.2 기존 테스트와 커버리지 갭

**기존 테스트**

- `LettuceNearCacheAutoConfigurationTest` — `ApplicationContextRunner` 기반 ~9 메서드. Bean 등록/프로퍼티 바인딩/스위치 검증에 국한.
- `LettuceNearCacheIntegrationTest` — `RedisServer` + H2 + JPA 통합, 3 메서드. 엔티티 저장/병렬 조회에 한정.

**갭**

| 영역                       | 미커버 로직                                                                                                                                                                                               |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ActuatorEndpoint           | `getAllRegionStats()` / `getRegionStats(name)` 실제 호출, 존재하지 않는 region → `null`, 잘못된 `RegionFactory`(LettuceNearCacheRegionFactory 아님) → 빈 map/null, statistics 비활성 → nullable 필드 null |
| MetricsBinder              | `afterSingletonsInstantiated()` 직접 호출, Gauge 값 (`active.regions`, `total.local.size`) 실제 값 검증, 잘못된 RegionFactory 조기 return, `runCatching` 실패 경로 로깅                                   |
| HibernateAutoConfiguration | `codec=zstdfory`/`useResp3=false` 변형, `local.maxSize` 커스텀, `redisTtl.regions` 다건 매핑, `metrics.enabled=false` 시 statistics 미설정, `Duration`의 ms 잔여 (1500ms) → "1500ms" 포맷 분기            |
| Integration                | Hibernate `Statistics` 기반 hit/miss 카운트 검증, update/delete 후 eviction, Actuator endpoint가 실제 stats 반환, MeterRegistry에서 Gauge 실제 값 조회                                                    |

### 1.3 목표 지표

- JaCoCo LINE coverage ≥ 70% (현재 추정 ~45~55%)
- 핵심 비분기 로직 (endpoint, metrics binder)의 BRANCH coverage ≥ 60%
- boot3/boot4 양쪽 모두 동일 수준 달성

---

## 2. 설계 리스크 / 실패 모드 (최소 3개)

### R1. Hibernate `Statistics` 값이 테스트 시점에 비동기로 업데이트 — 카운트 assertion 간헐 실패

- **원인**: Hibernate L2 cache stats는 트랜잭션 커밋/세션 clear 타이밍에 영향을 받음. 로드 직후 `hitCount`가 0일 수 있음.
- **완화**: `SessionFactoryImplementor.statistics.clear()` 후 작업 →
  `entityManager.clear()` (first-level cache flush) → 재조회 → 카운트 검증. `await` 루프 대신 명시적 flush +
  `TransactionTemplate.executeWithoutResult` 분리.

### R2. `RedisServer.Launcher.redis` 싱글턴 상태 누수 — 테스트 간 region 공유

- **원인**: Redis는 JVM 단위 공유. 이전 테스트의 key가 남아 hit/miss 카운트를 오염.
- **완화**: 각 테스트에서 고유 entity 이름 (`@Entity(name=...)`) 또는 각 테스트 클래스마다 다른 DB URL + DDL `create-drop`. Redis는 `FLUSHALL`을
  `@BeforeEach`에서 호출 (LettuceNearCacheRegionFactory 내부 Lettuce client 공유시) — 간단히는 다른 테스트 클래스 이름마다 고유 region 이름을 entity @Cache region으로 격리.

### R3. `LettuceNearCacheRegionFactory#getCaches()` 접근 시점 — Hibernate 부트스트랩 완료 전 호출 시 빈 map

- **원인**: `buildEntityManagerFactory`가 lazy하거나 첫 쿼리 전까지 region이 생성되지 않음.
- **완화**: 테스트에서 endpoint/metrics 검증 전 반드시 `itemRepository.save(...)` + `findById(...)` 등 최소 한 번의 L2 cache 적재 수행. 이후
  `endpoint.getAllRegionStats()` 호출.

### R4. `ApplicationContextRunner`는 Actuator Web 엔드포인트를 자동 등록하지 않음

- **원인**: `@Endpoint`는 `ManagementContextAutoConfiguration` 등 추가 auto-config가 없으면 bean 등록만 되고 HTTP 경로는 없음.
- **완화**: HTTP 요청이 아니라 `context.getBean(LettuceNearCacheActuatorEndpoint::class.java)`를 직접 호출해
  `@ReadOperation` 메서드를 invoke. HTTP 계층 테스트는 scope 밖 (통합 테스트에서만 선택적).

### R5. Spring Boot 3 vs 4 간 `HibernatePropertiesCustomizer` 패키지 차이

- **원인**: boot3는 `org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer`, boot4는
  `org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer` 계열 (정확 경로는 실제 빌드 시 확인).
- **완화**: 테스트 본문은 동일 로직으로 작성하되 import만 모듈별로 맞춤. 공통 테스트 DSL을 `internal fun` helper로 각 모듈 내부에 두고 중복 허용 (KISS).

### R6. `runCatching { ... }.getOrNull()` 로 삼켜진 실패 경로 — 테스트로 도달 어려움

- **원인**: endpoint/binder 모두 `EntityManagerFactory.unwrap`이 실패하면 silent fallback. Mock으로 유도해야 함.
- **완화**: MockK로 `EntityManagerFactory`를 만들고 `unwrap`이 `PersistenceException`을 throw하도록 구성 → `getAllRegionStats()` 가
  `emptyMap()`을 반환함을 검증.

---

## 3. 접근 방식 비교 (3개)

### A. Full Mock 단위 테스트 위주 (MockK + bluetape4k-assertions)

- **장점**: 빠름 (Redis/JPA 부팅 없음), 분기 직접 강제 가능 (`RegionFactory`가
  `LettuceNearCacheRegionFactory` 아닌 경우, statistics null 등), CI 안정적
- **단점**: 실제 Hibernate ↔ Lettuce 상호작용 미검증, `getDomainDataRegionStatistics` 등 Hibernate 내부 API 계약 변화 취약
- **커버리지 기여**: endpoint/metrics binder의 분기 로직 커버 최상

### B. `@SpringBootTest` + 실제 Redis 통합 위주 확장

- **장점**: 프로덕션 경로 그대로 검증, hit/miss 실제 확인
- **단점**: 테스트 실행 시간 증가, 간헐 실패 (R1/R2), 분기 강제가 어려움 (mock 주입 불가)
- **커버리지 기여**: 통합 경로 + AutoConfiguration 빈 등록은 잘 커버되지만 실패 분기는 누락

### C. **하이브리드 (Mock 단위 + 최소한의 통합)** ← **채택**

- **구성**
    1. `LettuceNearCacheActuatorEndpointTest` — MockK로 `EntityManagerFactory`/`SessionFactoryImplementor`/
       `RegionFactory` 조립, 모든 분기 검증
    2. `LettuceNearCacheMetricsBinderTest` — MockK + `SimpleMeterRegistry`로 Gauge 실제 값 조회
    3. `LettuceNearCacheHibernatePropertiesCustomizerTest` — `@ConfigurationProperties` 바인딩 →
       `HibernatePropertiesCustomizer` 호출 → `hibernateProperties` map 내용 검증 (Spring 없이 POJO)
    4. `LettuceNearCacheIntegrationTest` 확장 — 기존 3 + 신규 3 메서드 (Statistics 검증, update eviction, endpoint/metrics 실데이터 검증)
- **장점**: 커버리지 극대화 + 실행 시간 분산 + 분기/통합 균형
- **단점**: 테스트 파일 수 증가 (수용 가능)

**선택 근거**: 기존 구조를 유지하면서 갭을 정확히 메움. Mock 단위 테스트로 분기 강제 → 70% 커버리지 달성에 결정적. 통합은 회귀 방지 용도.

---

## 4. 추가 테스트 파일 목록 (boot3/boot4 동일 구조)

각 파일 경로는 상대 경로로 표기. 양 모듈에 동일 작성.

```
src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/
├── LettuceNearCacheActuatorEndpointTest.kt            (신규)
├── LettuceNearCacheMetricsBinderTest.kt               (신규)
├── LettuceNearCachePropertiesCustomizerTest.kt        (신규)
├── LettuceNearCacheAutoConfigurationTest.kt           (기존 유지, 소폭 보강)
└── LettuceNearCacheIntegrationTest.kt                 (기존 확장)
```

### 4.1 `LettuceNearCacheActuatorEndpointTest.kt` (신규, 순수 단위)

`EntityManagerFactory`, `SessionFactoryImplementor`, `LettuceNearCacheRegionFactory`, `Statistics`를 MockK로 구성.

| # | 테스트 메서드                                                                  | 검증 포인트                                                                                              |
|---|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| 1 | `getAllRegionStats는 region 이름을 key로 하는 map을 반환한다`                  | mock factory에 2개 region → 반환 map size=2, key 정확                                                    |
| 2 | `getAllRegionStats는 LettuceNearCacheRegionFactory가 아니면 빈 map을 반환한다` | `RegionFactory`가 다른 구현 → `emptyMap()`                                                               |
| 3 | `getAllRegionStats는 RegionFactory가 null이면 빈 map을 반환한다`               | `serviceRegistry.getService(RegionFactory)` returns null                                                 |
| 4 | `getAllRegionStats는 unwrap이 예외를 던지면 빈 map을 반환한다`                 | `entityManagerFactory.unwrap` throws → runCatching 경로                                                  |
| 5 | `getRegionStats는 존재하는 region에 대해 RegionStats를 반환한다`               | 특정 name → `RegionStats.regionName` 일치 + `localSize` 전달                                             |
| 6 | `getRegionStats는 존재하지 않는 region에 대해 null을 반환한다`                 | factory.getCaches()에 없음 → null                                                                        |
| 7 | `getRegionStats는 statistics가 비활성이면 l2 필드가 null이다`                  | `isStatisticsEnabled=false` → `l2HitCount`/`l2MissCount`/`l2PutCount` null                               |
| 8 | `getRegionStats는 localStats가 null이면 local 카운트 필드가 null이다`          | `cache.localStats()=null` → `localHitRate`/`localHitCount`/... null, `localSize`는 `localCacheSize()` 값 |
| 9 | `getRegionStats는 getDomainDataRegionStatistics 예외를 흡수한다`               | `statistics.getDomainDataRegionStatistics` throws → l2 필드 null, local 필드는 정상                      |

### 4.2 `LettuceNearCacheMetricsBinderTest.kt` (신규, 순수 단위)

MockK + `io.micrometer.core.instrument.simple.SimpleMeterRegistry`.

| # | 테스트 메서드                                                                      | 검증 포인트                                                                                               |
|---|------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| 1 | `afterSingletonsInstantiated는 active_regions와 total_local_size Gauge를 등록한다` | 호출 후 `registry.find("lettuce.nearcache.active.regions").gauge()` != null, `total.local.size`도 != null |
| 2 | `active_regions Gauge는 현재 region 수를 반영한다`                                 | caches map size=3 → `gauge.value() == 3.0`                                                                |
| 3 | `total_local_size Gauge는 모든 region의 localCacheSize 합이다`                     | 두 region이 localCacheSize 100, 250 → `gauge.value() == 350.0`                                            |
| 4 | `RegionFactory가 LettuceNearCacheRegionFactory 아니면 등록을 스킵한다`             | 다른 타입 → registry의 gauge 수 증가 없음                                                                 |
| 5 | `RegionFactory가 null이면 등록을 스킵한다`                                         | getService returns null → gauge 미등록                                                                    |
| 6 | `unwrap 예외는 runCatching으로 흡수되고 로깅만 한다`                               | unwrap throws → 예외 전파 없음, gauge 미등록                                                              |
| 7 | `Gauge는 런타임에 region이 추가되면 새로운 값을 반영한다`                          | 첫 호출 1, 추가 후 `gauge.value()==2.0` (dynamic supplier 검증)                                           |

### 4.3 `LettuceNearCachePropertiesCustomizerTest.kt` (신규, 순수 단위 — 갭 집중)

> 기존 `LettuceNearCacheAutoConfigurationTest`가 이미 대부분의 변형 (codec, useResp3, maxSize, TTL, ms Duration)을 커버.
> 이 파일은 기존 테스트가 확인하지 않는 **갭 케이스**만 다룬다.

Spring 컨텍스트 없이 `LettuceNearCacheHibernateAutoConfiguration` 내 람다를 직접 구성·호출.

| # | 테스트 메서드                                                        | 검증 포인트                                                                                                                    |
|---|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| 1 | `redisTtl.regions 다건 매핑이 전부 properties에 추가된다`            | regions=mapOf("A" to 60s, "B" to 300s, "C" to 900s) → `redis_ttl.A == "60s"`, `redis_ttl.B == "300s"`, `redis_ttl.C == "900s"` |
| 2 | `metrics.enabled=false면 generate_statistics 키가 없다`              | enabled=false → `hibernate.generate_statistics` key 부재                                                                       |
| 3 | `metrics.enableCaffeineStats=false면 local.record_stats가 false이다` | flag=false → `hibernate.cache.lettuce.local.record_stats == "false"`                                                           |

### 4.4 `LettuceNearCacheAutoConfigurationTest.kt` (기존, 소폭 보강 1 메서드)

| # | 테스트 메서드                                           | 검증 포인트                                                                                             |
|---|---------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| + | `ActuatorEndpoint는 metrics.enabled=false여도 등록된다` | `@ConditionalOnProperty`가 Actuator 설정에 없음을 확인 — `metrics.enabled=false`여도 endpoint bean 존재 |

### 4.5 `LettuceNearCacheIntegrationTest.kt` (기존 확장, 신규 3 메서드)

| # | 테스트 메서드                                                            | 검증 포인트                                                                                                                               |
|---|--------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| + | `엔티티 재조회 시 Hibernate L2 cache hitCount가 증가한다`                | `SessionFactoryImplementor.statistics.clear()` → save → flush/clear → findById 2회 → `getDomainDataRegionStatistics(region).hitCount ≥ 1` |
| + | `Actuator endpoint가 저장된 엔티티 region의 RegionStats를 반환한다`      | save 후 `endpoint.getAllRegionStats()`에 entity FQN key 존재 + `localSize ≥ 0`                                                            |
| + | `Metrics Gauge가 active_regions와 total_local_size의 실제 값을 보고한다` | save 후 `meterRegistry.find("lettuce.nearcache.active.regions").gauge()!!.value() ≥ 1.0`                                                  |

(선택적) update/delete 후 eviction 검증은 Hibernate eviction 정책 의존 → 안정성 이유로 기본 목록에서 제외, 남는 시간이 있으면 추가.

---

## 5. 테스트 헬퍼 / Fixture

- **MockK 헬퍼**: `FakeLettuceNearCacheRegionFactoryBuilder` (internal).
  `withRegion(name, localSize, localStats)` 체이닝으로 mock factory 생성. 양 모듈에 동일 코드 복제 (공통 testFixtures로 끌어올리는 비용 > 이득).
- **`SimpleMeterRegistry`**: `io.micrometer.core.instrument.simple.SimpleMeterRegistry` 직접 인스턴스화. 외부 의존 없음.
- **통합 테스트 Redis 싱글턴**: 기존 `RedisServer.Launcher.redis` 그대로 사용.
- **MockK/bluetape4k-assertions
  가용성**: `bluetape4k-junit5`가 `api(Libs.mockk)` + `api(Libs.bluetape4kAssertions)`로 transitive export. 양 모듈 모두
  `testImplementation(project(":bluetape4k-junit5"))` 있으므로 Gradle 수정 불필요.
- **스타일 통일**: 신규 테스트 파일은 MockK + bluetape4k-assertions로 작성. 기존 파일 (`LettuceNearCacheAutoConfigurationTest.kt`,
  `LettuceNearCacheIntegrationTest.kt`)은 기존 스타일 유지.

---

## 6. Draft Task List

- [ ] T0. **worktree 생성**:
  `git worktree add .worktrees/hibernate-lettuce-test-coverage -b feat/hibernate-lettuce-test-coverage`
- [ ] T1. boot3: `LettuceNearCacheActuatorEndpointTest.kt` 작성 (9 메서드, MockK)
- [ ] T2. boot3: `LettuceNearCacheMetricsBinderTest.kt` 작성 (7 메서드, MockK + SimpleMeterRegistry)
- [ ] T3. boot3: `LettuceNearCachePropertiesCustomizerTest.kt` 작성 (3 메서드, 갭 케이스만)
- [ ] T4. boot3: `LettuceNearCacheAutoConfigurationTest.kt`에 1 메서드 추가
- [ ] T5. boot3: `LettuceNearCacheIntegrationTest.kt`에 3 메서드 추가 (Statistics/endpoint/metrics 실데이터)
- [ ] T6. boot3: `./bin/repo-test-summary -- ./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test` pass 확인
- [ ] T7. boot3: `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:jacocoTestReport` →
  `build/reports/jacoco/test/html/index.html` 에서 LINE ≥ 70% 확인
- [ ] T8. boot4: T1–T5 동일 작성
    - T8.1 import 차이 확인: `HibernatePropertiesCustomizer` (boot4:
      `org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer`)
    - T8.2 boot4 resolution strategy 확인 (build.gradle.kts 기존 전략 상속)
- [ ] T9. boot4: `./bin/repo-test-summary -- ./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test` pass 확인
- [ ] T10. boot4: JaCoCo LINE ≥ 70% 확인
- [ ] T11. 양 모듈 `README.md` + `README.ko.md` 테스트 섹션 동기화 업데이트 (언어 전환 링크 + Architecture→Features→Examples 순서)
- [ ] T12. `docs/testlogs/2026-04.md` 맨 위에 실행 결과 기록 (통과 수/스킵 수/실패 수 + 소요 시간)
- [ ] T13. 한국어 prefix commit: `test: spring-boot3/4 hibernate-lettuce 테스트 커버리지 70%+ 달성`
- [ ] T14. PR 생성 전 `/wiki-update` 실행
- [ ] T15. PR 생성

---

## 7. 완료 정의 (Definition of Done)

- [ ] 두 모듈 모두 `./gradlew test` 녹색
- [ ] JaCoCo LINE coverage 두 모듈 모두 ≥ 70%
- [ ] 신규 테스트 모두 JUnit 5 + MockK + bluetape4k-assertions 사용
- [ ] Hibernate `Statistics` 검증 테스트가 최소 3회 연속 재실행 (pass) 안정
- [ ] 회귀: 기존 테스트 전부 통과 유지
- [ ] README 동기화 + testlog 기록
