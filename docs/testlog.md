# Test Execution Log

코드 수정 후 테스트 수행 결과를 기록합니다.

## 작성 가이드

- 새 행은 표 **맨 위**에 추가한다 (최신이 상단).
- 컬럼: `날짜 | 작업 | 대상 | 테스트 항목 | 결과 | 소요 | 비고`
- 결과: ✅ 성공, ❌ 실패, ⚠️ 일부 실패
- 편집 시 표 **첫 5행**만 읽어 중복 확인 후 추가한다.

---

| 날짜         | 작업                                     | 대상                          | 테스트 항목                           | 결과 | 소요    | 비고                                                       |
|------------|----------------------------------------|-----------------------------|----------------------------------|----|-------|----------------------------------------------------------|
| 2026-04-04 | KDoc 예제 추가 파일럿 최종 완료 (core+io 100%) | `bluetape4k-core` (157개), `bluetape4k-io` (47개) | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | core 157개·io 47개 전 파일 ```kotlin 태그 포함 예제 추가 완료; 16개 파일 bare ```→```kotlin 정규화(109건); 전체 ide_diagnostics 0 errors |
| 2026-04-04 | KDoc 정규화 + 예제 추가 (core 20개) | `ApacheConstructorUtils.kt`, `ApacheEnumUtils.kt`, `ApacheExceptionUtils.kt`, `DecoratorSupport.kt` 외 16개 | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | ide_diagnostics 4개 파일 0 errors; bare ``` → ```kotlin 정규화 없음(이미 kotlin 태그); Apache 래퍼 4개 파일 예제 누락 API에 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (collections 5개) | `IterableSupport.kt`, `JavaStreamSupport.kt`, `ListSupport.kt`, `MapEntrySupport.kt`, `QueueSupport.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | ide_diagnostics 5개 파일 모두 0 errors; IterableSupport(emptyIterator·emptyListIterator·asIterable·toList·toMutableList·size·exists·isSameElements·asCharArray·asByteArray·asIntArray·asLongArray·asFloatArray·asDoubleArray·asStringArray·asArray)·JavaStreamSupport(Stream.asIterable·toSet·Iterator/Iterable/Sequence.asStream×3·asParallelStream×3·IntStream/LongStream/DoubleStream asSequence/asIterable/toList/toXxxArray×3·Sequence/Iterable/XxxArray.toXxxStream×9·FloatArray.toDoubleStream)·ListSupport(```kotlin 태그 누락 수정)·MapEntrySupport/QueueSupport(기존 예제 그대로) 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제/@see 추가 (유틸/기타/예외 + Apache 래퍼) | `XXHasher.kt`, `BigIntegerPair.kt`, `Systemx.kt`, `DefaultFields.kt`, `SortDirection.kt`, `ValueObject.kt`, `AutoCloseableSupport.kt`, `ClassLoaderSupport.kt`, `JavaTypeSupport.kt`, `BluetapeException.kt`, `NotSupportedException.kt`, `ApacheConstructorUtils.kt`, `ApacheEnumUtils.kt`, `ApacheExceptionUtils.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | ide_diagnostics 14개 파일 모두 0 errors; XXHasher·BigIntegerPair·Systemx·DefaultFields·SortDirection·ValueObject·AutoCloseableSupport·ClassLoaderSupport·JavaTypeSupport·BluetapeException·NotSupportedException·ApacheConstructorUtils·ApacheEnumUtils·ApacheExceptionUtils 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (동시성 4개 + io 모듈 3개) | `NamedThreadFactory.kt`, `ThreadSupport.kt`, `VirtualThreadDispatcher.kt`, `VirtualThreadReactorScheduler.kt`, `StreamingCompressors.kt`, `BinarySerializationException.kt`, `KryoProvider.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | ide_diagnostics 7개 파일 모두 0 errors |
| 2026-04-04 | KDoc 예제 추가 (IteratorSupport·VarargSupport·DateIterator·TemporalIterator·YearQuarter·ZonedDateTimeSupport) | 6개 파일 (`bluetape4k-core`) | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | ide_diagnostics 6개 파일 모두 0 errors |
| 2026-04-04 | feat: bluetape4k-rule-engine 모듈 신규 구현 | `bluetape4k-rule-engine` | Facts(6)+DefaultRule(5)+DefaultRuleEngine(10)+DefaultSuspendRuleEngine(6)+InferenceRuleEngine(4)+RuleDsl(6)+RuleProxy(8)+ActivationRuleGroup(2)+ConditionalRuleGroup(2)+UnitRuleGroup(2)+MvelRule(5)+SpelRule(5)+KotlinScriptRule(5/disabled)+RuleReader(3)+DiscountExample(4)+AnnotationExample(4) = 72 passing, 5 skipped | ✅ | 763ms | DSL/어노테이션/코루틴 Rule Engine + MVEL2/SpEL/KotlinScript 엔진 + YAML/JSON/HOCON Reader + CompositeRule + InferenceRuleEngine |
| 2026-04-04 | KDoc 예제 추가 (Permutation + PermutationSupport) | `Permutation.kt`, `PermutationSupport.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | ide_diagnostics 0 errors |
| 2026-04-04 | KDoc 예제 추가 (PermutationStream + PermutationStreamSupport) | `PermutationStream.kt`, `PermutationStreamSupport.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | — | 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | feat: bluetape4k-states 모듈 신규 구현 | `bluetape4k-states` | DefaultStateMachine(8)+DSL(5)+Guard(4)+SuspendStateMachine(6)+Turnstile(3)+Order(4)+Appointment(5) = 35 tests | ✅ | 619ms | 동기 FSM(AtomicReference CAS)+코루틴 FSM(Mutex+StateFlow)+Guard 조건+DSL 빌더+3개 예제 테스트 |
| 2026-04-04 | KDoc 예제 파일럿 Phase 2 빌드 검증 | `bluetape4k-core`, `bluetape4k-io` | `./gradlew :bluetape4k-core:build :bluetape4k-io:build -x test` | ✅ | 393ms | P0(5개)+P1(5개)=10개 파일 KDoc 예제 추가 후 빌드 성공; 총 120개 kotlin 코드 블록 추가 |
| 2026-04-04 | KDoc 예제 추가 (T9: Wildcard) | `Wildcard.kt` | `WildcardTest` 16 tests | ✅ | 844ms | object Wildcard 전 public API에 kotlin 예제 추가; KDoc 내 `/*`·`*/`·`/**` 파싱 문제 회피 |
| 2026-04-04 | KDoc 예제 추가 (T8: KotlinDelegates) | `KotlinDelegates.kt` | `KotlinDelegatesTest` 7 tests | ✅ | 1.2s  | 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (T7: PaginatedList) | `PaginatedList.kt` | `PaginatedListTest` 6 tests | ✅ | 1s    | 전 public API에 kotlin 예제 추가; Wildcard.kt KDoc 컴파일 오류 함께 수정 |
| 2026-04-04 | KDoc 예제 추가 (T11: ZipFileSupport) | `ZipFileSupport.kt` | `ZipFileSupportTest` 9 tests | ✅ | 882ms | 전 public 함수에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (T10: Range) | `Range.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | —     | Range 인터페이스 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (T2: BoundedStack) | `BoundedStack.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | —     | 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (T5: BinarySerializers) | `BinarySerializers.kt` | KDoc 전용 편집 (코드 변경 없음) | ✅ | —     | 19개 프로퍼티에 kotlin 예제 추가 |
| 2026-04-04 | KDoc 예제 추가 (코드 변경 없음) | `RingBuffer.kt` | — (KDoc-only 변경) | ✅ | —     | 전 public API에 kotlin 예제 추가 |
| 2026-04-04 | BOMSupport KDoc·버그 수정·테스트 | `BOMSupportTest.kt` | getBOM/removeBom/withoutBom (19 tests) | ✅ | 1s    | utf_32le_array 버그(FE→FF FE), utf_8_array 오용 버그 2건 수정 |
| 2026-04-04 | 전체 모듈 README 최신화 (12 워커 병렬) | 전체 모듈 ~70개 | git 이력 기반 변경사항 반영 | ✅ | ~60m  | 실제 수정: io/feign README 1개 (feignBuilderOf builder 람다 반영) |
| 2026-04-04 | WireMockServer resetAll() stale 커넥션 수정 | `WireMockServerTest.kt`     | WireMockServerTest (5 tests) x3회 | ✅ | 2.7s  | stale 커넥션 재시도 로직 추가, 3회 반복 전부 통과                         |
| 2026-04-04 | ToxiproxyServer 개선                     | `ToxiproxyServerTest.kt`    | ToxiproxyServerTest (5 tests)    | ✅ | —     | exposeCustomPorts 추가, proxy+latency toxic 테스트 구현 (Codex) |
| 2026-04-04 | KDoc 프로퍼티 키 `.`→`-` 잔여 수정              | `RedisClusterServerTest.kt` | RedisClusterServerTest (3 tests) | ✅ | 14.1s | `redis-cluster` namespace KDoc 수정                        |
| 2026-04-04 | KDoc 프로퍼티 키 `.`→`-` 잔여 수정              | `MemgraphServerTest.kt`     | MemgraphServerTest (6 tests)     | ✅ | 14.1s | `bolt-port`, `log-port`, `bolt-url` KDoc 수정              |
| 2026-04-04 | ZooKeeper Curator 연결 안정화               | `ZooKeeperServerTest.kt`    | ZooKeeperServerTest (4 tests)    | ✅ | 8s    | IPv6→IPv4 폴백 안정화                                         |
| 2026-04-03 | property key `.`→`-` 마이그레이션            | `InfluxDBServerTest.kt`     | InfluxDBServerTest (2 tests)     | ✅ | 3.8s  | `admin-token` 프로퍼티 검증                                    |
| 2026-04-03 | property key `.`→`-` 마이그레이션            | `KeycloakServerTest.kt`     | KeycloakServerTest (4 tests)     | ✅ | 2.6s  | `auth-url`, `admin-username`, `admin-password` 검증        |
| 2026-04-03 | property key `.`→`-` 마이그레이션            | `Neo4jServerTest.kt`        | Neo4jServerTest (7 tests)        | ✅ | 3.9s  | `bolt-url` 프로퍼티 검증                                       |
| 2026-04-03 | property key `.`→`-` 마이그레이션            | `Neo4jServerTest.kt`        | compileTestKotlin                | ✅ | 9s    | 컴파일 검증                                                   |
