# Epic #1420 데이터 계약 검증 stacked train Implementation Plan

> 이 문서는 승인된 설계를 실행 가능한 작은 단계로 분해한 구현 계획이다. 각 단계는 독립적으로 검증하고, 다음 단계는 이전 단계의 exact head만을 base로 사용한다.

## Goal

Epic #1420의 남은 네 계약 결함을 하나의 큰 변경으로 묶지 않고 다음 순서의 독립적인 stacked PR train으로 해결한다.

1. #1359 — Cassandra write option의 nullable/zero/negative TTL 및 timestamp 처리
2. #1346 — Hibernate QueryDSL Kotlin codegen의 재현 가능한 호환성 검증과 문서화
3. #1357 — Hibernate-Lettuce near-cache root disable 경계 보강
4. #1358 — Spring Boot 4.1 MongoDB auto-configuration 경계 및 property namespace 정렬

성공 조건은 각 PR이 해당 issue 하나만 닫고, 이전 PR의 exact head를 base로 하며, 누적 train의 전체 테스트·정적 분석·문서·리뷰 증거가 남는 것이다. 병합은 이 계획의 실행과 별도의 fresh approval gate다.

## Architecture and constraints

- integration branch: develop
- planning branch: feat/epic-1420-data-contract-train
- implementation branches:
  - fix/1359-cassandra-write-options-nullable
  - build/1346-querydsl-kotlin-codegen
  - fix/1357-hibernate-lettuce-root-condition
  - fix/1358-mongodb-boot41-boundary
- PR train base chain:
  - PR-1359 base develop
  - PR-1346 base fix/1359-cassandra-write-options-nullable exact head
  - PR-1357 base build/1346-querydsl-kotlin-codegen exact head
  - PR-1358 base fix/1357-hibernate-lettuce-root-condition exact head
- Main develop worktree의 기존 untracked flow evidence는 보존하고, 일반 구현은 linked worktree에서 수행한다.
- Kotlin 변경은 Kotlin/JVM 25, Kotlin 2.4, Gradle wrapper 9.7.0 및 저장소의 기존 assertion/test 패턴을 따른다.
- Testcontainers 기반 검증은 모듈 간 동시 실행하지 않는다.
- 새 dependency, broad Epic PR, 자동 병합, integration branch 직접 push는 하지 않는다.

## Task 0 — 첫 구현 worktree와 baseline 고정

소유 범위: worktree/branch 준비와 baseline evidence만 담당한다.

1. Parent planning worktree의 clean 상태와 HEAD를 기록한다.
2. child를 만들기 직전에 `git fetch origin develop`으로 live integration head를 갱신한다. `git rev-parse origin/develop`와 `git log --oneline --decorate -n 1 origin/develop`를 receipt에 남긴다.
3. planning branch가 `origin/develop`보다 뒤처져 있으면 uncommitted 변경과 기존 commit을 확인한 뒤 `git rebase origin/develop`으로 갱신한다. 충돌이 발생하면 implementation을 시작하지 않고 충돌 파일과 선택지를 기록해 STOP한다. rebase 후 spec/plan commit의 의도 보존을 `git range-diff <old-base>..<old-head> <new-base>..<new-head>`로 확인하고, 문서에 적힌 base SHA를 새 live SHA로 갱신한 뒤 `git merge-base --is-ancestor origin/develop HEAD`와 `git merge-base HEAD origin/develop`를 검증한다.
4. spec/plan 문서의 base commit, branch chain, 시작 HEAD를 동일한 receipt로 갱신하고 계획 리뷰를 다시 실행한다. 계획이 가리키는 SHA와 실제 `git rev-parse`가 다르면 child 생성으로 진행하지 않는다.
5. 갱신된 parent exact head에서 linked worktree를 만들고 semantic branch를 생성한다.
   - path: .worktrees/fix/1359-cassandra-write-options-nullable
   - branch: fix/1359-cassandra-write-options-nullable
6. Main worktree의 기존 untracked flow files와 다른 linked worktrees를 변경하지 않았음을 확인한다.
7. 첫 slice 대상 모듈의 baseline을 다음 순서로 실행한다.
   - ./gradlew :bluetape4k-spring-boot-cassandra:test --tests 'io.bluetape4k.spring.cassandra.cql.OptionsSupportTest' --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-spring-boot-cassandra:detekt --no-configuration-cache --console=plain
8. baseline 결과와 갱신된 시작 HEAD를 parent plan/lesson evidence에 기록한다. 실패하면 구현 전에 실패 원인을 분류한다.

완료 증거: child worktree가 parent exact head에서 생성되고 baseline test/detekt 결과를 읽어 확인했다.

## Task 1 — #1359 failing tests 먼저 추가

