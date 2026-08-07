---
manualId: bluetape4k-hibernate
title: "Hibernate와 Querydsl 확장"
description: "Hibernate ORM과 JPA를 Kotlin에서 사용할 때 엔티티 수명주기, 쿼리, converter와 StatelessSession 경계를 다루는 방법을 설명합니다."
kind: library
group: data
learningOrder: 640
---

# Hibernate와 Querydsl 확장

## 제공하는 기능 {#problem}

`bluetape4k-hibernate`는 Hibernate ORM과 Jakarta Persistence API를 Kotlin 코드에서 쓰기 편하게 다듬은 라이브러리입니다. 엔티티 베이스 클래스, `EntityManager`와 `Session` 확장, Criteria·Querydsl 보조 함수, AttributeConverter, StatelessSession 작업 경계를 제공합니다.

이 모듈은 Hibernate의 persistence context와 transaction 규칙을 없애지 않습니다. `persist`와 `merge`의 차이, lazy loading, flush 시점, bulk query가 persistence context를 우회하는 동작은 그대로 유지됩니다. 그래서 API 이름만 익히기보다 각 helper가 어느 수명주기를 소유하는지 먼저 알아야 합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- aggregate와 entity lifecycle을 ORM이 관리할 가치가 있는지 판단합니다.
- Spring transaction manager가 경계를 소유할지, `withNewEntityManager`가 독립 transaction을 만들지 정합니다.
- entity의 `equals`와 `hashCode`에 쓸 business signature를 정의합니다.
- JPQL, Criteria, Querydsl 가운데 팀이 유지할 주 쿼리 방식을 고릅니다.
- converter에 저장할 데이터의 신뢰 경계와 암호화 keyset 보관·회전 방식을 정합니다.
- 대량 작업에 stateful Session이 필요한지, cascade와 listener가 없는 StatelessSession으로 충분한지 구분합니다.

SQL을 직접 통제하는 작은 persistence adapter라면 [bluetape4k-jdbc](./bluetape4k-jdbc.md)가 더 단순할 수 있습니다. Kotlin SQL DSL을 중심으로 개발하려면 [기술 선택 가이드](./bluetape4k-hibernate/ecosystem-paths.md)에서 Exposed와 비교합니다.

## 의존성 추가 {#coordinates}

사용자는 Hibernate, Querydsl과 하위 bluetape4k 모듈의 버전을 각각 맞추지 않습니다. 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate")

    runtimeOnly("org.postgresql:postgresql") // 사용하는 driver로 교체
}
```

Spring Data JPA, cache provider와 데이터베이스 driver는 애플리케이션이 선택합니다. helper가 해당 runtime 구현을 자동으로 설치하지는 않습니다.

## 첫 transaction {#quick-start}

독립 실행 코드에서는 `withNewEntityManager`가 `EntityManager`, transaction, close를 한 범위에서 관리합니다.

```kotlin
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.hibernate.withNewEntityManager
import jakarta.persistence.EntityManagerFactory

