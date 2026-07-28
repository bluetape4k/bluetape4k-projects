# WIP - bluetape4k-projects

스냅샷: 2026-06-06 KST
범위: 1.11.0 observability/telemetry 작업선과 backlog 분리
열린 이슈 수: 13개

## 갱신 메모

`1.10.0` stable line은 이미 배포됐고 downstream repository에서 소비됐다.
이제 개발 흐름은 `1.11.0` observability와 event-telemetry 작업선으로 이동한다.

Live GitHub 상태:

- `1.11.0`: 열린 이슈 11개
- `backlog`: 열린 이슈 2개
- 전체 열린 이슈 수: 13개

## 최근 완료 항목

- `#493`은 near-cache backend capability matrix와 conformance coverage를 추가했다.
- `#474`는 1.9.0 breaking-change line에 맞춰 deprecated Kafka, OpenTelemetry, cache, Redis, Resilience4j alias를 제거했다.
- `#596`은 재사용 가능한 `EtcdServer` Testcontainers launcher를 추가했다.
- `#607` / `#608`은 1.9.1 UTF-8 truncation 수정과 catalog-governance 수정을 배포했다.
- `#595`는 IO HTTP, Elasticsearch-backed search messaging, Memgraph-backed graph test의 Nightly 실패를 고쳤다.
- `#620`은 `bluetape4k-projects` `1.9.1` 이후 downstream BOM/catalog handoff를 조율했다.
- `#580`은 Fory-backed Kafka/Kafka4 codec을 `@BluetapeDelicateApi`로 표시하고 deserialization trust boundary를 문서화했다.
- PR #600은 `1.9.0-SNAPSHOT`용 source version을 준비했고 snapshot publish workflow는 성공적으로 완료됐다.

## 현재 방향

이 repository는 `1.10.0` stable release 이후 `1.11.0` development lane에 있다.
작업 선택은 observability Epic과 그 명시적인 dependency order를 우선해야 한다.
독립적인 test/docs cleanup은 작고 고립된 단위로 유지한다.

## 우선순위 큐

| 우선순위 | 이슈 | 난이도 | 메모 |
|---|---|---:|---|
| P0 | [#697](https://github.com/bluetape4k/bluetape4k-projects/issues/697) WIP backlog refresh | S | 현재 작업. 이 파일이 live milestone/backlog 상태를 반영한 뒤 닫는다. |
| P1 | [#696](https://github.com/bluetape4k/bluetape4k-projects/issues/696) shared telemetry contract | M | Spring Boot, Ktor, event, example 작업 전에 필요한 design gate. |
| P1 | [#702](https://github.com/bluetape4k/bluetape4k-projects/issues/702) NearJCache remove semantics | S | 독립적인 regression-test 개선. #696 전후 어느 쪽에서도 안전하게 진행할 수 있다. |
| P1 | [#691](https://github.com/bluetape4k/bluetape4k-projects/issues/691) Ktor opt-in OpenTelemetry tracing | M | #696 이후 첫 implementation slice. Spring/event 작업선보다 blast radius가 낮다. |
| P1 | [#694](https://github.com/bluetape4k/bluetape4k-projects/issues/694) Spring Boot 4 observability helpers | M | #696 이후 진행한다. Actuator/Micrometer convention은 application-owned로 유지한다. |
| P2 | [#695](https://github.com/bluetape4k/bluetape4k-projects/issues/695) event publish/consume telemetry helpers | M | #696 이후 진행하고 HTTP telemetry 작업에서 검증된 결정을 재사용한다. |
| P2 | [#692](https://github.com/bluetape4k/bluetape4k-projects/issues/692) observability examples | M | Spring Boot, Ktor, event helper issue에 의존한다. |
| P2 | [#699](https://github.com/bluetape4k/bluetape4k-projects/issues/699) Copilot instruction alignment | S | 독립적인 docs/agent-guidance cleanup. |
| P2 | [#698](https://github.com/bluetape4k/bluetape4k-projects/issues/698) HC5 interceptor ordering examples | S | 독립적인 test/docs cleanup. behavior를 고치기 전에 기대되는 HC5 ordering을 검증한다. |
| P2 | [#701](https://github.com/bluetape4k/bluetape4k-projects/issues/701) FutureUtils virtual-thread TODO | M | API-boundary cleanup. compatibility와 public KDoc을 보존한다. |
| P3 | [#690](https://github.com/bluetape4k/bluetape4k-projects/issues/690) observability telemetry Epic | L | Umbrella issue. child issue가 완료될 때까지 열어둔다. |

## Backlog 큐

| 이슈 | 메모 |
|---|---|
| [#700](https://github.com/bluetape4k/bluetape4k-projects/issues/700) Central snapshot task naming audit | Release-documentation cleanup. snapshot workflow drift가 blocker가 되기 전에는 1.11.0 범위 밖에 둔다. |
| [#706](https://github.com/bluetape4k/bluetape4k-projects/issues/706) data-r2dbc pool benchmark validation mode | Performance/benchmark lane. benchmark workflow가 active focus일 때만 승격한다. |

## 의존성 지도

```text
#690 observability and event telemetry Epic
  -> #696 shared telemetry contract
    -> #691 Ktor opt-in OpenTelemetry tracing
    -> #694 Spring Boot 4 observability helpers
    -> #695 event publish/consume telemetry helpers
      -> #692 Prometheus and OTLP examples

#697 WIP refresh
  -> update this file before choosing the next 1.11.0 execution slice

#702 NearJCache remove semantics
#699 Copilot instruction alignment
#698 HC5 interceptor ordering examples
#701 FutureUtils virtual-thread TODO
  -> independent of the observability Epic
```

## WIP 제한

| 작업선 | 제한 | 다음 작업 |
|---|---:|---|
| WIP / management | 1 | `#697`. 이 refresh가 merge되거나 별도로 승인된 뒤 닫는다. |
| Observability design | 1 | `#696` 먼저 진행한다. |
| Observability implementation | 1 | `#691`, 이후 `#694`, 이후 `#695`. |
| Observability examples | 1 | helper API가 생긴 뒤에만 `#692`를 진행한다. |
| Independent cleanup | 1 | `#702`를 우선하고, 아니면 `#699`, `#698`, `#701` 중 선택한다. |
| Backlog | 0 | 명시적으로 승격되기 전까지 `#700`과 `#706`은 대기 상태로 둔다. |

## 정리 작업

| 대상 | 조치 |
|---|---|
| `#697` | WIP refresh가 merge되거나 별도로 수락된 뒤에만 닫는다. |
| `#690` | child issue가 완료될 때까지 1.11.0 umbrella로 열어둔다. |
| `backlog` milestone | 우선순위가 바뀌기 전에는 `#700`과 `#706`을 1.11.0 lane 밖에 둔다. |
| Old 1.9.x / 1.10.0 queue references | historical context로만 취급하고 현재 작업 순서에는 사용하지 않는다. |