소유 파일: spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupportTest.kt

1. 현재 positive TTL assertion이 Duration.ZERO를 허용하는 이유를 확인하고, null/zero/negative/positive matrix를 명시하는 테스트를 추가한다.
   - Duration.ofMillis(1)과 Duration.ofMillis(500)은 초 단위 TTL 0으로 기존 statement semantics를 유지하는지 확인한다.
   - Duration.ofSeconds(Int.MAX_VALUE.toLong() + 1) 및 음수 입력은 조용히 overflow/무시되지 않고 명시적 예외로 끝나는지 확인한다.
   - 음수 builder 예외 메시지는 `TTL must be greater than equal to zero`로 고정하고, 초 범위 초과는 `ArithmeticException`의 overflow로 고정한다.
2. Insert, Update, UpdateStart, Delete 각각에 대해 다음 계약을 테스트한다.
   - ttl이 null이면 TTL clause를 생성하지 않는다.
   - ttl이 zero이면 기존 semantics대로 USING TTL 0을 생성한다.
   - negative ttl은 Spring Data Cassandra builder의 명시적 IllegalArgumentException으로 거부된다.
   - positive ttl은 seconds clause를 생성하고 Int 범위 초과는 ArithmeticException으로 거부된다.
   - timestamp가 있으면 insert/update/updateStart에 보존된다.
   - delete에는 TTL clause가 추가되지 않지만 timestamp clause는 보존된다.
3. 기존 fixture와 assertion helper를 재사용하고 JUnit/새 dependency를 추가하지 않는다.
4. 구현을 변경하지 않은 상태로 targeted test를 실행해 새 테스트가 실패하는 RED 증거를 확보한다.
   - ./gradlew :bluetape4k-spring-boot-cassandra:test --tests 'io.bluetape4k.spring.cassandra.cql.OptionsSupportTest' --no-configuration-cache --console=plain
5. 실패가 계약 검증이 아닌 fixture 문제이면 테스트만 바로잡고 다시 RED를 확인한다.

완료 증거: 새 matrix가 실제 기존 구현의 nullable/zero/negative 경계를 실패로 포착한다.

## Task 2 — #1359 안전한 nullable write option 구현

소유 파일:
- spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt
- spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupportTest.kt
- spring-boot/cassandra/README.md
- spring-boot/cassandra/README.ko.md
- OptionsSupport.kt 안에서 변경된 TTL/timestamp 계약을 설명하는 KDoc만

1. Insert/Update/UpdateStart의 TTL 적용을 nullable-safe 흐름으로 바꾼다. 공통 규칙은 다음과 같다.
   - ttl?.takeUnless { it.isNegative }?.let { ttl -> ... }
   - seconds 변환은 Math.toIntExact(ttl.seconds)로 수행하여 Int 범위를 벗어나면 명시적으로 실패한다.
   - timestamp는 nullable-safe하게 보존한다.
   - Delete는 write option의 TTL을 사용하지 않지만 timestamp는 nullable-safe하게 보존한다.
2. isPositiveTtl은 다음 의미를 정확히 갖게 한다.
   - null과 negative는 false
   - zero와 positive는 true
   - 구현은 Duration에 존재하는 isNegative만 사용한다.
3. 테스트가 GREEN이 되도록 method별 return type/cast를 기존 DSL 구조에 맞춰 적용한다.
4. targeted test를 재실행한다.
5. detekt와 diff 검사를 실행한다.
   - ./gradlew :bluetape4k-spring-boot-cassandra:detekt --no-configuration-cache --console=plain
   - git diff --check
6. rendered statement를 확인해 null TTL에서는 usingTtl이 호출되지 않고 zero/positive TTL에서는 정확히 한 번만 호출되며, negative/overflow 입력은 명시적 예외로 write가 진행되지 않고 timestamp는 Delete를 포함한 applicable subtype에 한 번만 적용되는지 검증한다. 이 순수 builder 경로에는 네트워크 I/O가 없고 JMH 대상이 없으므로 별도 benchmark는 N/A로 기록한다.
7. KDoc와 Cassandra EN/KO README를 같은 계약으로 갱신한다. timestamp는 Cassandra microseconds 단위임을 명시하고 `TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis())` 예시를 사용한다. TTL은 subsecond duration이 whole seconds로 절삭되어 1ms/500ms가 `USING TTL 0`이 되고, zero는 유효하게 유지되며, negative builder는 `IllegalArgumentException("TTL must be greater than equal to zero")`, Int 초 범위 초과는 `ArithmeticException`으로 끝난다는 caller-facing 표를 양쪽 문서에 추가한다. `isPositiveTtl` 이름은 유지하되 zero 포함의 historical 의미를 명시한다.
8. 변경이 의도한 production/test/docs 파일과 OptionsSupport.kt의 영향을 받은 KDoc에 한정되는지 확인한다.
9. Lore commit으로 기록한다.
   - intent: Cassandra write option 경계에서 invalid TTL을 제거하고 timestamp를 보존한다
   - Constraint: Cassandra DSL이 nullable Duration과 subtype별 return type을 사용한다
   - Rejected: 공통 추상화 추가 | 기존 method별 DSL 흐름보다 범위가 커진다
   - Confidence: high
   - Scope-risk: narrow
   - Directive: 후속 write option 변경도 null/zero/negative matrix를 유지한다
   - Tested: OptionsSupportTest, Cassandra detekt, git diff --check
   - Not-tested: hosted CI와 container 외부 서비스