fun renameAccount(
    emf: EntityManagerFactory,
    id: Long,
    newName: String,
) = emf.withNewEntityManager { em ->
    val account = checkNotNull(em.findAs<Account>(id))
    account.name = newName
    account
}
```

블록이 끝나면 dirty checking 결과를 commit하고 `EntityManager`를 닫습니다. 블록이 실패하면 rollback을 시도한 뒤 원래 예외를 던집니다. rollback 자체의 실패는 1.12.1에서 경고 로그로만 남고 suppressed exception에는 들어가지 않습니다. Spring 애플리케이션에서는 보통 `@Transactional`과 주입된 `EntityManager`가 경계를 소유하므로 이 helper로 transaction을 한 겹 더 만들지 않습니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| 독립 EntityManager와 transaction 실행 | `EntityManagerFactory.withNewEntityManager` | commit·rollback·close를 helper가 소유합니다. |
| 엔티티 저장·삭제 | `save`, `delete`, `deleteById` | `merge` 결과는 원본과 다른 managed instance일 수 있습니다. |
| 타입을 반복하지 않는 조회 | `findAs`, `findOne`, `createQueryAs` | proxy 초기화와 SQL 실행 시점은 Hibernate 규칙을 따릅니다. |
| natural id 조회 | `findBySimpleNaturalId`, `findByNaturalId` | 복합 natural-id map은 비어 있으면 안 됩니다. |
| Criteria 구성 | `createQueryAs`, `attribute`, `eq`, `ne`, `inValues` | property 이름이 JPA mapping 이름과 같아야 합니다. |
| Querydsl expression 구성 | `querydsl.core`, `querydsl.jpa` 확장 | Q 타입 생성과 annotation processing이 필요합니다. |
| 일시적인 JDBC batch 크기 변경 | `Session.withBatchSize` | 양수만 허용하며 종료 뒤 기존 값을 복원합니다. |
| 대량·저수준 작업 | `SessionFactory.withStateless` | 1차 캐시, dirty checking, cascade와 JPA listener가 없습니다. |
| 컬럼 값 변환 | `converters` package | JSON, 암호화와 직렬화 converter의 실패 계약이 서로 다릅니다. |

## 학습 경로 {#concepts}

각 장에는 기능 설명만이 아니라 실제 코드, 잘못 쓰기 쉬운 지점, 1.12.1 배포 소스와 대표 테스트를 함께 담았습니다. 설명을 읽고 곧바로 실행 예제와 구현 근거를 확인할 수 있습니다.

1. [엔티티 모델과 수명주기](./bluetape4k-hibernate/entity-model-lifecycle.md) — 식별자, `persist` 전후의 동일성, tree entity와 proxy를 다룹니다.
2. [EntityManager와 transaction](./bluetape4k-hibernate/entitymanager-transactions.md) — transaction 소유권, save·delete, flush와 bulk query 경계를 설명합니다.
3. [JPQL, Criteria와 Querydsl](./bluetape4k-hibernate/queries-criteria-querydsl.md) — 단순 조회에서 동적 query와 projection으로 확장합니다.
4. [Converter와 보안 경계](./bluetape4k-hibernate/converters-security.md) — JSON, 압축, 암호화 keyset과 안전한 직렬화를 구분합니다.
5. [StatelessSession, batch와 event](./bluetape4k-hibernate/stateless-batch-events.md) — 대량 처리에서 포기하는 ORM 기능과 운영 주의점을 확인합니다.
6. [Hibernate 다음의 기술 선택](./bluetape4k-hibernate/ecosystem-paths.md) — JDBC, Exposed, JPA 예제와 reactive 경로를 비교합니다.

처음 사용한다면 1→2→3 순서로 읽고, converter를 쓰는 애플리케이션은 4장을 배포 전에 반드시 확인합니다. 대량 처리를 설계할 때는 5장을 먼저 읽어 stateful Session과 StatelessSession의 차이를 정합니다.

## 권장 패턴 {#patterns}

transaction은 여러 변경이 함께 성공하거나 실패해야 하는 service 경계에 둡니다. `merge`를 호출한 뒤에는 반환된 managed entity를 사용하고, query 결과는 transaction 안에서 필요한 연관까지 읽거나 DTO로 바꿉니다. 대량 조회에는 항상 pagination을 지정하고, bulk update/delete 뒤에는 persistence context를 정리합니다.

엔티티의 business equality는 생성 시점부터 안정적인 값으로 정의합니다. 다만 1.12.1의 transient entity hash 계약에는 알려진 제한이 있으므로 식별자가 없는 엔티티를 hash 기반 collection의 중복 제거 키로 사용하지 않습니다.

## 연동 {#integrations}

모듈은 Hibernate ORM, Jakarta Persistence·Transaction·Validation, Querydsl JPA를 API로 제공합니다. Spring Boot JPA 연동은 `compileOnly`이며 애플리케이션이 starter와 transaction manager를 구성합니다. converter runtime에는 Tink, Jackson과 여러 압축·직렬화 구현이 사용됩니다.

Hibernate 2차 cache가 필요하면 `bluetape4k-hibernate-cache-lettuce`를 별도로 검토합니다. non-blocking 실행 모델이 필요하면 일반 Session을 coroutine에서 감싸지 말고 `bluetape4k-hibernate-reactive` 또는 R2DBC 경로를 선택합니다.

## 설정 {#configuration}

datasource, dialect, schema migration, connection pool, statement timeout, batch 크기, SQL logging과 cache 설정은 애플리케이션이 소유합니다. `HibernateConsts.DefaultJpaProperties`는 schema 자동 생성을 끄지만 `SHOW_SQL=true`, `FORMAT_SQL=true`, pool size 30을 설정합니다. 이 값을 운영 기본 설정으로 복사하지 말고 logging 정책과 실제 pool 구현에 맞춰 다시 정합니다.

암호화 converter를 쓰면 애플리케이션 시작 시 외부 secret store에서 keyset을 읽어 `EncryptedStringConverterKeysets`를 먼저 구성해야 합니다. source code나 평문 설정 파일에 keyset JSON을 넣지 않습니다.

## 실패 동작 {#failures}

Hibernate와 database provider 예외는 기본적으로 호출자에게 전파됩니다. SQL과 constraint 오류는 helper 호출이 아니라 flush나 commit에서 나타날 수 있습니다. `findOneOrNull`은 `NoResultException`만 null로 바꾸며 non-unique와 database 오류는 숨기지 않습니다.

`AbstractObjectAsJsonConverter`는 Jackson 변환 실패를 로그로 남기고 `null`을 반환합니다. 필수 컬럼이나 데이터 손실이 허용되지 않는 경로에서는 converter 앞뒤의 validation이 필요합니다. 암호화 converter는 keyset이 없으면 즉시 실패하고, 다른 keyset으로 만든 암호문은 복호화할 수 없습니다.

## 운영 {#operations}

query latency, flush 횟수, transaction rollback, connection pool, batch 크기, 1차·2차 cache hit rate와 lazy-loading query 수를 함께 관찰합니다. `findAll`의 기본 상한은 `Int.MAX_VALUE`이므로 운영 query에는 명시적으로 pagination을 둡니다. entity listener의 trace log는 entity 전체를 출력할 수 있어 개인정보나 암호화 전 평문이 남지 않는지 확인합니다.

## 테스트 {#testing}

모듈 테스트에는 EntityManager·Session helper, mapping, converter, Querydsl, StatelessSession과 Spring 통합 예제가 들어 있습니다. 일부 경로는 Testcontainers를 사용합니다.

```bash
./gradlew :bluetape4k-hibernate:test --no-build-cache --no-configuration-cache
```

`TestEntityManager`라는 helper가 테스트 소스에 있지만 일반 main artifact의 공개 API는 아닙니다. 매뉴얼 예제에서는 Spring Boot가 제공하는 기능처럼 사용하지 않습니다.

## 워크숍 {#workshops}

전용 workshop은 아직 등록되지 않았습니다. 대신 `mapping` 아래의 association·inheritance·natural-id·tree 테스트와 `SimpleQuerydslExamples`, `StatelessSessionStandaloneTest`가 실행 가능한 학습 자료입니다. 각 장에서 작은 테스트 단위로 연결합니다.

JPA 애플리케이션 형태의 예제는 [JPA Querydsl demo](./bluetape4k-examples-jpa-querydsl-demo.md)와 [Blaze-Persistence demo](./bluetape4k-examples-jpa-blazepersistence-demo.md)로 이어집니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 태그의 소스를 기준으로 합니다. 이후 `develop`에서 고친 transient entity hash 계약과 Spring transaction의 StatelessSession resource key 분리는 1.12.1 기능으로 설명하지 않습니다.

1.12.1의 `StatelessSessionFactoryBean`은 Spring transaction resource key가 기존 JPA resource와 충돌할 수 있습니다. 이 버전에서는 Spring 주입 proxy보다 명시적인 `SessionFactory.withStateless`를 우선합니다. StatelessSession 자체도 cascade, dirty checking, 1차 cache와 JPA listener를 제공하지 않습니다.

## Source와 tests {#sources}

- [`EntityManagerFactorySupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupport.kt)
- [`EntityManagerSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt)
- [`SessionSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt)
- [`AbstractJpaEntity.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractJpaEntity.kt)
- [`CriteriaSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/criteria/CriteriaSupport.kt)
- [`EncryptedStringConverters.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/EncryptedStringConverters.kt)
- [`StatelessSesisonSupport.kt`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/stateless/StatelessSesisonSupport.kt)
- [`EntityManagerSupportTest.kt`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerSupportTest.kt)
- [`SimpleQuerydslExamples.kt`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/SimpleQuerydslExamples.kt)
- [`StatelessSessionStandaloneTest.kt`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/standalone/StatelessSessionStandaloneTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 영속성 확장 구조 다이어그램

[![영속성 확장 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-01.svg)

_배포본 README: [`data/hibernate/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate/README.ko.md)_

### JPA 엔티티 클래스 계층 구조 다이어그램

[![JPA 엔티티 클래스 계층 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-02.svg)

_배포본 README: [`data/hibernate/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate/README.ko.md)_

### AttributeConverter 종류 다이어그램

[![AttributeConverter 종류 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-diagram-03.svg)

_배포본 README: [`data/hibernate/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate/README.ko.md)_

<!-- release-readme-diagrams:end -->
