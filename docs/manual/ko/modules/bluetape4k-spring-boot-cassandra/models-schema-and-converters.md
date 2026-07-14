---
title: "모델, 변환과 스키마"
description: "Persistable과 감사 모델의 신규 판단, converter 책임, SchemaGenerator의 생성·truncate 범위를 다룹니다."
---

# 모델, 변환과 스키마

## Persistable의 ID와 동등성

`AbstractCassandraPersistable<PK>`는 Spring Data `Persistable`과 `Serializable`을 구현합니다. 하위 class는 `getId()`와 `setId()`를 제공합니다.

- `id == null`이면 `isNew()`가 `true`입니다.
- 두 객체의 실제 user class와 non-null ID가 같을 때만 동등합니다.
- ID가 없으면 `hashCode()`는 identity hash를 사용합니다.
- ID가 생긴 뒤 hash code가 달라질 수 있습니다.

영속화 전 entity를 `HashSet`이나 `HashMap` key로 넣었다가 ID를 설정하면 collection에서 찾지 못할 수 있습니다. mutable entity의 equality 정책을 domain model과 함께 검토합니다.

## 감사 모델의 신규 판단

`AbstractCassandraAuditable<U, PK>`는 생성자·생성 시각·수정자·수정 시각을 Cassandra column에 매핑합니다. 이 class는 부모의 ID 기반 `isNew`를 override하고 `_createdAt == null`을 사용합니다.

```kotlin
@EnableCassandraAuditing
@Table("users")
class UserEntity : AbstractCassandraAuditable<String, UUID>() {
    @PrimaryKey
    private var pk: UUID? = null

    override fun getId(): UUID? = pk
    override fun setId(id: UUID) { pk = id }
}
```

감사 기능과 `AuditorAware`를 구성하지 않으면 `createdAt`이 채워지지 않습니다. 그 상태에서는 ID가 있어도 `isNew()`가 `true`입니다. source의 마지막 수정자 컬럼명은 `lastModified_by`이므로 기존 snake_case schema와 일치하는지도 확인합니다.

## Converter는 Spring Data가 소유한다

이 모듈은 application converter를 자동 등록하지 않습니다. custom type은 Spring Data Cassandra configuration에서 reading/writing converter를 등록합니다. 테스트의 `CurrencyConverter`와 type-mapping fixture는 이 경계를 보여 줍니다.

UDT, collection, temporal type과 enum mapping은 driver codec과 Spring Data converter 중 어느 계층이 책임지는지 하나로 정합니다. 같은 타입을 두 계층에서 다르게 변환하면 repository와 raw session 결과가 달라질 수 있습니다.

## SchemaGenerator가 하는 일

`SchemaGenerator.createTableAndTypes`는 Spring Data mapping context에서 entity metadata를 찾고 다음 순서로 실행합니다.

1. entity 또는 property에서 필요한 UDT를 탐색합니다.
2. UDT create specification에 `IF NOT EXISTS`를 붙여 실행합니다.
3. 현재 keyspace metadata에 table이 없을 때 create table CQL을 실행합니다.

`potentiallyCreateTableFor`는 table 존재만 확인합니다. 기존 column, primary key, clustering order나 option 차이를 비교하지 않습니다.

## truncate의 위험

`SchemaGenerator.truncate<T>`는 현재 keyspace metadata에 table이 있으면 Spring Data `truncate`를 실행합니다. table이 없으면 아무 일도 하지 않지만, table이 있으면 row를 전부 지웁니다.

테스트 fixture 초기화에는 유용하지만 일반 application startup이나 request path에 넣지 않습니다. 운영 migration은 review 가능한 CQL과 이력, 순서, rollback 전략을 가진 별도 절차를 사용합니다.

## Criteria helper

`Criteria.eq`는 backtick이 필요한 `Criteria.is(value)`를 infix 형태로 부르는 별칭입니다.

```kotlin
val query = Query.query(Criteria.where("tenant_id") eq tenantId)
```

partition key나 clustering key를 무시한 query를 허용해 주는 기능은 아닙니다. Cassandra query model과 index 설계는 그대로 지켜야 합니다.

## 근거

- [`AbstractCassandraPersistable.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraPersistable.kt)
- [`AbstractCassandraAuditable.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraAuditable.kt)
- [`SchemaGenerator.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt)
- [`AbstractCassandraModelTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraModelTest.kt)
- [`SchemaGeneratorTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGeneratorTest.kt)