10. commit 후 child branch exact head를 기록한다. 다음 단계는 그 SHA만을 base로 삼는다.

완료 증거: #1359 테스트 GREEN, detekt PASS, 좁은 diff, Lore commit SHA.

## Task 3 — #1346 QueryDSL codegen compatibility 평가

소유 파일:
- data/hibernate/build.gradle.kts
- data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/codegen/QuerydslCodegenCompatibilityTest.kt
- data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/ExampleEntity.kt 및 data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping/associations/join/models.kt의 stale KDoc/FIXME
- data/hibernate/README.md
- data/hibernate/README.ko.md

1. #1359 exact head에서 build/1346-querydsl-kotlin-codegen worktree/branch를 만든다.
2. clean generated source 상태에서 현재 Java APT 경로와 runtime QueryDSL 경로를 확인한다.
   - 실험 전 `find data/hibernate/build/generated/source/kapt -type f | wc -l`와 `du -sk data/hibernate/build/generated/source/kapt`를 기록한다.
3. QuerydslCodegenCompatibilityTest를 작성한다.
   - AbstractHibernateTest 기반
   - JoinUser/AddressEntity fixture와 QJoinUser/QAddressEntity 사용
   - JPAQuery로 실제 generated Q type path를 조회
   - generated source 존재와 compile/runtime query 결과를 assertion
   - repository path resolution을 통과하는 entity/property 조합을 포함
4. 기존 test를 먼저 실행해 평가 baseline을 확보한다.
   - ./gradlew :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.querydsl.simple.SimpleQuerydslExamples' --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.querydsl.codegen.QuerydslCodegenCompatibilityTest' --no-configuration-cache --console=plain
5. build.gradle.kts의 querydsl.kotlin.codegen kapt 설정을 별도 실험 commit 없이 working tree에서 정확히 활성화하고 module 전체 clean compile/test를 실행한다.
   - ./gradlew :bluetape4k-hibernate:clean :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.querydsl.codegen.QuerydslCodegenCompatibilityTest' --no-configuration-cache --console=plain --profile
   - clean 직후 generated source가 제거되었는지 확인하고, baseline과 후보 각각의 generated source 파일 수·바이트와 KAPT/compile wall time을 기록한다.
6. 실험 결과가 clean compile과 repository path/runtime query를 모두 통과하는 경우에만 설정을 유지한다.
7. 실패하는 경우 설정을 복원하고, build.gradle.kts의 비활성화 주석에 재현된 실패 형태와 upstream QueryDSL issue #3454를 명시한다. Java APT workaround와 검증 범위를 문서에 적는다.
8. 현재 FIXME/KDoc는 실험 결과와 무관하게 현재 fixture의 실제 상속·association·equals 제약과 지원 범위만 남기도록 갱신한다. `LongJpaTreeEntity` fixture를 `AbstractJpaTreeEntity` 직접 상속으로 잘못 설명하지 않으며, 일반 class/association의 원인을 실험 없이 단정하지 않는다.
9. README.md와 README.ko.md에 동일한 지원 matrix와 대체 경로를 추가하고 예제 compile을 검증한다.
   - Java APT generated Q types: supported and tested
   - Kotlin codegen: enabled only when clean matrix passes; otherwise intentionally disabled with issue reference
   - repository path resolution/runtime query: tested path
   - DTO, 일반 Entity, tree Entity, association/join별 supported/unsupported 결과
   - 실패 시 사용할 Java APT 설정의 정확한 Gradle snippet, generated source 위치, repository path 사용 예제
10. targeted test, compile, detekt, diff 검사를 실행한다.
    - ./gradlew :bluetape4k-hibernate:test --tests 'io.bluetape4k.hibernate.querydsl.codegen.QuerydslCodegenCompatibilityTest' --no-configuration-cache --console=plain
    - ./gradlew :bluetape4k-hibernate:detekt --no-configuration-cache --console=plain
    - git diff --check
