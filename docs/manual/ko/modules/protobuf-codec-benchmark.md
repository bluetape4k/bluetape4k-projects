---
manualId: protobuf-codec-benchmark
title: "Protobuf 코덱 성능 비교"
description: "이 모듈은 bluetape4k 직렬화와 Redisson 연동 코드에서 사용하는 protobuf codec 경로를 측정합니다. 단일 protobuf payload를 메모리에 두고 encode/decode 처리량을 비교하므로, codec 구현 간 상대적인 차이를 보기 위한 좁은 범위의 benchmark입니다."
kind: benchmark
group: examples
learningOrder: 1510
---

# Protobuf 코덱 성능 비교

## 해결하는 문제 {#problem}

이 모듈은 bluetape4k 직렬화와 Redisson 연동 코드에서 사용하는 protobuf codec 경로를 측정합니다. 단일 protobuf payload를 메모리에 두고 encode/decode 처리량을 비교하므로, codec 구현 간 상대적인 차이를 보기 위한 좁은 범위의 benchmark입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 가설, benchmark task, 환경, metric 방향, 비교 범위이 필요할 때 `protobuf-codec-benchmark`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

이 benchmark project는 Maven artifact로 게시하지 않습니다. 저장소에서 실행하고 명령을 선택하기 전에 Gradle task를 확인합니다.

Gradle project path는 `:protobuf-codec-benchmark`, source directory는 `benchmark/protobuf-codec-benchmark`입니다.

## 핵심 개념 {#concepts}

이 모듈은 설정 또는 platform metadata이며 index할 Kotlin/Java source type이 없습니다.

## 빠른 시작 {#quick-start}

example이나 benchmark를 실행하기 전에 project task를 확인합니다.

```bash
./gradlew :protobuf-codec-benchmark:tasks --all
```

그다음 모듈 README에 기록된 명령을 사용하고 필요한 외부 service는 격리합니다.

## 작업별 API {#api-by-task}

이 모듈에는 등록된 Kotlin/Java source file이 없습니다. build model과 README가 public surface입니다.

## 권장 패턴 {#patterns}

정확한 17-cell matrix는 serializer, Redisson, Lettuce 경로를 비교합니다. 유지된 optimized candidate만
allocation claim 대상이며 baseline, compatibility control, fallback cell, 롤백된 serializer decode cell은 claim에
사용할 수 없습니다. 커밋된 issue #757 report에는 canonical run 2회가 있으며 Lettuce heap/direct allocation
결과를 accepted로 판정하지만 zero-copy나 일반 throughput 향상을 증명하지는 않습니다.

## 연동 {#integrations}

모듈 build에 직접적인 `api`, `implementation`, `compileOnly`, `runtimeOnly` dependency line이 없습니다. build file의 plugin과 generated metadata를 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

JMH JAR 하나를 build하고 hash와 file identity를 고정한 뒤 rebuild 없이 canonical profile을 두 번 실행합니다.
동등한 환경만 비교하고 fail-closed runner가 accepted한 근거만 publish하며 delivery manifest에서 report를 다시
생성합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :protobuf-codec-benchmark:test --no-configuration-cache
```

manifest의 test path에서 Kotlin/Java test file을 찾지 못했습니다. module build를 확인하고 다른 곳에서 검증하지 않는 동작을 도입할 때 focused contract test를 추가합니다.

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../benchmark/protobuf-codec-benchmark/README.ko.md)
- [모듈 build](../../../../benchmark/protobuf-codec-benchmark/build.gradle.kts)
- [커밋된 issue #757 report](../../../benchmarks/2026-07-18-protobuf-buffer-allocation.md)
