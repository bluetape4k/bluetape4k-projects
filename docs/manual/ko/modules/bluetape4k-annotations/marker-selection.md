---
title: Marker 선택
description: 변경 가능성, 공개 경계, 운영 계약, 구현 안정성을 기준으로 여섯 marker를 고르는 방법을 설명합니다.
manualId: bluetape4k-annotations
chapterId: marker-selection
---

# Marker 선택

marker 이름만 보고 고르면 `Experimental`이나 `Internal`에 의미가 몰리기 쉽습니다. 먼저 무엇이 불안정한지 정해야 합니다. 호출 계약이 바뀔 수 있는지, public으로 노출됐지만 지원 대상이 아닌지, 안전하게 쓰려면 별도 지식이 필요한지, 외부 구현만 제한할 것인지가 선택 기준입니다.

![Bluetape API marker 선택 지도](../../../assets/annotations/annotation-decision-map.svg)

## 먼저 visibility를 확인합니다

외부에서 쓸 이유가 없는 선언은 `internal`이나 `private`으로 닫는 편이 낫습니다. marker는 public 선언에 정책을 덧붙이는 도구이지, visibility를 대신하는 장치가 아닙니다. inline 함수의 제약이나 framework 연동처럼 public 노출을 피할 수 없을 때 marker를 검토합니다.

## 여섯 marker의 계약

| Marker | 진단 수준 | 붙이는 상황 | 호출자가 받아들이는 것 |
| --- | --- | --- | --- |
| `BluetapeExperimentalApi` | Error | 계약이 바뀌거나 API가 사라질 수 있음 | source·binary 호환성을 보장하지 않음 |
| `BluetapeBetaApi` | Warning | 안정화가 가까웠지만 작은 변경 가능성이 남음 | minor source·binary·behavior 변경 가능성 |
| `BluetapeInternalApi` | Error | 기술적인 이유로만 public임 | 지원되는 외부 API가 아니며 예고 없이 바뀔 수 있음 |
| `BluetapeDelicateApi` | Warning | 수명주기, 동시성, 자원, 보안 지식이 필요함 | 문서와 실패 동작을 이해하고 사용함 |
| `BluetapeObsoleteApi` | Error | 마이그레이션이나 호환성을 위해서만 남아 있음 | 새 코드에서는 사용하지 않으며 제거 가능성을 받아들임 |
| `BluetapeImplementationApi` | Warning | 호출은 안정적이지만 외부 구현은 아직 안정적이지 않음 | 구현이나 상속 계약의 변경 가능성 |

`Experimental`, `Internal`, `Obsolete`는 opt-in하지 않으면 compile이 실패합니다. `Beta`와 `Delicate`는 warning을 냅니다. `Implementation`은 일반 호출이 아니라 `@SubclassOptInRequired`가 붙은 type을 구현하거나 상속할 때 작동합니다.

## 헷갈리기 쉬운 경계

### Experimental과 Beta

둘 다 안정화 전 API를 나타냅니다. 호환성을 약속할 수 없고 제거 가능성도 있다면 `Experimental`입니다. 방향은 정해졌고 안정화를 예상하지만 작은 수정 가능성이 남았다면 `Beta`가 맞습니다. warning을 받고도 그대로 build할 수 있다는 이유만으로 `Beta`를 고르면 안 됩니다.

### Internal과 Delicate

`Internal`은 외부 사용 자체가 지원 대상이 아니라는 뜻입니다. `Delicate`는 외부에서 사용할 수 있지만 안전하게 쓰려면 계약을 먼저 이해해야 한다는 뜻입니다. 예를 들어 현재 Kafka binary codec은 serialization 취약성과 type-safety 경계를 알리기 위해 `BluetapeDelicateApi`를 사용합니다.

### Obsolete와 Deprecated

`Obsolete`는 이 모듈의 opt-in 정책입니다. 대체 API와 migration message를 IDE에 보여 줘야 한다면 Kotlin `@Deprecated`가 필요합니다. 두 annotation은 역할이 다르므로, 제거 경로를 안내해야 하는 API에서는 함께 쓸 수 있습니다.

## Marker를 겹쳐 붙일 때

대부분은 계약을 가장 잘 설명하는 marker 하나면 충분합니다. 여러 marker를 붙이면 사용자가 무엇을 승인하는지 흐려집니다. 성숙도와 운영 위험이 정말 독립된 계약이라면 겹칠 수 있지만, 각 marker가 왜 필요한지 KDoc에 따로 설명해야 합니다.

## 근거

- [여섯 marker source](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations)
- [Marker 선택 표가 있는 README](../../../../../bluetape4k/annotations/README.ko.md)
- [Kafka codec의 Delicate 사용 예](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt)