11. codegen 후보가 generated source 수 또는 compile time을 유의미하게 늘리면 비활성화하고 측정값을 README와 lesson에 남긴다. 이 모듈에는 JMH 대상이 없으므로 별도 benchmark는 N/A로 기록한다.
12. Lore commit으로 기록하고 exact head를 저장한다.
    - intent: QueryDSL codegen 지원 범위를 재현 가능한 Java APT 계약으로 고정한다
    - Constraint: Spring Data JPA path resolution과 kapt codegen의 upstream 호환성
    - Rejected: 실패한 Kotlin codegen을 강제로 활성화 | clean compile/runtime 근거가 없다
    - Confidence: high
    - Scope-risk: moderate
    - Directive: codegen 설정 변경은 generated source와 repository path matrix를 함께 갱신한다
    - Tested: compatibility test, Hibernate test, detekt, diff check
    - Not-tested: hosted matrix와 외부 database provider 조합

완료 증거: codegen 설정의 pass/fail이 clean evidence로 고정되고 EN/KO 문서가 일치하며 PR-1346 commit SHA가 존재한다.

## Task 4 — #1357 root disable 경계 구현

소유 파일:
- spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt
- spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt
- spring-boot/hibernate-lettuce/README.md
- spring-boot/hibernate-lettuce/README.ko.md

1. #1346 exact head에서 fix/1357-hibernate-lettuce-root-condition worktree/branch를 만든다.
2. Hibernate, Metrics, Actuator 세 auto-configuration을 함께 올리는 context runner를 추가한다.
3. root/metrics 2×2와 기본값을 명시적으로 검증한다.
   - root=false + metrics=false/true 모두 MetricsBinder와 actuator endpoint가 생성되지 않는다.
   - root=true + metrics=false는 둘 다 생성되지 않고, root=true + metrics=true만 MetricsBinder가 생성된다.
   - actuator endpoint는 root=true이고 metrics/actuator 조건 및 `management.endpoints.web.exposure.include=nearcache`가 모두 충족될 때만 노출된다.
   - root=false에서 exposure 설정만으로 우회되지 않는지 확인한다.
4. 새 테스트를 먼저 실행해 현재 Metrics auto-configuration이 root 조건을 무시하는 RED를 확보한다.
   - ./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --tests 'io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheAutoConfigurationTest' --no-configuration-cache --console=plain
5. Metrics auto-configuration에 Hibernate/Actuator와 동일한 root condition을 추가한다. 새 meta-annotation이나 조건 순서 변경은 추가하지 않는다.
6. 전체 해당 test와 detekt/diff를 실행한다.
   - ./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --tests 'io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheAutoConfigurationTest' --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-spring-boot-hibernate-lettuce:detekt --no-configuration-cache --console=plain
   - git diff --check
7. Metrics KDoc, EN/KO README의 조건표와 메트릭 설명을 `root enabled && metrics enabled`로 동일하게 보강한다. optional dependency는 실제 `spring-boot-starter-actuator`와 조건명을 사용해 설명한다. root false에서 `metrics.enabled=true`여도 customizer, MetricsBinder, endpoint가 다시 켜지지 않는다는 점을 명시한다.
8. Lore commit으로 기록하고 exact head를 저장한다.
   - intent: Hibernate-Lettuce root disable이 metrics와 actuator 경계에도 일관되게 적용되게 한다
   - Constraint: 기존 auto-configuration property contract와 context runner 구조
   - Rejected: metrics 전용 조건만 조정 | root disabled 우회를 막지 못한다
   - Confidence: high
   - Scope-risk: narrow
   - Directive: 새 near-cache 기능은 root disable negative test를 포함한다
   - Tested: auto-configuration test, detekt, diff check
   - Not-tested: hosted CI와 실제 Redis metrics backend

완료 증거: root/metrics 2×2와 기본값/optional-class tests GREEN, detekt PASS, EN/KO docs parity, PR-1357 SHA.

## Task 5 — #1358 Spring Boot 4.1 Mongo boundary 구현

소유 파일:
- spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt
- spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/ReactiveMongoAutoConfigurationTest.kt
- spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoTest.kt
- spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt
- spring-boot/mongodb/src/test/resources/application.yml (bean overriding 비허용 설정)
- spring-boot/mongodb/README.md
- spring-boot/mongodb/README.ko.md
- CHANGELOG.md (Unreleased의 호환성/마이그레이션 항목)

1. #1357 exact head에서 fix/1358-mongodb-boot41-boundary worktree/branch를 만든다.
2. resolved dependency를 먼저 확인한다.
   - ./gradlew :bluetape4k-spring-boot-mongodb:dependencyInsight --dependency spring-boot-mongodb --configuration testRuntimeClasspath --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-spring-boot-mongodb:dependencyInsight --dependency spring-boot-data-mongodb --configuration testRuntimeClasspath --no-configuration-cache --console=plain
