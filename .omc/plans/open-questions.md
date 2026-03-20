# Open Questions

## spring-mongodb - 2026-03-03

- [ ] `data/mongodb` 모듈과의 의존 관계: `spring/mongodb`가 `data/mongodb`를
  `api()` 의존성으로 가져갈지, 아니면 독립적으로 Spring Data MongoDB만 사용할지 -- `data/mongodb`는 Native Kotlin Coroutine Driver 기반이고
  `spring/mongodb`는 Spring Data Reactive 기반이므로 독립이 자연스럽지만, 일부 유틸리티(예: `MongoClientProvider`)를 공유할 수 있음
- [ ] `ReactiveMongoRepository` coroutine 확장 제공 범위: `ReactiveMongoOperations` 확장만 제공할지,
  `CoroutineCrudRepository` 기반 Repository 지원도 포함할지 -- Spring Data MongoDB는 이미 `kotlin-coroutines` 모듈에서
  `CoroutineSortingRepository` 등을 제공하므로 중복 여부 확인 필요
- [ ] `MongoTestConfiguration`에서 Testcontainers 초기화 방식: `bluetape4k-testcontainers` 모듈에
  `MongoDBServer.Launcher`가 이미 존재하는지, 아니면 새로 만들어야 하는지 -- 탐색 결과 `Libs.testcontainers_mongodb` 의존성은 존재하나 Launcher 패턴 확인 필요
- [ ] Auto-configuration 범위: 단순 마커 수준인지,
  `ReactiveMongoTemplate` 커스터마이징(codec registry, converter 등)도 포함할지 -- 초기 버전은 마커 수준으로 시작하고 필요시 확장하는 것이 안전함
- [ ] 
  `tail()` (Tailable Cursor) 확장함수 우선순위: Tailable Cursor는 capped collection 전용이므로 일반적이지 않음 -- 1차 구현에서 제외하고 2차에서 추가할 수 있음

## uuid-generator-refactoring - 2026-03-20

- [ ] `encodeBase62()` vs `Url62.encode()` 결과 동등성 검증 필요 -- 두 구현의 padding/sign 처리가 다를 수 있어, 기존
  `TimebasedUuid.Epoch.nextIdAsString()`으로 생성된 문자열을 `Url62.decode()`로 복원할 수 없을 가능성 있음. 마이그레이션 가이드에 포함해야 함
- [ ] `Uuid.V5` object의 비결정론적 동작 유지 여부 -- 현재 `NamebasedUuidGenerator`가 random UUID를 name으로 사용하는 이상한 구조인데,
  `Uuid.V5`에서 이를 그대로 유지할지 아니면 올바른 name-based(결정론적)로만 제공할지 -- 호환성 vs 의미적 정확성 트레이드오프
- [ ] 외부 모듈 마이그레이션 시점 -- `TimebasedUuid.Epoch` 사용처가 10개 이상 모듈에 분포. deprecation WARNING 단계에서 충분한 기간 후 ERROR로 전환할 시점 결정 필요
- [ ] JUG(Java UUID Generator) 라이브러리의 UUID v3(MD5 name-based) 지원 여부 -- 현재
  `Uuid.V5`만 제공하는데 V3도 추가할지. 사용 빈도가 낮아 초기 스코프에서는 제외했으나 완전성 측면에서 검토 필요
