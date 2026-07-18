---
manualId: serializer-benchmark
title: "직렬화 할당량 성능 비교"
description: "이 비배포 모듈은 기존 serializer의 ByteArray, 호환 ByteBuffer, 최적화 ByteBuffer 경로에서 할당 동작을 검증합니다. production dispatch, wire format, 소유권, 보안 설정은 변경하지 않습니다."
kind: benchmark
group: examples
learningOrder: 1515
---

# 직렬화 할당량 성능 비교

## 해결하는 문제 {#problem}

이 비배포 모듈은 기존 serializer의 ByteArray, 호환 ByteBuffer, 최적화 ByteBuffer 경로에서 할당 동작을 검증합니다. production dispatch, wire format, 소유권, 보안 설정은 변경하지 않습니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 실행 entry point, 필요한 service, 기대 동작, 예제가 보여 주는 production pattern이 필요할 때 `serializer-benchmark`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

이 benchmark project는 Maven artifact로 게시하지 않습니다. 저장소에서 실행하고 명령을 선택하기 전에 Gradle task를 확인합니다.

Gradle project path는 `:serializer-benchmark`, source directory는 `benchmark/serializer-benchmark`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `AvroSerializerAllocationBenchmark`, `BinarySerializerAllocationBenchmark`, `JsonSerializerAllocationBenchmark`, `SerializerBenchmarkPayload`, `SerializerBenchmarkSupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

example이나 benchmark를 실행하기 전에 project task를 확인합니다.

```bash
./gradlew :serializer-benchmark:tasks --all
```

그다음 모듈 README에 기록된 명령을 사용하고 필요한 외부 service는 격리합니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`AvroSerializerAllocationBenchmark`](../../../../benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/AvroSerializerAllocationBenchmark.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BinarySerializerAllocationBenchmark`](../../../../benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/BinarySerializerAllocationBenchmark.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JsonSerializerAllocationBenchmark`](../../../../benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/JsonSerializerAllocationBenchmark.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SerializerBenchmarkPayload`](../../../../benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkPayload.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SerializerBenchmarkSupport`](../../../../benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupport.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **명령**, **매트릭스**, **Buffer 계약** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(project(":bluetape4k-io"))
implementation(project(":bluetape4k-json"))
implementation(project(":bluetape4k-jackson2"))
implementation(project(":bluetape4k-jackson3"))
implementation(project(":bluetape4k-fastjson2"))
implementation(project(":bluetape4k-avro"))
implementation(libs.kryo5)
implementation(bt4k.fory.kotlin)
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
./gradlew :serializer-benchmark:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`SerializerBenchmarkSupportTest`](../../../../benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupportTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../benchmark/serializer-benchmark/README.ko.md)
- [모듈 build](../../../../benchmark/serializer-benchmark/build.gradle.kts)
- [`AvroSerializerAllocationBenchmark`](../../../../benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/AvroSerializerAllocationBenchmark.kt)
- [`BinarySerializerAllocationBenchmark`](../../../../benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/BinarySerializerAllocationBenchmark.kt)
- [`JsonSerializerAllocationBenchmark`](../../../../benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/JsonSerializerAllocationBenchmark.kt)
- [`SerializerBenchmarkPayload`](../../../../benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkPayload.kt)
- [`SerializerBenchmarkSupport`](../../../../benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupport.kt)
- [`SerializerBenchmarkSupportTest`](../../../../benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupportTest.kt)