3. resolved Spring Boot source/API에서 다음 사실을 확인한다.
   - MongoProperties binding prefix는 spring.mongodb
   - DataMongoReactiveAutoConfiguration은 MongoReactiveAutoConfiguration 이후에 동작한다.
   - DataMongoReactiveAutoConfiguration의 ReactiveMongoTemplate이 custom operations bean과 충돌하지 않아야 한다.
4. ReactiveMongoAutoConfigurationTest를 추가한다. Testcontainers 없이 ApplicationContextRunner와 mock bean으로 검증한다.
   - `FilteredClassLoader(ReactiveMongoOperations::class)`를 사용해 ReactiveMongoOperations class가 없으면 `@ConditionalOnClass`로 auto-configuration이 비활성화되는지 검증한다.
   - spring.mongodb.uri가 MongoProperties에 bind된다.
   - legacy-only spring.data.mongodb.uri는 조용히 localhost로 fallback하지 않고 `IllegalStateException("Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+")`로 fail-fast한다. spring.mongodb.uri가 함께 있으면 새 key가 우선한다.
   - user-provided ReactiveMongoOperations bean이 fallback ReactiveMongoTemplate보다 우선한다.
   - no user bean일 때 fallback template이 생성된다.
   - Boot Data Mongo reactive auto-configuration과의 order가 보존된다.
   - operations/template/factory/client가 중복 생성되지 않고 각 context에서 단일 instance만 존재한다.
   - context-only 검증은 MongoDB 네트워크 I/O 없이 완료된다.
   - property/fallback context와 Boot order context를 분리하고, 후자에는 필요한 Mongo client/factory/converter를 모두 mock으로 공급해 driver thread·network side effect를 막는다.
   - 각 runner의 context close 뒤 client/template lifecycle과 startup failure 부재를 assertion한다.
   - ReactiveMongoDatabaseFactory 또는 MongoConverter가 없으면 fallback configuration이 fail-fast하고 원인이 context failure에 남는다.
   - synthetic URI만 사용하고 credential-bearing URI와 MongoProperties.toString()을 로그·lesson·PR evidence에 남기지 않는다. test resource의 bean overriding은 끄고, user ReactiveMongoOperations가 fallback/Boot 기본 template과 충돌 없이 우선하는지 검증한다.
5. 현재 구현/헬퍼를 먼저 실행해 새 property namespace/order 테스트의 RED 또는 contract gap을 확보한다.
   - ./gradlew :bluetape4k-spring-boot-mongodb:test --tests 'io.bluetape4k.spring.mongodb.ReactiveMongoAutoConfigurationTest' --no-configuration-cache --console=plain
6. production auto-configuration을 Boot 4.1 boundary에 맞춘다.
   - @AutoConfiguration(afterName=["org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration"])를 사용한다.
   - fallback bean의 conditional behavior를 유지
   - legacy namespace가 현재 namespace 없이 들어오면 configuration class 초기화 단계에서 위의 정확한 migration 예외를 내어 기본 localhost client로 진행되지 않게 한다.
   - 불필요한 manual client/configuration layer는 추가하지 않는다.
7. AbstractReactiveMongoTest의 dynamic property와 KDoc를 spring.mongodb.uri 기준으로 갱신한다.
8. MongoTestApplication에서 manual AbstractReactiveMongoConfiguration 상속, eager mongoServer companion, property masking을 제거한다. Testcontainers URL 주입과 singleton lifecycle owner는 AbstractReactiveMongoTest에만 남기고, integration test가 실제 auto-configuration을 사용하도록 한다.
   - context close는 Spring-managed client/template을 닫고, ShutdownQueue가 소유한 공유 MongoDBServer를 테스트별로 stop하지 않는다는 lifecycle 계약을 검증한다.
9. context/unit test를 먼저 실행한 뒤 MongoDB Testcontainers integration을 단독 실행한다.
   - ./gradlew :bluetape4k-spring-boot-mongodb:test --tests 'io.bluetape4k.spring.mongodb.ReactiveMongoAutoConfigurationTest' --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-spring-boot-mongodb:test --tests 'io.bluetape4k.spring.mongodb.coroutines.ReactiveMongoOperationsCoroutinesTest' --no-configuration-cache --console=plain
10. detekt와 diff 검사를 실행한다.
    - ./gradlew :bluetape4k-spring-boot-mongodb:detekt --no-configuration-cache --console=plain
    - git diff --check
