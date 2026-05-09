# hibernate-lettuce 테스트 커버리지 70%+ 구현 플랜

- 작성일: 2026-04-24
- 스펙: `docs/superpowers/specs/2026-04-24-hibernate-lettuce-test-coverage-design.md`
- 작업 디렉토리: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/hibernate-lettuce-test-coverage`
- 대상 모듈: `spring-boot3/hibernate-lettuce`, `spring-boot4/hibernate-lettuce`
- 목표: JaCoCo LINE coverage ≥ 70% (두 모듈 모두)

---

## 컨텍스트

- **언어/스택**: Kotlin 2.3, Spring Boot 3.5 / Spring Boot 4.0, Hibernate ORM, Lettuce (L2 cache)
- **테스트 스택**: JUnit 5 + MockK + bluetape4k-assertions + SimpleMeterRegistry (Micrometer)
- **소스 클래스 (모듈당 6개)**:
    1. `LettuceNearCacheHibernateAutoConfiguration`
    2. `LettuceNearCacheSpringProperties`
    3. `LettuceNearCacheActuatorAutoConfiguration`
    4. `LettuceNearCacheActuatorEndpoint`
    5. `LettuceNearCacheMetricsAutoConfiguration`
    6. `LettuceNearCacheMetricsBinder`
- **접근**: 하이브리드 (Mock 단위 + 최소 통합) — 스펙 §3 선택안 C

---

## 리스크 요약 (스펙 §2)

- **R1**: Hibernate Statistics 타이밍 → `statistics.clear()` + `entityManager.clear()` + 명시 flush
- **R2**: Redis 싱글턴 상태 누수 → 테스트 클래스별 고유 entity region 사용
- **R3**: `getCaches()` 부트스트랩 전 접근 → save + findById 선행
- **R4**: `ApplicationContextRunner`는 HTTP 엔드포인트 미등록 → `context.getBean()` 직접 호출
- **R5**: boot3/boot4 간 `HibernatePropertiesCustomizer` 패키지 차이 → import만 맞춤
- **R6**: `runCatching` silent 실패 경로 → MockK로 throw 유도

---

## Task List

### T0. Worktree 확인

- **complexity**: low
- **내용**: 작업 디렉토리 `.worktrees/hibernate-lettuce-test-coverage` 확인 (이미 생성된 상태 가정)
- **검증**: `pwd` 및 `git branch` 확인

---

### T1. boot3: `LettuceNearCacheActuatorEndpointTest.kt` 작성

- **complexity**: high
- **경로**:
  `spring-boot3/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpointTest.kt`
- **내용**: MockK 기반 9개 메서드 — `EntityManagerFactory`/`SessionFactoryImplementor`/`LettuceNearCacheRegionFactory`/
  `Statistics` mock 조립
- **메서드**: 스펙 §4.1 표 1~9
    1. 다건 region 반환 map 검증
    2. 다른 RegionFactory → emptyMap
    3. RegionFactory null → emptyMap
    4. unwrap throws → emptyMap
    5. getRegionStats 존재 region
    6. 존재하지 않는 region → null
    7. statistics disabled → l2 필드 null
    8. localStats null → local 필드 null
    9. getDomainDataRegionStatistics 예외 흡수
- **헬퍼**: internal `FakeLettuceNearCacheRegionFactoryBuilder` 구성
- **검증**: `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test --tests "*LettuceNearCacheActuatorEndpointTest"` 통과

---

### T2. boot3: `LettuceNearCacheMetricsBinderTest.kt` 작성

- **complexity**: high
- **경로**: `spring-boot3/hibernate-lettuce/src/test/kotlin/.../LettuceNearCacheMetricsBinderTest.kt`
- **내용**: MockK + `SimpleMeterRegistry` 기반 7개 메서드
- **메서드**: 스펙 §4.2 표 1~7
    1. Gauge 2개 등록 (`active.regions`, `total.local.size`)
    2. active_regions = region 수
    3. total_local_size = localCacheSize 합
    4. 다른 RegionFactory → 등록 skip
    5. RegionFactory null → 등록 skip
    6. unwrap 예외 → runCatching 흡수
    7. Gauge dynamic supplier 검증 (런타임 region 추가 반영)
- **검증**: 테스트 단독 실행 통과

---

### T3. boot3: `LettuceNearCachePropertiesCustomizerTest.kt` 작성

- **complexity**: medium
- **경로**: `spring-boot3/hibernate-lettuce/src/test/kotlin/.../LettuceNearCachePropertiesCustomizerTest.kt`
- **내용**: Spring 컨텍스트 없이 `LettuceNearCacheHibernateAutoConfiguration` 람다 직접 호출. 스펙 §4.3 갭 3 메서드
    1. `redisTtl.regions` 다건 매핑
    2. `metrics.enabled=false` → `generate_statistics` 키 부재
    3. `enableCaffeineStats=false` → `local.record_stats == "false"`
- **검증**: 테스트 통과

---

### T4. boot3: `LettuceNearCacheAutoConfigurationTest.kt` 보강

- **complexity**: low
- **경로**: 기존 파일 Edit
- **내용**: 1 메서드 추가 — `metrics.enabled=false`여도 Actuator endpoint bean 존재
- **검증**: 기존 테스트 전부 통과 + 신규 1건 통과

---

### T5. boot3: `LettuceNearCacheIntegrationTest.kt` 확장

- **complexity**: high
- **경로**: 기존 파일 Edit
- **내용**: 신규 3 메서드 (스펙 §4.5)
    1. L2 cache hitCount 증가 검증 (statistics.clear → save → em.clear → findById ×2)
    2. Actuator endpoint가 저장 entity region의 RegionStats 반환
    3. Metrics Gauge 실제 값 검증 (`active.regions`, `total.local.size`)
- **리스크 대응**: R1 (flush/clear 순서), R2 (entity name 격리), R3 (save 선행)
- **검증**: 테스트 3회 연속 실행 안정 확인

---

### T5.5. bluetape4k-patterns 체크리스트 점검

- **complexity**: low
- **내용**: 신규 테스트 파일에 대해 `/bluetape4k-patterns` 스킬로 다음 확인
    - `companion object : KLogging()` 누락 없음 (pure test class는 불필요)
    - bluetape4k-assertions assertion 사용 (`shouldBe`, `shouldBeNull`, 등)
    - MockK relaxed mock 남용 없음
- **검증**: 위반 없음

---

### T6. boot3: 테스트 실행 + JaCoCo 리포트

- **complexity**: low
- **내용**:
    - `./bin/repo-test-summary -- ./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test`
    - `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:jacocoTestReport`
    - `build/reports/jacoco/test/html/index.html`에서 LINE ≥ 70% 확인
- **검증**: LINE coverage 70% 이상 수치 기록
- **Gate**: T6 두 조건 모두 PASS(녹색 + ≥70%) 확인 후에만 T7 진행

---

### T7. boot4: T1–T5 동일 작성

- **complexity**: high
- **내용**:
    - T7.0 **boot4 `HibernatePropertiesCustomizer` FQN 선행 확인** —
      `mcp__intellij-index__ide_find_class` 또는 boot4 소스 import 검색으로 실제 패키지 확인
    - T7.1 `LettuceNearCacheActuatorEndpointTest.kt` 복제 (import 조정)
    - T7.2 `LettuceNearCacheMetricsBinderTest.kt` 복제
    - T7.3 `LettuceNearCachePropertiesCustomizerTest.kt` 복제 (import 조정)
    - T7.4 `LettuceNearCacheAutoConfigurationTest.kt` +1 메서드 (import 조정)
    - T7.5 `LettuceNearCacheIntegrationTest.kt` +3 메서드
- **R5 대응**: `HibernatePropertiesCustomizer` 패키지 확인 후 import 교체
    - boot3: `org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer`
    - boot4: T7.0에서 확인한 FQN 사용
- **검증**: 각 테스트 파일 단독 실행 통과

---

### T8. boot4: 테스트 실행 + JaCoCo 리포트

- **complexity**: low
- **내용**:
    - `./bin/repo-test-summary -- ./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test`
    - `./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:jacocoTestReport`
- **검증**: LINE coverage 70% 이상 확인

---

### T9. README.md + README.ko.md 양 모듈 업데이트

- **complexity**: low
- **내용**:
    - `spring-boot3/hibernate-lettuce/README.md` + `README.ko.md`
    - `spring-boot4/hibernate-lettuce/README.md` + `README.ko.md`
    - 언어 전환 링크 확인, Architecture → UML → Features → Examples → Testing 순서
    - Testing 섹션에 새 테스트 파일 목록과 커버리지 수치 추가
- **검증**: 두 언어 파일 내용 sync 확인

---

### T10. `docs/testlogs/2026-04.md` 기록

- **complexity**: low
- **내용**: 이달 파일 맨 위에 새 행 추가. 양 모듈의 pass/skip/fail, 소요 시간, JaCoCo LINE 수치 기록
- **검증**: 맨 위에 새 행이 있는지 확인

---

### T10.5. superpowers index 업데이트

- **complexity**: low
- **내용**:
    - `docs/superpowers/index/2026-04.md` 맨 위에 Evolution Event 항목 추가 (what/why/verification 형식)
    - `docs/superpowers/INDEX.md` 완료 카운트 +1
- **검증**: 두 파일 모두 업데이트 확인

---

### T10.6. `/wiki-update` 실행

- **complexity**: low
- **내용**: 스펙/플랜 생성 완료 후 `/wiki-update` 스킬 실행
- **검증**: 실행 완료 확인

---

### T11. 한국어 prefix commit

- **complexity**: low
- **내용**: `test: spring-boot3/4 hibernate-lettuce 테스트 커버리지 70%+ 달성`
    - 본문에 각 모듈별 커버리지 수치, 신규 테스트 파일 목록 포함
- **검증**: `git log -1` 확인

---

### T12. PR 생성

- **complexity**: low
- **내용**:
    - CLAUDE.md "Before Creating a PR" 체크리스트 확인
    - `gh pr create` 실행 (한국어 PR 설명, 커버리지 수치 포함)
- **검증**: PR URL 확인

---

## 완료 정의 (Definition of Done)

- [ ] 두 모듈 `./gradlew test` 녹색
- [ ] JaCoCo LINE coverage 두 모듈 모두 ≥ 70%
- [ ] 신규 테스트 JUnit 5 + MockK + bluetape4k-assertions 사용
- [ ] Hibernate Statistics 검증 테스트 3회 연속 안정
- [ ] 기존 테스트 회귀 없음
- [ ] README.md + README.ko.md 동기 업데이트
- [ ] testlog 기록 완료
- [ ] 한국어 prefix commit

---

## Task Complexity 요약

| Task | Complexity |
|------|------------|
| T0   | low        |
| T1   | high       |
| T2   | high       |
| T3   | medium     |
| T4   | low        |
| T5   | high       |
| T6   | low        |
| T7   | high       |
| T8   | low        |
| T9   | low        |
| T10  | low        |
| T11  | low        |
