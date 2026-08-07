---
manualId: bluetape4k-spring-boot-r2dbc
title: "Spring Data R2DBC 코루틴 지원"
description: "Spring Data R2DBC의 entity operation을 Kotlin suspend 함수와 Flow로 사용하는 방법을 설명합니다."
kind: library
group: spring
learningOrder: 930
---

# Spring Data R2DBC 코루틴 지원

## 제공하는 기능 {#problem}

`bluetape4k-spring-boot-r2dbc`는 Spring Data R2DBC의 `R2dbcEntityOperations`와 `ReactiveInsertOperation`, `ReactiveUpdateOperation`, `ReactiveDeleteOperation`을 Kotlin coroutine API로 감쌉니다. 조회 결과가 여러 건이면 `Flow<T>`, 한 건이면 suspend 함수, 변경 작업이면 영향받은 행 수를 반환합니다.

이 모듈은 `ConnectionFactory`나 connection pool을 만들지 않으며 auto-configuration도 제공하지 않습니다. Spring Boot와 애플리케이션이 구성한 `R2dbcEntityOperations`를 받아 Reactor publisher를 `awaitSingle`, `awaitSingleOrNull`, `flow`로 변환하는 얇은 확장 계층입니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- Spring Data R2DBC의 entity mapping과 `Query`·`Criteria`를 그대로 사용할지 정합니다.
- 조회 결과가 정확히 한 건이어야 하는지, 첫 행만 필요할지, 여러 행을 `Flow`로 흘려보낼지 정합니다.
- insert와 update를 구분합니다. 이 모듈에는 entity 상태를 판별하는 `save`나 upsert가 없습니다.
- transaction은 service 또는 Spring configuration에서 소유합니다. 확장 함수 하나를 호출할 때마다 별도 transaction이 생기지 않습니다.
- driver, `ConnectionFactory`, pool 크기와 종료 책임은 애플리케이션에 둡니다.

