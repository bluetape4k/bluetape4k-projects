---
title: 선택 의존성으로 전환
description: 우산 모듈 사용처를 조사하고 Lettuce 또는 Redisson 한 좌표로 안전하게 줄이는 절차를 설명합니다.
manualId: bluetape4k-redis
chapterId: selective-dependency-migration
---

# 선택 의존성으로 전환

## 먼저 실제 사용처를 찾는다

우산 좌표를 작은 좌표로 바꾸는 일은 문자열 한 줄을 고치는 작업이 아닙니다. source import, Spring bean, reflection·configuration, 테스트 fixture와 runtime classpath를 함께 조사합니다.

```bash
rg 'io\.bluetape4k\.redis\.(lettuce|redisson)|io\.lettuce|org\.redisson' src
./gradlew :application:dependencies --configuration runtimeClasspath
```

Redisson type import가 없더라도 Spring configuration이 `RedissonClient`를 만들 수 있고, Lettuce type을 직접 쓰지 않아도 Spring Data Redis의 기본 driver로 Lettuce가 남을 수 있습니다. 여기서는 bluetape4k client helper 제거와 Spring Data driver 선택을 별개로 봅니다.

## 한 단계씩 줄인다

1. 새 코드는 목표 client만 사용하도록 경계를 고정합니다.
2. 제거할 client의 factory, bean, Codec과 shutdown hook을 찾습니다.
3. 공유 key를 목표 Codec으로 읽을 수 있는지 확인하거나 새 key prefix를 정합니다.
4. 목표 client로 contract·integration test를 통과시킵니다.
5. 우산 좌표를 선택 좌표로 바꿉니다.
6. runtime dependency report에서 제거한 client와 불필요한 Codec runtime이 사라졌는지 확인합니다.

Lettuce만 남기는 build는 다음과 같습니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-lettuce")
}
```

Redisson만 남긴다면 artifact 이름만 `bluetape4k-redisson`으로 바꿉니다. 두 좌표에 개별 버전을 붙이지 않습니다.

## Codec migration을 의존성 변경과 섞지 않는다

client 제거와 저장 형식 변경을 한 번에 배포하면 decode failure가 발생했을 때 원인을 구분하기 어렵습니다. 기존 key를 그대로 유지해야 한다면 먼저 양쪽에서 호환되는 wire format을 검증합니다. 호환되지 않으면 새 prefix로 dual-write·backfill·cutover를 설계하고, 이전 key의 만료 또는 삭제 시점을 정합니다.

## 완료 조건

- 제거 대상 client의 source import와 bean이 없습니다.
- shutdown hook과 metrics에도 제거 대상 client가 남아 있지 않습니다.
- 공유 Redis key의 Codec migration이 검증됐습니다.
- runtime classpath에서 제거 대상 artifact가 사라졌거나, 다른 dependency가 가져오는 이유를 기록했습니다.
- application integration test가 실제 Redis에서 통과했습니다.

다른 library가 Redisson이나 Lettuce를 transitive dependency로 가져온다면 classpath에 jar가 남을 수 있습니다. 이것은 client instance가 실행된다는 뜻은 아니지만, 보안·용량 관리 관점에서는 dependency owner를 확인해야 합니다.

## Release sources

- [`infra/redis/build.gradle.kts`](../../../../../infra/redis/build.gradle.kts)
- [`infra/lettuce/build.gradle.kts`](../../../../../infra/lettuce/build.gradle.kts)
- [`infra/redisson/build.gradle.kts`](../../../../../infra/redisson/build.gradle.kts)
- [우산 의존성 계약](./umbrella-dependency-contract.md)
