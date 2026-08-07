---
title: 테스트와 실패 동작
description: 빠른 context test와 Redis 통합 test를 나누고 condition, cache cycle, 관측 실패를 검증합니다.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: testing-and-failure-modes
---

# 테스트와 실패 동작

## 두 층으로 나눠 테스트한다

설정 변환과 실제 cache 동작을 한 테스트에 모두 넣으면 실패 원인을 찾기 어렵습니다. 1.12.1 모듈도 두 층으로 나눕니다.

| 테스트 | 외부 자원 | 검증 내용 |
| --- | --- | --- |
| `LettuceNearCacheAutoConfigurationTest` | 없음 | condition, 기본값, customizer mapping, bean 등록 |
| `LettuceNearCachePropertiesCustomizerTest` | 없음 | region TTL 여러 건, metrics key 생략 |
| `LettuceNearCacheIntegrationTest` | Redis Testcontainer + H2 | entity 조회, L2 miss/put/hit, endpoint, gauge, 병렬 조회 |

일상 개발에서는 context test를 먼저 돌리고, cache provider나 통합 설정을 건드렸을 때 container test를 실행합니다.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test \
  --tests '*LettuceNearCacheAutoConfigurationTest' \
  --no-build-cache --no-configuration-cache
```

## Condition 계약 잠그기

최소한 다음을 테스트합니다.

- 기본 설정에서 `HibernatePropertiesCustomizer`가 하나 등록된다.
- 전체 `enabled=false`에서 customizer가 없다.
- metrics를 끄면 binder와 statistics property가 없다.
- `MeterRegistry`가 있을 때 binder가 하나 등록된다.
- `EntityManagerFactory`가 있을 때 endpoint가 등록된다.

이 검증은 Redis 연결보다 먼저 실패해야 하는 classpath·property 문제를 빠르게 분리합니다.

## 실제 cache cycle

통합 테스트는 Hibernate statistics를 초기화한 뒤 entity를 저장하고 transaction 밖에서 두 번 조회합니다. 첫 조회는 L2 miss와 put, 두 번째 조회는 hit를 만들어야 합니다. `@Transactional` 안에서 같은 persistence context로 두 번 읽으면 1차 cache가 응답해 L2 검증이 되지 않으므로 transaction 경계를 분리합니다.

```kotlin
sessionFactory.statistics.clear()

repository.findById(id) // L2 miss -> database -> put
repository.findById(id) // L2 hit
```

put 횟수는 insert 후 callback 영향이 있을 수 있어 테스트가 최소 1로 검증합니다. 구현 세부에 맞춘 지나치게 엄격한 counter는 upgrade 때 불필요한 실패를 만들 수 있습니다.

## 실패를 해석하는 법

- context에 customizer가 없으면 `enabled`, classpath와 auto-configuration import를 확인합니다.
- Hibernate가 RegionFactory를 쓰지 않으면 최종 Hibernate property map과 다른 customizer의 override를 봅니다.
- endpoint가 빈 map이면 factory unwrap, 실제 RegionFactory 타입과 region 생성 여부를 확인합니다.
- endpoint의 L2 필드가 null이면 `hibernate.generate_statistics`를 확인합니다.
- gauge가 없으면 `MeterRegistry`, metrics condition과 binder warning 로그를 확인합니다.
- cache test가 database query를 계속 만들면 transaction 경계, entity annotation, region 이름과 statistics를 함께 봅니다.

관측 구성은 실패를 완화해 startup을 계속할 수 있습니다. 반면 Redis 연결과 cache serialization 실패는 하위 provider 경로에서 발생하므로 Spring endpoint의 빈 응답과 같은 방식으로 처리된다고 가정하지 않습니다.

## 통합 테스트 주의점

1.12.1의 좁은 test classpath에서는 Spring Boot 4 split auto-configuration이 JPA/JDBC bean을 중복 등록할 수 있어 통합 테스트가 일부 framework default를 명시적으로 제외합니다. 이는 일반 애플리케이션에 그대로 복사할 설정이 아니라 이 모듈의 test fixture 제약입니다.

Testcontainers test는 다른 database·Redis suite와 병렬로 돌리지 않습니다. Docker 자원과 port보다 shared daemon·메모리 경합이 결과를 흐릴 수 있습니다.

## 실행 근거

- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
- [`LettuceNearCachePropertiesCustomizerTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCachePropertiesCustomizerTest.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
- [`ReadmeDependencyContractTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/ReadmeDependencyContractTest.kt)