SQL을 직접 제어하거나 connection·pool helper가 필요하면 먼저 [`bluetape4k-r2dbc`](./bluetape4k-r2dbc.md)를 확인합니다. table DSL과 repository abstraction이 필요하면 [생태계 학습 경로](./bluetape4k-spring-boot-r2dbc/ecosystem-paths.md)에서 Exposed R2DBC까지 비교합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 라이브러리 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다. 실제 database driver는 애플리케이션이 선택합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-r2dbc")

    runtimeOnly("org.postgresql:r2dbc-postgresql") // 사용하는 driver로 교체
}
```

이 artifact는 `bluetape4k-r2dbc`, `bluetape4k-spring-boot-core`, Spring Boot Data R2DBC starter를 API dependency로 제공합니다.

## 첫 repository {#quick-start}

Spring Boot가 제공하는 `R2dbcEntityOperations`를 repository에 주입하고 필요한 확장 함수만 사용합니다.

```kotlin
@Repository
class PostRepository(
    private val operations: R2dbcEntityOperations,
) {
    fun findAll(): Flow<Post> =
        operations.selectAllSuspending()

    suspend fun findById(id: Long): Post? =
        operations.findOneByIdOrNullSuspending(id)

    suspend fun insert(post: Post): Post =
        operations.insertSuspending(post)
}
```

`selectAllSuspending`이 반환한 `Flow`는 수집할 때 query를 실행합니다. controller가 `Flow<Post>`를 그대로 반환하거나 service에서 `toList()`로 수집할 수 있지만, blocking bridge로 바꾸면 R2DBC를 선택한 이점이 사라집니다.

## 작업별 API {#api-by-task}

| 필요한 작업 | API | 반환과 경계 |
| --- | --- | --- |
| ID로 정확히 한 건 조회 | `findOneByIdSuspending<T>` | 값이 없거나 여러 건이면 하위 Spring Data 예외가 전파됩니다. |
| ID로 nullable 단건 조회 | `findOneByIdOrNullSuspending<T>` | 0건은 `null`, 여러 건은 오류입니다. |
| 첫 행 조회 | `findFirstByIdSuspending<T>`, `findFirstByIdOrNullSuspending<T>` | 여러 건이어도 첫 행만 사용합니다. |
| 여러 행 조회 | `selectSuspending<T>`, `selectAllSuspending<T>` | cold `Flow<T>`를 반환합니다. |
| 단건·첫 행 조회 | `selectOne*`, `selectFirst*` | `one`과 `first`의 cardinality 계약이 다릅니다. |
| 존재·개수 확인 | `existsSuspending<T>`, `countSuspending<T>`, `countAllSuspending<T>` | `Boolean` 또는 `Long`을 반환합니다. |
| entity 삽입 | `insertSuspending<T>`, `insertOrNullSuspending<T>` | 저장된 entity를 반환하며 update나 upsert를 수행하지 않습니다. |
| 조건 update | `updateSuspending<T>` | 변경된 행 수를 반환합니다. |
| 조건·전체 delete | `deleteSuspending<T>`, `deleteAllSuspending<T>` | 삭제된 행 수를 반환합니다. |

## 학습 경로 {#concepts}

각 장은 API 목록에 그치지 않고 실제 repository를 만들 때 마주치는 cardinality, transaction, failure 경계를 1.12.1 소스와 테스트로 설명합니다. 예제를 따라간 뒤에는 같은 페이지의 source와 test 링크에서 구현을 바로 확인할 수 있습니다.

1. [Entity operation 시작하기](./bluetape4k-spring-boot-r2dbc/entity-operations.md) — receiver type과 ID 조회 helper, module이 소유하지 않는 구성 요소를 구분합니다.
2. [Flow와 조회 cardinality](./bluetape4k-spring-boot-r2dbc/flow-and-cardinality.md) — 여러 행, 정확히 한 행, 첫 행, nullable 결과의 차이를 익힙니다.
3. [Insert, update, delete](./bluetape4k-spring-boot-r2dbc/write-operations.md) — 저장된 entity와 영향받은 행 수를 해석하고 `save`와 혼동하지 않는 방법을 설명합니다.
4. [Query와 repository 구성](./bluetape4k-spring-boot-r2dbc/queries-and-repositories.md) — Spring Data `Query`·`Criteria`로 repository와 WebFlux endpoint를 구성합니다.
5. [Transaction, 실패, 테스트](./bluetape4k-spring-boot-r2dbc/transactions-and-testing.md) — 외부 transaction 경계, cancellation, H2 통합 테스트와 실패 검증을 연결합니다.
6. [R2DBC 생태계 학습 경로](./bluetape4k-spring-boot-r2dbc/ecosystem-paths.md) — core R2DBC, JDBC, Exposed R2DBC와 workshop 사이에서 다음 단계를 고릅니다.

처음 사용한다면 1→2→3→4→5 순서로 읽습니다. persistence 기술이나 abstraction 수준을 고르는 중이라면 6장을 먼저 읽고 필요한 장으로 돌아옵니다.

## 권장 패턴 {#patterns}

repository는 `R2dbcEntityOperations`를 주입받고 query 구성과 결과 cardinality만 결정합니다. business transaction은 여러 repository 호출을 묶을 수 있는 service 경계에 둡니다. `Flow`는 endpoint나 service 소비자까지 유지하고, 한 번만 실행해야 하는 write publisher는 suspend 함수로 끝냅니다.

단건 조회에서는 이름만 보고 `one`과 `first`를 바꾸지 않습니다. 중복 데이터가 오류여야 하면 `one`, 정렬된 결과의 선두만 필요하면 `first`를 선택합니다. update와 delete는 반환된 행 수를 확인해 낙관적 조건 불일치나 이미 삭제된 데이터를 구분합니다.

## 연동 {#integrations}

모듈은 다음 경계 위에서 동작합니다.

```text
WebFlux controller / coroutine service
                  ↓
bluetape4k Spring Data R2DBC coroutine extensions
                  ↓
R2dbcEntityOperations / Spring Data Query and mapping
                  ↓
