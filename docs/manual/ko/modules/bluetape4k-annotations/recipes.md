---
title: 실전 레시피
description: API 제공자와 사용자 관점에서 marker 선언, 좁은 opt-in, stable wrapper, SPI 제한을 적용하는 예를 모았습니다.
manualId: bluetape4k-annotations
chapterId: recipes
---

# 실전 레시피

## 실험 API를 공개하고 한 함수에서만 사용하기

제공자는 불안정한 계약을 가진 선언에 marker를 붙입니다.

```kotlin
@BluetapeExperimentalApi
fun planNextGenerationIndex(): IndexPlan = TODO()
```

사용자는 계약을 받아들이는 가장 작은 범위에 opt-in합니다.

```kotlin
@OptIn(BluetapeExperimentalApi::class)
fun evaluateNewIndex(): Report =
    benchmark(planNextGenerationIndex())
```

## Delicate API를 adapter 뒤에 가두기

수명주기나 serialization 위험을 이해해야 하는 API는 adapter 하나에 모읍니다. Bluetape Kafka codec도 이런 경계를 `BluetapeDelicateApi`로 표시합니다.

```kotlin
class TrustedPayloadDecoder {

    @OptIn(BluetapeDelicateApi::class)
    fun decode(bytes: ByteArray): Payload {
        require(bytes.isNotEmpty())
        return KafkaCodecs.Jdk.decode(bytes) as Payload
    }
}
```

adapter가 입력 검증과 허용 type을 책임지지 못한다면 이를 stable wrapper라고 부를 수 없습니다. 그 경우 wrapper에도 marker를 전파합니다.

## Internal API를 integration bridge에서 쓰기

`BluetapeInternalApi`는 일반 애플리케이션 코드에서 사용하지 않습니다. framework 연동처럼 public-for-technical-reasons 선언을 피할 수 없는 작은 bridge에서만 허용합니다.

```kotlin
@OptIn(BluetapeInternalApi::class)
internal fun installFrameworkBridge(registry: Registry) {
    registry.register(internalAdapter())
}
```

bridge 자체를 `internal`로 닫으면 위험이 다시 외부로 퍼지는 것을 막을 수 있습니다.

## 외부 구현만 제한하기

```kotlin
@SubclassOptInRequired(BluetapeImplementationApi::class)
interface QueryEngine {
    fun execute(query: Query): Result
}

fun run(engine: QueryEngine, query: Query): Result = engine.execute(query)

@OptIn(BluetapeImplementationApi::class)
class CustomQueryEngine : QueryEngine {
    override fun execute(query: Query): Result = TODO()
}
```

`run`은 opt-in이 필요 없고 `CustomQueryEngine`만 구현 계약을 받아들입니다.

## 실험 모듈 전체를 opt-in하기

source set 전체가 같은 실험 정책을 공유할 때만 compiler option을 씁니다.

```kotlin
kotlin {
    compilerOptions {
        optIn.add("io.bluetape4k.annotations.BluetapeExperimentalApi")
    }
}
```

일반 production 모듈에는 권장하지 않습니다. 새 experimental 호출이 들어와도 source diff에 `@OptIn`이 나타나지 않기 때문입니다.

## Review checklist

- visibility로 닫을 수 있는 선언에 marker를 붙이지 않았는가
- marker가 실제 불안정한 계약을 설명하는가
- error와 warning을 편의상 고르지 않았는가
- `@OptIn` 범위가 필요한 곳보다 넓지 않은가
- public wrapper가 위험을 실제로 흡수하는가, 아니면 marker를 전파해야 하는가
- Java caller와 migration 경로를 문서로 보완했는가
- upgrade 때 다시 검토할 owner가 정해져 있는가

## 검증

```bash
./gradlew :bluetape4k-annotations:test --no-configuration-cache
```

현재 test는 일반 marker가 명시적인 opt-in과 함께 compile되는지, 구현 전용 marker가 local SPI 구현에서 작동하는지, 여섯 marker type name이 유지되는지를 확인합니다.

## 근거

- [Marker 계약 test](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
- [Kafka codec source](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt)
- [Module build](../../../../../bluetape4k/annotations/build.gradle.kts)
