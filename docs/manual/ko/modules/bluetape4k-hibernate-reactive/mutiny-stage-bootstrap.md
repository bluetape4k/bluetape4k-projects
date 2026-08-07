---
title: Mutiny·Stage 선택과 시작
description: Reactive persistence unit을 구성하고 애플리케이션 API에 맞는 SessionFactory를 선택합니다.
manualId: bluetape4k-hibernate-reactive
chapterId: mutiny-stage-bootstrap
---

# Mutiny·Stage 선택과 시작

## 먼저 선택할 API

Hibernate Reactive는 같은 ORM 기능을 Mutiny와 Stage 두 API로 노출합니다. `bluetape4k-hibernate-reactive`는 양쪽 모두에 코루틴 bridge를 제공하지만, 하나의 use case에서 둘을 번갈아 쓸 이유는 거의 없습니다.

| 애플리케이션 경계 | 권장 API |
| --- | --- |
| Mutiny pipeline과 `Uni` operator가 이미 중심 | Mutiny |
| Java library와 `CompletionStage`로 연동 | Stage |
| Kotlin service가 suspend 함수만 노출 | 팀이 운영하기 쉬운 한쪽을 정해 일관되게 사용 |

Mutiny가 Stage의 완전한 별칭은 아닙니다. `1.12.1`에서 Mutiny `findAs`·`getAs`는 JPA `LockModeType`과 EntityGraph 기반 조회 오버로드를 더 제공하지만 Stage에는 그 오버로드가 없습니다.

## Reactive provider 구성

Reactive persistence unit은 provider를 명시하고 entity를 등록합니다.

```xml
<persistence-unit name="default">
    <provider>org.hibernate.reactive.provider.ReactivePersistenceProvider</provider>
    <class>com.example.Author</class>
    <class>com.example.Book</class>
    <exclude-unlisted-classes>true</exclude-unlisted-classes>
</persistence-unit>
```

테스트는 Jakarta Persistence XML schema 3.0 문서를 사용합니다. 이는 BOM이 제공하는 Jakarta Persistence API 버전과 별개입니다. README의 Hibernate Reactive·ORM 버전 표도 `1.12.1` version catalog와 맞지 않으므로 사용자 설정에 복사하지 않습니다.

## Factory unwrap

```kotlin
val mutiny = entityManagerFactory.asMutinySessionFactory()
val stage = entityManagerFactory.asStageSessionFactory()
```

두 함수는 `EntityManagerFactory.unwrap(...)`을 그대로 호출합니다. 새 factory를 만들지 않으며 원래 JPA factory와 같은 provider resource를 가리킵니다. Reactive provider가 아니면 unwrap 예외가 호출자에게 전달됩니다.

테스트에서는 factory를 lazy하게 만들고 suite 종료 시 `closeSafe()`로 닫습니다. 실제 애플리케이션도 factory를 만든 component가 shutdown을 책임져야 합니다.

## 첫 조회

```kotlin
val author = mutiny.withSessionSuspending { session ->
    session.findAs<Author>(authorId).awaitSuspending()
}
```

Stage를 선택했다면 같은 구조에서 `await()`를 사용합니다. 존재하지 않는 ID는 테스트에서 `null`로 확인됩니다. 호출부의 null 계약을 명시하고, 없음을 예외로 바꿀지는 service boundary에서 결정합니다.

## 실행 근거

- [`persistence.xml`](../../../../../data/hibernate-reactive/src/test/resources/META-INF/persistence.xml)
- [Mutiny factory 변환](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/EntityManagerFactorySupport.kt)
- [Stage factory 변환](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/EntityManagerFactorySupport.kt)
- [`MutinyExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)
- [`StageExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageExtrasTest.kt)