ConnectionFactory / R2DBC driver / database
```

`bluetape4k-r2dbc`는 connection, raw SQL, mapping, transaction helper를 제공합니다. 이 모듈은 그 위에서 Spring Data entity operation을 coroutine 문법으로 사용하게 합니다. 두 모듈을 함께 쓸 수 있지만 같은 책임을 중복 구현하지 않습니다.

## 설정 {#configuration}

이 모듈에는 property class, auto-configuration class, `src/main/resources` 설정 파일이 없습니다. `spring.r2dbc.*`와 driver dependency, `ConnectionFactory`, pool은 Spring Boot 애플리케이션이 구성합니다. 직접 구성하는 애플리케이션은 `R2dbcEntityOperations` bean까지 제공해야 합니다.

1.12.1 테스트 애플리케이션은 `AbstractR2dbcConfiguration`에서 H2 `ConnectionFactory`를 직접 만들고 `ConnectionFactoryInitializer`로 schema를 적용합니다. 이 코드는 확장 모듈의 자동 구성 기능이 아니라 테스트 fixture입니다.

## 실패 동작 {#failures}

확장 함수는 Spring Data와 driver 예외를 다른 값으로 바꾸지 않습니다. `selectOneSuspending`은 0건 또는 여러 건을 정상 결과로 숨기지 않으며, `selectOneOrNullSuspending`도 여러 건이면 실패할 수 있습니다. `selectFirstOrNullSuspending`만 결과 부재를 `null`로 다루면서 여러 건 중 첫 행을 허용합니다.

insert mapping 오류, constraint 위반, connection 획득 실패도 그대로 전파됩니다. coroutine cancellation을 잡아서 성공이나 빈 결과로 바꾸지 않습니다. update와 delete의 `0L`은 예외가 아니라 조건에 맞는 행이 없었다는 결과이므로, 호출자가 기대 건수와 비교해야 합니다.

## 운영 {#operations}

이 모듈 자체에는 metric이나 health indicator가 없습니다. query 지연, connection pool pending acquire, timeout, transaction rollback과 WebFlux request latency는 Spring Boot, driver, pool 계층에서 관찰합니다. `Flow`를 여러 번 수집하면 query도 여러 번 실행될 수 있으므로 재수집 여부를 로그와 trace에서 구분합니다.

대량 조회는 `Flow`라는 이유만으로 메모리와 database 부하가 자동 제한되지 않습니다. `Query`에 정렬과 limit를 명시하고, consumer 처리량과 connection 보유 시간을 함께 확인합니다.

## 테스트 {#testing}

1.12.1 대표 테스트는 H2 in-memory database와 실제 Spring Boot context를 사용해 조회, insert, update, delete, WebFlux endpoint를 검증합니다.

```bash
./gradlew :bluetape4k-spring-boot-r2dbc:test --no-build-cache --no-configuration-cache
```

`R2dbcEntityOperationsExtensionsTest`는 insert→update→select→delete→exists를 한 흐름으로 검증합니다. `PostRepositoryTest`와 `CommentRepositoryTest`는 nullable 단건, 첫 행, `Flow`, count를 repository 경계에서 확인하고, `PostControllerTest`는 `Flow`와 suspend 반환값이 WebFlux endpoint까지 이어지는지 검증합니다.

## 워크숍과 예제 {#workshops}

모듈 안의 `coroutines.blog` 테스트 애플리케이션이 가장 가까운 실행 예제입니다. `PostRepository`, `CommentRepository`, `PostController`를 함께 읽으면 entity→repository→WebFlux endpoint 흐름을 한 번에 볼 수 있습니다.

더 높은 수준의 SQL DSL과 repository를 연습하려면 [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop)으로 이어갑니다. raw R2DBC부터 시작하고 싶다면 [`bluetape4k-r2dbc` 학습 경로](./bluetape4k-r2dbc/ecosystem-paths.md)를 먼저 읽습니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. 모듈 이름에 `spring-boot`가 들어가지만 별도 auto-configuration, condition, property binding, pool 관리 기능은 없습니다. `R2dbcRepository` 구현을 생성하지 않으며 Spring Data repository interface를 확장하지도 않습니다.

API는 reified type과 Spring Data entity mapping에 의존합니다. raw SQL, batch, generated key 세부 제어, custom row mapping이 필요하면 `bluetape4k-r2dbc` 또는 Spring `DatabaseClient`를 사용합니다.

## Source와 tests {#sources}

- [모듈 README](../../../../spring-boot/r2dbc/README.ko.md)
- [모듈 build](../../../../spring-boot/r2dbc/build.gradle.kts)
- [`R2dbcEntityOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/R2dbcEntityOperationExtensions.kt)
- [`ReactiveSelectOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`ReactiveInsertOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveInsertOperationExtensions.kt)
- [`ReactiveUpdateOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveUpdateOperationExtensions.kt)
- [`ReactiveDeleteOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveDeleteOperationExtensions.kt)
- [`R2dbcEntityOperationsExtensionsTest.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/R2dbcEntityOperationsExtensionsTest.kt)
- [`PostRepository.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/PostRepository.kt)
- [`CommentRepository.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/CommentRepository.kt)
- [`PostControllerTest.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/controller/PostControllerTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot R2DBC 핵심 클래스 구조 다이어그램

[![Spring Boot R2DBC 핵심 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-01.svg)

_배포본 README: [`spring-boot/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.ko.md)_

### R2DBC와 Coroutines 데이터 흐름 다이어그램

[![R2DBC와 Coroutines 데이터 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-02.svg)

_배포본 README: [`spring-boot/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.ko.md)_

### R2DBC CRUD 연산 계층 구조 다이어그램

[![R2DBC CRUD 연산 계층 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-03.svg)

_배포본 README: [`spring-boot/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.ko.md)_

### R2DBC 코루틴 변환 시퀀스 다이어그램

[![R2DBC 코루틴 변환 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-sequence-01.svg)

_배포본 README: [`spring-boot/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.ko.md)_

<!-- release-readme-diagrams:end -->