11. context/unit와 container integration을 별도 실행하는 동안 container startup/teardown 횟수와 wall time을 기록하고, 최종 검증에서 반복 실행이 필요한 이유 또는 제거한 중복을 lesson에 남긴다.
12. EN/KO README의 property 예제와 auto-configuration 설명을 동일하게 고치고, `spring.data.mongodb.uri`에서 `spring.mongodb.uri`로의 before/after migration, 위의 exact exception, dual-key precedence와 rollback(마이그레이션 불가 시 이전 artifact 버전으로 pin)을 명시한다. Boot 4.1+를 지원 범위로 표기하고 현재 저장소의 Boot 4.x compatibility matrix를 compile/context test로 확인한다.
13. Lore commit으로 기록하고 exact head를 저장한다.
    - intent: Spring Boot 4.1 Mongo reactive auto-configuration의 namespace와 order 경계를 맞춘다
    - Constraint: Boot 4.1 MongoProperties/DataMongoReactiveAutoConfiguration source contract
    - Rejected: legacy spring.data.mongodb namespace 유지 | Boot 4.1 binding contract와 어긋난다
    - Confidence: high
    - Scope-risk: moderate
    - Directive: Mongo test helper는 production auto-configuration을 가리지 않도록 유지한다
    - Tested: context test, isolated Testcontainers integration, detekt, diff check
    - Not-tested: hosted matrix와 외부 Mongo provider

완료 증거: context tests와 isolated container integration GREEN, property/order evidence, EN/KO docs parity, PR-1358 SHA.

## Task 6 — 누적 train 검증과 lesson

소유 범위: 마지막 child branch에서 누적 검증, lesson, final evidence를 담당한다.

1. 각 child branch가 앞선 exact head를 포함하는지 git log와 git range-diff로 확인한다.
2. Testcontainers 모듈을 직렬로 검증한다.
3. 누적 영향을 확인하는 targeted/full tests를 순서대로 실행한다.
   - ./gradlew :bluetape4k-spring-boot-cassandra:test --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-hibernate:test --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --no-configuration-cache --console=plain
   - ./gradlew :bluetape4k-spring-boot-mongodb:test --no-configuration-cache --console=plain
4. 각 affected module detekt를 다시 실행한다.
5. 모든 변경 파일에 git diff --check를 실행한다.
6. Testcontainers 명령은 백그라운드로 돌리지 않고 최초 실패의 Gradle 로그, exit code, container ID와 Docker 상태를 보존한다. Docker/cleanup 상태를 확인한 뒤에만 최대 1회 재시도하며, 취소·중단 시 Gradle 프로세스와 orphan container가 없는지 확인한다.
7. docs/lessons/2026-08-26-epic-1420-data-contract-train.md를 작성한다.
   - 각 issue의 root cause와 exact test evidence
   - QueryDSL upstream issue #3454 확인 결과
   - Spring Boot 4.1 source/API namespace/order evidence
   - Testcontainers 실행 순서와 실패/재시도 여부
   - train restack 및 rollback 절차
   - 남은 hosted CI/Nightly gate
8. CHANGELOG.md의 Unreleased에 공개 계약 변경을 기록한다. Cassandra TTL/timestamp 경계, QueryDSL 지원 matrix, hibernate-lettuce root/metrics 조건, Mongo `spring.mongodb.uri` migration 및 legacy-only fail-fast를 각각 호환성/버그 수정/마이그레이션 항목으로 적고, 구현 결과가 release-note 대상이 아니면 그 근거를 lesson에 남긴다.
9. EN/KO README parity를 자동화된 최소 검사로 확인한다. 두 파일 모두에 각 계약의 핵심 토큰(`spring.mongodb.uri`, `spring.data.mongodb.uri`, `root`, `metrics.enabled`, `USING TTL 0`, `ArithmeticException`, `querydsl-kotlin-codegen`)과 동일한 설정 예제/표 행이 존재하는지 `for token in ...; do rg -q "$token" <en> && rg -q "$token" <ko> || exit 1; done`으로 검사하고, code block/표의 의미 차이는 수동 diff receipt로 남긴다.
10. lesson을 Korean reader-facing prose로 작성하고 Lore commit으로 기록한다.

완료 증거: 누적 train의 module tests/detekt/diff PASS, lesson commit SHA, no known local errors.

## Task 7 — 독립 review, PR 생성, hosted gate

1. Type-A required review perspectives를 독립적으로 수행한다.
   - Performance: hot path, allocation, round trip, benchmark/stress acceptance
   - Stability: lifecycle, failure path, retry/cancellation, Testcontainers stability
   - Security: trust boundary, secret/config safety, injection/deserialization, negative defaults
   - Operator/Ops: diagnostics, rollout/rollback, ownership, release impact
   - Developer/API: Kotlin/API compatibility, module boundaries, implementable ordering
   - User/caller: misuse resistance, examples, unsupported behavior, EN/KO parity
