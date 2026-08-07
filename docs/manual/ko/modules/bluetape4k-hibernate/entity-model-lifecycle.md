---
title: 엔티티 모델과 수명주기
description: 식별자 생성, transient·managed·detached 상태와 엔티티 동일성 계약을 설명합니다.
manualId: bluetape4k-hibernate
chapterId: entity-model-lifecycle
---

# 엔티티 모델과 수명주기

## 상태를 먼저 구분한다

Hibernate entity는 새로 만든 transient 상태, persistence context가 추적하는 managed 상태, context에서 떨어진 detached 상태를 오갑니다. 같은 객체처럼 보여도 상태에 따라 `persist`, `merge`, lazy loading과 변경 감지 결과가 달라집니다.

`IntJpaEntity`와 `LongJpaEntity`는 `IDENTITY` 전략을 사용하므로 보통 insert 뒤에 id가 생깁니다. `UuidJpaEntity`는 객체를 만들 때 UUID v7을 할당합니다. 이 모듈의 `isPersisted`는 `id != null`만 확인하므로 UUID entity는 실제 insert 전에도 persisted로 판단됩니다. `isPersisted`를 database 존재 여부 검사로 사용하면 안 됩니다.

```kotlin
@Entity
class Account(
    var email: String = "",
): LongJpaEntity() {
    override fun equalProperties(other: Any): Boolean =
        other is Account && email == other.email
}
```

`identifier`는 id가 아직 없으면 `IllegalStateException`을 던집니다. 생성 직후 호출하지 말고 id 할당이 끝난 뒤 사용합니다.

## equals와 hashCode 계약

`AbstractJpaEntity.equals`는 두 entity가 모두 persisted면 id를 비교합니다. 둘 다 transient면 `equalProperties`를 사용하고, 한쪽만 persisted면 같지 않다고 판단합니다. proxy는 `Hibernate.unproxy`로 실제 entity를 찾습니다.

1.12.1에는 알려진 제한이 있습니다. business signature가 같은 transient entity 두 개는 `equals == true`가 될 수 있지만 `hashCode`는 각 instance의 identity hash라 서로 다를 수 있습니다. 이 동작은 1.12.1 뒤에 수정됐으며, 현재 `develop`의 class-based hash를 이 버전의 계약으로 설명하면 안 됩니다.

- id 없는 entity를 `HashSet`의 중복 제거 키로 사용하지 않습니다.
- entity가 persistent 상태로 바뀌는 동안 hash collection에 보관하지 않습니다.
- service 경계 밖의 식별에는 entity 대신 안정적인 business key나 별도 value object를 사용합니다.

## Tree entity

`IntJpaTreeEntity`와 `LongJpaTreeEntity`는 self-reference tree의 parent와 children mapping을 제공합니다. `addChildren`과 `removeChildren`은 collection과 반대편 parent를 함께 고칩니다.

```kotlin
val root = Category("root")
val child = Category("child")

root.addChildren(child)
check(child.parent === root)

root.removeChildren(child)
check(child.parent == null)
```

`children`에는 `CascadeType.ALL`이 적용됩니다. 삭제 전파가 도메인 규칙과 맞는지 확인해야 합니다. `AbstractJpaTreeEntity`에는 Querydsl 제약 때문에 `@MappedSuperclass`를 붙이지 않고 실제 Int/Long 구현에만 둡니다.

## Lazy proxy와 transaction

`getReference`는 즉시 row를 읽지 않고 proxy를 돌려줄 수 있습니다. transaction이 끝난 뒤 lazy property를 읽으면 초기화할 session이 없어 실패합니다. API 응답이나 message로 entity를 바로 넘기지 말고 transaction 안에서 필요한 값을 읽어 DTO로 바꿉니다.

`isLoaded(entity, property)`는 특정 연관이 초기화됐는지 검사할 때 쓸 수 있지만 fetch 계획을 대신하지 않습니다. repository query의 join fetch, EntityGraph 또는 projection으로 필요한 데이터를 명시합니다.

## 학습할 테스트

```bash
./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.model.JpaEntityModelTest'

./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.mapping.tree.TreeNodeTest'
```

association, inheritance와 composite id 예제는 `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping` 아래에 주제별로 나뉘어 있습니다.

## Source와 tests

- [`JpaEntity.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/JpaEntity.kt)
- [`AbstractJpaEntity.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractJpaEntity.kt)
- [`UuidJpaEntity.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/UuidJpaEntity.kt)
- [`JpaTreeEntity.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/JpaTreeEntity.kt)
- [`TreeNodeTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping/tree/TreeNodeTest.kt)
