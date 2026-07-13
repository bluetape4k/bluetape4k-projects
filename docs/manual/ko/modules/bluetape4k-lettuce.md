---
manualId: bluetape4k-lettuce
title: "bluetape4k-lettuce"
description: "Lettuce Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. 고성능 바이너리 Codec과 RedisFuture → Coroutines 어댑터를 제공합니다."
kind: library
group: infrastructure
---

# bluetape4k-lettuce

## 해결하는 문제 {#problem}

Lettuce Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. 고성능 바이너리 Codec과 RedisFuture → Coroutines 어댑터를 제공합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-lettuce`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-lettuce")
}
```

Gradle project path는 `:bluetape4k-lettuce`, source directory는 `infra/lettuce`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `LettuceClients`, `LettuceConst`, `RedisCommandSupports`, `RedisFutureSupport`, `LettuceAtomicLong`, `LettuceSuspendAtomicLong`, `LettuceBinaryCodec`, `LettuceBinaryCodecs`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`LettuceClients`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`LettuceClients`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceConst`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceConst.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedisCommandSupports`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupports.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedisFutureSupport`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceAtomicLong.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceSuspendAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceSuspendAtomicLong.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceBinaryCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceBinaryCodecs`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceIntCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceIntCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceJsonCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **주요 기능**, **성능 최적화**, **Codec 벤치마크 결과**, **커넥션 벤치마크 결과**, **핵심 기법**, **1. 공유 DEFAULTCLIENTRESOURCES (NCPU 스레드 풀)**, **2. 튜닝된 SocketOptions**, **3. withPipeline{} — 일괄 플러시 확장**, **4. Collection.awaitAll() — 일괄 대기**, **벤치마크에서 얻은 교훈** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(libs.lettuce.core)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
compileOnly(project(":bluetape4k-cache-core"))
compileOnly(libs.fory.kotlin)
compileOnly(libs.kryo5)
compileOnly(libs.lz4.java)
compileOnly(libs.snappy.java)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

connection 상태, queue 깊이, retry, timeout, remote 오류, graceful shutdown을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-lettuce:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractLettuceTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AbstractLettuceTest.kt)
- [`AsyncCommandsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AsyncCommandsTest.kt)
- [`CoroutinesCommandTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/CoroutinesCommandTest.kt)
- [`LettuceClientsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)
- [`LettuceTestUtils`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceTestUtils.kt)
- [`RedisCommandSupportsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupportsTest.kt)
- [`RedisFutureSupportTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupportTest.kt)
- [`LettuceAtomicLongTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceAtomicLongTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../infra/lettuce/README.ko.md)
- [모듈 build](../../../../infra/lettuce/build.gradle.kts)
- [`LettuceClients`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`LettuceConst`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceConst.kt)
- [`RedisCommandSupports`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupports.kt)
- [`RedisFutureSupport`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`LettuceAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceAtomicLong.kt)
- [`LettuceSuspendAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceSuspendAtomicLong.kt)
- [`LettuceBinaryCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt)
- [`LettuceBinaryCodecs`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
- [`LettuceIntCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceIntCodec.kt)
- [`LettuceJsonCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt)
- [`AbstractLettuceTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AbstractLettuceTest.kt)
- [`AsyncCommandsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AsyncCommandsTest.kt)
