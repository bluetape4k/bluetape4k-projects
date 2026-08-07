---
manualId: bluetape4k-examples-redisson-demo
title: "Redisson 예제"
description: "Redisson을 Kotlin Coroutines와 함께 사용하는 분산 Redis 패턴 예제 모음입니다."
kind: example
group: examples
learningOrder: 1420
---

# Redisson 예제

## 해결하는 문제 {#problem}

Redisson을 Kotlin Coroutines와 함께 사용하는 분산 Redis 패턴 예제 모음입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 실행 entry point, 필요한 service, 기대 동작, 예제가 보여 주는 production pattern이 필요할 때 `bluetape4k-examples-redisson-demo`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

이 example project는 Maven artifact로 게시하지 않습니다. 저장소에서 실행하고 명령을 선택하기 전에 Gradle task를 확인합니다.

Gradle project path는 `:bluetape4k-examples-redisson-demo`, source directory는 `examples/redisson-demo`입니다.

## 핵심 개념 {#concepts}

이 모듈은 설정 또는 platform metadata이며 index할 Kotlin/Java source type이 없습니다.

## 빠른 시작 {#quick-start}

example이나 benchmark를 실행하기 전에 project task를 확인합니다.

```bash
./gradlew :bluetape4k-examples-redisson-demo:tasks --all
```

그다음 모듈 README에 기록된 명령을 사용하고 필요한 외부 service는 격리합니다.

## 작업별 API {#api-by-task}

이 모듈에는 등록된 Kotlin/Java source file이 없습니다. build model과 README가 public surface입니다.

## 권장 패턴 {#patterns}

README 근거는 **예제 목록**, **분산 락 (coroutines/locks/)**, **Redis 객체 (coroutines/objects/)**, **컬렉션 (coroutines/collections/)**, **캐시 전략 (coroutines/cachestrategy/)**, **Read/Write Through (coroutines/readwritethrough/)**, **주요 패턴 예시**, **분산 락**, **Read-Through 캐시**, **분산 세마포어** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(enforcedPlatform(libs.spring.boot.dependencies))
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

격리된 환경에서 example을 실행하고 startup, dependency health, request, shutdown을 확인합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-examples-redisson-demo:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractRedissonCoroutineTest`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/AbstractRedissonCoroutineTest.kt)
- [`AbstractCacheExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/AbstractCacheExample.kt)
- [`ActorSchema`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/ActorSchema.kt)
- [`CacheApplication`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheApplication.kt)
- [`CacheReadThroughExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheReadThroughExample.kt)
- [`CacheWriteBehindExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteBehindExample.kt)
- [`CacheWriteBehindForIoTData`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteBehindForIoTData.kt)
- [`CacheWriteThroughExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteThroughExample.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Redisson 예제 pattern 지도

[![Redisson 예제 pattern 지도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-redisson-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-redisson-demo-diagram-01.svg)

_배포본 README: [`examples/redisson-demo/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/redisson-demo/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../examples/redisson-demo/README.ko.md)
- [모듈 build](../../../../examples/redisson-demo/build.gradle.kts)
- [`AbstractRedissonCoroutineTest`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/AbstractRedissonCoroutineTest.kt)
- [`AbstractCacheExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/AbstractCacheExample.kt)
- [`ActorSchema`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/ActorSchema.kt)
- [`CacheApplication`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheApplication.kt)
- [`CacheReadThroughExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheReadThroughExample.kt)
- [`CacheWriteBehindExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteBehindExample.kt)
- [`CacheWriteBehindForIoTData`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteBehindForIoTData.kt)
- [`CacheWriteThroughExample`](../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteThroughExample.kt)