2. 각 PR마다 reviewer가 read-only로 P0/P1/P2를 분류하고, P0/P1은 수정 후 재검증한다. 동일 finding은 duplicate-check한다.
3. integration review에서 누적 diff, train ordering, exact bases, issue linkage, docs/lesson, rollback을 확인한다.
4. pre-PR checklist를 만족한 뒤 각 issue의 live GitHub metadata를 재확인한다.
   - issue open state, milestone, labels, assignee debop
   - predecessor/successor linkage
   - target base/head exact SHA
5. PR body는 Korean으로 작성하고 다음을 포함한다.
   - Fixes #issue
   - train 위치와 predecessor exact head
   - 변경 범위와 non-scope
   - targeted/full test 및 detekt 결과
   - docs/lesson evidence
   - rollback/restack notes
   - 마지막 heading은 ## DoD Status
6. 각 PR의 push 전에 local/remote ref를 보호한다.
   - `local_sha=$(git rev-parse HEAD)`와 `remote_sha=$(git ls-remote --heads origin refs/heads/<branch> | awk '{print $1}')`를 저장한다.
   - remote ref가 비어 있을 때만 `git push -u origin <branch>`를 실행한다. remote ref가 있고 local SHA와 다르면 overwrite하지 않고 STOP한다.
   - push 후 `gh api repos/bluetape4k/bluetape4k-projects/git/ref/heads/<branch> --jq .object.sha`가 `local_sha`와 같은지 확인한다.
   - predecessor merge 후 restack이 필요한 경우 expected-old remote SHA를 별도 receipt로 고정하고, `git range-diff`를 먼저 남긴 뒤 정확한 lease를 사용한 `git push --force-with-lease=refs/heads/<branch>:<expected-old-sha>`만 허용한다. 예기치 않은 remote 이동이면 STOP한다.
7. PR을 순서대로 만든다.
   - `gh pr create --repo bluetape4k/bluetape4k-projects --base develop --head fix/1359-cassandra-write-options-nullable --title "[kotlin] Cassandra WriteOptions nullable 계약 수정" --body-file .omx/pr-bodies/epic-1420-1359.md`
   - `gh pr create --repo bluetape4k/bluetape4k-projects --base fix/1359-cassandra-write-options-nullable --head build/1346-querydsl-kotlin-codegen --title "[hibernate] QueryDSL codegen 호환성 계약 고정" --body-file .omx/pr-bodies/epic-1420-1346.md`
   - `gh pr create --repo bluetape4k/bluetape4k-projects --base build/1346-querydsl-kotlin-codegen --head fix/1357-hibernate-lettuce-root-condition --title "[hibernate-lettuce] root disable 조건 전파" --body-file .omx/pr-bodies/epic-1420-1357.md`
   - `gh pr create --repo bluetape4k/bluetape4k-projects --base fix/1357-hibernate-lettuce-root-condition --head fix/1358-mongodb-boot41-boundary --title "[mongodb] Spring Boot 4.1 boundary 정렬" --body-file .omx/pr-bodies/epic-1420-1358.md`
   - 각 body 파일은 PR 생성 전에 exact head, predecessor SHA, `Fixes #<issue>`, 테스트/lesson evidence와 `## DoD Status`를 채운다.
8. PR 생성 후 CI/checks/reviews/threads를 확인한다. predecessor가 merge되기 전에는 successor를 다른 base로 바꾸지 않는다.
9. hosted evidence matrix를 PR별로 작성한다. 현재 `.github/workflows/ci.yml`의 자동 PR 조건과 `spring-boot/**` path filter를 live 파일에서 재확인하고, feature-base successor PR의 자동 CI는 `N/A (base가 develop/main이 아님)`로 구분한다. 각 exact head에 대해 승인된 수동 dispatch를 실행한다.
   - `gh workflow run ci.yml --repo bluetape4k/bluetape4k-projects --ref <branch>`
   - Spring Boot/Testcontainers 범위는 `gh workflow run nightly-tests.yml --repo bluetape4k/bluetape4k-projects --ref <branch> -f scope=full`로 실행하고, 해당 run의 scope를 receipt에 기록한다.
   - dispatch 직전 각 branch의 `local_sha`와 UTC timestamp를 기록한다. `gh run list --workflow <workflow-file> --branch <branch> --event workflow_dispatch --json databaseId,headSha,createdAt,status,conclusion`에서 dispatch 이후 생성된 `headSha == local_sha` run ID를 고정하고, 동일 SHA run이 여러 개면 dispatch 이후 `createdAt`이면서 가장 큰 `databaseId`인 run을 authority로 선택한다. `gh run view <databaseId> --json headSha,status,conclusion,jobs,url`을 terminal receipt로 저장한다.
   - 고정한 run의 `headSha`가 로컬 기록 SHA와 같은지, terminal job conclusion과 skipped/path-filtered job을 구분해 확인한다. dispatch 권한이 없거나 exact-head run을 만들 수 없으면 hosted gate를 `PENDING`으로 두고 merge-ready로 진행하지 않는다.
   - develop protection을 live API로 읽어 `required_status_checks`와 required/non-required/N/A를 구분한다. 로컬 green, stale SHA, path-filtered skip은 hosted exact-head proof로 세지 않는다.
