---
title: StatelessSession, batch와 event
description: 대량 처리에서 stateful Session과 StatelessSession의 차이, batch 복원과 listener 운영 경계를 설명합니다.
manualId: bluetape4k-hibernate
chapterId: stateless-batch-events
---

# StatelessSession, batch와 event

## 먼저 stateful Session을 줄여 본다

일반 Session에서 JDBC batch만 일시적으로 바꾸려면 `withBatchSize`를 사용합니다. 양수만 허용하고 block 뒤에 이전 값을 복원합니다.

```kotlin
session.withBatchSize(100) {
    entities.forEachIndexed { index, entity ->
        persist(entity)
        if ((index + 1) % 100 == 0) {
            flush()
            clear()
        }
    }
}
```

batch size만 바꿔서는 1차 cache가 줄지 않습니다. 대량 insert에서는 일정 간격으로 flush와 clear를 호출합니다. 이전 batch 값을 읽지 못하면 1.12.1 helper는 0을 사용하고, 복원 실패는 warn log만 남깁니다.

## StatelessSession이 포기하는 기능

`SessionFactory.withStateless`는 StatelessSession과 transaction을 열고 commit·rollback·close합니다. 대량 작업에서 persistence context 비용을 피할 수 있지만 다음 기능이 없습니다.

- 1차 cache와 automatic dirty checking
- cascade persist/remove
- JPA EntityListener
- collection을 따라가는 자동 저장

```kotlin
sessionFactory.withStateless { stateless ->
    masters.forEach { master ->
        stateless.insert(master)
        master.details.forEach(stateless::insert)
    }
}
```

연관 entity는 위 예제처럼 명시적으로 저장합니다. 일반 Session의 entity graph 동작을 그대로 기대하면 일부 row만 저장될 수 있습니다.

## 1.12.1 Spring 경로 제한

`StatelessSessionFactoryBean`은 활성 Spring transaction에서 stateless proxy를 제공하려 하지만 1.12.1에서는 `SessionFactory` 자체를 transaction resource key로 사용합니다. 기존 JPA resource와 충돌할 수 있으며 이 문제는 배포 뒤 dedicated key로 수정됐습니다.

1.12.1 매뉴얼과 예제에서는 Spring 주입 proxy를 기본 경로로 권하지 않습니다. 명시적인 `SessionFactory.withStateless`를 우선하고, Spring transaction과 같은 connection을 반드시 공유해야 한다면 버전과 resource binding을 별도로 검증합니다.

## Event listener와 로그

`SessionFactory.registerEventListener`는 Hibernate event group에 listener를 추가합니다. 오타가 있는 `registEventListener`는 deprecated이므로 새 코드에서 쓰지 않습니다. `JpaEntityEventLogger`와 `HibernateEntityListener`는 entity를 trace log에 출력할 수 있습니다.

- 운영에서 trace가 켜질 수 있는지 확인합니다.
- entity `toString`에 credential, token, 개인정보를 넣지 않습니다.
- listener는 audit 저장소가 아니다. transaction 결과와 별도 audit 내구성이 필요하면 outbox 같은 경계를 사용합니다.

## 실행 예제

```bash
./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.standalone.StatelessSessionStandaloneTest'

./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.SessionSupportTest'
```

## Source와 tests

- [`SessionSupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt)
- [`StatelessSesisonSupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/stateless/StatelessSesisonSupport.kt)
- [`StatelessSessionExtensions.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/stateless/StatelessSessionExtensions.kt)
- [`StatelessSessionFactoryBean.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/spring/StatelessSessionFactoryBean.kt)
- [`StatelessSessionStandaloneTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/standalone/StatelessSessionStandaloneTest.kt)