10. predecessor merge 후에만 successor를 rebase/restack하고, remote expected head를 확인한 뒤 필요한 경우 위의 명시적 `--force-with-lease`를 사용한다.
11. 각 PR에서 issue milestone/labels/assignee가 보존되는지 확인하고, broad Epic PR을 만들지 않는다.

완료 증거: 네 PR이 승인된 train 순서와 exact head/base, Korean body, live metadata, CI/review evidence를 갖는다. 단, 병합은 아직 하지 않는다.

## Task 8 — merge-ready report와 승인 대기

1. 각 PR에 대해 merge 전 체크를 fresh state에서 다시 읽는다.
   - exact head/base
   - required checks와 Nightly
   - review approvals와 unresolved threads
   - mergeability/conflicts
   - linked issue, labels, milestone, assignee
   - body의 test evidence와 ## DoD Status
2. local green, skipped/path-filtered, stale-SHA run을 hosted exact-head proof와 구분한다.
3. P0/P1=0, required checks 통과, unresolved thread=0, range-diff 의도 일치인지 확인한다.
   - Performance 렌즈의 P0–P3 결과와 codegen/container 비용 측정이 기록되었는지 확인한다.
4. 다음을 포함한 merge-ready report를 Korean으로 제출한다.
   - plan item status
   - PR별 exact SHA/base/check/review evidence
   - known gaps와 blocked checks
   - rollback/cleanup plan
   - final state: PENDING — fresh explicit merge approval required
5. 사용자가 별도로 merge를 승인하기 전에는 merge, auto-merge, branch deletion, worktree cleanup을 수행하지 않는다. merge 전에는 successor PR과 child branch를 freeze하고 exact head/base receipt를 보존한다.
6. fresh merge approval 이후에만 PR을 순서대로 merge하고, 각 merge SHA와 develop fast-forward를 검증한다. merge 후 회귀가 발생하면 후속 merge를 즉시 중단하고, 별도 승인을 받은 `git revert <merge-sha>` PR만 사용한다. revert 후 affected targeted/full test와 hosted exact-head gate를 다시 통과시키며 history rewrite는 하지 않는다.
7. merge 완료와 `develop` 통합 증거를 먼저 보고한 뒤 사용자가 cleanup 대상을 명시적으로 별도 승인한 경우에만 통합이 증명된 child를 cleanup한다. cleanup 직전에 `git fetch origin develop <child-branches>`로 remote refs를 갱신한다. 각 대상에 대해 `git merge-base --is-ancestor <child-sha> origin/develop`, clean status, `git log origin/<branch>..<branch>` 및 반대 방향의 unpushed/unmerged commit 0을 확인한다. squash merge이면 PR base와 child patch의 `git range-diff <predecessor-base>..<child-sha> <predecessor-base>..<merge-sha>` 또는 저장소 표준 patch-equivalence를 통과시킨다. rebase merge이면 동일 범위의 range-diff로 patch series가 통합됐음을 확인하고, merge commit 방식이면 ancestor와 PR diff equivalence를 확인한다. 이 receipt 뒤에만 `git worktree remove <exact-path>`를 `--force` 없이 실행하며, branch deletion은 별도 authority로 둔다. cleanup 승인이 없으면 worktree/branch를 보존하고 final state를 `PENDING(cleanup)`으로 둔다. main worktree의 untracked flow evidence와 unrelated worktrees는 보존한다.
8. Epic #1420 milestone이 stale하면 merge/closeout evidence를 확인한 뒤 별도 명시적 조정으로 처리한다.

## Stop conditions

- 계획 승인 전에는 implementation mutation을 시작하지 않는다.
- 이전 PR exact head가 없거나 remote state가 예상과 다르면 해당 slice를 멈추고 evidence를 수집한다.
- required CI, review, issue linkage, or mergeability가 unresolved이면 merge-ready가 아니다.
- destructive cleanup, branch deletion, merge, auto-merge는 fresh explicit approval 없이는 수행하지 않는다.
- 검증 실패는 숨기지 않고 원인·영향·다음 대안을 lesson과 최종 DoD에 남긴다.
